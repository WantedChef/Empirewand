package com.example.empirewand.api.impl;

import com.example.empirewand.api.ConfigService;
import com.example.empirewand.api.ServiceHealth;
import com.example.empirewand.api.Version;
import org.jetbrains.annotations.NotNull;

/**
 * Adapter for ConfigService that wraps core ConfigService implementation.
 * Delegates all config operations to core while providing API contract.
 * Implements EmpireWandService base methods with defaults.
 *
 * <h2>Usage Example:</h2>
 * 
 * <pre>{@code
 * // Get the config service from the API
 * ConfigService config = EmpireWandAPI.getProvider().getConfigService();
 *
 * // Get different configuration sections
 * ReadableConfig mainConfig = config.getMainConfig();
 * ReadableConfig spellsConfig = config.getSpellsConfig();
 * ReadableConfig wandsConfig = config.getWandsConfig();
 *
 * // Read configuration values
 * boolean debugEnabled = mainConfig.getBoolean("debug", false);
 * int defaultCooldown = mainConfig.getInt("cooldowns.default", 100);
 * // Note: cost system removed; no related config keys exist
 * double spellRange = mainConfig.getDouble("spells.range", 20.0);
 * String welcomeMessage = mainConfig.getString("messages.welcome", "Welcome!");
 *
 * // Reload configuration
 * config.reload();
 * }</pre>
 *
 * @since 2.0.0
 */
public class ConfigServiceAdapter implements ConfigService {

    private final com.example.empirewand.core.services.ConfigService core;

    /**
     * Constructor.
     *
     * @param core the core ConfigService to wrap
     */
    public ConfigServiceAdapter(com.example.empirewand.core.services.ConfigService core) {
        if (core == null) {
            throw new IllegalArgumentException("core ConfigService cannot be null");
        }
        this.core = core;
    }

    // EmpireWandService implementations

    @Override
    public @NotNull String getServiceName() {
        return "ConfigService";
    }

    @Override
    public @NotNull Version getServiceVersion() {
        return Version.of(2, 0, 0);
    }

    @Override
    public boolean isEnabled() {
        return true; // Assume enabled if core is injected
    }

    @Override
    public @NotNull ServiceHealth getHealth() {
        try {
            // Check if core service is available
            if (core == null) {
                return ServiceHealth.UNHEALTHY;
            }

            // Check if configurations are loaded
            if (core.getConfig() == null) {
                return ServiceHealth.UNHEALTHY;
            }

            return ServiceHealth.HEALTHY;
        } catch (Exception e) {
            return ServiceHealth.UNHEALTHY;
        }
    }

    @Override
    public void reload() {
        try {
            // Reload core configurations
            if (core != null) {
                core.loadConfigs();
            }
        } catch (Exception e) {
            // Log error but don't propagate - graceful degradation
            System.err.println("Failed to reload config service: " + e.getMessage());
        }
    }

    // ConfigService implementations

    @Override
    public @NotNull ReadableConfig getMainConfig() {
        // Wrap core's read-only view with API ReadableConfig
        return new ReadableConfigAdapter(core.getConfig());
    }

    @Override
    public @NotNull ReadableConfig getSpellsConfig() {
        // Wrap core's read-only view with API ReadableConfig
        return new ReadableConfigAdapter(core.getSpellsConfig());
    }

    @Override
    public @NotNull ReadableConfig getWandsConfig() {
        // Wands config might be part of main config or separate; assume main covers it
        return new ReadableConfigAdapter(core.getConfig());
    }

    /**
     * Adapter for ReadableConfig that wraps core ReadOnlyConfig.
     */
    private static class ReadableConfigAdapter implements ReadableConfig {
        private final com.example.empirewand.core.config.ReadableConfig coreConfig;

        ReadableConfigAdapter(com.example.empirewand.core.config.ReadableConfig coreConfig) {
            this.coreConfig = coreConfig;
        }

        @Override
        public boolean getBoolean(@NotNull String path, boolean def) {
            return coreConfig.getBoolean(path, def);
        }

        @Override
        public int getInt(@NotNull String path, int def) {
            return coreConfig.getInt(path, def);
        }

        @Override
        public long getLong(@NotNull String path, long def) {
            return coreConfig.getLong(path, def);
        }

        @Override
        public double getDouble(@NotNull String path, double def) {
            return coreConfig.getDouble(path, def);
        }

        @Override
        public @org.jetbrains.annotations.Nullable String getString(@NotNull String path,
                @org.jetbrains.annotations.Nullable String def) {
            return coreConfig.getString(path, def);
        }
    }

    // Additional API-specific methods can be added here if needed beyond core

}