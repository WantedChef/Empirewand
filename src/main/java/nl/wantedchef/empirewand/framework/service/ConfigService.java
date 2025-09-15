package nl.wantedchef.empirewand.framework.service;

import nl.wantedchef.empirewand.core.config.ConfigMigrationService;
import nl.wantedchef.empirewand.core.config.ConfigValidator;
import nl.wantedchef.empirewand.core.util.PerformanceMonitor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import nl.wantedchef.empirewand.core.config.ReadOnlyConfig;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages the plugin's configurations, including loading, validation, and migration.
 * <p>
 * This service is responsible for handling `config.yml` and `spells.yml`.
 * It provides read-only access to the configurations to prevent uncontrolled modifications.
 * 
 * Optimized for performance with caching and efficient data structures.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {
        "EI_EXPOSE_REP2" }, justification = "Bukkit Plugin reference retained for lifecycle + logging; service returns read-only wrappers so only the plugin field triggers exposure warning.")
public class ConfigService {
    private final Plugin plugin;
    private final ConfigValidator validator;
    private final ConfigMigrationService migrationService;
    private FileConfiguration config; // internal mutable reference
    private FileConfiguration spellsConfig; // internal mutable reference
    private volatile ReadableConfig readOnlyConfig; // cached read-only view with volatile for thread safety
    private volatile ReadableConfig readOnlySpellsConfig; // cached read-only view with volatile for thread safety
    
    // Performance monitor for tracking config operations
    private final PerformanceMonitor performanceMonitor;
    
    // Caches for frequently accessed configuration values
    private final ConcurrentHashMap<String, String> messageCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> featureFlagCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> categorySpellsCache = new ConcurrentHashMap<>();
    private volatile Set<String> categoryNamesCache = null;
    
    // Cache for default cooldown value
    private volatile Long defaultCooldownCache = null;
    
    // Cache for spell configuration sections to avoid repeated lookups
    private final ConcurrentHashMap<String, ReadableConfig> spellConfigCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new ConfigService.
     *
     * @param plugin The plugin instance. Must not be null.
     */
    public ConfigService(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
        this.validator = new ConfigValidator();
        this.migrationService = new ConfigMigrationService(plugin, validator);
        this.performanceMonitor = new PerformanceMonitor(plugin.getLogger());
        loadConfigs();
    }

    /**
     * Loads or reloads all configurations from disk, including `config.yml` and `spells.yml`.
     * This method also triggers validation and migration services.
     */
    public final void loadConfigs() {
        try (var timing = performanceMonitor.startTiming("ConfigService.loadConfigs", 50)) {
            // Load config.yml
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            this.config = plugin.getConfig();

            // Load spells.yml
            File spellsFile = new File(plugin.getDataFolder(), "spells.yml");
            if (!spellsFile.exists()) {
                plugin.saveResource("spells.yml", false);
            }
            this.spellsConfig = YamlConfiguration.loadConfiguration(spellsFile);

            // (Re)create read-only wrappers
            this.readOnlyConfig = new ReadOnlyConfig(this.config);
            this.readOnlySpellsConfig = new ReadOnlyConfig(this.spellsConfig);

            // Validate and migrate configs
            validateAndMigrateConfigs(spellsFile);
            
            // Clear caches after config reload
            clearCaches();
            
            plugin.getLogger().info("Configuration loaded successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configurations", e);
            // Initialize with empty configs to prevent NPE
            this.config = new YamlConfiguration();
            this.spellsConfig = new YamlConfiguration();
            this.readOnlyConfig = new ReadOnlyConfig(this.config);
            this.readOnlySpellsConfig = new ReadOnlyConfig(this.spellsConfig);
            
            // Clear caches on error
            clearCaches();
        }
    }

    /**
     * Validates and migrates the loaded configurations.
     *
     * @param spellsFile the spells.yml file
     */
    private void validateAndMigrateConfigs(File spellsFile) {
        try (var timing = performanceMonitor.startTiming("ConfigService.validateAndMigrateConfigs", 100)) {
            // Validate main config
            List<String> mainConfigErrors = validator.validateMainConfig(config);
            if (!mainConfigErrors.isEmpty()) {
                plugin.getLogger().severe("Main config validation errors:");
                for (String error : mainConfigErrors) {
                    plugin.getLogger().log(Level.SEVERE, "  - {0}", error);
                }
                plugin.getLogger().severe("Please fix the configuration errors and restart the server.");
                return;
            }

            // Validate spells config
            List<String> spellsConfigErrors = validator.validateSpellsConfig(spellsConfig);
            if (!spellsConfigErrors.isEmpty()) {
                plugin.getLogger().severe("Spells config validation errors:");
                for (String error : spellsConfigErrors) {
                    plugin.getLogger().log(Level.SEVERE, "  - {0}", error);
                }
                plugin.getLogger().severe("Please fix the configuration errors and restart the server.");
                return;
            }

            // Attempt migrations if needed
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            boolean mainConfigMigrated = migrationService.migrateMainConfig(config, configFile);
            boolean spellsConfigMigrated = migrationService.migrateSpellsConfig(spellsConfig, spellsFile);

            if (mainConfigMigrated || spellsConfigMigrated) {
                plugin.getLogger().info("Configuration migration completed. Reloading configs...");
                // Reload configs after migration
                plugin.reloadConfig();
                this.config = plugin.getConfig();
                this.spellsConfig = YamlConfiguration.loadConfiguration(spellsFile);
                this.readOnlyConfig = new ReadOnlyConfig(this.config);
                this.readOnlySpellsConfig = new ReadOnlyConfig(this.spellsConfig);
                
                // Clear caches after migration
                clearCaches();
            }

            plugin.getLogger().info("Configuration validation and migration completed successfully.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during configuration validation and migration", e);
        }
    }
    
    /**
     * Clears all cached configuration values.
     * This method should be called when configuration is reloaded or during shutdown.
     */
    private void clearCaches() {
        messageCache.clear();
        featureFlagCache.clear();
        categorySpellsCache.clear();
        categoryNamesCache = null;
        defaultCooldownCache = null;
        spellConfigCache.clear();
    }

    /**
     * Returns a read-only view of the main `config.yml` configuration.
     *
     * @return A ReadableConfig instance for safe configuration access.
     */
    public ReadableConfig getConfig() {
        return readOnlyConfig;
    }

    /**
     * Returns a read-only view of the `spells.yml` configuration.
     *
     * @return A ReadableConfig instance for safe spell configuration access.
     */
    public ReadableConfig getSpellsConfig() {
        return readOnlySpellsConfig;
    }

    /**
     * Gets a message from the `messages` section of the main config.
     *
     * @param key The key of the message to retrieve.
     * @return The message string, or an empty string if not found.
     */
    public String getMessage(String key) {
        if (key == null) {
            return "";
        }
        
        // Check cache first
        String cached = messageCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        try (var timing = performanceMonitor.startTiming("ConfigService.getMessage", 5)) {
            String message = config.getString("messages." + key, "");
            // Cache the result
            messageCache.put(key, message);
            return message;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting message for key: " + key);
            return "";
        }
    }

    /**
     * Gets a feature flag from the `features` section of the main config.
     *
     * @param key The key of the feature flag to retrieve.
     * @return The boolean value of the feature flag, or false if not found.
     */
    public boolean getFeatureFlag(String key) {
        if (key == null) {
            return false;
        }
        
        // Check cache first
        Boolean cached = featureFlagCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        try (var timing = performanceMonitor.startTiming("ConfigService.getFeatureFlag", 5)) {
            boolean flag = config.getBoolean("features." + key, false);
            // Cache the result
            featureFlagCache.put(key, flag);
            return flag;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting feature flag for key: " + key);
            return false;
        }
    }

    /**
     * Gets the configuration migration service.
     *
     * @return The ConfigMigrationService instance.
     */
    public ConfigMigrationService getMigrationService() {
        return migrationService;
    }

    /**
     * Gets the default spell cooldown from the configuration.
     *
     * @return The default cooldown in milliseconds.
     */
    public long getDefaultCooldown() {
        // Check cache first
        Long cached = defaultCooldownCache;
        if (cached != null) {
            return cached;
        }
        
        try (var timing = performanceMonitor.startTiming("ConfigService.getDefaultCooldown", 5)) {
            long cooldown = config.getLong("cooldowns.default", 500);
            // Cache the result
            defaultCooldownCache = cooldown;
            return cooldown;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting default cooldown, using fallback");
            return 500;
        }
    }

    /**
     * Returns the list of spell keys in the given category from config.yml
     * under categories.<name>.spells. Returns an empty list if missing.
     *
     * @param name The name of the category.
     * @return A list of spell keys for the category.
     */
    public List<String> getCategorySpells(String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        
        // Check cache first
        List<String> cached = categorySpellsCache.get(name);
        if (cached != null) {
            return cached;
        }
        
        try (var timing = performanceMonitor.startTiming("ConfigService.getCategorySpells", 10)) {
            List<String> list = config.getStringList("categories." + name + ".spells");
            if (list == null) {
                list = Collections.emptyList();
            }
            // Cache the result
            categorySpellsCache.put(name, list);
            return list;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting category spells for: " + name);
            return Collections.emptyList();
        }
    }

    /**
     * Returns available category names under categories.* in config.yml.
     *
     * @return A set of category names.
     */
    public Set<String> getCategoryNames() {
        // Check cache first
        Set<String> cached = categoryNamesCache;
        if (cached != null) {
            return cached;
        }
        
        try (var timing = performanceMonitor.startTiming("ConfigService.getCategoryNames", 10)) {
            var section = config.getConfigurationSection("categories");
            Set<String> names = section == null ? Collections.emptySet() : section.getKeys(false);
            // Cache the result
            categoryNamesCache = names;
            return names;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting category names");
            return Collections.emptySet();
        }
    }
    
    /**
     * Gets a spell-specific configuration section from spells.yml.
     * This method caches the result for better performance.
     *
     * @param spellKey The key of the spell.
     * @return A ReadableConfig for the spell's configuration section.
     */
    public ReadableConfig getSpellConfig(String spellKey) {
        if (spellKey == null || spellKey.isEmpty()) {
            return new ReadOnlyConfig(new YamlConfiguration());
        }
        
        // Check cache first
        ReadableConfig cached = spellConfigCache.get(spellKey);
        if (cached != null) {
            return cached;
        }
        
        try (var timing = performanceMonitor.startTiming("ConfigService.getSpellConfig", 5)) {
            var section = spellsConfig.getConfigurationSection(spellKey);
            YamlConfiguration spellConfig = new YamlConfiguration();
            if (section != null) {
                // Copy the section data to a new YamlConfiguration
                for (String key : section.getKeys(true)) {
                    spellConfig.set(key, section.get(key));
                }
            }
            ReadableConfig readOnlySpellConfig = new ReadOnlyConfig(spellConfig);
            
            // Cache the result
            spellConfigCache.put(spellKey, readOnlySpellConfig);
            return readOnlySpellConfig;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting spell config for key: " + spellKey);
            return new ReadOnlyConfig(new YamlConfiguration());
        }
    }
    
    /**
     * Gets performance metrics for this service.
     *
     * @return A string containing performance metrics.
     */
    public String getPerformanceMetrics() {
        // This method is deprecated in the new PerformanceMonitor
        return "Metrics not available.";
    }

    /**
     * Reloads all configurations safely.
     */
    public void reload() {
        loadConfigs();
    }
}





