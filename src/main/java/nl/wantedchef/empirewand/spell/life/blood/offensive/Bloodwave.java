package nl.wantedchef.empirewand.spell.life.blood.offensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.base.SpellContext;
import nl.wantedchef.empirewand.spell.base.SpellType;
import nl.wantedchef.empirewand.spell.misc.utility.WaveSpell;
import nl.wantedchef.empirewand.spell.base.PrereqInterface;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Bloodwave - A wave of blood that damages enemies and applies bleeding
 * effects.
 */
public class Bloodwave extends WaveSpell {

    public static class Builder extends WaveSpell.Builder {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Bloodwave";
            this.description = "Unleashes a wave of blood that damages and bleeds enemies.";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.LIFE;

            // Configure wave properties
            this.waveConfig = new WaveConfig(
                    0.3, // wave speed
                    8.0, // max radius
                    6.0, // damage
                    24, // particle count
                    "REDSTONE", // particle type
                    "ENTITY_ZOMBIE_ATTACK_IRON_DOOR", // sound
                    0.8f, // volume
                    1.0f, // pitch
                    60, // duration ticks (3 seconds)
                    1.5 // entity effect radius
            );
        }

        @Override
        public @NotNull WaveSpell build() {
            return new Bloodwave(this);
        }
    }

    private Bloodwave(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "bloodwave";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void applyWaveEffectToEntity(@NotNull SpellContext context, @NotNull LivingEntity entity,
            double distance) {
        // Skip friendly entities - check if it's the caster or friendly fire is
        // disabled
        if (entity instanceof Player && entity.equals(context.caster())) {
            return;
        }

        // Apply damage
        entity.damage(config.damage, context.caster());

        // Apply bleeding effect (wither)
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.WITHER,
                100, // 5 seconds
                0, // Level 1
                false,
                true));

        // Apply weakness
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS,
                120, // 6 seconds
                0, // Level 1
                false,
                true));

        // Create blood particle effect on the entity
        context.fx().spawnParticles(entity.getLocation(), Particle.CLOUD, 8, 0.3, 0.3, 0.3, 0.1);
    }

    @Override
    protected void createWaveParticles(@NotNull SpellContext context, @NotNull Location center, double radius) {
        // Create blood-red particles
        for (int i = 0; i < config.particleCount; i++) {
            double angle = 2 * Math.PI * i / config.particleCount;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;

            // Create particles at ground level and slightly above
            for (int y = 0; y <= 2; y++) {
                Location particleLoc = new Location(center.getWorld(), x, center.getY() + y, z);
                context.fx().spawnParticles(particleLoc, Particle.CLOUD, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }
}
