package nl.wantedchef.empirewand.framework.util;

import nl.wantedchef.empirewand.api.Scheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * A Paper-based implementation of the {@link Scheduler} facade.
 * This class wraps Bukkit's scheduler to provide a simplified and standardized way to execute tasks
 * on the main server thread or asynchronously.
 *
 * @since 2.0.0
 */
public final class PaperScheduler implements Scheduler {
    private final JavaPlugin plugin;

    /**
     * Constructs a new PaperScheduler.
     *
     * @param plugin The plugin instance.
     */
    public PaperScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs a task on the main server thread.
     * If the current thread is already the main thread, the task is executed immediately.
     *
     * @param runnable The task to run.
     */
    @Override
    public void runMain(@NotNull Runnable runnable) {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Runs a task asynchronously off the main server thread.
     *
     * @param runnable The task to run.
     */
    @Override
    public void runAsync(@NotNull Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }
}





