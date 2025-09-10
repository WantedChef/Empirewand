package nl.wantedchef.empirewand.spell;

/**
 * Spell context for execution.
 */
public class SpellContext {
    private final Object player;
    private final Object target;
    private final Object location;
    private final String spellKey;
    
    public SpellContext(Object player, Object target, Object location, String spellKey) {
        this.player = player;
        this.target = target;
        this.location = location;
        this.spellKey = spellKey;
    }
    
    public Object getPlayer() {
        return player;
    }
    
    public Object getTarget() {
        return target;
    }
    
    public Object getLocation() {
        return location;
    }
    
    public String getSpellKey() {
        return spellKey;
    }
}