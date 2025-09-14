package nl.wantedchef.empirewand.spell.swarns;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.swarns.SummonSwarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SummonSwarmVoid extends SummonSwarm {
    public static class Builder extends Spell.Builder<Void> {
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Summon Swarm (Void)";
            this.description = "Summon a void swarm that blinds foes.";
            this.cooldown = java.time.Duration.ofSeconds(70);
            this.spellType = SpellType.DARK;
        }

        @Override
        public @NotNull Spell<Void> build() { return new SummonSwarmVoid(this); }
    }

    protected SummonSwarmVoid(Spell.Builder<Void> builder) { super(builder); }

    @Override
    public @NotNull String key() { return "summon-swarm-void"; }

    @Override
    public @NotNull PrereqInterface prereq() { return new PrereqInterface.NonePrereq(); }
}
