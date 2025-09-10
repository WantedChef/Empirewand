package nl.wantedchef.empirewand.api.service;

/**
 * Wand customizer interface.
 */
public interface WandCustomizer {
    
    /**
     * Sets the wand name.
     */
    WandCustomizer name(String name);
    
    /**
     * Adds a spell to the wand.
     */
    WandCustomizer addSpell(String spellKey);
    
    /**
     * Applies the customizations.
     */
    Object build();
}