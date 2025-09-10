package nl.wantedchef.empirewand.api;

/**
 * Service health status enumeration.
 */
public enum ServiceHealth {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    INITIALIZING,
    SHUTDOWN
}