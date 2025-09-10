package nl.wantedchef.empirewand.core.task;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public final class TaskManager {
    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    
    public TaskManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
    }
    
    public BukkitTask runTask(@NotNull Runnable task) {
        return scheduler.runTask(plugin, task);
    }
    
    public BukkitTask runTaskLater(@NotNull Runnable task, long delay) {
        return scheduler.runTaskLater(plugin, task, delay);
    }
    
    public BukkitTask runTaskTimer(@NotNull Runnable task, long delay, long period) {
        return scheduler.runTaskTimer(plugin, task, delay, period);
    }
    
    public BukkitTask runTaskAsynchronously(@NotNull Runnable task) {
        return scheduler.runTaskAsynchronously(plugin, task);
    }
    
    public void cancelAllTasks() {
        scheduler.cancelTasks(plugin);
    }
}
