package nl.wantedchef.empirewand.spell.lightning.advanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.common.visual.WaveEffectType;
import nl.wantedchef.empirewand.common.visual.WaveProjectile;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.enhanced.destructive.EnhancedWaveSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Enhanced Lightning Wave - An explosive burst of electric projectiles that chain lightning
 * between enemies and create stunning visual effects with intense screen shake.
 * 
 * Features:
 * - Burst formation for maximum chaos and impact
 * - Electric particle effects with sparks and energy arcs
 * - Chain lightning that jumps between nearby enemies
 * - Applies glowing effect to mark targets
 * - High speed for instant impact
 * - Intense screen shake for thunderous impact
 */
public class LightningWave extends EnhancedWaveSpell {

    public static class Builder extends EnhancedWaveSpell.Builder {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lightning Wave";
            this.description = "Unleashes an explosive burst of lightning that chains between enemies.";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.LIGHTNING;

            // Configure enhanced wave properties for lightning magic
            this.formation = WaveProjectile.WaveFormation.BURST;
            this.effectType = WaveEffectType.LIGHTNING;
            this.speed = 1.8; // Very fast, instant impact
            this.maxDistance = 18.0; // Moderate range for burst
            this.projectileCount = 10; // Many projectiles for chaos
            this.damage = 7.5; // Good damage + chain effects
            this.lifetimeTicks = 60; // 3 seconds, fast impact
            this.hitRadius = 2.0; // Standard hit radius
            this.pierceEntities = false; // No piercing, focus on chain
            this.maxPierces = 0; // Chain lightning instead
            this.particleDensity = 1.5; // Intense electric effects
            this.enableScreenShake = true; // Thunder impact
            this.screenShakeIntensity = 0.9; // Very intense shake
        }

        @Override
        public @NotNull EnhancedWaveSpell build() {
            return new LightningWave(this);
        }
    }

    private LightningWave(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "lightning-wave";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }
}