package nl.wantedchef.empirewand.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.config.WandSettingsService;
import nl.wantedchef.empirewand.core.config.model.WandSettings;
import nl.wantedchef.empirewand.gui.session.WandSessionManager;
import nl.wantedchef.empirewand.gui.util.GuiUtil;
import nl.wantedchef.empirewand.gui.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Main wand selector menu GUI. Displays all available wands in a paginated interface
 * and allows players to select a wand to configure its settings.
 */
public class WandSelectorMenu {

    private static final int WANDS_PER_PAGE = 45; // Leave space for navigation
    private static final int INFO_SLOT = 49;

    private final WandSettingsService wandSettingsService;
    private final WandSessionManager sessionManager;
    private final Logger logger;
    private final EmpireWandPlugin plugin;

    public WandSelectorMenu(@NotNull WandSettingsService wandSettingsService,
                           @NotNull WandSessionManager sessionManager,
                           @NotNull Logger logger,
                           @NotNull EmpireWandPlugin plugin) {
        this.wandSettingsService = Objects.requireNonNull(wandSettingsService, "WandSettingsService cannot be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "SessionManager cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
    }

    /**
     * Opens the wand selector menu for a player.
     *
     * @param player The player to show the menu to
     */
    public void openMenu(@NotNull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        try {
            // Get or create session
            WandSessionManager.WandSession session = sessionManager.getOrCreateSession(player);

            // Load all wand settings
            Map<String, WandSettings> allWandSettings = wandSettingsService.getAllWandSettings();
            Set<String> configuredWands = allWandSettings.keySet();

            // Add default wands if they're not configured
            addDefaultWandsIfMissing(allWandSettings, configuredWands);

            // Create paginated GUI
            PaginatedGui gui = createPaginatedGui(player, session);

            // Add wand items
            addWandItems(gui, allWandSettings);

            // Add navigation items
            addNavigationItems(gui);

            // Open the GUI
            gui.open(player);

            // Play sound feedback
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

            logger.fine("Opened wand selector menu for player: " + player.getName());

        } catch (Exception e) {
            logger.severe("Failed to open wand selector menu for player: " + player.getName() + " - " + e.getMessage());
            player.sendMessage(Component.text("Failed to open wand selector menu. Please try again.")
                    .color(NamedTextColor.RED));
        }
    }

    private PaginatedGui createPaginatedGui(@NotNull Player player, @NotNull WandSessionManager.WandSession session) {
        return Gui.paginated()
                .title(Component.text("Wand Settings - Select Wand")
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD))
                .rows(6)
                .pageSize(WANDS_PER_PAGE)
                .disableAllInteractions()
                .create();
    }

    private void addDefaultWandsIfMissing(@NotNull Map<String, WandSettings> allWandSettings, @NotNull Set<String> configuredWands) {
        // Add default wands if not already present
        String[] defaultWands = {"empirewand", "mephidantes_zeist"};

        for (String defaultWand : defaultWands) {
            if (!configuredWands.contains(defaultWand)) {
                WandSettings defaultSettings = wandSettingsService.getWandSettings(defaultWand);
                allWandSettings.put(defaultWand, defaultSettings);
            }
        }
    }

    private void addWandItems(@NotNull PaginatedGui gui, @NotNull Map<String, WandSettings> allWandSettings) {
        for (Map.Entry<String, WandSettings> entry : allWandSettings.entrySet()) {
            String wandKey = entry.getKey();
            WandSettings settings = entry.getValue();

            GuiItem wandItem = createWandItem(wandKey, settings);
            gui.addItem(wandItem);
        }
    }

    private GuiItem createWandItem(@NotNull String wandKey, @NotNull WandSettings settings) {
        // Determine display material based on wand type
        Material displayMaterial = getWandDisplayMaterial(wandKey);

        // Create item stack
        ItemStack item = new ItemStack(displayMaterial);
        ItemMeta meta = item.getItemMeta();

        // Set display name
        String displayName = GuiUtil.formatWandDisplayName(wandKey);
        meta.displayName(Component.text(displayName)
                .color(NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        // Create lore with current settings
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        lore.add(Component.text("Current Settings:")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        // Difficulty
        lore.add(Component.text("• Difficulty: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(settings.getDifficulty().getDisplayName())
                        .color(getDifficultyColor(settings.getDifficulty())))
                .decoration(TextDecoration.ITALIC, false));

        // Cooldown blocking
        lore.add(Component.text("• Cooldown Block: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(settings.isCooldownBlock() ? "Enabled" : "Disabled")
                        .color(settings.isCooldownBlock() ? NamedTextColor.GREEN : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));

        // Block damage
        lore.add(Component.text("• Block Damage: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(settings.isGriefBlockDamage() ? "Enabled" : "Disabled")
                        .color(settings.isGriefBlockDamage() ? NamedTextColor.GREEN : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));

        // Player damage
        lore.add(Component.text("• Player Damage: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(settings.isPlayerDamage() ? "Enabled" : "Disabled")
                        .color(settings.isPlayerDamage() ? NamedTextColor.GREEN : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());
        lore.add(Component.text("Click to configure this wand")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.ITALIC)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);

        // Create GUI item with click handler
        return ItemBuilder.from(item)
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    Player player = (Player) event.getWhoClicked();

                    // Update session with selected wand
                    WandSessionManager.WandSession session = sessionManager.getOrCreateSession(player);
                    session.setCurrentWandKey(wandKey);

                    // Play sound feedback
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);

                    // Close current GUI and open wand rules menu
                    player.closeInventory();
                    new WandRulesMenu(wandSettingsService, sessionManager, logger, plugin).openMenu(player, wandKey);
                });
    }

    private Material getWandDisplayMaterial(@NotNull String wandKey) {
        return switch (wandKey.toLowerCase()) {
            case "empirewand" -> Material.BLAZE_ROD;
            case "mephidantes_zeist" -> Material.GOLDEN_HOE;
            default -> Material.STICK;
        };
    }

    

    private NamedTextColor getDifficultyColor(@NotNull nl.wantedchef.empirewand.core.config.model.WandDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> NamedTextColor.GREEN;
            case MEDIUM -> NamedTextColor.YELLOW;
            case HARD -> NamedTextColor.RED;
        };
    }

    private void addNavigationItems(@NotNull PaginatedGui gui) {
        // Info item
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.displayName(Component.text("Wand Settings")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.empty());
        infoLore.add(Component.text("Configure individual wand settings")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        infoLore.add(Component.text("including difficulty, damage rules,")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        infoLore.add(Component.text("and cooldown behavior.")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        infoLore.add(Component.empty());
        infoLore.add(Component.text("Select a wand above to begin.")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        infoMeta.lore(infoLore);
        infoItem.setItemMeta(infoMeta);

        gui.setItem(INFO_SLOT, ItemBuilder.from(infoItem).asGuiItem());

        // Navigation items (previous/next page) are handled by the PaginatedGui automatically
    }

}