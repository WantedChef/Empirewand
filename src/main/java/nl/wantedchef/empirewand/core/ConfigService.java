package nl.wantedchef.empirewand.core;

import nl.wantedchef.empirewand.core.config.ConfigMigrationService;
import nl.wantedchef.empirewand.core.config.ConfigValidator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import nl.wantedchef.empirewand.core.config.ReadOnlyConfig;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;

import java.util.logging.Level;
import java.util.Objects;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {
        "EI_EXPOSE_REP2" }, justification = "Bukkit Plugin reference retained for lifecycle + logging; service returns read-only wrappers so only the plugin field triggers exposure warning.")
public class ConfigService {
    private final Plugin plugin;
    private final ConfigValidator validator;
    private final ConfigMigrationService migrationService;
    private FileConfiguration config; // internal mutable reference
    private FileConfiguration spellsConfig; // internal mutable reference
    private ReadableConfig readOnlyConfig; // cached read-only view
    private ReadableConfig readOnlySpellsConfig; // cached read-only view

    public ConfigService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.validator = new ConfigValidator();
        this.migrationService = new ConfigMigrationService(plugin, validator);
        loadConfigs();
    }

    public final void loadConfigs() {
        try {
            // Load config.yml
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            this.config = plugin.getConfig();

            if (this.config == null) {
                plugin.getLogger().severe("Failed to load main config.yml");
                return;
            }

            // Load spells.yml with proper error handling
            File spellsFile = new File(plugin.getDataFolder(), "spells.yml");
            if (!spellsFile.exists()) {
                try {
                    plugin.saveResource("spells.yml", false);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save default spells.yml", e);
                    return;
                }
            }

            // Validate spells file exists and is readable
            if (!spellsFile.isFile() || !spellsFile.canRead()) {
                plugin.getLogger().severe("spells.yml is not a file or cannot be read.");
                return;
            }

            try {
                this.spellsConfig = YamlConfiguration.loadConfiguration(spellsFile);
                if (this.spellsConfig == null) {
                    plugin.getLogger().severe("Failed to load spells.yml configuration");
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading spells.yml", e);
                return;
            }

            // (Re)create read-only wrappers
            this.readOnlyConfig = new ReadOnlyConfig(this.config);
            this.readOnlySpellsConfig = new ReadOnlyConfig(this.spellsConfig);

            // Validate and migrate configs
            validateAndMigrateConfigs(spellsFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error loading configurations", e);
        }
    }

    /**
     * Validates and migrates the loaded configurations.
     *
     * @param spellsFile the spells.yml file
     */
    private void validateAndMigrateConfigs(File spellsFile) {
        if (spellsFile == null) {
            plugin.getLogger().warning("Cannot validate configs - spells file is null");
            return;
        }

        try {
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
            if (!configFile.isFile() || !configFile.canRead()) {
                plugin.getLogger().severe("config.yml is not a file or cannot be read for migration.");
                return;
            }

            boolean mainConfigMigrated = migrationService.migrateMainConfig(config, configFile);
            boolean spellsConfigMigrated = migrationService.migrateSpellsConfig(spellsConfig, spellsFile);

            if (mainConfigMigrated || spellsConfigMigrated) {
                plugin.getLogger().info("Configuration migration completed. Reloading configs...");
                // Reload configs after migration
                try {
                    plugin.reloadConfig();
                    this.config = plugin.getConfig();
                    this.spellsConfig = YamlConfiguration.loadConfiguration(spellsFile);
                    this.readOnlyConfig = new ReadOnlyConfig(this.config);
                    this.readOnlySpellsConfig = new ReadOnlyConfig(this.spellsConfig);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error reloading configs after migration", e);
                }
            }

            plugin.getLogger().info("Configuration validation and migration completed successfully.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during configuration validation and migration", e);
        }
    }

    /**
     * Returns a read-only view of the main configuration.
     */
    public ReadableConfig getConfig() {
        if (readOnlyConfig == null) {
            plugin.getLogger().warning("Read-only config wrapper is null, returning empty config");
            return new ReadOnlyConfig(new org.bukkit.configuration.MemoryConfiguration());
        }
        return readOnlyConfig;
    }

    /**
     * Returns a read-only view of the spells configuration.
     */
    public ReadableConfig getSpellsConfig() {
        if (readOnlySpellsConfig == null) {
            plugin.getLogger().warning("Read-only spells config wrapper is null, returning empty config");
            return new ReadOnlyConfig(new org.bukkit.configuration.MemoryConfiguration());
        }
        return readOnlySpellsConfig;
    }

    public String getMessage(String key) {
        if (key == null || key.trim().isEmpty()) {
            return "";
        }
        try {
            return config != null ? config.getString("messages." + key, "") : "";
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting message for key: " + key, e);
            return "";
        }
    }

    public boolean getFeatureFlag(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        try {
            return config != null ? config.getBoolean("features." + key, false) : false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting feature flag for key: " + key, e);
            return false;
        }
    }

    public ConfigMigrationService getMigrationService() {
        return migrationService;
    }

    public long getDefaultCooldown() {
        try {
            return config != null ? config.getLong("cooldowns.default", 500) : 500;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting default cooldown", e);
            return 500;
        }
    }

    /**
     * Returns the list of spell keys in the given category from config.yml
     * under categories.<name>.spells. Returns an empty list if missing.
     */
    public java.util.List<String> getCategorySpells(String name) {
        if (name == null || name.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        try {
            if (config == null) {
                return java.util.Collections.emptyList();
            }
            java.util.List<String> list = config.getStringList("categories." + name + ".spells");
            return list != null ? list : java.util.Collections.emptyList();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting category spells for: " + name, e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Returns available category names under categories.* in config.yml.
     */
    public java.util.Set<String> getCategoryNames() {
        try {
            if (config == null) {
                return java.util.Set.of();
            }
            var section = config.getConfigurationSection("categories");
            if (section == null) {
                return java.util.Set.of();
            }
            java.util.Set<String> keys = section.getKeys(false);
            return keys != null ? keys : java.util.Set.of();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting category names", e);
            return java.util.Set.of();
        }
    }

    /**
     * Reloads all configurations safely.
     */
    public void reload() {
        loadConfigs();
    }

    /**
     * Checks if configurations are properly loaded.
     *
     * @return true if both configs are loaded and valid
     */
    public boolean isConfigLoaded() {
        return config != null && spellsConfig != null && 
               readOnlyConfig != null && readOnlySpellsConfig != null;
    }
}
