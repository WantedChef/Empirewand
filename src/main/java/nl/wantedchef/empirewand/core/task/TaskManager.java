package nl.wantedchef.empirewand.core.task;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central task manager for tracking and cancelling all plugin tasks
 */
public class TaskManager {
    private final Plugin plugin;
    private final Set<BukkitTask> activeTasks = ConcurrentHashMap.newKeySet();

    public TaskManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register a task for tracking
     */
    public void registerTask(BukkitTask task) {
        if (task != null) {
            activeTasks.add(task);
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
        for (BukkitTask task : activeTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        activeTasks.clear();

        // Also cancel any remaining tasks registered with Bukkit
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
}





