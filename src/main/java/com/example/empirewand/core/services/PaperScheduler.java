package com.example.empirewand.core.services;

import com.example.empirewand.api.Scheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Paper-based implementation of the Scheduler facade.
 * Wraps Bukkit's scheduler for thread-safe task execution.
 *
 * @since 2.0.0
 */
public final class PaperScheduler implements Scheduler {
    private final JavaPlugin plugin;

    public PaperScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runMain(@NotNull Runnable runnable) {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, runnable);
        }
    }

    @Override
    public void runAsync(@NotNull Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }
}