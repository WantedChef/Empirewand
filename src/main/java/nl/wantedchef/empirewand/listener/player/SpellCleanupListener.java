package nl.wantedchef.empirewand.listener.player;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.SpellManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Listener for managing toggleable spell cleanup when players disconnect,
 * die, or change worlds.
 */
public class SpellCleanupListener implements Listener {

    private final Plugin plugin;

    public SpellCleanupListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayerSpells(event.getPlayer(), "quit");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerKick(PlayerKickEvent event) {
        cleanupPlayerSpells(event.getPlayer(), "kick");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        cleanupPlayerSpells(event.getEntity(), "death");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // Optional: deactivate spells when changing worlds
        // This can be configured based on server needs
        cleanupPlayerSpells(event.getPlayer(), "world-change");
    }

    private void cleanupPlayerSpells(Player player, String reason) {
        try {
            SpellManager spellManager = EmpireWandAPI.getService(SpellManager.class);
            int activeSpellCount = spellManager.getActiveSpellCount(player);

            if (activeSpellCount > 0) {
                plugin.getLogger().log(Level.FINE,
                        "Cleaning up " + activeSpellCount + " active spells for player " +
                                player.getName() + " (reason: " + reason + ")");

                spellManager.deactivateAllSpells(player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error cleaning up spells for player " + player.getName() +
                            " on " + reason,
                    e);
        }
    }
}





