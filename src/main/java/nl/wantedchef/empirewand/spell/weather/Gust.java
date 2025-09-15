package nl.wantedchef.empirewand.spell.weather;

import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Gust extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Gust";
            this.description = "Creates a powerful gust of wind that knocks back enemies.";
            this.cooldown = Duration.ofMillis(2000);
            this.spellType = SpellType.WEATHER;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Gust(this);
        }
    }

    private Gust(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "gust";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        Location origin = player.getEyeLocation();

        // Configuration values with enhanced defaults
        double range = spellConfig.getDouble("values.range", 25.0);
        double angle = spellConfig.getDouble("values.angle", 70.0);
        double knockback = spellConfig.getDouble("values.knockback", 1.2);
        double damage = spellConfig.getDouble("values.damage", 2.0);
        int windDuration = spellConfig.getInt("values.wind-duration-ticks", 40);
        boolean environmentalEffects = spellConfig.getBoolean("values.environmental-effects", true);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        // Get targets in cone with enhanced detection
        List<LivingEntity> targets = getEntitiesInCone(player, range, angle);

        // Create spectacular wind visuals immediately
        createWindConeVisuals(context, origin, range, angle, windDuration);

        // Apply effects to targets with enhanced physics
        for (LivingEntity target : targets) {
            if (target.equals(player) && !friendlyFire) continue;

            // Calculate distance-based effects for realism
            double distance = target.getLocation().distance(origin);
            double distanceMultiplier = Math.max(0.3, 1.0 - (distance / range));

            // Apply damage with falloff
            if (damage > 0) {
                target.damage(damage * distanceMultiplier, player);
            }

            // Enhanced knockback with realistic physics
            Vector pushDirection = target.getLocation().toVector()
                    .subtract(origin.toVector())
                    .normalize();

            // Add some upward lift and lateral spread for realism
            pushDirection.setY(Math.max(0.15, pushDirection.getY() + 0.3));
            Vector pushVelocity = pushDirection.multiply(knockback * distanceMultiplier);

            // Apply velocity with momentum conservation
            Vector currentVelocity = target.getVelocity();
            target.setVelocity(currentVelocity.add(pushVelocity));

            // Enhanced target impact effects
            createTargetImpactEffects(context, target.getLocation(), distanceMultiplier);
        }

        // Environmental effects for immersion
        if (environmentalEffects) {
            createEnvironmentalEffects(context, origin, range, angle);
        }

        // Enhanced spatial audio system
        createWindSoundscape(context, player, origin, range, windDuration);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private List<LivingEntity> getEntitiesInCone(Player player, double range, double coneAngle) {
        List<LivingEntity> targets = new ArrayList<>();
        Vector playerDir = player.getEyeLocation().getDirection().normalize();
        double angleRadians = Math.toRadians(coneAngle / 2.0);
        Location playerLoc = player.getEyeLocation();

        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.equals(player)) continue;

            Location entityLoc = entity.getEyeLocation();
            double distance = entityLoc.distance(playerLoc);

            if (distance > range) continue;

            Vector toEntity = entityLoc.toVector().subtract(playerLoc.toVector()).normalize();
            if (playerDir.angle(toEntity) <= angleRadians) {
                targets.add(entity);
            }
        }
        return targets;
    }

    /**
     * Creates spectacular wind cone visuals with swirling patterns and layered effects
     */
    private void createWindConeVisuals(SpellContext context, Location origin, double range, double angle, int duration) {
        new WindConeRenderer(context, origin, range, angle, duration)
                .runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * Creates enhanced impact effects when wind hits targets
     */
    private void createTargetImpactEffects(SpellContext context, Location location, double intensity) {
        // Multi-layered particle burst
        int particleCount = (int) (15 * intensity);

        // Wind impact particles
        context.fx().spawnParticles(location, Particle.SWEEP_ATTACK, particleCount, 0.4, 0.4, 0.4, 0.1);
        context.fx().spawnParticles(location, Particle.CLOUD, particleCount / 2, 0.3, 0.3, 0.3, 0.08);
        context.fx().spawnParticles(location, Particle.CRIT, particleCount / 3, 0.2, 0.2, 0.2, 0.15);

        // Add some dust particles for ground impacts
        if (location.getY() - location.getWorld().getHighestBlockYAt(location) < 3) {
            context.fx().spawnParticles(location.clone().add(0, -0.5, 0),
                Particle.BLOCK, (int) (8 * intensity), 0.5, 0.1, 0.5, 0.1,
                Material.DIRT.createBlockData());
        }
    }

    /**
     * Creates environmental wind effects like moving leaves and dust
     */
    private void createEnvironmentalEffects(SpellContext context, Location origin, double range, double angle) {
        Vector direction = origin.getDirection().normalize();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Create environmental particle effects throughout the cone
        for (int i = 0; i < 20; i++) {
            double distance = random.nextDouble(2.0, range);
            double spreadAngle = random.nextGaussian() * Math.toRadians(angle / 4);

            Vector particleDir = direction.clone().rotateAroundY(spreadAngle);
            Location effectLoc = origin.clone().add(particleDir.multiply(distance));
            effectLoc.setY(effectLoc.getY() + random.nextGaussian() * 2);

            // Environmental particles based on biome and location
            if (random.nextFloat() < 0.7) {
                context.fx().spawnParticles(effectLoc, Particle.ITEM, 3, 0.3, 0.3, 0.3, 0.1,
                    new org.bukkit.inventory.ItemStack(Material.OAK_LEAVES));
            }
            if (random.nextFloat() < 0.5) {
                context.fx().spawnParticles(effectLoc, Particle.FALLING_DUST, 2, 0.2, 0.2, 0.2, 0.1,
                    Material.SAND.createBlockData());
            }
        }
    }

    /**
     * Creates layered wind soundscape with 3D spatial audio
     */
    private void createWindSoundscape(SpellContext context, Player player, Location origin, double range, int duration) {
        // Immediate cast sound
        context.fx().playSound(player, Sound.ENTITY_PHANTOM_FLAP, 0.9f, 1.3f);
        context.fx().playSound(origin, Sound.ITEM_ELYTRA_FLYING, 1.2f, 0.8f);

        // Create a sustained wind effect
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                float volume = Math.max(0.2f, 1.0f - (ticks / (float) duration));
                float pitch = 0.7f + (ticks / (float) duration) * 0.4f;

                // Layer multiple wind sounds for richness
                if (ticks % 8 == 0) {
                    context.fx().playSound(origin, Sound.ITEM_ELYTRA_FLYING, volume * 0.6f, pitch);
                }
                if (ticks % 12 == 0) {
                    context.fx().playSound(origin, Sound.ENTITY_PHANTOM_AMBIENT, volume * 0.4f, pitch * 1.2f);
                }

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 3L, 2L);
    }

    /**
     * Advanced wind cone renderer with mathematical spiral patterns and optimized particle batching
     */
    private class WindConeRenderer extends BukkitRunnable {
        private final SpellContext context;
        private final Location origin;
        private final double range;
        private final double angle;
        private final Vector direction;
        private final int maxTicks;
        private int currentTick = 0;
        private double spiralOffset = 0;

        public WindConeRenderer(SpellContext context, Location origin, double range, double angle, int maxTicks) {
            this.context = context;
            this.origin = origin.clone();
            this.range = range;
            this.angle = angle;
            this.direction = origin.getDirection().normalize();
            this.maxTicks = maxTicks;
        }

        @Override
        public void run() {
            if (currentTick >= maxTicks) {
                // Final burst effect
                createFinalWindBurst();
                cancel();
                return;
            }

            double progress = currentTick / (double) maxTicks;
            double currentRange = range * Math.min(1.0, progress * 2.5);

            renderWindCone(currentRange, progress);
            spiralOffset += 0.3; // Spiral animation speed
            currentTick++;
        }

        private void renderWindCone(double currentRange, double progress) {
            double angleRad = Math.toRadians(angle);

            // Main wind streams
            int streamCount = 8;
            for (int stream = 0; stream < streamCount; stream++) {
                double streamAngle = (stream / (double) streamCount) * angleRad - angleRad / 2;
                Vector streamDir = rotateVectorAroundAxis(direction, streamAngle);

                renderWindStream(streamDir, currentRange, progress, stream);
            }

            // Swirling wind effects
            renderSpiralWinds(currentRange, progress);

            // Edge turbulence
            renderTurbulence(currentRange, progress);
        }

        private void renderWindStream(Vector streamDir, double currentRange, double progress, int streamIndex) {
            int particleCount = Math.max(5, (int) (15 * progress));
            double stepSize = Math.max(0.5, currentRange / 20);

            for (double distance = 1.0; distance <= currentRange; distance += stepSize) {
                Location particleLoc = origin.clone().add(streamDir.clone().multiply(distance));

                // Add some randomness for natural wind movement
                particleLoc.add(
                    (ThreadLocalRandom.current().nextGaussian() - 0.5) * 0.3,
                    (ThreadLocalRandom.current().nextGaussian() - 0.5) * 0.2,
                    (ThreadLocalRandom.current().nextGaussian() - 0.5) * 0.3
                );

                // Particle intensity decreases with distance
                double intensity = Math.max(0.3, 1.0 - (distance / currentRange));
                int particles = Math.max(1, (int) (particleCount * intensity));

                // Use different particles for visual variety
                if (streamIndex % 3 == 0) {
                    context.fx().batchParticles(particleLoc, Particle.CLOUD, particles, 0.1, 0.1, 0.1, 0.05);
                } else if (streamIndex % 3 == 1) {
                    context.fx().batchParticles(particleLoc, Particle.WHITE_ASH, particles, 0.2, 0.2, 0.2, 0.08);
                } else {
                    context.fx().batchParticles(particleLoc, Particle.SMOKE, particles / 2, 0.15, 0.15, 0.15, 0.03);
                }
            }
        }

        private void renderSpiralWinds(double currentRange, double progress) {
            int spirals = 3;
            double spiralRadius = Math.min(3.0, currentRange * 0.2);

            for (int spiral = 0; spiral < spirals; spiral++) {
                double baseAngle = (spiral / (double) spirals) * Math.PI * 2 + spiralOffset;

                for (double distance = 2.0; distance <= currentRange * 0.8; distance += 0.8) {
                    double spiralAngle = baseAngle + (distance / currentRange) * Math.PI * 4;
                    double radius = spiralRadius * Math.sin(distance / currentRange * Math.PI);

                    Vector offset = new Vector(
                        Math.cos(spiralAngle) * radius,
                        Math.sin(spiralAngle * 1.5) * radius * 0.5,
                        Math.sin(spiralAngle) * radius
                    );

                    Location spiralLoc = origin.clone().add(direction.clone().multiply(distance)).add(offset);

                    double intensity = Math.max(0.2, 1.0 - (distance / currentRange));
                    int particles = Math.max(1, (int) (8 * intensity * progress));

                    context.fx().batchParticles(spiralLoc, Particle.WHITE_ASH, particles, 0.1, 0.1, 0.1, 0.1);
                }
            }
        }

        private void renderTurbulence(double currentRange, double progress) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            double angleRad = Math.toRadians(angle);

            // Create turbulent edge effects
            for (int i = 0; i < 12; i++) {
                double edgeAngle = (i / 12.0) * angleRad - angleRad / 2;
                double distance = currentRange * (0.7 + random.nextDouble() * 0.3);

                Vector edgeDir = rotateVectorAroundAxis(direction, edgeAngle * 0.9);
                Location turbulentLoc = origin.clone().add(edgeDir.multiply(distance));

                // Add chaotic movement
                turbulentLoc.add(
                    random.nextGaussian() * 0.8,
                    random.nextGaussian() * 0.4,
                    random.nextGaussian() * 0.8
                );

                int particles = Math.max(1, (int) (6 * progress));
                context.fx().batchParticles(turbulentLoc, Particle.SMOKE, particles, 0.3, 0.3, 0.3, 0.1);
            }
        }

        private void createFinalWindBurst() {
            // Create a spectacular final wind burst
            Location burstCenter = origin.clone().add(direction.multiply(range * 0.6));

            // Explosive wind ring
            for (int i = 0; i < 16; i++) {
                double ringAngle = (i / 16.0) * Math.PI * 2;
                double ringRadius = 2.5;

                Vector ringOffset = new Vector(
                    Math.cos(ringAngle) * ringRadius,
                    Math.sin(ringAngle * 2) * 0.8, // Figure-8 pattern
                    Math.sin(ringAngle) * ringRadius
                );

                Location ringLoc = burstCenter.clone().add(ringOffset);
                context.fx().spawnParticles(ringLoc, Particle.EXPLOSION, 3, 0.2, 0.2, 0.2, 0.1);
                context.fx().spawnParticles(ringLoc, Particle.CLOUD, 8, 0.3, 0.3, 0.3, 0.15);
            }

            // Final sound burst
            context.fx().playSound(burstCenter, Sound.ENTITY_PHANTOM_DEATH, 0.8f, 1.5f);

            // Flush any remaining batched particles for performance
            context.fx().flushParticleBatch();
        }

        private Vector rotateVectorAroundAxis(Vector vector, double angle) {
            // Rotate around Y-axis for horizontal spread
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            return new Vector(
                vector.getX() * cos - vector.getZ() * sin,
                vector.getY(),
                vector.getX() * sin + vector.getZ() * cos
            );
        }
    }
}
