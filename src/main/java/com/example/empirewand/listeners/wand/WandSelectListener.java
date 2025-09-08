package com.example.empirewand.listeners.wand;

import com.example.empirewand.EmpireWandPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Scrollen door spells via muiswiel.
 */
public final class WandSelectListener implements Listener {
    private final EmpireWandPlugin plugin;

    public WandSelectListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHotbarChange(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();

        // Check if player is still online
        if (!p.isOnline()) {
            return;
        }

        ItemStack item = p.getInventory().getItemInMainHand();
        if (!plugin.getWandService().isWand(item))
            return;

        List<String> spells = plugin.getWandService().getSpells(item);
        if (spells == null || spells.isEmpty())
            return;

        int cur = plugin.getWandService().getActiveIndex(item);

        // Bounds check to prevent IndexOutOfBoundsException
        if (cur < 0 || cur >= spells.size()) {
            cur = 0;
        }

        cur = Math.max(0, Math.min(cur, spells.size() - 1));
        int dir = event.getNewSlot() > event.getPreviousSlot() ? 1 : -1;
        int next = (cur + dir + spells.size()) % spells.size();

        // Validate next index as well
        if (next >= spells.size()) {
            next = 0;
        }

        String key = spells.get(next);

        // Validate spell exists
        if (plugin.getSpellRegistry().getSpell(key).isEmpty()) {
            plugin.getLogger().warning("Invalid spell '" + key + "' in wand for player " + p.getName());
            return;
        }

        plugin.getWandService().setActiveIndex(item, next);
        String display = plugin.getSpellRegistry().getSpellDisplayName(key);

        // Only show message if player is still online
        if (p.isOnline()) {
            plugin.getFxService().actionBar(p, display != null ? display : key);
        }
    }
}