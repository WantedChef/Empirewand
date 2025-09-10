package nl.wantedchef.empirewand.api.spell;

import nl.wantedchef.empirewand.api.EmpireWandService;
import nl.wantedchef.empirewand.spell.base.Spell;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spell registry interface.
 */
public interface SpellRegistry extends EmpireWandService {
    
    /**
     * Gets a spell by key.
     */
    Optional<Spell> getSpell(String key);
    
    /**
     * Gets all registered spells.
     */
    Set<String> getSpellKeys();
    
    /**
     * Registers a spell.
     */
    boolean registerSpell(Spell spell);
    
    /**
     * Unregisters a spell.
     */
    boolean unregisterSpell(String key);
    
    /**
     * Creates a spell query.
     */
    SpellQuery createQuery();
    
    /**
     * Spell query interface.
     */
    interface SpellQuery {
        List<Spell> execute();
        
        interface Builder {
            Builder category(String category);
            Builder tag(String tag);
            SpellQuery build();
        }
    }
}