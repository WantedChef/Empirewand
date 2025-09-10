package nl.wantedchef.empirewand.api.spell.toggle;

/**
 * Interface for spells that can be toggled on/off.
 */
public interface ToggleableSpell {
    
    /**
     * Gets the spell key.
     */
    String getKey();
    
    /**
     * Called when the spell is activated.
     */
    void onActivate(Object player);
    
    /**
     * Called when the spell is deactivated.
     */
    void onDeactivate(Object player);
    
    /**
     * Called on each tick while the spell is active.
     */
    void onTick(Object player);
    
    /**
     * Checks if the spell can be toggled for the player.
     */
    boolean canToggle(Object player);
}