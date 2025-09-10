package nl.wantedchef.empirewand.spell.poison;

import nl.wantedchef.empirewand.spell.Spell;

public class SoulSever implements Spell<Void> {
    @Override public String getKey() { return "soul-sever"; }
    @Override public String getDisplayName() { return "Soul Sever"; }
    @Override public String getDescription() { return "Soul damage spell"; }
}
