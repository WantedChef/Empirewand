package com.example.empirewand.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring utility for tracking execution times and metrics.
 * Provides lightweight timing and logging for performance-critical operations.
 */
public class PerformanceMonitor {

    private static final AtomicLong operationCounter = new AtomicLong(0);

    /**
     * Records the execution time of an operation and logs it if it exceeds the
     * threshold.
     *
     * @param operationName the name of the operation being monitored
     * @param startTime     the start time in nanoseconds
     * @param thresholdMs   threshold in milliseconds above which to log
     */
    public static void recordExecutionTime(String operationName, long startTime, long thresholdMs) {
        long endTime = System.nanoTime();
        long durationNs = endTime - startTime;
        long durationMs = durationNs / 1_000_000;

        if (durationMs >= thresholdMs) {
            long operationId = operationCounter.incrementAndGet();
            System.out.println(String.format(
                    "[PERF] Operation %d: %s took %d ms (threshold: %d ms)",
                    operationId, operationName, durationMs, thresholdMs));
        }
    }

    /**
     * Creates a timing context for measuring operation duration.
     *
     * @param operationName the name of the operation
     * @return a TimingContext that can be used to record completion
     */
    public static TimingContext startTiming(String operationName) {
        return new TimingContext(operationName, System.nanoTime());
    }

    /**
     * Timing context for measuring operation duration.
     */
    public static class TimingContext {
        private final String operationName;
        private final long startTime;

        private TimingContext(String operationName, long startTime) {
            this.operationName = operationName;
            this.startTime = startTime;
        }

        /**
         * Records the completion of the operation.
         *
         * @param thresholdMs threshold in milliseconds above which to log
         */
        public void complete(long thresholdMs) {
            PerformanceMonitor.recordExecutionTime(operationName, startTime, thresholdMs);
        }

        /**
         * Records the completion with a default threshold of 10ms.
         */
        public void complete() {
            complete(10);
        }
    }
}