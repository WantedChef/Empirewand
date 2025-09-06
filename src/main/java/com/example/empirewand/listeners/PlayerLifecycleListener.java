package com.example.empirewand.listeners;

import com.example.empirewand.EmpireWandPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player lifecycle events such as quit to clean up per-player state.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {
        "EI_EXPOSE_REP2" }, justification = "Listener stores plugin reference to access services; standard Bukkit lifecycle pattern.")
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
