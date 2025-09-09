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
        this.plugin = plugin;
        this.validator = new ConfigValidator();
        this.migrationService = new ConfigMigrationService(plugin, validator);
        loadConfigs();
    }

    public final void loadConfigs() {
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
        }

        plugin.getLogger().info("Configuration validation and migration completed successfully.");
    }

    /**
     * Returns a read-only view of the main configuration.
     */
    public ReadableConfig getConfig() {
        return readOnlyConfig;
    }

    /**
     * Returns a read-only view of the spells configuration.
     */
    public ReadableConfig getSpellsConfig() {
        return readOnlySpellsConfig;
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

    /**
     * Returns the list of spell keys in the given category from config.yml
     * under categories.<name>.spells. Returns an empty list if missing.
     */
    public java.util.List<String> getCategorySpells(String name) {
        java.util.List<String> list = config.getStringList("categories." + name + ".spells");
        return list;
    }

    /**
     * Returns available category names under categories.* in config.yml.
     */
    public java.util.Set<String> getCategoryNames() {
        var section = config.getConfigurationSection("categories");
        if (section == null)
            return java.util.Set.of();
        return section.getKeys(false);
    }
}





