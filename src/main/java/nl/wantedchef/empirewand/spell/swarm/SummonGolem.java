package nl.wantedchef.empirewand.spell.swarm;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

public class SummonGolem extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Summon Golem";
            this.description = "Summons an iron golem guardian to protect you.";
            this.cooldown = Duration.ofSeconds(75);
            this.spellType = SpellType.EARTH;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new SummonGolem(this);
        }
    }

    private SummonGolem(Builder builder) { super(builder); }

    @Override
    public @NotNull String key() { return "summon-golem"; }

    @Override
    public @NotNull PrereqInterface prereq() { return new PrereqInterface.NonePrereq(); }

    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Player caster = context.caster();
        World world = caster.getWorld();
        if (world == null) return null;

        int durationTicks = spellConfig.getInt("values.duration-ticks", 20 * 25); // 25s
        double followRadius = spellConfig.getDouble("values.follow-radius", 6.0);

        world.playSound(caster.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.2f, 0.8f);
        context.plugin().getTaskManager().runTaskTimer(new GolemTask(context, durationTicks, followRadius), 0L, 5L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) { }

    private static final class GolemTask extends BukkitRunnable {
        private final SpellContext context;
        private final int maxSteps;
        private final double followRadius;
        private IronGolem golem;
        private int steps = 0;

        GolemTask(SpellContext context, int durationTicks, double followRadius) {
            this.context = Objects.requireNonNull(context);
            this.maxSteps = Math.max(1, durationTicks / 5);
            this.followRadius = followRadius;
        }

        @Override
        public void run() {
            World world = context.caster().getWorld();
            if (world == null) { cancel(); return; }
            if (steps == 0) spawn(world);
            if (steps >= maxSteps) { dismiss(); cancel(); return; }

            updateBehavior();
            effects();
            steps++;
        }

        private void spawn(World world) {
            Location base = context.caster().getLocation();
            if (base == null) return;
            golem = world.spawn(base, IronGolem.class, g -> {
                g.setPlayerCreated(true);
                g.setHealth(Math.min(100.0, Math.max(20.0, context.caster().getHealth() + 20.0)));
            });
            world.spawnParticle(Particle.CLOUD, base, 25, 0.6, 0.2, 0.6, 0.02);
            world.playSound(base, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
        }

        private void updateBehavior() {
            if (golem == null || !golem.isValid() || golem.isDead()) return;
            Player caster = context.caster();
            boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class)
                    .getMainConfig().getBoolean("features.friendly-fire", false);

            LivingEntity target = nearestEnemy(golem, friendlyFire);
            if (target != null) {
                golem.setTarget(target);
            } else {
                Location gl = golem.getLocation();
                Location cl = caster.getLocation();
                if (gl != null && cl != null) {
                    double d = gl.distanceSquared(cl);
                    if (d > followRadius * followRadius) {
                        Vector dir = cl.toVector().subtract(gl.toVector()).normalize();
                        golem.setVelocity(dir.multiply(0.35));
                    }
                }
            }
        }

        private @Nullable LivingEntity nearestEnemy(IronGolem source, boolean friendlyFire) {
            LivingEntity best = null;
            double bestD2 = 20.0 * 20.0;
            for (Entity e : source.getNearbyEntities(20, 12, 20)) {
                if (!(e instanceof LivingEntity le)) continue;
                if (le.equals(context.caster())) continue;
                if (!friendlyFire && le instanceof Player) continue;
                if (!le.isValid() || le.isDead()) continue;
                double d2 = le.getLocation().distanceSquared(source.getLocation());
                if (d2 < bestD2) { best = le; bestD2 = d2; }
            }
            return best;
        }

        private void effects() {
            if (golem == null || !golem.isValid()) return;
            if (steps % 6 == 0) {
                World w = golem.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.CRIT, golem.getLocation().add(0, 1.4, 0), 6, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }

        private void dismiss() {
            if (golem != null && golem.isValid()) {
                World world = golem.getWorld();
                if (world != null) {
                    world.playSound(golem.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.8f, 1.2f);
                    world.spawnParticle(Particle.CLOUD, golem.getLocation(), 20, 0.6, 0.6, 0.6, 0.02);
                }
                golem.remove();
            }
        }
    }
}

