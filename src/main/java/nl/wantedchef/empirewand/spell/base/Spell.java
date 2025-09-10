package nl.wantedchef.empirewand.spell.base;

/**
 * Base interface for spells in the nl.wantedchef.empirewand package.
 */
public interface Spell {
    
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
    void execute(Object context);
    
    /**
     * Gets the spell type.
     */
    SpellType getType();
}