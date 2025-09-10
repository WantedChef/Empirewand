package nl.wantedchef.empirewand.api.service;

import java.util.List;

/**
 * Wand template interface.
 */
public interface WandTemplate {
    
    /**
     * Gets the template name.
     */
    String getName();
    
    /**
     * Gets the template description.
     */
    String getDescription();
    
    /**
     * Gets the default spells.
     */
    List<String> getDefaultSpells();
    
    /**
     * Gets the material type.
     */
    String getMaterial();
}