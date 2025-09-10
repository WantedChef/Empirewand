package nl.wantedchef.empirewand.spell.poison;

import nl.wantedchef.empirewand.spell.Spell;

public class PoisonWave implements Spell<Void> {
    @Override public String getKey() { return "poison-wave"; }
    @Override public String getDisplayName() { return "Poison Wave"; }
    @Override public String getDescription() { return "Poison area spell"; }
}
