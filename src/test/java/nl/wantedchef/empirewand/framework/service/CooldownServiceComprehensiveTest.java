package nl.wantedchef.empirewand.framework.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
=======
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
>>>>>>> origin/main

class CooldownServiceComprehensiveTest {

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
    @DisplayName("Test setting and checking cooldown with various scenarios")
    void testSetAndCheckCooldownComprehensive() {
        long nowTicks = 1000L;
        long cooldownTicks = 100L;
        
        // Test 1: Initially not on cooldown
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks), 
            "Player should not be on cooldown initially");
        
        // Test 2: Set cooldown and verify it's active
        cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks), 
            "Player should be on cooldown after setting it");
        
        // Test 3: Check at exact expiry time
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + cooldownTicks), 
            "Player should not be on cooldown at exact expiry time");
        
        // Test 4: Check after expiry
        assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + cooldownTicks + 1), 
            "Player should not be on cooldown after expiry");
        
        // Test 5: Check with null parameters (should not throw exceptions)
        assertDoesNotThrow(() -> cooldownService.isOnCooldown(null, spellKey, nowTicks));
        assertDoesNotThrow(() -> cooldownService.isOnCooldown(playerId, null, nowTicks));
        assertFalse(cooldownService.isOnCooldown(null, spellKey, nowTicks));
        assertFalse(cooldownService.isOnCooldown(playerId, null, nowTicks));
    }

    @Test
    @DisplayName("Test remaining cooldown time calculations")
    void testRemainingCooldownCalculations() {
        long nowTicks = 1000L;
        long cooldownTicks = 100L;
        
        // Test 1: No cooldown set
        assertEquals(0L, cooldownService.remaining(playerId, spellKey, nowTicks), 
            "Remaining time should be 0 when no cooldown is set");
        
        // Test 2: Set cooldown and check remaining time
        cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
        assertEquals(cooldownTicks, cooldownService.remaining(playerId, spellKey, nowTicks), 
            "Remaining time should match set cooldown");
        
        // Test 3: Check remaining time halfway through
        assertEquals(cooldownTicks / 2, cooldownService.remaining(playerId, spellKey, nowTicks + cooldownTicks / 2), 
            "Remaining time should be half when halfway through cooldown");
        
        // Test 4: Check remaining time after expiry
        assertEquals(0L, cooldownService.remaining(playerId, spellKey, nowTicks + cooldownTicks + 1), 
            "Remaining time should be 0 after cooldown expiry");
    }

    @Test
    @DisplayName("Test clearing all cooldowns for player")
    void testClearAllCooldowns() {
        long nowTicks = 1000L;
        long cooldownTicks = 100L;
        
        // Set multiple cooldowns for the same player
        cooldownService.set(playerId, "spell1", nowTicks + cooldownTicks);
        cooldownService.set(playerId, "spell2", nowTicks + cooldownTicks * 2);
        cooldownService.set(playerId, "spell3", nowTicks + cooldownTicks * 3);
        
        // Verify all are active
        assertTrue(cooldownService.isOnCooldown(playerId, "spell1", nowTicks));
        assertTrue(cooldownService.isOnCooldown(playerId, "spell2", nowTicks));
        assertTrue(cooldownService.isOnCooldown(playerId, "spell3", nowTicks));
        
        // Clear all cooldowns
        cooldownService.clearAll(playerId);
        
        // Verify all are cleared
        assertFalse(cooldownService.isOnCooldown(playerId, "spell1", nowTicks));
        assertFalse(cooldownService.isOnCooldown(playerId, "spell2", nowTicks));
        assertFalse(cooldownService.isOnCooldown(playerId, "spell3", nowTicks));
    }

    @Test
    @DisplayName("Test shutdown method")
    void testShutdown() {
        long nowTicks = 1000L;
        long cooldownTicks = 100L;
        
        // Set some cooldowns
        cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
        assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        
        // Call shutdown
        assertDoesNotThrow(() -> cooldownService.shutdown(), 
            "Shutdown should not throw exceptions");
        
        // After shutdown, operations should still work (they should be defensive)
        assertDoesNotThrow(() -> cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        assertDoesNotThrow(() -> cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks));
    }
}