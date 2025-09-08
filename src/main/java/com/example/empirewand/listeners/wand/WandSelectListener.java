package com.example.empirewand.listeners.wand;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import com.example.empirewand.EmpireWandPlugin;

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
        org.bukkit.entity.Player p = event.getPlayer();

        // Check if player is still online
        if (!p.isOnline()) {
            return;
        }

        ItemStack item = p.getInventory().getItemInMainHand();
        if (!plugin.getWandService().isWand(item))
            return;

        List<String> spells = plugin.getWandService().getSpells(item);
        if (spells.isEmpty())
            return;

        int cur = plugin.getWandService().getActiveIndex(item);

        // Bounds check to prevent IndexOutOfBoundsException
        if (cur < 0 || cur >= spells.size()) {
            cur = 0;
        }

        cur = Math.max(0, Math.min(cur, spells.size() - 1));

        // Bepaal scrollrichting met wrap-around correctie voor de hotbar (9 slots)
        final int hotbarSlots = 9;
        int delta = (event.getNewSlot() - event.getPreviousSlot() + hotbarSlots) % hotbarSlots;
        if (delta == 0) {
            return; // Geen verandering
        }
        int dir = (delta <= hotbarSlots / 2) ? 1 : -1; // dichterbij: vooruit, anders achteruit
        int next = (cur + dir + spells.size()) % spells.size();

        String key = spells.get(next);

        // Validate spell exists
        if (plugin.getSpellRegistry().getSpell(key).isEmpty()) {
            plugin.getLogger().warning(String.format("Invalid spell '%s' in wand for player %s", key, p.getName()));
            return;
        }

        plugin.getWandService().setActiveIndex(item, next);
        String display = plugin.getSpellRegistry().getSpellDisplayName(key);

        // Only show message if player is still online
        if (p.isOnline()) {
            plugin.getFxService().actionBar(p, display);
        }
    }
}