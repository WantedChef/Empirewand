package nl.wantedchef.empirewand.api;

/**
 * Base interface for all EmpireWand services.
 */
public interface EmpireWandService {
    
    /**
     * Gets the service name.
     */
    String getServiceName();
    
    /**
     * Gets the service version.
     */
    Version getServiceVersion();
    
    /**
     * Checks if the service is enabled.
     */
    boolean isEnabled();
    
    /**
     * Gets the service health status.
     */
    ServiceHealth getHealth();
    
    /**
     * Reloads the service configuration.
     */
    void reload();
}