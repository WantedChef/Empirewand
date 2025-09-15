package nl.wantedchef.empirewand.gui.util;

import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for building ItemStacks for GUIs with a fluent API.
 */
public final class ItemBuilder {
    private final ItemStack item;

    private ItemBuilder(@NotNull ItemStack item) {
        this.item = item.clone();
    }

    @NotNull
    public static ItemBuilder from(@NotNull ItemStack item) {
        return new ItemBuilder(item);
    }

    @NotNull
    public GuiItem asGuiItem(@NotNull java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> action) {
        return new GuiItem(item, action::accept);
    }

    @NotNull
    public GuiItem asGuiItem() {
        // Creates a GuiItem that does nothing on click (event is cancelled)
        return new GuiItem(item);
    }
}
