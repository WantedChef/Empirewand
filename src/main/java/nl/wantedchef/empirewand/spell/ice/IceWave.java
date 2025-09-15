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
            this.speed = 1.0; // Slightly faster for punchier feel
            this.maxDistance = 32.0; // Longer precise range
            this.projectileCount = 5; // Slightly denser line
            this.damage = 5.5; // Slight bump
            this.lifetimeTicks = 150; // Maintain full range with added distance
            this.hitRadius = 2.4; // Slightly wider for reliability
            this.pierceEntities = true; // Excellent piercing
            this.maxPierces = 4; // More crowd control
            this.particleDensity = 1.2; // Slightly richer ice effects
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