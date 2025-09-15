package nl.wantedchef.empirewand.gui;

import dev.triumphteam.gui.guis.Gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.config.WandSettingsService;
import nl.wantedchef.empirewand.core.config.model.WandDifficulty;
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
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Category selection menu for quick configuration of specific setting categories.
 * Allows players to quickly configure all settings within a category.
 */
public class CategorySelectionMenu {

    // GUI Slot Layout
    private static final int DIFFICULTY_CATEGORY_SLOT = 11;
    private static final int DAMAGE_CATEGORY_SLOT = 13;
    private static final int COOLDOWN_CATEGORY_SLOT = 15;
    private static final int BACK_SLOT = 31;

    private final WandSettingsService wandSettingsService;
    private final WandSessionManager sessionManager;
    private final Logger logger;
    private final EmpireWandPlugin plugin;

    public CategorySelectionMenu(@NotNull WandSettingsService wandSettingsService,
                                @NotNull WandSessionManager sessionManager,
                                @NotNull Logger logger,
                                @NotNull EmpireWandPlugin plugin) {
        this.wandSettingsService = Objects.requireNonNull(wandSettingsService, "WandSettingsService cannot be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "SessionManager cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
    }

    /**
     * Opens the category selection menu.
     *
     * @param player The player to show the menu to
     * @param wandKey The wand key being configured
     */
    public void openMenu(@NotNull Player player, @NotNull String wandKey) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(wandKey, "Wand key cannot be null");

        try {
            // Get or create session
            WandSessionManager.WandSession session = sessionManager.getOrCreateSession(player);
            session.setCurrentWandKey(wandKey);

            // Create GUI
            Gui gui = createGui(player, wandKey, session);

            // Open the GUI
            gui.open(player);

            // Play sound feedback
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.1f);

            logger.fine("Opened category selection menu for player: " + player.getName() + ", wand: " + wandKey);

        } catch (Exception e) {
            logger.severe("Failed to open category selection menu for player: " + player.getName() +
                         ", wand: " + wandKey + " - " + e.getMessage());
            player.sendMessage(Component.text("Failed to open category menu. Please try again.")
                    .color(NamedTextColor.RED));
        }
    }

    private Gui createGui(@NotNull Player player, @NotNull String wandKey, @NotNull WandSessionManager.WandSession session) {
        String displayName = GuiUtil.formatWandDisplayName(wandKey);

        Gui gui = Gui.gui()
                .title(Component.text("Quick Config: " + displayName)
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD))
                .rows(4)
                .disableAllInteractions()
                .create();

        // Add category items
        addDifficultyCategory(gui, player, wandKey, session);
        addDamageCategory(gui, player, wandKey, session);
        addCooldownCategory(gui, player, wandKey, session);

        // Add back button
        addBackItem(gui, player, wandKey);

        return gui;
    }

    private void addDifficultyCategory(@NotNull Gui gui, @NotNull Player player, @NotNull String wandKey,
                                     @NotNull WandSessionManager.WandSession session) {

        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Difficulty Settings")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Quick difficulty configuration")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        // Show available options
        lore.add(Component.text("Available Options:")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Left Click: Easy Mode")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Right Click: Medium Mode")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Shift+Click: Hard Mode")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(DIFFICULTY_CATEGORY_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            WandDifficulty targetDifficulty;
            String actionName;

            if (event.isShiftClick()) {
                targetDifficulty = WandDifficulty.HARD;
                actionName = "Hard Mode";
            } else if (event.isRightClick()) {
                targetDifficulty = WandDifficulty.MEDIUM;
                actionName = "Medium Mode";
            } else {
                targetDifficulty = WandDifficulty.EASY;
                actionName = "Easy Mode";
            }

            // Get current settings and update difficulty
            WandSettings currentSettings = getCurrentSettings(wandKey, session);
            WandSettings newSettings = currentSettings.toBuilder().difficulty(targetDifficulty).build();
            session.setPendingChanges(wandKey, newSettings);

            // Send feedback
            player.sendMessage(Component.text("Set difficulty to " + actionName + " for " + GuiUtil.formatWandDisplayName(wandKey))
                    .color(targetDifficulty == WandDifficulty.EASY ? NamedTextColor.GREEN :
                           targetDifficulty == WandDifficulty.MEDIUM ? NamedTextColor.YELLOW : NamedTextColor.RED));

            // Play sound
            float pitch = targetDifficulty == WandDifficulty.EASY ? 1.2f :
                         targetDifficulty == WandDifficulty.MEDIUM ? 1.0f : 0.8f;
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, pitch);

            // Close menu and return to main wand rules
            player.closeInventory();
            new WandRulesMenu(wandSettingsService, sessionManager, logger, plugin).openMenu(player, wandKey);
        }));
    }

    private void addDamageCategory(@NotNull Gui gui, @NotNull Player player, @NotNull String wandKey,
                                 @NotNull WandSessionManager.WandSession session) {

        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Damage Settings")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Quick damage configuration")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        lore.add(Component.text("Available Options:")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Left Click: Enable All Damage")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Right Click: Disable All Damage")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Shift+Left: PvP Only")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Shift+Right: Environment Only")
                .color(NamedTextColor.BLUE)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(DAMAGE_CATEGORY_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            WandSettings currentSettings = getCurrentSettings(wandKey, session);
            WandSettings.Builder builder = currentSettings.toBuilder();
            String actionName;

            if (event.isShiftClick()) {
                if (event.isLeftClick()) {
                    // PvP Only: Enable player damage, disable block damage
                    builder.playerDamage(true).griefBlockDamage(false);
                    actionName = "PvP Only Mode";
                } else {
                    // Environment Only: Disable player damage, enable block damage
                    builder.playerDamage(false).griefBlockDamage(true);
                    actionName = "Environment Only Mode";
                }
            } else {
                if (event.isLeftClick()) {
                    // Enable all damage
                    builder.playerDamage(true).griefBlockDamage(true);
                    actionName = "All Damage Enabled";
                } else {
                    // Disable all damage
                    builder.playerDamage(false).griefBlockDamage(false);
                    actionName = "All Damage Disabled";
                }
            }

            WandSettings newSettings = builder.build();
            session.setPendingChanges(wandKey, newSettings);

            // Send feedback
            NamedTextColor messageColor = actionName.contains("Disabled") ? NamedTextColor.RED : NamedTextColor.GREEN;
            player.sendMessage(Component.text("Set damage to " + actionName + " for " + GuiUtil.formatWandDisplayName(wandKey))
                    .color(messageColor));

            // Play sound
            float pitch = actionName.contains("Disabled") ? 0.7f : 1.3f;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, pitch);

            // Close menu and return to main wand rules
            player.closeInventory();
            new WandRulesMenu(wandSettingsService, sessionManager, logger, plugin).openMenu(player, wandKey);
        }));
    }

    private void addCooldownCategory(@NotNull Gui gui, @NotNull Player player, @NotNull String wandKey,
                                   @NotNull WandSessionManager.WandSession session) {

        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Cooldown Settings")
                .color(NamedTextColor.BLUE)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Quick cooldown configuration")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        lore.add(Component.text("Available Options:")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Left Click: Enable Cooldown Block")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Right Click: Disable Cooldown Block")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(COOLDOWN_CATEGORY_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            WandSettings currentSettings = getCurrentSettings(wandKey, session);
            boolean enableCooldownBlock = event.isLeftClick();
            String actionName = enableCooldownBlock ? "Enabled" : "Disabled";

            WandSettings newSettings = currentSettings.toBuilder()
                    .cooldownBlock(enableCooldownBlock)
                    .build();
            session.setPendingChanges(wandKey, newSettings);

            // Send feedback
            NamedTextColor messageColor = enableCooldownBlock ? NamedTextColor.GREEN : NamedTextColor.RED;
            player.sendMessage(Component.text("Cooldown blocking " + actionName + " for " + GuiUtil.formatWandDisplayName(wandKey))
                    .color(messageColor));

            // Play sound
            float pitch = enableCooldownBlock ? 1.2f : 0.8f;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, pitch);

            // Close menu and return to main wand rules
            player.closeInventory();
            new WandRulesMenu(wandSettingsService, sessionManager, logger, plugin).openMenu(player, wandKey);
        }));
    }

    private void addBackItem(@NotNull Gui gui, @NotNull Player player, @NotNull String wandKey) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Back to Wand Settings")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Return to detailed wand")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("configuration menu.")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to go back")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(BACK_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            // Play sound
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.9f);

            // Close current menu and open wand rules
            player.closeInventory();
            new WandRulesMenu(wandSettingsService, sessionManager, logger, plugin).openMenu(player, wandKey);
        }));
    }

    private WandSettings getCurrentSettings(@NotNull String wandKey, @NotNull WandSessionManager.WandSession session) {
        // Check for pending changes first, fallback to saved settings
        WandSettings pendingSettings = session.getPendingChanges(wandKey);
        return pendingSettings != null ? pendingSettings : wandSettingsService.getWandSettings(wandKey);
    }

    

}