package nl.wantedchef.empirewand.spell.life;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.common.visual.WaveEffectType;
import nl.wantedchef.empirewand.common.visual.WaveProjectile;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.enhanced.EnhancedWaveSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Enhanced Bloodwave - A spectacular wave of blood projectiles that damages enemies,
 * applies bleeding effects, and provides life steal to the caster.
 * 
 * Features:
 * - Sine wave formation for maximum coverage and visual impact
 * - Life drain mechanics (heals caster for damage dealt)
 * - Blood particle trails and dramatic impact effects
 * - Pierces through multiple enemies
 * - Screen shake effects for immersive gameplay
 */
public class Bloodwave extends EnhancedWaveSpell {

    public static class Builder extends EnhancedWaveSpell.Builder {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Bloodwave";
            this.description = "Unleashes a devastating wave of blood projectiles that drain life from enemies.";
            this.cooldown = Duration.ofSeconds(18);
            this.spellType = SpellType.LIFE;

            // Configure enhanced wave properties for blood magic
            this.formation = WaveProjectile.WaveFormation.SINE_WAVE;
            this.effectType = WaveEffectType.BLOOD;
            this.speed = 1.0; // Moderate speed for dramatic effect
            this.maxDistance = 22.0; // Good range for life magic
            this.projectileCount = 5; // Focused wave
            this.damage = 8.0; // High damage for life drain
            this.lifetimeTicks = 110; // 5.5 seconds
            this.hitRadius = 2.8; // Large hit radius
            this.pierceEntities = true; // Pierce through enemies
            this.maxPierces = 2; // Can hit up to 3 enemies per projectile
            this.particleDensity = 1.2; // Rich blood effects
            this.enableScreenShake = true; // Dramatic impact
            this.screenShakeIntensity = 0.7; // Strong shake for dark magic
        }

        @Override
        public @NotNull EnhancedWaveSpell build() {
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
}
