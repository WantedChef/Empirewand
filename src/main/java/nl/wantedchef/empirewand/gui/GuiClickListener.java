package nl.wantedchef.empirewand.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Listener for GUI inventory events to prevent item duplication and ensure
 * proper interaction handling for Wand Rules GUI system.
 */
public class GuiClickListener implements Listener {

    private final Logger logger;

    public GuiClickListener(@NotNull Logger logger) {
        this.logger = logger;
    }

    /**
     * Handles inventory click events in GUI inventories.
     * Prevents item manipulation in managed GUIs to avoid duplication.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }

        // Check if this is one of our GUI inventories
        if (isWandRulesGui(event.getView())) {
            // Let Triumph GUI handle the click, but prevent item movement
            preventItemManipulation(event);

            logger.fine("Prevented item manipulation in Wand Rules GUI for player: " + player.getName());
        }
    }

    /**
     * Handles inventory drag events in GUI inventories.
     * Prevents item dragging in managed GUIs.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Check if this is one of our GUI inventories
        if (isWandRulesGui(event.getView())) {
            event.setCancelled(true);
            logger.fine("Prevented inventory drag in Wand Rules GUI for player: " + player.getName());
        }
    }

    /**
     * Handles inventory move item events.
     * Prevents hoppers and other automation from interfering with GUIs.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(@NotNull InventoryMoveItemEvent event) {
        // Check if source or destination is one of our GUI inventories
        if (isWandRulesGui(event.getSource()) || isWandRulesGui(event.getDestination())) {
            event.setCancelled(true);
            logger.fine("Prevented automated item movement in Wand Rules GUI");
        }
    }

    /**
     * Handles inventory close events for cleanup and session management.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Check if this was one of our GUI inventories
        if (isWandRulesGui(event.getView())) {
            logger.fine("Wand Rules GUI closed for player: " + player.getName());

            // Note: Session cleanup is handled by the session manager's TTL system
            // We don't need to manually clean up here unless there are specific resources to release
        }
    }

    /**
     * Checks if an InventoryView represents a Wand Rules GUI.
     * Uses title-based detection to identify our GUIs.
     */
    private boolean isWandRulesGui(@NotNull InventoryView view) {
        String title = view.title().toString(); // Convert Component to string for checking

        return title.contains("Wand Settings") ||
               title.contains("Configure:") ||
               title.contains("Quick Config:") ||
               title.contains("Save Changes") ||
               title.contains("Discard Changes") ||
               title.contains("Reset to Defaults");
    }

    /**
     * Checks if an Inventory represents a Wand Rules GUI.
     * Used for inventory move item events.
     */
    private boolean isWandRulesGui(@NotNull Inventory inventory) {
        InventoryView view = inventory.getViewers().stream()
                .findFirst()
                .map(viewer -> viewer.getOpenInventory())
                .orElse(null);

        return view != null && isWandRulesGui(view);
    }

    /**
     * Prevents various forms of item manipulation in GUI inventories.
     */
    private void preventItemManipulation(@NotNull InventoryClickEvent event) {
        // Cancel the event to prevent the default behavior
        event.setCancelled(true);

        // Also prevent specific types of clicks that could bypass cancellation
        switch (event.getAction()) {
            case PICKUP_ALL:
            case PICKUP_SOME:
            case PICKUP_HALF:
            case PICKUP_ONE:
            case PLACE_ALL:
            case PLACE_SOME:
            case PLACE_ONE:
            case SWAP_WITH_CURSOR:
            case MOVE_TO_OTHER_INVENTORY:
            case HOTBAR_MOVE_AND_READD:
            case HOTBAR_SWAP:
            case CLONE_STACK:
            case COLLECT_TO_CURSOR:
            case DROP_ALL_CURSOR:
            case DROP_ONE_CURSOR:
            case DROP_ALL_SLOT:
            case DROP_ONE_SLOT:
                // All these actions should be cancelled
                event.setCancelled(true);
                break;
            case NOTHING:
                // This is fine, let it proceed
                break;
            default:
                // For any unknown actions, err on the side of caution
                event.setCancelled(true);
                break;
        }
    }
}