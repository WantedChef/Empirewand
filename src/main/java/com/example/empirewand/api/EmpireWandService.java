package com.example.empirewand.api;

import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all EmpireWand services.
 * Provides common functionality and metadata for service management.
 *
 * @since 2.0.0
 */
public interface EmpireWandService {

    /**
     * Gets the service name for identification.
     *
     * @return the service name
     */
    @NotNull
    String getServiceName();

    /**
     * Gets the service version.
     *
     * @return the service version
     */
    @NotNull
    Version getServiceVersion();

    /**
     * Checks if the service is enabled and operational.
     *
     * @return true if the service is enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Gets the service health status.
     *
     * @return the service health
     */
    @NotNull
    ServiceHealth getHealth();

    /**
     * Reloads the service configuration.
     * This method should be called when configuration changes are detected.
     */
    void reload();
}