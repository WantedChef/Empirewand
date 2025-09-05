package com.example.empirewand.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class ConfigService {
    private final Plugin plugin;
    private FileConfiguration config;
    private FileConfiguration spellsConfig;

    public ConfigService(Plugin plugin) {
        this.plugin = plugin;
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

    public long getDefaultCooldown() {
        return config.getLong("cooldowns.default", 500);
    }
}

