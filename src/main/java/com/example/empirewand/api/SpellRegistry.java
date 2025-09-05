package com.example.empirewand.api;

import com.example.empirewand.spell.Spell;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Public API interface for accessing EmpireWand's spell registry.
 *
 * <p>This interface provides read-only access to registered spells and their metadata.
 * External plugins can use this to query available spells and their properties.</p>
 *
 * <p><b>API Stability:</b> Experimental - Subject to change in future versions</p>
 *
 * @since 1.1.0
 */
public interface SpellRegistry {

    /**
     * Gets a spell by its registry key.
     *
     * @param key the spell key (kebab-case)
     * @return the spell instance, or null if not found
     */
    @Nullable
    Spell getSpell(@NotNull String key);

    /**
     * Gets all registered spells.
     *
     * @return an unmodifiable map of spell keys to spell instances
     */
    @NotNull
    Map<String, Spell> getAllSpells();

    /**
     * Gets all registered spell keys.
     *
     * @return an unmodifiable set of spell keys
     */
    @NotNull
    Set<String> getSpellKeys();

    /**
     * Checks if a spell is registered.
     *
     * @param key the spell key to check
     * @return true if the spell is registered, false otherwise
     */
    boolean isSpellRegistered(@NotNull String key);

    /**
     * Gets the display name for a spell.
     *
     * @param key the spell key
     * @return the display name, or the key if not configured
     */
    @NotNull
    String getSpellDisplayName(@NotNull String key);
}