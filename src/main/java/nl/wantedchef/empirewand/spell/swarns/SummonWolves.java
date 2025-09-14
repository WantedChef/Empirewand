package nl.wantedchef.empirewand.spell.swarns;

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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SummonWolves extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Summon Wolves";
            this.description = "Summons a pack of wolves that defend you.";
            this.cooldown = Duration.ofSeconds(55);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new SummonWolves(this);
        }
    }

    private SummonWolves(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "summon-wolves";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Player caster = context.caster();
        World world = caster.getWorld();
        if (world == null) return null;

        int count = spellConfig.getInt("values.count", 3);
        double radius = spellConfig.getDouble("values.radius", 5.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 20 * 20); // 20s

        world.playSound(caster.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 1.0f);

        context.plugin().getTaskManager().runTaskTimer(new PackTask(context, count, radius, durationTicks), 0L, 5L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // async task handles visuals/cleanup
    }

    private static final class PackTask extends BukkitRunnable {
        private final SpellContext context;
        private final int count;
        private final double radius;
        private final int durationTicks;
        private final int maxSteps;
        private final List<Wolf> wolves = new ArrayList<>();
        private int steps = 0;

        PackTask(SpellContext context, int count, double radius, int durationTicks) {
            this.context = Objects.requireNonNull(context);
            this.count = count;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.maxSteps = Math.max(1, durationTicks / 5);
        }

        @Override
        public void run() {
            World world = context.caster().getWorld();
            if (world == null) { cancel(); return; }

            if (steps == 0) spawnWolves();
            if (steps >= maxSteps) { dismiss(); cancel(); return; }

            updateBehavior();
            ringEffects();
            steps++;
        }

        private void spawnWolves() {
            Player caster = context.caster();
            World world = caster.getWorld();
            Location c = caster.getLocation();
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / Math.max(1, count);
                double x = c.getX() + Math.cos(angle) * radius;
                double z = c.getZ() + Math.sin(angle) * radius;
                Location spawn = new Location(world, x, world.getHighestBlockYAt((int) x, (int) z) + 1, z);
                Wolf wolf = world.spawn(spawn, Wolf.class, w -> {
                    w.setOwner(caster);
                    w.setInvulnerable(false);
                    w.setAdult();
                    w.setCollarColor(org.bukkit.DyeColor.BLUE);
                });
                wolves.add(wolf);
                world.spawnParticle(Particle.HEART, spawn, 4, 0.3, 0.3, 0.3, 0.01);
            }
        }

        private void updateBehavior() {
            Player caster = context.caster();
            boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class)
                    .getMainConfig().getBoolean("features.friendly-fire", false);
            for (Wolf wolf : wolves) {
                if (!wolf.isValid() || wolf.isDead()) continue;
                LivingEntity target = nearestEnemy(wolf, friendlyFire);
                if (target != null) {
                    wolf.setTarget(target);
                } else {
                    // follow owner via small velocity nudge
                    Location wl = wolf.getLocation();
                    Location cl = caster.getLocation();
                    if (wl != null && cl != null) {
                        Vector dir = cl.toVector().subtract(wl.toVector());
                        if (dir.lengthSquared() > 4.0) { // only nudge if far
                            wolf.setVelocity(dir.normalize().multiply(0.35));
                        }
                    }
                }
                if (steps % 8 == 0) {
                    World w = wolf.getWorld();
                    if (w != null) w.spawnParticle(Particle.ENCHANT, wolf.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }

        private @Nullable LivingEntity nearestEnemy(Wolf source, boolean friendlyFire) {
            LivingEntity best = null;
            double dist = 16.0;
            for (Entity e : source.getNearbyEntities(16, 16, 16)) {
                if (!(e instanceof LivingEntity le)) continue;
                if (le.equals(context.caster())) continue;
                if (!friendlyFire && le instanceof Player) continue;
                if (!le.isValid() || le.isDead()) continue;
                double d = le.getLocation().distanceSquared(source.getLocation());
                if (d < dist * dist) { best = le; dist = Math.sqrt(d); }
            }
            return best;
        }

        private void ringEffects() {
            if (steps % 4 != 0) return;
            World world = context.caster().getWorld();
            Location c = context.caster().getLocation();
            if (world == null || c == null) return;
            double r = 1.5 + Math.sin(steps * 0.25) * 0.5;
            for (int i = 0; i < 12; i++) {
                double ang = 2 * Math.PI * i / 12.0 + steps * 0.05;
                Location p = c.clone().add(Math.cos(ang) * r, 0.1, Math.sin(ang) * r);
                world.spawnParticle(Particle.HEART, p, 1, 0, 0, 0, 0);
            }
        }

        private void dismiss() {
            for (Wolf w : wolves) {
                if (w.isValid()) {
                    World world = w.getWorld();
                    if (world != null) {
                        world.spawnParticle(Particle.POOF, w.getLocation(), 8, 0.4, 0.4, 0.4, 0.02);
                        world.playSound(w.getLocation(), Sound.ENTITY_WOLF_WHINE, 0.7f, 1.3f);
                    }
                    w.remove();
                }
            }
            wolves.clear();
        }
    }
}

