package nl.wantedchef.empirewand.framework.service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import nl.wantedchef.empirewand.api.service.EffectService;
import nl.wantedchef.empirewand.core.text.TextService;
import nl.wantedchef.empirewand.spell.CastResult;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;

import net.kyori.adventure.text.Component;

/**
 * Optimized asynchronous spell processing system.
 * <p>
 * This class provides a dedicated, managed thread pool for executing spells
 * asynchronously. It ensures that spell casting does not block the main server
 * thread, improving performance and responsiveness. It includes features for
 * performance monitoring, error handling, and graceful shutdown.
 */
public final class AsyncSpellProcessor {

    /**
     * A lightweight, non-leaky reference to the plugin.
     * <p>
     * This class avoids holding a direct reference to the Plugin instance,
     * which can prevent proper garbage collection during plugin reloads. It
     * retrieves the plugin instance from Bukkit's PluginManager when needed.
     */
    private static final class PluginRef {

        private final String name;

        /**
         * Creates a new PluginRef.
         *
         * @param plugin The plugin instance. Must not be null.
         */
        PluginRef(@NotNull Plugin plugin) {
            this.name = Objects.requireNonNull(plugin, "plugin").getName();
        }

        /**
         * Gets the plugin instance.
         *
         * @return The plugin instance, or null if not found.
         */
        Plugin get() {
            return Bukkit.getPluginManager().getPlugin(name);
        }

        /**
         * Checks if the plugin is enabled.
         *
         * @return true if the plugin is enabled, false otherwise.
         */
        boolean isEnabled() {
            final Plugin p = get();
            return p != null && p.isEnabled();
        }

        /**
         * Runs a task on the main server thread.
         *
         * @param task The task to run. Must not be null.
         */
        void runOnMain(@NotNull Runnable task) {
            final Plugin p = get();
            if (p != null && p.isEnabled()) {
                Bukkit.getScheduler().runTask(p, task);
            }
        }
    }

    private final PluginRef pluginRef;
    private static final Logger LOGGER = Logger.getLogger(AsyncSpellProcessor.class.getName());
    private final ThreadPoolExecutor spellExecutor;
    private final MetricsCollector metrics;
    private final AtomicLong threadCounter = new AtomicLong(0);

    /**
     * Internal metrics collector for tracking spell performance and failures.
     * This class is thread-safe and uses LongAdder for high-performance,
     * concurrent updates.
     */
    private static final class MetricsCollector {

        private final LongAdder totalSpellsProcessed = new LongAdder();
        private final LongAdder totalProcessingTime = new LongAdder();
        private final LongAdder failedSpells = new LongAdder();

        /**
         * Records the result of a single spell cast.
         *
         * @param processingTimeNanos The time it took to process the spell, in
         * nanoseconds.
         * @param success True if the spell was successful, false otherwise.
         */
        void recordSpellCast(long processingTimeNanos, boolean success) {
            totalSpellsProcessed.increment();
            totalProcessingTime.add(processingTimeNanos);
            if (!success) {
                failedSpells.increment();
            }
        }

        /**
         * Records a spell failure that occurred before or after the main
         * execution.
         */
        void recordSpellFailure() {
            failedSpells.increment();
        }

        /**
         * Calculates the average spell processing time.
         *
         * @return The average processing time in milliseconds.
         */
        double getAverageProcessingTimeMs() {
            long processed = totalSpellsProcessed.sum();
            if (processed == 0) {
                return 0.0;
            }
            return (totalProcessingTime.sum() / (double) processed) / 1_000_000.0;
        }

        /**
         * Gets the total number of spells processed.
         *
         * @return The total spell count.
         */
        long getTotalSpellsProcessed() {
            return totalSpellsProcessed.sum();
        }

        /**
         * Gets the total number of failed spells.
         *
         * @return The failed spell count.
         */
        long getFailedSpells() {
            return failedSpells.sum();
        }
    }

    /**
     * Constructs a new AsyncSpellProcessor.
     *
     * @param plugin The plugin instance. Must not be null.
     */
    public AsyncSpellProcessor(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.pluginRef = new PluginRef(plugin);
        // Use class logger (static) to conform to logging best practices
        this.metrics = new MetricsCollector();

        final int cores = Runtime.getRuntime().availableProcessors();
        final int coreThreads = Math.max(2, cores / 2);
        final int maxThreads = Math.max(coreThreads, cores);

        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setName("EmpireWand-SpellProcessor-" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };

        this.spellExecutor = new ThreadPoolExecutor(
                coreThreads,
                maxThreads,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                tf,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        LOGGER.info(() -> String.format("AsyncSpellProcessor initialized with %d-%d threads", coreThreads, maxThreads));
    }

    /**
     * Casts a spell asynchronously, returning a future with the result. This
     * method offloads the spell execution to a worker thread.
     *
     * @param spell The spell to cast. Must not be null.
     * @param context The context of the spell cast. Must not be null.
     * @return A CompletableFuture that will complete with the CastResult.
     */
    public CompletableFuture<CastResult> castSpellAsync(@NotNull Spell<?> spell, @NotNull SpellContext context) {
        Objects.requireNonNull(spell, "spell");
        Objects.requireNonNull(context, "context");
        final long startTime = System.nanoTime();

        return CompletableFuture.supplyAsync(() -> {
            try {
                CastResult result = spell.cast(context);
                boolean ok = result.isSuccess();
                metrics.recordSpellCast(System.nanoTime() - startTime, ok);
                return result;
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING,
                        () -> String.format("Async spell casting failed for '%s': %s", safeKey(spell), t.getMessage()));
                LOGGER.log(Level.FINE, "Stacktrace for failed async spell cast", t);
                metrics.recordSpellFailure();
                return CastResult.fail(Component.text("Spell casting failed: " + t.getMessage()));
            }
        }, spellExecutor);
    }

    /**
     * Casts a spell asynchronously and handles the result on the main thread.
     * This is the preferred method for most spell casting scenarios as it
     * ensures that any Bukkit API calls in the result handler are performed
     * safely.
     *
     * @param spell The spell to cast. Must not be null.
     * @param context The context of the spell cast. Must not be null.
     * @param effectService The service to use for displaying effects. Must not
     * be null.
     * @param textService The service to use for displaying text. Must not be
     * null.
     */
    public void castSpellAsyncWithCallback(@NotNull Spell<?> spell,
            @NotNull SpellContext context,
            @NotNull EffectService effectService,
            @NotNull TextService textService) {
        Objects.requireNonNull(spell, "spell");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(effectService, "effectService");
        Objects.requireNonNull(textService, "textService");

        castSpellAsync(spell, context).whenComplete((result, throwable) -> {
            if (pluginRef.isEnabled()) {
                pluginRef.runOnMain(()
                        -> handleCastResult(spell, context, result, throwable, effectService, textService));
            }
        });
    }

    /**
     * Handles the result of an asynchronous spell cast on the main thread. This
     * method is responsible for providing feedback to the player based on the
     * outcome.
     *
     * @param spell The spell that was cast.
     * @param context The original spell context.
     * @param result The result of the spell cast, may be null.
     * @param throwable The exception thrown during casting, may be null.
     * @param effectService The service for displaying effects.
     * @param textService The service for displaying text.
     */
    private void handleCastResult(@NotNull Spell<?> spell,
            @NotNull SpellContext context,
            CastResult result,
            Throwable throwable,
            @NotNull EffectService effectService,
            @NotNull TextService textService) {

        // Touch textService to avoid "never read" hint (and handy for deep debug)
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer(() -> String.format("Callback using TextService: %s", textService.getClass().getSimpleName()));
        }

        if (throwable != null) {
            LOGGER.log(Level.WARNING,
                    () -> String.format("Exception in async spell cast for '%s'", safeKey(spell)));
            LOGGER.log(Level.FINE, "Stacktrace in async spell callback", throwable);
            if (context.caster().isOnline()) {
                effectService.actionBar(context.caster(),
                        Component.text("§cSpell failed: " + throwable.getMessage()));
            }
            return;
        }

        if (result == null) {
            LOGGER.warning(() -> String.format("Null result from async spell cast for '%s'", safeKey(spell)));
            if (context.caster().isOnline()) {
                effectService.actionBar(context.caster(), Component.text("§cSpell failed: Unknown error"));
            }
            return;
        }

        // Handle success/failure feedback
        if (context.caster().isOnline()) {
            if (result.isSuccess()) {
                effectService.showSuccess(context.caster(), "spell-cast");
            } else {
                effectService.showError(context.caster(), "spell-failed");
            }
        }
    }

    /**
     * Gets performance metrics for monitoring the state of the spell processor.
     *
     * @return A snapshot of the current performance metrics.
     */
    public SpellProcessorMetrics getMetrics() {
        return new SpellProcessorMetrics(
                metrics.getTotalSpellsProcessed(),
                metrics.getAverageProcessingTimeMs(),
                metrics.getFailedSpells(),
                spellExecutor.getActiveCount(),
                spellExecutor.getQueue().size()
        );
    }

    /**
     * Shuts down the async processor gracefully, allowing currently running
     * tasks to complete.
     */
    public void shutdown() {
        LOGGER.info("Shutting down AsyncSpellProcessor...");

        spellExecutor.shutdown();
        try {
            if (!spellExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                spellExecutor.shutdownNow();
                if (!spellExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOGGER.warning("AsyncSpellProcessor did not terminate cleanly");
                }
            }
        } catch (InterruptedException e) {
            spellExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info(() -> String.format("AsyncSpellProcessor shut down. Processed %d spells",
                metrics.getTotalSpellsProcessed()));
    }

    /**
     * Safely gets the key of a spell, returning a placeholder if an error
     * occurs.
     *
     * @param spell The spell to get the key from.
     * @return The spell key, or a placeholder string if an error occurs.
     */
    private static String safeKey(Spell<?> spell) {
        try {
            return String.valueOf(spell.key());
        } catch (Throwable ignored) {
            return "<unknown-spell>";
        }
    }

    /**
     * A data class holding a snapshot of performance metrics for the spell
     * processor.
     *
     * @param totalSpellsProcessed The total number of spells processed since
     * startup.
     * @param averageProcessingTimeMs The average time to process a spell, in
     * milliseconds.
     * @param failedSpells The total number of spells that failed.
     * @param activeThreads The number of threads currently executing spells.
     * @param queuedSpells The number of spells waiting in the queue for
     * execution.
     */
    public record SpellProcessorMetrics(
            long totalSpellsProcessed,
            double averageProcessingTimeMs,
            long failedSpells,
            int activeThreads,
            int queuedSpells
            ) {

    }
}
