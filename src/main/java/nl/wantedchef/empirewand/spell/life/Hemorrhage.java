package nl.wantedchef.empirewand.spell.life;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class Hemorrhage extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Hemorrhage";
            this.description = "Causes the target to bleed, taking extra damage when they move.";
            this.cooldown = java.time.Duration.ofSeconds(18);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Hemorrhage(this);
        }
    }

    private Hemorrhage(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "hemorrhage";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double baseDamage = spellConfig.getDouble("values.base-damage", 2.0);
        boolean hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);

        Entity targetEntity = player.getTargetEntity(20);
        if (!(targetEntity instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return null;
        }

        if ((target instanceof Player && !hitPlayers) || (!(target instanceof Player) && !hitMobs)) {
            context.fx().fizzle(player);
            return null;
        }

        target.damage(baseDamage, player);
        new HemorrhageTask(target, player, context).runTaskTimer(context.plugin(), 0L,
                spellConfig.getInt("values.check-interval-ticks", 10));
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private class HemorrhageTask extends BukkitRunnable {
        private final LivingEntity target;
        private final Player caster;
        private final double movementBonus;
        private final double movementThreshold;
        private final int totalDuration;
        private Location lastLocation;
        private int ticksElapsed = 0;

        public HemorrhageTask(LivingEntity target, Player caster, SpellContext context) {
            this.target = target;
            this.caster = caster;
            this.movementBonus = spellConfig.getDouble("values.movement-bonus", 0.5);
            this.movementThreshold = spellConfig.getDouble("values.movement-threshold", 0.8);
            this.totalDuration = spellConfig.getInt("values.duration-ticks", 120);
            this.lastLocation = target.getLocation().clone();
        }

        @Override
        public void run() {
            if (!target.isValid() || target.isDead() || !caster.isValid() || ticksElapsed >= totalDuration) {
                this.cancel();
                return;
            }

            ticksElapsed += 10; // Assuming interval of 10

            if (lastLocation.distanceSquared(target.getLocation()) > movementThreshold * movementThreshold) {
                target.damage(movementBonus, caster);
                spawnBloodParticles(target);
            }

            lastLocation = target.getLocation().clone();
        }

        private void spawnBloodParticles(LivingEntity target) {
            target.getWorld().spawnParticle(Particle.FALLING_LAVA, target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3,
                    0);
        }
    }
}
