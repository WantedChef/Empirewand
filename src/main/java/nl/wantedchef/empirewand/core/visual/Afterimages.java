package nl.wantedchef.empirewand.core.visual;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Global helper to manage a shared AfterimageManager instance with periodic
 * rendering.
 */
public final class Afterimages {
    private static AfterimageManager manager;
    private static BukkitTask renderTask;

    private Afterimages() {
    }

    public static void initialize(Plugin plugin, int maxSize, int lifetimeTicks, long periodTicks) {
        if (manager != null) {
            return; // already initialized
        }

        manager = new AfterimageManager(maxSize, lifetimeTicks);
        renderTask = new BukkitRunnable() {
            @Override
            public void run() {
                AfterimageManager m = manager; // local snapshot
                if (m != null) {
                    m.tickRender();
                }
            }
        }.runTaskTimer(plugin, periodTicks, periodTicks);
    }

    public static void record(Player player) {
        if (manager != null) {
            manager.record(player);
        }
    }

    public static void record(Location location) {
        if (manager != null) {
            manager.record(location);
        }
    }

    public static void clear() {
        if (manager != null) {
            manager.clear();
        }
    }

    // Remove the get() method that exposes the manager
    // public static AfterimageManager get() {
    //     return manager;
    // }

    /**
     * Shuts down the afterimage system and cleans up resources
     */
    public static void shutdown() {
        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }
        if (manager != null) {
            manager.clear();
            manager = null;
        }
    }
}





