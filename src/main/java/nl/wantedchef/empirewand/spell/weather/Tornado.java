package nl.wantedchef.empirewand.spell.weather;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Tornado extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Tornado";
            this.description = "Creates a powerful whirlwind that lifts and damages enemies.";
            this.cooldown = Duration.ofMillis(8000);
            this.spellType = SpellType.WEATHER;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Tornado(this);
        }
    }

    private Tornado(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "tornado";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        Location centerLoc = player.getLocation();

        // Configuration values with enhanced defaults
        double radius = spellConfig.getDouble("values.radius", 8.0);
        double maxHeight = spellConfig.getDouble("values.max-height", 12.0);
        double pullStrength = spellConfig.getDouble("values.pull-strength", 0.4);
        double damage = spellConfig.getDouble("values.damage", 6.0);
        int duration = spellConfig.getInt("values.duration-ticks", 140); // 7 seconds
        int spiralDensity = spellConfig.getInt("values.spiral-density", 8);
        double windSpeed = spellConfig.getDouble("values.wind-speed", 1.2);
        boolean environmentalEffects = spellConfig.getBoolean("values.environmental-effects", true);

        // Find affected entities
        List<LivingEntity> affectedEntities = new ArrayList<>();
        for (var entity : player.getWorld().getNearbyLivingEntities(centerLoc, radius)) {
            if (!entity.equals(player)) {
                affectedEntities.add(entity);
            }
        }

        // Create the tornado effect
        createTornadoEffect(context, centerLoc, affectedEntities, radius, maxHeight, pullStrength,
                          damage, duration, spiralDensity, windSpeed, environmentalEffects);

        return null;
    }

    /**
     * Creates a spectacular tornado effect with spiraling particles, realistic physics,
     * and immersive audio-visual elements.
     */
    private void createTornadoEffect(SpellContext context, Location center, List<LivingEntity> entities,
                                   double radius, double maxHeight, double pullStrength, double damage,
                                   int duration, int spiralDensity, double windSpeed, boolean envEffects) {

        // Initial tornado spawn sound and effects
        context.fx().playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.4f);
        context.fx().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.3f);

        new BukkitRunnable() {
            private int ticks = 0;
            private final double baseRadius = radius;

            @Override
            public void run() {
                if (ticks >= duration) {
                    // Tornado dissipation effects
                    createDissipationEffect(context, center);
                    context.fx().flushParticleBatch();
                    this.cancel();
                    return;
                }

                // Calculate tornado properties that change over time
                double progress = (double) ticks / duration;
                double currentIntensity = calculateIntensity(progress);
                double currentRadius = baseRadius * (0.3 + 0.7 * currentIntensity);
                double currentHeight = maxHeight * currentIntensity;

                // Create multi-layered tornado visual effects
                createTornadoVisuals(context, center, currentRadius, currentHeight, spiralDensity,
                                   ticks, currentIntensity);

                // Apply physics to entities
                applyTornadoPhysics(context, center, entities, currentRadius, currentHeight,
                                  pullStrength, damage, currentIntensity);

                // Environmental effects
                if (envEffects && ticks % 10 == 0) {
                    createEnvironmentalEffects(context, center, currentRadius, currentIntensity);
                }

                // Dynamic wind sounds
                if (ticks % 20 == 0) {
                    playWindSounds(context, center, currentIntensity, windSpeed);
                }

                // Flush particle batch for performance
                if (ticks % 5 == 0) {
                    context.fx().flushParticleBatch();
                }

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * Calculates tornado intensity over time with realistic build-up and dissipation.
     */
    private double calculateIntensity(double progress) {
        if (progress < 0.2) {
            // Build-up phase: smooth acceleration
            return Math.sin(progress * Math.PI / 0.4) * 0.5;
        } else if (progress < 0.8) {
            // Sustained phase: full intensity with slight variation
            return 0.9 + 0.1 * Math.sin(progress * Math.PI * 8);
        } else {
            // Dissipation phase: smooth deceleration
            double dissipationProgress = (progress - 0.8) / 0.2;
            return (1.0 - dissipationProgress) * Math.cos(dissipationProgress * Math.PI / 2);
        }
    }

    /**
     * Creates spectacular multi-layered tornado visual effects with spiraling particles.
     */
    private void createTornadoVisuals(SpellContext context, Location center, double radius, double height,
                                    int density, int ticks, double intensity) {
        double angleOffset = ticks * 0.3; // Rotation speed

        // Main tornado spiral - multiple layers for depth
        for (int layer = 0; layer < 3; layer++) {
            double layerRadius = radius * (0.4 + layer * 0.3);
            double layerHeight = height * (0.8 + layer * 0.1);
            Particle layerParticle = layer == 0 ? Particle.CLOUD :
                                   layer == 1 ? Particle.CAMPFIRE_COSY_SMOKE : Particle.SMOKE;

            createSpiralLayer(context, center, layerRadius, layerHeight, density,
                            angleOffset + layer * 0.2, layerParticle, intensity);
        }

        // Core vortex effect
        createVortexCore(context, center, radius * 0.2, height, ticks, intensity);

        // Debris and wind particles
        createDebrisEffect(context, center, radius, height, ticks, intensity);

        // Lightning effects during peak intensity
        if (intensity > 0.7 && ticks % 15 == 0) {
            createLightningEffect(context, center, height, intensity);
        }
    }

    /**
     * Creates a single spiral layer of the tornado with mathematical precision.
     */
    private void createSpiralLayer(SpellContext context, Location center, double radius, double height,
                                 int density, double angleOffset, Particle particle, double intensity) {
        int particleCount = (int) (density * intensity);

        for (int i = 0; i < particleCount; i++) {
            double heightFraction = (double) i / particleCount;
            double currentRadius = radius * (1.0 - heightFraction * 0.7); // Tapered spiral
            double angle = angleOffset + heightFraction * Math.PI * 6; // 3 full rotations

            double x = center.getX() + currentRadius * Math.cos(angle);
            double z = center.getZ() + currentRadius * Math.sin(angle);
            double y = center.getY() + height * heightFraction;

            Location particleLoc = new Location(center.getWorld(), x, y, z);

            // Add swirling motion to particles
            double velX = -Math.sin(angle) * 0.1 * intensity;
            double velZ = Math.cos(angle) * 0.1 * intensity;
            double velY = 0.05 * intensity;

            context.fx().batchParticles(particleLoc, particle, 1, velX, velY, velZ, 0.02);
        }
    }

    /**
     * Creates the intense vortex core effect at the center of the tornado.
     */
    private void createVortexCore(SpellContext context, Location center, double coreRadius, double height,
                                int ticks, double intensity) {
        // Rapidly spinning core particles
        for (int i = 0; i < height * 2; i++) {
            double y = center.getY() + i * 0.5;
            double angle = ticks * 0.8 + i * 0.2;
            double x = center.getX() + coreRadius * Math.cos(angle);
            double z = center.getZ() + coreRadius * Math.sin(angle);

            Location coreLoc = new Location(center.getWorld(), x, y, z);
            context.fx().batchParticles(coreLoc, Particle.ENCHANT, 2, 0.1, 0.1, 0.1, 0.05 * intensity);
        }

        // Upward rushing air effect
        for (int i = 0; i < 5; i++) {
            Location rushLoc = center.clone().add(
                (Math.random() - 0.5) * coreRadius * 2,
                Math.random() * height * 0.3,
                (Math.random() - 0.5) * coreRadius * 2
            );
            context.fx().batchParticles(rushLoc, Particle.ENCHANT, 1, 0, 0.3 * intensity, 0, 0.1);
        }
    }

    /**
     * Creates debris and environmental particle effects around the tornado.
     */
    private void createDebrisEffect(SpellContext context, Location center, double radius, double height,
                                  int ticks, double intensity) {
        // Flying debris simulation
        int debrisCount = (int) (8 * intensity);
        for (int i = 0; i < debrisCount; i++) {
            double angle = (ticks * 0.4 + i * Math.PI * 2 / debrisCount) % (Math.PI * 2);
            double distance = radius * (0.8 + Math.random() * 0.4);
            double debrisHeight = Math.random() * height * 0.7;

            Location debrisLoc = center.clone().add(
                distance * Math.cos(angle),
                debrisHeight,
                distance * Math.sin(angle)
            );

            context.fx().batchParticles(debrisLoc, Particle.CRIT, 3,
                0.3, 0.2, 0.3, 0.1 * intensity);
        }

        // Ground disturbance
        if (ticks % 8 == 0) {
            for (int i = 0; i < 6; i++) {
                Location groundLoc = center.clone().add(
                    (Math.random() - 0.5) * radius * 1.5,
                    -0.5,
                    (Math.random() - 0.5) * radius * 1.5
                );
                context.fx().batchParticles(groundLoc, Particle.SMOKE, 5,
                    0.5, 0.1, 0.5, 0.05 * intensity);
            }
        }
    }

    /**
     * Creates lightning effects during peak tornado intensity.
     */
    private void createLightningEffect(SpellContext context, Location center, double height, double intensity) {
        // Lightning bolt particles
        Location lightningStart = center.clone().add(0, height * 0.8, 0);
        Location lightningEnd = center.clone().add(
            (Math.random() - 0.5) * 4,
            height * 0.3,
            (Math.random() - 0.5) * 4
        );

        context.fx().trail(lightningStart, lightningEnd, Particle.ELECTRIC_SPARK, 3);

        // Lightning sound
        context.fx().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
            (float) (0.3 * intensity), 1.5f);
    }

    /**
     * Applies realistic tornado physics to entities with spiraling motion.
     */
    private void applyTornadoPhysics(SpellContext context, Location center, List<LivingEntity> entities,
                                   double radius, double height, double pullStrength, double damage,
                                   double intensity) {

        for (LivingEntity entity : entities) {
            Location entityLoc = entity.getLocation();
            double distance = entityLoc.distance(center);

            if (distance <= radius * 1.2) { // Slightly larger affect radius
                // Calculate pull vector with spiral motion
                Vector toCenter = center.toVector().subtract(entityLoc.toVector());
                toCenter.setY(0); // Horizontal pull only initially
                toCenter.normalize();

                // Add spiral component
                double angle = Math.atan2(toCenter.getZ(), toCenter.getX()) + Math.PI / 2;
                Vector spiral = new Vector(Math.cos(angle), 0, Math.sin(angle));
                spiral.multiply(pullStrength * 0.7);

                // Combine pull and spiral
                Vector finalForce = toCenter.multiply(pullStrength).add(spiral);

                // Add upward force based on proximity to center
                double proximityFactor = 1.0 - (distance / radius);
                double upwardForce = pullStrength * proximityFactor * intensity;
                finalForce.setY(upwardForce);

                // Apply the force
                entity.setVelocity(entity.getVelocity().add(finalForce));

                // Apply effects based on intensity
                if (intensity > 0.5) {
                    // Levitation effect for sustained lift
                    int levitationDuration = (int) (20 * intensity);
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,
                        levitationDuration, 0, false, false));

                    // Damage over time
                    if (context.caster() instanceof Player caster) {
                        entity.damage(damage * intensity * 0.1, caster); // Reduced per-tick damage
                    }
                }

                // Visual feedback on affected entities
                context.fx().batchParticles(entityLoc, Particle.ENCHANT, 8,
                    0.5, 1.0, 0.5, 0.1 * intensity);
            }
        }
    }

    /**
     * Creates environmental effects around the tornado.
     */
    private void createEnvironmentalEffects(SpellContext context, Location center, double radius, double intensity) {
        // Weather particles
        for (int i = 0; i < 15; i++) {
            Location weatherLoc = center.clone().add(
                (Math.random() - 0.5) * radius * 2.5,
                Math.random() * 3,
                (Math.random() - 0.5) * radius * 2.5
            );

            context.fx().batchParticles(weatherLoc, Particle.DRIPPING_WATER, 2,
                0.1, 0, 0.1, 0.02);
        }

        // Atmospheric disturbance
        context.fx().batchParticles(center.clone().add(0, radius, 0), Particle.CLOUD,
            (int) (20 * intensity), radius * 0.8, 2, radius * 0.8, 0.1);
    }

    /**
     * Plays realistic wind sounds with 3D positioning and intensity variation.
     */
    private void playWindSounds(SpellContext context, Location center, double intensity, double windSpeed) {
        // Main wind sound
        float volume = (float) (0.8 * intensity);
        float pitch = (float) (0.4 + 0.6 * windSpeed);
        context.fx().playSound(center, Sound.ITEM_ELYTRA_FLYING, volume, pitch);

        // Additional atmospheric sounds
        if (intensity > 0.6) {
            context.fx().playSound(center, Sound.ENTITY_PHANTOM_AMBIENT,
                (float) (0.4 * intensity), 0.3f);
        }

        // Thunder rumble during peak intensity
        if (intensity > 0.8) {
            context.fx().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                (float) (0.2 * intensity), 0.2f);
        }
    }

    /**
     * Creates spectacular dissipation effects when the tornado ends.
     */
    private void createDissipationEffect(SpellContext context, Location center) {
        // Final burst of particles
        context.fx().spawnParticles(center, Particle.EXPLOSION, 15, 3, 2, 3, 0.1);
        context.fx().spawnParticles(center, Particle.CLOUD, 50, 4, 3, 4, 0.2);
        context.fx().spawnParticles(center, Particle.SWEEP_ATTACK, 20, 2, 1, 2, 0);

        // Dissipation sound
        context.fx().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.4f);
        context.fx().playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.3f);
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
