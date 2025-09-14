package nl.wantedchef.empirewand.spell.ice;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.common.visual.WaveEffectType;
import nl.wantedchef.empirewand.common.visual.WaveProjectile;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.enhanced.destructive.EnhancedWaveSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Enhanced Ice Wave - A beautiful line of frost projectiles that creates crystalline
 * ice structures and applies powerful slowing debuffs for battlefield control.
 * 
 * Features:
 * - Line formation for precise targeting and maximum control
 * - Ice crystal particles with frost trail effects
 * - Powerful slowing effects (slowness, mining fatigue, weakness)
 * - Creates temporary ice structures around hit targets
 * - Excellent piercing capability for crowd control
 * - Beautiful crystalline visual effects with subtle screen effects
 */
public class IceWave extends EnhancedWaveSpell {

    public static class Builder extends EnhancedWaveSpell.Builder {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Ice Wave";
            this.description = "Launches a precise line of frost that slows enemies and creates ice structures.";
            this.cooldown = Duration.ofSeconds(13);
            this.spellType = SpellType.ICE;

            // Configure enhanced wave properties for ice magic
            this.formation = WaveProjectile.WaveFormation.LINE;
            this.effectType = WaveEffectType.ICE;
            this.speed = 0.9; // Slower, more controlled
            this.maxDistance = 28.0; // Long precise range
            this.projectileCount = 4; // Focused line
            this.damage = 5.0; // Lower damage, focus on control
            this.lifetimeTicks = 140; // 7 seconds for full range
            this.hitRadius = 2.2; // Moderate hit radius
            this.pierceEntities = true; // Excellent piercing
            this.maxPierces = 3; // Can hit up to 4 enemies per projectile
            this.particleDensity = 1.1; // Beautiful ice effects
            this.enableScreenShake = false; // Subtle, controlled magic
            this.screenShakeIntensity = 0.2; // Minimal shake
        }

        @Override
        public @NotNull EnhancedWaveSpell build() {
            return new IceWave(this);
        }
    }

    private IceWave(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "ice-wave";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }
}