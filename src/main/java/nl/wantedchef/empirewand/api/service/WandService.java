package nl.wantedchef.empirewand.api.service;

import nl.wantedchef.empirewand.api.EmpireWandService;
import java.util.List;

/**
 * Wand service interface.
 */
public interface WandService extends EmpireWandService {
    
    /**
     * Creates a basic wand.
     */
    Object createBasicWand();
    
    /**
     * Checks if an item is a wand.
     */
    boolean isWand(Object item);
    
    /**
     * Gets bound spells on a wand.
     */
    List<String> getBoundSpells(Object wand);
    
    /**
     * Sets spells on a wand.
     */
    void setSpells(Object wand, List<String> spellKeys);
    
    /**
     * Gets the active spell index.
     */
    int getActiveIndex(Object wand);
    
    /**
     * Sets the active spell index.
     */
    void setActiveIndex(Object wand, int index);
}