package nl.wantedchef.empirewand.spell.swarns;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SummonSwarmStone extends SummonSwarm {
    public static class Builder extends Spell.Builder<Void> {
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Summon Swarm (Stone)";
            this.description = "Summon a stone swarm that hinders foes.";
            this.cooldown = java.time.Duration.ofSeconds(70);
            this.spellType = SpellType.DARK;
        }

        @Override
        public @NotNull Spell<Void> build() { return new SummonSwarmStone(this); }
    }

    protected SummonSwarmStone(Spell.Builder<Void> builder) { super(builder); }

    @Override
    public @NotNull String key() { return "summon-swarm-stone"; }

    @Override
    public @NotNull PrereqInterface prereq() { return new PrereqInterface.NonePrereq(); }
}
