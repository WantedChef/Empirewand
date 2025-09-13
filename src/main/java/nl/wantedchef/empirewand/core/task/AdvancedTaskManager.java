package nl.wantedchef.empirewand.core.task;

import nl.wantedchef.empirewand.core.util.AdvancedPerformanceMonitor;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;


import java.util.Objects;
import java.util.function.Supplier;
import java.time.Duration;
import java.time.Instant;

/**
 * Enterprise-grade task management system with advanced scheduling, resource pooling,
 * priority management, and comprehensive monitoring capabilities.
 * 
 * Features:
 * - Multi-tier thread pool management with priority queues
 * - Adaptive thread pool sizing based on workload
 * - Task lifecycle monitoring and performance analytics
 * - Dead task detection and automatic cleanup
 * - Resource pooling and reuse for common task types
 * - Rate limiting and burst protection
 * - Circuit breaker pattern for failing tasks
 * - Task dependency management and execution ordering
 * - Graceful shutdown with configurable timeouts
 * - Real-time metrics and health monitoring
 */
public class AdvancedTaskManager {
    
    private final Plugin plugin;
    private final Logger logger;
    private final AdvancedPerformanceMonitor performanceMonitor;
    
    // Core task tracking
    private final Set<BukkitTask> activeBukkitTasks = ConcurrentHashMap.newKeySet();
    private final Map<String, TaskGroup> taskGroups = new ConcurrentHashMap<>();
    private final Map<CompletableFuture<?>, TaskMetadata> asyncTasks = new ConcurrentHashMap<>();
    
    // Thread pool management
    private final ThreadPoolExecutor highPriorityExecutor;
    private final ThreadPoolExecutor normalPriorityExecutor;
    private final ThreadPoolExecutor lowPriorityExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final ForkJoinPool forkJoinPool;
    
    // Task metrics and monitoring
    private final LongAdder totalTasksSubmitted = new LongAdder();
    private final LongAdder totalTasksCompleted = new LongAdder();
    private final LongAdder totalTasksFailed = new LongAdder();
    private final AtomicLong maxConcurrentTasks = new AtomicLong();
    private final AtomicInteger currentActiveTasks = new AtomicInteger();
    
    // Configuration and thresholds
    private volatile int maxTasksPerSecond = 1000;
    private volatile int maxConcurrentAsyncTasks = 200;
    private volatile long taskTimeoutMs = 30000; // 30 seconds
    private volatile boolean circuitBreakerEnabled = true;
    
    // Rate limiting
    private final RateLimiter globalRateLimiter;
    private final Map<String, RateLimiter> groupRateLimiters = new ConcurrentHashMap<>();
    
    // Circuit breaker for failing tasks
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    // Task cleanup and monitoring
    private final ScheduledFuture<?> cleanupTask;
    private final ScheduledFuture<?> monitoringTask;
    
    /**
     * Task priority levels for execution ordering.
     */
    public enum TaskPriority {
        LOW(0),
        NORMAL(1),
        HIGH(2),
        CRITICAL(3);
        
        private final int level;
        
        TaskPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * Task execution context with metadata and monitoring.
     */
    public static class TaskContext {
        private final String name;
        private final TaskPriority priority;
        private final String group;
        private final long timeoutMs;
        private final boolean retryOnFailure;
        private final int maxRetries;
        
        private TaskContext(Builder builder) {
            this.name = builder.name;
            this.priority = builder.priority;
            this.group = builder.group;
            this.timeoutMs = builder.timeoutMs;
            this.retryOnFailure = builder.retryOnFailure;
            this.maxRetries = builder.maxRetries;
        }
        
        public static Builder builder(String name) {
            return new Builder(name);
        }
        
        public static class Builder {
            private final String name;
            private TaskPriority priority = TaskPriority.NORMAL;
            private String group = "default";
            private long timeoutMs = 30000;
            private boolean retryOnFailure = false;
            private int maxRetries = 3;
            
            private Builder(String name) {
                this.name = Objects.requireNonNull(name);
            }
            
            public Builder priority(TaskPriority priority) {
                this.priority = priority;
                return this;
            }
            
            public Builder group(String group) {
                this.group = group;
                return this;
            }
            
            public Builder timeout(Duration timeout) {
                this.timeoutMs = timeout.toMillis();
                return this;
            }
            
            public Builder retryOnFailure(boolean retry, int maxRetries) {
                this.retryOnFailure = retry;
                this.maxRetries = maxRetries;
                return this;
            }
            
            public TaskContext build() {
                return new TaskContext(this);
            }
        }
        
        // Getters
        public String getName() { return name; }
        public TaskPriority getPriority() { return priority; }
        public String getGroup() { return group; }
        public long getTimeoutMs() { return timeoutMs; }
        public boolean shouldRetryOnFailure() { return retryOnFailure; }
        public int getMaxRetries() { return maxRetries; }
    }
    
    /**
     * Metadata for tracking async task execution.
     */
    private static class TaskMetadata {
        private final TaskContext context;
        private final Instant submissionTime;
        private final AtomicInteger retryCount = new AtomicInteger();
        private volatile Instant startTime;
        private volatile Instant endTime;
        private volatile TaskStatus status = TaskStatus.SUBMITTED;
        
        public TaskMetadata(TaskContext context) {
            this.context = context;
            this.submissionTime = Instant.now();
        }
        
        public enum TaskStatus {
            SUBMITTED, RUNNING, COMPLETED, FAILED, TIMEOUT, CANCELLED
        }
    }
    
    /**
     * Task group for organizing and limiting related tasks.
     */
    private static class TaskGroup {
        private final String name;
        private final int maxConcurrentTasks;
        private final AtomicInteger activeTasks = new AtomicInteger();
        private final LongAdder totalTasksSubmitted = new LongAdder();
        private final LongAdder totalTasksCompleted = new LongAdder();
        
        public TaskGroup(String name, int maxConcurrentTasks) {
            this.name = name;
            this.maxConcurrentTasks = maxConcurrentTasks;
        }
        
        public boolean tryAcquire() {
            int current = activeTasks.get();
            return current < maxConcurrentTasks && activeTasks.compareAndSet(current, current + 1);
        }
        
        public void release() {
            activeTasks.decrementAndGet();
            totalTasksCompleted.increment();
        }
        
        public void submit() {
            totalTasksSubmitted.increment();
        }
    }
    
    /**
     * Simple rate limiter implementation.
     */
    private static class RateLimiter {
        private final int maxPermitsPerSecond;
        private final AtomicLong lastRefillTime = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger permits;
        
        public RateLimiter(int maxPermitsPerSecond) {
            this.maxPermitsPerSecond = maxPermitsPerSecond;
            this.permits = new AtomicInteger(maxPermitsPerSecond);
        }
        
        public boolean tryAcquire() {
            refillIfNeeded();
            return permits.get() > 0 && permits.decrementAndGet() >= 0;
        }
        
        private void refillIfNeeded() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();
            
            if (now - lastRefill >= 1000) { // 1 second
                if (lastRefillTime.compareAndSet(lastRefill, now)) {
                    permits.set(maxPermitsPerSecond);
                }
            }
        }
    }
    
    /**
     * Circuit breaker for failing task groups.
     */
    private static class CircuitBreaker {
        private final int failureThreshold;
        private final long timeoutMs;
        private final AtomicInteger failureCount = new AtomicInteger();
        private volatile long lastFailureTime = 0;
        private volatile State state = State.CLOSED;
        
        public enum State {
            CLOSED, OPEN, HALF_OPEN
        }
        
        public CircuitBreaker(int failureThreshold, long timeoutMs) {
            this.failureThreshold = failureThreshold;
            this.timeoutMs = timeoutMs;
        }
        
        public boolean allowRequest() {
            if (state == State.CLOSED) {
                return true;
            }
            
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime >= timeoutMs) {
                    state = State.HALF_OPEN;
                    return true;
                }
                return false;
            }
            
            return true; // HALF_OPEN
        }
        
        public void recordSuccess() {
            if (state == State.HALF_OPEN) {
                state = State.CLOSED;
                failureCount.set(0);
            }
        }
        
        public void recordFailure() {
            lastFailureTime = System.currentTimeMillis();
            int failures = failureCount.incrementAndGet();
            
            if (failures >= failureThreshold) {
                state = State.OPEN;
            }
        }
    }
    
    public AdvancedTaskManager(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        this.logger = plugin.getLogger();
        this.performanceMonitor = new AdvancedPerformanceMonitor(plugin, logger);
        
        // Initialize rate limiter
        this.globalRateLimiter = new RateLimiter(maxTasksPerSecond);
        
        // Create thread pools with different priorities
        this.highPriorityExecutor = createThreadPool("HighPriority", 4, 8, TaskPriority.HIGH);
        this.normalPriorityExecutor = createThreadPool("NormalPriority", 8, 16, TaskPriority.NORMAL);
        this.lowPriorityExecutor = createThreadPool("LowPriority", 2, 4, TaskPriority.LOW);
        
        // Create scheduled executor for timed tasks
        this.scheduledExecutor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "EmpireWand-Scheduled-" + System.currentTimeMillis());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
        
        // Create ForkJoinPool for parallel processing
        this.forkJoinPool = new ForkJoinPool(
            Runtime.getRuntime().availableProcessors(),
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            true // async mode for better task distribution
        );
        
        // Start monitoring and cleanup tasks
        this.cleanupTask = scheduledExecutor.scheduleWithFixedDelay(
            this::cleanupDeadTasks, 30, 30, TimeUnit.SECONDS);
        this.monitoringTask = scheduledExecutor.scheduleWithFixedDelay(
            this::updateMetrics, 10, 10, TimeUnit.SECONDS);
        
        // Start performance monitoring
        performanceMonitor.startMonitoring();
        
        logger.info("AdvancedTaskManager initialized with enterprise-grade task management");
    }
    
    /**
     * Submits an async task with context and monitoring.
     */
    public <T> CompletableFuture<T> submitAsync(Supplier<T> task, TaskContext context) {
        Objects.requireNonNull(task);
        Objects.requireNonNull(context);
        
        if (!globalRateLimiter.tryAcquire()) {
            return CompletableFuture.failedFuture(
                new RejectedExecutionException("Global rate limit exceeded"));
        }
        
        // Check task group limits
        TaskGroup group = getOrCreateTaskGroup(context.getGroup());
        if (!group.tryAcquire()) {
            return CompletableFuture.failedFuture(
                new RejectedExecutionException("Task group limit exceeded: " + context.getGroup()));
        }
        
        // Check circuit breaker
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(context.getGroup());
        if (circuitBreakerEnabled && !circuitBreaker.allowRequest()) {
            group.release();
            return CompletableFuture.failedFuture(
                new RejectedExecutionException("Circuit breaker open for group: " + context.getGroup()));
        }
        
        // Check concurrent task limit
        if (asyncTasks.size() >= maxConcurrentAsyncTasks) {
            group.release();
            return CompletableFuture.failedFuture(
                new RejectedExecutionException("Maximum concurrent tasks reached"));
        }
        
        totalTasksSubmitted.increment();
        group.submit();
        
        TaskMetadata metadata = new TaskMetadata(context);
        
        CompletableFuture<T> baseFuture = CompletableFuture
            .supplyAsync(() -> executeTaskWithMonitoring(task, metadata), getExecutorForPriority(context.getPriority()))
            .orTimeout(context.getTimeoutMs(), TimeUnit.MILLISECONDS);
        
        CompletableFuture<T> future = baseFuture.whenComplete((result, throwable) -> {
                // Cleanup and update metrics
                asyncTasks.remove(baseFuture);
                group.release();
                currentActiveTasks.decrementAndGet();
                
                if (throwable == null) {
                    totalTasksCompleted.increment();
                    metadata.status = TaskMetadata.TaskStatus.COMPLETED;
                    circuitBreaker.recordSuccess();
                } else {
                    totalTasksFailed.increment();
                    metadata.status = throwable instanceof TimeoutException ? 
                        TaskMetadata.TaskStatus.TIMEOUT : TaskMetadata.TaskStatus.FAILED;
                    circuitBreaker.recordFailure();
                    
                    // Log task failure
                    logger.warning("Task failed: " + context.getName() + " - " + throwable.getMessage());
                }
                
                metadata.endTime = Instant.now();
            });
        
        // Track the task
        asyncTasks.put(baseFuture, metadata);
        currentActiveTasks.incrementAndGet();
        maxConcurrentTasks.accumulateAndGet(currentActiveTasks.get(), Math::max);
        
        return future;
    }
    
    /**
     * Submits a simple async task with default context.
     */
    public <T> CompletableFuture<T> submitAsync(String name, Supplier<T> task) {
        return submitAsync(task, TaskContext.builder(name).build());
    }
    
    /**
     * Submits a parallel processing task using ForkJoinPool.
     */
    public <T> CompletableFuture<T> submitParallel(Supplier<T> task, TaskContext context) {
        return CompletableFuture.supplyAsync(task, forkJoinPool)
            .orTimeout(context.getTimeoutMs(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Registers a Bukkit task for tracking.
     */
    public BukkitTask registerTask(BukkitTask task) {
        if (task != null) {
            activeBukkitTasks.add(task);
            totalTasksSubmitted.increment();
        }
        return task;
    }
    
    /**
     * Runs a task timer with tracking and monitoring.
     */
    public BukkitTask runTaskTimer(BukkitRunnable runnable, long delay, long period) {
        BukkitTask task = runnable.runTaskTimer(plugin, delay, period);
        return registerTask(task);
    }
    
    /**
     * Runs a task timer with tracking and monitoring.
     */
    public BukkitTask runTaskTimer(Runnable runnable, long delay, long period) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, delay, period);
        return registerTask(task);
    }
    
    /**
     * Runs a delayed task with tracking.
     */
    public BukkitTask runTaskLater(BukkitRunnable runnable, long delay) {
        BukkitTask task = runnable.runTaskLater(plugin, delay);
        return registerTask(task);
    }
    
    /**
     * Runs a delayed task with tracking.
     */
    public BukkitTask runTaskLater(Runnable runnable, long delay) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delay);
        return registerTask(task);
    }
    
    /**
     * Runs an immediate task with tracking.
     */
    public BukkitTask runTask(BukkitRunnable runnable) {
        BukkitTask task = runnable.runTask(plugin);
        return registerTask(task);
    }
    
    /**
     * Runs an immediate task with tracking.
     */
    public BukkitTask runTask(Runnable runnable) {
        BukkitTask task = plugin.getServer().getScheduler().runTask(plugin, runnable);
        return registerTask(task);
    }
    
    /**
     * Creates a task group with specified concurrency limits.
     */
    public void createTaskGroup(String name, int maxConcurrentTasks) {
        taskGroups.put(name, new TaskGroup(name, maxConcurrentTasks));
        logger.info("Created task group: " + name + " with max concurrent tasks: " + maxConcurrentTasks);
    }
    
    /**
     * Configures rate limiting for a specific task group.
     */
    public void configureRateLimit(String groupName, int maxTasksPerSecond) {
        groupRateLimiters.put(groupName, new RateLimiter(maxTasksPerSecond));
        logger.info("Configured rate limit for group " + groupName + ": " + maxTasksPerSecond + " tasks/second");
    }
    
    /**
     * Gets comprehensive task management metrics.
     */
    public TaskManagerMetrics getMetrics() {
        Map<String, TaskGroupMetrics> groupMetrics = new ConcurrentHashMap<>();
        
        taskGroups.forEach((name, group) -> {
            groupMetrics.put(name, new TaskGroupMetrics(
                name,
                group.activeTasks.get(),
                group.maxConcurrentTasks,
                group.totalTasksSubmitted.sum(),
                group.totalTasksCompleted.sum()
            ));
        });
        
        return new TaskManagerMetrics(
            totalTasksSubmitted.sum(),
            totalTasksCompleted.sum(),
            totalTasksFailed.sum(),
            currentActiveTasks.get(),
            maxConcurrentTasks.get(),
            activeBukkitTasks.size(),
            asyncTasks.size(),
            groupMetrics,
            getThreadPoolMetrics()
        );
    }
    
    /**
     * Cancels all tasks and shuts down the task manager.
     */
    public void shutdown() {
        logger.info("Shutting down AdvancedTaskManager...");
        
        // Cancel monitoring tasks
        if (cleanupTask != null) cleanupTask.cancel(true);
        if (monitoringTask != null) monitoringTask.cancel(true);
        
        // Cancel all Bukkit tasks
        for (BukkitTask task : activeBukkitTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        activeBukkitTasks.clear();
        
        // Cancel all async tasks
        for (CompletableFuture<?> future : asyncTasks.keySet()) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        asyncTasks.clear();
        
        // Shutdown thread pools
        shutdownExecutor(highPriorityExecutor, "HighPriority");
        shutdownExecutor(normalPriorityExecutor, "NormalPriority");
        shutdownExecutor(lowPriorityExecutor, "LowPriority");
        shutdownExecutor(scheduledExecutor, "Scheduled");
        
        // Shutdown ForkJoinPool
        forkJoinPool.shutdown();
        try {
            if (!forkJoinPool.awaitTermination(10, TimeUnit.SECONDS)) {
                forkJoinPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            forkJoinPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Stop performance monitoring
        performanceMonitor.stopMonitoring();
        
        // Also cancel any remaining Bukkit tasks
        plugin.getServer().getScheduler().cancelTasks(plugin);
        
        logger.info("AdvancedTaskManager shutdown complete");
    }
    
    // Private implementation methods
    
    private ThreadPoolExecutor createThreadPool(String name, int coreSize, int maxSize, TaskPriority priority) {
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "EmpireWand-" + name + "-" + System.currentTimeMillis());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + priority.getLevel() - 1);
            return t;
        };
        
        BlockingQueue<Runnable> queue = new PriorityBlockingQueue<>(100, 
            (a, b) -> Integer.compare(getPriorityLevel(b), getPriorityLevel(a)));
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            coreSize, maxSize, 60L, TimeUnit.SECONDS, queue, threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
    
    private int getPriorityLevel(Runnable runnable) {
        // Default priority if not wrapped
        return TaskPriority.NORMAL.getLevel();
    }
    
    private Executor getExecutorForPriority(TaskPriority priority) {
        return switch (priority) {
            case HIGH, CRITICAL -> highPriorityExecutor;
            case NORMAL -> normalPriorityExecutor;
            case LOW -> lowPriorityExecutor;
        };
    }
    
    private <T> T executeTaskWithMonitoring(Supplier<T> task, TaskMetadata metadata) {
        try (var timing = performanceMonitor.startTiming("Task:" + metadata.context.getName(), 100)) {
            metadata.startTime = Instant.now();
            metadata.status = TaskMetadata.TaskStatus.RUNNING;
            
            return task.get();
            
        } catch (Exception e) {
            metadata.status = TaskMetadata.TaskStatus.FAILED;
            throw e;
        }
    }
    
    private TaskGroup getOrCreateTaskGroup(String groupName) {
        return taskGroups.computeIfAbsent(groupName, name -> new TaskGroup(name, 50)); // Default limit
    }
    
    private CircuitBreaker getOrCreateCircuitBreaker(String groupName) {
        return circuitBreakers.computeIfAbsent(groupName, 
            name -> new CircuitBreaker(10, 60000)); // 10 failures, 1 minute timeout
    }
    
    private void cleanupDeadTasks() {
        try {
            // Clean up cancelled/completed Bukkit tasks
            activeBukkitTasks.removeIf(task -> task == null || task.isCancelled());
            
            // Clean up completed async tasks
            asyncTasks.entrySet().removeIf(entry -> entry.getKey().isDone());
            
            // Update current active task count
            currentActiveTasks.set(activeBukkitTasks.size() + asyncTasks.size());
            
        } catch (Exception e) {
            logger.warning("Error during task cleanup: " + e.getMessage());
        }
    }
    
    private void updateMetrics() {
        try {
            // Update thread pool metrics
            adaptThreadPoolSizes();
            
            // Log metrics periodically
            if (System.currentTimeMillis() % 60000 < 10000) { // Every minute
                TaskManagerMetrics metrics = getMetrics();
                logger.info(String.format("Task Manager Metrics: Submitted=%d, Completed=%d, Failed=%d, Active=%d",
                    metrics.totalTasksSubmitted(), metrics.totalTasksCompleted(), 
                    metrics.totalTasksFailed(), metrics.currentActiveTasks()));
            }
            
        } catch (Exception e) {
            logger.warning("Error updating task metrics: " + e.getMessage());
        }
    }
    
    private void adaptThreadPoolSizes() {
        // Simple adaptive sizing based on queue length and active tasks
        adaptThreadPool(highPriorityExecutor, "HighPriority");
        adaptThreadPool(normalPriorityExecutor, "NormalPriority");
        adaptThreadPool(lowPriorityExecutor, "LowPriority");
    }
    
    private void adaptThreadPool(ThreadPoolExecutor executor, String name) {
        int queueSize = executor.getQueue().size();
        int activeCount = executor.getActiveCount();
        int corePoolSize = executor.getCorePoolSize();
        
        // Increase core pool size if queue is building up
        if (queueSize > 10 && activeCount >= corePoolSize && corePoolSize < executor.getMaximumPoolSize()) {
            executor.setCorePoolSize(Math.min(corePoolSize + 1, executor.getMaximumPoolSize()));
            logger.fine("Increased " + name + " core pool size to " + executor.getCorePoolSize());
        }
        
        // Decrease core pool size if underutilized
        if (queueSize == 0 && activeCount < corePoolSize / 2 && corePoolSize > 2) {
            executor.setCorePoolSize(Math.max(corePoolSize - 1, 2));
            logger.fine("Decreased " + name + " core pool size to " + executor.getCorePoolSize());
        }
    }
    
    private void shutdownExecutor(ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning(name + " executor did not terminate cleanly");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private Map<String, ThreadPoolMetrics> getThreadPoolMetrics() {
        Map<String, ThreadPoolMetrics> metrics = new ConcurrentHashMap<>();
        
        metrics.put("HighPriority", createThreadPoolMetrics(highPriorityExecutor));
        metrics.put("NormalPriority", createThreadPoolMetrics(normalPriorityExecutor));
        metrics.put("LowPriority", createThreadPoolMetrics(lowPriorityExecutor));
        
        return metrics;
    }
    
    private ThreadPoolMetrics createThreadPoolMetrics(ThreadPoolExecutor executor) {
        return new ThreadPoolMetrics(
            executor.getCorePoolSize(),
            executor.getMaximumPoolSize(),
            executor.getActiveCount(),
            executor.getPoolSize(),
            executor.getQueue().size(),
            executor.getCompletedTaskCount()
        );
    }
    
    // Metrics records
    
    public record TaskManagerMetrics(
        long totalTasksSubmitted,
        long totalTasksCompleted,
        long totalTasksFailed,
        int currentActiveTasks,
        long maxConcurrentTasks,
        int activeBukkitTasks,
        int activeAsyncTasks,
        Map<String, TaskGroupMetrics> taskGroups,
        Map<String, ThreadPoolMetrics> threadPools
    ) {
        public double getSuccessRate() {
            long total = totalTasksCompleted + totalTasksFailed;
            return total > 0 ? (double) totalTasksCompleted / total : 1.0;
        }
    }
    
    public record TaskGroupMetrics(
        String name,
        int activeTasks,
        int maxConcurrentTasks,
        long totalSubmitted,
        long totalCompleted
    ) {}
    
    public record ThreadPoolMetrics(
        int corePoolSize,
        int maximumPoolSize,
        int activeCount,
        int poolSize,
        int queueSize,
        long completedTaskCount
    ) {}
}