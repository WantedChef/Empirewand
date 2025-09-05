package com.example.empirewand.core;

import com.example.empirewand.EmpireWandTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CooldownService.
 */
class CooldownServiceTest extends EmpireWandTestBase {

    private CooldownService cooldownService;
    private UUID playerId;
    private String spellKey;

    @BeforeEach
    void setUp() {
        cooldownService = new CooldownService();
        playerId = UUID.randomUUID();
        spellKey = "test-spell";
    }

    @Test
    void testNotOnCooldownInitially() {
        // Given
        long nowTicks = 1000;

        // When
        boolean onCooldown = cooldownService.isOnCooldown(playerId, spellKey, nowTicks);

        // Then
        assertFalse(onCooldown);
    }

    @Test
    void testRemainingTimeInitially() {
        // Given
        long nowTicks = 1000;

        // When
        long remaining = cooldownService.remaining(playerId, spellKey, nowTicks);

        // Then
        assertEquals(0L, remaining);
    }

    @Test
    void testSetCooldown() {
        // Given
        long nowTicks = 1000;
        long cooldownUntil = 1500;

        // When
        cooldownService.set(playerId, spellKey, cooldownUntil);

        // Then
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        assertEquals(500L, cooldownService.remaining(playerId, spellKey, nowTicks));
    }

    @Test
    void testCooldownExpires() {
        // Given
        long nowTicks = 1000;
        long cooldownUntil = 1500;
        cooldownService.set(playerId, spellKey, cooldownUntil);

        // When
        long afterExpiry = 1600;

        // Then
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, afterExpiry));
        assertEquals(0L, cooldownService.remaining(playerId, spellKey, afterExpiry));
    }

    @Test
    void testMultipleSpellsPerPlayer() {
        // Given
        String spellKey2 = "test-spell-2";
        long nowTicks = 1000;
        cooldownService.set(playerId, spellKey, 1500);
        cooldownService.set(playerId, spellKey2, 2000);

        // Then
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey2, nowTicks));
        assertEquals(500L, cooldownService.remaining(playerId, spellKey, nowTicks));
        assertEquals(1000L, cooldownService.remaining(playerId, spellKey2, nowTicks));
    }

    @Test
    void testMultiplePlayers() {
        // Given
        UUID playerId2 = UUID.randomUUID();
        long nowTicks = 1000;
        cooldownService.set(playerId, spellKey, 1500);
        cooldownService.set(playerId2, spellKey, 2000);

        // Then
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        assertTrue(cooldownService.isOnCooldown(playerId2, spellKey, nowTicks));
        assertEquals(500L, cooldownService.remaining(playerId, spellKey, nowTicks));
        assertEquals(1000L, cooldownService.remaining(playerId2, spellKey, nowTicks));
    }

    @Test
    void testClearAll() {
        // Given
        String spellKey2 = "test-spell-2";
        long nowTicks = 1000;
        cooldownService.set(playerId, spellKey, 1500);
        cooldownService.set(playerId, spellKey2, 2000);

        // When
        cooldownService.clearAll(playerId);

        // Then
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey2, nowTicks));
    }

    @Test
    void testClearAllDoesNotAffectOtherPlayers() {
        // Given
        UUID playerId2 = UUID.randomUUID();
        long nowTicks = 1000;
        cooldownService.set(playerId, spellKey, 1500);
        cooldownService.set(playerId2, spellKey, 2000);

        // When
        cooldownService.clearAll(playerId);

        // Then
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        assertTrue(cooldownService.isOnCooldown(playerId2, spellKey, nowTicks));
    }

    @Test
    void testOverwriteCooldown() {
        // Given
        long nowTicks = 1000;
        cooldownService.set(playerId, spellKey, 1500);

        // When
        cooldownService.set(playerId, spellKey, 1200);

        // Then
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        assertEquals(200L, cooldownService.remaining(playerId, spellKey, nowTicks));
    }
}