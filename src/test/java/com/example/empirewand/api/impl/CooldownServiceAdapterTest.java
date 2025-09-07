package com.example.empirewand.api.impl;

import com.example.empirewand.core.services.CooldownService;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CooldownServiceAdapter.
 * Verifies that the adapter properly wraps core CooldownService and delegates
 * calls.
 */
@ExtendWith(MockitoExtension.class)
class CooldownServiceAdapterTest {

    @Mock
    private CooldownService mockCooldownService;

    @Mock
    private ItemStack mockWand;

    private CooldownServiceAdapter adapter;
    private UUID playerId;
    private String spellKey;

    @BeforeEach
    void setUp() {
        adapter = new CooldownServiceAdapter(mockCooldownService);
        playerId = UUID.randomUUID();
        spellKey = "test-spell";
    }

    @Test
    void testGetServiceName() {
        assertEquals("CooldownService", adapter.getServiceName());
    }

    @Test
    void testGetServiceVersion() {
        assertEquals(2, adapter.getServiceVersion().getMajor());
        assertEquals(0, adapter.getServiceVersion().getMinor());
        assertEquals(0, adapter.getServiceVersion().getPatch());
    }

    @Test
    void testIsEnabled() {
        assertTrue(adapter.isEnabled());
    }

    @Test
    void testGetHealth() {
        assertNotNull(adapter.getHealth());
    }

    @Test
    void testIsOnCooldown() {
        long nowTicks = 1000L;
        when(mockCooldownService.isOnCooldown(playerId, spellKey, nowTicks)).thenReturn(true);

        boolean result = adapter.isOnCooldown(playerId, spellKey, nowTicks);

        assertTrue(result);
        verify(mockCooldownService).isOnCooldown(playerId, spellKey, nowTicks);
    }

    @Test
    void testIsOnCooldownWithWand() {
        long nowTicks = 1000L;
        when(mockCooldownService.isOnCooldown(playerId, spellKey, nowTicks, mockWand)).thenReturn(false);

        boolean result = adapter.isOnCooldown(playerId, spellKey, nowTicks, mockWand);

        assertFalse(result);
        verify(mockCooldownService).isOnCooldown(playerId, spellKey, nowTicks, mockWand);
    }

    @Test
    void testRemaining() {
        long nowTicks = 1000L;
        long expectedRemaining = 500L;
        when(mockCooldownService.remaining(playerId, spellKey, nowTicks)).thenReturn(expectedRemaining);

        long result = adapter.remaining(playerId, spellKey, nowTicks);

        assertEquals(expectedRemaining, result);
        verify(mockCooldownService).remaining(playerId, spellKey, nowTicks);
    }

    @Test
    void testRemainingWithWand() {
        long nowTicks = 1000L;
        long expectedRemaining = 200L;
        when(mockCooldownService.remaining(playerId, spellKey, nowTicks, mockWand)).thenReturn(expectedRemaining);

        long result = adapter.remaining(playerId, spellKey, nowTicks, mockWand);

        assertEquals(expectedRemaining, result);
        verify(mockCooldownService).remaining(playerId, spellKey, nowTicks, mockWand);
    }

    @Test
    void testSet() {
        long untilTicks = 1500L;
        adapter.set(playerId, spellKey, untilTicks);

        verify(mockCooldownService).set(playerId, spellKey, untilTicks);
    }

    @Test
    void testClearAll() {
        adapter.clearAll(playerId);

        verify(mockCooldownService).clearAll(playerId);
    }

    @Test
    void testSetCooldownDisabled() {
        boolean disabled = true;
        adapter.setCooldownDisabled(playerId, mockWand, disabled);

        verify(mockCooldownService).setCooldownDisabled(playerId, mockWand, disabled);
    }

    @Test
    void testIsCooldownDisabled() {
        when(mockCooldownService.isCooldownDisabled(playerId, mockWand)).thenReturn(true);

        boolean result = adapter.isCooldownDisabled(playerId, mockWand);

        assertTrue(result);
        verify(mockCooldownService).isCooldownDisabled(playerId, mockWand);
    }

    @Test
    void testIsCooldownDisabledWithNullWand() {
        when(mockCooldownService.isCooldownDisabled(playerId, null)).thenReturn(false);

        boolean result = adapter.isCooldownDisabled(playerId, null);

        assertFalse(result);
        verify(mockCooldownService).isCooldownDisabled(playerId, null);
    }
}