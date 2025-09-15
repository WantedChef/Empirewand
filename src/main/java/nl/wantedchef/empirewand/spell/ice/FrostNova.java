package nl.wantedchef.empirewand.spell.ice;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.common.visual.RingRenderer;
import nl.wantedchef.empirewand.common.visual.SpiralEmitter;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * FrostNova - Creates an expanding nova of frost that damages and slows enemies.
 * Features proper resource management and optimized visual effects.
 */
public class FrostNova extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Frost Nova";
            this.description = "Creates an expanding nova of frost, damaging and slowing enemies";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new FrostNova(this);
        }
    }

    // Configuration defaults
    private static final double DEFAULT_RADIUS = 5.0;
    private static final double DEFAULT_DAMAGE = 6.0;
    private static final int DEFAULT_SLOW_DURATION = 100;
    private static final int DEFAULT_SLOW_AMPLIFIER = 2;
    private static final int DEFAULT_WEAKNESS_DURATION = 60;
    private static final int DEFAULT_WEAKNESS_AMPLIFIER = 1;
    private static final double DEFAULT_KNOCKBACK = 0.35;

    // Effect constants
    private static final int SNOWFLAKE_PARTICLES = 10;
    private static final int IMPACT_PARTICLES = 30;
    private static final int RING_PARTICLES = 32;
    private static final double RING_STEP = 0.4;
    private static final int SWIRL_DENSITY = 18;
    private static final int BURST_COUNT = 6;

    private FrostNova(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "frost-nova";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();

        // Load configuration
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int slowDuration = spellConfig.getInt("values.slow-duration-ticks", DEFAULT_SLOW_DURATION);
        int slowAmplifier = spellConfig.getInt("values.slow-amplifier", DEFAULT_SLOW_AMPLIFIER);
        int weaknessDuration = spellConfig.getInt("values.weakness-duration-ticks", DEFAULT_WEAKNESS_DURATION);
        int weaknessAmplifier = spellConfig.getInt("values.weakness-amplifier", DEFAULT_WEAKNESS_AMPLIFIER);
        double knockback = spellConfig.getDouble("values.knockback", DEFAULT_KNOCKBACK);

        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
            .getBoolean("features.friendly-fire", false);

        Location center = player.getLocation();

        // Damage and affect nearby entities
        for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living) {
                if (living.equals(player) && !friendlyFire) continue;
                if (living.isDead() || !living.isValid()) continue;

                living.damage(damage, player);
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, true));
                living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, weaknessAmplifier, false, true));

                // Radial knockback
                Vector direction = living.getLocation().toVector().subtract(center.toVector());
                if (direction.lengthSquared() > 0.001) {
                    direction = direction.normalize();

                    // Safety check for finite values
                    if (Double.isFinite(direction.getX()) && Double.isFinite(direction.getY()) && Double.isFinite(direction.getZ())) {
                        Vector knockbackVector = direction.multiply(knockback).setY(0.12);

                        if (Double.isFinite(knockbackVector.getX()) && Double.isFinite(knockbackVector.getY()) && Double.isFinite(knockbackVector.getZ())) {
                            living.setVelocity(living.getVelocity().add(knockbackVector));
                        }
                    }
                }

                // Damage particles
                context.fx().spawnParticles(living.getLocation(), Particle.SNOWFLAKE, SNOWFLAKE_PARTICLES,
                    0.3, 0.3, 0.3, 0.05);
            }
        }

        // Impact effect
        context.fx().impact(center, Particle.CLOUD, IMPACT_PARTICLES, Sound.BLOCK_SNOW_BREAK, 0.8f, 1.2f);

        // Visual effect configuration
        int ringParticles = spellConfig.getInt("ring-particle-count", RING_PARTICLES);
        double ringStep = spellConfig.getDouble("ring-expand-step", RING_STEP);
        int swirlDensity = spellConfig.getInt("snow-swirl-density", SWIRL_DENSITY);
        int burstCount = spellConfig.getInt("ice-burst-count", BURST_COUNT);

        // Create visual effects task
        BukkitTask task = new NovaVisuals(center, radius, ringStep, ringParticles, swirlDensity, burstCount)
            .runTaskTimer(context.plugin(), 0L, 2L);

        // Register task for cleanup
        if (context.plugin() instanceof nl.wantedchef.empirewand.EmpireWandPlugin plugin) {
            plugin.getTaskManager().registerTask(task);
        }

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in executeSpell
    }

    /**
     * Visual effects task for the frost nova.
     */
    private static final class NovaVisuals extends BukkitRunnable {
        private final Location center;
        private final double maxRadius;
        private final double ringStep;
        private final int ringParticles;
        private final int swirlDensity;
        private final int burstCount;
        private double currentRadius = 0.4;
        private int ticks = 0;

        public NovaVisuals(Location center, double maxRadius, double ringStep, int ringParticles, int swirlDensity, int burstCount) {
            this.center = center;
            this.maxRadius = maxRadius;
            this.ringStep = ringStep;
            this.ringParticles = ringParticles;
            this.swirlDensity = swirlDensity;
            this.burstCount = burstCount;
        }

        @Override
        public void run() {
            if (currentRadius >= maxRadius || center.getWorld() == null) {
                cancel();
                return;
            }

            // Ring particles
            RingRenderer.renderRing(center, currentRadius, ringParticles,
                (loc, vec) -> loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 1, 0, 0, 0, 0));

            // Spiral effect
            SpiralEmitter.emit(center.clone().add(0, 0.05, 0), 0.6, 1, swirlDensity, currentRadius * 0.25, Particle.SNOWFLAKE);

            // Burst effects
            if (ticks % 4 == 0) {
                for (int i = 0; i < burstCount; i++) {
                    double angle = (2 * Math.PI * i) / burstCount;
                    Location burstLoc = center.clone().add(
                        Math.cos(angle) * currentRadius * 0.6,
                        0.2,
                        Math.sin(angle) * currentRadius * 0.6
                    );
                    center.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, burstLoc, 2, 0.05, 0.05, 0.05, 0.01);
                }
            }

            currentRadius += ringStep;
            ticks++;
        }
    }
}