package nl.wantedchef.empirewand.listener.wand;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Voorkomt dat de wand naar/offhand wordt gewisseld.
 */
public final class WandSwapHandListener implements Listener {
    private final EmpireWandPlugin plugin;

    public WandSwapHandListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        ItemStack main = event.getMainHandItem();
        ItemStack off = event.getOffHandItem();
        boolean mainIsWand = main != null && plugin.getWandService().isWand(main);
        boolean offIsWand = off != null && plugin.getWandService().isWand(off);
        if (mainIsWand || offIsWand) {
            event.setCancelled(true);
            plugin.getFxService().showError(event.getPlayer(), "wand.swap-blocked");
        }
    }
}





