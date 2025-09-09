package nl.wantedchef.empirewand.api;

/**
 * Represents the health status of a service.
 *
 * @since 2.0.0
 */
public enum ServiceHealth {

    /**
     * Service is fully operational and healthy.
     */
    HEALTHY("Service is operating normally"),

    /**
     * Service is operational but with some issues or warnings.
     */
    DEGRADED("Service is operational but with issues"),

    /**
     * Service is not operational.
     */
    UNHEALTHY("Service is not operational"),

    /**
     * Service health status is unknown.
     */
    UNKNOWN("Service health status is unknown");

    private final String description;

    ServiceHealth(@org.jetbrains.annotations.NotNull String description) {
        this.description = description;
    }

    /**
     * Gets a human-readable description of the health status.
     *
     * @return the description
     */
    @org.jetbrains.annotations.NotNull
    public String getDescription() {
        return description;
    }
}





