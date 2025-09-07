package com.example.empirewand.api.impl;

import com.example.empirewand.core.config.ReadableConfig;
import com.example.empirewand.core.services.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfigServiceAdapter.
 * Verifies that the adapter properly wraps core ConfigService and delegates
 * calls.
 */
@ExtendWith(MockitoExtension.class)
class ConfigServiceAdapterTest {

    @Mock
    private ConfigService mockConfigService;

    @Mock
    private ReadableConfig mockReadableConfig;

    private ConfigServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ConfigServiceAdapter(mockConfigService);
    }

    @Test
    void testGetServiceName() {
        assertEquals("ConfigService", adapter.getServiceName());
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
    void testReload() {
        adapter.reload();

        verify(mockConfigService).loadConfigs();
    }

    @Test
    void testGetMainConfig() {
        when(mockConfigService.getConfig()).thenReturn(mockReadableConfig);

        var result = adapter.getMainConfig();

        assertNotNull(result);
        verify(mockConfigService).getConfig();
    }

    @Test
    void testGetSpellsConfig() {
        when(mockConfigService.getSpellsConfig()).thenReturn(mockReadableConfig);

        var result = adapter.getSpellsConfig();

        assertNotNull(result);
        verify(mockConfigService).getSpellsConfig();
    }

    @Test
    void testGetWandsConfig() {
        when(mockConfigService.getConfig()).thenReturn(mockReadableConfig);

        var result = adapter.getWandsConfig();

        assertNotNull(result);
        verify(mockConfigService).getConfig();
    }
}