package com.example.empirewand.core.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptimizedCooldownServiceTest {

    @Mock
    private Plugin plugin;

    @Mock
    private ItemStack wand;

    @Mock
    private ItemMeta itemMeta;

    private OptimizedCooldownService service;
    private UUID playerId;
    private String spellKey;

    @BeforeEach
    void setUp() {
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("TestLogger"));
        service = new OptimizedCooldownService(plugin);
        playerId = UUID.randomUUID();
        spellKey = "test-spell";
    }

    @Test
    void testIsOnCooldownFalseWhenNoCooldown() {
        // Act
        boolean result = service.isOnCooldown(playerId, spellKey, 1000L);

        // Assert
        assertFalse(result);
    }

    @Test
    void testSetAndCheckCooldown() {
        // Arrange
        long nowTicks = 1000L;
        long cooldownTicks = 200L;

        // Act
        service.set(playerId, spellKey, nowTicks + cooldownTicks);
        boolean isOnCooldown = service.isOnCooldown(playerId, spellKey, nowTicks);

        // Assert
        assertTrue(isOnCooldown);
    }

    @Test
    void testCooldownExpires() {
        // Arrange
        long nowTicks = 1000L;
        long cooldownTicks = 200L;

        // Act
        service.set(playerId, spellKey, nowTicks + cooldownTicks);
        boolean isOnCooldownBeforeExpiry = service.isOnCooldown(playerId, spellKey, nowTicks);
        boolean isOnCooldownAfterExpiry = service.isOnCooldown(playerId, spellKey, nowTicks + cooldownTicks + 1);

        // Assert
        assertTrue(isOnCooldownBeforeExpiry);
        assertFalse(isOnCooldownAfterExpiry);
    }

    @Test
    void testRemainingCooldown() {
        // Arrange
        long nowTicks = 1000L;
        long cooldownTicks = 200L;

        // Act
        service.set(playerId, spellKey, nowTicks + cooldownTicks);
        long remaining = service.remaining(playerId, spellKey, nowTicks);

        // Assert
        assertEquals(cooldownTicks, remaining);
    }

    @Test
    void testRemainingCooldownAfterPartialTime() {
        // Arrange
        long nowTicks = 1000L;
        long cooldownTicks = 200L;
        long elapsedTicks = 50L;

        // Act
        service.set(playerId, spellKey, nowTicks + cooldownTicks);
        long remaining = service.remaining(playerId, spellKey, nowTicks + elapsedTicks);

        // Assert
        assertEquals(cooldownTicks - elapsedTicks, remaining);
    }

    @Test
    void testRemainingCooldownExpired() {
        // Arrange
        long nowTicks = 1000L;
        long cooldownTicks = 200L;

        // Act
        service.set(playerId, spellKey, nowTicks + cooldownTicks);
        long remaining = service.remaining(playerId, spellKey, nowTicks + cooldownTicks + 1);

        // Assert
        assertEquals(0L, remaining);
    }

    @Test
    void testClearAllCooldowns() {
        // Arrange
        long nowTicks = 1000L;
        service.set(playerId, spellKey, nowTicks + 200L);
        service.set(playerId, "other-spell", nowTicks + 300L);

        // Act
        service.clearAll(playerId);
        boolean isOnCooldown = service.isOnCooldown(playerId, spellKey, nowTicks);

        // Assert
        assertFalse(isOnCooldown);
    }

    @Test
    void testCooldownDisabled() {
        // Arrange
        when(wand.getItemMeta()).thenReturn(itemMeta);
        when(itemMeta.hasDisplayName()).thenReturn(true);
        when(itemMeta.displayName()).thenReturn(net.kyori.adventure.text.Component.text("Test Wand"));
        when(wand.getType()).thenReturn(org.bukkit.Material.STICK);

        // Act
        service.setCooldownDisabled(playerId, wand, true);
        boolean isDisabled = service.isCooldownDisabled(playerId, wand);

        // Assert
        assertTrue(isDisabled);
    }

    @Test
    void testCooldownDisabledWithWand() {
        // Arrange
        long nowTicks = 1000L;
        when(wand.getItemMeta()).thenReturn(itemMeta);
        when(itemMeta.hasDisplayName()).thenReturn(true);
        when(itemMeta.displayName()).thenReturn(net.kyori.adventure.text.Component.text("Test Wand"));
        when(wand.getType()).thenReturn(org.bukkit.Material.STICK);

        service.setCooldownDisabled(playerId, wand, true);
        service.set(playerId, spellKey, nowTicks + 200L);

        // Act
        boolean isOnCooldown = service.isOnCooldown(playerId, spellKey, nowTicks, wand);

        // Assert
        assertFalse(isOnCooldown); // Should be false because cooldown is disabled
    }

    @Test
    void testCooldownEnabledWithWand() {
        // Arrange
        long nowTicks = 1000L;
        when(wand.getItemMeta()).thenReturn(itemMeta);
        when(itemMeta.hasDisplayName()).thenReturn(true);
        when(itemMeta.displayName()).thenReturn(net.kyori.adventure.text.Component.text("Test Wand"));
        when(wand.getType()).thenReturn(org.bukkit.Material.STICK);

        service.setCooldownDisabled(playerId, wand, false); // Explicitly enable
        service.set(playerId, spellKey, nowTicks + 200L);

        // Act
        boolean isOnCooldown = service.isOnCooldown(playerId, spellKey, nowTicks, wand);

        // Assert
        assertTrue(isOnCooldown); // Should be true because cooldown is enabled
    }

    @Test
    void testGetMetrics() {
        // Arrange
        service.set(playerId, spellKey, 1200L);

        // Act
        OptimizedCooldownService.CooldownMetrics metrics = service.getMetrics();

        // Assert
        assertNotNull(metrics);
        assertTrue(metrics.totalPlayers() >= 1);
        assertTrue(metrics.totalCooldowns() >= 1);
    }

    @Test
    void testShutdown() {
        // Arrange
        service.set(playerId, spellKey, 1200L);

        // Act
        service.shutdown();

        // Assert - Should not throw any exceptions
        assertDoesNotThrow(() -> service.getMetrics());
    }

    @Test
    void testNullPlayerIdHandling() {
        // Act & Assert
        assertDoesNotThrow(() -> service.isOnCooldown(null, spellKey, 1000L));
        assertDoesNotThrow(() -> service.set(null, spellKey, 1200L));
        assertDoesNotThrow(() -> service.clearAll(null));
    }

    @Test
    void testNullSpellKeyHandling() {
        // Act & Assert
        assertDoesNotThrow(() -> service.isOnCooldown(playerId, null, 1000L));
        assertDoesNotThrow(() -> service.set(playerId, null, 1200L));
    }

    @Test
    void testNullWandHandling() {
        // Act & Assert
        assertDoesNotThrow(() -> service.setCooldownDisabled(playerId, null, true));
        assertDoesNotThrow(() -> service.isCooldownDisabled(playerId, null));
    }
}