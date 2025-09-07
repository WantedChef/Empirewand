package com.example.empirewand.api.impl;

import com.example.empirewand.api.EmpireWandService;
import com.example.empirewand.api.MetricsService;
import com.example.empirewand.api.ServiceHealth;
import com.example.empirewand.api.Version;
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
        return ServiceHealth.HEALTHY; // Default healthy; can add core health check if implemented
    }

    @Override
    public void reload() {
        // No-op for metrics; core doesn't have reload, but can clear debug metrics if
        // needed
        // debugMetrics.clear(); // Optional
    }

    // MetricsService implementations

    @Override
    public void recordSpellCast(@NotNull String spellKey) {
        coreMetrics.recordSpellCast(spellKey);
    }

    @Override
    public void recordSpellCast(@NotNull String spellKey, long durationMs) {
        coreMetrics.recordSpellCast(spellKey, durationMs);
    }

    @Override
    public void recordFailedCast() {
        coreMetrics.recordFailedCast();
    }

    @Override
    public void recordEventProcessing(long durationMs) {
        coreMetrics.recordEventProcessing(durationMs);
    }

    @Override
    public void recordWandCreated() {
        coreMetrics.recordWandCreated();
    }

    @Override
    public @NotNull String getDebugInfo() {
        return coreMetrics.getDebugInfo();
    }

    @Override
    public long getSpellCastP95() {
        return debugMetrics.getSpellCastP95();
    }

    @Override
    public long getEventProcessingP95() {
        return debugMetrics.getEventProcessingP95();
    }

    @Override
    public long getTotalSpellCasts() {
        return debugMetrics.getTotalSpellCasts();
    }

    @Override
    public long getTotalFailedCasts() {
        return debugMetrics.getTotalFailedCasts();
    }

    @Override
    public double getSpellCastSuccessRate() {
        return debugMetrics.getSpellCastSuccessRate();
    }

    @Override
    public void clear() {
        debugMetrics.clear();
    }

    // Additional API-specific methods can be added here if needed beyond core

}