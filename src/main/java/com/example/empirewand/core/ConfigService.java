package com.example.empirewand.core;

import com.example.empirewand.core.config.ConfigMigrationService;
import com.example.empirewand.core.config.ConfigValidator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;

public class ConfigService {
    private final Plugin plugin;
    private final ConfigValidator validator;
    private final ConfigMigrationService migrationService;
    private FileConfiguration config;
    private FileConfiguration spellsConfig;

    public ConfigService(Plugin plugin) {
        this.plugin = plugin;
        this.validator = new ConfigValidator();
        this.migrationService = new ConfigMigrationService(plugin, validator);
        loadConfigs();
    }

    public void loadConfigs() {
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

        // Validate and migrate configs
        validateAndMigrateConfigs(spellsFile);
    }

    /**
     * Validates and migrates the loaded configurations.
     *
     * @param spellsFile the spells.yml file
     */
    private void validateAndMigrateConfigs(File spellsFile) {
        // Validate main config
        List<String> mainConfigErrors = validator.validateMainConfig(config);
        if (!mainConfigErrors.isEmpty()) {
            plugin.getLogger().severe("Main config validation errors:");
            for (String error : mainConfigErrors) {
                plugin.getLogger().severe("  - " + error);
            }
            plugin.getLogger().severe("Please fix the configuration errors and restart the server.");
            return;
        }

        // Validate spells config
        List<String> spellsConfigErrors = validator.validateSpellsConfig(spellsConfig);
        if (!spellsConfigErrors.isEmpty()) {
            plugin.getLogger().severe("Spells config validation errors:");
            for (String error : spellsConfigErrors) {
                plugin.getLogger().severe("  - " + error);
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
        }

        plugin.getLogger().info("Configuration validation and migration completed successfully.");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getSpellsConfig() {
        return spellsConfig;
    }

    public String getMessage(String key) {
        return config.getString("messages." + key, "");
    }

    public boolean getFeatureFlag(String key) {
        return config.getBoolean("features." + key, false);
    }

    public ConfigMigrationService getMigrationService() {
        return migrationService;
    }

    public long getDefaultCooldown() {
        return config.getLong("cooldowns.default", 500);
    }
}