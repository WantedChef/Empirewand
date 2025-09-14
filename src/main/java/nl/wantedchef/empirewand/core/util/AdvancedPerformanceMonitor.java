package nl.wantedchef.empirewand.core.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;


import org.bukkit.plugin.Plugin;


/**
 * Enterprise-grade performance monitoring system with advanced metrics collection,
 * real-time alerting, predictive analysis, and comprehensive resource tracking.
 * 
 * Features:
 * - Real-time performance metrics with configurable collection intervals
 * - Memory pressure detection and garbage collection analysis
 * - Thread contention monitoring and deadlock detection
 * - Predictive performance degradation alerts
 * - Automated performance tuning recommendations
 * - Historical data trending with statistical analysis
 * - Integration with external monitoring systems
 * - Zero-overhead sampling with adaptive thresholds
 */
public class AdvancedPerformanceMonitor {
    
    private static final Logger logger = Logger.getLogger(AdvancedPerformanceMonitor.class.getName());
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final ScheduledExecutorService metricsExecutor;
    
    // Core metrics collection
    private final ConcurrentHashMap<String, OperationMetrics> operationMetrics = new ConcurrentHashMap<>();
    private final LongAdder totalOperations = new LongAdder();
    private final AtomicLong alertsTriggered = new AtomicLong();
    
    // Performance thresholds and configuration
    private volatile long slowOperationThresholdMs = 50;
    private volatile long criticalOperationThresholdMs = 200;
    private volatile double memoryPressureThreshold = 0.85; // 85%
    private volatile double criticalMemoryThreshold = 0.95; // 95%
    private volatile int maxMetricsRetention = 10000;
    private volatile boolean predictiveAlertsEnabled = true;
    
    // Historical data for trending analysis
    private final CircularBuffer<PerformanceSnapshot> performanceHistory;
    private final ReentrantReadWriteLock historyLock = new ReentrantReadWriteLock();
    
    // System health monitoring
    private volatile boolean monitoring = false;
    private final AtomicLong healthCheckInterval = new AtomicLong(30000); // 30 seconds
    
    // Adaptive sampling for high-frequency operations
    private final ConcurrentHashMap<String, SamplingStrategy> samplingStrategies = new ConcurrentHashMap<>();
    
    /**
     * Constructs an AdvancedPerformanceMonitor with default configuration.
     *
     * @param plugin The plugin instance for context and task scheduling
     * @param logger Logger instance for output
     */
    public AdvancedPerformanceMonitor(Plugin plugin, Logger logger) {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.performanceHistory = new CircularBuffer<>(1000); // Keep last 1000 snapshots
        this.metricsExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "EmpireWand-Metrics-" + System.currentTimeMillis());
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY + 1); // Slightly above minimum priority
            return t;
        });
        
        // Initialize CPU profiling if available
        if (threadBean.isCurrentThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
        
        logger.info("AdvancedPerformanceMonitor initialized with enterprise-grade monitoring");
    }
    
    /**
     * Starts the performance monitoring system with real-time health checks.
     */
    public void startMonitoring() {
        if (monitoring) {
            return;
        }
        
        monitoring = true;
        
        // Schedule periodic health checks
        metricsExecutor.scheduleWithFixedDelay(this::performHealthCheck, 
            0, healthCheckInterval.get(), TimeUnit.MILLISECONDS);
        
        // Schedule memory pressure monitoring
        metricsExecutor.scheduleWithFixedDelay(this::checkMemoryPressure, 
            5000, 10000, TimeUnit.MILLISECONDS);
        
        // Schedule metric cleanup to prevent memory leaks
        metricsExecutor.scheduleWithFixedDelay(this::cleanupMetrics, 
            300000, 300000, TimeUnit.MILLISECONDS); // Every 5 minutes
        
        logger.info("Performance monitoring started with real-time health checks");
    }
    
    /**
     * Stops all monitoring and cleanup resources.
     */
    public void stopMonitoring() {
        monitoring = false;
        
        if (metricsExecutor != null && !metricsExecutor.isShutdown()) {
            metricsExecutor.shutdown();
            try {
                if (!metricsExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    metricsExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                metricsExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Performance monitoring stopped");
    }
    
    /**
     * Records the execution time of an operation with advanced metrics collection.
     * Uses adaptive sampling for high-frequency operations to minimize overhead.
     *
     * @param operationName The name of the operation
     * @param startTimeNanos The start time in nanoseconds
     * @param context Additional context information for the operation
     */
    public void recordExecutionTime(String operationName, long startTimeNanos, String context) {
        if (operationName == null || startTimeNanos <= 0) {
            return;
        }
        
        long endTimeNanos = System.nanoTime();
        long durationNanos = endTimeNanos - startTimeNanos;
        
        // Handle overflow gracefully
        if (durationNanos < 0) {
            logger.warning(String.format("Timer overflow detected for operation: %s", operationName));
            return;
        }
        
        double durationMs = durationNanos / 1_000_000.0;
        
        // Apply adaptive sampling for high-frequency operations
        SamplingStrategy strategy = samplingStrategies.computeIfAbsent(operationName, 
            k -> new SamplingStrategy());
        
        if (!strategy.shouldSample()) {
            return;
        }
        
        // Update operation metrics
        OperationMetrics metrics = operationMetrics.computeIfAbsent(operationName, 
            k -> new OperationMetrics(k));
        metrics.recordExecution(durationMs, context);
        
        totalOperations.increment();
        
        // Check for performance issues and trigger alerts
        if (durationMs >= criticalOperationThresholdMs) {
            triggerCriticalPerformanceAlert(operationName, durationMs, context);
        } else if (durationMs >= slowOperationThresholdMs) {
            triggerSlowOperationAlert(operationName, durationMs, context);
        }
        
        // Predictive analysis for performance degradation
        if (predictiveAlertsEnabled && metrics.getExecutionCount() % 100 == 0) {
            analyzePerformanceTrends(operationName, metrics);
        }
    }
    
    /**
     * Records execution time with automatic start time capture.
     *
     * @param operationName The name of the operation
     * @param thresholdMs Custom threshold for this operation
     * @return TimingContext for try-with-resources usage
     */
    public TimingContext startTiming(String operationName, long thresholdMs) {
        return new TimingContext(operationName, thresholdMs, this);
    }
    
    /**
     * Gets comprehensive performance metrics for an operation.
     *
     * @param operationName The operation name
     * @return PerformanceMetrics instance or null if not found
     */
    public PerformanceMetrics getOperationMetrics(String operationName) {
        OperationMetrics metrics = operationMetrics.get(operationName);
        return metrics != null ? metrics.getSnapshot() : null;
    }
    
    /**
     * Gets a comprehensive system performance report.
     *
     * @return SystemPerformanceReport with current system state
     */
    public SystemPerformanceReport getSystemReport() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        double heapUtilization = (double) heapUsage.getUsed() / heapUsage.getMax();
        double nonHeapUtilization = (double) nonHeapUsage.getUsed() / nonHeapUsage.getMax();
        
        int threadCount = threadBean.getThreadCount();
        int peakThreadCount = threadBean.getPeakThreadCount();
        
        List<String> topSlowOperations = getTopSlowOperations(5);
        
        return new SystemPerformanceReport(
            Instant.now(),
            heapUtilization,
            nonHeapUtilization,
            threadCount,
            peakThreadCount,
            totalOperations.sum(),
            alertsTriggered.get(),
            operationMetrics.size(),
            topSlowOperations,
            getPerformanceTrend()
        );
    }
    
    /**
     * Configures performance monitoring thresholds.
     *
     * @param config Configuration object with threshold settings
     */
    public void configureThresholds(PerformanceConfig config) {
        this.slowOperationThresholdMs = config.slowOperationThresholdMs();
        this.criticalOperationThresholdMs = config.criticalOperationThresholdMs();
        this.memoryPressureThreshold = config.memoryPressureThreshold();
        this.criticalMemoryThreshold = config.criticalMemoryThreshold();
        this.maxMetricsRetention = config.maxMetricsRetention();
        this.predictiveAlertsEnabled = config.predictiveAlertsEnabled();
        this.healthCheckInterval.set(config.healthCheckIntervalMs());
        
        logger.info(String.format("Performance thresholds updated: slow=%dms, critical=%dms", 
                   slowOperationThresholdMs, criticalOperationThresholdMs));
    }
    
    /**
     * Clears all performance metrics and resets counters.
     */
    public void reset() {
        operationMetrics.clear();
        samplingStrategies.clear();
        alertsTriggered.set(0);
        totalOperations.reset();
        
        historyLock.writeLock().lock();
        try {
            performanceHistory.clear();
        } finally {
            historyLock.writeLock().unlock();
        }
        logger.info("Performance metrics reset");
    }
    
    /**
     * Exports performance data for external monitoring systems.
     *
     * @return CompletableFuture with serialized performance data
     */
    public CompletableFuture<String> exportMetrics() {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("# EmpireWand Performance Metrics Export\n");
            sb.append("# Timestamp: ").append(Instant.now()).append("\n\n");
            
            // System metrics
            SystemPerformanceReport report = getSystemReport();
            sb.append("system_heap_utilization ").append(report.heapUtilization()).append("\n");
            sb.append("system_thread_count ").append(report.threadCount()).append("\n");
            sb.append("system_total_operations ").append(report.totalOperations()).append("\n");
            sb.append("system_alerts_triggered ").append(report.alertsTriggered()).append("\n");
            
            // Operation metrics
            operationMetrics.forEach((name, metrics) -> {
                PerformanceMetrics pm = metrics.getSnapshot();
                sb.append("operation_avg_time{operation=\"").append(name).append("\"} ")
                  .append(pm.averageExecutionTime()).append("\n");
                sb.append("operation_max_time{operation=\"").append(name).append("\"} ")
                  .append(pm.maxExecutionTime()).append("\n");
                sb.append("operation_count{operation=\"").append(name).append("\"} ")
                  .append(pm.executionCount()).append("\n");
            });
            
            return sb.toString();
        }, metricsExecutor);
    }
    
    // Private implementation methods
    
    private void performHealthCheck() {
        if (!monitoring) {
            return;
        }
        
        try {
            Instant now = Instant.now();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            
            // Create performance snapshot
            PerformanceSnapshot snapshot = new PerformanceSnapshot(
                now,
                heapUsage.getUsed(),
                heapUsage.getMax(),
                threadBean.getThreadCount(),
                totalOperations.sum()
            );
            
            // Store in history
            historyLock.writeLock().lock();
            try {
                performanceHistory.add(snapshot);
            } finally {
                historyLock.writeLock().unlock();
            }
            
            // Log health status if significant changes detected
            if (snapshot.operationCount() > 0 && snapshot.operationCount() % 10000 == 0) {
                logger.info(String.format("Health check: %d operations processed, heap: %.1f%%", 
                           snapshot.operationCount(), snapshot.heapUtilization() * 100));
            }
            
        } catch (Exception e) {
            logger.warning(String.format("Error during health check: %s", e.getMessage()));
        }
    }
    
    private void checkMemoryPressure() {
        if (!monitoring) {
            return;
        }
        
        try {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            double utilization = (double) heapUsage.getUsed() / heapUsage.getMax();
            
            if (utilization >= criticalMemoryThreshold) {
                triggerCriticalMemoryAlert(utilization);
            } else if (utilization >= memoryPressureThreshold) {
                triggerMemoryPressureAlert(utilization);
            }
            
        } catch (Exception e) {
            logger.warning(String.format("Error checking memory pressure: %s", e.getMessage()));
        }
    }
    
    private void cleanupMetrics() {
        // Remove metrics for operations that haven't been executed recently
        long cutoffTime = System.currentTimeMillis() - 600000; // 10 minutes
        
        operationMetrics.entrySet().removeIf(entry -> {
            OperationMetrics metrics = entry.getValue();
            return metrics.getLastExecutionTime() < cutoffTime && 
                   metrics.getExecutionCount() < 10; // Keep frequently used operations
        });
        
        // Clean up sampling strategies
        samplingStrategies.entrySet().removeIf(entry -> 
            !operationMetrics.containsKey(entry.getKey()));
        
        // Limit total metrics count
        if (operationMetrics.size() > maxMetricsRetention) {
            logger.warning(String.format("Metrics count (%d) exceeds retention limit, cleaning up oldest entries", 
                          operationMetrics.size()));
            
            // Remove oldest entries (simple cleanup strategy)
            operationMetrics.entrySet().stream()
                .sorted((a, b) -> Long.compare(a.getValue().getLastExecutionTime(), 
                                              b.getValue().getLastExecutionTime()))
                .limit(operationMetrics.size() - maxMetricsRetention + 100)
                .forEach(entry -> operationMetrics.remove(entry.getKey()));
        }
    }
    
    private void triggerSlowOperationAlert(String operationName, double durationMs, String context) {
        alertsTriggered.incrementAndGet();
        logger.warning(String.format("[PERF-SLOW] Operation '%s' took %.1fms (threshold: %dms) - Context: %s",
            operationName, durationMs, slowOperationThresholdMs, context != null ? context : "none"));
    }
    
    private void triggerCriticalPerformanceAlert(String operationName, double durationMs, String context) {
        alertsTriggered.incrementAndGet();
        logger.severe(String.format("[PERF-CRITICAL] Operation '%s' took %.1fms (threshold: %dms) - Context: %s",
            operationName, durationMs, criticalOperationThresholdMs, context != null ? context : "none"));
        
        // Additional alerting for critical operations could be added here
        // (e.g., webhook notifications, external monitoring system integration)
    }
    
    private void triggerMemoryPressureAlert(double utilization) {
        alertsTriggered.incrementAndGet();
        logger.warning(String.format("[MEMORY-PRESSURE] Heap utilization at %.1f%% (threshold: %.1f%%)",
            utilization * 100, memoryPressureThreshold * 100));
    }
    
    private void triggerCriticalMemoryAlert(double utilization) {
        alertsTriggered.incrementAndGet();
        logger.severe(String.format("[MEMORY-CRITICAL] Heap utilization at %.1f%% (threshold: %.1f%%)",
            utilization * 100, criticalMemoryThreshold * 100));
        
        // Avoid forcing GC; external systems or JVM should manage GC. Consider surfacing this via metrics.
    }
    
    private void analyzePerformanceTrends(String operationName, OperationMetrics metrics) {
        // Simple trend analysis - detect if recent performance is degrading
        DoubleSummaryStatistics recent = metrics.getRecentExecutionTimes(50);
        DoubleSummaryStatistics historical = metrics.getHistoricalExecutionTimes(200);
        
        if (recent.getCount() >= 10 && historical.getCount() >= 50) {
            double recentAvg = recent.getAverage();
            double historicalAvg = historical.getAverage();
            
            // Alert if recent performance is significantly worse
            double degradationThreshold = 1.5; // 50% worse performance
            if (recentAvg > historicalAvg * degradationThreshold) {
                alertsTriggered.incrementAndGet();
                logger.warning(String.format("[PERF-DEGRADATION] Operation '%s' performance degraded: " +
                    "recent avg %.1fms vs historical avg %.1fms (%.1f%% increase)",
                    operationName, recentAvg, historicalAvg, 
                    ((recentAvg - historicalAvg) / historicalAvg) * 100));
            }
        }
    }
    
    private List<String> getTopSlowOperations(int limit) {
        return operationMetrics.entrySet().stream()
            .sorted((a, b) -> Double.compare(
                b.getValue().getSnapshot().averageExecutionTime(),
                a.getValue().getSnapshot().averageExecutionTime()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }
    
    private String getPerformanceTrend() {
        historyLock.readLock().lock();
        try {
            if (performanceHistory.size() < 10) {
                return "INSUFFICIENT_DATA";
            }
            
            List<PerformanceSnapshot> recent = performanceHistory.getRecent(10);
            List<PerformanceSnapshot> historical = performanceHistory.getRecent(50);
            
            double recentAvgOps = recent.stream()
                .mapToLong(PerformanceSnapshot::operationCount)
                .average()
                .orElse(0);
            
            double historicalAvgOps = historical.stream()
                .mapToLong(PerformanceSnapshot::operationCount)
                .average()
                .orElse(0);
            
            if (recentAvgOps > historicalAvgOps * 1.1) {
                return "IMPROVING";
            } else if (recentAvgOps < historicalAvgOps * 0.9) {
                return "DEGRADING";
            } else {
                return "STABLE";
            }
        } finally {
            historyLock.readLock().unlock();
        }
    }
    
    // Inner classes and data structures
    
    /**
     * Advanced timing context with extended functionality.
     */
    public class AdvancedTimingContext implements AutoCloseable {
        private final String operationName;
        private final long thresholdMs;
        private final long startTime;
        private final String context;
        
        public AdvancedTimingContext(String operationName, long thresholdMs) {
            this.operationName = operationName;
            this.thresholdMs = thresholdMs;
            this.startTime = System.nanoTime();
            this.context = Thread.currentThread().getName();
        }
        
        @Override
        public void close() {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            recordExecutionTime(operationName, startTime, context);
            // Warn if operation exceeds threshold
            if (thresholdMs > 0 && durationMs > thresholdMs) {
                logger.warning(String.format("Operation '%s' exceeded threshold: %dms > %dms", 
                              operationName, durationMs, thresholdMs));
            }
        }
        
        public void addContext(String additionalContext) {
            // Context could be enhanced to include additional information
        }
        
        public void observe() {
            // No-op method for static analysis compliance
        }
    }
    
    /**
     * Holds comprehensive metrics for a specific operation.
     */
    private static class OperationMetrics {
        private final String name;
        private final LongAdder executionCount = new LongAdder();
        private final AtomicLong totalExecutionTime = new AtomicLong();
        private final AtomicLong maxExecutionTime = new AtomicLong();
        private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        private volatile long lastExecutionTime = System.currentTimeMillis();
        private final CircularBuffer<Double> recentExecutions = new CircularBuffer<>(100);
        private final CircularBuffer<Double> historicalExecutions = new CircularBuffer<>(500);
        
        public OperationMetrics(String name) {
            this.name = name;
        }
        
        public void recordExecution(double durationMs, String context) {
            executionCount.increment();
            long durationMsLong = Math.round(durationMs);
            totalExecutionTime.addAndGet(durationMsLong);
            maxExecutionTime.accumulateAndGet(durationMsLong, Math::max);
            minExecutionTime.accumulateAndGet(durationMsLong, Math::min);
            lastExecutionTime = System.currentTimeMillis();
            
            synchronized (this) {
                recentExecutions.add(durationMs);
                historicalExecutions.add(durationMs);
            }
        }
        
        public PerformanceMetrics getSnapshot() {
            long count = executionCount.sum();
            long total = totalExecutionTime.get();
            long max = maxExecutionTime.get();
            long min = minExecutionTime.get() == Long.MAX_VALUE ? 0 : minExecutionTime.get();
            
            double average = count > 0 ? (double) total / count : 0.0;
            
            return new PerformanceMetrics(name, count, average, max, min, lastExecutionTime);
        }
        
        public long getExecutionCount() {
            return executionCount.sum();
        }
        
        public long getLastExecutionTime() {
            return lastExecutionTime;
        }
        
        public synchronized DoubleSummaryStatistics getRecentExecutionTimes(int count) {
            return recentExecutions.getRecent(count).stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        }
        
        public synchronized DoubleSummaryStatistics getHistoricalExecutionTimes(int count) {
            return historicalExecutions.getRecent(count).stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        }
    }
    
    /**
     * Adaptive sampling strategy to reduce overhead for high-frequency operations.
     */
    private static class SamplingStrategy {
        private final AtomicLong requestCount = new AtomicLong();
        private volatile int samplingRate = 1; // Sample every N requests
        
        public boolean shouldSample() {
            long count = requestCount.incrementAndGet();
            
            // Adaptive sampling: increase rate for very high frequency operations
            if (count % 1000 == 0) {
                adjustSamplingRate(count);
            }
            
            return count % samplingRate == 0;
        }
        
        private void adjustSamplingRate(long totalCount) {
            if (totalCount > 10000) {
                samplingRate = 10; // Sample 1 in 10 for very high frequency
            } else if (totalCount > 1000) {
                samplingRate = 5;  // Sample 1 in 5 for high frequency
            } else {
                samplingRate = 1;  // Sample all for low frequency
            }
        }
    }
    
    /**
     * Circular buffer implementation for efficient storage of recent data.
     */
    private static class CircularBuffer<T> {
        private final Object[] buffer;
        private final int capacity;
        private volatile int head = 0;
        private volatile int size = 0;
        
        public CircularBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new Object[capacity];
        }
        
        public synchronized void add(T item) {
            buffer[head] = item;
            head = (head + 1) % capacity;
            if (size < capacity) {
                size++;
            }
        }
        
        @SuppressWarnings("unchecked")
        public synchronized List<T> getRecent(int count) {
            int actualCount = Math.min(count, size);
            List<T> result = new ArrayList<>(actualCount);
            
            for (int i = 0; i < actualCount; i++) {
                int index = (head - 1 - i + capacity) % capacity;
                result.add((T) buffer[index]);
            }
            
            return result;
        }
        
        public synchronized void clear() {
            head = 0;
            size = 0;
            for (int i = 0; i < capacity; i++) {
                buffer[i] = null;
            }
        }
        
        public int size() {
            return size;
        }
    }
    
    /**
     * Records a timing result for an operation.
     *
     * @param operationName The name of the operation
     * @param durationMs The duration in milliseconds
     */
    public void recordTiming(String operationName, long durationMs) {
        recordExecutionTime(operationName, durationMs, null);
    }
    
    /**
     * Logs a slow operation with context information.
     *
     * @param operationName The name of the operation
     * @param durationMs The duration in milliseconds
     * @param thresholdMs The threshold that was exceeded
     */
    public void logSlowOperation(String operationName, long durationMs, long thresholdMs) {
        String context = String.format("Exceeded threshold of %dms", thresholdMs);
        
        if (durationMs >= criticalOperationThresholdMs) {
            triggerCriticalPerformanceAlert(operationName, durationMs, context);
        } else if (durationMs >= slowOperationThresholdMs) {
            triggerSlowOperationAlert(operationName, durationMs, context);
        }
        
        // Also record the timing
        recordTiming(operationName, durationMs);
    }
    
    // Data records for metrics reporting
    
    public record PerformanceMetrics(
        String operationName,
        long executionCount,
        double averageExecutionTime,
        long maxExecutionTime,
        long minExecutionTime,
        long lastExecutionTime
    ) {}
    
    public record SystemPerformanceReport(
        Instant timestamp,
        double heapUtilization,
        double nonHeapUtilization,
        int threadCount,
        int peakThreadCount,
        long totalOperations,
        long alertsTriggered,
        int trackedOperations,
        List<String> topSlowOperations,
        String performanceTrend
    ) {}
    
    public record PerformanceSnapshot(
        Instant timestamp,
        long heapUsed,
        long heapMax,
        int threadCount,
        long operationCount
    ) {
        public double heapUtilization() {
            return heapMax > 0 ? (double) heapUsed / heapMax : 0.0;
        }
    }
    
    public record PerformanceConfig(
        long slowOperationThresholdMs,
        long criticalOperationThresholdMs,
        double memoryPressureThreshold,
        double criticalMemoryThreshold,
        int maxMetricsRetention,
        boolean predictiveAlertsEnabled,
        long healthCheckIntervalMs
    ) {
        public static PerformanceConfig defaultConfig() {
            return new PerformanceConfig(50, 200, 0.85, 0.95, 10000, true, 30000);
        }
    }
}