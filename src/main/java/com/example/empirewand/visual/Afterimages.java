package com.example.empirewand.visual;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Global helper to manage a shared AfterimageManager instance with periodic
 * rendering.
 */
public final class Afterimages {
    private static AfterimageManager manager;

    private Afterimages() {
    }

    public static void initialize(Plugin plugin, int maxSize, int lifetimeTicks, long periodTicks) {
        if (manager != null)
            return; // already
        manager = new AfterimageManager(maxSize, lifetimeTicks);
        new BukkitRunnable() {
            @Override
            public void run() {
                AfterimageManager m = manager; // local snapshot
                if (m != null) {
                    m.tickRender();
                }
            }
        }.runTaskTimer(plugin, periodTicks, periodTicks);
    }

    public static AfterimageManager get() {
        return manager;
    }
}
