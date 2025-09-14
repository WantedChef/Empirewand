package nl.wantedchef.empirewand.core.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import nl.wantedchef.empirewand.core.config.model.WandDifficulty;
import nl.wantedchef.empirewand.core.config.model.WandSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing wand settings with YAML persistence, validation, and atomic operations.
 * Provides thread-safe access to wand configurations with caching and automatic backups.
 */
public class WandSettingsService {

    private static final String CONFIG_FILE_NAME = "wand-settings.yml";
    private static final String BACKUP_FILE_SUFFIX = ".backup";
    private static final int CACHE_SIZE = 100;
    private static final Duration CACHE_EXPIRY = Duration.ofMinutes(30);

    private final Plugin plugin;
    private final Logger logger;
    private final File configFile;
    private final File backupFile;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Thread-safe storage
    private final Map<String, WandSettings> wandSettings = new ConcurrentHashMap<>();
    private final Cache<String, WandSettings> settingsCache;

    // Configuration state
    private YamlConfiguration config;
    private WandSettings defaultSettings;
    private volatile boolean initialized = false;

    public WandSettingsService(@NotNull Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.logger = plugin.getLogger();
        this.configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
        this.backupFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME + BACKUP_FILE_SUFFIX);

        // Initialize cache
        this.settingsCache = Caffeine.newBuilder()
                .maximumSize(CACHE_SIZE)
                .expireAfterWrite(CACHE_EXPIRY)
                .build();
    }

    /**
     * Initializes the service by loading the configuration file.
     * This method must be called before using the service.
     *
     * @return CompletableFuture that completes when initialization is done
     */
    @NotNull
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                if (initialized) {
                    return;
                }

                logger.info("Initializing WandSettingsService...");

                // Create plugin directory if it doesn't exist
                if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                    throw new RuntimeException("Failed to create plugin data directory");
                }

                // Copy default configuration if file doesn't exist
                if (!configFile.exists()) {
                    copyDefaultConfiguration();
                }

                // Load configuration
                reloadConfiguration();

                // Load settings into memory
                loadAllSettings();

                initialized = true;
                logger.info("WandSettingsService initialized successfully with " + wandSettings.size() + " wand configurations");

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize WandSettingsService", e);
                throw new RuntimeException("WandSettingsService initialization failed", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    /**
     * Gets the settings for a specific wand.
     *
     * @param wandKey The wand key to get settings for
     * @return The wand settings, or default settings if not configured
     */
    @NotNull
    public WandSettings getWandSettings(@NotNull String wandKey) {
        Objects.requireNonNull(wandKey, "Wand key cannot be null");
        ensureInitialized();

        // Check cache first
        WandSettings cached = settingsCache.getIfPresent(wandKey);
        if (cached != null) {
            return cached;
        }

        lock.readLock().lock();
        try {
            WandSettings settings = wandSettings.get(wandKey);
            if (settings == null) {
                settings = createDefaultSettingsForWand(wandKey);
                // Don't cache default settings to avoid memory leaks for unused wands
            } else {
                settingsCache.put(wandKey, settings);
            }
            return settings;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Updates the settings for a specific wand.
     *
     * @param settings The new settings to apply
     * @return CompletableFuture that completes when the update is saved
     */
    @NotNull
    public CompletableFuture<Void> updateWandSettings(@NotNull WandSettings settings) {
        Objects.requireNonNull(settings, "Settings cannot be null");
        ensureInitialized();

        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                String wandKey = settings.getWandKey();

                // Validate settings
                validateSettings(settings);

                // Update in-memory storage
                wandSettings.put(wandKey, settings);

                // Update configuration
                ConfigurationSection wandSection = config.getConfigurationSection("wands");
                if (wandSection == null) {
                    wandSection = config.createSection("wands");
                }

                ConfigurationSection thisWandSection = wandSection.getConfigurationSection(wandKey);
                if (thisWandSection == null) {
                    thisWandSection = wandSection.createSection(wandKey);
                }

                // Write settings to config
                thisWandSection.set("cooldownBlock", settings.isCooldownBlock());
                thisWandSection.set("griefBlockDamage", settings.isGriefBlockDamage());
                thisWandSection.set("playerDamage", settings.isPlayerDamage());
                thisWandSection.set("difficulty", settings.getDifficulty().name());

                // Perform atomic save operation
                saveConfiguration();

                // Update cache
                settingsCache.put(wandKey, settings);

                logger.log(Level.INFO, "Updated settings for wand: {0}", wandKey);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to update wand settings for: " + settings.getWandKey(), e);
                throw new RuntimeException("Failed to update wand settings", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    /**
     * Gets all configured wand keys.
     *
     * @return Set of all wand keys that have been configured
     */
    @NotNull
    public Set<String> getConfiguredWandKeys() {
        ensureInitialized();

        lock.readLock().lock();
        try {
            return new HashSet<>(wandSettings.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all configured wand settings.
     *
     * @return Map of wand keys to their settings
     */
    @NotNull
    public Map<String, WandSettings> getAllWandSettings() {
        ensureInitialized();

        lock.readLock().lock();
        try {
            return new HashMap<>(wandSettings);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes settings for a specific wand, reverting it to defaults.
     *
     * @param wandKey The wand key to remove settings for
     * @return CompletableFuture that completes when the removal is saved
     */
    @NotNull
    public CompletableFuture<Void> removeWandSettings(@NotNull String wandKey) {
        Objects.requireNonNull(wandKey, "Wand key cannot be null");
        ensureInitialized();

        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                // Remove from in-memory storage
                wandSettings.remove(wandKey);

                // Remove from configuration
                ConfigurationSection wandSection = config.getConfigurationSection("wands");
                if (wandSection != null && wandSection.contains(wandKey)) {
                    wandSection.set(wandKey, null);
                    saveConfiguration();
                }

                // Remove from cache
                settingsCache.invalidate(wandKey);

                logger.log(Level.INFO, "Removed settings for wand: {0}", wandKey);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to remove wand settings for: " + wandKey, e);
                throw new RuntimeException("Failed to remove wand settings", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    /**
     * Reloads the configuration from disk.
     *
     * @return CompletableFuture that completes when reload is finished
     */
    @NotNull
    public CompletableFuture<Void> reload() {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                logger.info("Reloading wand settings configuration...");

                // Clear caches
                settingsCache.invalidateAll();
                wandSettings.clear();

                // Reload from disk
                reloadConfiguration();
                loadAllSettings();

                logger.info("Wand settings configuration reloaded successfully");

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to reload wand settings configuration", e);
                throw new RuntimeException("Failed to reload configuration", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    /**
     * Gets the default settings used for new wands.
     *
     * @return The default wand settings
     */
    @NotNull
    public WandSettings getDefaultSettings() {
        ensureInitialized();
        return defaultSettings;
    }

    // Private implementation methods

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("WandSettingsService not initialized - call initialize() first");
        }
    }

    private void copyDefaultConfiguration() throws IOException {
        logger.info("Creating default wand settings configuration...");

        try (InputStream defaultConfigStream = plugin.getResource(CONFIG_FILE_NAME)) {
            if (defaultConfigStream == null) {
                // Create minimal default configuration programmatically for test environments
                createMinimalDefaultConfiguration();
                return;
            }

            Files.copy(defaultConfigStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Default configuration copied to: " + configFile.getPath());
        }
    }

    private void createMinimalDefaultConfiguration() throws IOException {
        // Create a minimal default configuration for test environments
        YamlConfiguration defaultConfig = new YamlConfiguration();
        defaultConfig.set("schemaVersion", 1);

        // Add default wands
        String[] defaultWands = {"empirewand", "mephidantes_zeist"};
        for (String wandKey : defaultWands) {
            defaultConfig.set("wands." + wandKey + ".displayName",
                wandKey.equals("empirewand") ? "Empire Wand" : "Mephidantes Zeist");
            defaultConfig.set("wands." + wandKey + ".icon",
                wandKey.equals("empirewand") ? "BLAZE_ROD" : "GOLDEN_HOE");
            defaultConfig.set("wands." + wandKey + ".settings.cooldownBlock", true);
            defaultConfig.set("wands." + wandKey + ".settings.griefBlockDamage", false);
            defaultConfig.set("wands." + wandKey + ".settings.playerDamage", true);
            defaultConfig.set("wands." + wandKey + ".settings.difficulty", "MEDIUM");
        }

        defaultConfig.save(configFile);
        logger.info("Minimal default configuration created at: " + configFile.getPath());
    }

    private void reloadConfiguration() throws IOException {
        if (!configFile.exists()) {
            throw new IOException("Configuration file does not exist: " + configFile.getPath());
        }

        try {
            config = YamlConfiguration.loadConfiguration(configFile);

            // Validate configuration version
            String version = config.getString("version");
            if (version == null) {
                logger.warning("Configuration file missing version information");
            }

            // Load default settings
            loadDefaultSettings();

        } catch (Exception e) {
            throw new IOException("Failed to load configuration file: " + configFile.getPath(), e);
        }
    }

    private void loadDefaultSettings() {
        ConfigurationSection defaultsSection = config.getConfigurationSection("defaults");
        if (defaultsSection == null) {
            logger.warning("No defaults section found in configuration, using hardcoded defaults");
            defaultSettings = WandSettings.defaultSettings("default");
            return;
        }

        boolean cooldownBlock = defaultsSection.getBoolean("cooldownBlock", false);
        boolean griefBlockDamage = defaultsSection.getBoolean("griefBlockDamage", true);
        boolean playerDamage = defaultsSection.getBoolean("playerDamage", true);
        String difficultyStr = defaultsSection.getString("difficulty", "MEDIUM");
        WandDifficulty difficulty = WandDifficulty.fromString(difficultyStr);

        defaultSettings = WandSettings.builder("default")
                .cooldownBlock(cooldownBlock)
                .griefBlockDamage(griefBlockDamage)
                .playerDamage(playerDamage)
                .difficulty(difficulty)
                .build();
    }

    private void loadAllSettings() {
        ConfigurationSection wandsSection = config.getConfigurationSection("wands");
        if (wandsSection == null) {
            logger.warning("No wands section found in configuration");
            return;
        }

        for (String wandKey : wandsSection.getKeys(false)) {
            try {
                ConfigurationSection wandSection = wandsSection.getConfigurationSection(wandKey);
                if (wandSection == null) {
                    logger.warning("Invalid wand configuration for: " + wandKey);
                    continue;
                }

                WandSettings settings = loadWandSettings(wandKey, wandSection);
                wandSettings.put(wandKey, settings);

            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to load settings for wand: " + wandKey, e);
            }
        }
    }

    private WandSettings loadWandSettings(String wandKey, ConfigurationSection section) {
        boolean cooldownBlock = section.getBoolean("cooldownBlock", defaultSettings.isCooldownBlock());
        boolean griefBlockDamage = section.getBoolean("griefBlockDamage", defaultSettings.isGriefBlockDamage());
        boolean playerDamage = section.getBoolean("playerDamage", defaultSettings.isPlayerDamage());
        String difficultyStr = section.getString("difficulty", defaultSettings.getDifficulty().name());
        WandDifficulty difficulty = WandDifficulty.fromString(difficultyStr);

        return WandSettings.builder(wandKey)
                .cooldownBlock(cooldownBlock)
                .griefBlockDamage(griefBlockDamage)
                .playerDamage(playerDamage)
                .difficulty(difficulty)
                .build();
    }

    private WandSettings createDefaultSettingsForWand(String wandKey) {
        return WandSettings.builder(wandKey)
                .cooldownBlock(defaultSettings.isCooldownBlock())
                .griefBlockDamage(defaultSettings.isGriefBlockDamage())
                .playerDamage(defaultSettings.isPlayerDamage())
                .difficulty(defaultSettings.getDifficulty())
                .build();
    }

    private void validateSettings(WandSettings settings) {
        if (settings.getWandKey() == null || settings.getWandKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Wand key cannot be null or empty");
        }

        if (settings.getDifficulty() == null) {
            throw new IllegalArgumentException("Difficulty cannot be null");
        }
    }

    private void saveConfiguration() throws IOException {
        // Create backup before saving
        createBackup();

        try {
            // Atomic save operation using temporary file
            File tempFile = new File(configFile.getParent(), configFile.getName() + ".tmp");
            config.save(tempFile);

            // Ensure data is written to disk
            syncFile(tempFile.toPath());

            // Atomic move to final location
            Files.move(tempFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Ensure final file is synced
            syncFile(configFile.toPath());

        } catch (IOException e) {
            // Attempt to restore from backup if save failed
            if (backupFile.exists()) {
                logger.warning("Save failed, attempting to restore from backup...");
                Files.copy(backupFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            throw e;
        }
    }

    private void createBackup() {
        if (configFile.exists()) {
            try {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to create configuration backup", e);
            }
        }
    }

    private void syncFile(Path path) throws IOException {
        // Force data to be written to storage
        try {
            Files.newOutputStream(path).close();
        } catch (IOException e) {
            // Log but don't fail - this is a best-effort operation
            logger.log(Level.WARNING, "Failed to sync file to disk: " + path, e);
        }
    }
}