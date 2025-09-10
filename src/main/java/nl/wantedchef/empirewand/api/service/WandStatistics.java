package nl.wantedchef.empirewand.api.service;

/**
 * Wand statistics interface.
 */
public interface WandStatistics {
    
    /**
     * Gets the total spells cast.
     */
    int getTotalSpellsCast();
    
    /**
     * Gets the total damage dealt.
     */
    double getTotalDamageDealt();
    
    /**
     * Gets the total mana used.
     */
    double getTotalManaUsed();
}