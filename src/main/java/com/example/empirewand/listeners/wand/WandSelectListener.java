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
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!plugin.getWandService().isWand(item))
            return;

        List<String> spells = plugin.getWandService().getSpells(item);
        if (spells == null || spells.isEmpty())
            return;

        int cur = Math.max(0, Math.min(plugin.getWandService().getActiveIndex(item), spells.size() - 1));
        int dir = event.getNewSlot() > event.getPreviousSlot() ? 1 : -1;
        int next = (cur + dir + spells.size()) % spells.size();

        plugin.getWandService().setActiveIndex(item, next);
        String key = spells.get(next);
        String display = plugin.getSpellRegistry().getSpellDisplayName(key);
        plugin.getFxService().actionBar(p, display != null ? display : key);
    }
}