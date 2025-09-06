package com.example.empirewand.core;

import com.example.empirewand.EmpireWandTestBase;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for SpellRegistry.
 */
class SpellRegistryTest extends EmpireWandTestBase {

    private SpellRegistryImpl spellRegistry;

    @BeforeEach
    void setUp() {
        spellRegistry = new SpellRegistryImpl();
    }

    @Test
    void testSpellRegistryInitialization() {
        // When
        var allSpells = spellRegistry.getAllSpells();

        // Then
        assertNotNull(allSpells);
        assertFalse(allSpells.isEmpty());
        // Should have the default spells registered
        assertTrue(allSpells.size() >= 10);
    }

    @Test
    void testGetSpellKeys() {
        // When
        var keys = spellRegistry.getSpellKeys();

        // Then
        assertNotNull(keys);
        assertFalse(keys.isEmpty());
        // Check that keys are in kebab-case
        for (String key : keys) {
            assertTrue(key.matches("^[a-z]+(-[a-z]+)*$"), "Key should be kebab-case: " + key);
        }
    }

    @Test
    void testIsSpellRegistered() {
        // Given
        var keys = spellRegistry.getSpellKeys();
        String firstKey = keys.iterator().next();

        // When & Then
        assertTrue(spellRegistry.isSpellRegistered(firstKey));
        assertFalse(spellRegistry.isSpellRegistered("nonexistent-spell"));
    }

    @Test
    void testGetSpell() {
        // Given
        var keys = spellRegistry.getSpellKeys();
        String firstKey = keys.iterator().next();

        // When
        Spell spell = spellRegistry.getSpell(firstKey);

        // Then
        assertNotNull(spell);
        assertEquals(firstKey, spell.key());
    }

    @Test
    void testGetSpellNonexistent() {
        // When
        Spell spell = spellRegistry.getSpell("nonexistent-spell");

        // Then
        assertNull(spell);
    }

    @Test
    void testGetSpellDisplayName() {
        // Given
        var keys = spellRegistry.getSpellKeys();
        String firstKey = keys.iterator().next();

        // When
        String displayName = spellRegistry.getSpellDisplayName(firstKey);

        // Then
        assertNotNull(displayName);
        assertFalse(displayName.isEmpty());
    }

    @Test
    void testGetSpellDisplayNameNonexistent() {
        // When
        String displayName = spellRegistry.getSpellDisplayName("nonexistent-spell");

        // Then
        assertEquals("nonexistent-spell", displayName);
    }

    @Test
    void testGetAllSpellsReturnsUnmodifiableMap() {
        // When
        var allSpells = spellRegistry.getAllSpells();

        // Then
        assertThrows(UnsupportedOperationException.class, () -> {
            allSpells.put("test", null);
        });
    }

    @Test
    void testGetSpellKeysReturnsUnmodifiableSet() {
        // When
        var keys = spellRegistry.getSpellKeys();

        // Then
        assertThrows(UnsupportedOperationException.class, () -> {
            keys.add("test");
        });
    }

    @Test
    void testRegisterCustomSpell() {
        // Given
        Spell customSpell = new TestSpell();

        // When
        spellRegistry.register(customSpell);

        // Then
        assertTrue(spellRegistry.isSpellRegistered("test-spell"));
        assertEquals(customSpell, spellRegistry.getSpell("test-spell"));
    }

    /**
     * Simple test spell implementation for testing.
     */
    private static class TestSpell implements Spell {
        @Override
        public void execute(com.example.empirewand.spell.SpellContext context) {
            // Test implementation
        }

        @Override
        public String getName() {
            return "test-spell";
        }

        @Override
        public String key() {
            return "test-spell";
        }

        @Override
        public Component displayName() {
            return Component.text("Test Spell");
        }

        @Override
        public Prereq prereq() {
            return new Prereq(true, Component.text(""));
        }
    }
}
