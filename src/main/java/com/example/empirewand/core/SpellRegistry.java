package com.example.empirewand.core;

import com.example.empirewand.spell.Spell;
import java.util.Set;

/**
 * Registry for managing spell implementations.
 */
public interface SpellRegistry {

    /**
     * Gets a spell by its key.
     */
    Spell getSpell(String key);

    /**
     * Gets all registered spells as an unmodifiable map.
     */
    java.util.Map<String, Spell> getAllSpells();

    /**
     * Gets all registered spell keys.
     */
    Set<String> getSpellKeys();

    /**
     * Checks if a spell is registered.
     */
    boolean isSpellRegistered(String key);

    /**
     * Gets the display name for a spell.
     */
    String getSpellDisplayName(String key);
}
