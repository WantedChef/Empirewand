package com.example.empirewand.core.config;

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
}
