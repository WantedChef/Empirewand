package nl.wantedchef.empirewand.spell.control;

import nl.wantedchef.empirewand.spell.base.Spell;
import nl.wantedchef.empirewand.spell.base.SpellContext;
import nl.wantedchef.empirewand.spell.base.CastResult;
import nl.wantedchef.empirewand.spell.base.PrereqInterface;

public class Polymorph extends Spell<Void> {
    
    public Polymorph() {
        super("polymorph", "Polymorph spell", 10000);
    }
    
    @Override
    public String key() {
        return "polymorph";
    }
    
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }
    
    @Override
    protected CastResult<Void> cast(SpellContext context) {
        return CastResult.success(null);
    }
}
