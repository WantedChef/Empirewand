package nl.wantedchef.empirewand.api.spell;

import java.util.Set;

/**
 * Spell metadata interface.
 */
public interface SpellMetadata {
    
    /**
     * Gets the spell key.
     */
    String getKey();
    
    /**
     * Gets the display name.
     */
    String getDisplayName();
    
    /**
     * Gets the description.
     */
    String getDescription();
    
    /**
     * Gets the category.
     */
    String getCategory();
    
    /**
     * Gets the tags.
     */
    Set<String> getTags();
    
    /**
     * Gets the cooldown in ticks.
     */
    long getCooldownTicks();
    
    /**
     * Gets the range.
     */
    double getRange();
    
    /**
     * Gets the level requirement.
     */
    int getLevelRequirement();
    
    /**
     * Checks if the spell is enabled.
     */
    boolean isEnabled();
}