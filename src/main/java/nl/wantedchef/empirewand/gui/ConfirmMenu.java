package nl.wantedchef.empirewand.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.wantedchef.empirewand.core.config.WandSettingsService;
import nl.wantedchef.empirewand.core.config.model.WandSettings;
import nl.wantedchef.empirewand.gui.session.WandSessionManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Confirmation menu for save/cancel operations and other destructive actions.
 * Provides a clear interface for users to confirm their intentions.
 */
public class ConfirmMenu {

    // GUI Slot Layout
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;
    private static final int INFO_SLOT = 13;

    private final WandSettingsService wandSettingsService;
    private final WandSessionManager sessionManager;
    private final Logger logger;

    public ConfirmMenu(@NotNull WandSettingsService wandSettingsService,
                      @NotNull WandSessionManager sessionManager,
                      @NotNull Logger logger) {
        this.wandSettingsService = Objects.requireNonNull(wandSettingsService, "WandSettingsService cannot be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "SessionManager cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
    }

    /**
     * Opens a save confirmation menu.
     *
     * @param player The player to show the menu to
     * @param wandKey The wand key being saved
     * @param pendingSettings The settings to be saved
     * @param onConfirm Callback when save is confirmed
     * @param onCancel Callback when save is cancelled
     */
    public void openSaveConfirmation(@NotNull Player player,
                                   @NotNull String wandKey,
                                   @NotNull WandSettings pendingSettings,
                                   @NotNull Runnable onConfirm,
                                   @NotNull Runnable onCancel) {
        openConfirmationMenu(
                player,
                "Save Changes?",
                "Save settings for " + formatWandDisplayName(wandKey),
                createSaveDetailsLore(pendingSettings),
                Material.EMERALD_BLOCK,
                NamedTextColor.GREEN,
                "Save",
                onConfirm,
                onCancel
        );
    }

    /**
     * Opens a discard changes confirmation menu.
     *
     * @param player The player to show the menu to
     * @param wandKey The wand key being modified
     * @param onConfirm Callback when discard is confirmed
     * @param onCancel Callback when discard is cancelled
     */
    public void openDiscardConfirmation(@NotNull Player player,
                                      @NotNull String wandKey,
                                      @NotNull Runnable onConfirm,
                                      @NotNull Runnable onCancel) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("All unsaved changes will be lost!")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("This action cannot be undone.")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        openConfirmationMenu(
                player,
                "Discard Changes?",
                "Discard changes for " + formatWandDisplayName(wandKey),
                lore,
                Material.RED_STAINED_GLASS,
                NamedTextColor.RED,
                "Discard",
                onConfirm,
                onCancel
        );
    }

    /**
     * Opens a reset to defaults confirmation menu.
     *
     * @param player The player to show the menu to
     * @param wandKey The wand key being reset
     * @param onConfirm Callback when reset is confirmed
     * @param onCancel Callback when reset is cancelled
     */
    public void openResetConfirmation(@NotNull Player player,
                                    @NotNull String wandKey,
                                    @NotNull Runnable onConfirm,
                                    @NotNull Runnable onCancel) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Reset all settings to defaults:")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        WandSettings defaults = wandSettingsService.getDefaultSettings();
        lore.add(Component.text("• Difficulty: " + defaults.getDifficulty().getColoredDisplayName())
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Cooldown Block: " + (defaults.isCooldownBlock() ? "Enabled" : "Disabled"))
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Block Damage: " + (defaults.isGriefBlockDamage() ? "Enabled" : "Disabled"))
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Player Damage: " + (defaults.isPlayerDamage() ? "Enabled" : "Disabled"))
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        openConfirmationMenu(
                player,
                "Reset to Defaults?",
                "Reset " + formatWandDisplayName(wandKey) + " to defaults",
                lore,
                Material.ORANGE_STAINED_GLASS,
                NamedTextColor.GOLD,
                "Reset",
                onConfirm,
                onCancel
        );
    }

    private void openConfirmationMenu(@NotNull Player player,
                                    @NotNull String title,
                                    @NotNull String description,
                                    @NotNull List<Component> detailLore,
                                    @NotNull Material confirmMaterial,
                                    @NotNull NamedTextColor confirmColor,
                                    @NotNull String confirmText,
                                    @NotNull Runnable onConfirm,
                                    @NotNull Runnable onCancel) {

        try {
            Gui gui = Gui.gui()
                    .title(Component.text(title)
                            .color(NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD))
                    .rows(3)
                    .disableAllInteractions()
                    .create();

            // Add confirm button
            addConfirmButton(gui, confirmMaterial, confirmColor, confirmText, player, onConfirm);

            // Add info item
            addInfoItem(gui, description, detailLore);

            // Add cancel button
            addCancelButton(gui, player, onCancel);

            // Open the GUI
            gui.open(player);

            // Play sound feedback
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 0.9f);

        } catch (Exception e) {
            logger.severe("Failed to open confirmation menu for player: " + player.getName() + " - " + e.getMessage());
            player.sendMessage(Component.text("Failed to open confirmation menu. Please try again.")
                    .color(NamedTextColor.RED));
        }
    }

    private void addConfirmButton(@NotNull Gui gui, @NotNull Material material, @NotNull NamedTextColor color,
                                @NotNull String actionText, @NotNull Player player, @NotNull Runnable onConfirm) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("✓ " + actionText)
                .color(color)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Click to confirm this action")
                .color(color)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(CONFIRM_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            // Play confirmation sound
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

            // Close menu and execute callback
            player.closeInventory();
            try {
                onConfirm.run();
            } catch (Exception e) {
                logger.severe("Error in confirmation callback: " + e.getMessage());
                player.sendMessage(Component.text("An error occurred while processing your request.")
                        .color(NamedTextColor.RED));
            }
        }));
    }

    private void addInfoItem(@NotNull Gui gui, @NotNull String description, @NotNull List<Component> detailLore) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(description)
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.addAll(detailLore);

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(INFO_SLOT, ItemBuilder.from(item).asGuiItem());
    }

    private void addCancelButton(@NotNull Gui gui, @NotNull Player player, @NotNull Runnable onCancel) {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("✗ Cancel")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Click to cancel and go back")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);

        gui.setItem(CANCEL_SLOT, ItemBuilder.from(item).asGuiItem(event -> {
            event.setCancelled(true);

            // Play cancel sound
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.8f);

            // Close menu and execute callback
            player.closeInventory();
            try {
                onCancel.run();
            } catch (Exception e) {
                logger.severe("Error in cancel callback: " + e.getMessage());
                player.sendMessage(Component.text("An error occurred while canceling.")
                        .color(NamedTextColor.RED));
            }
        }));
    }

    private List<Component> createSaveDetailsLore(@NotNull WandSettings settings) {
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text("Settings to be saved:")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        // Difficulty
        lore.add(Component.text("• Difficulty: " + settings.getDifficulty().getColoredDisplayName())
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

        return lore;
    }

    private String formatWandDisplayName(@NotNull String wandKey) {
        // Convert snake_case to Title Case
        String[] parts = wandKey.split("_");
        StringBuilder displayName = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                displayName.append(" ");
            }
            String part = parts[i];
            displayName.append(part.substring(0, 1).toUpperCase())
                      .append(part.substring(1).toLowerCase());
        }

        return displayName.toString();
    }

    /**
     * Convenience method for saving settings with confirmation.
     *
     * @param player The player
     * @param wandKey The wand key
     * @param settings The settings to save
     * @param onSuccess Callback when save succeeds
     * @param onFailure Callback when save fails
     */
    public void saveWithConfirmation(@NotNull Player player,
                                   @NotNull String wandKey,
                                   @NotNull WandSettings settings,
                                   @NotNull Runnable onSuccess,
                                   @NotNull Consumer<Throwable> onFailure) {

        openSaveConfirmation(player, wandKey, settings, () -> {
            // Perform the save operation
            wandSettingsService.updateWandSettings(settings)
                    .thenRun(() -> {
                        // Clear pending changes
                        WandSessionManager.WandSession session = sessionManager.getSession(player);
                        if (session != null) {
                            session.removePendingChanges(wandKey);
                        }

                        // Send success message
                        player.sendMessage(Component.text("Settings saved successfully for " + formatWandDisplayName(wandKey))
                                .color(NamedTextColor.GREEN));

                        // Play success sound
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

                        // Execute success callback
                        onSuccess.run();
                    })
                    .exceptionally(throwable -> {
                        logger.severe("Failed to save wand settings: " + throwable.getMessage());

                        // Send failure message
                        player.sendMessage(Component.text("Failed to save settings: " + throwable.getMessage())
                                .color(NamedTextColor.RED));

                        // Play failure sound
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 0.5f);

                        // Execute failure callback
                        onFailure.accept(throwable);

                        return null;
                    });
        }, () -> {
            // User cancelled save, return to wand rules menu
            new WandRulesMenu(wandSettingsService, sessionManager, logger).openMenu(player, wandKey);
        });
    }

    /**
     * Utility class for building ItemStacks with fluent API.
     */
    private static class ItemBuilder {
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
            return new GuiItem(item);
        }
    }
}