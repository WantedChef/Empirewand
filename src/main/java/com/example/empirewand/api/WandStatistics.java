package com.example.empirewand.api;

import org.jetbrains.annotations.NotNull;

/**
 * Statistics and analytics for wand usage.
 *
 * @since 2.0.0
 */
public interface WandStatistics {

    /**
     * Gets the total number of spells bound to the wand.
     *
     * @return the spell count
     */
    int getSpellCount();

    /**
     * Gets the number of times the wand has been used.
     *
     * @return the usage count
     */
    long getUsageCount();

    /**
     * Gets the most frequently used spell key.
     *
     * @return the most used spell key, or null if no spells have been used
     */
    @org.jetbrains.annotations.Nullable
    String getMostUsedSpell();

    /**
     * Gets the usage count for a specific spell.
     *
     * @param spellKey the spell key
     * @return the usage count for the spell
     */
    long getSpellUsageCount(@NotNull String spellKey);

    /**
     * Gets the creation timestamp of the wand.
     *
     * @return the creation timestamp in milliseconds
     */
    long getCreationTimestamp();

    /**
     * Gets the last used timestamp of the wand.
     *
     * @return the last used timestamp in milliseconds, or 0 if never used
     */
    long getLastUsedTimestamp();
}