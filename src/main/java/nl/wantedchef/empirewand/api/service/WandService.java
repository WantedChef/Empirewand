package nl.wantedchef.empirewand.api.service;

import nl.wantedchef.empirewand.api.EmpireWandService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Advanced API interface for managing EmpireWand wands.
 *
 * <p>
 * This interface provides comprehensive wand management capabilities including
 * creation, customization, templating, statistics, and advanced operations.
 * External plugins can use this to integrate advanced wand functionality.
 * </p>
 *
 * <p>
 * <b>API Stability:</b> Stable - Follows semantic versioning
 * </p>
 *
 * <p>
 * <b>Usage Examples:</b>
 * 
 * <pre>{@code
 * // Get services
 * WandService wandService = EmpireWandAPI.getService(WandService.class);
 *
 * // Create a custom wand
 * ItemStack customWand = wandService.createWand()
 *         .material(Material.STICK)
 *         .name(Component.text("My Wand"))
 *         .spells("magic-missile", "heal")
 *         .build();
 *
 * // Use a template
 * WandTemplate template = wandService.getTemplate("fire_wand");
 * ItemStack templatedWand = wandService.createWand(template);
 *
 * // Customize existing wand
 * wandService.getCustomizer(wand)
 *         .addLore(Component.text("Custom enchantment"))
 *         .addEnchantment(Enchantment.DURABILITY, 3)
 *         .apply();
 *
 * // Get statistics
 * WandStatistics stats = wandService.getStatistics(wand);
 * 
 * }</pre>
 *
 * @since 2.0.0
 */
public interface WandService extends EmpireWandService {

    // ===== EXISTING METHODS (ENHANCED) =====

    

    /**
     * Checks if an ItemStack is an EmpireWand.
     *
     * @param item the item to check
     * @return true if the item is a wand, false otherwise
     */
    boolean isWand(@Nullable ItemStack item);

    /**
     * Gets the spells bound to a wand.
     *
     * @param wand the wand ItemStack
     * @return a list of spell keys bound to the wand
     */
    @NotNull
    List<String> getBoundSpells(@NotNull ItemStack wand);

    /**
     * Gets the spells bound to a wand (alias for getBoundSpells).
     *
     * @param item the wand ItemStack
     * @return a list of spell keys bound to the wand
     */
    @NotNull
    List<String> getSpells(@NotNull ItemStack item);

    /**
     * Sets all spells bound to a wand, replacing any existing spells.
     *
     * @param wand      the wand ItemStack
     * @param spellKeys the list of spell keys to bind
     */
    void setSpells(@NotNull ItemStack wand, @NotNull List<String> spellKeys);

    /**
     * Gets the active spell index for a wand.
     *
     * @param wand the wand ItemStack
     * @return the active spell index
     */
    int getActiveIndex(@NotNull ItemStack wand);

    /**
     * Sets the active spell index for a wand.
     *
     * @param wand  the wand ItemStack
     * @param index the spell index to set
     */
    void setActiveIndex(@NotNull ItemStack wand, int index);

    /**
     * Binds a spell to a wand.
     *
     * @param wand     the wand ItemStack
     * @param spellKey the spell key to bind
     * @return true if the spell was bound successfully, false otherwise
     */
    boolean bindSpell(@NotNull ItemStack wand, @NotNull String spellKey);

    /**
     * Unbinds a spell from a wand.
     *
     * @param wand     the wand ItemStack
     * @param spellKey the spell key to unbind
     * @return true if the spell was unbound successfully, false otherwise
     */
    boolean unbindSpell(@NotNull ItemStack wand, @NotNull String spellKey);

    /**
     * Sets the active spell index for a wand.
     *
     * @param wand  the wand ItemStack
     * @param index the spell index (0-based)
     * @return true if the index was set successfully, false otherwise
     */
    boolean setActiveSpell(@NotNull ItemStack wand, int index);

    /**
     * Gets the active spell index for a wand.
     *
     * @param wand the wand ItemStack
     * @return the active spell index, or -1 if no active spell
     */
    int getActiveSpellIndex(@NotNull ItemStack wand);

    /**
     * Gets the active spell key for a wand.
     *
     * @param wand the wand ItemStack
     * @return the active spell key, or null if no active spell
     */
    @Nullable
    String getActiveSpellKey(@NotNull ItemStack wand);

    /**
     * Gets the wand a player is currently holding.
     *
     * @param player the player to check
     * @return the wand ItemStack, or null if player is not holding a wand
     */
    @Nullable
    ItemStack getHeldWand(@NotNull Player player);

    /**
     * Creates a new MephidantesZeist (Netherite Scythe wand).
     *
     * @return a new MephidantesZeist ItemStack
     */
    @NotNull
    ItemStack createMephidantesZeist();

    /**
     * Checks if an ItemStack is a MephidantesZeist.
     *
     * @param item the item to check
     * @return true if the item is a MephidantesZeist, false otherwise
     */
    boolean isMephidantesZeist(@Nullable ItemStack item);

    /**
     * Gives a wand to a player.
     *
     * @param player the player to give the wand to
     * @return true if the wand was given successfully, false otherwise
     */
    boolean giveWand(@NotNull Player player);

    /**
     * Gives a MephidantesZeist to a player.
     *
     * @param player the player to give the MephidantesZeist to
     * @return true if the MephidantesZeist was given successfully, false otherwise
     */
    boolean giveMephidantesZeist(@NotNull Player player);

    // ===== NEW ADVANCED METHODS =====

    /**
     * Creates a new wand builder for constructing custom wands.
     *
     * @return a new WandBuilder instance
     */
    @NotNull
    WandBuilder createWand();

    /**
     * Creates a wand from a template.
     *
     * @param template the wand template to use
     * @return a new WandBuilder pre-configured with template settings
     */
    @NotNull
    WandBuilder createWand(@NotNull WandTemplate template);

    /**
     * Gets a wand customizer for modifying existing wands.
     *
     * @param wand the wand to customize
     * @return a WandCustomizer for the wand
     */
    @NotNull
    WandCustomizer getCustomizer(@NotNull ItemStack wand);

    // ===== TEMPLATE MANAGEMENT =====

    /**
     * Creates a new wand template.
     *
     * @param name the template name
     * @return a new WandTemplate.Builder instance
     */
    @NotNull
    WandTemplate.Builder createTemplate(@NotNull String name);

    /**
     * Gets a wand template by name.
     *
     * @param name the template name
     * @return an Optional containing the template, or empty if not found
     */
    @NotNull
    Optional<WandTemplate> getTemplate(@NotNull String name);

    /**
     * Gets all available wand template names.
     *
     * @return an unmodifiable set of template names
     */
    @NotNull
    Set<String> getAvailableTemplates();

    /**
     * Registers a custom wand template.
     *
     * @param template the template to register
     * @return true if registered successfully, false otherwise
     */
    boolean registerTemplate(@NotNull WandTemplate template);

    /**
     * Unregisters a wand template.
     *
     * @param name the template name to unregister
     * @return true if unregistered successfully, false otherwise
     */
    boolean unregisterTemplate(@NotNull String name);

    // ===== STATISTICS AND ANALYTICS =====

    /**
     * Gets statistics for a specific wand.
     *
     * @param wand the wand ItemStack
     * @return the wand statistics
     */
    @NotNull
    WandStatistics getStatistics(@NotNull ItemStack wand);

    /**
     * Gets global wand statistics across all wands.
     *
     * @return the global wand statistics
     */
    @NotNull
    WandStatistics getGlobalStatistics();

    // ===== ADVANCED OPERATIONS =====

    /**
     * Merges two wands, combining their spells and properties.
     *
     * @param source the source wand
     * @param target the target wand (will be modified)
     * @return true if merged successfully, false otherwise
     */
    boolean mergeWands(@NotNull ItemStack source, @NotNull ItemStack target);

    /**
     * Splits a wand, extracting a specific spell into a new wand.
     *
     * @param wand     the wand to split
     * @param spellKey the spell key to extract
     * @return an Optional containing the new wand with the extracted spell, or
     *         empty if failed
     */
    @NotNull
    Optional<ItemStack> splitWand(@NotNull ItemStack wand, @NotNull String spellKey);

    /**
     * Clones a wand with all its properties and spells.
     *
     * @param wand the wand to clone
     * @return a new ItemStack that is a clone of the original wand
     */
    @NotNull
    ItemStack cloneWand(@NotNull ItemStack wand);

    /**
     * Repairs a wand, restoring its durability if applicable.
     *
     * @param wand the wand to repair
     * @return true if repaired successfully, false otherwise
     */
    boolean repairWand(@NotNull ItemStack wand);

    // ===== WAND BUILDER INTERFACE =====

    /**
     * Builder interface for creating custom wands.
     *
     * @since 2.0.0
     */
    interface WandBuilder {
        @NotNull
        WandBuilder material(@NotNull org.bukkit.Material material);

        @NotNull
        WandBuilder name(@NotNull net.kyori.adventure.text.Component name);

        @NotNull
        WandBuilder lore(@NotNull net.kyori.adventure.text.Component... lore);

        @NotNull
        WandBuilder spells(@NotNull String... spellKeys);

        @NotNull
        WandBuilder activeSpell(@NotNull String spellKey);

        @NotNull
        WandBuilder customData(@NotNull String key, Object value);

        @NotNull
        WandBuilder enchantments(@NotNull java.util.Map<org.bukkit.enchantments.Enchantment, Integer> enchantments);

        @NotNull
        ItemStack build();
    }

    // ===== WAND TEMPLATE BUILDER =====

    /**
     * Builder interface for creating wand templates.
     *
     * @since 2.0.0
     */
    interface WandTemplateBuilder {
        // Template builder methods would be defined here
    }
}
