package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.common.visual.WaveProjectile;
import nl.wantedchef.empirewand.common.visual.WaveEffectType;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enhanced wave spell base class that uses the new projectile-based wave system.
 * This provides spectacular visual effects with formation movement, particle trails,
 * and coordinated group behavior that's far superior to the old circular wave system.
 */
public abstract class EnhancedWaveSpell extends Spell<WaveProjectile> {

    protected WaveConfig waveConfig;
    
    /**
     * Configuration for enhanced wave spells
     */
    public static class WaveConfig {
        public final WaveProjectile.WaveFormation formation;
        public final WaveEffectType effectType;
        public final double speed;
        public final double maxDistance;
        public final int projectileCount;
        public final double damage;
        public final int lifetimeTicks;
        public final double hitRadius;
        public final boolean pierceEntities;
        public final int maxPierces;
        public final double particleDensity;
        public final boolean enableScreenShake;
        public final double screenShakeIntensity;
        
        public WaveConfig(WaveProjectile.WaveFormation formation, WaveEffectType effectType, 
                         double speed, double maxDistance, int projectileCount, double damage,
                         int lifetimeTicks, double hitRadius, boolean pierceEntities, int maxPierces,
                         double particleDensity, boolean enableScreenShake, double screenShakeIntensity) {
            this.formation = formation;
            this.effectType = effectType;
            this.speed = speed;
            this.maxDistance = maxDistance;
            this.projectileCount = projectileCount;
            this.damage = damage;
            this.lifetimeTicks = lifetimeTicks;
            this.hitRadius = hitRadius;
            this.pierceEntities = pierceEntities;
            this.maxPierces = maxPierces;
            this.particleDensity = particleDensity;
            this.enableScreenShake = enableScreenShake;
            this.screenShakeIntensity = screenShakeIntensity;
        }
    }

    /**
     * Builder for enhanced wave spells with fluent configuration
     */
    public abstract static class Builder extends Spell.Builder<WaveProjectile> {
        protected WaveProjectile.WaveFormation formation = WaveProjectile.WaveFormation.SINE_WAVE;
        protected WaveEffectType effectType = WaveEffectType.BLOOD;
        protected double speed = 1.2;
        protected double maxDistance = 25.0;
        protected int projectileCount = 7;
        protected double damage = 8.0;
        protected int lifetimeTicks = 120;
        protected double hitRadius = 2.5;
        protected boolean pierceEntities = true;
        protected int maxPierces = 2;
        protected double particleDensity = 1.0;
        protected boolean enableScreenShake = true;
        protected double screenShakeIntensity = 0.6;
        
        protected Builder(@Nullable EmpireWandAPI api) {
            super(api);
        }
        
        public Builder formation(WaveProjectile.WaveFormation formation) { 
            this.formation = formation; 
            return this; 
        }
        
        public Builder effectType(WaveEffectType effectType) { 
            this.effectType = effectType; 
            return this; 
        }
        
        public Builder speed(double speed) { 
            this.speed = speed; 
            return this; 
        }
        
        public Builder maxDistance(double maxDistance) { 
            this.maxDistance = maxDistance; 
            return this; 
        }
        
        public Builder projectileCount(int projectileCount) { 
            this.projectileCount = projectileCount; 
            return this; 
        }
        
        public Builder damage(double damage) { 
            this.damage = damage; 
            return this; 
        }
        
        public Builder lifetimeTicks(int lifetimeTicks) { 
            this.lifetimeTicks = lifetimeTicks; 
            return this; 
        }
        
        public Builder hitRadius(double hitRadius) { 
            this.hitRadius = hitRadius; 
            return this; 
        }
        
        public Builder pierceEntities(boolean pierceEntities) { 
            this.pierceEntities = pierceEntities; 
            return this; 
        }
        
        public Builder maxPierces(int maxPierces) { 
            this.maxPierces = maxPierces; 
            return this; 
        }
        
        public Builder particleDensity(double particleDensity) { 
            this.particleDensity = particleDensity; 
            return this; 
        }
        
        public Builder enableScreenShake(boolean enableScreenShake) { 
            this.enableScreenShake = enableScreenShake; 
            return this; 
        }
        
        public Builder screenShakeIntensity(double screenShakeIntensity) { 
            this.screenShakeIntensity = screenShakeIntensity; 
            return this; 
        }
    }

    protected EnhancedWaveSpell(Builder builder) {
        super(builder);
        this.waveConfig = new WaveConfig(
            builder.formation, builder.effectType, builder.speed, builder.maxDistance,
            builder.projectileCount, builder.damage, builder.lifetimeTicks, builder.hitRadius,
            builder.pierceEntities, builder.maxPierces, builder.particleDensity,
            builder.enableScreenShake, builder.screenShakeIntensity
        );
    }

    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        super.loadConfig(spellConfig);
        
        // Load wave configuration from YAML
        WaveProjectile.WaveFormation formation = WaveProjectile.WaveFormation.valueOf(
            spellConfig.getString("wave.formation", waveConfig.formation.name())
        );
        
        double speed = spellConfig.getDouble("wave.speed", waveConfig.speed);
        double maxDistance = spellConfig.getDouble("wave.max_distance", waveConfig.maxDistance);
        int projectileCount = spellConfig.getInt("wave.projectile_count", waveConfig.projectileCount);
        double damage = spellConfig.getDouble("wave.damage", waveConfig.damage);
        int lifetimeTicks = spellConfig.getInt("wave.lifetime_ticks", waveConfig.lifetimeTicks);
        double hitRadius = spellConfig.getDouble("wave.hit_radius", waveConfig.hitRadius);
        boolean pierceEntities = spellConfig.getBoolean("wave.pierce_entities", waveConfig.pierceEntities);
        int maxPierces = spellConfig.getInt("wave.max_pierces", waveConfig.maxPierces);
        double particleDensity = spellConfig.getDouble("wave.particle_density", waveConfig.particleDensity);
        boolean enableScreenShake = spellConfig.getBoolean("wave.enable_screen_shake", waveConfig.enableScreenShake);
        double screenShakeIntensity = spellConfig.getDouble("wave.screen_shake_intensity", waveConfig.screenShakeIntensity);
        
        this.waveConfig = new WaveConfig(
            formation, waveConfig.effectType, speed, maxDistance, projectileCount, damage,
            lifetimeTicks, hitRadius, pierceEntities, maxPierces, particleDensity,
            enableScreenShake, screenShakeIntensity
        );
    }

    @Override
    @Nullable
    protected WaveProjectile executeSpell(@NotNull SpellContext context) {
        // Create the enhanced wave projectile configuration
        WaveProjectile.WaveConfig projectileConfig = new WaveProjectile.WaveConfig.Builder()
            .formation(waveConfig.formation)
            .effectType(waveConfig.effectType)
            .speed(waveConfig.speed)
            .maxDistance(waveConfig.maxDistance)
            .projectileCount(waveConfig.projectileCount)
            .damage(waveConfig.damage)
            .lifetimeTicks(waveConfig.lifetimeTicks)
            .hitRadius(waveConfig.hitRadius)
            .pierceEntities(waveConfig.pierceEntities)
            .maxPierces(waveConfig.maxPierces)
            .particleDensity(waveConfig.particleDensity)
            .enableScreenShake(waveConfig.enableScreenShake)
            .screenShakeIntensity(waveConfig.screenShakeIntensity)
            .build();
        
        // Create and launch the wave
        WaveProjectile wave = new WaveProjectile(
            context.plugin(), 
            context.fx(), 
            context.caster(), 
            projectileConfig
        );
        
        wave.launch();
        return wave;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull WaveProjectile wave) {
        // The wave handles its own effects through the projectile system
        // We can add additional handling here if needed
        
        // Example: Log spell usage for metrics
        context.plugin().getLogger().info(String.format(
            "Enhanced wave spell '%s' launched by %s with %d projectiles",
            key(), context.caster().getName(), wave.getActiveProjectileCount()
        ));
    }

    @Override
    public boolean requiresAsyncExecution() {
        // Wave spells don't need async execution since they use scheduled tasks
        return false;
    }
    
    /**
     * Get the wave configuration for this spell
     */
    public WaveConfig getWaveConfig() {
        return waveConfig;
    }
}