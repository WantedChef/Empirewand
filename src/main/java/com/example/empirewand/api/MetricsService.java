package com.example.empirewand.api;

import com.example.empirewand.api.common.AnyThread;

/**
 * Service for collecting and analyzing usage metrics.
 * Provides spell usage statistics, player metrics, and system analytics.
 *
 * @since 2.0.0
 */
public interface MetricsService extends EmpireWandService {

    /**
     * Records a spell cast event for metrics.
     *
     * @param spellKey the key of the spell that was cast
     */
    @AnyThread
    void recordSpellCast(@org.jetbrains.annotations.NotNull String spellKey);

    /**
     * Records a spell cast event with timing for performance metrics.
     *
     * @param spellKey   the key of the spell that was cast
     * @param durationMs the duration of the spell cast in milliseconds
     */
    @AnyThread
    void recordSpellCast(@org.jetbrains.annotations.NotNull String spellKey, long durationMs);

    /**
     * Records a failed spell cast.
     */
    @AnyThread
    void recordFailedCast();

    /**
     * Records event processing timing.
     *
     * @param durationMs the duration of event processing in milliseconds
     */
    @AnyThread
    void recordEventProcessing(long durationMs);

    /**
     * Records a wand creation event.
     */
    @AnyThread
    void recordWandCreated();

    /**
     * Gets debug information about metrics.
     *
     * @return formatted debug information string
     */
    @AnyThread
    @org.jetbrains.annotations.NotNull
    String getDebugInfo();

    /**
     * Gets the P95 spell cast time.
     *
     * @return the 95th percentile spell cast time in milliseconds
     */
    @AnyThread
    long getSpellCastP95();

    /**
     * Gets the P95 event processing time.
     *
     * @return the 95th percentile event processing time in milliseconds
     */
    @AnyThread
    long getEventProcessingP95();

    /**
     * Gets the total number of spell casts.
     *
     * @return the total number of spell casts
     */
    @AnyThread
    long getTotalSpellCasts();

    /**
     * Gets the total number of failed casts.
     *
     * @return the total number of failed casts
     */
    @AnyThread
    long getTotalFailedCasts();

    /**
     * Gets the spell cast success rate as a percentage.
     *
     * @return the success rate as a percentage (0.0 to 100.0)
     */
    @AnyThread
    double getSpellCastSuccessRate();

    /**
     * Checks if metrics collection is enabled.
     *
     * @return true if metrics are enabled, false otherwise
     */
    @AnyThread
    @Override
    boolean isEnabled();

    /**
     * Clears all metrics data.
     */
    @AnyThread
    void clear();
}