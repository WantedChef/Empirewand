package nl.wantedchef.empirewand.api.service;

import nl.wantedchef.empirewand.api.EmpireWandService;

/**
 * Permission service interface.
 */
public interface PermissionService extends EmpireWandService {
    
    /**
     * Checks if a player has a permission.
     */
    boolean hasPermission(Object player, String permission);
    
    /**
     * Checks if a player can use a spell.
     */
    boolean canUseSpell(Object player, String spellKey);
}