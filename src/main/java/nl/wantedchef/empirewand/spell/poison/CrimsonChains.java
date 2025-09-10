package nl.wantedchef.empirewand.spell.poison;

import nl.wantedchef.empirewand.spell.Spell;

public class CrimsonChains implements Spell<Void> {
    @Override public String getKey() { return "crimson-chains"; }
    @Override public String getDisplayName() { return "Crimson Chains"; }
    @Override public String getDescription() { return "Poison spell"; }
}
