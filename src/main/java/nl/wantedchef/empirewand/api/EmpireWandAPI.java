package nl.wantedchef.empirewand.api;

import java.util.Optional;

import nl.wantedchef.empirewand.api.service.CooldownService;
import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.api.service.EffectService;
import nl.wantedchef.empirewand.api.service.MetricsService;
import nl.wantedchef.empirewand.api.service.PermissionService;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.api.spell.SpellManager;
import org.jetbrains.annotations.NotNull;

/**
 * Main entry point for the EmpireWand API.
 *
 * <p>
 * This class provides static access to EmpireWand's public services and
 * utilities. External plugins should use this class to obtain service
 * instances.
 * </p>
 *
 * <p>
 * <b>API Stability:</b> Stable - Follows semantic versioning
 * </p>
 *
 * <p>
 * <b>Usage Example:</b>
 *
 * <pre>{@code
 * // Get services using the enhanced API
 * SpellRegistry spellRegistry = EmpireWandAPI.getService(SpellRegistry.class);
 * WandService wandService = EmpireWandAPI.getService(WandService.class);
 * ConfigService configService = EmpireWandAPI.getService(ConfigService.class);
 *
 * // Check API availability
 * if (EmpireWandAPI.isAvailable()) {
 *     Version apiVersion = EmpireWandAPI.getAPIVersion();
 *     // Use the API...
 * }
 * }</pre>
 *
 * @since 2.0.0
 */
public final class EmpireWandAPI {

    private static volatile EmpireWandProvider provider;
    private static final EmpireWandProvider NO_OP_PROVIDER = new EmpireWandProvider() {
        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull SpellRegistry getSpellRegistry() {
            throw new IllegalStateException("EmpireWand API provider not available (no-op)");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull WandService getWandService() {
            throw new IllegalStateException("EmpireWand API provider not available (no-op)");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull PermissionService getPermissionService() {
            throw new IllegalStateException("EmpireWand API provider not available (no-op)");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull ConfigService getConfigService() {
            throw new IllegalStateException("EmpireWand API provider not available (no-op)");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull CooldownService getCooldownService() {
            throw new IllegalStateException("EmpireWand API provider not available (no-op)");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull EffectService getEffectService() {
            throw new IllegalStateException("EmpireWand API provider not available (no-op)");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull MetricsService getMetricsService() {
            throw new IllegalStateException("EmpireWand API provider not available (no-op)");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull Version getAPIVersion() {
            return Version.of(2, 0, 0);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCompatible(@NotNull Version version) {
            return false;
        }
    };

    // Current API version
    private static final Version API_VERSION = Version.of(2, 0, 0);

    /**
     * Private constructor for legacy instance-based API support.
     *
     * <p>
     * Instances created via {@link #get()} act as a thin wrapper around the
     * static API methods. Do not instantiate directly outside this class.
     * </p>
     */
    private EmpireWandAPI() {
        // Intentionally no-op to support legacy {@code EmpireWandAPI.get()} usage
        // which delegates to static service accessors.
    }

    /**
     * Sets the API provider. This is called internally by EmpireWand.
     *
     * @param provider the provider implementation
     * @throws IllegalStateException if a provider is already set
     */
    public static void setProvider(@NotNull EmpireWandProvider provider) {
        if (EmpireWandAPI.provider != null && EmpireWandAPI.provider != NO_OP_PROVIDER) {
            throw new IllegalStateException("Provider already set");
        }
        EmpireWandAPI.provider = provider;
    }

    /**
     * Clears (resets) the provider back to a guarded no-op implementation.
     * Subsequent API calls will throw the same IllegalStateException but we avoid
     * null and
     * related NPE warnings.
     */
    public static void clearProvider() {
        EmpireWandAPI.provider = NO_OP_PROVIDER;
    }

    /**
     * Gets a service instance by its class type.
     *
     * @param serviceClass the service class
     * @param <T>          the service type
     * @return the service instance
     * @throws IllegalStateException    if the API provider is not available
     * @throws IllegalArgumentException if the service type is not supported
     */
    @NotNull
    public static <T extends EmpireWandService> T getService(@NotNull Class<T> serviceClass) {
        ensureProvider();

        if (SpellRegistry.class.equals(serviceClass)) {
            return serviceClass.cast(provider.getSpellRegistry());
        } else if (WandService.class.equals(serviceClass)) {
            return serviceClass.cast(provider.getWandService());
        } else if (PermissionService.class.equals(serviceClass)) {
            return serviceClass.cast(provider.getPermissionService());
        } else if (ConfigService.class.equals(serviceClass)) {
            return serviceClass.cast(provider.getConfigService());
        } else if (CooldownService.class.equals(serviceClass)) {
            return serviceClass.cast(provider.getCooldownService());
        } else if (EffectService.class.equals(serviceClass)) {
            return serviceClass.cast(provider.getEffectService());
        } else if (MetricsService.class.equals(serviceClass)) {
            return serviceClass.cast(provider.getMetricsService());
        }

        throw new IllegalArgumentException("Unsupported service type: " + serviceClass.getName());
    }

    /**
     * Gets a service instance by its class type, returning an empty Optional if not
     * available.
     *
     * @param serviceClass the service class
     * @param <T>          the service type
     * @return an Optional containing the service instance, or empty if not
     *         available
     */
    @NotNull
    public static <T extends EmpireWandService> Optional<T> getServiceOptional(@NotNull Class<T> serviceClass) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            return Optional.of(getService(serviceClass));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the spell registry service.
     *
     * @return the spell registry
     * @throws IllegalStateException if the API provider is not available
     * @deprecated Use {@link #getService(Class)} instead
     */
    @Deprecated(forRemoval = true)
    @NotNull
    public static SpellRegistry getSpellRegistry() {
        return getService(SpellRegistry.class);
    }

    /**
     * Gets the wand service.
     *
     * @return the wand service
     * @throws IllegalStateException if the API provider is not available
     * @deprecated Use {@link #getService(Class)} instead
     */
    @Deprecated(forRemoval = true)
    @NotNull
    public static WandService getWandService() {
        return getService(WandService.class);
    }

    /**
     * Gets the permission service.
     *
     * @return the permission service
     * @throws IllegalStateException if the API provider is not available
     * @deprecated Use {@link #getService(Class)} instead
     */
    @Deprecated(forRemoval = true)
    @NotNull
    public static PermissionService getPermissionService() {
        return getService(PermissionService.class);
    }

    /**
     * Gets the API version.
     *
     * @return the API version
     */
    @NotNull
    public static Version getAPIVersion() {
        return API_VERSION;
    }

    /**
     * Checks if the EmpireWand API is available.
     *
     * @return true if the API is available, false otherwise
     */
    public static boolean isAvailable() {
        return provider != null && provider != NO_OP_PROVIDER;
    }

    /**
     * Checks if a given version is compatible with this API.
     *
     * @param version the version to check
     * @return true if compatible, false otherwise
     */
    public static boolean isCompatible(@NotNull Version version) {
        if (!isAvailable()) {
            return false;
        }
        return provider.isCompatible(version);
    }

    /**
     * Ensures that a provider is available.
     *
     * @throws IllegalStateException if no provider is available
     */
    private static void ensureProvider() {
        if (provider == null || provider == NO_OP_PROVIDER) {
            throw new IllegalStateException("EmpireWand API provider not available. Make sure EmpireWand is loaded.");
        }
    }

    /**
     * Enhanced interface for providing EmpireWand API services.
     * This is implemented internally by EmpireWand.
     *
     * @since 2.0.0
     */
    public interface EmpireWandProvider {
        /**
         * Retrieves the central registry for all available spells.
         * <p>
         * The {@link SpellRegistry} allows you to look up spells by their unique key,
         * get a collection of all registered spells, and check for their existence.
         * It is the primary way to interact with the spell system.
         *
         * @return The non-null {@link SpellRegistry} instance.
         */
        @NotNull
        SpellRegistry getSpellRegistry();

        /**
         * Retrieves the service responsible for managing wands.
         * <p>
         * The {@link WandService} provides methods to create, identify, and manipulate
         * magic wands. Use this service to check if an item is a wand, get the spell
         * bound to a wand, or create new wands from templates.
         *
         * @return The non-null {@link WandService} instance.
         */
        @NotNull
        WandService getWandService();

        /**
         * Retrieves the service for handling permissions.
         * <p>
         * The {@link PermissionService} is a utility for checking if a player has
         * the required permissions to use a specific spell or feature of the plugin.
         *
         * @return The non-null {@link PermissionService} instance.
         */
        @NotNull
        PermissionService getPermissionService();

        /**
         * Retrieves the service for accessing plugin configurations.
         * <p>
         * The {@link ConfigService} provides access to `config.yml`, `spells.yml`,
         * and other configuration files. It allows reading values that control
         * plugin behavior and spell properties.
         *
         * @return The non-null {@link ConfigService} instance.
         */
        @NotNull
        ConfigService getConfigService();

        /**
         * Retrieves the service for managing spell cooldowns.
         * <p>
         * The {@link CooldownService} tracks cooldowns for players and spells,
         * preventing spam. You can use it to check, apply, or clear cooldowns.
         *
         * @return The non-null {@link CooldownService} instance.
         */
        @NotNull
        CooldownService getCooldownService();

        /**
         * Retrieves the service for creating visual and audio effects.
         * <p>
         * The {@link EffectService} (often referred to as FxService) is a helper
         * for spawning particles, playing sounds, and creating other visual effects
         * at specific locations, abstracting away the complexities of the Bukkit API.
         *
         * @return The non-null {@link EffectService} instance.
         */
        @NotNull
        EffectService getEffectService();

        /**
         * Retrieves the service for collecting plugin metrics.
         * <p>
         * The {@link MetricsService} handles the collection of anonymous usage data
         * via bStats, providing insights into how the plugin is used.
         *
         * @return The non-null {@link MetricsService} instance.
         */
        @NotNull
        MetricsService getMetricsService();

        /**
         * Gets the API version supported by this provider.
         *
         * @return the API version
         */
        @NotNull
        Version getAPIVersion();

        /**
         * Checks if the provider is compatible with the given API version.
         *
         * @param version the API version to check
         * @return true if compatible, false otherwise
         */
        boolean isCompatible(@NotNull Version version);
    }

    // Legacy instance-based API support
    private static class Holder {
        static final EmpireWandAPI INSTANCE = new EmpireWandAPI();
    }

    /**
     * Gets the singleton instance of EmpireWandAPI.
     * 
     * @return EmpireWandAPI instance
     * @deprecated Use static service methods instead
     */
    @Deprecated(forRemoval = true)
    @NotNull
    public static EmpireWandAPI get() {
        return Holder.INSTANCE;
    }

    /**
     * Legacy instance method for getting config service.
     * 
     * @return ConfigService instance
     * @deprecated Use static getService(ConfigService.class) instead
     */
    @Deprecated(forRemoval = true)
    @NotNull
    public ConfigService getConfigService() {
        return getService(ConfigService.class);
    }

    /**
     * Legacy instance method for getting main config.
     * 
     * @return ReadableConfig instance for main config
     * @deprecated Use getConfigService().getMainConfig() instead
     */
    @Deprecated(forRemoval = true)
    @NotNull
    public ConfigService.ReadableConfig getConfig() {
        return getConfigService().getMainConfig();
    }

    /**
     * Legacy instance method for getting FX service.
     * 
     * @return EffectService instance
     * @deprecated Use static getService(EffectService.class) instead
     */
    @Deprecated(forRemoval = true)
    @NotNull
    public EffectService getFxService() {
        return getService(EffectService.class);
    }
}
