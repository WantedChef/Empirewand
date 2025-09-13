package nl.wantedchef.empirewand.core.util;

import java.util.concurrent.TimeUnit;

/**
 * Context for timing operations with automatic resource management.
 * Implements AutoCloseable for use in try-with-resources blocks.
 */
public class TimingContext implements AutoCloseable {
    
    private final String operationName;
    private final long thresholdMs;
    private final long startTime;
    private final AdvancedPerformanceMonitor monitor;
    private boolean closed = false;
    private boolean observed = false;

    /**
     * Creates a new timing context.
     * 
     * @param operationName The name of the operation being timed
     * @param thresholdMs The threshold in milliseconds for warnings
     * @param monitor The performance monitor to report to
     */
    public TimingContext(String operationName, long thresholdMs, AdvancedPerformanceMonitor monitor) {
        this.operationName = operationName;
        this.thresholdMs = thresholdMs;
        this.startTime = System.nanoTime();
        this.monitor = monitor;
    }

    /**
     * Marks this timing context as observed to satisfy static analysis.
     * This is used to prevent warnings about unused variables.
     */
    public void observe() {
        this.observed = true;
    }

    /**
     * Gets the elapsed time in nanoseconds.
     * 
     * @return The elapsed time in nanoseconds
     */
    public long getElapsedNanos() {
        return System.nanoTime() - startTime;
    }

    /**
     * Gets the elapsed time in milliseconds.
     * 
     * @return The elapsed time in milliseconds
     */
    public long getElapsedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(getElapsedNanos());
    }

    /**
     * Gets the start time in milliseconds since epoch.
     * 
     * @return The start time in milliseconds since epoch
     */
    public long getStartTimeMillis() {
        return TimeUnit.NANOSECONDS.toMillis(startTime);
    }

    /**
     * Gets the operation name being timed.
     * 
     * @return The operation name
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * Gets the warning threshold in milliseconds.
     * 
     * @return The threshold in milliseconds
     */
    public long getThresholdMs() {
        return thresholdMs;
    }

    /**
     * Checks if this timing context has been observed.
     * 
     * @return true if observed, false otherwise
     */
    public boolean isObserved() {
        return observed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        long elapsedMs = getElapsedMillis();
        
        // Report the timing to the monitor
        if (monitor != null) {
            monitor.recordTiming(operationName, elapsedMs);
            
            // Log warning if operation exceeded threshold
            if (elapsedMs > thresholdMs) {
                monitor.logSlowOperation(operationName, elapsedMs, thresholdMs);
            }
        }
    }

    /**
     * Creates a simple timing context without a monitor.
     * 
     * @param operationName The name of the operation
     * @return A new timing context
     */
    public static TimingContext simple(String operationName) {
        return new TimingContext(operationName, Long.MAX_VALUE, null);
    }

    /**
     * Creates a timing context with a default threshold.
     * 
     * @param operationName The name of the operation
     * @param monitor The performance monitor
     * @return A new timing context
     */
    public static TimingContext withMonitor(String operationName, AdvancedPerformanceMonitor monitor) {
        return new TimingContext(operationName, 100L, monitor); // Default 100ms threshold
    }
}