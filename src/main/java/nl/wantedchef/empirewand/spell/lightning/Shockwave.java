package nl.wantedchef.empirewand.spell.lightning;

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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Shockwave - A wave of electrical energy that damages and stuns enemies.
 */
public class Shockwave extends WaveSpell {

    public static class Builder extends WaveSpell.Builder {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Shockwave";
            this.description = "Unleashes a wave of electrical energy that damages and stuns enemies.";
            this.cooldown = Duration.ofSeconds(10);
            this.spellType = SpellType.LIGHTNING;

            // Configure wave properties
            this.waveConfig = new WaveConfig(
                    0.4, // wave speed
                    6.0, // max radius
                    5.0, // damage
                    28, // particle count
                    "ELECTRIC_SPARK", // particle type
                    "ENTITY_LIGHTNING_BOLT_THUNDER", // sound
                    1.0f, // volume
                    1.2f, // pitch
                    40, // duration ticks (2 seconds)
                    1.5 // entity effect radius
            );
        }

        @Override
        public @NotNull WaveSpell build() {
            return new Shockwave(this);
        }
    }

    private Shockwave(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "shockwave";
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

        // Apply slowness (electrical stun effect)
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                80, // 4 seconds
                2, // Level 3 (severe slowness)
                false,
                true));

        // Apply mining fatigue (muscular paralysis)
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.MINING_FATIGUE,
                60, // 3 seconds
                1, // Level 2
                false,
                true));

        // Apply brief blindness (flash effect)
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                20, // 1 second
                0, // Level 1
                false,
                true));

        // Create electric spark particle effect on the entity
        context.fx().spawnParticles(entity.getLocation(), Particle.ELECTRIC_SPARK, 10, 0.3, 0.3, 0.3, 0.1);

        // Apply knockback away from caster
        Vector direction = entity.getLocation().toVector().subtract(context.caster().getLocation().toVector())
                .normalize();
        Vector knockback = direction.multiply(0.8); // Moderate knockback
        entity.setVelocity(entity.getVelocity().add(knockback));
    }

    @Override
    protected void createWaveParticles(@NotNull SpellContext context, @NotNull org.bukkit.Location center,
            double radius) {
        // Create electric spark particles
        for (int i = 0; i < config.particleCount; i++) {
            double angle = 2 * Math.PI * i / config.particleCount;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;

            // Create particles at multiple heights
            for (int y = 0; y <= 2; y++) {
                org.bukkit.Location particleLoc = new org.bukkit.Location(center.getWorld(), x, center.getY() + y, z);

                // Use electric spark particles
                context.fx().spawnParticles(particleLoc, Particle.ELECTRIC_SPARK, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }
}
