package nl.wantedchef.empirewand.listener.wand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import nl.wantedchef.empirewand.EmpireWandPlugin;

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

        // We allow scrolling to switch spells while keeping the wand selected.
        // Detect if the wand is in the previously held slot or currently in hand.
        ItemStack prevItem = p.getInventory().getItem(event.getPreviousSlot());
        ItemStack handItem = p.getInventory().getItemInMainHand();

        boolean prevIsWand = plugin.getWandService().isWand(prevItem);
        boolean handIsWand = plugin.getWandService().isWand(handItem);

        // If neither previous nor current hand item is a wand, ignore.
        if (!prevIsWand && !handIsWand)
            return;

        // Prefer the wand from the previous slot to maintain selection stability while scrolling.
        ItemStack wandItem = prevIsWand ? prevItem : handItem;

        List<String> spells = new ArrayList<>(plugin.getWandService().getSpells(wandItem));
        if (spells.isEmpty())
            return;

        int cur = plugin.getWandService().getActiveIndex(wandItem);

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

        // Cancel the hotbar slot change when we are using the wand to scroll spells,
        // so the wand stays selected in the same slot.
        if (prevIsWand) {
            event.setCancelled(true);
            // Ensure the held item slot remains the previous one visually.
            p.getInventory().setHeldItemSlot(event.getPreviousSlot());
        }

        plugin.getWandService().setActiveIndex(wandItem, next);
        String display = plugin.getSpellRegistry().getSpellDisplayName(key);

        // Only show message if player is still online
        if (p.isOnline()) {
            Map<String, String> params = Map.of("spell", display);
            plugin.getFxService().actionBarKey(p, "spell-selected", params);
            plugin.getFxService().playUISound(p, "select");
        }
    }
}
