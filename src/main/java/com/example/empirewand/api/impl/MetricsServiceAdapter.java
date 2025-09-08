package com.example.empirewand.api.impl;

import com.example.empirewand.api.MetricsService;
import com.example.empirewand.api.ServiceHealth;
import com.example.empirewand.api.Version;
import com.example.empirewand.api.common.AnyThread;
import org.jetbrains.annotations.NotNull;

/**
 * Adapter for MetricsService that wraps core MetricsService and
 * DebugMetricsService implementations.
 * Delegates all metrics operations to core services while providing API
 * contract.
 * Implements EmpireWandService base methods with defaults.
 *
 * <h2>Usage Example:</h2>
 * 
 * <pre>{@code
 * // Get the metrics service from the API
 * MetricsService metrics = EmpireWandAPI.getProvider().getMetricsService();
 *
 * // Record spell casts
 * metrics.recordSpellCast("fireball");
 * metrics.recordSpellCast("lightning-bolt", 150); // with duration
 *
 * // Record other events
 * metrics.recordFailedCast();
 * metrics.recordWandCreated();
 * metrics.recordEventProcessing(50);
 *
 * // Get metrics data
 * long totalCasts = metrics.getTotalSpellCasts();
 * double successRate = metrics.getSpellCastSuccessRate();
 * long p95Time = metrics.getSpellCastP95();
 *
 * // Get debug information
 * String debugInfo = metrics.getDebugInfo();
 * }</pre>
 *
 * @since 2.0.0
 */
public class MetricsServiceAdapter implements MetricsService {

    private final com.example.empirewand.core.services.metrics.MetricsService coreMetrics;
    private final com.example.empirewand.core.services.metrics.DebugMetricsService debugMetrics;

    /**
     * Constructor.
     *
     * @param coreMetrics  the core MetricsService to wrap
     * @param debugMetrics the core DebugMetricsService to wrap
     */
    public MetricsServiceAdapter(
            com.example.empirewand.core.services.metrics.MetricsService coreMetrics,
            com.example.empirewand.core.services.metrics.DebugMetricsService debugMetrics) {
        if (coreMetrics == null) {
            throw new IllegalArgumentException("coreMetrics cannot be null");
        }
        if (debugMetrics == null) {
            throw new IllegalArgumentException("debugMetrics cannot be null");
        }
        this.coreMetrics = coreMetrics;
        this.debugMetrics = debugMetrics;
    }

    // EmpireWandService implementations

    @Override
    public @NotNull String getServiceName() {
        return "MetricsService";
    }

    @Override
    public @NotNull Version getServiceVersion() {
        return Version.of(2, 0, 0);
    }

    @Override
    public boolean isEnabled() {
        return coreMetrics.isEnabled();
    }

    @Override
    public @NotNull ServiceHealth getHealth() {
        try {
            // Check if core services are available and functioning
            if (coreMetrics == null || debugMetrics == null) {
                return ServiceHealth.UNHEALTHY;
            }
            
            // Check if metrics collection is enabled
            if (!coreMetrics.isEnabled()) {
                return ServiceHealth.DEGRADED;
            }
            
            // Additional health checks can be added here
            // For example, check if metrics are being collected properly
            return ServiceHealth.HEALTHY;
        } catch (Exception e) {
            return ServiceHealth.UNHEALTHY;
        }
    }

    @Override
    public void reload() {
        try {
            // Clear debug metrics data on reload
            if (debugMetrics != null) {
                debugMetrics.clear();
            }
        } catch (Exception e) {
            // Log error but don't propagate - graceful degradation
            System.err.println("Failed to reload metrics service: " + e.getMessage());
        }
    }

    // MetricsService implementations

    @Override
    @AnyThread
    public void recordSpellCast(@NotNull String spellKey) {
        coreMetrics.recordSpellCast(spellKey);
    }

    @Override
    @AnyThread
    public void recordSpellCast(@NotNull String spellKey, long durationMs) {
        coreMetrics.recordSpellCast(spellKey, durationMs);
    }

    @Override
    @AnyThread
    public void recordFailedCast() {
        coreMetrics.recordFailedCast();
    }

    @Override
    @AnyThread
    public void recordEventProcessing(long durationMs) {
        coreMetrics.recordEventProcessing(durationMs);
    }

    @Override
    @AnyThread
    public void recordWandCreated() {
        coreMetrics.recordWandCreated();
    }

    @Override
    @AnyThread
    public @NotNull String getDebugInfo() {
        return coreMetrics.getDebugInfo();
    }

    @Override
    @AnyThread
    public long getSpellCastP95() {
        return debugMetrics.getSpellCastP95();
    }

    @Override
    @AnyThread
    public long getEventProcessingP95() {
        return debugMetrics.getEventProcessingP95();
    }

    @Override
    @AnyThread
    public long getTotalSpellCasts() {
        return debugMetrics.getTotalSpellCasts();
    }

    @Override
    @AnyThread
    public long getTotalFailedCasts() {
        return debugMetrics.getTotalFailedCasts();
    }

    @Override
    @AnyThread
    public double getSpellCastSuccessRate() {
        return debugMetrics.getSpellCastSuccessRate();
    }

    @Override
    @AnyThread
    public void clear() {
        debugMetrics.clear();
    }

    // Additional API-specific methods can be added here if needed beyond core

}