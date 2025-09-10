package nl.wantedchef.empirewand.spell;

import nl.wantedchef.empirewand.spell.base.SpellType;

/**
 * Spell interface with generic support.
 */
public interface Spell<T> {
    
    /**
     * Gets the spell key/name.
     */
    String getKey();
    
    /**
     * Gets the display name.
     */
    String getDisplayName();
    
    /**
     * Gets the spell description.
     */
    String getDescription();
    
    /**
     * Executes the spell.
     */
    CastResult execute(SpellContext context);
    
    /**
     * Gets the spell type.
     */
    SpellType getType();
}