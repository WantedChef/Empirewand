package nl.wantedchef.empirewand.api.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interface for customizing wand properties.
 *
 * @since 2.0.0
 */
public interface WandCustomizer {

    /**
     * Sets the wand's display name.
     *
     * @param displayName the new display name
     * @return this customizer for chaining
     */
    @NotNull
    WandCustomizer setDisplayName(@NotNull Component displayName);

    /**
     * Sets the wand's lore.
     *
     * @param lore the new lore lines
     * @return this customizer for chaining
     */
    @NotNull
    WandCustomizer setLore(@NotNull Component... lore);

    /**
     * Adds lore lines to the wand.
     *
     * @param lore the lore lines to add
     * @return this customizer for chaining
     */
    @NotNull
    WandCustomizer addLore(@NotNull Component... lore);

    /**
     * Sets the wand's material.
     *
     * @param material the new material
     * @return this customizer for chaining
     */
    @NotNull
    WandCustomizer setMaterial(@NotNull Material material);

    /**
     * Adds an enchantment to the wand.
     *
     * @param enchantment the enchantment to add
     * @param level       the enchantment level
     * @return this customizer for chaining
     */
    @NotNull
    WandCustomizer addEnchantment(@NotNull org.bukkit.enchantments.Enchantment enchantment, int level);

    /**
     * Removes an enchantment from the wand.
     *
     * @param enchantment the enchantment to remove
     * @return this customizer for chaining
     */
    @NotNull
    WandCustomizer removeEnchantment(@NotNull org.bukkit.enchantments.Enchantment enchantment);

    /**
     * Sets multiple enchantments on the wand.
     *
     * @param enchantments the enchantments to set
     * @return this customizer for chaining
     */
    @NotNull
    WandCustomizer setEnchantments(@NotNull Map<org.bukkit.enchantments.Enchantment, Integer> enchantments);

    /**
     * Sets a custom property on the wand.
     *
     * @param key   the property key
     * @param value the property value
     * @return this customizer for chaining
     */
    @NotNull
    WandCustomizer setProperty(@NotNull String key, @NotNull Object value);

    /**
     * Removes a custom property from the wand.
     *
     * @param key the property key to remove
     * @return this customizer for chaining
     */
    @NotNull
    WandCustomizer removeProperty(@NotNull String key);

    /**
     * Applies all changes to the wand.
     *
     * @return the modified wand ItemStack
     */
    @NotNull
    ItemStack apply();
}
