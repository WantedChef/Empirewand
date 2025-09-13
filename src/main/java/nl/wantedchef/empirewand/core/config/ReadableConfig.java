package nl.wantedchef.empirewand.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Read-only view over a Bukkit FileConfiguration to avoid exposing mutable
 * internals.
 */
public interface ReadableConfig {
    boolean getBoolean(@NotNull String path, boolean def);

    int getInt(@NotNull String path, int def);

    long getLong(@NotNull String path, long def);

    double getDouble(@NotNull String path, double def);

    @Nullable
    String getString(@NotNull String path, @Nullable String def);

    @Nullable
    ReadableConfig getConfigurationSection(@NotNull String path);
    
    /**
     * Gets a value at the specified path as an Object.
     * 
     * @param path The path to get the value from
     * @return The value at the path, or null if not found
     */
    @Nullable
    Object get(@NotNull String path);
    
    /**
     * Gets a value at the specified path as an Object with a default value.
     * 
     * @param path The path to get the value from
     * @param def The default value to return if not found
     * @return The value at the path, or the default value if not found
     */
    @Nullable
    Object get(@NotNull String path, @Nullable Object def);
}





