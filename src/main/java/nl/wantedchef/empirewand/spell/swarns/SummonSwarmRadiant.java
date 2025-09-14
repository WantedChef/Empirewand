package nl.wantedchef.empirewand.spell.swarns;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.swarns.SummonSwarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SummonSwarmRadiant extends SummonSwarm {
    public static class Builder extends Spell.Builder<Void> {
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Summon Swarm (Radiant)";
            this.description = "Summon a radiant swarm that reveals foes.";
            this.cooldown = java.time.Duration.ofSeconds(70);
            this.spellType = SpellType.DARK;
        }

        @Override
        public @NotNull Spell<Void> build() { return new SummonSwarmRadiant(this); }
    }

    protected SummonSwarmRadiant(Spell.Builder<Void> builder) { super(builder); }

    @Override
    public @NotNull String key() { return "summon-swarm-radiant"; }

    @Override
    public @NotNull PrereqInterface prereq() { return new PrereqInterface.NonePrereq(); }
}
