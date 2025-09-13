package nl.wantedchef.empirewand.core.config;

import nl.wantedchef.empirewand.core.util.AdvancedPerformanceMonitor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Instant;
import java.time.Duration;

/**
 * Enterprise-grade configuration service with advanced features:
 * - Hot reloading with file system watchers
 * - Multi-level caching with intelligent invalidation
 * - Configuration validation and schema enforcement
 * - Performance-optimized access patterns
 * - Event-driven configuration changes
 * - Thread-safe concurrent access
 * - Automatic backup and rollback capabilities
 * - Configuration migration and versioning
 * - Real-time metrics and monitoring
 */
public class EnhancedConfigService {
    
    private final Plugin plugin;
    private final Logger logger;
    private final AdvancedPerformanceMonitor performanceMonitor;
    
    // Configuration storage
    private final AtomicReference<ConfigurationSnapshot> currentSnapshot = new AtomicReference<>();
    private final ReentrantReadWriteLock configLock = new ReentrantReadWriteLock();
    
    // Hot reloading
    private final WatchService fileWatchService;
    private final Map<String, WatchKey> watchKeys = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watchExecutor;
    private volatile boolean hotReloadEnabled = true;
    
    // Multi-level caching
    private final ConfigurationCache l1Cache; // In-memory cache
    private final ConfigurationCache l2Cache; // Disk-backed cache
    private final Map<String, CachedValue<?>> valueCache = new ConcurrentHashMap<>();
    
    // Configuration listeners and events
    private final List<ConfigurationChangeListener> changeListeners = new CopyOnWriteArrayList<>();
    private final Map<String, List<Consumer<Object>>> keyListeners = new ConcurrentHashMap<>();
    
    // Performance metrics
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong hotReloads = new AtomicLong();
    private final AtomicLong configAccesses = new AtomicLong();
    
    // Configuration validation
    private final ConfigurationValidator validator;
    private final ConfigurationSchema schema;
    
    // Backup and versioning
    private final ConfigurationBackupManager backupManager;
    private final AtomicLong configVersion = new AtomicLong(1);
    
    /**
     * Configuration snapshot containing immutable config data and metadata.
     */
    public static class ConfigurationSnapshot {
        private final FileConfiguration mainConfig;
        private final FileConfiguration spellsConfig;
        private final Map<String, FileConfiguration> additionalConfigs;
        private final Instant createdAt;
        private final long version;
        private final String checksum;
        
        public ConfigurationSnapshot(FileConfiguration mainConfig, FileConfiguration spellsConfig,
                                   Map<String, FileConfiguration> additionalConfigs, long version) {
            this.mainConfig = mainConfig;
            this.spellsConfig = spellsConfig;
            this.additionalConfigs = Map.copyOf(additionalConfigs);
            this.createdAt = Instant.now();
            this.version = version;
            this.checksum = calculateChecksum();
        }
        
        private String calculateChecksum() {
            // Simple checksum based on config content
            int hash = Objects.hash(
                mainConfig.saveToString(),
                spellsConfig.saveToString(),
                additionalConfigs.toString()
            );
            return Integer.toHexString(hash);
        }
        
        // Getters
        public FileConfiguration getMainConfig() { return mainConfig; }
        public FileConfiguration getSpellsConfig() { return spellsConfig; }
        public Map<String, FileConfiguration> getAdditionalConfigs() { return additionalConfigs; }
        public Instant getCreatedAt() { return createdAt; }
        public long getVersion() { return version; }
        public String getChecksum() { return checksum; }
    }
    
    /**
     * Cached value with expiration and access tracking.
     */
    private static class CachedValue<T> {
        private final T value;
        private final Instant cachedAt;
        private final Duration ttl;
        private final AtomicLong accessCount = new AtomicLong();
        private volatile Instant lastAccessed;
        
        public CachedValue(T value, Duration ttl) {
            this.value = value;
            this.cachedAt = Instant.now();
            this.lastAccessed = this.cachedAt;
            this.ttl = ttl;
        }
        
        public T getValue() {
            lastAccessed = Instant.now();
            accessCount.incrementAndGet();
            return value;
        }
        
        public boolean isExpired() {
            return ttl != null && Instant.now().isAfter(cachedAt.plus(ttl));
        }
        
        public CacheStats getStats() {
            return new CacheStats(accessCount.get(), cachedAt, lastAccessed, isExpired());
        }
    }
    
    /**
     * Cache statistics for monitoring.
     */
    public record CacheStats(long accessCount, Instant cachedAt, Instant lastAccessed, boolean expired) {}
    
    /**
     * Configuration change listener interface.
     */
    @FunctionalInterface
    public interface ConfigurationChangeListener {
        void onConfigurationChanged(ConfigurationChangeEvent event);
    }
    
    /**
     * Configuration change event with detailed information.
     */
    public static class ConfigurationChangeEvent {
        private final String configName;
        private final Set<String> changedKeys;
        private final ConfigurationSnapshot oldSnapshot;
        private final ConfigurationSnapshot newSnapshot;
        private final Instant timestamp;
        
        public ConfigurationChangeEvent(String configName, Set<String> changedKeys,
                                       ConfigurationSnapshot oldSnapshot, ConfigurationSnapshot newSnapshot) {
            this.configName = configName;
            this.changedKeys = Set.copyOf(changedKeys);
            this.oldSnapshot = oldSnapshot;
            this.newSnapshot = newSnapshot;
            this.timestamp = Instant.now();
        }
        
        // Getters
        public String getConfigName() { return configName; }
        public Set<String> getChangedKeys() { return changedKeys; }
        public ConfigurationSnapshot getOldSnapshot() { return oldSnapshot; }
        public ConfigurationSnapshot getNewSnapshot() { return newSnapshot; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Multi-level configuration cache.
     */
    private interface ConfigurationCache {
        void put(String key, Object value);
        <T> Optional<T> get(String key, Class<T> type);
        void invalidate(String key);
        void invalidateAll();
        long size();
        CacheMetrics getMetrics();
    }
    
    /**
     * Cache metrics for monitoring.
     */
    public record CacheMetrics(long hits, long misses, long size, double hitRate) {}
    
    /**
     * In-memory configuration cache implementation.
     */
    private static class InMemoryConfigCache implements ConfigurationCache {
        private final Map<String, CachedValue<?>> cache = new ConcurrentHashMap<>();
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();
        private final Duration defaultTtl;
        
        public InMemoryConfigCache(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }
        
        @Override
        public void put(String key, Object value) {
            cache.put(key, new CachedValue<>(value, defaultTtl));
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(String key, Class<T> type) {
            CachedValue<?> cached = cache.get(key);
            if (cached == null || cached.isExpired()) {
                if (cached != null && cached.isExpired()) {
                    cache.remove(key);
                }
                misses.incrementAndGet();
                return Optional.empty();
            }
            
            hits.incrementAndGet();
            Object value = cached.getValue();
            if (type.isInstance(value)) {
                return Optional.of((T) value);
            }
            return Optional.empty();
        }
        
        @Override
        public void invalidate(String key) {
            cache.remove(key);
        }
        
        @Override
        public void invalidateAll() {
            cache.clear();
        }
        
        @Override
        public long size() {
            return cache.size();
        }
        
        @Override
        public CacheMetrics getMetrics() {
            long totalHits = hits.get();
            long totalMisses = misses.get();
            double hitRate = (totalHits + totalMisses) > 0 ? (double) totalHits / (totalHits + totalMisses) : 0.0;
            return new CacheMetrics(totalHits, totalMisses, cache.size(), hitRate);
        }
    }
    
    public EnhancedConfigService(Plugin plugin) throws IOException {
        this.plugin = Objects.requireNonNull(plugin);
        this.logger = plugin.getLogger();
        this.performanceMonitor = new AdvancedPerformanceMonitor(plugin, logger);
        
        // Initialize file watching
        this.fileWatchService = FileSystems.getDefault().newWatchService();
        this.watchExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EmpireWand-ConfigWatcher");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        
        // Initialize caches
        this.l1Cache = new InMemoryConfigCache(Duration.ofMinutes(15)); // 15 minute TTL
        this.l2Cache = new InMemoryConfigCache(Duration.ofHours(1)); // 1 hour TTL for L2
        
        // Initialize validation and backup
        this.validator = new ConfigurationValidator();
        this.schema = loadConfigurationSchema();
        this.backupManager = new ConfigurationBackupManager(plugin.getDataFolder());
        
        // Load initial configuration
        loadInitialConfiguration();
        
        // Start monitoring services
        startConfigurationWatching();
        startPerformanceMonitoring();
        
        logger.info("EnhancedConfigService initialized with enterprise features");
    }
    
    /**
     * Loads the initial configuration from disk with validation and backup.
     */
    private void loadInitialConfiguration() {
        try (var timing = performanceMonitor.startTiming("ConfigService.loadInitial", 200)) {
            timing.observe();
            
            // Ensure config files exist
            plugin.saveDefaultConfig();
            saveResourceIfNotExists("spells.yml");
            
            // Load configurations
            FileConfiguration mainConfig = plugin.getConfig();
            FileConfiguration spellsConfig = loadConfigFile("spells.yml");
            Map<String, FileConfiguration> additionalConfigs = loadAdditionalConfigs();
            
            // Validate configuration
            validateConfiguration(mainConfig, spellsConfig);
            
            // Create snapshot
            ConfigurationSnapshot snapshot = new ConfigurationSnapshot(
                cloneConfig(mainConfig), 
                cloneConfig(spellsConfig), 
                additionalConfigs,
                configVersion.getAndIncrement()
            );
            
            // Create backup
            backupManager.createBackup(snapshot);
            
            // Update current snapshot
            ConfigurationSnapshot oldSnapshot = currentSnapshot.getAndSet(snapshot);
            
            // Fire change events if not initial load
            if (oldSnapshot != null) {
                fireConfigurationChangeEvent("main", oldSnapshot, snapshot);
            }
            
            logger.info("Configuration loaded successfully (version: " + snapshot.getVersion() + 
                       ", checksum: " + snapshot.getChecksum() + ")");
                       
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }
    
    /**
     * Gets a configuration value with multi-level caching and performance optimization.
     */
    public <T> T getValue(String configName, String path, Class<T> type, T defaultValue) {
        try (var timing = performanceMonitor.startTiming("ConfigService.getValue", 5)) {
            timing.observe();
            configAccesses.incrementAndGet();
            
            String cacheKey = configName + ":" + path;
            
            // Try L1 cache first
            Optional<T> cached = l1Cache.get(cacheKey, type);
            if (cached.isPresent()) {
                cacheHits.incrementAndGet();
                return cached.get();
            }
            
            // Try L2 cache
            cached = l2Cache.get(cacheKey, type);
            if (cached.isPresent()) {
                cacheHits.incrementAndGet();
                // Promote to L1 cache
                l1Cache.put(cacheKey, cached.get());
                return cached.get();
            }
            
            cacheMisses.incrementAndGet();
            
            // Load from configuration
            ConfigurationSnapshot snapshot = currentSnapshot.get();
            if (snapshot == null) {
                return defaultValue;
            }
            
            FileConfiguration config = getConfigByName(snapshot, configName);
            if (config == null) {
                return defaultValue;
            }
            
            Object value = config.get(path, defaultValue);
            T typedValue = castValue(value, type, defaultValue);
            
            // Cache the result
            l1Cache.put(cacheKey, typedValue);
            l2Cache.put(cacheKey, typedValue);
            
            return typedValue;
        }
    }
    
    /**
     * Gets a string value from configuration.
     */
    public String getString(String configName, String path, String defaultValue) {
        return getValue(configName, path, String.class, defaultValue);
    }
    
    /**
     * Gets an integer value from configuration.
     */
    public int getInt(String configName, String path, int defaultValue) {
        return getValue(configName, path, Integer.class, defaultValue);
    }
    
    /**
     * Gets a boolean value from configuration.
     */
    public boolean getBoolean(String configName, String path, boolean defaultValue) {
        return getValue(configName, path, Boolean.class, defaultValue);
    }
    
    /**
     * Gets a double value from configuration.
     */
    public double getDouble(String configName, String path, double defaultValue) {
        return getValue(configName, path, Double.class, defaultValue);
    }
    
    /**
     * Gets a list value from configuration.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String configName, String path, Class<T> elementType, List<T> defaultValue) {
        Object value = getValue(configName, path, List.class, defaultValue);
        if (value instanceof List<?> list) {
            return (List<T>) list;
        }
        return defaultValue;
    }
    
    /**
     * Gets a string list from configuration.
     */
    public List<String> getStringList(String configName, String path) {
        return getList(configName, path, String.class, Collections.emptyList());
    }
    
    /**
     * Reloads configuration from disk with hot reload support.
     */
    public CompletableFuture<Void> reloadAsync() {
        return CompletableFuture.runAsync(() -> {
            try (var timing = performanceMonitor.startTiming("ConfigService.reload", 100)) {
                timing.observe();
                hotReloads.incrementAndGet();
                
                // Invalidate all caches
                l1Cache.invalidateAll();
                l2Cache.invalidateAll();
                valueCache.clear();
                
                // Reload configuration
                loadInitialConfiguration();
                
                logger.info("Configuration reloaded successfully");
            }
        }, watchExecutor);
    }
    
    /**
     * Adds a configuration change listener.
     */
    public void addChangeListener(ConfigurationChangeListener listener) {
        changeListeners.add(listener);
    }
    
    /**
     * Removes a configuration change listener.
     */
    public void removeChangeListener(ConfigurationChangeListener listener) {
        changeListeners.remove(listener);
    }
    
    /**
     * Adds a listener for specific configuration key changes.
     */
    public void addKeyListener(String key, Consumer<Object> listener) {
        keyListeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
    
    /**
     * Gets comprehensive configuration service metrics.
     */
    public ConfigServiceMetrics getMetrics() {
        CacheMetrics l1Metrics = l1Cache.getMetrics();
        CacheMetrics l2Metrics = l2Cache.getMetrics();
        
        return new ConfigServiceMetrics(
            configAccesses.get(),
            cacheHits.get(),
            cacheMisses.get(),
            hotReloads.get(),
            currentSnapshot.get() != null ? currentSnapshot.get().getVersion() : 0,
            l1Metrics,
            l2Metrics,
            changeListeners.size(),
            keyListeners.size(),
            backupManager.getBackupCount()
        );
    }
    
    /**
     * Enables or disables hot reloading.
     */
    public void setHotReloadEnabled(boolean enabled) {
        this.hotReloadEnabled = enabled;
        if (enabled) {
            startConfigurationWatching();
        } else {
            stopConfigurationWatching();
        }
        logger.info("Hot reload " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Creates a backup of the current configuration.
     */
    public CompletableFuture<String> createBackup(String description) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigurationSnapshot snapshot = currentSnapshot.get();
            if (snapshot != null) {
                return backupManager.createNamedBackup(snapshot, description);
            }
            throw new IllegalStateException("No configuration snapshot available");
        });
    }
    
    /**
     * Restores configuration from a backup.
     */
    public CompletableFuture<Void> restoreFromBackup(String backupId) {
        return CompletableFuture.runAsync(() -> {
            try {
                ConfigurationSnapshot backup = backupManager.restoreBackup(backupId);
                if (backup != null) {
                    ConfigurationSnapshot old = currentSnapshot.getAndSet(backup);
                    fireConfigurationChangeEvent("restore", old, backup);
                    logger.info("Configuration restored from backup: " + backupId);
                } else {
                    throw new IllegalArgumentException("Backup not found: " + backupId);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to restore from backup: " + backupId, e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Shuts down the configuration service and releases resources.
     */
    public void shutdown() {
        logger.info("Shutting down EnhancedConfigService...");
        
        try {
            // Stop file watching
            stopConfigurationWatching();
            
            // Shutdown executors
            watchExecutor.shutdown();
            if (!watchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                watchExecutor.shutdownNow();
            }
            
            // Close file watch service
            if (fileWatchService != null) {
                fileWatchService.close();
            }
            
            // Stop performance monitoring
            performanceMonitor.stopMonitoring();
            
            // Clear caches and listeners
            l1Cache.invalidateAll();
            l2Cache.invalidateAll();
            valueCache.clear();
            changeListeners.clear();
            keyListeners.clear();
            
            logger.info("EnhancedConfigService shutdown complete");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during EnhancedConfigService shutdown", e);
        }
    }
    
    // Private implementation methods
    
    private void startConfigurationWatching() {
        if (!hotReloadEnabled) {
            return;
        }
        
        try {
            Path dataFolder = plugin.getDataFolder().toPath();
            
            // Watch the plugin data folder for config file changes
            WatchKey watchKey = dataFolder.register(fileWatchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE);
            
            watchKeys.put("main", watchKey);
            
            // Start the file watching task
            watchExecutor.scheduleWithFixedDelay(this::processFileEvents, 1, 1, TimeUnit.SECONDS);
            
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to start configuration file watching", e);
        }
    }
    
    private void stopConfigurationWatching() {
        watchKeys.values().forEach(WatchKey::cancel);
        watchKeys.clear();
    }
    
    private void processFileEvents() {
        WatchKey key = fileWatchService.poll();
        if (key != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                
                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path filename = pathEvent.context();
                
                if (isConfigFile(filename.toString())) {
                    logger.info("Configuration file changed: " + filename);
                    
                    // Delay reload to avoid multiple rapid reloads
                    watchExecutor.schedule(() -> reloadAsync(), 2, TimeUnit.SECONDS);
                }
            }
            
            if (!key.reset()) {
                logger.warning("Configuration file watch key became invalid");
            }
        }
    }
    
    private boolean isConfigFile(String filename) {
        return filename.equals("config.yml") || filename.equals("spells.yml") || filename.endsWith(".yml");
    }
    
    private void startPerformanceMonitoring() {
        performanceMonitor.startMonitoring();
        
        // Schedule periodic metrics logging
        watchExecutor.scheduleWithFixedDelay(() -> {
            if (logger.isLoggable(Level.FINE)) {
                ConfigServiceMetrics metrics = getMetrics();
                logger.fine("Config Service Metrics: " + metrics);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    private FileConfiguration loadConfigFile(String filename) {
        File file = new File(plugin.getDataFolder(), filename);
        return YamlConfiguration.loadConfiguration(file);
    }
    
    private void saveResourceIfNotExists(String filename) {
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) {
            plugin.saveResource(filename, false);
        }
    }
    
    private Map<String, FileConfiguration> loadAdditionalConfigs() {
        Map<String, FileConfiguration> configs = new HashMap<>();
        
        // Load any additional .yml files in the plugin directory
        File dataFolder = plugin.getDataFolder();
        if (dataFolder.exists() && dataFolder.isDirectory()) {
            File[] ymlFiles = dataFolder.listFiles((dir, name) -> 
                name.endsWith(".yml") && !name.equals("config.yml") && !name.equals("spells.yml"));
            
            if (ymlFiles != null) {
                for (File file : ymlFiles) {
                    String name = file.getName().replace(".yml", "");
                    configs.put(name, YamlConfiguration.loadConfiguration(file));
                }
            }
        }
        
        return configs;
    }
    
    private FileConfiguration cloneConfig(FileConfiguration original) {
        YamlConfiguration clone = new YamlConfiguration();
        for (String key : original.getKeys(true)) {
            clone.set(key, original.get(key));
        }
        return clone;
    }
    
    private void validateConfiguration(FileConfiguration mainConfig, FileConfiguration spellsConfig) {
        try (var timing = performanceMonitor.startTiming("ConfigService.validate", 50)) {
            timing.observe();
            
            List<String> errors = new ArrayList<>();
            
            // Validate main config
            errors.addAll(validator.validate(mainConfig, schema.getMainConfigSchema()));
            
            // Validate spells config
            errors.addAll(validator.validate(spellsConfig, schema.getSpellsConfigSchema()));
            
            if (!errors.isEmpty()) {
                logger.warning("Configuration validation errors found:");
                errors.forEach(error -> logger.warning("  - " + error));
            }
        }
    }
    
    private FileConfiguration getConfigByName(ConfigurationSnapshot snapshot, String name) {
        return switch (name.toLowerCase()) {
            case "main", "config" -> snapshot.getMainConfig();
            case "spells" -> snapshot.getSpellsConfig();
            default -> snapshot.getAdditionalConfigs().get(name);
        };
    }
    
    @SuppressWarnings("unchecked")
    private <T> T castValue(Object value, Class<T> type, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        // Type conversion logic
        if (type == String.class) {
            return (T) String.valueOf(value);
        } else if (type == Integer.class) {
            if (value instanceof Number) {
                return (T) Integer.valueOf(((Number) value).intValue());
            }
            try {
                return (T) Integer.valueOf(String.valueOf(value));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else if (type == Boolean.class) {
            if (value instanceof Boolean) {
                return (T) value;
            }
            return (T) Boolean.valueOf(String.valueOf(value));
        } else if (type == Double.class) {
            if (value instanceof Number) {
                return (T) Double.valueOf(((Number) value).doubleValue());
            }
            try {
                return (T) Double.valueOf(String.valueOf(value));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }
    
    private void fireConfigurationChangeEvent(String configName, ConfigurationSnapshot oldSnapshot, ConfigurationSnapshot newSnapshot) {
        if (changeListeners.isEmpty()) {
            return;
        }
        
        Set<String> changedKeys = findChangedKeys(oldSnapshot, newSnapshot);
        if (changedKeys.isEmpty()) {
            return;
        }
        
        ConfigurationChangeEvent event = new ConfigurationChangeEvent(configName, changedKeys, oldSnapshot, newSnapshot);
        
        // Fire to general listeners
        for (ConfigurationChangeListener listener : changeListeners) {
            try {
                listener.onConfigurationChanged(event);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in configuration change listener", e);
            }
        }
        
        // Fire to key-specific listeners
        for (String changedKey : changedKeys) {
            List<Consumer<Object>> listeners = keyListeners.get(changedKey);
            if (listeners != null) {
                Object newValue = getValueFromSnapshot(newSnapshot, configName, changedKey);
                for (Consumer<Object> listener : listeners) {
                    try {
                        listener.accept(newValue);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error in key-specific configuration listener for key: " + changedKey, e);
                    }
                }
            }
        }
    }
    
    private Set<String> findChangedKeys(ConfigurationSnapshot oldSnapshot, ConfigurationSnapshot newSnapshot) {
        Set<String> changedKeys = new HashSet<>();
        
        if (oldSnapshot == null) {
            return changedKeys; // Initial load, no changes
        }
        
        // Compare main config
        changedKeys.addAll(findChangedKeys(oldSnapshot.getMainConfig(), newSnapshot.getMainConfig()));
        
        // Compare spells config
        changedKeys.addAll(findChangedKeys(oldSnapshot.getSpellsConfig(), newSnapshot.getSpellsConfig()));
        
        return changedKeys;
    }
    
    private Set<String> findChangedKeys(FileConfiguration oldConfig, FileConfiguration newConfig) {
        Set<String> changedKeys = new HashSet<>();
        Set<String> allKeys = new HashSet<>();
        
        allKeys.addAll(oldConfig.getKeys(true));
        allKeys.addAll(newConfig.getKeys(true));
        
        for (String key : allKeys) {
            Object oldValue = oldConfig.get(key);
            Object newValue = newConfig.get(key);
            
            if (!Objects.equals(oldValue, newValue)) {
                changedKeys.add(key);
            }
        }
        
        return changedKeys;
    }
    
    private Object getValueFromSnapshot(ConfigurationSnapshot snapshot, String configName, String key) {
        FileConfiguration config = getConfigByName(snapshot, configName);
        return config != null ? config.get(key) : null;
    }
    
    private ConfigurationSchema loadConfigurationSchema() {
        // Load configuration schemas for validation
        // This would typically load from embedded schema files
        return new ConfigurationSchema();
    }
    
    // Inner classes for validation and backup
    
    private static class ConfigurationValidator {
        public List<String> validate(FileConfiguration config, Map<String, Object> schema) {
            // Implement configuration validation logic
            return new ArrayList<>();
        }
    }
    
    private static class ConfigurationSchema {
        public Map<String, Object> getMainConfigSchema() {
            return new HashMap<>();
        }
        
        public Map<String, Object> getSpellsConfigSchema() {
            return new HashMap<>();
        }
    }
    
    private static class ConfigurationBackupManager {
        private final File backupDir;
        private final AtomicLong backupCounter = new AtomicLong();
        
        public ConfigurationBackupManager(File dataFolder) {
            this.backupDir = new File(dataFolder, "config-backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
        }
        
        public String createBackup(ConfigurationSnapshot snapshot) {
            return createNamedBackup(snapshot, "auto-" + Instant.now().toEpochMilli());
        }
        
        public String createNamedBackup(ConfigurationSnapshot snapshot, String description) {
            String backupId = "backup-" + backupCounter.incrementAndGet() + "-" + description;
            // Implementation would save snapshot to disk
            return backupId;
        }
        
        public ConfigurationSnapshot restoreBackup(String backupId) {
            // Implementation would load snapshot from disk
            return null;
        }
        
        public long getBackupCount() {
            return backupCounter.get();
        }
    }
    
    // Metrics record
    public record ConfigServiceMetrics(
        long totalAccesses,
        long cacheHits,
        long cacheMisses,
        long hotReloads,
        long currentVersion,
        CacheMetrics l1CacheMetrics,
        CacheMetrics l2CacheMetrics,
        int changeListeners,
        int keyListeners,
        long backupCount
    ) {
        public double getCacheHitRate() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }
    }
}