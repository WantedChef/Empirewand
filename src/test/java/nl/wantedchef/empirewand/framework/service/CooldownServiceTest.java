package nl.wantedchef.empirewand.framework.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CooldownServiceTest {

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
    @DisplayName("Test setting and checking cooldown")
    void testSetAndCheckCooldown() {
        long nowTicks = 1000L;
        long cooldownTicks = 100L;
        
        // Initially not on cooldown
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        
        // Set cooldown
        cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
        
        // Should be on cooldown
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        
        // Should still be on cooldown before expiry
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + cooldownTicks - 1));
        
        // Should not be on cooldown at exact expiry time (nowTicks == until)
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + cooldownTicks));
        
        // Should not be on cooldown after expiry
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + cooldownTicks + 1));
    }

    @Test
    @DisplayName("Test setting and checking cooldown with edge cases")
    void testSetAndCheckCooldownEdgeCases() {
        long nowTicks = 1000L;
        
        // Test with 0 cooldown (expires immediately)
        cooldownService.set(playerId, spellKey, nowTicks);
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        
        // Test with negative cooldown (should not be set)
        cooldownService.set(playerId, spellKey, -1L);
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        
        // Test with very large cooldown
        cooldownService.set(playerId, spellKey, Long.MAX_VALUE);
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
    }

    @Test
    @DisplayName("Test clearing all cooldowns for player")
    void testClearAllCooldowns() {
        long nowTicks = 1000L;
        long cooldownTicks = 100L;
        
        // Set cooldown
        cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
        
        // Verify on cooldown
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        
        // Clear all cooldowns
        cooldownService.clearAll(playerId);
        
        // Should not be on cooldown anymore
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
    }

    @Test
    @DisplayName("Test clearing all cooldowns for non-existent player")
    void testClearAllCooldownsForNonExistentPlayer() {
        UUID nonExistentPlayer = UUID.randomUUID();
        
        // Should not throw exception
        assertDoesNotThrow(() -> cooldownService.clearAll(nonExistentPlayer));
        
        // Should not be on cooldown
        assertFalse(cooldownService.isOnCooldown(nonExistentPlayer, spellKey, 1000L));
    }

    @Test
    @DisplayName("Test remaining cooldown time")
    void testRemainingCooldown() {
        long nowTicks = 1000L;
        long cooldownTicks = 100L;
        
        // Set cooldown
        cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
        
        // Check remaining time
        assertEquals(cooldownTicks, cooldownService.remaining(playerId, spellKey, nowTicks));
        assertEquals(50L, cooldownService.remaining(playerId, spellKey, nowTicks + 50));
        assertEquals(0L, cooldownService.remaining(playerId, spellKey, nowTicks + cooldownTicks));
        assertEquals(0L, cooldownService.remaining(playerId, spellKey, nowTicks + cooldownTicks + 1));
    }

    @Test
    @DisplayName("Test remaining cooldown time edge cases")
    void testRemainingCooldownEdgeCases() {
        long nowTicks = 1000L;
        
        // Test with non-existent player/spell
        assertEquals(0L, cooldownService.remaining(UUID.randomUUID(), spellKey, nowTicks));
        assertEquals(0L, cooldownService.remaining(playerId, "non-existent-spell", nowTicks));
        
        // Test with null parameters
        assertEquals(0L, cooldownService.remaining(null, spellKey, nowTicks));
        assertEquals(0L, cooldownService.remaining(playerId, null, nowTicks));
        assertEquals(0L, cooldownService.remaining(null, null, nowTicks));
    }

    @Test
    @DisplayName("Test cooldown data structure behavior")
    void testCooldownDataStructureBehavior() {
        long nowTicks = 1000L;
        long cooldownTicks = 100L;
        
        // Test with multiple players
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        cooldownService.set(player1, spellKey, nowTicks + cooldownTicks);
        
        // Player 1 should be on cooldown
        assertTrue(cooldownService.isOnCooldown(player1, spellKey, nowTicks));
        
        // Player 2 should not be on cooldown
        assertFalse(cooldownService.isOnCooldown(player2, spellKey, nowTicks));
        
        // Test with multiple spells for same player
        String spell2 = "test-spell-2";
        cooldownService.set(player1, spell2, nowTicks + cooldownTicks * 2);
        
        // Player 1 should be on cooldown for both spells
        assertTrue(cooldownService.isOnCooldown(player1, spellKey, nowTicks));
        assertTrue(cooldownService.isOnCooldown(player1, spell2, nowTicks));
        
        // After first spell expires, only second should be active
        assertFalse(cooldownService.isOnCooldown(player1, spellKey, nowTicks + cooldownTicks + 1));
        assertTrue(cooldownService.isOnCooldown(player1, spell2, nowTicks + cooldownTicks + 1));
    }
}