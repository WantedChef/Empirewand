package nl.wantedchef.empirewand.spell.swarns;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.swarns.SummonSwarm;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Vex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SummonSwarmFlame extends SummonSwarm {
    public static class Builder extends Spell.Builder<Void> {
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Summon Swarm (Flame)";
            this.description = "Summon a flame-themed swarm that ignites foes.";
            this.cooldown = java.time.Duration.ofSeconds(70);
            this.spellType = SpellType.DARK;
        }

        @Override
        public @NotNull Spell<Void> build() { return new SummonSwarmFlame(this); }
    }

    protected SummonSwarmFlame(Spell.Builder<Void> builder) { super(builder); }

    @Override
    public @NotNull String key() { return "summon-swarm-flame"; }

    @Override
    public @NotNull PrereqInterface prereq() { return new PrereqInterface.NonePrereq(); }

    @Override
    protected SummonSwarm.Variant getVariant(@NotNull SpellContext context) { return SummonSwarm.Variant.FLAME; }

    @Override
    protected void onMinionSpawn(@NotNull Vex minion, @NotNull SpellContext context) {
        var w = minion.getWorld();
        w.spawnParticle(Particle.FLAME, minion.getLocation().add(0, 0.5, 0), 12, 0.3, 0.3, 0.3, 0.02);
        context.fx().playSound(minion.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.6f, 1.2f);
    }

    @Override
    protected void onMinionTick(@NotNull Vex minion, int ticks, @NotNull SpellContext context) {
        if (ticks % 5 == 0) {
            var w = minion.getWorld();
            w.spawnParticle(Particle.FLAME, minion.getLocation().add(0, 0.6, 0), 3, 0.12, 0.12, 0.12, 0.01);
            w.spawnParticle(Particle.SMALL_FLAME, minion.getLocation().add(0, 0.4, 0), 2, 0.08, 0.08, 0.08, 0.01);
        }
    }

    @Override
    protected void onMinionDismiss(@NotNull Vex minion, @NotNull SpellContext context) {
        var w = minion.getWorld();
        w.spawnParticle(Particle.FLAME, minion.getLocation(), 20, 0.5, 0.5, 0.5, 0.03);
        w.spawnParticle(Particle.SMALL_FLAME, minion.getLocation(), 10, 0.4, 0.4, 0.4, 0.02);
        context.fx().playSound(minion.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.4f);
    }
}
