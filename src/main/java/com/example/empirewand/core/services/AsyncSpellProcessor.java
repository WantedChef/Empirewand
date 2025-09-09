package com.example.empirewand.core.services;

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

import com.example.empirewand.api.EffectService;
import com.example.empirewand.core.text.TextService;
import com.example.empirewand.spell.CastResult;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;

import net.kyori.adventure.text.Component;

/**
 * Optimized asynchronous spell processing system.
 * Provides dedicated thread pools for spell execution with proper error
 * handling and performance monitoring.
 */
public final class AsyncSpellProcessor {

    /** Lightweight, non-leaky reference to the plugin. */
    private static final class PluginRef {
        private final String name;

        PluginRef(@NotNull Plugin plugin) {
            this.name = Objects.requireNonNull(plugin, "plugin").getName();
        }

        Plugin get() {
            return Bukkit.getPluginManager().getPlugin(name);
        }

        boolean isEnabled() {
            final Plugin p = get();
            return p != null && p.isEnabled();
        }

        void runOnMain(@NotNull Runnable task) {
            final Plugin p = get();
            if (p != null && p.isEnabled()) {
                Bukkit.getScheduler().runTask(p, task);
            }
        }
    }

    private final PluginRef pluginRef;
    private final Logger logger;
    private final ThreadPoolExecutor spellExecutor;
    private final MetricsCollector metrics;
    private final AtomicLong threadCounter = new AtomicLong(0);

    /**
     * Internal metrics collector for spell performance tracking.
     * Uses LongAdder to avoid synchronization hotspots.
     */
    private static final class MetricsCollector {
        private final LongAdder totalSpellsProcessed = new LongAdder();
        private final LongAdder totalProcessingTime = new LongAdder();
        private final LongAdder failedSpells = new LongAdder();

        void recordSpellCast(long processingTimeNanos, boolean success) {
            totalSpellsProcessed.increment();
            totalProcessingTime.add(processingTimeNanos);
            if (!success) {
                failedSpells.increment();
            }
        }

        void recordSpellFailure() {
            failedSpells.increment();
        }

        double getAverageProcessingTimeMs() {
            long processed = totalSpellsProcessed.sum();
            if (processed == 0) return 0.0;
            return (totalProcessingTime.sum() / (double) processed) / 1_000_000.0;
        }

        long getTotalSpellsProcessed() {
            return totalSpellsProcessed.sum();
        }

        long getFailedSpells() {
            return failedSpells.sum();
        }
    }

    public AsyncSpellProcessor(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.pluginRef = new PluginRef(plugin);
        this.logger = plugin.getLogger(); // instance logger is fine (plugin-provided)
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

        logger.info(String.format("AsyncSpellProcessor initialized with %d-%d threads", coreThreads, maxThreads));
    }

    /**
     * Casts a spell asynchronously with proper error handling and metrics.
     */
    public CompletableFuture<CastResult> castSpellAsync(@NotNull Spell<?> spell, @NotNull SpellContext context) {
        Objects.requireNonNull(spell, "spell");
        Objects.requireNonNull(context, "context");
        final long startTime = System.nanoTime();

        return CompletableFuture.supplyAsync(() -> {
            try {
                CastResult result = spell.cast(context);
                boolean ok = result != null && result.isSuccess();
                metrics.recordSpellCast(System.nanoTime() - startTime, ok);
                // --- FIX: removed the extra ')' that broke parsing and return type ---
                return (result != null) ? result : CastResult.fail(Component.text("Spell returned null result"));
            } catch (Throwable t) {
                logger.log(Level.WARNING,
                        () -> String.format("Async spell casting failed for '%s': %s", safeKey(spell), t.getMessage()));
                logger.log(Level.FINE, "Stacktrace for failed async spell cast", t);
                metrics.recordSpellFailure();
                return CastResult.fail(Component.text("Spell casting failed: " + t.getMessage()));
            }
        }, spellExecutor);
    }

    /**
     * Casts a spell asynchronously and handles the result on the main thread.
     * This is the preferred method for most spell casting scenarios.
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
                pluginRef.runOnMain(() ->
                        handleCastResult(spell, context, result, throwable, effectService, textService));
            }
        });
    }

    /**
     * Handles the result of an asynchronous spell cast on the main thread.
     */
    private void handleCastResult(@NotNull Spell<?> spell,
                                  @NotNull SpellContext context,
                                  CastResult result,
                                  Throwable throwable,
                                  @NotNull EffectService effectService,
                                  @NotNull TextService textService) {

        // Touch textService to avoid "never read" hint (and handy for deep debug)
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Callback using TextService: " + textService.getClass().getSimpleName());
        }

        if (throwable != null) {
            logger.log(Level.WARNING,
                    () -> String.format("Exception in async spell cast for '%s'", safeKey(spell)));
            logger.log(Level.FINE, "Stacktrace in async spell callback", throwable);
            if (context.caster().isOnline()) {
                effectService.actionBar(context.caster(),
                        Component.text("§cSpell failed: " + throwable.getMessage()));
            }
            return;
        }

        if (result == null) {
            logger.warning(() -> String.format("Null result from async spell cast for '%s'", safeKey(spell)));
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
     * Gets performance metrics for monitoring.
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
     * Shuts down the async processor gracefully.
     */
    public void shutdown() {
        logger.info("Shutting down AsyncSpellProcessor...");

        spellExecutor.shutdown();
        try {
            if (!spellExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                spellExecutor.shutdownNow();
                if (!spellExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.warning("AsyncSpellProcessor did not terminate cleanly");
                }
            }
        } catch (InterruptedException e) {
            spellExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info(() -> String.format("AsyncSpellProcessor shut down. Processed %d spells",
                metrics.getTotalSpellsProcessed()));
    }

    private static String safeKey(Spell<?> spell) {
        try { return String.valueOf(spell.key()); }
        catch (Throwable ignored) { return "<unknown-spell>"; }
    }

    /**
     * Metrics data class for external monitoring.
     */
    public record SpellProcessorMetrics(
            long totalSpellsProcessed,
            double averageProcessingTimeMs,
            long failedSpells,
            int activeThreads,
            int queuedSpells
    ) {}
}
