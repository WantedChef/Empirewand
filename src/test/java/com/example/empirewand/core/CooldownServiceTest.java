package com.example.empirewand.core;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CooldownService.
 */
class CooldownServiceTest {

    private CooldownService service;
    private UUID playerId;
    private final long defaultTicks = 1000L;

    @BeforeEach
    void setUp() {
        service = new CooldownService();
        playerId = UUID.randomUUID();
    }

    @Test
    void testNotOnCooldownInitially() {
        // When
        boolean onCooldown = service.isOnCooldown(playerId, "spell", defaultTicks);

        // Then
        assertFalse(onCooldown);
    }

    @Test
    void testRemainingTimeInitially() {
        // When
        long remaining = service.remaining(playerId, "spell", defaultTicks);

        // Then
        assertEquals(0L, remaining);
    }

    @Test
    void testSetCooldown() {
        // Given
        long cooldownUntil = 1500;

        // When
        service.set(playerId, "spell", cooldownUntil);

        // Then
        assertTrue(service.isOnCooldown(playerId, "spell", defaultTicks));
        assertEquals(500L, service.remaining(playerId, "spell", defaultTicks));
    }

    @Test
    void testCooldownExpires() {
        // Given
        long cooldownUntil = 1500;
        service.set(playerId, "spell", cooldownUntil);

        // When
        long afterExpiry = 1600;

        // Then
        assertFalse(service.isOnCooldown(playerId, "spell", afterExpiry));
        assertEquals(0L, service.remaining(playerId, "spell", afterExpiry));
    }

    @Test
    void testMultipleSpellsPerPlayer() {
        // Given
        String spellKey2 = "test-spell-2";
        service.set(playerId, "spell", 1500);
        service.set(playerId, spellKey2, 2000);

        // Then
        assertTrue(service.isOnCooldown(playerId, "spell", defaultTicks));
        assertTrue(service.isOnCooldown(playerId, spellKey2, defaultTicks));
        assertEquals(500L, service.remaining(playerId, "spell", defaultTicks));
        assertEquals(1000L, service.remaining(playerId, spellKey2, defaultTicks));
    }

    @Test
    void testMultiplePlayers() {
        // Given
        UUID playerId2 = UUID.randomUUID();
        service.set(playerId, "spell", 1500);
        service.set(playerId2, "spell", 2000);

        // Then
        assertTrue(service.isOnCooldown(playerId, "spell", defaultTicks));
        assertTrue(service.isOnCooldown(playerId2, "spell", defaultTicks));
        assertEquals(500L, service.remaining(playerId, "spell", defaultTicks));
        assertEquals(1000L, service.remaining(playerId2, "spell", defaultTicks));
    }

    @Test
    void testClearAll() {
        // Given
        String spellKey2 = "test-spell-2";
        service.set(playerId, "spell", 1500);
        service.set(playerId, spellKey2, 2000);

        // When
        service.clearAll(playerId);

        // Then
        assertFalse(service.isOnCooldown(playerId, "spell", defaultTicks));
        assertFalse(service.isOnCooldown(playerId, spellKey2, defaultTicks));
    }

    @Test
    void testClearAllDoesNotAffectOtherPlayers() {
        // Given
        UUID playerId2 = UUID.randomUUID();
        service.set(playerId, "spell", 1500);
        service.set(playerId2, "spell", 2000);

        // When
        service.clearAll(playerId);

        // Then
        assertFalse(service.isOnCooldown(playerId, "spell", defaultTicks));
        assertTrue(service.isOnCooldown(playerId2, "spell", defaultTicks));
    }

    @Test
    void testOverwriteCooldown() {
        // Given
        service.set(playerId, "spell", 1500);

        // When
        service.set(playerId, "spell", 1200);

        // Then
        assertTrue(service.isOnCooldown(playerId, "spell", defaultTicks));
        assertEquals(200L, service.remaining(playerId, "spell", defaultTicks));
    }

    @Test
    void testIsOnCooldownFalseInitially() {
        assertFalse(service.isOnCooldown(playerId, "spell", defaultTicks));
    }

    @Test
    void testSetAndIsOnCooldownTrue() {
        service.set(playerId, "spell", defaultTicks + 10);
        assertTrue(service.isOnCooldown(playerId, "spell", defaultTicks));
    }

    @Test
    void testRemaining() {
        service.set(playerId, "spell", defaultTicks + 15);
        assertEquals(15, service.remaining(playerId, "spell", defaultTicks));
        assertEquals(0, service.remaining(playerId, "other", defaultTicks));
    }

    @Test
    void testExpiredCooldown() {
        service.set(playerId, "spell", defaultTicks - 1);
        assertFalse(service.isOnCooldown(playerId, "spell", defaultTicks));
        assertEquals(0, service.remaining(playerId, "spell", defaultTicks));
    }
}
