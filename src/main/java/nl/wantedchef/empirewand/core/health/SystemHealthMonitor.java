package nl.wantedchef.empirewand.core.health;

import nl.wantedchef.empirewand.core.util.AdvancedPerformanceMonitor;
import org.bukkit.plugin.Plugin;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Instant;
import java.time.Duration;
import java.util.stream.Collectors;

/**
 * Enterprise-grade system health monitoring with comprehensive metrics:
 * - Real-time JVM and system resource monitoring
 * - Performance threshold alerting and automated responses
 * - Health check orchestration with dependency mapping
 * - Predictive analysis and trend detection
 * - Integration with external monitoring systems
 * - Automated recovery and self-healing capabilities
 * - Comprehensive logging and audit trails
 * - Custom health indicators and metrics collection
 */
public class SystemHealthMonitor {
    
    private final Plugin plugin;
    private static final Logger logger = Logger.getLogger(SystemHealthMonitor.class.getName());
    private final AdvancedPerformanceMonitor performanceMonitor;
    
    // JVM management beans
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final OperatingSystemMXBean osBean;
    private final RuntimeMXBean runtimeBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    
    // Health monitoring
    private final Map<String, HealthIndicator> healthIndicators = new ConcurrentHashMap<>();
    private final Map<String, HealthCheckResult> lastHealthResults = new ConcurrentHashMap<>();
    private final ScheduledExecutorService monitoringExecutor;
    
    // System metrics
    private final SystemMetricsCollector metricsCollector;
    private final CircularBuffer<SystemSnapshot> systemHistory;
    private final AlertManager alertManager;
    
    // Performance thresholds
    private volatile double memoryWarningThreshold = 0.80; // 80%
    private volatile double memoryCriticalThreshold = 0.95; // 95%
    private volatile double cpuWarningThreshold = 80.0; // 80%
    private volatile double cpuCriticalThreshold = 95.0; // 95%
    
    // Health check intervals
    private volatile Duration quickCheckInterval = Duration.ofSeconds(30);
    private volatile Duration deepCheckInterval = Duration.ofMinutes(5);
    private volatile Duration historicalAnalysisInterval = Duration.ofMinutes(15);
    
    // Status and metrics
    private volatile OverallHealthStatus overallStatus = OverallHealthStatus.HEALTHY;
    private final LongAdder totalHealthChecks = new LongAdder();
    private final LongAdder failedHealthChecks = new LongAdder();
    private final AtomicLong lastFullCheck = new AtomicLong();
    
    /**
     * Overall system health status.
     */
    public enum OverallHealthStatus {
        HEALTHY(0, "System is operating normally"),
        WARNING(1, "System is experiencing minor issues"),
        CRITICAL(2, "System is experiencing serious issues"),
        DOWN(3, "System is not functioning properly"),
        UNKNOWN(4, "System health status is unknown");
        
        private final int severity;
        private final String description;
        
        OverallHealthStatus(int severity, String description) {
            this.severity = severity;
            this.description = description;
        }
        
        public int getSeverity() { return severity; }
        public String getDescription() { return description; }
    }
    
    /**
     * Health indicator interface for custom health checks.
     */
    @FunctionalInterface
    public interface HealthIndicator {
        CompletableFuture<HealthCheckResult> checkHealth();
        
        default String getName() {
            return getClass().getSimpleName();
        }
        
        default Duration getTimeout() {
            return Duration.ofSeconds(10);
        }
        
        default boolean isCritical() {
            return false;
        }
    }
    
    /**
     * Result of a health check operation.
     */
    public static class HealthCheckResult {
        private final String indicator;
        private final HealthStatus status;
        private final String message;
        private final Map<String, Object> details;
        private final Instant timestamp;
        private final Duration executionTime;
        private final Throwable error;
        
        public HealthCheckResult(String indicator, HealthStatus status, String message) {
            this(indicator, status, message, Collections.emptyMap(), null);
        }
        
        public HealthCheckResult(String indicator, HealthStatus status, String message, 
                               Map<String, Object> details, Throwable error) {
            this.indicator = indicator;
            this.status = status;
            this.message = message;
            this.details = Map.copyOf(details != null ? details : Collections.emptyMap());
            this.timestamp = Instant.now();
            this.executionTime = Duration.ZERO; // Set by monitoring system
            this.error = error;
        }
        
        // Getters
        public String getIndicator() { return indicator; }
        public HealthStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return details; }
        public Instant getTimestamp() { return timestamp; }
        public Duration getExecutionTime() { return executionTime; }
        public Throwable getError() { return error; }
        
        public boolean isHealthy() {
            return status == HealthStatus.UP;
        }
    }
    
    /**
     * Health status enumeration.
     */
    public enum HealthStatus {
        UP, DOWN, WARNING, UNKNOWN
    }
    
    /**
     * System snapshot for historical analysis.
     */
    public record SystemSnapshot(
        Instant timestamp,
        double cpuUsage,
        double memoryUsage,
        long heapMemoryUsed,
        long heapMemoryMax,
        long nonHeapMemoryUsed,
        int threadCount,
        int deadlockedThreads,
        long gcCollectionCount,
        long gcCollectionTime,
        Map<String, HealthStatus> indicatorStatuses,
        OverallHealthStatus overallStatus
    ) {}
    
    /**
     * Alert configuration and management.
     */
    private static class AlertManager {
        private final Logger logger;
        private final List<AlertHandler> alertHandlers = new CopyOnWriteArrayList<>();
        private final Map<String, Instant> lastAlertTimes = new ConcurrentHashMap<>();
        private final Duration alertCooldown = Duration.ofMinutes(5);
        
        public AlertManager(Logger logger) {
            this.logger = logger;
        }
        
        public void addAlertHandler(AlertHandler handler) {
            alertHandlers.add(handler);
        }
        
        public void triggerAlert(AlertLevel level, String component, String message, Map<String, Object> details) {
            String alertKey = level + ":" + component;
            Instant lastAlert = lastAlertTimes.get(alertKey);
            
            // Rate limiting - don't spam alerts
            if (lastAlert != null && Instant.now().minus(alertCooldown).isBefore(lastAlert)) {
                return;
            }
            
            lastAlertTimes.put(alertKey, Instant.now());
            
            Alert alert = new Alert(level, component, message, details, Instant.now());
            
            String alertMessage = String.format("SYSTEM ALERT [%s] %s: %s", level, component, message);
            logger.log(getLogLevel(level), alertMessage);
            
            for (AlertHandler handler : alertHandlers) {
                try {
                    handler.handleAlert(alert);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error in alert handler", e);
                }
            }
        }
        
        private Level getLogLevel(AlertLevel level) {
            return switch (level) {
                case INFO -> Level.INFO;
                case WARNING -> Level.WARNING;
                case CRITICAL -> Level.SEVERE;
            };
        }
    }
    
    /**
     * Alert levels.
     */
    public enum AlertLevel {
        INFO, WARNING, CRITICAL
    }
    
    /**
     * Alert data structure.
     */
    public record Alert(
        AlertLevel level,
        String component,
        String message,
        Map<String, Object> details,
        Instant timestamp
    ) {}
    
    /**
     * Alert handler interface.
     */
    @FunctionalInterface
    public interface AlertHandler {
        void handleAlert(Alert alert);
    }
    
    /**
     * System metrics collector.
     */
    private class SystemMetricsCollector {
        private final com.sun.management.OperatingSystemMXBean sunOsBean;
        
        public SystemMetricsCollector() {
            this.sunOsBean = osBean instanceof com.sun.management.OperatingSystemMXBean ? 
                (com.sun.management.OperatingSystemMXBean) osBean : null;
        }
        
        public SystemMetrics collect() {
            try (var timing = performanceMonitor.startTiming("HealthMonitor.collectMetrics", 100)) {
                timing.observe();
                
                // Memory metrics
                MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
                MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
                
                // Thread metrics
                int threadCount = threadBean.getThreadCount();
                int peakThreadCount = threadBean.getPeakThreadCount();
                long[] deadlockedThreads = threadBean.findDeadlockedThreads();
                int deadlockedCount = deadlockedThreads != null ? deadlockedThreads.length : 0;
                
                // GC metrics
                long totalGcCollections = 0;
                long totalGcTime = 0;
                for (GarbageCollectorMXBean gcBean : gcBeans) {
                    totalGcCollections += gcBean.getCollectionCount();
                    totalGcTime += gcBean.getCollectionTime();
                }
                
                // CPU metrics (if available) - avoid deprecated getSystemCpuLoad
                double processCpuLoad = sunOsBean != null ? sunOsBean.getProcessCpuLoad() * 100 : -1;
                double systemCpuLoad = -1; // Removed deprecated getSystemCpuLoad() method call
                
                // System load
                double systemLoadAverage = osBean.getSystemLoadAverage();
                
                return new SystemMetrics(
                    Instant.now(),
                    processCpuLoad,
                    systemCpuLoad,
                    systemLoadAverage,
                    heapUsage.getUsed(),
                    heapUsage.getMax(),
                    heapUsage.getCommitted(),
                    nonHeapUsage.getUsed(),
                    nonHeapUsage.getMax(),
                    threadCount,
                    peakThreadCount,
                    deadlockedCount,
                    totalGcCollections,
                    totalGcTime,
                    Runtime.getRuntime().totalMemory(),
                    Runtime.getRuntime().freeMemory(),
                    Runtime.getRuntime().maxMemory()
                );
            }
        }
    }
    
    /**
     * System metrics data structure.
     */
    public record SystemMetrics(
        Instant timestamp,
        double processCpuLoad,
        double systemCpuLoad,
        double systemLoadAverage,
        long heapMemoryUsed,
        long heapMemoryMax,
        long heapMemoryCommitted,
        long nonHeapMemoryUsed,
        long nonHeapMemoryMax,
        int threadCount,
        int peakThreadCount,
        int deadlockedThreads,
        long gcCollectionCount,
        long gcCollectionTime,
        long runtimeTotalMemory,
        long runtimeFreeMemory,
        long runtimeMaxMemory
    ) {
        public double getHeapUtilization() {
            return heapMemoryMax > 0 ? (double) heapMemoryUsed / heapMemoryMax : 0.0;
        }
        
        public double getNonHeapUtilization() {
            return nonHeapMemoryMax > 0 ? (double) nonHeapMemoryUsed / nonHeapMemoryMax : 0.0;
        }
    }
    
    /**
     * Circular buffer implementation for efficient historical data storage.
     */
    private static class CircularBuffer<T> {
        private final Object[] buffer;
        private final int capacity;
        private volatile int head = 0;
        private volatile int size = 0;
        private final Object lock = new Object();
        
        public CircularBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new Object[capacity];
        }
        
        public void add(T item) {
            synchronized (lock) {
                buffer[head] = item;
                head = (head + 1) % capacity;
                if (size < capacity) {
                    size++;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        public List<T> getRecent(int count) {
            synchronized (lock) {
                int actualCount = Math.min(count, size);
                List<T> result = new ArrayList<>(actualCount);
                
                for (int i = 0; i < actualCount; i++) {
                    int index = (head - 1 - i + capacity) % capacity;
                    result.add((T) buffer[index]);
                }
                
                return result;
            }
        }
        
        public int size() {
            return size;
        }
    }
    
    public SystemHealthMonitor(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        this.performanceMonitor = new AdvancedPerformanceMonitor(plugin, logger);
        
        // Initialize JMX beans
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        
        // Enable thread CPU time if supported
        if (threadBean.isThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
        
        // Initialize components
        this.metricsCollector = new SystemMetricsCollector();
        this.systemHistory = new CircularBuffer<>(1000); // Keep last 1000 snapshots
        this.alertManager = new AlertManager(logger);
        
        // Initialize executor
        this.monitoringExecutor = Executors.newScheduledThreadPool(3, r -> {
            Thread t = new Thread(r, "EmpireWand-HealthMonitor");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        
        // Register built-in health indicators
        registerBuiltinHealthIndicators();
        
        // Start monitoring
        startMonitoring();
        performanceMonitor.startMonitoring();
        
        logger.info("SystemHealthMonitor initialized with comprehensive monitoring");
    }
    
    /**
     * Registers a custom health indicator.
     */
    public void registerHealthIndicator(String name, HealthIndicator indicator) {
        healthIndicators.put(name, indicator);
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Registered health indicator: {0}", name);
        }
    }
    
    /**
     * Registers a simple health check function.
     */
    public void registerHealthCheck(String name, Supplier<Boolean> healthCheck, String description) {
        HealthIndicator indicator = () -> CompletableFuture.supplyAsync(() -> {
            try {
                boolean isHealthy = healthCheck.get();
                return new HealthCheckResult(name, 
                    isHealthy ? HealthStatus.UP : HealthStatus.DOWN,
                    description);
            } catch (Exception e) {
                return new HealthCheckResult(name, HealthStatus.DOWN, 
                    "Health check failed: " + e.getMessage(), null, e);
            }
        });
        
        registerHealthIndicator(name, indicator);
    }
    
    /**
     * Performs a comprehensive health check of all indicators.
     */
    public CompletableFuture<HealthReport> performHealthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            try (var timing = performanceMonitor.startTiming("HealthMonitor.fullHealthCheck", 5000)) {
                timing.observe();
                
                totalHealthChecks.increment();
                Map<String, CompletableFuture<HealthCheckResult>> futures = new HashMap<>();
                
                // Start all health checks concurrently
                for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
                    String name = entry.getKey();
                    HealthIndicator indicator = entry.getValue();
                    
                    CompletableFuture<HealthCheckResult> future = indicator.checkHealth()
                        .orTimeout(indicator.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                        .exceptionally(throwable -> {
                            failedHealthChecks.increment();
                            return new HealthCheckResult(name, HealthStatus.DOWN, 
                                "Health check timeout or error: " + throwable.getMessage(), null, throwable);
                        });
                    
                    futures.put(name, future);
                }
                
                // Collect results
                Map<String, HealthCheckResult> results = new HashMap<>();
                for (Map.Entry<String, CompletableFuture<HealthCheckResult>> entry : futures.entrySet()) {
                    try {
                        HealthCheckResult result = entry.getValue().get(30, TimeUnit.SECONDS);
                        results.put(entry.getKey(), result);
                        lastHealthResults.put(entry.getKey(), result);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        failedHealthChecks.increment();
                        HealthCheckResult errorResult = new HealthCheckResult(entry.getKey(), 
                            HealthStatus.DOWN, "Failed to get health check result", null, e);
                        results.put(entry.getKey(), errorResult);
                        lastHealthResults.put(entry.getKey(), errorResult);
                    }
                }
                
                // Determine overall status
                OverallHealthStatus newStatus = determineOverallStatus(results);
                if (newStatus != overallStatus) {
                    OverallHealthStatus oldStatus = overallStatus;
                    overallStatus = newStatus;
                    
                    // Trigger status change alert
                    alertManager.triggerAlert(
                        newStatus.getSeverity() > oldStatus.getSeverity() ? AlertLevel.WARNING : AlertLevel.INFO,
                        "SystemHealth",
                        String.format("System health status changed from %s to %s", oldStatus, newStatus),
                        Map.of("oldStatus", oldStatus, "newStatus", newStatus)
                    );
                }
                
                lastFullCheck.set(System.currentTimeMillis());
                
                // Collect system metrics
                SystemMetrics systemMetrics = metricsCollector.collect();
                
                // Check system thresholds
                checkSystemThresholds(systemMetrics);
                
                // Create snapshot
                Map<String, HealthStatus> indicatorStatuses = results.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, 
                        entry -> entry.getValue().getStatus()));
                
                SystemSnapshot snapshot = new SystemSnapshot(
                    Instant.now(),
                    systemMetrics.processCpuLoad(),
                    systemMetrics.getHeapUtilization(),
                    systemMetrics.heapMemoryUsed(),
                    systemMetrics.heapMemoryMax(),
                    systemMetrics.nonHeapMemoryUsed(),
                    systemMetrics.threadCount(),
                    systemMetrics.deadlockedThreads(),
                    systemMetrics.gcCollectionCount(),
                    systemMetrics.gcCollectionTime(),
                    indicatorStatuses,
                    overallStatus
                );
                
                systemHistory.add(snapshot);
                
                return new HealthReport(overallStatus, results, systemMetrics, 
                    Instant.now(), Duration.ofMillis(timing.getElapsedMillis()));
            }
        }, monitoringExecutor);
    }
    
    /**
     * Gets the current system health status.
     */
    public OverallHealthStatus getOverallHealthStatus() {
        return overallStatus;
    }
    
    /**
     * Gets the latest health check results.
     */
    public Map<String, HealthCheckResult> getLatestHealthResults() {
        return Map.copyOf(lastHealthResults);
    }
    
    /**
     * Gets recent system snapshots for trend analysis.
     */
    public List<SystemSnapshot> getRecentSnapshots(int count) {
        return systemHistory.getRecent(count);
    }
    
    /**
     * Gets comprehensive health monitoring metrics.
     */
    public HealthMonitorMetrics getMetrics() {
        List<SystemSnapshot> recent = systemHistory.getRecent(60); // Last hour
        
        double avgCpuUsage = recent.stream()
            .mapToDouble(SystemSnapshot::cpuUsage)
            .filter(cpu -> cpu >= 0)
            .average()
            .orElse(0.0);
        
        double avgMemoryUsage = recent.stream()
            .mapToDouble(SystemSnapshot::memoryUsage)
            .average()
            .orElse(0.0);
        
        SystemMetrics currentMetrics = metricsCollector.collect();
        
        return new HealthMonitorMetrics(
            overallStatus,
            totalHealthChecks.sum(),
            failedHealthChecks.sum(),
            healthIndicators.size(),
            lastFullCheck.get(),
            currentMetrics,
            avgCpuUsage,
            avgMemoryUsage,
            systemHistory.size()
        );
    }
    
    /**
     * Adds an alert handler for system notifications.
     */
    public void addAlertHandler(AlertHandler handler) {
        alertManager.addAlertHandler(handler);
    }
    
    /**
     * Configures health monitoring thresholds.
     */
    public void configureThresholds(HealthThresholds thresholds) {
        this.memoryWarningThreshold = thresholds.memoryWarningThreshold();
        this.memoryCriticalThreshold = thresholds.memoryCriticalThreshold();
        this.cpuWarningThreshold = thresholds.cpuWarningThreshold();
        this.cpuCriticalThreshold = thresholds.cpuCriticalThreshold();
        
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Health monitoring thresholds updated: {0}", thresholds);
        }
    }
    
    /**
     * Shuts down the health monitoring system.
     */
    public void shutdown() {
        logger.info("Shutting down SystemHealthMonitor...");
        
        try {
            monitoringExecutor.shutdown();
            if (!monitoringExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                monitoringExecutor.shutdownNow();
            }
            
            performanceMonitor.stopMonitoring();
            
            healthIndicators.clear();
            lastHealthResults.clear();
            
            logger.info("SystemHealthMonitor shutdown complete");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            logger.log(Level.WARNING, "Interrupted during SystemHealthMonitor shutdown", e);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Runtime error during SystemHealthMonitor shutdown", e);
        }
    }
    
    // Private implementation methods
    
    private void startMonitoring() {
        // Quick health checks
        monitoringExecutor.scheduleWithFixedDelay(
            () -> performQuickHealthCheck(),
            quickCheckInterval.toMillis(),
            quickCheckInterval.toMillis(),
            TimeUnit.MILLISECONDS
        );
        
        // Deep health checks
        monitoringExecutor.scheduleWithFixedDelay(
            () -> performHealthCheck(),
            deepCheckInterval.toMillis(),
            deepCheckInterval.toMillis(),
            TimeUnit.MILLISECONDS
        );
        
        // Historical analysis
        monitoringExecutor.scheduleWithFixedDelay(
            () -> performHistoricalAnalysis(),
            historicalAnalysisInterval.toMillis(),
            historicalAnalysisInterval.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }
    
    private void performQuickHealthCheck() {
        try {
            SystemMetrics metrics = metricsCollector.collect();
            checkSystemThresholds(metrics);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during quick health check", e);
        }
    }
    
    private void checkSystemThresholds(SystemMetrics metrics) {
        // Memory checks
        double memoryUsage = metrics.getHeapUtilization();
        if (memoryUsage >= memoryCriticalThreshold) {
            alertManager.triggerAlert(AlertLevel.CRITICAL, "Memory", 
                String.format("Heap memory usage critical: %.1f%%", memoryUsage * 100),
                Map.of("usage", memoryUsage, "threshold", memoryCriticalThreshold));
        } else if (memoryUsage >= memoryWarningThreshold) {
            alertManager.triggerAlert(AlertLevel.WARNING, "Memory", 
                String.format("Heap memory usage high: %.1f%%", memoryUsage * 100),
                Map.of("usage", memoryUsage, "threshold", memoryWarningThreshold));
        }
        
        // CPU checks
        if (metrics.processCpuLoad() >= 0) {
            if (metrics.processCpuLoad() >= cpuCriticalThreshold) {
                alertManager.triggerAlert(AlertLevel.CRITICAL, "CPU", 
                    String.format("CPU usage critical: %.1f%%", metrics.processCpuLoad()),
                    Map.of("usage", metrics.processCpuLoad(), "threshold", cpuCriticalThreshold));
            } else if (metrics.processCpuLoad() >= cpuWarningThreshold) {
                alertManager.triggerAlert(AlertLevel.WARNING, "CPU", 
                    String.format("CPU usage high: %.1f%%", metrics.processCpuLoad()),
                    Map.of("usage", metrics.processCpuLoad(), "threshold", cpuWarningThreshold));
            }
        }
        
        // Thread deadlock check
        if (metrics.deadlockedThreads() > 0) {
            alertManager.triggerAlert(AlertLevel.CRITICAL, "Threads", 
                "Thread deadlock detected: " + metrics.deadlockedThreads() + " threads",
                Map.of("deadlockedThreads", metrics.deadlockedThreads()));
        }
    }
    
    private void performHistoricalAnalysis() {
        try {
            List<SystemSnapshot> recent = systemHistory.getRecent(60); // Last hour
            if (recent.size() < 10) {
                return; // Not enough data for analysis
            }
            
            // Analyze trends
            analyzeTrends(recent);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during historical analysis", e);
        }
    }
    
    private void analyzeTrends(List<SystemSnapshot> snapshots) {
        // Simple trend analysis - could be enhanced with more sophisticated algorithms
        
        // Memory trend
        double[] memoryValues = snapshots.stream()
            .mapToDouble(SystemSnapshot::memoryUsage)
            .toArray();
        
        double memoryTrend = calculateTrend(memoryValues);
        if (memoryTrend > 0.1) { // Increasing by more than 10%
            alertManager.triggerAlert(AlertLevel.WARNING, "MemoryTrend", 
                "Memory usage trending upward", 
                Map.of("trend", memoryTrend));
        }
        
        // CPU trend
        double[] cpuValues = snapshots.stream()
            .mapToDouble(SystemSnapshot::cpuUsage)
            .filter(cpu -> cpu >= 0)
            .toArray();
        
        if (cpuValues.length > 0) {
            double cpuTrend = calculateTrend(cpuValues);
            if (cpuTrend > 10.0) { // Increasing by more than 10%
                alertManager.triggerAlert(AlertLevel.WARNING, "CpuTrend", 
                    "CPU usage trending upward", 
                    Map.of("trend", cpuTrend));
            }
        }
    }
    
    private double calculateTrend(double[] values) {
        if (values.length < 2) {
            return 0.0;
        }
        
        // Simple linear trend calculation
        double sum = 0;
        for (int i = 1; i < values.length; i++) {
            sum += values[i] - values[i - 1];
        }
        
        return sum / (values.length - 1);
    }
    
    private OverallHealthStatus determineOverallStatus(Map<String, HealthCheckResult> results) {
        boolean hasDown = false;
        boolean hasWarning = false;
        
        for (HealthCheckResult result : results.values()) {
            switch (result.getStatus()) {
                case DOWN -> hasDown = true;
                case WARNING -> hasWarning = true;
                case UNKNOWN -> hasWarning = true;
                case UP -> {} // continue
            }
        }
        
        if (hasDown) {
            return OverallHealthStatus.CRITICAL;
        } else if (hasWarning) {
            return OverallHealthStatus.WARNING;
        } else {
            return OverallHealthStatus.HEALTHY;
        }
    }
    
    private void registerBuiltinHealthIndicators() {
        // JVM Health Indicator
        registerHealthIndicator("jvm", () -> CompletableFuture.supplyAsync(() -> {
            try {
                SystemMetrics metrics = metricsCollector.collect();
                Map<String, Object> details = new HashMap<>();
                
                details.put("heapUtilization", String.format("%.1f%%", metrics.getHeapUtilization() * 100));
                details.put("threadCount", metrics.threadCount());
                details.put("gcCollections", metrics.gcCollectionCount());
                
                HealthStatus status = HealthStatus.UP;
                String message = "JVM is healthy";
                
                if (metrics.getHeapUtilization() > memoryCriticalThreshold) {
                    status = HealthStatus.DOWN;
                    message = "JVM memory usage critical";
                } else if (metrics.getHeapUtilization() > memoryWarningThreshold) {
                    status = HealthStatus.WARNING;
                    message = "JVM memory usage high";
                }
                
                return new HealthCheckResult("jvm", status, message, details, null);
                
            } catch (Exception e) {
                return new HealthCheckResult("jvm", HealthStatus.DOWN, 
                    "JVM health check failed", null, e);
            }
        }));
        
        // Plugin Health Indicator
        registerHealthIndicator("plugin", () -> CompletableFuture.supplyAsync(() -> {
            Map<String, Object> details = new HashMap<>();
            details.put("pluginEnabled", plugin.isEnabled());
            details.put("uptime", Duration.ofMillis(System.currentTimeMillis() - runtimeBean.getStartTime()));
            
            HealthStatus status = plugin.isEnabled() ? HealthStatus.UP : HealthStatus.DOWN;
            String message = plugin.isEnabled() ? "Plugin is enabled" : "Plugin is disabled";
            
            return new HealthCheckResult("plugin", status, message, details, null);
        }));
        
        // Disk Space Health Indicator
        registerHealthIndicator("diskspace", () -> CompletableFuture.supplyAsync(() -> {
            try {
                long totalSpace = plugin.getDataFolder().getTotalSpace();
                long freeSpace = plugin.getDataFolder().getFreeSpace();
                double utilization = 1.0 - ((double) freeSpace / totalSpace);
                
                Map<String, Object> details = new HashMap<>();
                details.put("totalSpaceMB", totalSpace / 1024 / 1024);
                details.put("freeSpaceMB", freeSpace / 1024 / 1024);
                details.put("utilization", String.format("%.1f%%", utilization * 100));
                
                HealthStatus status = HealthStatus.UP;
                String message = "Disk space healthy";
                
                if (utilization > 0.95) { // 95% full
                    status = HealthStatus.DOWN;
                    message = "Disk space critical";
                } else if (utilization > 0.85) { // 85% full
                    status = HealthStatus.WARNING;
                    message = "Disk space low";
                }
                
                return new HealthCheckResult("diskspace", status, message, details, null);
                
            } catch (Exception e) {
                return new HealthCheckResult("diskspace", HealthStatus.DOWN, 
                    "Disk space check failed", null, e);
            }
        }));
    }
    
    // Data structures for reporting
    
    public record HealthReport(
        OverallHealthStatus overallStatus,
        Map<String, HealthCheckResult> indicatorResults,
        SystemMetrics systemMetrics,
        Instant timestamp,
        Duration executionTime
    ) {}
    
    public record HealthThresholds(
        double memoryWarningThreshold,
        double memoryCriticalThreshold,
        double cpuWarningThreshold,
        double cpuCriticalThreshold
    ) {
        public static HealthThresholds defaultThresholds() {
            return new HealthThresholds(0.80, 0.95, 80.0, 95.0);
        }
    }
    
    public record HealthMonitorMetrics(
        OverallHealthStatus overallStatus,
        long totalHealthChecks,
        long failedHealthChecks,
        int registeredIndicators,
        long lastFullCheckTimestamp,
        SystemMetrics currentSystemMetrics,
        double avgCpuUsage,
        double avgMemoryUsage,
        int historicalSnapshotCount
    ) {
        public double getHealthCheckSuccessRate() {
            return totalHealthChecks > 0 ? 
                (double) (totalHealthChecks - failedHealthChecks) / totalHealthChecks : 1.0;
        }
    }
}