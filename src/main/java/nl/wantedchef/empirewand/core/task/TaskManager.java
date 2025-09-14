package nl.wantedchef.empirewand.core.task;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

/**
 * Unified TaskManager that delegates to AdvancedTaskManager for all behavior.
 * This preserves the existing TaskManager API while consolidating logic in one place.
 */
public class TaskManager {
    private final AdvancedTaskManager delegate;

    public TaskManager(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.delegate = new AdvancedTaskManager(plugin);
    }

    /** Register a task for tracking */
    public void registerTask(BukkitTask task) {
        delegate.registerTask(task);
    }

    /** Unregister a completed task (no-op: delegate cleans up internally) */
    public void unregisterTask(BukkitTask task) {
        // AdvancedTaskManager cleans up automatically; explicit removal not required.
    }

    /** Cancel and clear all tracked tasks */
    public void cancelAllTasks() {
        delegate.shutdown();
    }

    /** Get the number of active tasks (Bukkit only) */
    public int getActiveTaskCount() {
        return delegate.getMetrics().activeBukkitTasks();
    }

    /** Expose task manager metrics */
    public AdvancedTaskManager.TaskManagerMetrics getMetrics() {
        return delegate.getMetrics();
    }

    /** Simple health status relayed from delegate */
    public boolean isHealthy() {
        return delegate.isHealthy();
    }

    // Scheduling APIs (sync)
    public BukkitTask runTaskTimer(BukkitRunnable runnable, long delay, long period) {
        return delegate.runTaskTimer(runnable, delay, period);
    }

    public BukkitTask runTaskTimer(Runnable runnable, long delay, long period) {
        return delegate.runTaskTimer(runnable, delay, period);
    }

    public BukkitTask runTaskLater(BukkitRunnable runnable, long delay) {
        return delegate.runTaskLater(runnable, delay);
    }

    public BukkitTask runTaskLater(Runnable runnable, long delay) {
        return delegate.runTaskLater(runnable, delay);
    }

    public BukkitTask runTask(BukkitRunnable runnable) {
        return delegate.runTask(runnable);
    }

    public BukkitTask runTask(Runnable runnable) {
        return delegate.runTask(runnable);
    }

    // Scheduling APIs (async)
    public BukkitTask runTaskTimerAsynchronously(BukkitRunnable runnable, long delay, long period) {
        return delegate.runTaskTimerAsynchronously(runnable, delay, period);
    }

    public BukkitTask runTaskTimerAsynchronously(Runnable runnable, long delay, long period) {
        return delegate.runTaskTimerAsynchronously(runnable, delay, period);
    }

    public BukkitTask runTaskLaterAsynchronously(BukkitRunnable runnable, long delay) {
        return delegate.runTaskLaterAsynchronously(runnable, delay);
    }

    public BukkitTask runTaskLaterAsynchronously(Runnable runnable, long delay) {
        return delegate.runTaskLaterAsynchronously(runnable, delay);
    }

    public BukkitTask runTaskAsynchronously(BukkitRunnable runnable) {
        return delegate.runTaskAsynchronously(runnable);
    }

    public BukkitTask runTaskAsynchronously(Runnable runnable) {
        return delegate.runTaskAsynchronously(runnable);
    }
}



