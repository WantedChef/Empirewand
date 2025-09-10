package nl.wantedchef.empirewand.api.service;

import nl.wantedchef.empirewand.api.EmpireWandService;

/**
 * Effect service interface for visual and audio effects.
 */
public interface EffectService extends EmpireWandService {
    
    /**
     * Plays a sound effect.
     */
    void playSound(Object location, String sound, float volume, float pitch);
    
    /**
     * Shows a particle effect.
     */
    void showParticles(Object location, String particle, int count);
    
    /**
     * Shows an action bar message.
     */
    void showActionBar(Object player, String message);
}