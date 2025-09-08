package com.example.empirewand.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple delegating wrapper that only exposes read methods we allow.
 */
public final class ReadOnlyConfig implements ReadableConfig {
    private final FileConfiguration delegate;

    public ReadOnlyConfig(@NotNull FileConfiguration delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate cannot be null");
        }
        this.delegate = delegate;
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        if (path.trim().isEmpty()) {
            return def;
        }
        try {
            return delegate.getBoolean(path, def);
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        if (path.trim().isEmpty()) {
            return def;
        }
        try {
            return delegate.getInt(path, def);
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    public long getLong(@NotNull String path, long def) {
        if (path.trim().isEmpty()) {
            return def;
        }
        try {
            return delegate.getLong(path, def);
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        if (path.trim().isEmpty()) {
            return def;
        }
        try {
            return delegate.getDouble(path, def);
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    public @Nullable String getString(@NotNull String path, @Nullable String def) {
        if (path.trim().isEmpty()) {
            return def;
        }
        try {
            String value = delegate.getString(path);
            return value == null ? def : value;
        } catch (Exception e) {
            return def;
        }
    }
}