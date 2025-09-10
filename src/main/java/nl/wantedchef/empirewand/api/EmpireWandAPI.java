package nl.wantedchef.empirewand.api;

import nl.wantedchef.empirewand.api.service.*;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;

/**
 * Main API entry point for nl.wantedchef.empirewand.
 */
public final class EmpireWandAPI {
    
    private static volatile EmpireWandProvider provider;
    
    /**
     * Gets the spell registry service.
     */
    public static SpellRegistry getSpellRegistry() {
        if (provider == null) {
            throw new IllegalStateException("EmpireWand API provider not available");
        }
        return provider.getSpellRegistry();
    }
    
    /**
     * Gets the wand service.
     */
    public static WandService getWandService() {
        if (provider == null) {
            throw new IllegalStateException("EmpireWand API provider not available");
        }
        return provider.getWandService();
    }
    
    /**
     * Gets the permission service.
     */
    public static PermissionService getPermissionService() {
        if (provider == null) {
            throw new IllegalStateException("EmpireWand API provider not available");
        }
        return provider.getPermissionService();
    }
    
    /**
     * Gets the config service.
     */
    public static ConfigService getConfigService() {
        if (provider == null) {
            throw new IllegalStateException("EmpireWand API provider not available");
        }
        return provider.getConfigService();
    }
    
    /**
     * Gets the effect service.
     */
    public static EffectService getEffectService() {
        if (provider == null) {
            throw new IllegalStateException("EmpireWand API provider not available");
        }
        return provider.getEffectService();
    }
    
    /**
     * Sets the API provider.
     */
    public static void setProvider(EmpireWandProvider provider) {
        EmpireWandAPI.provider = provider;
    }
    
    /**
     * Provider interface for services.
     */
    public interface EmpireWandProvider {
        SpellRegistry getSpellRegistry();
        WandService getWandService();
        PermissionService getPermissionService();
        ConfigService getConfigService();
        EffectService getEffectService();
    }
}