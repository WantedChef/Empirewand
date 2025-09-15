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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wand-specific rules configuration menu. Allows players to configure individual
 * settings for a specific wand including difficulty, damage rules, and cooldown behavior.
 */
public class WandRulesMenu {

    // GUI Slot Layout
    private static final int DIFFICULTY_SLOT = 11;
    private static final int COOLDOWN_BLOCK_SLOT = 13;
    private static final int BLOCK_DAMAGE_SLOT = 15;
    private static final int PLAYER_DAMAGE_SLOT = 29;
    private static final int SAVE_SLOT = 31;
    private static final int CANCEL_SLOT = 33;
    private static final int BACK_SLOT = 40;
    private static final int GET_WAND_SLOT = 20;
    private static final int BIND_SPELLS_SLOT = 24;
    private static final int INFO_SLOT = 22;

    private final WandSettingsService wandSettingsService;
    private final WandSessionManager sessionManager;
    private final Logger logger;
    private final EmpireWandPlugin plugin;

    /**
     * Creates a new WandRulesMenu instance.
     *
     * @param wandSettingsService The service for managing wand settings
     * @param sessionManager The session manager for tracking user state
     * @param logger The logger for recording events
     * @param plugin The main plugin instance
     */
    public WandRulesMenu(@NotNull WandSettingsService wandSettingsService,
                        @NotNull WandSessionManager sessionManager,
                        @NotNull Logger logger,
                        @NotNull EmpireWandPlugin plugin) {
        this.wandSettingsService = Objects.requireNonNull(wandSettingsService, "WandSettingsService cannot be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "SessionManager cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
    }

    /**
     * Opens the wand rules menu for a specific wand.
     *
     * @param player The player to show the menu to
     * @param wandKey The wand key to configure
     */
    public void openMenu(@NotNull Player player, @NotNull String wandKey) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(wandKey, "Wand key cannot be null");

        try {
            // Get or create session
            WandSessionManager.WandSession session = sessionManager.getOrCreateSession(player);
            session.setCurrentWandKey(wandKey);

            // Get current settings (either pending changes or saved settings)
            WandSettings currentSettings = session.getPendingChanges(wandKey);
            if (currentSettings == null) {
                currentSettings = wandSettingsService.getWandSettings(wandKey);
            }

            // Create GUI
            Gui gui = createGui(player, wandKey, currentSettings, session);

                        // Open the GUI
                        gui.open(player);

                        // Play sound feedback
                        safePlay(player, Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f);

                        logger.log(Level.FINE, () -> "Opened wand rules menu for player: " + player.getName() + ", wand: " + wandKey);

        } catch (Exception e) {
                        logger.log(Level.SEVERE, () -> "Failed to open wand rules menu for player: " + player.getName() +
                                                 ", wand: " + wandKey + " - " + e.getMessage());
            player.sendMessage(Component.text("Failed to open wand configuration menu. Please try again.")
                    .color(NamedTextColor.RED));
        }
    }

    private Gui createGui(@NotNull Player player, @NotNull String wandKey, @NotNull WandSettings currentSettings,
                         @NotNull WandSessionManager.WandSession session) {

        String displayName = GuiUtil.formatWandDisplayName(wandKey);

        Gui gui = Gui.gui()
                .title(Component.text("Configure: " + displayName)
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD))
                .rows(5)
                .disableAllInteractions()
                .create();

        // Add setting items
        addDifficultyItem(gui, currentSettings, player, wandKey, session);
        addCooldownBlockItem(gui, currentSettings, player, wandKey, session);
        addBlockDamageItem(gui, currentSettings, player, wandKey, session);
        addPlayerDamageItem(gui, currentSettings, player, wandKey, session);

        // Add control items
        addInfoItem(gui, wandKey);
        addGetWandItem(gui, player, wandKey);
        addBindSpellsItem(gui, player, wandKey);
        addSaveItem(gui, player, wandKey, session);
        addCancelItem(gui, player, wandKey, session);
        addBackItem(gui, player);

        return gui;
    }

    private void addDifficultyItem(@NotNull Gui gui, @NotNull WandSettings settings, @NotNull Player player,
                                  @NotNull String wandKey, @NotNull WandSessionManager.WandSession session) {

        WandDifficulty difficulty = settings.getDifficulty();
        Material material = difficulty.getDisplayMaterial();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Difficulty: " + difficulty.getColoredDisplayName())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Current: " + difficulty.getColoredDisplayName())
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        // Add difficulty descriptions
        lore.add(Component.text("Difficulty Levels:")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text("• Easy: ")
                .color(NamedTextColor.GREEN)
                .append(Component.text("Reduced mana cost, increased effects")
                        .color(NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text("• Medium: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text("Balanced gameplay experience")
                        .color(NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text("• Hard: ")
                .color(NamedTextColor.RED)
                .append(Component.text("Higher costs, skill required")
                        .color(NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());
        lore.add(Component.text("Click to change difficulty")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.ITALIC)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(DIFFICULTY_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            // Cycle through difficulties
            WandDifficulty nextDifficulty = getNextDifficulty(difficulty);
            WandSettings newSettings = settings.toBuilder().difficulty(nextDifficulty).build();
            session.setPendingChanges(wandKey, newSettings);

            // Play sound
            safePlay(player, Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);

            // Refresh menu
            player.closeInventory();
            openMenu(player, wandKey);
        }));
    }

    private void addCooldownBlockItem(@NotNull Gui gui, @NotNull WandSettings settings, @NotNull Player player,
                                     @NotNull String wandKey, @NotNull WandSessionManager.WandSession session) {

        boolean enabled = settings.isCooldownBlock();
        Material material = enabled ? Material.REDSTONE_BLOCK : Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Cooldown Blocking: ")
                .color(NamedTextColor.WHITE)
                .append(Component.text(enabled ? "Enabled" : "Disabled")
                        .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("When enabled:")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Players cannot cast new spells")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("while any spell is on cooldown.")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Current: " + (enabled ? "Enabled" : "Disabled"))
                .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to toggle")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.ITALIC)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(COOLDOWN_BLOCK_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            // Toggle setting
            WandSettings newSettings = settings.toBuilder().cooldownBlock(!enabled).build();
            session.setPendingChanges(wandKey, newSettings);

            // Play sound
            safePlay(player, Sound.UI_BUTTON_CLICK, 0.8f, enabled ? 0.8f : 1.2f);

            // Refresh menu
            player.closeInventory();
            openMenu(player, wandKey);
        }));
    }

    private void addBlockDamageItem(@NotNull Gui gui, @NotNull WandSettings settings, @NotNull Player player,
                                   @NotNull String wandKey, @NotNull WandSessionManager.WandSession session) {

        boolean enabled = settings.isGriefBlockDamage();
        Material material = enabled ? Material.TNT : Material.OBSIDIAN;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Block Damage: ")
                .color(NamedTextColor.WHITE)
                .append(Component.text(enabled ? "Enabled" : "Disabled")
                        .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("When enabled:")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Spells can damage and modify")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("world blocks (griefing allowed).")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Current: " + (enabled ? "Enabled" : "Disabled"))
                .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to toggle")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.ITALIC)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(BLOCK_DAMAGE_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            // Toggle setting
            WandSettings newSettings = settings.toBuilder().griefBlockDamage(!enabled).build();
            session.setPendingChanges(wandKey, newSettings);

            // Play sound
            safePlay(player, Sound.UI_BUTTON_CLICK, 0.8f, enabled ? 0.8f : 1.2f);

            // Refresh menu
            player.closeInventory();
            openMenu(player, wandKey);
        }));
    }

    private void addPlayerDamageItem(@NotNull Gui gui, @NotNull WandSettings settings, @NotNull Player player,
                                    @NotNull String wandKey, @NotNull WandSessionManager.WandSession session) {

        boolean enabled = settings.isPlayerDamage();
        Material material = enabled ? Material.DIAMOND_SWORD : Material.SHIELD;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Player Damage: ")
                .color(NamedTextColor.WHITE)
                .append(Component.text(enabled ? "Enabled" : "Disabled")
                        .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("When enabled:")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Spells can damage other players")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("(PvP magic allowed).")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Current: " + (enabled ? "Enabled" : "Disabled"))
                .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to toggle")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.ITALIC)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(PLAYER_DAMAGE_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            // Toggle setting
            WandSettings newSettings = settings.toBuilder().playerDamage(!enabled).build();
            session.setPendingChanges(wandKey, newSettings);

            // Play sound
            safePlay(player, Sound.UI_BUTTON_CLICK, 0.8f, enabled ? 0.8f : 1.2f);

            // Refresh menu
            player.closeInventory();
            openMenu(player, wandKey);
        }));
    }

    private void addInfoItem(@NotNull Gui gui, @NotNull String wandKey) {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();

        String displayName = GuiUtil.formatWandDisplayName(wandKey);
        meta.displayName(Component.text(displayName + " Settings")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Configure this wand's behavior")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("and restrictions.")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Make your changes above,")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("then click Save to apply.")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(INFO_SLOT, ItemBuilder.from(item).asGuiItem());
    }

    private void addGetWandItem(@NotNull Gui gui, @NotNull Player player, @NotNull String wandKey) {
        Material material = switch (wandKey.toLowerCase()) {
            case "mephidantes_zeist" -> Material.NETHERITE_HOE;
            case "empirewand" -> Material.BLAZE_ROD;
            default -> Material.STICK;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Get " + GuiUtil.formatWandDisplayName(wandKey))
                .color(NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Click to receive this wand")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(GET_WAND_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            try {
                boolean success;
                if ("mephidantes_zeist".equalsIgnoreCase(wandKey)) {
                    success = plugin.getWandService().giveMephidantesZeist(player);
                } else {
                    success = plugin.getWandService().giveWand(player);
                }

                if (success) {
                    player.sendMessage(Component.text("You received a " + GuiUtil.formatWandDisplayName(wandKey) + "!")
                            .color(NamedTextColor.GREEN));
                    safePlay(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.2f);
                } else {
                    player.sendMessage(Component.text("Inventory full. Could not give the wand.")
                            .color(NamedTextColor.RED));
                    safePlay(player, Sound.BLOCK_ANVIL_LAND, 0.7f, 0.8f);
                }
            } catch (Exception ex) {
                logger.warning("Failed to give wand '" + wandKey + "' to player '" + player.getName() + "': " + ex.getMessage());
                player.sendMessage(Component.text("Failed to give wand. Please try again.")
                        .color(NamedTextColor.RED));
                safePlay(player, Sound.BLOCK_ANVIL_LAND, 0.7f, 0.8f);
            }
        }));
    }

    private void addBindSpellsItem(@NotNull Gui gui, @NotNull Player player, @NotNull String wandKey) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Bind Spells")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Open the spell binding menu")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Hold your wand to bind/unbind")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(BIND_SPELLS_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);
            safePlay(player, Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            player.closeInventory();
            new SpellBindingMenu(plugin, logger).openMenu(player, wandKey);
        }));
    }

    private void addSaveItem(@NotNull Gui gui, @NotNull Player player, @NotNull String wandKey,
                           @NotNull WandSessionManager.WandSession session) {

        boolean hasPendingChanges = session.hasPendingChanges(wandKey);
        Material material = hasPendingChanges ? Material.EMERALD_BLOCK : Material.GRAY_STAINED_GLASS;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Save Changes")
                .color(hasPendingChanges ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (hasPendingChanges) {
            lore.add(Component.text("You have unsaved changes!")
                    .color(NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click to save your changes")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, true));
        } else {
            lore.add(Component.text("No pending changes")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(SAVE_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            if (!hasPendingChanges) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
                return;
            }

            // Get pending changes and save them
            WandSettings pendingSettings = session.getPendingChanges(wandKey);
            if (pendingSettings != null) {
                wandSettingsService.updateWandSettings(pendingSettings).thenRun(() -> {
                    // Schedule UI operations on the main thread
                    plugin.getTaskManager().runTask(() -> {
                        // Clear pending changes
                        session.removePendingChanges(wandKey);

                        // Send success message
                        player.sendMessage(Component.text("Settings saved successfully for " + GuiUtil.formatWandDisplayName(wandKey))
                                .color(NamedTextColor.GREEN));

                        // Play success sound
                        safePlay(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

                        // Close menu
                        player.closeInventory();
                    });
                }).exceptionally(throwable -> {
                    // Schedule error handling on the main thread
                    plugin.getTaskManager().runTask(() -> {
                        player.sendMessage(Component.text("Failed to save settings: " + throwable.getMessage())
                                .color(NamedTextColor.RED));
                        safePlay(player, Sound.BLOCK_ANVIL_LAND, 0.7f, 0.5f);
                    });
                    return null;
                });
            }
        }));
    }

    private void addCancelItem(@NotNull Gui gui, @NotNull Player player, @NotNull String wandKey,
                             @NotNull WandSessionManager.WandSession session) {

        boolean hasPendingChanges = session.hasPendingChanges(wandKey);
        Material material = hasPendingChanges ? Material.RED_STAINED_GLASS : Material.GRAY_STAINED_GLASS;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Cancel Changes")
                .color(hasPendingChanges ? NamedTextColor.RED : NamedTextColor.GRAY)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (hasPendingChanges) {
            lore.add(Component.text("Discard all unsaved changes")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("and return to the original settings.")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click to cancel changes")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, true));
        } else {
            lore.add(Component.text("No changes to cancel")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(CANCEL_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            if (hasPendingChanges) {
                // Clear pending changes
                session.removePendingChanges(wandKey);

                // Send message
                player.sendMessage(Component.text("Changes cancelled for " + GuiUtil.formatWandDisplayName(wandKey))
                        .color(NamedTextColor.YELLOW));

                // Play sound
                safePlay(player, Sound.UI_BUTTON_CLICK, 0.7f, 0.8f);

                // Refresh menu to show original settings
                player.closeInventory();
                openMenu(player, wandKey);
            } else {
                                safePlay(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
            }
        }));
    }

    private void addBackItem(@NotNull Gui gui, @NotNull Player player) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Back to Wand Selector")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Return to the main wand")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("selection menu.")
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
            safePlay(player, Sound.UI_BUTTON_CLICK, 0.7f, 0.9f);

            // Close current menu and open wand selector
            player.closeInventory();
            new WandSelectorMenu(wandSettingsService, sessionManager, logger, plugin).openMenu(player);
        }));
    }

    private WandDifficulty getNextDifficulty(@NotNull WandDifficulty current) {
        return switch (current) {
            case EASY -> WandDifficulty.MEDIUM;
            case MEDIUM -> WandDifficulty.HARD;
            case HARD -> WandDifficulty.EASY;
        };
    }

    private void safePlay(@NotNull Player player, @NotNull Sound sound, float volume, float pitch) {
        var loc = player.getLocation();
        if (loc != null) {
            player.playSound(loc, sound, volume, pitch);
        }
    }
}