package nl.wantedchef.empirewand.api.service;

import nl.wantedchef.empirewand.api.EmpireWandService;

/**
 * Configuration service interface.
 */
public interface ConfigService extends EmpireWandService {
    
    /**
     * Gets a configuration value.
     */
    Object get(String path);
    
    /**
     * Gets a string value.
     */
    String getString(String path, String defaultValue);
    
    /**
     * Gets an integer value.
     */
    int getInt(String path, int defaultValue);
    
    /**
     * Gets a boolean value.
     */
    boolean getBoolean(String path, boolean defaultValue);
    
    /**
     * Gets a double value.
     */
    double getDouble(String path, double defaultValue);
}