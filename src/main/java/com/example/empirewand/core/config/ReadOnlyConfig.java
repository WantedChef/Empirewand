package com.example.empirewand.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Simple delegating wrapper that only exposes read methods we allow.
 */
public final class ReadOnlyConfig implements ReadableConfig {
    private final FileConfiguration delegate;

    public ReadOnlyConfig(@NotNull FileConfiguration delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        return delegate.getBoolean(path, def);
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        return delegate.getInt(path, def);
    }

    @Override
    public long getLong(@NotNull String path, long def) {
        return delegate.getLong(path, def);
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        return delegate.getDouble(path, def);
    }

    @Override
    public @Nullable String getString(@NotNull String path, @Nullable String def) {
        String value = delegate.getString(path);
        return value == null ? def : value;
    }
}
