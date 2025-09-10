package nl.wantedchef.empirewand.api.spell.toggle;

import nl.wantedchef.empirewand.api.EmpireWandService;
import java.util.Set;

/**
 * Spell manager interface for toggle spells.
 */
public interface SpellManager extends EmpireWandService {
    
    /**
     * Registers a toggleable spell.
     */
    void registerToggleableSpell(ToggleableSpell spell);
    
    /**
     * Gets all active spells for a player.
     */
    Set<String> getActiveSpells(Object player);
    
    /**
     * Toggles a spell for a player.
     */
    boolean toggleSpell(Object player, String spellKey);
    
    /**
     * Checks if a spell is active for a player.
     */
    boolean isSpellActive(Object player, String spellKey);
}