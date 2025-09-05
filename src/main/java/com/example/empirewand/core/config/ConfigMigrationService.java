package com.example.empirewand.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Handles configuration migrations based on config-version.
 * Performs non-destructive migrations with automatic backups.
 */
public class ConfigMigrationService {

    private final Plugin plugin;
    private final ConfigValidator validator;

    public ConfigMigrationService(Plugin plugin, ConfigValidator validator) {
        this.plugin = plugin;
        this.validator = validator;
    }

    /**
     * Migrates the main config if necessary.
     *
     * @param config the current config
     * @param configFile the config file
     * @return true if migration was performed, false otherwise
     */
    public boolean migrateMainConfig(FileConfiguration config, File configFile) {
        return migrateMainConfig(config, configFile, false);
    }

    /**
     * Migrates the main config if necessary.
     *
     * @param config the current config
     * @param configFile the config file
     * @param force force migration even if versions match
     * @return true if migration was performed, false otherwise
     */
    public boolean migrateMainConfig(FileConfiguration config, File configFile, boolean force) {
        String currentVersion = getConfigVersion(config);
        if (currentVersion == null) {
            plugin.getLogger().warning("No config-version found in config.yml, assuming version 1.0");
            currentVersion = "1.0";
        }

        String targetVersion = "1.0"; // Current target version

        if (!force && targetVersion.equals(currentVersion)) {
            return false; // No migration needed
        }

        plugin.getLogger().info("Migrating main config from version " + currentVersion + " to " + targetVersion);

        // Perform step-by-step migration
        boolean migrated = false;

        // Migration steps from older versions to current
        if (compareVersions(currentVersion, "1.0") < 0) {
            migrated = migrateTo10(config, configFile) || migrated;
        }

        // Future migrations:
        // if (compareVersions(currentVersion, "1.1") < 0) {
        //     migrated = migrateTo11(config, configFile) || migrated;
        // }

        if (migrated) {
            plugin.getLogger().info("Main config migration completed successfully");
        }

        return migrated;
    }

    /**
     * Migrates the spells config if necessary.
     *
     * @param spellsConfig the current spells config
     * @param spellsFile the spells file
     * @return true if migration was performed, false otherwise
     */
    public boolean migrateSpellsConfig(FileConfiguration spellsConfig, File spellsFile) {
        return migrateSpellsConfig(spellsConfig, spellsFile, false);
    }

    /**
     * Migrates the spells config if necessary.
     *
     * @param spellsConfig the current spells config
     * @param spellsFile the spells file
     * @param force force migration even if versions match
     * @return true if migration was performed, false otherwise
     */
    public boolean migrateSpellsConfig(FileConfiguration spellsConfig, File spellsFile, boolean force) {
        String currentVersion = getConfigVersion(spellsConfig);
        if (currentVersion == null) {
            plugin.getLogger().warning("No config-version found in spells.yml, assuming version 1.0");
            currentVersion = "1.0";
        }

        String targetVersion = "1.0"; // Current target version

        if (!force && targetVersion.equals(currentVersion)) {
            return false; // No migration needed
        }

        plugin.getLogger().info("Migrating spells config from version " + currentVersion + " to " + targetVersion);

        // Perform step-by-step migration
        boolean migrated = false;

        if (compareVersions(currentVersion, "1.0") < 0) {
            migrated = migrateSpellsTo10(spellsConfig, spellsFile) || migrated;
        }

        if (migrated) {
            plugin.getLogger().info("Spells config migration completed successfully");
        }

        return migrated;
    }

    /**
     * Performs a migration of spells config to version 1.0.
     */
    private boolean migrateSpellsTo10(FileConfiguration spellsConfig, File spellsFile) {
        plugin.getLogger().info("Migrating spells config to version 1.0");

        // Create backup
        File backup = createBackup(spellsFile);
        if (backup == null) {
            plugin.getLogger().severe("Migration aborted: could not create backup");
            return false;
        }

        try {
            // Step 1: Ensure all spells have required fields
            plugin.getLogger().info("Migration Step 1: Validating spell configurations");

            // Step 2: Update version
            plugin.getLogger().info("Migration Step 2: Updating spells config version");
            spellsConfig.set("config-version", "1.0");

            // Save the migrated config
            spellsConfig.save(spellsFile);

            // Validate the migrated config
            var errors = validator.validateSpellsConfig(spellsConfig);
            if (!errors.isEmpty()) {
                plugin.getLogger().severe("Spells migration validation failed: " + String.join(", ", errors));
                // Restore from backup
                restoreFromBackup(backup, spellsFile);
                return false;
            }

            plugin.getLogger().info("Successfully migrated spells config to version 1.0");
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Spells migration failed", e);
            // Restore from backup
            restoreFromBackup(backup, spellsFile);
            return false;
        }
    }

    /**
     * Performs a forced migration of all configs (for CLI use).
     *
     * @return true if any migration was performed
     */
    public boolean migrateAllConfigs() {
        plugin.getLogger().info("Performing forced migration of all configurations...");

        boolean migrated = false;

        // Migrate main config
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            FileConfiguration config = plugin.getConfig();
            migrated = migrateMainConfig(config, configFile, true) || migrated;
        }

        // Migrate spells config
        File spellsFile = new File(plugin.getDataFolder(), "spells.yml");
        if (spellsFile.exists()) {
            FileConfiguration spellsConfig = YamlConfiguration.loadConfiguration(spellsFile);
            migrated = migrateSpellsConfig(spellsConfig, spellsFile, true) || migrated;
        }

        if (migrated) {
            plugin.getLogger().info("Forced migration completed. Please restart the server for changes to take effect.");
        } else {
            plugin.getLogger().info("No migrations were necessary.");
        }

        return migrated;
    }

    /**
     * Creates a backup of the given config file.
     *
     * @param originalFile the file to backup
     * @return the backup file, or null if backup failed
     */
    public File createBackup(File originalFile) {
        try {
            Path originalPath = originalFile.toPath();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = originalFile.getName().replace(".yml", "_backup_" + timestamp + ".yml");
            Path backupPath = originalFile.getParentFile().toPath().resolve(backupFileName);

            Files.copy(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Created backup: " + backupPath.getFileName());
            return backupPath.toFile();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create backup for " + originalFile.getName(), e);
            return null;
        }
    }

    /**
     * Restores a config from backup.
     *
     * @param backupFile the backup file
     * @param targetFile the target file to restore to
     * @return true if restore succeeded, false otherwise
     */
    public boolean restoreFromBackup(File backupFile, File targetFile) {
        try {
            Files.copy(backupFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Restored config from backup: " + backupFile.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore from backup " + backupFile.getName(), e);
            return false;
        }
    }

    /**
     * Gets the config version from a configuration.
     *
     * @param config the configuration
     * @return the version string, or null if not found
     */
    private String getConfigVersion(FileConfiguration config) {
        Object version = config.get("config-version");
        if (version == null) {
            return null;
        }
        if (version instanceof String) {
            return (String) version;
        }
        if (version instanceof Number) {
            return String.valueOf(version);
        }
        return null;
    }

    /**
     * Performs a migration from any version to 1.0.
     * This is a template for future migrations.
     */
    private boolean migrateTo10(FileConfiguration config, File configFile) {
        plugin.getLogger().info("Migrating config to version 1.0");

        // Create backup
        File backup = createBackup(configFile);
        if (backup == null) {
            plugin.getLogger().severe("Migration aborted: could not create backup");
            return false;
        }

        try {
            // Step 1: Ensure metrics section exists
            if (!config.contains("metrics")) {
                plugin.getLogger().info("Migration Step 1: Adding metrics section");
                config.set("metrics.enabled", true);
                config.set("metrics.debug", false);
            }

            // Step 2: Ensure cooldowns section has default
            if (!config.contains("cooldowns.default")) {
                plugin.getLogger().info("Migration Step 2: Adding default cooldown");
                config.set("cooldowns.default", 500);
            }

            // Step 3: Ensure features section exists
            if (!config.contains("features")) {
                plugin.getLogger().info("Migration Step 3: Adding features section");
                config.set("features.block-damage", false);
                config.set("features.friendly-fire", false);
            }

            // Step 4: Update version
            plugin.getLogger().info("Migration Step 4: Updating config version");
            config.set("config-version", "1.0");

            // Save the migrated config
            config.save(configFile);

            // Validate the migrated config
            var errors = validator.validateMainConfig(config);
            if (!errors.isEmpty()) {
                plugin.getLogger().severe("Migration validation failed: " + String.join(", ", errors));
                // Restore from backup
                restoreFromBackup(backup, configFile);
                return false;
            }

            plugin.getLogger().info("Successfully migrated config to version 1.0");
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Migration failed", e);
            // Restore from backup
            restoreFromBackup(backup, configFile);
            return false;
        }
    }

    /**
     * Compares two version strings.
     * Returns negative if v1 < v2, 0 if equal, positive if v1 > v2.
     */
    private int compareVersions(String v1, String v2) {
        if (v1 == null || v2 == null) return 0;

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (part1 < part2) return -1;
            if (part1 > part2) return 1;
        }
        return 0;
    }
}