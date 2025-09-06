package com.example.empirewand.listeners;

import com.example.empirewand.EmpireWandPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player lifecycle events such as quit to clean up per-player state.
 */
public class PlayerLifecycleListener implements Listener {

    private final EmpireWandPlugin plugin;

    public PlayerLifecycleListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clear all cooldowns for this player to prevent stale state
        var player = event.getPlayer();
        plugin.getCooldownService().clearAll(player.getUniqueId());
    }
}

