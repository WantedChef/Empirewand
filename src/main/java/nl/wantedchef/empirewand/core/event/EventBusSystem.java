package nl.wantedchef.empirewand.core.event;

import nl.wantedchef.empirewand.core.util.AdvancedPerformanceMonitor;
import org.bukkit.plugin.Plugin;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Instant;
import java.time.Duration;

/**
 * Enterprise-grade event bus system with advanced features:
 * - Type-safe event handling with generic support
 * - Asynchronous and synchronous event processing
 * - Event prioritization and ordering
 * - Dead event handling and debugging
 * - Event replay and audit trail
 * - Performance monitoring and metrics
 * - Circuit breaker for failing handlers
 * - Event filtering and transformation
 * - Hierarchical event inheritance
 * - Thread-safe concurrent processing
 */
public class EventBusSystem {
    
    private final Plugin plugin;
    private final Logger logger;
    private final AdvancedPerformanceMonitor performanceMonitor;
    
    // Core event handling
    private final Map<Class<?>, List<EventHandler>> eventHandlers = new ConcurrentHashMap<>();
    private final Map<String, List<EventHandler>> namedEventHandlers = new ConcurrentHashMap<>();
    private final List<EventHandler> globalHandlers = new CopyOnWriteArrayList<>();
    
    // Event processing
    private final ExecutorService asyncExecutor;
    private final ExecutorService syncExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Event audit and replay
    private final EventAuditLog auditLog;
    private final EventReplaySystem replaySystem;
    private final CircularBuffer<EventRecord> recentEvents;
    
    // Performance metrics
    private final LongAdder totalEventsPublished = new LongAdder();
    private final LongAdder totalEventsProcessed = new LongAdder();
    private final LongAdder totalHandlerFailures = new LongAdder();
    private final AtomicLong averageProcessingTime = new AtomicLong();
    
    // Circuit breakers for failing handlers
    private final Map<EventHandler, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    // Dead event handling
    private final List<Consumer<DeadEvent>> deadEventHandlers = new CopyOnWriteArrayList<>();
    
    // Event filtering and transformation
    private final List<EventFilter> globalFilters = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, List<EventTransformer<?>>> transformers = new ConcurrentHashMap<>();
    
    // Configuration
    private volatile boolean auditEnabled = true;
    private volatile boolean metricsEnabled = true;
    private volatile int maxRecentEvents = 10000;
    private volatile Duration handlerTimeout = Duration.ofSeconds(30);
    
    /**
     * Event handler annotation for automatic registration.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Subscribe {
        EventPriority priority() default EventPriority.NORMAL;
        boolean async() default false;
        String[] eventNames() default {};
        boolean ignoreCancelled() default false;
    }
    
    /**
     * Event priority levels.
     */
    public enum EventPriority {
        LOWEST(0),
        LOW(1),
        NORMAL(2),
        HIGH(3),
        HIGHEST(4),
        MONITOR(5);
        
        private final int level;
        
        EventPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * Base interface for all events.
     */
    public interface Event {
        default String getEventName() {
            return getClass().getSimpleName();
        }
        
        default Instant getTimestamp() {
            return Instant.now();
        }
        
        default Map<String, Object> getMetadata() {
            return Collections.emptyMap();
        }
    }
    
    /**
     * Cancellable event interface.
     */
    public interface CancellableEvent extends Event {
        boolean isCancelled();
        void setCancelled(boolean cancelled);
    }
    
    /**
     * Event handler wrapper with metadata.
     */
    private static class EventHandler implements Comparable<EventHandler> {
        private final Object listener;
        private final Method method;
        private final Class<?> eventType;
        private final EventPriority priority;
        private final boolean async;
        private final Set<String> eventNames;
        private final boolean ignoreCancelled;
        private final String handlerId;
        private final LongAdder invocationCount = new LongAdder();
        private final LongAdder failureCount = new LongAdder();
        private volatile long totalExecutionTime = 0;
        
        public EventHandler(Object listener, Method method, Subscribe annotation) {
            this.listener = listener;
            this.method = method;
            this.priority = annotation.priority();
            this.async = annotation.async();
            this.eventNames = Set.of(annotation.eventNames());
            this.ignoreCancelled = annotation.ignoreCancelled();
            this.handlerId = generateHandlerId(listener, method);
            
            // Determine event type from method parameter
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new IllegalArgumentException("Event handler must have exactly one parameter");
            }
            this.eventType = paramTypes[0];
            
            // Make method accessible
            method.setAccessible(true);
        }
        
        public void invoke(Object event) throws Exception {
            long startTime = System.nanoTime();
            try {
                invocationCount.increment();
                method.invoke(listener, event);
                
            } catch (Exception e) {
                failureCount.increment();
                throw e;
            } finally {
                long executionTime = System.nanoTime() - startTime;
                totalExecutionTime += executionTime;
            }
        }
        
        @Override
        public int compareTo(EventHandler other) {
            return Integer.compare(this.priority.getLevel(), other.priority.getLevel());
        }
        
        // Getters
        public Object getListener() { return listener; }
        public Method getMethod() { return method; }
        public Class<?> getEventType() { return eventType; }
        public EventPriority getPriority() { return priority; }
        public boolean isAsync() { return async; }
        public Set<String> getEventNames() { return eventNames; }
        public boolean isIgnoreCancelled() { return ignoreCancelled; }
        public String getHandlerId() { return handlerId; }
        public long getInvocationCount() { return invocationCount.sum(); }
        public long getFailureCount() { return failureCount.sum(); }
        public long getAverageExecutionTime() {
            long count = invocationCount.sum();
            return count > 0 ? totalExecutionTime / count : 0;
        }
        
        private String generateHandlerId(Object listener, Method method) {
            return listener.getClass().getSimpleName() + "#" + method.getName() + "@" + 
                   Integer.toHexString(listener.hashCode());
        }
    }
    
    /**
     * Dead event - fired when no handlers are found for an event.
     */
    public record DeadEvent(Object originalEvent, String reason, Instant timestamp) implements Event {}
    
    /**
     * Event record for audit trail.
     */
    public record EventRecord(
        String eventId,
        Object event,
        Instant timestamp,
        String source,
        List<String> handlerIds,
        Duration processingTime,
        boolean successful,
        String errorMessage
    ) {}
    
    /**
     * Event filter interface for pre-processing events.
     */
    @FunctionalInterface
    public interface EventFilter {
        boolean shouldProcess(Object event);
    }
    
    /**
     * Event transformer interface for modifying events before processing.
     */
    @FunctionalInterface
    public interface EventTransformer<T> {
        T transform(T event);
    }
    
    /**
     * Circuit breaker for failing event handlers.
     */
    private static class CircuitBreaker {
        private final int failureThreshold;
        private final Duration timeout;
        private final AtomicLong failureCount = new AtomicLong();
        private volatile long lastFailureTime = 0;
        private volatile State state = State.CLOSED;
        
        public enum State {
            CLOSED, OPEN, HALF_OPEN
        }
        
        public CircuitBreaker(int failureThreshold, Duration timeout) {
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
        }
        
        public boolean allowExecution() {
            if (state == State.CLOSED) {
                return true;
            }
            
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime >= timeout.toMillis()) {
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
            long failures = failureCount.incrementAndGet();
            
            if (failures >= failureThreshold) {
                state = State.OPEN;
            }
        }
        
        public State getState() { return state; }
        public long getFailureCount() { return failureCount.get(); }
    }
    
    /**
     * Circular buffer for storing recent events.
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
    
    /**
     * Event audit log for tracking all events.
     */
    private static class EventAuditLog {
        private final CircularBuffer<EventRecord> auditTrail;
        private final AtomicLong eventIdCounter = new AtomicLong();
        
        public EventAuditLog(int maxRecords) {
            this.auditTrail = new CircularBuffer<>(maxRecords);
        }
        
        public String recordEvent(Object event, List<String> handlerIds, Duration processingTime, 
                                boolean successful, String errorMessage) {
            String eventId = "event-" + eventIdCounter.incrementAndGet();
            EventRecord record = new EventRecord(
                eventId, event, Instant.now(), Thread.currentThread().getName(),
                handlerIds, processingTime, successful, errorMessage
            );
            auditTrail.add(record);
            return eventId;
        }
        
        public List<EventRecord> getRecentEvents(int count) {
            return auditTrail.getRecent(count);
        }
    }
    
    /**
     * Event replay system for debugging and testing.
     */
    private static class EventReplaySystem {
        private final Map<String, Object> capturedEvents = new ConcurrentHashMap<>();
        private volatile boolean capturing = false;
        
        public void startCapture() {
            capturing = true;
            capturedEvents.clear();
        }
        
        public void stopCapture() {
            capturing = false;
        }
        
        public void captureEvent(String eventId, Object event) {
            if (capturing) {
                capturedEvents.put(eventId, event);
            }
        }
        
        public Map<String, Object> getCapturedEvents() {
            return new HashMap<>(capturedEvents);
        }
    }
    
    public EventBusSystem(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        this.logger = plugin.getLogger();
        this.performanceMonitor = new AdvancedPerformanceMonitor(plugin, logger);
        
        // Initialize executors
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "EmpireWand-EventBus-Async");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
        
        this.syncExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "EmpireWand-EventBus-Sync");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 1);
            return t;
        });
        
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "EmpireWand-EventBus-Scheduled");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        
        // Initialize audit and replay systems
        this.auditLog = new EventAuditLog(maxRecentEvents);
        this.replaySystem = new EventReplaySystem();
        this.recentEvents = new CircularBuffer<>(maxRecentEvents);
        
        // Start monitoring
        performanceMonitor.startMonitoring();
        startMaintenanceTasks();
        
        logger.info("EventBusSystem initialized with enterprise features");
    }
    
    /**
     * Registers an event listener with automatic method scanning.
     */
    public void register(Object listener) {
        Objects.requireNonNull(listener);
        
        Class<?> listenerClass = listener.getClass();
        Method[] methods = listenerClass.getDeclaredMethods();
        
        for (Method method : methods) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation != null) {
                try {
                    EventHandler handler = new EventHandler(listener, method, annotation);
                    registerHandler(handler);
                    
                    logger.fine("Registered event handler: " + handler.getHandlerId() + 
                              " for event type: " + handler.getEventType().getSimpleName());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to register event handler: " + method, e);
                }
            }
        }
    }
    
    /**
     * Registers a lambda-based event handler.
     */
    public <T> void register(Class<T> eventType, Consumer<T> handler) {
        register(eventType, handler, EventPriority.NORMAL, false);
    }
    
    /**
     * Registers a lambda-based event handler with priority and async options.
     */
    public <T> void register(Class<T> eventType, Consumer<T> handler, EventPriority priority, boolean async) {
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(handler);
        
        // Create a wrapper handler
        EventHandler eventHandler = new EventHandler(handler, createLambdaMethod(handler), 
                                                     createSubscribeAnnotation(priority, async)) {
            @Override
            public void invoke(Object event) throws Exception {
                if (eventType.isInstance(event)) {
                    @SuppressWarnings("unchecked")
                    T typedEvent = (T) event;
                    handler.accept(typedEvent);
                }
            }
        };
        
        registerHandler(eventHandler);
    }
    
    /**
     * Registers a string-based event handler for named events.
     * This allows for more flexible event handling without requiring specific classes.
     */
    public void register(String eventName, Consumer<Object> handler) {
        register(eventName, handler, EventPriority.NORMAL, false);
    }
    
    /**
     * Registers a string-based event handler with priority and async options.
     */
    public void register(String eventName, Consumer<Object> handler, EventPriority priority, boolean async) {
        Objects.requireNonNull(eventName);
        Objects.requireNonNull(handler);
        
        // Create a wrapper handler for named events
        EventHandler eventHandler = new EventHandler(handler, createLambdaMethod(handler), 
                                                     createSubscribeAnnotation(priority, async)) {
            @Override
            public void invoke(Object event) throws Exception {
                handler.accept(event);
            }
        };
        
        // Add to named event handlers
        namedEventHandlers.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(eventHandler);
        
        logger.info("Registered handler for named event: " + eventName);
    }
    
    /**
     * Publishes a named event to registered string-based handlers.
     */
    public CompletableFuture<Void> publish(String eventName, Object eventData) {
        Objects.requireNonNull(eventName);
        
        return CompletableFuture.runAsync(() -> {
            try (var timing = performanceMonitor.startTiming("EventBus.publishNamed:" + eventName, 50)) {
                timing.observe();
                
                totalEventsPublished.increment();
                
                List<EventHandler> handlers = namedEventHandlers.get(eventName);
                if (handlers == null || handlers.isEmpty()) {
                    handleDeadEvent(new DeadEvent(eventData, "No handlers for named event: " + eventName, Instant.now()));
                    return;
                }
                
                // Process handlers
                processHandlers(eventData != null ? eventData : eventName, handlers);
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error publishing named event: " + eventName, e);
            }
        }, asyncExecutor);
    }
    
    /**
     * Publishes an event to all registered handlers.
     */
    public <T> CompletableFuture<Void> publish(T event) {
        Objects.requireNonNull(event);
        
        return CompletableFuture.runAsync(() -> {
            try (var timing = performanceMonitor.startTiming("EventBus.publish:" + event.getClass().getSimpleName(), 50)) {
                timing.observe();
                
                totalEventsPublished.increment();
                
                // Apply global filters
                if (!applyFilters(event)) {
                    return;
                }
                
                // Transform event if needed
                Object transformedEvent = applyTransformers(event);
                
                // Find handlers
                List<EventHandler> handlers = findHandlers(transformedEvent);
                
                if (handlers.isEmpty()) {
                    handleDeadEvent(new DeadEvent(event, "No handlers found", Instant.now()));
                    return;
                }
                
                // Process handlers
                processHandlers(transformedEvent, handlers);
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error publishing event: " + event.getClass().getSimpleName(), e);
            }
        }, syncExecutor);
    }
    
    /**
     * Adds a global event filter.
     */
    public void addGlobalFilter(EventFilter filter) {
        globalFilters.add(filter);
    }
    
    /**
     * Adds an event transformer for a specific event type.
     */
    public <T> void addTransformer(Class<T> eventType, EventTransformer<T> transformer) {
        @SuppressWarnings("unchecked")
        List<EventTransformer<?>> transformerList = transformers.computeIfAbsent(eventType, 
            k -> new CopyOnWriteArrayList<>());
        transformerList.add(transformer);
    }
    
    /**
     * Adds a dead event handler.
     */
    public void addDeadEventHandler(Consumer<DeadEvent> handler) {
        deadEventHandlers.add(handler);
    }
    
    /**
     * Gets event bus metrics.
     */
    public EventBusMetrics getMetrics() {
        Map<String, HandlerMetrics> handlerMetrics = new HashMap<>();
        
        eventHandlers.values().stream()
            .flatMap(List::stream)
            .forEach(handler -> {
                handlerMetrics.put(handler.getHandlerId(), new HandlerMetrics(
                    handler.getHandlerId(),
                    handler.getInvocationCount(),
                    handler.getFailureCount(),
                    handler.getAverageExecutionTime(),
                    circuitBreakers.get(handler) != null ? circuitBreakers.get(handler).getState().name() : "CLOSED"
                ));
            });
        
        return new EventBusMetrics(
            totalEventsPublished.sum(),
            totalEventsProcessed.sum(),
            totalHandlerFailures.sum(),
            averageProcessingTime.get(),
            eventHandlers.size(),
            handlerMetrics,
            recentEvents.size()
        );
    }
    
    /**
     * Starts event replay capture.
     */
    public void startEventCapture() {
        replaySystem.startCapture();
        logger.info("Event capture started");
    }
    
    /**
     * Stops event replay capture and returns captured events.
     */
    public Map<String, Object> stopEventCapture() {
        replaySystem.stopCapture();
        Map<String, Object> captured = replaySystem.getCapturedEvents();
        logger.info("Event capture stopped. Captured " + captured.size() + " events");
        return captured;
    }
    
    /**
     * Gets recent event records for debugging.
     */
    public List<EventRecord> getRecentEvents(int count) {
        return auditLog.getRecentEvents(count);
    }
    
    /**
     * Shuts down the event bus system.
     */
    public void shutdown() {
        logger.info("Shutting down EventBusSystem...");
        
        try {
            // Shutdown executors
            asyncExecutor.shutdown();
            syncExecutor.shutdown();
            scheduledExecutor.shutdown();
            
            // Wait for completion
            if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
            if (!syncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
            
            // Stop monitoring
            performanceMonitor.stopMonitoring();
            
            // Clear handlers and data
            eventHandlers.clear();
            namedEventHandlers.clear();
            globalHandlers.clear();
            circuitBreakers.clear();
            
            logger.info("EventBusSystem shutdown complete");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during EventBusSystem shutdown", e);
        }
    }
    
    // Private implementation methods
    
    private void registerHandler(EventHandler handler) {
        // Register by event type
        eventHandlers.computeIfAbsent(handler.getEventType(), k -> new CopyOnWriteArrayList<>())
                   .add(handler);
        
        // Register by event names if specified
        for (String eventName : handler.getEventNames()) {
            namedEventHandlers.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>())
                             .add(handler);
        }
        
        // Sort handlers by priority
        List<EventHandler> handlers = eventHandlers.get(handler.getEventType());
        if (handlers != null) {
            handlers.sort(Comparator.comparingInt(h -> h.getPriority().getLevel()));
        }
        
        // Initialize circuit breaker
        circuitBreakers.put(handler, new CircuitBreaker(10, Duration.ofMinutes(5)));
    }
    
    private boolean applyFilters(Object event) {
        for (EventFilter filter : globalFilters) {
            try {
                if (!filter.shouldProcess(event)) {
                    return false;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in event filter", e);
            }
        }
        return true;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T applyTransformers(T event) {
        Class<?> eventClass = event.getClass();
        List<EventTransformer<?>> transformerList = transformers.get(eventClass);
        
        if (transformerList != null) {
            for (EventTransformer<?> transformer : transformerList) {
                try {
                    EventTransformer<T> typedTransformer = (EventTransformer<T>) transformer;
                    event = typedTransformer.transform(event);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error in event transformer", e);
                }
            }
        }
        
        return event;
    }
    
    private List<EventHandler> findHandlers(Object event) {
        List<EventHandler> handlers = new ArrayList<>();
        
        // Find handlers by exact type
        Class<?> eventClass = event.getClass();
        List<EventHandler> typeHandlers = eventHandlers.get(eventClass);
        if (typeHandlers != null) {
            handlers.addAll(typeHandlers);
        }
        
        // Find handlers by superclasses and interfaces
        findInheritanceHandlers(eventClass, handlers);
        
        // Find named handlers if it's a named event
        if (event instanceof NamedEvent namedEvent) {
            List<EventHandler> namedHandlers = namedEventHandlers.get(namedEvent.getName());
            if (namedHandlers != null) {
                handlers.addAll(namedHandlers);
            }
        }
        
        // Add global handlers
        handlers.addAll(globalHandlers);
        
        // Sort by priority
        handlers.sort(EventHandler::compareTo);
        
        return handlers;
    }
    
    private void findInheritanceHandlers(Class<?> eventClass, List<EventHandler> handlers) {
        // Check superclasses
        Class<?> superClass = eventClass.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            List<EventHandler> superHandlers = eventHandlers.get(superClass);
            if (superHandlers != null) {
                handlers.addAll(superHandlers);
            }
            superClass = superClass.getSuperclass();
        }
        
        // Check interfaces
        for (Class<?> interfaceClass : eventClass.getInterfaces()) {
            List<EventHandler> interfaceHandlers = eventHandlers.get(interfaceClass);
            if (interfaceHandlers != null) {
                handlers.addAll(interfaceHandlers);
            }
        }
    }
    
    private void processHandlers(Object event, List<EventHandler> handlers) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<String> handlerIds = new ArrayList<>();
        boolean successful = true;
        String errorMessage = null;
        
        long startTime = System.nanoTime();
        
        for (EventHandler handler : handlers) {
            handlerIds.add(handler.getHandlerId());
            
            // Check if event is cancelled and handler ignores cancelled events
            if (event instanceof CancellableEvent cancellable && 
                cancellable.isCancelled() && 
                handler.isIgnoreCancelled()) {
                continue;
            }
            
            // Check circuit breaker
            CircuitBreaker circuitBreaker = circuitBreakers.get(handler);
            if (circuitBreaker != null && !circuitBreaker.allowExecution()) {
                continue;
            }
            
            // Execute handler
            CompletableFuture<Void> future = executeHandler(handler, event, circuitBreaker);
            futures.add(future);
        }
        
        // Wait for all handlers to complete
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allFutures.get(handlerTimeout.toMillis(), TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            successful = false;
            errorMessage = e.getMessage();
            totalHandlerFailures.increment();
            logger.log(Level.WARNING, "Error processing event handlers", e);
        }
        
        long processingTime = System.nanoTime() - startTime;
        totalEventsProcessed.increment();
        
        // Update average processing time
        averageProcessingTime.set((averageProcessingTime.get() + processingTime / 1_000_000) / 2);
        
        // Record in audit log
        if (auditEnabled) {
            String eventId = auditLog.recordEvent(event, handlerIds, 
                Duration.ofNanos(processingTime), successful, errorMessage);
            
            // Add to recent events
            EventRecord record = new EventRecord(eventId, event, Instant.now(), 
                Thread.currentThread().getName(), handlerIds, 
                Duration.ofNanos(processingTime), successful, errorMessage);
            recentEvents.add(record);
            
            // Capture for replay if enabled
            replaySystem.captureEvent(eventId, event);
        }
    }
    
    private CompletableFuture<Void> executeHandler(EventHandler handler, Object event, CircuitBreaker circuitBreaker) {
        ExecutorService executor = handler.isAsync() ? asyncExecutor : syncExecutor;
        
        return CompletableFuture.runAsync(() -> {
            try {
                handler.invoke(event);
                if (circuitBreaker != null) {
                    circuitBreaker.recordSuccess();
                }
            } catch (Exception e) {
                if (circuitBreaker != null) {
                    circuitBreaker.recordFailure();
                }
                logger.log(Level.WARNING, "Event handler failed: " + handler.getHandlerId(), e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    private void handleDeadEvent(DeadEvent deadEvent) {
        for (Consumer<DeadEvent> handler : deadEventHandlers) {
            try {
                handler.accept(deadEvent);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in dead event handler", e);
            }
        }
        
        logger.fine("Dead event: " + deadEvent.reason() + " for event: " + 
                   deadEvent.originalEvent().getClass().getSimpleName());
    }
    
    private void startMaintenanceTasks() {
        // Periodic cleanup task
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                // Reset circuit breakers that have been closed for a while
                circuitBreakers.entrySet().removeIf(entry -> {
                    CircuitBreaker cb = entry.getValue();
                    return cb.getState() == CircuitBreaker.State.CLOSED && cb.getFailureCount() == 0;
                });
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error during EventBus maintenance", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
        
        // Metrics logging
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            if (metricsEnabled && logger.isLoggable(Level.FINE)) {
                EventBusMetrics metrics = getMetrics();
                logger.fine("EventBus Metrics: " + metrics);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    // Helper methods for lambda registration
    
    private Method createLambdaMethod(Consumer<?> handler) {
        // Create a dummy method for lambda handlers
        try {
            return Consumer.class.getMethod("accept", Object.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    
    private Subscribe createSubscribeAnnotation(EventPriority priority, boolean async) {
        return new Subscribe() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Subscribe.class;
            }
            
            @Override
            public EventPriority priority() {
                return priority;
            }
            
            @Override
            public boolean async() {
                return async;
            }
            
            @Override
            public String[] eventNames() {
                return new String[0];
            }
            
            @Override
            public boolean ignoreCancelled() {
                return false;
            }
        };
    }
    
    // Event types
    
    /**
     * Named event for string-based event publishing.
     */
    public static class NamedEvent implements Event {
        private final String name;
        private final Object data;
        private final Instant timestamp;
        private final Map<String, Object> metadata;
        
        public NamedEvent(String name, Object data) {
            this(name, data, Collections.emptyMap());
        }
        
        public NamedEvent(String name, Object data, Map<String, Object> metadata) {
            this.name = name;
            this.data = data;
            this.timestamp = Instant.now();
            this.metadata = Map.copyOf(metadata);
        }
        
        public String getName() { return name; }
        public Object getData() { return data; }
        
        @Override
        public String getEventName() { return name; }
        
        @Override
        public Instant getTimestamp() { return timestamp; }
        
        @Override
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    // Metrics records
    
    public record EventBusMetrics(
        long totalEventsPublished,
        long totalEventsProcessed,
        long totalHandlerFailures,
        long averageProcessingTimeNs,
        int registeredEventTypes,
        Map<String, HandlerMetrics> handlerMetrics,
        int recentEventsCount
    ) {
        public double getSuccessRate() {
            return totalEventsProcessed > 0 ? 
                (double) (totalEventsProcessed - totalHandlerFailures) / totalEventsProcessed : 1.0;
        }
    }
    
    public record HandlerMetrics(
        String handlerId,
        long invocationCount,
        long failureCount,
        long averageExecutionTimeNs,
        String circuitBreakerState
    ) {
        public double getSuccessRate() {
            return invocationCount > 0 ? 
                (double) (invocationCount - failureCount) / invocationCount : 1.0;
        }
    }
}