package com.example.empirewand.api.impl;

import com.example.empirewand.core.services.metrics.DebugMetricsService;
import com.example.empirewand.core.services.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MetricsServiceAdapter.
 * Verifies that the adapter properly wraps core MetricsService and
 * DebugMetricsService.
 */
@ExtendWith(MockitoExtension.class)
class MetricsServiceAdapterTest {

    @Mock
    private MetricsService mockMetricsService;

    @Mock
    private DebugMetricsService mockDebugMetricsService;

    private MetricsServiceAdapter adapter;
    private String spellKey;

    @BeforeEach
    void setUp() {
        adapter = new MetricsServiceAdapter(mockMetricsService, mockDebugMetricsService);
        spellKey = "test-spell";
    }

    @Test
    void testGetServiceName() {
        assertEquals("MetricsService", adapter.getServiceName());
    }

    @Test
    void testGetServiceVersion() {
        assertEquals(2, adapter.getServiceVersion().getMajor());
        assertEquals(0, adapter.getServiceVersion().getMinor());
        assertEquals(0, adapter.getServiceVersion().getPatch());
    }

    @Test
    void testIsEnabled() {
        when(mockMetricsService.isEnabled()).thenReturn(true);

        assertTrue(adapter.isEnabled());
        verify(mockMetricsService).isEnabled();
    }

    @Test
    void testGetHealth() {
        assertNotNull(adapter.getHealth());
    }

    @Test
    void testRecordSpellCast() {
        adapter.recordSpellCast(spellKey);

        verify(mockMetricsService).recordSpellCast(spellKey);
    }

    @Test
    void testRecordSpellCastWithDuration() {
        long durationMs = 150L;
        adapter.recordSpellCast(spellKey, durationMs);

        verify(mockMetricsService).recordSpellCast(spellKey, durationMs);
    }

    @Test
    void testRecordFailedCast() {
        adapter.recordFailedCast();

        verify(mockMetricsService).recordFailedCast();
    }

    @Test
    void testRecordEventProcessing() {
        long durationMs = 50L;
        adapter.recordEventProcessing(durationMs);

        verify(mockMetricsService).recordEventProcessing(durationMs);
    }

    @Test
    void testRecordWandCreated() {
        adapter.recordWandCreated();

        verify(mockMetricsService).recordWandCreated();
    }

    @Test
    void testGetDebugInfo() {
        String expectedInfo = "Debug metrics info";
        when(mockMetricsService.getDebugInfo()).thenReturn(expectedInfo);

        String result = adapter.getDebugInfo();

        assertEquals(expectedInfo, result);
        verify(mockMetricsService).getDebugInfo();
    }

    @Test
    void testGetSpellCastP95() {
        long expectedP95 = 200L;
        when(mockDebugMetricsService.getSpellCastP95()).thenReturn(expectedP95);

        long result = adapter.getSpellCastP95();

        assertEquals(expectedP95, result);
        verify(mockDebugMetricsService).getSpellCastP95();
    }

    @Test
    void testGetEventProcessingP95() {
        long expectedP95 = 75L;
        when(mockDebugMetricsService.getEventProcessingP95()).thenReturn(expectedP95);

        long result = adapter.getEventProcessingP95();

        assertEquals(expectedP95, result);
        verify(mockDebugMetricsService).getEventProcessingP95();
    }

    @Test
    void testGetTotalSpellCasts() {
        long expectedTotal = 1000L;
        when(mockDebugMetricsService.getTotalSpellCasts()).thenReturn(expectedTotal);

        long result = adapter.getTotalSpellCasts();

        assertEquals(expectedTotal, result);
        verify(mockDebugMetricsService).getTotalSpellCasts();
    }

    @Test
    void testGetTotalFailedCasts() {
        long expectedTotal = 50L;
        when(mockDebugMetricsService.getTotalFailedCasts()).thenReturn(expectedTotal);

        long result = adapter.getTotalFailedCasts();

        assertEquals(expectedTotal, result);
        verify(mockDebugMetricsService).getTotalFailedCasts();
    }

    @Test
    void testGetSpellCastSuccessRate() {
        double expectedRate = 95.2;
        when(mockDebugMetricsService.getSpellCastSuccessRate()).thenReturn(expectedRate);

        double result = adapter.getSpellCastSuccessRate();

        assertEquals(expectedRate, result);
        verify(mockDebugMetricsService).getSpellCastSuccessRate();
    }

    @Test
    void testClear() {
        adapter.clear();

        verify(mockDebugMetricsService).clear();
    }
}