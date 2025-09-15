package nl.wantedchef.empirewand.gui;

import dev.triumphteam.gui.guis.BaseGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Listener for GUI inventory events to prevent item duplication and ensure
 * proper interaction handling for the GUI system.
 * This acts as a safeguard for GUIs created with the Triumph GUI library.
 */
public class GuiClickListener implements Listener {

    private final Logger logger;

    public GuiClickListener(@NotNull Logger logger) {
        this.logger = logger;
    }

    /**
     * Handles inventory click events in GUI inventories.
     * Prevents any item manipulation as a safeguard.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // Check if the click is within a Triumph GUI.
        if (isTriumphGui(event.getView().getTopInventory())) {
            // The Triumph GUI library handles its own clicks.
            // This listener acts as a high-priority safeguard to unconditionally
            // cancel the event, preventing any potential item manipulation
            // that might slip through.
            event.setCancelled(true);
        }
    }

    /**
     * Handles inventory drag events in GUI inventories.
     * Prevents item dragging in our GUIs.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // Check if the drag is within a Triumph GUI.
        if (isTriumphGui(event.getInventory())) {
            event.setCancelled(true);
            logger.fine("Prevented inventory drag in GUI for player: " + event.getWhoClicked().getName());
        }
    }

    /**
     * Handles inventory move item events (e.g., hoppers).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(@NotNull InventoryMoveItemEvent event) {
        if (isTriumphGui(event.getSource()) || isTriumphGui(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    /**
     * Handles inventory close events for logging.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        if (isTriumphGui(event.getInventory())) {
            logger.fine("GUI closed for player: " + event.getPlayer().getName());
            // Session cleanup is handled by WandSessionManager's TTL.
        }
    }

    /**
     * Checks if an Inventory is a GUI created by the Triumph GUI library.
     *
     * @param inventory The inventory to check.
     * @return True if it's a Triumph GUI, false otherwise.
     */
    private boolean isTriumphGui(@NotNull Inventory inventory) {
        return inventory.getHolder() instanceof BaseGui;
    }
}
