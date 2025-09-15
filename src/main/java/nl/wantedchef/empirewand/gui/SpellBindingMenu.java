package nl.wantedchef.empirewand.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.api.spell.SpellMetadata;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.gui.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Spell binding GUI. Shows all spells and lets players bind/unbind to the held wand.
 */
public class SpellBindingMenu {

    private static final int ROWS = 6;
    private static final int PAGE_SIZE = 45;
    private static final int INFO_SLOT = 49;

    private final EmpireWandPlugin plugin;
    private final Logger logger;

    public SpellBindingMenu(@NotNull EmpireWandPlugin plugin, @NotNull Logger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void openMenu(@NotNull Player player, @NotNull String wandKey) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(wandKey, "wandKey");

        try {
            SpellRegistry registry = plugin.getSpellRegistry();
            WandService wandService = plugin.getWandService();

            // Validate held wand
            ItemStack held = wandService.getHeldWand(player);
            if (held == null) {
                player.sendMessage(Component.text("Hold a wand to bind spells.").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
                return;
            }

            PaginatedGui gui = Gui.paginated()
                    .title(Component.text("Bind Spells")
                            .color(NamedTextColor.LIGHT_PURPLE)
                            .decorate(TextDecoration.BOLD))
                    .rows(ROWS)
                    .pageSize(PAGE_SIZE)
                    .disableAllInteractions()
                    .create();

            // Build current bindings set for quick lookup
            List<String> bound = new ArrayList<>(wandService.getSpells(held));

            // Add spell items
            addSpellItems(gui, registry, bound, player, held);

            // Info item
            addInfoItem(gui, wandKey);

            gui.open(player);
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.05f);
        } catch (Exception e) {
            logger.severe("Failed to open SpellBindingMenu: " + e.getMessage());
            player.sendMessage(Component.text("Failed to open binding menu. Try again.")
                    .color(NamedTextColor.RED));
        }
    }

    private void addSpellItems(@NotNull PaginatedGui gui,
                               @NotNull SpellRegistry registry,
                               @NotNull List<String> bound,
                               @NotNull Player player,
                               @NotNull ItemStack heldWand) {

        // Sort by display name for UX
        Map<String, nl.wantedchef.empirewand.spell.Spell<?>> all = registry.getAllSpells();
        List<String> keys = new ArrayList<>(all.keySet());
        keys.sort(Comparator.comparing(k -> safeDisplayName(registry, k)));

        for (String key : keys) {
            boolean isBound = bound.contains(key);
            ItemStack item = createSpellIcon(registry, key, isBound);

            gui.addItem(ItemBuilder.from(item).asGuiItem((InventoryClickEvent event) -> {
                event.setCancelled(true);

                try {
                    boolean result;
                    if (isBound) {
                        result = plugin.getWandService().unbindSpell(heldWand, key);
                    } else {
                        // Permission check for binding
                        if (!plugin.getPermissionService().canBindSpell(player, key)) {
                            player.sendMessage(Component.text("No permission to bind " + safeDisplayName(registry, key))
                                    .color(NamedTextColor.RED));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.6f);
                            return;
                        }
                        result = plugin.getWandService().bindSpell(heldWand, key);
                    }

                    if (result) {
                        if (isBound) {
                            player.sendMessage(Component.text("Unbound " + safeDisplayName(registry, key))
                                    .color(NamedTextColor.YELLOW));
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.9f);
                        } else {
                            player.sendMessage(Component.text("Bound " + safeDisplayName(registry, key))
                                    .color(NamedTextColor.GREEN));
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.2f);
                        }
                        // Refresh view by reopening
                        player.closeInventory();
                        openMenu(player, plugin.getWandService().isMephidantesZeist(heldWand) ? "mephidantes_zeist" : "empirewand");
                    } else {
                        player.sendMessage(Component.text("No change for " + safeDisplayName(registry, key))
                                .color(NamedTextColor.GRAY));
                    }
                } catch (Exception ex) {
                    player.sendMessage(Component.text("Failed: " + ex.getMessage())
                            .color(NamedTextColor.RED));
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 0.7f);
                }
            }));
        }
    }

    private ItemStack createSpellIcon(@NotNull SpellRegistry registry, @NotNull String key, boolean bound) {
        Material material = Material.BOOK;
        try {
            Optional<SpellMetadata> metaOpt = registry.getSpellMetadata(key);
            if (metaOpt.isPresent()) {
                String icon = metaOpt.get().getIconMaterial();
                Material m = Material.matchMaterial(icon.toUpperCase());
                if (m != null) {
                    material = m;
                }
            }
        } catch (Exception ignored) { }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String display = safeDisplayName(registry, key);
        meta.displayName(Component.text(display)
                .color(bound ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, bound)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>(3); // Pre-size for 3 elements
        lore.add(Component.empty());
        lore.add(Component.text(bound ? "Currently bound" : "Not bound")
                .color(bound ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(bound ? "Click to unbind" : "Click to bind")
                .color(bound ? NamedTextColor.YELLOW : NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String safeDisplayName(@NotNull SpellRegistry registry, @NotNull String key) {
        try {
            return registry.getSpellDisplayName(key);
        } catch (Exception e) {
            return key;
        }
    }

    private void addInfoItem(@NotNull PaginatedGui gui, @NotNull String wandKey) {
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text("Binding: " + wandKey)
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>(4); // Pre-size for 4 elements
        lore.add(Component.empty());
        lore.add(Component.text("- Green = bound, White = not bound")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("- Click to toggle binding")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("- Hold your wand while binding")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        info.setItemMeta(meta);

        gui.setItem(INFO_SLOT, ItemBuilder.from(info).asGuiItem());
    }
}


