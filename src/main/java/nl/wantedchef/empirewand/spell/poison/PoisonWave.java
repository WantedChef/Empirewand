package nl.wantedchef.empirewand.spell.poison;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.common.visual.WaveEffectType;
import nl.wantedchef.empirewand.common.visual.WaveProjectile;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.enhanced.EnhancedWaveSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Enhanced Poison Wave - A toxic wave of projectiles that creates lingering poison clouds
 * and applies multiple debuffs to enemies for battlefield control.
 * 
 * Features:
 * - Arc formation for maximum area coverage
 * - Multi-layered poison effects (poison, nausea, weakness)
 * - Lingering poison clouds for area denial
 * - Moderate piercing capability
 * - Toxic green particle trails with bubbling effects
 */
public class PoisonWave extends EnhancedWaveSpell {

    public static class Builder extends EnhancedWaveSpell.Builder {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Poison Wave";
            this.description = "Unleashes a toxic wave that poisons enemies and creates lingering poison clouds.";
            this.cooldown = Duration.ofSeconds(14);
            this.spellType = SpellType.POISON;

            // Configure enhanced wave properties for poison magic
            this.formation = WaveProjectile.WaveFormation.ARC;
            this.effectType = WaveEffectType.POISON;
            this.speed = 1.1; // Fast spread for area denial
            this.maxDistance = 20.0; // Good coverage range
            this.projectileCount = 6; // Wide arc coverage
            this.damage = 6.0; // Moderate damage, focus on debuffs
            this.lifetimeTicks = 100; // 5 seconds
            this.hitRadius = 2.5; // Good area effect
            this.pierceEntities = true; // Pierce for area control
            this.maxPierces = 1; // Limited piercing to balance
            this.particleDensity = 1.3; // Rich toxic effects
            this.enableScreenShake = false; // Less dramatic than blood magic
            this.screenShakeIntensity = 0.3; // Subtle effect
        }

        @Override
        public @NotNull EnhancedWaveSpell build() {
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
}
