package nl.wantedchef.empirewand.api.service;

import nl.wantedchef.empirewand.api.EmpireWandService;
import org.jetbrains.annotations.NotNull;

/**
 * Service for managing EmpireWand configuration.
 * Provides access to configuration files and dynamic configuration management.
 *
 * @since 2.0.0
 */
public interface ConfigService extends EmpireWandService {

    /**
     * Gets the main configuration.
     *
     * @return the main configuration
     */
    @NotNull
    ReadableConfig getMainConfig();

    /**
     * Gets the spells configuration.
     *
     * @return the spells configuration
     */
    @NotNull
    ReadableConfig getSpellsConfig();

    /**
     * Gets the wands configuration.
     *
     * @return the wands configuration
     */
    @NotNull
    ReadableConfig getWandsConfig();

    /**
     * Configuration interface for reading values.
     *
     * @since 2.0.0
     */
    interface ReadableConfig {
        /**
         * Gets a boolean value from the configuration.
         *
         * @param path the configuration path
         * @param def  the default value if the path is not found
         * @return the boolean value
         */
        boolean getBoolean(@NotNull String path, boolean def);

        /**
         * Gets an integer value from the configuration.
         *
         * @param path the configuration path
         * @param def  the default value if the path is not found
         * @return the integer value
         */
        int getInt(@NotNull String path, int def);

        /**
         * Gets a long value from the configuration.
         *
         * @param path the configuration path
         * @param def  the default value if the path is not found
         * @return the long value
         */
        long getLong(@NotNull String path, long def);

        /**
         * Gets a double value from the configuration.
         *
         * @param path the configuration path
         * @param def  the default value if the path is not found
         * @return the double value
         */
        double getDouble(@NotNull String path, double def);

        /**
         * Gets a string value from the configuration.
         *
         * @param path the configuration path
         * @param def  the default value if the path is not found
         * @return the string value, or null if not found and no default provided
         */
        @org.jetbrains.annotations.Nullable
        String getString(@NotNull String path, @org.jetbrains.annotations.Nullable String def);
    }
}
