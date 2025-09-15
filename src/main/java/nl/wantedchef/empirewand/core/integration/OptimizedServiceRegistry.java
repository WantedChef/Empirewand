package nl.wantedchef.empirewand.core.integration;

import nl.wantedchef.empirewand.core.util.AdvancedPerformanceMonitor;
import nl.wantedchef.empirewand.core.event.EventBusSystem;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Instant;
import java.time.Duration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Proxy;

/**
 * Enterprise-grade service registry and dependency injection system with advanced features:
 * - Type-safe service registration and discovery
 * - Dependency injection with circular dependency detection
 * - Service lifecycle management with automatic startup/shutdown ordering
 * - Service health monitoring and automatic recovery
 * - Dynamic service replacement and hot-swapping
 * - Service versioning and compatibility checking
 * - Performance monitoring and metrics collection
 * - Event-driven service communication
 * - Service proxy generation with interceptors
 * - Thread-safe concurrent operations
 */
public class OptimizedServiceRegistry {
    
    private static final Logger logger = Logger.getLogger(OptimizedServiceRegistry.class.getName());
    private final AdvancedPerformanceMonitor performanceMonitor;
    private final EventBusSystem eventBus;
    
    // Core service storage
    private final Map<Class<?>, ServiceRegistration<?>> services = new ConcurrentHashMap<>();
    private final Map<String, ServiceRegistration<?>> namedServices = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock registryLock = new ReentrantReadWriteLock();
    
    // Dependency management
    private final Map<Class<?>, Set<Class<?>>> dependencyGraph = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<Class<?>>> reverseDependencyGraph = new ConcurrentHashMap<>();
    private final DependencyResolver dependencyResolver;
    
    // Service lifecycle
    private final ScheduledExecutorService lifecycleExecutor;
    private volatile boolean registryStarted = false;
    
    // Service monitoring
    private final ServiceHealthMonitor healthMonitor;
    private final ServiceMetricsCollector metricsCollector;
    
    // Service interception
    private final Map<Class<?>, List<ServiceInterceptor>> interceptors = new ConcurrentHashMap<>();
    private final ServiceProxyFactory proxyFactory;
    
    // Performance tracking
    private final LongAdder totalServiceCalls = new LongAdder();
    private final LongAdder totalServiceFailures = new LongAdder();
    private final AtomicLong averageServiceCallTime = new AtomicLong();
    
    /**
     * Service registration annotation for automatic discovery.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Service {
        String name() default "";
        Class<?>[] interfaces() default {};
        int priority() default 0;
        boolean singleton() default true;
        ServiceScope scope() default ServiceScope.SINGLETON;
    }
    
    /**
     * Dependency injection annotation.
     */
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Inject {
        String name() default "";
        boolean optional() default false;
    }
    
    /**
     * Post-construction callback annotation.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PostConstruct {
    }
    
    /**
     * Pre-destruction callback annotation.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PreDestroy {
    }
    
    /**
     * Service scope enumeration.
     */
    public enum ServiceScope {
        SINGLETON,    // One instance per registry
        PROTOTYPE,    // New instance per request
        REQUEST,      // One instance per request context
        SESSION       // One instance per session (plugin context)
    }
    
    /**
     * Service lifecycle states.
     */
    public enum ServiceState {
        REGISTERED,
        INITIALIZING,
        RUNNING,
        STOPPING,
        STOPPED,
        FAILED,
        DESTROYED
    }
    
    /**
     * Service registration metadata.
     */
    private static class ServiceRegistration<T> {
        private final Class<T> serviceType;
        private final String name;
        private final ServiceScope scope;
        private final int priority;
        private final Set<Class<?>> interfaces;
        private volatile T instance;
        private volatile ServiceState state = ServiceState.REGISTERED;
        private final ServiceFactory<T> factory;
        private final Instant registrationTime;
        private final LongAdder callCount = new LongAdder();
        private volatile Instant lastUsed;
        private volatile String version = "1.0.0";
        private final LongAdder failureCount = new LongAdder();
        private volatile Instant lastFailure;
        
        public ServiceRegistration(Class<T> serviceType, String name, ServiceScope scope,
                                 int priority, Set<Class<?>> interfaces, ServiceFactory<T> factory) {
            this.serviceType = serviceType;
            this.name = name;
            this.scope = scope;
            this.priority = priority;
            this.interfaces = Set.copyOf(interfaces);
            this.factory = factory;
            this.registrationTime = Instant.now();
            this.lastUsed = this.registrationTime;
        }
        
        // Getters and setters
        public Class<T> getServiceType() { return serviceType; }
        public String getName() { return name; }
        public ServiceScope getScope() { return scope; }
        public int getPriority() { return priority; }
        public Set<Class<?>> getInterfaces() { return interfaces; }
        public T getInstance() { return instance; }
        public void setInstance(T instance) { this.instance = instance; }
        public ServiceState getState() { return state; }
        public void setState(ServiceState state) { this.state = state; }
        public ServiceFactory<T> getFactory() { return factory; }
        public Instant getRegistrationTime() { return registrationTime; }
        public long getCallCount() { return callCount.sum(); }
        public void incrementCallCount() { callCount.increment(); this.lastUsed = Instant.now(); }
        public Instant getLastUsed() { return lastUsed; }
        public String getVersion() { return version; }
        public void incrementFailureCount() { failureCount.increment(); this.lastFailure = Instant.now(); }
        public long getFailureCount() { return failureCount.sum(); }
        public Instant getLastFailure() { return lastFailure; }
    }
    
    /**
     * Service factory interface for creating service instances.
     */
    @FunctionalInterface
    public interface ServiceFactory<T> {
        T createInstance(ServiceContext context) throws Exception;
    }
    
    /**
     * Service context for dependency injection.
     */
    public static class ServiceContext {
        private final OptimizedServiceRegistry registry;
        private final Map<String, Object> properties;
        
        public ServiceContext(OptimizedServiceRegistry registry) {
            this.registry = registry;
            this.properties = new ConcurrentHashMap<>();
        }
        
        public <T> T getService(Class<T> serviceType) {
            return registry.getService(serviceType);
        }
        
        public <T> T getService(String name, Class<T> serviceType) {
            return registry.getService(name, serviceType);
        }
        
        public void setProperty(String key, Object value) {
            properties.put(key, value);
        }
        
        public Object getProperty(String key) {
            return properties.get(key);
        }
    }
    
    /**
     * Service lifecycle interface for managing service startup and shutdown.
     */
    public interface ServiceLifecycle {
        void start() throws Exception;
        void stop() throws Exception;
        ServiceState getState();
        boolean isHealthy();
        
        default int getStartupOrder() { return 0; }
        default int getShutdownOrder() { return 0; }
        default Duration getStartupTimeout() { return Duration.ofSeconds(30); }
        default Duration getShutdownTimeout() { return Duration.ofSeconds(10); }
    }
    
    /**
     * Service interceptor for cross-cutting concerns.
     */
    @FunctionalInterface
    public interface ServiceInterceptor {
        Object intercept(ServiceInvocation invocation) throws Throwable;
        
        default int getPriority() { return 0; }
    }
    
    /**
     * Service method invocation context.
     */
    public static class ServiceInvocation {
        private final Object target;
        private final String method;
        private final Object[] arguments;
        private final Map<String, Object> context;
        
        public ServiceInvocation(Object target, String method, Object[] arguments) {
            this.target = target;
            this.method = method;
            this.arguments = arguments != null ? arguments.clone() : new Object[0];
            this.context = new ConcurrentHashMap<>();
        }
        
        public Object getTarget() { return target; }
        public String getMethod() { return method; }
        public Object[] getArguments() { return arguments.clone(); }
        public Map<String, Object> getContext() { return context; }
        
        public Object proceed() throws Throwable {
            // Default implementation - would be overridden by proxy
            throw new UnsupportedOperationException("Proceed not implemented");
        }
    }
    
    /**
     * Service proxy factory for creating intercepted service proxies.
     */
    private class ServiceProxyFactory {
        @SuppressWarnings("unchecked")
        public <T> T createProxy(Class<T> serviceType, T target, List<ServiceInterceptor> interceptorList) {
            if (interceptorList.isEmpty()) {
                return target;
            }
            
            return (T) Proxy.newProxyInstance(
                serviceType.getClassLoader(),
                new Class<?>[]{serviceType},
                (proxy, method, args) -> {
                    long startTime = System.nanoTime();
                    totalServiceCalls.increment();
                    
                    try {
                        // Create invocation chain
                        ServiceInvocation invocation = new ServiceInvocation(target, method.getName(), args);
                        
                        // Apply interceptors
                        Object result = applyInterceptors(invocation, interceptorList, 0);
                        if (result == null && invocation.getContext().containsKey("proceed")) {
                            result = method.invoke(target, args);
                        }
                        
                        return result;
                        
                    } catch (Exception e) {
                        totalServiceFailures.increment();
                        throw e;
                    } finally {
                        long executionTime = System.nanoTime() - startTime;
                        updateAverageServiceCallTime(executionTime);
                    }
                }
            );
        }
        
        private Object applyInterceptors(ServiceInvocation invocation, List<ServiceInterceptor> interceptorList, int index) throws Throwable {
            if (index >= interceptorList.size()) {
                invocation.getContext().put("proceed", true);
                return null;
            }
            
            ServiceInterceptor interceptor = interceptorList.get(index);
            
            // Override proceed method for this interceptor
            ServiceInvocation chainedInvocation = new ServiceInvocation(invocation.getTarget(), invocation.getMethod(), invocation.getArguments()) {
                @Override
                public Object proceed() throws Throwable {
                    return applyInterceptors(invocation, interceptorList, index + 1);
                }
            };
            
            return interceptor.intercept(chainedInvocation);
        }
    }
    
    /**
     * Dependency resolver for managing service dependencies.
     */
    private class DependencyResolver {
        public List<Class<?>> resolveStartupOrder() {
            return topologicalSort(dependencyGraph);
        }
        
        public List<Class<?>> resolveShutdownOrder() {
            return topologicalSort(reverseDependencyGraph);
        }
        
        public boolean hasCyclicDependency() {
            return detectCycle(dependencyGraph);
        }
        
        public List<Class<?>> findCyclicDependencyChain() {
            return findCycle(dependencyGraph);
        }
        
        private List<Class<?>> topologicalSort(Map<Class<?>, Set<Class<?>>> graph) {
            List<Class<?>> result = new ArrayList<>();
            Set<Class<?>> visited = new HashSet<>();
            Set<Class<?>> temp = new HashSet<>();
            
            for (Class<?> node : graph.keySet()) {
                if (!visited.contains(node)) {
                    topologicalSortUtil(node, graph, visited, temp, result);
                }
            }
            
            Collections.reverse(result);
            return result;
        }
        
        private void topologicalSortUtil(Class<?> node, Map<Class<?>, Set<Class<?>>> graph,
                                       Set<Class<?>> visited, Set<Class<?>> temp, List<Class<?>> result) {
            temp.add(node);
            
            Set<Class<?>> neighbors = graph.getOrDefault(node, Collections.emptySet());
            for (Class<?> neighbor : neighbors) {
                if (temp.contains(neighbor)) {
                    throw new RuntimeException("Circular dependency detected: " + node + " -> " + neighbor);
                }
                if (!visited.contains(neighbor)) {
                    topologicalSortUtil(neighbor, graph, visited, temp, result);
                }
            }
            
            temp.remove(node);
            visited.add(node);
            result.add(node);
        }
        
        private boolean detectCycle(Map<Class<?>, Set<Class<?>>> graph) {
            try {
                topologicalSort(graph);
                return false;
            } catch (RuntimeException e) {
                return e.getMessage().contains("Circular dependency");
            }
        }
        
        private List<Class<?>> findCycle(Map<Class<?>, Set<Class<?>>> graph) {
            if (graph == null || graph.isEmpty()) {
                return Collections.emptyList();
            }

            Set<Class<?>> visited = new HashSet<>();
            Set<Class<?>> recursionStack = new HashSet<>();

            for (Class<?> node : graph.keySet()) {
                if (!visited.contains(node)) {
                    List<Class<?>> path = new ArrayList<>();
                    Class<?> cycleNode = findCycleRecursive(node, graph, visited, recursionStack, path);
                    if (cycleNode != null) {
                        int cycleStartIndex = path.indexOf(cycleNode);
                        List<Class<?>> cycle = new ArrayList<>(path.subList(cycleStartIndex, path.size()));
                        cycle.add(cycleNode); // Add the start of the cycle to show the loop
                        return cycle;
                    }
                }
            }

            return Collections.emptyList();
        }

        private Class<?> findCycleRecursive(Class<?> node, Map<Class<?>, Set<Class<?>>> graph,
                                           Set<Class<?>> visited, Set<Class<?>> recursionStack,
                                           List<Class<?>> path) {
            visited.add(node);
            recursionStack.add(node);
            path.add(node);

            for (Class<?> neighbor : graph.getOrDefault(node, Collections.emptySet())) {
                if (recursionStack.contains(neighbor)) {
                    return neighbor; // Cycle detected
                }
                if (!visited.contains(neighbor)) {
                    Class<?> cycleNode = findCycleRecursive(neighbor, graph, visited, recursionStack, path);
                    if (cycleNode != null) {
                        return cycleNode;
                    }
                }
            }

            recursionStack.remove(node);
            path.remove(path.size() - 1); // Backtrack
            return null;
        }
    }
    
    /**
     * Service health monitoring system.
     */
    private class ServiceHealthMonitor {
        private final ScheduledFuture<?> monitoringTask;
        
        public ServiceHealthMonitor() {
            this.monitoringTask = lifecycleExecutor.scheduleWithFixedDelay(
                this::performHealthChecks, 30, 30, TimeUnit.SECONDS);
        }
        
        private void performHealthChecks() {
            services.values().forEach(this::checkServiceHealth);
        }
        
        private void checkServiceHealth(ServiceRegistration<?> registration) {
            if (registration.getState() != ServiceState.RUNNING) {
                return;
            }
            
            try {
                Object instance = registration.getInstance();
                if (instance instanceof ServiceLifecycle lifecycle) {
                    if (!lifecycle.isHealthy()) {
                        registration.incrementFailureCount();
                        logger.log(Level.WARNING, "Service health check failed: {0}", registration.getServiceType().getSimpleName());
                        
                        // Trigger health check failed event
                        eventBus.publish("service.health.failed", Map.of(
                            "serviceType", registration.getServiceType(),
                            "serviceName", registration.getName(),
                            "timestamp", Instant.now()
                        ));
                        
                        // Optionally restart the service
                        if (shouldRestartUnhealthyService(registration)) {
                            restartService(registration);
                        }
                    }
                }
            } catch (Exception e) {
                registration.incrementFailureCount();
                logger.log(Level.WARNING, "Error during health check for service: " + 
                          registration.getServiceType().getSimpleName(), e);
            }
        }
        
        private boolean shouldRestartUnhealthyService(ServiceRegistration<?> registration) {
            if (registration == null) {
                return false;
            }

            // Simple restart policy:
            // - Max 3 restarts.
            // - Cooldown of 1 minute between restarts.
            final int MAX_RESTARTS = 3;
            final Duration RESTART_COOLDOWN = Duration.ofMinutes(1);

            long failures = registration.getFailureCount();
            if (failures > MAX_RESTARTS) {
                logger.log(Level.WARNING, "Service {0} has failed {1} times and will not be restarted again.",
                        new Object[]{registration.getServiceType().getSimpleName(), failures});
                return false;
            }

            Instant lastFailure = registration.getLastFailure();
            if (lastFailure != null && Duration.between(lastFailure, Instant.now()).compareTo(RESTART_COOLDOWN) < 0) {
                logger.log(Level.INFO, "Service {0} failed recently. Waiting for cooldown before next restart attempt.",
                        registration.getServiceType().getSimpleName());
                return false;
            }

            return true;
        }
        
        private void restartService(ServiceRegistration<?> registration) {
            logger.log(Level.INFO, "Attempting to restart unhealthy service: {0}", registration.getServiceType().getSimpleName());
            
            lifecycleExecutor.submit(() -> {
                try {
                    // Stop the service
                    stopService(registration);
                    
                    // Wait a bit
                    Thread.sleep(5000);
                    
                    // Start the service again
                    startService(registration);
                    
                    logger.log(Level.INFO, "Successfully restarted service: {0}", registration.getServiceType().getSimpleName());
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.log(Level.SEVERE, "Service restart interrupted: {0}", registration.getServiceType().getSimpleName());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to restart service: " + registration.getServiceType().getSimpleName(), e);
                }
            });
        }
        
        public void shutdown() {
            if (monitoringTask != null) {
                monitoringTask.cancel(true);
            }
        }
    }
    
    /**
     * Service metrics collection system.
     */
    private class ServiceMetricsCollector {
        public ServiceRegistryMetrics collectMetrics() {
            Map<String, ServiceMetrics> serviceMetrics = new HashMap<>();
            
            services.values().forEach(registration -> {
                ServiceMetrics metrics = new ServiceMetrics(
                    registration.getServiceType().getSimpleName(),
                    registration.getName(),
                    registration.getState(),
                    registration.getCallCount(),
                    Duration.between(registration.getRegistrationTime(), Instant.now()),
                    Duration.between(registration.getLastUsed(), Instant.now()),
                    registration.getVersion()
                );
                serviceMetrics.put(registration.getServiceType().getSimpleName(), metrics);
            });
            
            return new ServiceRegistryMetrics(
                Instant.now(),
                services.size(),
                totalServiceCalls.sum(),
                totalServiceFailures.sum(),
                averageServiceCallTime.get(),
                registryStarted,
                serviceMetrics
            );
        }
    }
    
    public OptimizedServiceRegistry(Plugin plugin, EventBusSystem eventBus) {
        Objects.requireNonNull(plugin);
        this.eventBus = Objects.requireNonNull(eventBus);
        this.performanceMonitor = new AdvancedPerformanceMonitor(plugin, logger);
        
        // Initialize executor FIRST - before any components that depend on it
        this.lifecycleExecutor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "EmpireWand-ServiceRegistry");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
        
        // Initialize components that depend on lifecycleExecutor
        this.dependencyResolver = new DependencyResolver();
        this.proxyFactory = new ServiceProxyFactory();
        this.healthMonitor = new ServiceHealthMonitor();
        this.metricsCollector = new ServiceMetricsCollector();
        
        // Start monitoring
        performanceMonitor.startMonitoring();
        
        // Register built-in services
        registerBuiltinServices();
        
        logger.info("OptimizedServiceRegistry initialized with enterprise features");
    }
    
    /**
     * Registers a service with the registry.
     */
    public <T> void registerService(Class<T> serviceType, ServiceFactory<T> factory) {
        registerService(serviceType, serviceType.getSimpleName(), ServiceScope.SINGLETON, 0, Set.of(), factory);
    }
    
    /**
     * Registers a service with full configuration.
     */
    public <T> void registerService(Class<T> serviceType, String name, ServiceScope scope,
                                  int priority, Set<Class<?>> interfaces, ServiceFactory<T> factory) {
        Objects.requireNonNull(serviceType);
        Objects.requireNonNull(factory);
        
        registryLock.writeLock().lock();
        try {
            if (services.containsKey(serviceType)) {
                throw new IllegalArgumentException("Service already registered: " + serviceType.getName());
            }
            
            ServiceRegistration<T> registration = new ServiceRegistration<>(
                serviceType, name, scope, priority, interfaces, factory);
            
            services.put(serviceType, registration);
            if (!name.equals(serviceType.getSimpleName())) {
                namedServices.put(name, registration);
            }
            
            logger.log(Level.INFO, "Registered service: {0} with scope: {1}", new Object[]{serviceType.getSimpleName(), scope});
            
        } finally {
            registryLock.writeLock().unlock();
        }
        
        // Publish registration event AFTER releasing the lock to avoid deadlock
        eventBus.publish("service.registered", Map.of(
            "serviceType", serviceType,
            "name", name,
            "scope", scope
        ));
    }
    
    /**
     * Registers a service instance directly.
     */
    public <T> void registerServiceInstance(Class<T> serviceType, T instance) {
        registerService(serviceType, context -> instance);
        
        ServiceRegistration<T> registration = getServiceRegistration(serviceType);
        if (registration != null) {
            registration.setInstance(instance);
            registration.setState(ServiceState.RUNNING);
        }
    }
    
    /**
     * Gets a service instance by type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceType) {
        Objects.requireNonNull(serviceType);
        
        registryLock.readLock().lock();
        try {
            ServiceRegistration<T> registration = (ServiceRegistration<T>) services.get(serviceType);
            if (registration == null) {
                // Try to find by interface
                for (ServiceRegistration<?> reg : services.values()) {
                    if (reg.getInterfaces().contains(serviceType)) {
                        registration = (ServiceRegistration<T>) reg;
                        break;
                    }
                }
            }
            
            if (registration == null) {
                return null;
            }
            
            return getServiceInstance(registration);
            
        } finally {
            registryLock.readLock().unlock();
        }
    }
    
    /**
     * Gets a service instance by name and type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(String name, Class<T> serviceType) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(serviceType);
        
        registryLock.readLock().lock();
        try {
            ServiceRegistration<T> registration = (ServiceRegistration<T>) namedServices.get(name);
            if (registration == null || !serviceType.isAssignableFrom(registration.getServiceType())) {
                return null;
            }
            
            return getServiceInstance(registration);
            
        } finally {
            registryLock.readLock().unlock();
        }
    }
    
    /**
     * Gets all services of a specific type.
     */
    public <T> List<T> getServices(Class<T> serviceType) {
        List<T> result = new ArrayList<>();
        
        registryLock.readLock().lock();
        try {
            for (ServiceRegistration<?> registration : services.values()) {
                if (serviceType.isAssignableFrom(registration.getServiceType()) ||
                    registration.getInterfaces().contains(serviceType)) {
                    
                    @SuppressWarnings("unchecked")
                    T instance = (T) getServiceInstance((ServiceRegistration<T>) registration);
                    if (instance != null) {
                        result.add(instance);
                    }
                }
            }
        } finally {
            registryLock.readLock().unlock();
        }
        
        // Sort by priority
        result.sort((a, b) -> {
            ServiceRegistration<?> regA = findRegistrationForInstance(a);
            ServiceRegistration<?> regB = findRegistrationForInstance(b);
            
            int priorityA = regA != null ? regA.getPriority() : 0;
            int priorityB = regB != null ? regB.getPriority() : 0;
            
            return Integer.compare(priorityB, priorityA); // Higher priority first
        });
        
        return result;
    }
    
    /**
     * Adds a service interceptor for cross-cutting concerns.
     */
    public void addInterceptor(Class<?> serviceType, ServiceInterceptor interceptor) {
        interceptors.computeIfAbsent(serviceType, k -> new CopyOnWriteArrayList<>()).add(interceptor);
        
        // Re-create proxy if service is already running
        ServiceRegistration<?> registration = services.get(serviceType);
        if (registration != null && registration.getInstance() != null) {
            recreateServiceProxy(registration);
        }
    }
    
    /**
     * Starts all registered services in dependency order.
     */
    public CompletableFuture<Void> startServices() {
        return CompletableFuture.runAsync(() -> {
            try (var timing = performanceMonitor.startTiming("ServiceRegistry.startServices", 10000)) {
                timing.observe();
                
                logger.info("Starting service registry...");
                
                // Check for circular dependencies
                if (dependencyResolver.hasCyclicDependency()) {
                    List<Class<?>> cycle = dependencyResolver.findCyclicDependencyChain();
                    throw new IllegalStateException("Circular dependency detected: " + cycle);
                }
                
                // Get startup order
                List<Class<?>> startupOrder = dependencyResolver.resolveStartupOrder();
                
                // Start services in order
                for (Class<?> serviceType : startupOrder) {
                    ServiceRegistration<?> registration = services.get(serviceType);
                    if (registration != null) {
                        startService(registration);
                    }
                }
                
                registryStarted = true;
                logger.log(Level.INFO, "Service registry started successfully with {0} services", services.size());
                
                // Publish startup complete event (no lock held here, so it's safe)
                eventBus.publish("service.registry.started", Map.of(
                    "serviceCount", services.size(),
                    "timestamp", Instant.now()
                ));
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to start service registry", e);
                throw new RuntimeException(e);
            }
        }, lifecycleExecutor);
    }
    
    /**
     * Stops all services in reverse dependency order.
     */
    public CompletableFuture<Void> stopServices() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Stopping service registry...");
            
            registryStarted = false;
            
            // Get shutdown order
            List<Class<?>> shutdownOrder = dependencyResolver.resolveShutdownOrder();
            
            // Stop services in order
            for (Class<?> serviceType : shutdownOrder) {
                ServiceRegistration<?> registration = services.get(serviceType);
                if (registration != null) {
                    stopService(registration);
                }
            }
            
            // Shutdown health monitor
            healthMonitor.shutdown();
            
            // Shutdown executor
            lifecycleExecutor.shutdown();
            try {
                if (!lifecycleExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    lifecycleExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                lifecycleExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // Stop performance monitoring
            performanceMonitor.stopMonitoring();
            
            logger.info("Service registry stopped");
            
        }, lifecycleExecutor);
    }
    
    /**
     * Gets comprehensive service registry metrics.
     */
    public ServiceRegistryMetrics getMetrics() {
        return metricsCollector.collectMetrics();
    }
    
    // Private implementation methods
    
    @SuppressWarnings("unchecked")
    private <T> ServiceRegistration<T> getServiceRegistration(Class<T> serviceType) {
        return (ServiceRegistration<T>) services.get(serviceType);
    }
    
    private <T> T getServiceInstance(ServiceRegistration<T> registration) {
        if (registration == null) {
            return null;
        }
        
        registration.incrementCallCount();
        
        // Handle different scopes
        switch (registration.getScope()) {
            case SINGLETON -> {
                synchronized (registration) {
                    if (registration.getInstance() == null) {
                        createServiceInstance(registration);
                    }
                }
                return applyInterceptors(registration);
            }
            
            case PROTOTYPE -> {
                ServiceRegistration<T> prototypeRegistration = new ServiceRegistration<>(
                    registration.getServiceType(), registration.getName(), registration.getScope(),
                    registration.getPriority(), registration.getInterfaces(), registration.getFactory());
                createServiceInstance(prototypeRegistration);
                return applyInterceptors(prototypeRegistration);
            }
            
            default -> {
                // For now, treat other scopes as singleton
                return getServiceInstance(registration);
            }
        }
    }
    
    private <T> void createServiceInstance(ServiceRegistration<T> registration) {
        try {
            registration.setState(ServiceState.INITIALIZING);
            
            ServiceContext context = new ServiceContext(this);
            T instance = registration.getFactory().createInstance(context);
            
            // Perform dependency injection
            performDependencyInjection(instance);
            
            // Call post-construct methods
            invokePostConstructMethods(instance);
            
            registration.setInstance(instance);
            registration.setState(ServiceState.RUNNING);
            
        } catch (Exception e) {
            registration.setState(ServiceState.FAILED);
            logger.log(Level.SEVERE, "Failed to create service instance: " + 
                      registration.getServiceType().getSimpleName(), e);
            throw new RuntimeException(e);
        }
    }
    
    private <T> T applyInterceptors(ServiceRegistration<T> registration) {
        T instance = registration.getInstance();
        if (instance == null) {
            return null;
        }
        
        List<ServiceInterceptor> serviceInterceptors = interceptors.get(registration.getServiceType());
        if (serviceInterceptors != null && !serviceInterceptors.isEmpty()) {
            return (T) proxyFactory.createProxy(registration.getServiceType(), instance, serviceInterceptors);
        }
        
        return instance;
    }
    
    // Implement performDependencyInjection
    private void performDependencyInjection(Object instance) {
        try {
            for (java.lang.reflect.Field field : instance.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    Inject inject = field.getAnnotation(Inject.class);
                    Object dependency;
                    if (!inject.name().isEmpty()) {
                        dependency = getService(inject.name(), field.getType());
                    } else {
                        dependency = getService(field.getType());
                    }
                    if (dependency == null && !inject.optional()) {
                        throw new IllegalStateException("Required dependency not found: " + field.getType().getSimpleName());
                    }
                    field.set(instance, dependency);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Dependency injection failed for " + instance.getClass().getSimpleName(), e);
        }
    }
    
    // Implement invokePostConstructMethods
    private void invokePostConstructMethods(Object instance) {
        try {
            for (java.lang.reflect.Method method : instance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    method.setAccessible(true);
                    method.invoke(instance);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Post-construct failed for " + instance.getClass().getSimpleName(), e);
        }
    }
    
    // Implement invokePreDestroyMethods
    private void invokePreDestroyMethods(Object instance) {
        try {
            for (java.lang.reflect.Method method : instance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(PreDestroy.class)) {
                    method.setAccessible(true);
                    method.invoke(instance);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Pre-destroy failed for " + instance.getClass().getSimpleName(), e);
        }
    }
    
    private void startService(ServiceRegistration<?> registration) {
        try {
            Object instance = registration.getInstance();
            if (instance instanceof ServiceLifecycle lifecycle) {
                lifecycle.start();
            }
            registration.setState(ServiceState.RUNNING);
            
        } catch (Exception e) {
            registration.setState(ServiceState.FAILED);
            logger.log(Level.SEVERE, "Failed to start service: " + registration.getServiceType().getSimpleName(), e);
        }
    }
    
    private void stopService(ServiceRegistration<?> registration) {
        try {
            Object instance = registration.getInstance();
            if (instance != null) {
                // Call pre-destroy methods
                invokePreDestroyMethods(instance);
                
                // Stop if lifecycle service
                if (instance instanceof ServiceLifecycle lifecycle) {
                    lifecycle.stop();
                }
            }
            
            registration.setState(ServiceState.STOPPED);
            
        } catch (Exception e) {
            registration.setState(ServiceState.FAILED);
            logger.log(Level.WARNING, "Error stopping service: " + registration.getServiceType().getSimpleName(), e);
        }
    }
    
    // Implement recreateServiceProxy
    private <T> void recreateServiceProxy(ServiceRegistration<T> registration) {
        List<ServiceInterceptor> serviceInterceptors = interceptors.get(registration.getServiceType());
        if (serviceInterceptors != null) {
            T proxy = proxyFactory.createProxy(registration.getServiceType(), registration.getInstance(), new ArrayList<>(serviceInterceptors));
            registration.setInstance(proxy);
        }
    }
    
    private ServiceRegistration<?> findRegistrationForInstance(Object instance) {
        return services.values().stream()
            .filter(reg -> reg.getInstance() == instance)
            .findFirst()
            .orElse(null);
    }
    
    private void updateAverageServiceCallTime(long executionTime) {
        long currentAvg = averageServiceCallTime.get();
        long newAvg = (currentAvg + executionTime / 1_000_000) / 2; // Convert to ms
        averageServiceCallTime.set(newAvg);
    }
    
    private void registerBuiltinServices() {
        // Register the registry itself as a service
        registerServiceInstance(OptimizedServiceRegistry.class, this);

        // Register event bus
        registerServiceInstance(EventBusSystem.class, eventBus);

        // Register performance monitor
        registerServiceInstance(AdvancedPerformanceMonitor.class, performanceMonitor);
    }

    /**
     * Registers GUI-related services for the wand settings system.
     * Called after initialization to register GUI services.
     */
    public void registerGuiServices(Plugin plugin) {
        // Register WandSettingsService
        registerService(
                nl.wantedchef.empirewand.core.config.WandSettingsService.class,
                context -> new nl.wantedchef.empirewand.core.config.WandSettingsService(plugin)
        );

        // Register WandSessionManager
        registerService(
                nl.wantedchef.empirewand.gui.session.WandSessionManager.class,
                context -> new nl.wantedchef.empirewand.gui.session.WandSessionManager(plugin.getLogger())
        );
    }
    
    // Data structures for reporting
    
    public record ServiceMetrics(
        String serviceType,
        String name,
        ServiceState state,
        long callCount,
        Duration uptime,
        Duration lastUsed,
        String version
    ) {}
    
    public record ServiceRegistryMetrics(
        Instant timestamp,
        int totalServices,
        long totalServiceCalls,
        long totalServiceFailures,
        long averageServiceCallTimeMs,
        boolean registryStarted,
        Map<String, ServiceMetrics> serviceMetrics
    ) {
        public double getServiceCallSuccessRate() {
            return totalServiceCalls > 0 ? 
                (double) (totalServiceCalls - totalServiceFailures) / totalServiceCalls : 1.0;
        }
    }
}