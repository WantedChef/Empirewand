package com.example.empirewand.api;

import org.jetbrains.annotations.NotNull;

/**
 * Main entry point for the EmpireWand API.
 *
 * <p>This class provides static access to EmpireWand's public services and utilities.
 * External plugins should use this class to obtain service instances.</p>
 *
 * <p><b>API Stability:</b> Experimental - Subject to change in future versions</p>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Get the spell registry
 * SpellRegistry registry = EmpireWandAPI.getSpellRegistry();
 *
 * // Check if a spell exists
 * if (registry.isSpellRegistered("magic-missile")) {
 *     Spell spell = registry.getSpell("magic-missile");
 *     // Use the spell...
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public final class EmpireWandAPI {

    private static EmpireWandProvider provider;

    /**
     * Private constructor to prevent instantiation.
     */
    private EmpireWandAPI() {
        throw new UnsupportedOperationException("EmpireWandAPI cannot be instantiated");
    }

    /**
     * Sets the API provider. This is called internally by EmpireWand.
     *
     * @param provider the provider implementation
     */
    public static void setProvider(@NotNull EmpireWandProvider provider) {
        if (EmpireWandAPI.provider != null) {
            throw new IllegalStateException("Provider already set");
        }
        EmpireWandAPI.provider = provider;
    }

    /**
     * Gets the spell registry service.
     *
     * @return the spell registry
     * @throws IllegalStateException if the API provider is not available
     */
    @NotNull
    public static SpellRegistry getSpellRegistry() {
        ensureProvider();
        return provider.getSpellRegistry();
    }

    /**
     * Gets the permission service.
     *
     * @return the permission service
     * @throws IllegalStateException if the API provider is not available
     */
    @NotNull
    public static PermissionService getPermissionService() {
        ensureProvider();
        return provider.getPermissionService();
    }

    /**
     * Gets the wand service.
     *
     * @return the wand service
     * @throws IllegalStateException if the API provider is not available
     */
    @NotNull
    public static WandService getWandService() {
        ensureProvider();
        return provider.getWandService();
    }

    /**
     * Checks if the EmpireWand API is available.
     *
     * @return true if the API is available, false otherwise
     */
    public static boolean isAvailable() {
        return provider != null;
    }

    /**
     * Ensures that a provider is available.
     *
     * @throws IllegalStateException if no provider is available
     */
    private static void ensureProvider() {
        if (provider == null) {
            throw new IllegalStateException("EmpireWand API provider not available. Make sure EmpireWand is loaded.");
        }
    }

    /**
     * Interface for providing EmpireWand API services.
     * This is implemented internally by EmpireWand.
     */
    public interface EmpireWandProvider {
        @NotNull
        SpellRegistry getSpellRegistry();

        @NotNull
        PermissionService getPermissionService();

        @NotNull
        WandService getWandService();
    }
}