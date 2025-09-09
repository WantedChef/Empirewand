package nl.wantedchef.empirewand.spell.poison;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.misc.WaveSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Poison Wave - A wave of poison that damages and poisons enemies.
 */
public class PoisonWave extends WaveSpell {

    public static class Builder extends WaveSpell.Builder {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Poison Wave";
            this.description = "Unleashes a wave of poison that damages and poisons enemies.";
            this.cooldown = Duration.ofSeconds(12);
            this.spellType = SpellType.POISON;

            // Configure wave properties
            this.waveConfig = new WaveConfig(
                    0.25, // wave speed
                    7.0, // max radius
                    4.0, // damage
                    20, // particle count
                    "SPELL", // particle type
                    "ENTITY_SPIDER_HURT", // sound
                    0.7f, // volume
                    1.0f, // pitch
                    50, // duration ticks (2.5 seconds)
                    1.5 // entity effect radius
            );
        }

        @Override
        public @NotNull WaveSpell build() {
            return new PoisonWave(this);
        }
    }

    private PoisonWave(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "poison-wave";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void applyWaveEffectToEntity(@NotNull SpellContext context, @NotNull LivingEntity entity,
            double distance) {
        // Skip the caster
        if (entity instanceof Player && entity.equals(context.caster())) {
            return;
        }

        // Apply damage
        entity.damage(config.damage, context.caster());

        // Apply poison effect
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.POISON,
                140, // 7 seconds
                1, // Level 2
                false,
                true));

        // Apply weakness
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS,
                160, // 8 seconds
                0, // Level 1
                false,
                true));

        // Create poison particle effect on the entity
        context.fx().spawnParticles(entity.getLocation(), Particle.SMOKE, 8, 0.3, 0.3, 0.3, 0.1);
    }

    @Override
    protected void createWaveParticles(@NotNull SpellContext context, @NotNull org.bukkit.Location center,
            double radius) {
        // Create green poison particles
        for (int i = 0; i < config.particleCount; i++) {
            double angle = 2 * Math.PI * i / config.particleCount;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;

            // Create particles at ground level and slightly above
            for (int y = 0; y <= 2; y++) {
                org.bukkit.Location particleLoc = new org.bukkit.Location(center.getWorld(), x, center.getY() + y, z);

                // Use green particles for poison effect
                context.fx().spawnParticles(particleLoc, Particle.SMOKE, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }
}
