package com.example.empirewand.core.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Performance monitoring utility for tracking execution times and metrics.
 * Provides lightweight timing and logging for performance-critical operations.
 * This is an instance-based service to allow for proper dependency injection and logging.
 */
public class PerformanceMonitor {

    private final AtomicLong operationCounter = new AtomicLong(0);
    private final Logger logger;

    public PerformanceMonitor(Logger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger cannot be null");
        }
        this.logger = logger;
    }

    /**
     * Records the execution time of an operation and logs it if it exceeds the
     * threshold.
     *
     * @param operationName the name of the operation being monitored
     * @param startTime     the start time in nanoseconds
     * @param thresholdMs   threshold in milliseconds above which to log
     */
    public void recordExecutionTime(String operationName, long startTime, long thresholdMs) {
        if (operationName == null || operationName.trim().isEmpty()) {
            return;
        }
        if (startTime <= 0) {
            return;
        }
        if (thresholdMs < 0) {
            thresholdMs = 0; // Ensure threshold is non-negative
        }
        
        try {
            long endTime = System.nanoTime();
            long durationNs = endTime - startTime;
            
            // Handle potential overflow
            if (durationNs < 0) {
                logger.warning(String.format(
                        "[PERF] Operation timing overflow detected for: %s", operationName));
                return;
            }
            
            long durationMs = durationNs / 1_000_000;

            if (durationMs >= thresholdMs) {
                long operationId = operationCounter.incrementAndGet();
                logger.warning(String.format(
                        "[PERF] Operation %d: %s took %d ms (threshold: %d ms)",
                        operationId, operationName, durationMs, thresholdMs));
            }
        } catch (Exception e) {
            // Log error but don't crash - performance monitoring should never break functionality
            logger.warning(String.format(
                    "[PERF] Error recording execution time for operation: %s - %s",
                    operationName, e.getMessage()));
        }
    }

    /**
     * Creates a timing context for measuring operation duration.
     *
     * @param operationName the name of the operation
     * @return a TimingContext that can be used to record completion
     */
    public TimingContext startTiming(String operationName) {
        if (operationName == null || operationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Operation name cannot be null or empty");
        }
        return new TimingContext(operationName, System.nanoTime());
    }

    /**
     * Timing context for measuring operation duration.
     */
    public class TimingContext {
        private final String operationName;
        private final long startTime;

        private TimingContext(String operationName, long startTime) {
            if (operationName == null || operationName.trim().isEmpty()) {
                throw new IllegalArgumentException("Operation name cannot be null or empty");
            }
            if (startTime <= 0) {
                throw new IllegalArgumentException("Start time must be positive");
            }
            this.operationName = operationName;
            this.startTime = startTime;
        }

        /**
         * Records the completion of the operation.
         *
         * @param thresholdMs threshold in milliseconds above which to log
         */
        public void complete(long thresholdMs) {
            if (thresholdMs < 0) {
                thresholdMs = 0; // Ensure threshold is non-negative
            }
            try {
                PerformanceMonitor.this.recordExecutionTime(operationName, startTime, thresholdMs);
            } catch (Exception e) {
                // Log error but don't crash - timing completion should never break functionality
                logger.warning(String.format(
                        "[PERF] Error completing timing for operation: %s - %s",
                        operationName, e.getMessage()));
            }
        }

        /**
         * Records the completion with a default threshold of 10ms.
         */
        public void complete() {
            complete(10);
        }
    }
}
