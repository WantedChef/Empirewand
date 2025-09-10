package nl.wantedchef.empirewand.spell.misc.utility;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.base.Spell;
import nl.wantedchef.empirewand.spell.base.SpellContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for wave spells that create expanding wave effects.
 * Waves expand outward from the caster, affecting entities in their path.
 */
public abstract class WaveSpell extends Spell<Void> {

    protected WaveConfig config;

    public static class WaveConfig {
        public final double waveSpeed;
        public final double maxRadius;
        public final double damage;
        public final int particleCount;
        public final String particleType;
        public final String sound;
        public final float volume;
        public final float pitch;
        public final int waveDurationTicks;
        public final double entityEffectRadius;

        public WaveConfig(double waveSpeed, double maxRadius, double damage, int particleCount,
                String particleType, String sound, float volume, float pitch,
                int waveDurationTicks, double entityEffectRadius) {
            this.waveSpeed = waveSpeed;
            this.maxRadius = maxRadius;
            this.damage = damage;
            this.particleCount = particleCount;
            this.particleType = particleType;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.waveDurationTicks = waveDurationTicks;
            this.entityEffectRadius = entityEffectRadius;
        }
    }

    public static abstract class Builder extends Spell.Builder<Void> {
        protected WaveConfig waveConfig;

        public Builder(EmpireWandAPI api) {
            super(api);
        }

        public Builder waveSpeed(double speed) {
            this.waveConfig = new WaveConfig(
                    speed,
                    waveConfig.maxRadius,
                    waveConfig.damage,
                    waveConfig.particleCount,
                    waveConfig.particleType,
                    waveConfig.sound,
                    waveConfig.volume,
                    waveConfig.pitch,
                    waveConfig.waveDurationTicks,
                    waveConfig.entityEffectRadius);
            return this;
        }

        public Builder maxRadius(double radius) {
            this.waveConfig = new WaveConfig(
                    waveConfig.waveSpeed,
                    radius,
                    waveConfig.damage,
                    waveConfig.particleCount,
                    waveConfig.particleType,
                    waveConfig.sound,
                    waveConfig.volume,
                    waveConfig.pitch,
                    waveConfig.waveDurationTicks,
                    waveConfig.entityEffectRadius);
            return this;
        }

        public Builder damage(double damage) {
            this.waveConfig = new WaveConfig(
                    waveConfig.waveSpeed,
                    waveConfig.maxRadius,
                    damage,
                    waveConfig.particleCount,
                    waveConfig.particleType,
                    waveConfig.sound,
                    waveConfig.volume,
                    waveConfig.pitch,
                    waveConfig.waveDurationTicks,
                    waveConfig.entityEffectRadius);
            return this;
        }

        public Builder particles(int count, String type) {
            this.waveConfig = new WaveConfig(
                    waveConfig.waveSpeed,
                    waveConfig.maxRadius,
                    waveConfig.damage,
                    count,
                    type,
                    waveConfig.sound,
                    waveConfig.volume,
                    waveConfig.pitch,
                    waveConfig.waveDurationTicks,
                    waveConfig.entityEffectRadius);
            return this;
        }

        public Builder sound(String sound, float volume, float pitch) {
            this.waveConfig = new WaveConfig(
                    waveConfig.waveSpeed,
                    waveConfig.maxRadius,
                    waveConfig.damage,
                    waveConfig.particleCount,
                    waveConfig.particleType,
                    sound,
                    volume,
                    pitch,
                    waveConfig.waveDurationTicks,
                    waveConfig.entityEffectRadius);
            return this;
        }

        public Builder duration(int ticks) {
            this.waveConfig = new WaveConfig(
                    waveConfig.waveSpeed,
                    waveConfig.maxRadius,
                    waveConfig.damage,
                    waveConfig.particleCount,
                    waveConfig.particleType,
                    waveConfig.sound,
                    waveConfig.volume,
                    waveConfig.pitch,
                    ticks,
                    waveConfig.entityEffectRadius);
            return this;
        }

        public Builder entityEffectRadius(double radius) {
            this.waveConfig = new WaveConfig(
                    waveConfig.waveSpeed,
                    waveConfig.maxRadius,
                    waveConfig.damage,
                    waveConfig.particleCount,
                    waveConfig.particleType,
                    waveConfig.sound,
                    waveConfig.volume,
                    waveConfig.pitch,
                    waveConfig.waveDurationTicks,
                    radius);
            return this;
        }
    }

    protected WaveSpell(Builder builder) {
        super(builder);
        this.config = builder.waveConfig;
    }

    @Override
    protected Void executeSpell(@NotNull SpellContext context) {
        Player caster = context.caster();
        Location startLoc = caster.getLocation();

        // Play initial sound
        try {
            Sound sound = Sound.valueOf(config.sound);
            context.fx().playSound(startLoc, sound, config.volume, config.pitch);
        } catch (IllegalArgumentException e) {
            // Fallback to default sound
            context.fx().playSound(startLoc, Sound.ENTITY_GENERIC_EXPLODE, config.volume, config.pitch);
        }

        // Start the wave expansion
        startWaveExpansion(context, startLoc);

        return null;
    }

    protected void startWaveExpansion(@NotNull SpellContext context, @NotNull Location startLoc) {
        // Create expanding wave effect
        org.bukkit.scheduler.BukkitRunnable waveTask = new org.bukkit.scheduler.BukkitRunnable() {
            private int tick = 0;
            private double currentRadius = 0;

            @Override
            public void run() {
                if (tick >= config.waveDurationTicks || currentRadius >= config.maxRadius) {
                    cancel();
                    return;
                }

                // Expand the wave
                currentRadius += config.waveSpeed;
                if (currentRadius > config.maxRadius) {
                    currentRadius = config.maxRadius;
                }

                // Create particle ring at current radius
                createWaveParticles(context, startLoc, currentRadius);

                // Apply effects to entities at this radius
                applyWaveEffects(context, startLoc, currentRadius);

                tick++;
            }
        };

        waveTask.runTaskTimer(context.plugin(), 0L, 1L);
    }

    protected void createWaveParticles(@NotNull SpellContext context, @NotNull Location center, double radius) {
        // Create a ring of particles at the specified radius
        for (int i = 0; i < config.particleCount; i++) {
            double angle = 2 * Math.PI * i / config.particleCount;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;

            // Create particles at multiple heights
            for (int y = -1; y <= 1; y++) {
                Location particleLoc = new Location(center.getWorld(), x, center.getY() + y, z);

                try {
                    Particle particle = Particle.valueOf(config.particleType);
                    context.fx().spawnParticles(particleLoc, particle, 1, 0.1, 0.1, 0.1, 0.01);
                } catch (IllegalArgumentException e) {
                    // Fallback to smoke particles
                    context.fx().spawnParticles(particleLoc, Particle.SMOKE, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
    }

    protected void applyWaveEffects(@NotNull SpellContext context, @NotNull Location center, double radius) {
        // Get entities near the wave
        for (LivingEntity entity : center.getWorld().getNearbyLivingEntities(center,
                radius + config.entityEffectRadius)) {
            // Skip the caster
            if (entity.equals(context.caster())) {
                continue;
            }

            // Check if entity is at the right distance from the center
            double distance = entity.getLocation().distance(center);
            double minDistance = radius - config.entityEffectRadius;
            double maxDistance = radius + config.entityEffectRadius;

            if (distance >= minDistance && distance <= maxDistance) {
                applyWaveEffectToEntity(context, entity, distance);
            }
        }
    }

    /**
     * Apply the wave effect to a specific entity.
     * Subclasses should override this to implement their specific effects.
     */
    protected abstract void applyWaveEffectToEntity(@NotNull SpellContext context, @NotNull LivingEntity entity,
            double distance);

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Wave effects are handled during expansion
    }
}
