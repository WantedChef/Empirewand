package nl.wantedchef.empirewand.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Core implementation of ConfigService.
 */
public class ConfigServiceImpl implements nl.wantedchef.empirewand.api.service.ConfigService {
    private final Plugin plugin;
    private FileConfiguration config;

    public ConfigServiceImpl(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Override
    @Nullable
    public Object get(@NotNull String path) {
        return config.get(path);
    }

    @Override
    @NotNull
    public String getString(@NotNull String path, @NotNull String defaultValue) {
        return config.getString(path, defaultValue);
    }

    @Override
    public int getInt(@NotNull String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    @Override
    public double getDouble(@NotNull String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    @Override
    public void set(@NotNull String path, @Nullable Object value) {
        config.set(path, value);
    }

    @Override
    public void save() {
        plugin.saveConfig();
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    /**
     * Gets the spells configuration section.
     */
    public Object getSpellsConfig() {
        return config.get("spells");
    }
}
