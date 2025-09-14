package nl.wantedchef.empirewand.spell.fire.advanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.common.visual.WaveEffectType;
import nl.wantedchef.empirewand.common.visual.WaveProjectile;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.enhanced.destructive.EnhancedWaveSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Enhanced Flame Wave - A spectacular spiral of fire that creates intense visual effects
 * and ignites the battlefield while dealing high damage to enemies.
 * 
 * Features:
 * - Spiral formation for maximum visual impact and dramatic flair
 * - Fire effects with ember trails and heat shimmer
 * - Sets enemies on fire for persistent damage
 * - Can ignite nearby flammable blocks (respects protection)
 * - High damage output with moderate piercing
 * - Intense screen shake for dramatic impact
 */
public class FlameWave extends EnhancedWaveSpell {

    public static class Builder extends EnhancedWaveSpell.Builder {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Flame Wave";
            this.description = "Unleashes a devastating spiral of fire that ignites enemies and the battlefield.";
            this.cooldown = Duration.ofSeconds(16);
            this.spellType = SpellType.FIRE;

            // Configure enhanced wave properties for flame magic
            this.formation = WaveProjectile.WaveFormation.SPIRAL;
            this.effectType = WaveEffectType.FLAME;
            this.speed = 1.3; // Fast and aggressive
            this.maxDistance = 25.0; // Long range for area control
            this.projectileCount = 8; // Dense spiral
            this.damage = 9.0; // High fire damage
            this.lifetimeTicks = 120; // 6 seconds for full spiral
            this.hitRadius = 3.0; // Large blast radius
            this.pierceEntities = true; // Pierce through enemies
            this.maxPierces = 1; // Moderate piercing
            this.particleDensity = 1.4; // Rich fire effects
            this.enableScreenShake = true; // Dramatic explosions
            this.screenShakeIntensity = 0.8; // Intense shake for fire magic
        }

        @Override
        public @NotNull EnhancedWaveSpell build() {
            return new FlameWave(this);
        }
    }

    private FlameWave(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "flame-wave";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }
}