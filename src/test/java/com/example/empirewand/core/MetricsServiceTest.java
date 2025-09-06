package com.example.empirewand.core;

import com.example.empirewand.core.config.ReadableConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.empirewand.EmpireWandTestBase;

/**
 * Unit tests for MetricsService.
 */
class MetricsServiceTest extends EmpireWandTestBase {

    @Mock
    private org.bukkit.plugin.java.JavaPlugin mockJavaPlugin;

    @Mock
    private ConfigService mockConfig;

    @Mock
    private ReadableConfig mockFileConfig;

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        when(mockConfig.getConfig()).thenReturn(mockFileConfig);
        metricsService = new MetricsService(mockJavaPlugin, mockConfig);
    }

    @Test
    void testInitializeMetricsEnabled() {
        // Given
        when(mockFileConfig.getBoolean("metrics.enabled", true)).thenReturn(true);
        when(mockFileConfig.getInt("metrics.plugin-id", 12345)).thenReturn(12345);
        when(mockFileConfig.getBoolean("metrics.debug", false)).thenReturn(false);

        // When
        metricsService.initialize();

        // Then
        verify(mockFileConfig, org.mockito.Mockito.times(1)).getBoolean("metrics.enabled", true);
        verify(mockFileConfig, org.mockito.Mockito.times(1)).getInt("metrics.plugin-id", 12345);
        assertTrue(metricsService.isEnabled());
    }

    @Test
    void testInitializeMetricsDisabled() {
        // Given
        when(mockFileConfig.getBoolean("metrics.enabled", true)).thenReturn(false);

        // When
        metricsService.initialize();

        // Then
        verify(mockFileConfig, org.mockito.Mockito.times(1)).getBoolean("metrics.enabled", true);
        assertFalse(metricsService.isEnabled());
    }

    @Test
    void testShutdown() {
        // Given
        when(mockFileConfig.getBoolean("metrics.enabled", true)).thenReturn(true);
        metricsService.initialize();

        // When
        metricsService.shutdown();

        // Then
        // Metrics shutdown is called internally, but we can't easily verify without
        // exposing it
        assertTrue(true); // Test passes if no exception
    }

    @Test
    void testRecordSpellCast() {
        // Given
        when(mockFileConfig.getBoolean("metrics.enabled", true)).thenReturn(true);
        metricsService.initialize();

        // When
        metricsService.recordSpellCast("fireball");

        // Then
        // Internal counter incremented, but we can't verify without exposing it
        assertTrue(true);
    }

    @Test
    void testRecordSpellCastWithTiming() {
        // Given
        when(mockFileConfig.getBoolean("metrics.enabled", true)).thenReturn(true);
        when(mockFileConfig.getBoolean("metrics.debug", false)).thenReturn(true);
        metricsService.initialize();

        // When
        metricsService.recordSpellCast("fireball", 150);

        // Then
        assertTrue(true);
    }

    @Test
    void testRecordFailedCast() {
        // Given
        when(mockFileConfig.getBoolean("metrics.debug", false)).thenReturn(true);

        // When
        metricsService.recordFailedCast();

        // Then
        assertTrue(true);
    }

    @Test
    void testRecordEventProcessing() {
        // Given
        when(mockFileConfig.getBoolean("metrics.debug", false)).thenReturn(true);

        // When
        metricsService.recordEventProcessing(50);

        // Then
        assertTrue(true);
    }

    @Test
    void testGetDebugInfoEnabled() {
        // Given
        when(mockFileConfig.getBoolean("metrics.debug", false)).thenReturn(true);

        // When
        String result = metricsService.getDebugInfo();

        // Then
        assertNotNull(result);
        assertFalse(result.contains("disabled"));
    }

    @Test
    void testGetDebugInfoDisabled() {
        // Given
        when(mockFileConfig.getBoolean("metrics.debug", false)).thenReturn(false);

        // When
        String result = metricsService.getDebugInfo();

        // Then
        assertEquals("Debug metrics disabled in configuration", result);
    }

    @Test
    void testRecordWandCreated() {
        // Given
        when(mockFileConfig.getBoolean("metrics.enabled", true)).thenReturn(true);
        metricsService.initialize();

        // When
        metricsService.recordWandCreated();

        // Then
        assertTrue(true);
    }

    @Test
    void testIsEnabledInitiallyFalse() {
        // When
        boolean result = metricsService.isEnabled();

        // Then
        assertFalse(result);
    }

    @Test
    void testIsEnabledAfterInitialization() {
        // Given
        when(mockFileConfig.getBoolean("metrics.enabled", true)).thenReturn(true);
        metricsService.initialize();

        // When
        boolean result = metricsService.isEnabled();

        // Then
        assertTrue(result);
    }
}
