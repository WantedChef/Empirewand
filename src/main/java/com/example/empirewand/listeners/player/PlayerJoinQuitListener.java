package com.example.empirewand.listeners.player;

import com.example.empirewand.EmpireWandPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Houdt per-speler state netjes bij join/quit.
 */
public final class PlayerJoinQuitListener implements Listener {
    private final EmpireWandPlugin plugin;

    public PlayerJoinQuitListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Lichtgewicht: evt. toekomstige migratie/repair hooks.
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        plugin.getCooldownService().clearAll(player.getUniqueId());
    }
}