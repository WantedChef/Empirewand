package com.example.empirewand.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Template for creating wands with predefined properties.
 *
 * @since 2.0.0
 */
public interface WandTemplate {

    /**
     * Gets the template name.
     *
     * @return the template name
     */
    @NotNull
    String getName();

    /**
     * Gets the template display name.
     *
     * @return the display name
     */
    @NotNull
    Component getDisplayName();

    /**
     * Gets the base material for wands created from this template.
     *
     * @return the base material
     */
    @NotNull
    Material getMaterial();

    /**
     * Gets the default spells for wands created from this template.
     *
     * @return an unmodifiable set of spell keys
     */
    @NotNull
    Set<String> getDefaultSpells();

    /**
     * Gets the default lore for wands created from this template.
     *
     * @return an array of lore components
     */
    @NotNull
    Component[] getDefaultLore();

    /**
     * Gets the default enchantments for wands created from this template.
     *
     * @return an unmodifiable map of enchantments to levels
     */
    @NotNull
    Map<org.bukkit.enchantments.Enchantment, Integer> getDefaultEnchantments();

    /**
     * Gets a custom property from the template.
     *
     * @param key the property key
     * @return the property value, or null if not set
     */
    @Nullable
    Object getProperty(@NotNull String key);

    /**
     * Builder for creating WandTemplate instances.
     *
     * @since 2.0.0
     */
    interface Builder {
        @NotNull
        Builder displayName(@NotNull Component displayName);

        @NotNull
        Builder material(@NotNull Material material);

        @NotNull
        Builder defaultSpells(@NotNull String... spellKeys);

        @NotNull
        Builder defaultLore(@NotNull Component... lore);

        @NotNull
        Builder defaultEnchantments(@NotNull java.util.Map<org.bukkit.enchantments.Enchantment, Integer> enchantments);

        @NotNull
        Builder property(@NotNull String key, @NotNull Object value);

        @NotNull
        WandTemplate build();
    }
}