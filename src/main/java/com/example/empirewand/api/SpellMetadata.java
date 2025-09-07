package com.example.empirewand.api;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Metadata associated with a spell.
 * Contains information about the spell's properties, requirements, and
 * behavior.
 *
 * @since 2.0.0
 */
public interface SpellMetadata {

    /**
     * Gets the spell's unique key.
     *
     * @return the spell key
     */
    @NotNull
    String getKey();

    /**
     * Gets the spell's display name.
     *
     * @return the display name
     */
    @NotNull
    Component getDisplayName();

    /**
     * Gets the spell's description.
     *
     * @return the description
     */
    @NotNull
    Component getDescription();

    /**
     * Gets the spell's category.
     *
     * @return the category
     */
    @NotNull
    String getCategory();

    /**
     * Gets the spell's tags.
     *
     * @return an unmodifiable set of tags
     */
    @NotNull
    Set<String> getTags();

    /**
     * Gets the spell's cooldown in ticks.
     *
     * @return the cooldown in ticks
     */
    long getCooldownTicks();

    /**
     * Gets the spell's range.
     *
     * @return the range in blocks
     */
    double getRange();

    /**
     * Gets the spell's level requirement.
     *
     * @return the level requirement, or 0 if none
     */
    int getLevelRequirement();

    /**
     * Checks if the spell is enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Gets the spell's icon material.
     *
     * @return the icon material
     */
    @NotNull
    String getIconMaterial();

    /**
     * Gets additional custom properties.
     *
     * @param key the property key
     * @return the property value, or null if not set
     */
    @Nullable
    Object getProperty(@NotNull String key);

    /**
     * Builder for creating SpellMetadata instances.
     *
     * @since 2.0.0
     */
    interface Builder {
        @NotNull
        Builder displayName(@NotNull Component displayName);

        @NotNull
        Builder description(@NotNull Component description);

        @NotNull
        Builder category(@NotNull String category);

        @NotNull
        Builder tags(@NotNull String... tags);

        @NotNull
        Builder cooldownTicks(long cooldownTicks);

        @NotNull
        Builder range(double range);

        @NotNull
        Builder levelRequirement(int levelRequirement);

        @NotNull
        Builder enabled(boolean enabled);

        @NotNull
        Builder iconMaterial(@NotNull String iconMaterial);

        @NotNull
        Builder property(@NotNull String key, @NotNull Object value);

        @NotNull
        SpellMetadata build();
    }
}