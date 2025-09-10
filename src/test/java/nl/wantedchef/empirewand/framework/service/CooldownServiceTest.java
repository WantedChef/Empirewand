package nl.wantedchef.empirewand.framework.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}