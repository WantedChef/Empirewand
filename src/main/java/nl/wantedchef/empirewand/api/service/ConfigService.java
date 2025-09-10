package nl.wantedchef.empirewand.api.service;

import org.jetbrains.annotations.NotNull;

public interface ConfigService {
    Object get(@NotNull String path);
    Object get(@NotNull String path, Object defaultValue);
    String getString(@NotNull String path);
    String getString(@NotNull String path, String defaultValue);
    int getInt(@NotNull String path);
    int getInt(@NotNull String path, int defaultValue);
    double getDouble(@NotNull String path);
    double getDouble(@NotNull String path, double defaultValue);
    boolean getBoolean(@NotNull String path);
    boolean getBoolean(@NotNull String path, boolean defaultValue);
    long getLong(@NotNull String path);
    long getLong(@NotNull String path, long defaultValue);
}
