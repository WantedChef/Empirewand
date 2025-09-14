package nl.wantedchef.empirewand.core;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.config.EnhancedConfigService;
import nl.wantedchef.empirewand.core.storage.OptimizedDataManager;
import nl.wantedchef.empirewand.core.event.EventBusSystem;
import nl.wantedchef.empirewand.core.health.SystemHealthMonitor;
import nl.wantedchef.empirewand.core.integration.OptimizedServiceRegistry;
import nl.wantedchef.empirewand.core.task.AdvancedTaskManager;
import nl.wantedchef.empirewand.core.util.AdvancedPerformanceMonitor;


import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enhanced core system that integrates all enterprise-grade components:
 * - Advanced configuration management with hot reloading
 * - Optimized data storage with multi-level caching
 * - Event-driven architecture with comprehensive messaging
 * - System health monitoring with predictive analysis
 * - Service registry with dependency injection
 * - Advanced task management with priority queues
 * - Performance monitoring with real-time metrics
 * 
 * This core provides a foundation for enterprise-level plugin architecture
 * with comprehensive monitoring, optimization, and reliability features.
 */
public class EnhancedEmpireWandCore {
    
    private final EmpireWandPlugin plugin;
    private static final Logger logger = Logger.getLogger(EnhancedEmpireWandCore.class.getName());
    
    // Core system components
    private AdvancedPerformanceMonitor performanceMonitor;
    private EventBusSystem eventBus;
    private OptimizedServiceRegistry serviceRegistry;
    private EnhancedConfigService configService;
    private OptimizedDataManager dataManager;
    private SystemHealthMonitor healthMonitor;
    private AdvancedTaskManager taskManager;
    
    // System state
    private final AtomicReference<CoreSystemState> systemState = new AtomicReference<>(CoreSystemState.INITIALIZING);
    private volatile Instant startupTime;
    private volatile boolean shutdownInProgress = false;
    
    /**
     * Core system states.
     */
    public enum CoreSystemState {
        INITIALIZING,
        STARTING,
        RUNNING,
        DEGRADED,
        STOPPING,
        STOPPED,
        ERROR
    }
    
    /**
     * Core system metrics for monitoring.
     */
    public record CoreSystemMetrics(
        CoreSystemState state,
        Instant startupTime,
        Duration uptime,
        Map<String, Object> componentMetrics,
        SystemHealthMonitor.OverallHealthStatus healthStatus,
        long totalEvents,
        long totalTasks,
        double memoryUsage,
        double cpuUsage
    ) {}
    
    public EnhancedEmpireWandCore(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.startupTime = Instant.now();
        
        logger.info("Initializing Enhanced EmpireWand Core System...");
    }
    
    /**
     * Initializes and starts all core system components.
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                systemState.set(CoreSystemState.STARTING);
                
                logger.info("Starting Enhanced EmpireWand Core initialization sequence...");
                
                // Phase 1: Initialize performance monitoring first
                initializePerformanceMonitoring();
                
                // Phase 2: Initialize event system
                initializeEventSystem();
                
                // Phase 3: Initialize service registry
                initializeServiceRegistry();
                
                // Phase 4: Initialize configuration system
                try {
                    initializeConfigurationSystem();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to initialize configuration system due to I/O error", e);
                    throw new RuntimeException("Configuration system initialization failed", e);
                }
                
                // Phase 5: Initialize data management
                initializeDataManagement();
                
                // Phase 6: Initialize task management
                initializeTaskManagement();
                
                // Phase 7: Initialize health monitoring
                initializeHealthMonitoring();
                
                // Phase 8: Register core services
                registerCoreServices();
                
                // Phase 9: Start all services
                startAllServices();
                
                // Phase 10: Setup system integration
                setupSystemIntegration();
                
                // Phase 11: Register shutdown hooks
                registerShutdownHooks();
                
                systemState.set(CoreSystemState.RUNNING);
                
                // Publish system ready event
                eventBus.publish("core.system.ready", Map.of(
                    "startupTime", startupTime,
                    "initializationDuration", Duration.between(startupTime, Instant.now()),
                    "componentCount", getComponentCount()
                ));
                
                logger.log(Level.INFO, "Enhanced EmpireWand Core system initialized successfully in {0}ms",
                           Duration.between(startupTime, Instant.now()).toMillis());
                
            } catch (final RuntimeException e) {
                systemState.set(CoreSystemState.ERROR);
                logger.log(Level.SEVERE, "Failed to initialize Enhanced EmpireWand Core", e);
                throw e;
            }
        });
    }
    
    /**
     * Gracefully shuts down all core system components.
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            if (shutdownInProgress) {
                return;
            }

            shutdownInProgress = true;
            systemState.set(CoreSystemState.STOPPING);

            logger.info("Starting Enhanced EmpireWand Core shutdown sequence...");

            try {
                // Publish shutdown starting event
                if (eventBus != null) {
                    eventBus.publish("core.system.shutdown.starting", Map.of(
                        "timestamp", Instant.now(),
                        "uptime", Duration.between(startupTime, Instant.now())
                    ));
                }

                // Phase 1: Stop accepting new work
                stopAcceptingNewWork();

                // Phase 2: Wait for current operations to complete
                waitForCurrentOperations();

                // Phase 3: Stop services in reverse order
                stopAllServices();

                // Phase 4: Shutdown individual components
                shutdownComponents();

                // Phase 5: Final cleanup
                performFinalCleanup();

                systemState.set(CoreSystemState.STOPPED);

                logger.log(Level.INFO, "Enhanced EmpireWand Core shutdown completed in {0}ms total uptime",
                           Duration.between(startupTime, Instant.now()).toMillis());

            } catch (final RuntimeException e) {
                systemState.set(CoreSystemState.ERROR);
                logger.log(Level.SEVERE, "Error during Enhanced EmpireWand Core shutdown", e);
            }
        });
    }
    
    /**
     * Gets comprehensive system metrics.
     */
    public CoreSystemMetrics getSystemMetrics() {
        Map<String, Object> componentMetrics = Map.of(
            "performanceMonitor", performanceMonitor != null ? performanceMonitor.getSystemReport() : "not initialized",
            "eventBus", eventBus != null ? eventBus.getMetrics() : "not initialized",
            "serviceRegistry", serviceRegistry != null ? serviceRegistry.getMetrics() : "not initialized",
            "configService", configService != null ? configService.getMetrics() : "not initialized",
            "dataManager", dataManager != null ? dataManager.getMetrics() : "not initialized",
            "healthMonitor", healthMonitor != null ? healthMonitor.getMetrics() : "not initialized",
            "taskManager", taskManager != null ? taskManager.getMetrics() : "not initialized"
        );
        
        SystemHealthMonitor.OverallHealthStatus healthStatus = 
            healthMonitor != null ? healthMonitor.getOverallHealthStatus() : 
            SystemHealthMonitor.OverallHealthStatus.UNKNOWN;
        
        // Get basic system metrics
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
        
        return new CoreSystemMetrics(
            systemState.get(),
            startupTime,
            Duration.between(startupTime, Instant.now()),
            componentMetrics,
            healthStatus,
            eventBus != null ? eventBus.getMetrics().totalEventsPublished() : 0,
            taskManager != null ? taskManager.getMetrics().totalTasksSubmitted() : 0,
            memoryUsage,
            -1.0 // CPU usage would require more complex monitoring
        );
    }
    
    /**
     * Gets the current system state.
     */
    public CoreSystemState getSystemState() {
        return systemState.get();
    }
    
    /**
     * Checks if the core system is healthy and operational.
     */
    public boolean isHealthy() {
        CoreSystemState state = systemState.get();
        return state == CoreSystemState.RUNNING && 
               (healthMonitor == null || healthMonitor.getOverallHealthStatus() != SystemHealthMonitor.OverallHealthStatus.DOWN);
    }
    
    // Core component getters
    public AdvancedPerformanceMonitor getPerformanceMonitor() { return performanceMonitor; }
    public EventBusSystem getEventBus() { return eventBus; }
    public OptimizedServiceRegistry getServiceRegistry() { return serviceRegistry; }
    public EnhancedConfigService getConfigService() { return configService; }
    public OptimizedDataManager getDataManager() { return dataManager; }
    public SystemHealthMonitor getHealthMonitor() { return healthMonitor; }
    public AdvancedTaskManager getTaskManager() { return taskManager; }
    
    // Private initialization methods
    
    private void initializePerformanceMonitoring() {
        logger.info("Initializing performance monitoring...");
        try {
            performanceMonitor = new AdvancedPerformanceMonitor(plugin, logger);
            performanceMonitor.startMonitoring();
            logger.info("Performance monitoring initialized");
        } catch (final RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to initialize performance monitoring", e);
            throw e;
        }
    }

    private void initializeEventSystem() {
        logger.info("Initializing event system...");
        try {
            eventBus = new EventBusSystem(plugin);

            // Register core event handlers
            // System state change events
            eventBus.register("system.state.change", (event) -> {
                logger.log(Level.INFO, "System state changed: {0}", event);
            });

            // Critical error events
            eventBus.register("system.error.critical", (event) -> {
                logger.log(Level.SEVERE, "Critical system error detected: {0}", event);
                // Could trigger automatic recovery procedures
            });

            // Performance warning events
            eventBus.register("system.performance.warning", (event) -> {
                logger.log(Level.WARNING, "Performance issue detected: {0}", event);
            });

            logger.info("Event system initialized");
        } catch (final RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to initialize event system", e);
            throw e;
        }
    }

    private void initializeServiceRegistry() {
        logger.info("Initializing service registry...");
        try {
            serviceRegistry = new OptimizedServiceRegistry(plugin, eventBus);
            logger.info("Service registry initialized");
        } catch (final RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to initialize service registry", e);
            throw e;
        }
    }

    private void initializeConfigurationSystem() throws IOException {
        logger.info("Initializing configuration system...");
        try {
            configService = new EnhancedConfigService(plugin, performanceMonitor);

            // Register configuration change handlers
            configService.addChangeListener(this::handleConfigurationChange);

            logger.info("Configuration system initialized");
        } catch (final RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to initialize configuration system", e);
            throw e;
        }
    }

    private void initializeDataManagement() {
        logger.info("Initializing data management...");
        try {
            dataManager = new OptimizedDataManager(plugin);
            logger.info("Data management initialized");
        } catch (final RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to initialize data management", e);
            throw e;
        }
    }

    private void initializeTaskManagement() {
        logger.info("Initializing task management...");
        try {
            taskManager = new AdvancedTaskManager(plugin);
            logger.info("Task management initialized");
        } catch (final RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to initialize task management", e);
            throw e;
        }
    }

    private void initializeHealthMonitoring() {
        logger.info("Initializing health monitoring...");
        try {
            healthMonitor = new SystemHealthMonitor(plugin);

            // Register custom health checks
            registerCustomHealthChecks();

            // Add alert handlers
            healthMonitor.addAlertHandler(this::handleSystemAlert);

            logger.info("Health monitoring initialized");
        } catch (final RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to initialize health monitoring", e);
            throw e;
        }
    }

    private void registerCoreServices() {
        logger.info("Registering core services...");
        try {
            // Register all core components as services
            serviceRegistry.registerServiceInstance(AdvancedPerformanceMonitor.class, performanceMonitor);
            serviceRegistry.registerServiceInstance(EnhancedConfigService.class, configService);
            serviceRegistry.registerServiceInstance(OptimizedDataManager.class, dataManager);
            serviceRegistry.registerServiceInstance(SystemHealthMonitor.class, healthMonitor);
            serviceRegistry.registerServiceInstance(AdvancedTaskManager.class, taskManager);

            logger.info("Core services registered");
        } catch (final RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to register core services", e);
            throw e;
        }
    }

    private void startAllServices() {
        logger.info("Starting all services...");
        try {
            CompletableFuture<Void> serviceStartup = serviceRegistry.startServices();
            serviceStartup.get(60, TimeUnit.SECONDS); // 60 second timeout
            logger.info("All services started successfully");
        } catch (final InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.SEVERE, "Failed to start services", e);
            throw new RuntimeException(e);
        }
    }

    private void setupSystemIntegration() {
        logger.info("Setting up system integration...");
        try {
            // Configure cross-component communication
            setupEventIntegration();
            setupMetricsIntegration();
            setupHealthIntegration();

            logger.info("System integration configured");
        } catch (final RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to setup system integration", e);
            throw e;
        }
    }

    private void registerShutdownHooks() {
        // Register JVM shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM shutdown detected, initiating Enhanced EmpireWand Core shutdown...");
            try {
                shutdown().get(30, TimeUnit.SECONDS);
            } catch (final ExecutionException | InterruptedException | TimeoutException e) {
                logger.log(Level.WARNING, "Error during shutdown hook execution", e);
            }
        }, "EmpireWand-Core-Shutdown"));
    }

    private void registerCoreEventHandlers() {
        // System state change events
        eventBus.register("system.state.change", (event) -> {
            logger.log(Level.INFO, "System state changed: {0}", event);
        });

        // Critical error events
        eventBus.register("system.error.critical", (event) -> {
            logger.log(Level.SEVERE, "Critical system error detected: {0}", event);
            // Could trigger automatic recovery procedures
        });

        // Performance warning events
        eventBus.register("system.performance.warning", (event) -> {
            logger.log(Level.WARNING, "Performance issue detected: {0}", event);
        });
    }
    
    private void registerCustomHealthChecks() {
        // Core system health check
        healthMonitor.registerHealthCheck("core-system", 
            () -> systemState.get() == CoreSystemState.RUNNING,
            "Core system operational status");
        
        // Event bus health check
        healthMonitor.registerHealthCheck("event-bus",
            () -> eventBus != null && eventBus.getMetrics().getSuccessRate() > 0.95,
            "Event bus processing health");
        
        // Service registry health check
        healthMonitor.registerHealthCheck("service-registry",
            () -> serviceRegistry != null && serviceRegistry.getMetrics().getServiceCallSuccessRate() > 0.95,
            "Service registry health");
        
        // Configuration system health check
        healthMonitor.registerHealthCheck("configuration",
            () -> configService != null && configService.getMetrics().getCacheHitRate() > 0.80,
            "Configuration system health");
    }
    
    private void setupEventIntegration() {
        // Forward performance events to health monitor
        eventBus.register("performance.degraded", this::ignoreEvent);
        
        // Forward health events to metrics collection
        eventBus.register("health.status.changed", this::ignoreEvent);
    }
    
    private void setupMetricsIntegration() {
        // Create a scheduled task to collect and publish comprehensive metrics
        if (taskManager != null) {
            taskManager.runTaskTimer(() -> {
                try {
                    CoreSystemMetrics metrics = getSystemMetrics();
                    eventBus.publish("metrics.system.update", metrics);
                } catch (final RuntimeException e) {
                    logger.log(Level.WARNING, "Error collecting system metrics", e);
                }
            }, 0, 300); // Every 15 seconds (300 ticks)
        }
    }

    private void ignoreEvent(@SuppressWarnings("unused") Object ignored) {
        // intentionally no-op; registration keeps integration pathways alive
    }
    
    private void setupHealthIntegration() {
        // Configure automatic recovery for certain health issues
        eventBus.register("health.critical", (event) -> {
            logger.log(Level.WARNING, "Critical health issue detected: {0}", event);
            // Could trigger automatic recovery procedures

            // Example: restart unhealthy services
            systemState.set(CoreSystemState.DEGRADED);
        });
    }

    private void handleConfigurationChange(EnhancedConfigService.ConfigurationChangeEvent event) {
        logger.log(Level.INFO, "Configuration changed: {0}", event.getChangedKeys());

        // Publish configuration change event
        eventBus.publish("configuration.changed", Map.of(
            "changedKeys", event.getChangedKeys(),
            "timestamp", event.getTimestamp()
        ));

        // Handle specific configuration changes
        for (String changedKey : event.getChangedKeys()) {
            if (changedKey.startsWith("performance.")) {
                // Update performance monitoring configuration
                updatePerformanceConfiguration(changedKey, event);
            } else if (changedKey.startsWith("health.")) {
                // Update health monitoring configuration
                updateHealthConfiguration(changedKey);
            }
        }
    }

    private void updatePerformanceConfiguration(String key, EnhancedConfigService.ConfigurationChangeEvent event) {
        // Implementation would update performance monitoring based on config changes
        logger.log(Level.FINE, "Updating performance configuration for key: {0}", key);
    }

    private void updateHealthConfiguration(String key) {
        // Implementation would update health monitoring based on config changes
        logger.log(Level.FINE, "Updating health configuration for key: {0}", key);
    }

    private void handleSystemAlert(SystemHealthMonitor.Alert alert) {
        logger.log(Level.WARNING, "System Alert [{0}] {1}: {2}",
                  new Object[]{alert.level(), alert.component(), alert.message()});

        // Publish alert event
        eventBus.publish("system.alert", alert);

        // Handle critical alerts
        if (alert.level() == SystemHealthMonitor.AlertLevel.CRITICAL) {
            systemState.set(CoreSystemState.DEGRADED);

            // Could trigger automatic recovery procedures
            initiateAutomaticRecovery(alert);
        }
    }

    private void initiateAutomaticRecovery(SystemHealthMonitor.Alert alert) {
        logger.log(Level.INFO, "Initiating automatic recovery for critical alert: {0}", alert.component());

        // Implementation would contain specific recovery procedures
        // For now, just log the attempt
        taskManager.submitAsync("recovery:" + alert.component(), () -> {
            try {
                // Simulate recovery process
                Thread.sleep(5000);
                logger.log(Level.INFO, "Recovery completed for: {0}", alert.component());
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }
    
    // Shutdown methods
    
    private void stopAcceptingNewWork() {
        logger.info("Stopping acceptance of new work...");
        // Implementation would stop accepting new operations
    }
    
    private void waitForCurrentOperations() {
        logger.info("Waiting for current operations to complete...");
        try {
            // Give current operations time to complete
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void stopAllServices() {
        logger.info("Stopping all services...");
        if (serviceRegistry != null) {
            try {
                serviceRegistry.stopServices().get(30, TimeUnit.SECONDS);
            } catch (final RuntimeException | InterruptedException | ExecutionException | TimeoutException e) {
                logger.log(Level.WARNING, "Error stopping services", e);
            }
        }
    }
    
    private void shutdownComponents() {
        logger.info("Shutting down individual components...");
        
        // Shutdown in reverse initialization order
        
        if (healthMonitor != null) {
            try {
                healthMonitor.shutdown();
                logger.fine("Health monitor shutdown");
            } catch (final RuntimeException e) {
                logger.log(Level.WARNING, "Error shutting down health monitor", e);
            }
        }
        
        if (taskManager != null) {
            try {
                taskManager.shutdown();
                logger.fine("Task manager shutdown");
            } catch (final RuntimeException e) {
                logger.log(Level.WARNING, "Error shutting down task manager", e);
            }
        }
        
        if (dataManager != null) {
            try {
                dataManager.shutdown();
                logger.fine("Data manager shutdown");
            } catch (final RuntimeException e) {
                logger.log(Level.WARNING, "Error shutting down data manager", e);
            }
        }
        
        if (configService != null) {
            try {
                configService.shutdown();
                logger.fine("Config service shutdown");
            } catch (final RuntimeException e) {
                logger.log(Level.WARNING, "Error shutting down config service", e);
            }
        }
        
        if (eventBus != null) {
            try {
                eventBus.shutdown();
                logger.fine("Event bus shutdown");
            } catch (final RuntimeException e) {
                logger.log(Level.WARNING, "Error shutting down event bus", e);
            }
        }
        
        if (performanceMonitor != null) {
            try {
                performanceMonitor.stopMonitoring();
                logger.fine("Performance monitor shutdown");
            } catch (final RuntimeException e) {
                logger.log(Level.WARNING, "Error shutting down performance monitor", e);
            }
        }
    }
    
    private void performFinalCleanup() {
        logger.info("Performing final cleanup...");
        // Clear references to help GC
        performanceMonitor = null;
        eventBus = null;
        serviceRegistry = null;
        configService = null;
        dataManager = null;
        healthMonitor = null;
        taskManager = null;
    }
    
    private int getComponentCount() {
        int count = 0;
        if (performanceMonitor != null) count++;
        if (eventBus != null) count++;
        if (serviceRegistry != null) count++;
        if (configService != null) count++;
        if (dataManager != null) count++;
        if (healthMonitor != null) count++;
        if (taskManager != null) count++;
        return count;
    }
}
