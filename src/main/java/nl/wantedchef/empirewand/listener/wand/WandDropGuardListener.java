package nl.wantedchef.empirewand.listener.wand;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Blokkeert droppen van de wand (Q / CTRL+Q).
 */
public final class WandDropGuardListener implements Listener {
    private final EmpireWandPlugin plugin;

    public WandDropGuardListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (stack != null && plugin.getWandService().isWand(stack)) {
            event.setCancelled(true);
            plugin.getFxService().showError(event.getPlayer(), "wand.drop-blocked");
        }
    }
}





