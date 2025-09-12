package nl.wantedchef.empirewand.core.config;

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
import java.util.Objects;

/**
 * Handles configuration migrations based on config-version.
 * Performs non-destructive migrations with automatic backups.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {
        "EI_EXPOSE_REP2" }, justification = "Holds reference to Bukkit Plugin to access its logger/data folder; lifecycle-managed singleton per server instance; not exposing mutable internal representation externally.")
public class ConfigMigrationService {

    private final Plugin plugin;
    private final ConfigValidator validator;

    public ConfigMigrationService(Plugin plugin, ConfigValidator validator) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.validator = Objects.requireNonNull(validator, "ConfigValidator cannot be null");
    }

    /**
     * Migrates the main config if necessary.
     *
     * @param config     the current config
     * @param configFile the config file
     * @return true if migration was performed, false otherwise
     */
    public boolean migrateMainConfig(FileConfiguration config, File configFile) {
        return migrateMainConfig(config, configFile, false);
    }

    /**
     * Migrates the main config if necessary.
     *
     * @param config     the current config
     * @param configFile the config file
     * @param force      force migration even if versions match
     * @return true if migration was performed, false otherwise
     */
    public boolean migrateMainConfig(FileConfiguration config, File configFile, boolean force) {
        if (config == null) {
            plugin.getLogger().warning("Cannot migrate null config");
            return false;
        }
        if (configFile == null) {
            plugin.getLogger().warning("Cannot migrate null config file");
            return false;
        }
        
        try {
            String currentVersion = getConfigVersion(config);
            if (currentVersion == null) {
                plugin.getLogger().warning("No config-version found in config.yml, assuming version 1.0");
                currentVersion = "1.0";
            }

            String targetVersion = "1.0"; // Current target version

            if (!force && targetVersion.equals(currentVersion)) {
                return false; // No migration needed
            }

            plugin.getLogger()
                    .info(String.format("Migrating main config from version %s to %s", currentVersion, targetVersion));

            // Perform step-by-step migration
            boolean migrated = false;

            // Migration steps from older versions to current
            if (compareVersions(currentVersion, "1.0") < 0) {
                migrated = migrateTo10(config, configFile) || migrated;
            }

            if (migrated) {
                plugin.getLogger().info("Main config migration completed successfully");
            }

            return migrated;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during main config migration", e);
            return false;
        }
    }

    /**
     * Migrates the spells config if necessary.
     *
     * @param spellsConfig the current spells config
     * @param spellsFile   the spells file
     * @return true if migration was performed, false otherwise
     */
    public boolean migrateSpellsConfig(FileConfiguration spellsConfig, File spellsFile) {
        return migrateSpellsConfig(spellsConfig, spellsFile, false);
    }

    /**
     * Migrates the spells config if necessary.
     *
     * @param spellsConfig the current spells config
     * @param spellsFile   the spells file
     * @param force        force migration even if versions match
     * @return true if migration was performed, false otherwise
     */
    public boolean migrateSpellsConfig(FileConfiguration spellsConfig, File spellsFile, boolean force) {
        if (spellsConfig == null) {
            plugin.getLogger().warning("Cannot migrate null spells config");
            return false;
        }
        if (spellsFile == null) {
            plugin.getLogger().warning("Cannot migrate null spells file");
            return false;
        }
        
        try {
            String currentVersion = getConfigVersion(spellsConfig);
            if (currentVersion == null) {
                plugin.getLogger().warning("No config-version found in spells.yml, assuming version 1.0");
                currentVersion = "1.0";
            }

            String targetVersion = "1.0"; // Current target version

            if (!force && targetVersion.equals(currentVersion)) {
                return false; // No migration needed
            }

            plugin.getLogger()
                    .info(String.format("Migrating spells config from version %s to %s", currentVersion, targetVersion));

            boolean migrated = false;

            // Migration steps from older versions to current
            if (compareVersions(currentVersion, "1.0") < 0) {
                migrated = migrateSpellsTo10(spellsConfig, spellsFile) || migrated;
            }

            if (migrated) {
                plugin.getLogger().info("Spells config migration completed successfully");
            }

            return migrated;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during spells config migration", e);
            return false;
        }
    }

    /**
     * Performs a migration of spells config to version 1.0.
     */
    private boolean migrateSpellsTo10(FileConfiguration spellsConfig, File spellsFile) {
        if (spellsConfig == null || spellsFile == null) {
            plugin.getLogger().warning("Cannot migrate spells config - null parameters");
            return false;
        }

        plugin.getLogger().info("Migrating spells config to version 1.0");

        // Create backup
        File backup = createBackup(spellsFile);
        if (backup == null) {
            plugin.getLogger().severe("Migration aborted: could not create backup");
            return false;
        }

        try {
            // Ensure spells section exists
            if (!spellsConfig.contains("spells")) {
                plugin.getLogger().info("Migration: Adding spells section");
                spellsConfig.createSection("spells");
            }

            // Update version
            spellsConfig.set("config-version", "1.0");

            // Save the migrated config
            spellsConfig.save(spellsFile);

            // Validate the migrated config
            var errors = validator.validateSpellsConfig(spellsConfig);
            if (!errors.isEmpty()) {
                plugin.getLogger().log(Level.SEVERE, "Spells migration validation failed: {0}", String.join(", ", errors));
                // Restore from backup
                restoreFromBackup(backup, spellsFile);
                return false;
            }

            plugin.getLogger().info("Successfully migrated spells config to version 1.0");
            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Spells migration failed", e);
            // Restore from backup
            restoreFromBackup(backup, spellsFile);
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error during spells migration", e);
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
        boolean migrated = false;
        
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            File spellsFile = new File(plugin.getDataFolder(), "spells.yml");

            if (configFile.exists() && configFile.canRead()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                migrated = migrateMainConfig(config, configFile, true) || migrated;
            }

            if (spellsFile.exists() && spellsFile.canRead()) {
                FileConfiguration spellsConfig = YamlConfiguration.loadConfiguration(spellsFile);
                migrated = migrateSpellsConfig(spellsConfig, spellsFile, true) || migrated;
            }

            return migrated;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during forced migration", e);
            return false;
        }
    }

    /**
     * Creates a backup of the given config file.
     *
     * @param originalFile the file to backup
     * @return the backup file, or null if backup failed
     */
    public File createBackup(File originalFile) {
        if (originalFile == null || !originalFile.exists()) {
            plugin.getLogger().warning("Cannot backup null or non-existent file");
            return null;
        }

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupName = originalFile.getName().replace(".yml", "_backup_" + timestamp + ".yml");
            File backupFile = new File(originalFile.getParentFile(), backupName);

            // Ensure parent directory exists
            File parentDir = backupFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    plugin.getLogger().warning("Failed to create backup directory");
                    return null;
                }
            }

            // Create backup with proper resource handling
            Path source = originalFile.toPath();
            Path target = backupFile.toPath();
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

            plugin.getLogger().info(String.format("Created backup: %s", backupFile.getName()));
            return backupFile;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create backup", e);
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error during backup creation", e);
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
        if (backupFile == null || targetFile == null) {
            plugin.getLogger().warning("Cannot restore from null backup or target file");
            return false;
        }

        if (!backupFile.exists()) {
            plugin.getLogger().warning("Backup file does not exist: " + backupFile.getName());
            return false;
        }

        try {
            Path source = backupFile.toPath();
            Path target = targetFile.toPath();
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

            plugin.getLogger().info(String.format("Restored from backup: %s", backupFile.getName()));
            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore from backup", e);
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error during restore", e);
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
        if (config == null) {
            return null;
        }
        try {
            Object version = config.get("config-version");
            if (version == null) {
                return null;
            }
            if (version instanceof String s) {
                return s;
            }
            if (version instanceof Number) {
                return String.valueOf(version);
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning(String.format("Error getting config version: %s", e.getMessage()));
            return null;
        }
    }

    /**
     * Performs a migration from any version to 1.0.
     * This is a template for future migrations.
     */
    private boolean migrateTo10(FileConfiguration config, File configFile) {
        if (config == null || configFile == null) {
            plugin.getLogger().warning("Cannot migrate config - null parameters");
            return false;
        }

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
                plugin.getLogger().log(Level.SEVERE, "Migration validation failed: {0}", String.join(", ", errors));
                // Restore from backup
                restoreFromBackup(backup, configFile);
                return false;
            }

            plugin.getLogger().info("Successfully migrated config to version 1.0");
            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Migration failed", e);
            // Restore from backup
            restoreFromBackup(backup, configFile);
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error during migration", e);
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
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        try {
            String[] parts1 = v1.split("\\.");
            String[] parts2 = v2.split("\\.");

            int maxLength = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < maxLength; i++) {
                int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
                int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
                
                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            }
            return 0;
        } catch (NumberFormatException e) {
            // Fallback to string comparison if parsing fails
            return v1.compareTo(v2);
        }
    }
}
