package nl.wantedchef.empirewand.core.task;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

/**
 * Central task manager for tracking and cancelling all plugin tasks
 */
public class TaskManager {
    private final Plugin plugin;
    private final Set<BukkitTask> activeTasks = ConcurrentHashMap.newKeySet();

    public TaskManager(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Register a task for tracking
     */
    public void registerTask(BukkitTask task) {
        if (task != null) {
            activeTasks.add(task);
            // Automatically clean up cancelled tasks to prevent memory leaks
            cleanupCancelledTasks();
        }
    }
    
    /**
     * Clean up cancelled tasks to prevent memory accumulation
     */
    private void cleanupCancelledTasks() {
        if (activeTasks.size() > 100) { // Only clean up when we have many tasks
            activeTasks.removeIf(task -> task == null || task.isCancelled());
        }
    }

    /**
     * Unregister a completed task
     */
    public void unregisterTask(BukkitTask task) {
        activeTasks.remove(task);
    }

    /**
     * Cancel and clear all tracked tasks
     */
    public void cancelAllTasks() {
        // Cancel all tracked tasks
        for (BukkitTask task : activeTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        activeTasks.clear();

        // Also cancel any remaining tasks registered with Bukkit
        // This ensures we don't leave any tasks running
        plugin.getServer().getScheduler().cancelTasks(plugin);
    }

    /**
     * Get the number of active tasks
     */
    public int getActiveTaskCount() {
        // Clean up cancelled tasks
        activeTasks.removeIf(task -> task == null || task.isCancelled());
        return activeTasks.size();
    }

    /**
     * Run a BukkitRunnable as a timer and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @param delay delay before first execution (in ticks)
     * @param period period between executions (in ticks)
     * @return the created BukkitTask
     */
    public BukkitTask runTaskTimer(BukkitRunnable runnable, long delay, long period) {
        BukkitTask task = runnable.runTaskTimer(plugin, delay, period);
        registerTask(task);
        return task;
    }

    /**
     * Run a Runnable as a timer and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @param delay delay before first execution (in ticks)  
     * @param period period between executions (in ticks)
     * @return the created BukkitTask
     */
    public BukkitTask runTaskTimer(Runnable runnable, long delay, long period) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, delay, period);
        registerTask(task);
        return task;
    }

    /**
     * Run a BukkitRunnable once with a delay and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @param delay delay before execution (in ticks)
     * @return the created BukkitTask
     */
    public BukkitTask runTaskLater(BukkitRunnable runnable, long delay) {
        BukkitTask task = runnable.runTaskLater(plugin, delay);
        registerTask(task);
        return task;
    }

    /**
     * Run a Runnable once with a delay and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @param delay delay before execution (in ticks)
     * @return the created BukkitTask
     */
    public BukkitTask runTaskLater(Runnable runnable, long delay) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delay);
        registerTask(task);
        return task;
    }

    /**
     * Run a BukkitRunnable immediately and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @return the created BukkitTask
     */
    public BukkitTask runTask(BukkitRunnable runnable) {
        BukkitTask task = runnable.runTask(plugin);
        registerTask(task);
        return task;
    }

    /**
     * Run a Runnable immediately and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @return the created BukkitTask
     */
    public BukkitTask runTask(Runnable runnable) {
        BukkitTask task = plugin.getServer().getScheduler().runTask(plugin, runnable);
        registerTask(task);
        return task;
    }

    /**
     * Run a BukkitRunnable asynchronously as a timer and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @param delay delay before first execution (in ticks)
     * @param period period between executions (in ticks)
     * @return the created BukkitTask
     */
    public BukkitTask runTaskTimerAsynchronously(BukkitRunnable runnable, long delay, long period) {
        BukkitTask task = runnable.runTaskTimerAsynchronously(plugin, delay, period);
        registerTask(task);
        return task;
    }

    /**
     * Run a Runnable asynchronously as a timer and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @param delay delay before first execution (in ticks)  
     * @param period period between executions (in ticks)
     * @return the created BukkitTask
     */
    public BukkitTask runTaskTimerAsynchronously(Runnable runnable, long delay, long period) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
        registerTask(task);
        return task;
    }

    /**
     * Run a BukkitRunnable asynchronously once with a delay and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @param delay delay before execution (in ticks)
     * @return the created BukkitTask
     */
    public BukkitTask runTaskLaterAsynchronously(BukkitRunnable runnable, long delay) {
        BukkitTask task = runnable.runTaskLaterAsynchronously(plugin, delay);
        registerTask(task);
        return task;
    }

    /**
     * Run a Runnable asynchronously once with a delay and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @param delay delay before execution (in ticks)
     * @return the created BukkitTask
     */
    public BukkitTask runTaskLaterAsynchronously(Runnable runnable, long delay) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
        registerTask(task);
        return task;
    }

    /**
     * Run a BukkitRunnable asynchronously immediately and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @return the created BukkitTask
     */
    public BukkitTask runTaskAsynchronously(BukkitRunnable runnable) {
        BukkitTask task = runnable.runTaskAsynchronously(plugin);
        registerTask(task);
        return task;
    }

    /**
     * Run a Runnable asynchronously immediately and register it for tracking
     * 
     * @param runnable the runnable to execute
     * @return the created BukkitTask
     */
    public BukkitTask runTaskAsynchronously(Runnable runnable) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
        registerTask(task);
        return task;
    }
}





