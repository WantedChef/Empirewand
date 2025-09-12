package nl.wantedchef.empirewand.core.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Performance monitoring utility for tracking execution times and metrics.
 * Provides lightweight timing and logging for performance-critical operations.
 * This is an instance-based service to allow for proper dependency injection
 * and logging.
 */
public class PerformanceMonitor {

    private final AtomicLong operationCounter = new AtomicLong(0);
    private final Logger logger;

    // Enhanced metrics collection with size limits to prevent memory leaks
    private final ConcurrentHashMap<String, AtomicLong> operationCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> totalExecutionTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> maxExecutionTimes = new ConcurrentHashMap<>();

    // Maximum number of unique operation names to store (prevents unbounded growth)
    private static final int MAX_OPERATIONS = 1000;
    private static final long CLEANUP_INTERVAL = 10000; // Clean up every 10k operations

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
     * @param startTime the start time in nanoseconds
     * @param thresholdMs threshold in milliseconds above which to log
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
            // Clean up old metrics periodically
            long count = operationCounter.incrementAndGet();
            if (count % CLEANUP_INTERVAL == 0) {
                cleanupOldMetrics();
            }

            long endTime = System.nanoTime();
            long durationNs = endTime - startTime;

            // Handle potential overflow
            if (durationNs < 0) {
                logger.warning(String.format(
                        "[PERF] Operation timing overflow detected for: %s", operationName));
                return;
            }

            long durationMs = durationNs / 1_000_000;

            // Update metrics collections with size limit
            if (operationCounters.size() < MAX_OPERATIONS) {
                updateMetrics(operationName, durationMs);
            } else {
                // If at limit, only update existing operations
                if (operationCounters.containsKey(operationName)) {
                    updateMetrics(operationName, durationMs);
                }
            }

            if (durationMs >= thresholdMs) {
                logger.warning(String.format(
                        "[PERF] Operation %d: %s took %d ms (threshold: %d ms)",
                        count, operationName, durationMs, thresholdMs));
            }
        } catch (Exception e) {
            // Log error but don't crash - performance monitoring should never break
            // functionality
            logger.warning(String.format(
                    "[PERF] Error recording execution time for operation: %s - %s",
                    operationName, e.getMessage()));
        }
    }

    /**
     * Updates the metrics collections with the execution time for an operation.
     *
     * @param operationName the name of the operation
     * @param durationMs the duration in milliseconds
     */
    private void updateMetrics(String operationName, long durationMs) {
        // Increment operation counter
        operationCounters.computeIfAbsent(operationName, k -> new AtomicLong(0)).incrementAndGet();

        // Add to total execution time
        totalExecutionTimes.computeIfAbsent(operationName, k -> new AtomicLong(0)).addAndGet(durationMs);

        // Update maximum execution time
        maxExecutionTimes.computeIfAbsent(operationName, k -> new AtomicLong(0))
                .accumulateAndGet(durationMs, Math::max);
    }

    /**
     * Cleans up old metrics to prevent memory leaks. Removes operations that
     * haven't been used recently.
     */
    private void cleanupOldMetrics() {
        try {
            // Simple cleanup: if we have too many operations, clear the least used ones
            if (operationCounters.size() > MAX_OPERATIONS * 0.8) {
                // Remove entries that have very low usage
                operationCounters.entrySet().removeIf(entry -> entry.getValue().get() < 10);
                totalExecutionTimes.entrySet().removeIf(entry
                        -> !operationCounters.containsKey(entry.getKey()));
                maxExecutionTimes.entrySet().removeIf(entry
                        -> !operationCounters.containsKey(entry.getKey()));
            }
        } catch (Exception e) {
            logger.warning("[PERF] Error during metrics cleanup: " + e.getMessage());
        }
    }

    /**
     * Gets the average execution time for an operation.
     *
     * @param operationName the name of the operation
     * @return the average execution time in milliseconds, or 0 if not found
     */
    public double getAverageExecutionTime(String operationName) {
        if (operationName == null) {
            return 0.0;
        }

        try {
            AtomicLong count = operationCounters.get(operationName);
            AtomicLong totalTime = totalExecutionTimes.get(operationName);

            if (count == null || totalTime == null || count.get() == 0) {
                return 0.0;
            }

            return (double) totalTime.get() / count.get();
        } catch (Exception e) {
            logger.warning("[PERF] Error getting average execution time: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Gets the maximum execution time for an operation.
     *
     * @param operationName the name of the operation
     * @return the maximum execution time in milliseconds, or 0 if not found
     */
    public long getMaxExecutionTime(String operationName) {
        if (operationName == null) {
            return 0;
        }

        try {
            AtomicLong maxTime = maxExecutionTimes.get(operationName);
            return maxTime != null ? maxTime.get() : 0;
        } catch (Exception e) {
            logger.warning("[PERF] Error getting max execution time: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Clears all performance metrics. Useful for testing or memory cleanup.
     */
    public void clearMetrics() {
        try {
            operationCounters.clear();
            totalExecutionTimes.clear();
            maxExecutionTimes.clear();
            operationCounter.set(0);
        } catch (Exception e) {
            logger.warning("[PERF] Error clearing metrics: " + e.getMessage());
        }
    }

    /**
     * Gets the current number of tracked operations.
     *
     * @return the number of unique operations being tracked
     */
    public int getTrackedOperationCount() {
        return operationCounters.size();
    }

    /**
     * Timing context for measuring execution time.
     */
    public static class TimingContext implements AutoCloseable {

        private final PerformanceMonitor monitor;
        private final String operationName;
        private final long thresholdMs;
        private final long startTime;

        public TimingContext(PerformanceMonitor monitor, String operationName, long thresholdMs) {
            this.monitor = monitor;
            this.operationName = operationName;
            this.thresholdMs = thresholdMs;
            this.startTime = System.nanoTime();
        }

        /**
         * Explicitly marks this timing context as observed/used. This is a
         * no-op method whose sole purpose is to allow callers to reference the
         * timing variable (e.g. timing.observe();) so static analysis tools do
         * not flag the resource variable in try-with-resources blocks as unused
         * while still retaining the concise "try (var timing =
         * monitor.startTiming(...))" form.
         */
        public void observe() {
            // intentionally empty
        }

        @Override
        public void close() {
            if (monitor != null && operationName != null) {
                monitor.recordExecutionTime(operationName, startTime, thresholdMs);
            }
        }
    }

    /**
     * Starts timing an operation.
     *
     * @param operationName the name of the operation
     * @param thresholdMs the threshold in milliseconds
     * @return a timing context that will automatically record the time when
     * closed
     */
    public TimingContext startTiming(String operationName, long thresholdMs) {
        return new TimingContext(this, operationName, thresholdMs);
    }
}
