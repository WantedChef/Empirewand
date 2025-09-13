package nl.wantedchef.empirewand.common.visual;

import nl.wantedchef.empirewand.framework.service.FxService;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;

/**
 * Defines different wave effect types with their unique visual signatures,
 * sound effects, and gameplay mechanics.
 */
public enum WaveEffectType {
    
    BLOOD(
        // Visual Effects
        Particle.DUST,              // Main particle (red dust)
        Particle.DRIPPING_LAVA,     // Trail particle
        Particle.CLOUD,             // Impact particle
        
        // Audio Effects
        Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,  // Launch sound
        Sound.ENTITY_ZOMBIE_HURT,              // Impact sound
        
        // Colors (RGB values for colored particles)
        new double[]{0.8, 0.1, 0.1},  // Dark red
        new double[]{0.6, 0.0, 0.0}   // Deep crimson
    ),
    
    POISON(
        // Visual Effects
        Particle.ITEM_SLIME,        // Main particle
        Particle.WITCH,             // Trail particle (green)
        Particle.SNEEZE,            // Impact particle
        
        // Audio Effects
        Sound.ENTITY_SPIDER_HURT,   // Launch sound
        Sound.ENTITY_WITCH_THROW,   // Impact sound
        
        // Colors
        new double[]{0.2, 0.8, 0.1},  // Toxic green
        new double[]{0.1, 0.6, 0.0}   // Dark green
    ),
    
    FLAME(
        // Visual Effects
        Particle.FLAME,             // Main particle
        Particle.LAVA,              // Trail particle
        Particle.EXPLOSION,         // Impact particle
        
        // Audio Effects
        Sound.ENTITY_BLAZE_SHOOT,   // Launch sound
        Sound.ENTITY_GENERIC_EXPLODE, // Impact sound
        
        // Colors
        new double[]{1.0, 0.5, 0.1},  // Orange-red
        new double[]{1.0, 0.8, 0.0}   // Yellow
    ),
    
    ICE(
        // Visual Effects
        Particle.SNOWFLAKE,         // Main particle
        Particle.CLOUD,             // Trail particle (white)
        Particle.SNOWFLAKE,         // Impact particle (replaces deprecated BLOCK_CRACK)
        
        // Audio Effects
        Sound.BLOCK_GLASS_BREAK,    // Launch sound
        Sound.ENTITY_PLAYER_HURT_FREEZE, // Impact sound
        
        // Colors
        new double[]{0.7, 0.9, 1.0},  // Ice blue
        new double[]{0.9, 0.9, 1.0}   // Pale white
    ),
    
    LIGHTNING(
        // Visual Effects
        Particle.END_ROD,           // Main particle
        Particle.ELECTRIC_SPARK,    // Trail particle
        Particle.FLASH,             // Impact particle
        
        // Audio Effects
        Sound.ENTITY_LIGHTNING_BOLT_THUNDER, // Launch sound
        Sound.ENTITY_LIGHTNING_BOLT_IMPACT,  // Impact sound
        
        // Colors
        new double[]{0.8, 0.8, 1.0},  // Electric blue
        new double[]{1.0, 1.0, 0.8}   // Bright white
    );
    
    private final Particle mainParticle;
    private final Particle trailParticle;
    private final Particle impactParticle;
    private final Sound launchSound;
    private final Sound impactSound;
    private final double[] primaryColor;
    private final double[] secondaryColor;
    
    WaveEffectType(Particle mainParticle, Particle trailParticle, Particle impactParticle,
                   Sound launchSound, Sound impactSound, 
                   double[] primaryColor, double[] secondaryColor) {
        this.mainParticle = mainParticle;
        this.trailParticle = trailParticle;
        this.impactParticle = impactParticle;
        this.launchSound = launchSound;
        this.impactSound = impactSound;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }
    
    /**
     * Creates trail effects for a wave projectile
     */
    public void createTrailEffect(@NotNull Location location, @NotNull FxService fxService, double density) {
        int particleCount = (int) (3 * density);
        
        switch (this) {
            case BLOOD:
                // Blood drops and dark mist
                fxService.spawnParticles(location, trailParticle, particleCount, 0.1, 0.1, 0.1, 0.0);
                fxService.spawnParticles(location, Particle.CLOUD, 1, 0.2, 0.2, 0.2, 0.02);
                break;
                
            case POISON:
                // Toxic bubbles and green mist
                fxService.spawnParticles(location, trailParticle, particleCount, 0.15, 0.15, 0.15, 0.0);
                fxService.spawnParticles(location, Particle.ITEM_SLIME, 1, 0.1, 0.1, 0.1, 0.0);
                break;
                
            case FLAME:
                // Fire trail with ember sparks
                fxService.spawnParticles(location, trailParticle, particleCount, 0.1, 0.1, 0.1, 0.0);
                fxService.spawnParticles(location, Particle.LAVA, 1, 0.2, 0.2, 0.2, 0.0);
                // Heat shimmer effect
                fxService.spawnParticles(location, Particle.FLAME, 2, 0.3, 0.3, 0.3, 0.01);
                break;
                
            case ICE:
                // Frost crystals and cold mist
                fxService.spawnParticles(location, trailParticle, particleCount, 0.2, 0.2, 0.2, 0.02);
                fxService.spawnParticles(location, Particle.SNOWFLAKE, 2, 0.3, 0.3, 0.3, 0.0);
                break;
                
            case LIGHTNING:
                // Electric sparks and energy
                fxService.spawnParticles(location, trailParticle, particleCount, 0.1, 0.1, 0.1, 0.1);
                fxService.spawnParticles(location, Particle.END_ROD, 1, 0.2, 0.2, 0.2, 0.05);
                break;
        }
    }
    
    /**
     * Creates impact effects when a wave projectile hits something
     */
    public void createImpactEffect(@NotNull Location location, @NotNull FxService fxService, double density) {
        int particleCount = (int) (15 * density);
        
        switch (this) {
            case BLOOD:
                // Blood explosion with dark clouds
                fxService.spawnParticles(location, impactParticle, particleCount, 0.5, 0.5, 0.5, 0.2);
                fxService.spawnParticles(location, Particle.DUST, particleCount * 2, 0.8, 0.8, 0.8, 0.1);
                fxService.spawnParticles(location, Particle.DRIPPING_LAVA, 8, 0.3, 0.3, 0.3, 0.0);
                break;
                
            case POISON:
                // Toxic cloud burst
                fxService.spawnParticles(location, impactParticle, particleCount, 0.6, 0.6, 0.6, 0.1);
                fxService.spawnParticles(location, Particle.ITEM_SLIME, particleCount, 0.7, 0.7, 0.7, 0.2);
                fxService.spawnParticles(location, Particle.WITCH, 10, 0.5, 0.5, 0.5, 0.0);
                break;
                
            case FLAME:
                // Fiery explosion with ember shower
                fxService.spawnParticles(location, impactParticle, 1, 0.0, 0.0, 0.0, 0.0);
                fxService.spawnParticles(location, Particle.FLAME, particleCount * 2, 0.8, 0.8, 0.8, 0.3);
                fxService.spawnParticles(location, Particle.LAVA, particleCount, 1.0, 1.0, 1.0, 0.0);
                fxService.spawnParticles(location, Particle.SMOKE, 8, 0.5, 0.5, 0.5, 0.1);
                break;
                
            case ICE:
                // Ice crystal burst with frost shards
                fxService.spawnParticles(location, Particle.SNOWFLAKE, particleCount * 2, 0.7, 0.7, 0.7, 0.2);
                fxService.spawnParticles(location, impactParticle, particleCount, 0.5, 0.5, 0.5, 0.1);
                fxService.spawnParticles(location, Particle.CLOUD, 6, 0.4, 0.4, 0.4, 0.02);
                break;
                
            case LIGHTNING:
                // Electric discharge with sparks
                fxService.spawnParticles(location, impactParticle, 1, 0.0, 0.0, 0.0, 0.0);
                fxService.spawnParticles(location, Particle.ELECTRIC_SPARK, particleCount * 2, 0.8, 0.8, 0.8, 0.3);
                fxService.spawnParticles(location, Particle.END_ROD, particleCount, 0.6, 0.6, 0.6, 0.2);
                break;
        }
    }
    
    /**
     * Creates special effects when a wave hits an entity
     */
    public void createEntityHitEffect(@NotNull Location location, @NotNull FxService fxService, double density) {
        int particleCount = (int) (8 * density);
        
        switch (this) {
            case BLOOD:
                // Blood spurt from the entity
                fxService.spawnParticles(location.clone().add(0, 1, 0), Particle.DUST, particleCount, 0.3, 0.5, 0.3, 0.2);
                fxService.spawnParticles(location.clone().add(0, 0.5, 0), Particle.DRIPPING_LAVA, 4, 0.2, 0.2, 0.2, 0.0);
                break;
                
            case POISON:
                // Poison bubbles around the entity
                fxService.spawnParticles(location.clone().add(0, 1, 0), Particle.ITEM_SLIME, particleCount, 0.4, 0.6, 0.4, 0.1);
                fxService.spawnParticles(location.clone().add(0, 0.5, 0), Particle.SNEEZE, 6, 0.3, 0.3, 0.3, 0.0);
                break;
                
            case FLAME:
                // Flames engulf the entity briefly
                fxService.spawnParticles(location.clone().add(0, 1, 0), Particle.FLAME, particleCount, 0.3, 0.6, 0.3, 0.2);
                fxService.spawnParticles(location.clone().add(0, 0.5, 0), Particle.SMOKE, 4, 0.2, 0.4, 0.2, 0.05);
                break;
                
            case ICE:
                // Frost crystals form around the entity
                fxService.spawnParticles(location.clone().add(0, 1, 0), Particle.SNOWFLAKE, particleCount, 0.4, 0.6, 0.4, 0.1);
                fxService.spawnParticles(location.clone().add(0, 0.5, 0), Particle.CLOUD, 3, 0.3, 0.3, 0.3, 0.02);
                break;
                
            case LIGHTNING:
                // Electric arcs around the entity
                fxService.spawnParticles(location.clone().add(0, 1, 0), Particle.ELECTRIC_SPARK, particleCount, 0.4, 0.6, 0.4, 0.3);
                fxService.spawnParticles(location.clone().add(0, 0.5, 0), Particle.END_ROD, 3, 0.2, 0.4, 0.2, 0.1);
                break;
        }
    }
    
    /**
     * Applies type-specific effects to a hit entity
     */
    public void applyEffects(@NotNull LivingEntity target, @NotNull Player caster, @NotNull FxService fxService) {
        switch (this) {
            case BLOOD:
                // Life drain - heal caster for 30% of damage dealt
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 0, false, true));
                
                // Heal caster (life steal effect)
                double healAmount = 2.0; // 30% of typical damage
                if (caster.getHealth() + healAmount <= caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()) {
                    caster.setHealth(caster.getHealth() + healAmount);
                }
                
                // Visual feedback for life steal
                fxService.spawnParticles(caster.getLocation().add(0, 1, 0), Particle.HEART, 3, 0.3, 0.3, 0.3, 0.0);
                break;
                
            case POISON:
                // Multi-layered poison effects
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 140, 1, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 160, 0, false, true));
                
                // Lingering poison cloud
                createLingeringPoisonCloud(target.getLocation(), fxService);
                break;
                
            case FLAME:
                // Fire effects
                target.setFireTicks(100); // 5 seconds of fire
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0, false, true));
                
                // Chance to ignite nearby blocks (if not protected)
                igniteNearbyBlocks(target.getLocation());
                break;
                
            case ICE:
                // Freezing effects
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 2, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 1, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, false, true));
                
                // Temporary ice blocks around target
                createTemporaryIceStructure(target.getLocation());
                break;
                
            case LIGHTNING:
                // Electric shock effects
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
                
                // Chain lightning to nearby enemies
                chainLightningToNearbyEnemies(target, caster, fxService);
                break;
        }
    }
    
    private void createLingeringPoisonCloud(Location location, FxService fxService) {
        // Create a lingering poison effect that damages enemies who walk through
        // This would typically involve creating a temporary area effect
        for (int i = 0; i < 20; i++) {
            final int tick = i;
            // Schedule particle effects over time
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                (org.bukkit.plugin.Plugin) fxService, 
                () -> fxService.spawnParticles(location, Particle.ITEM_SLIME, 2, 1.0, 0.5, 1.0, 0.02),
                tick * 4L
            );
        }
    }
    
    private void igniteNearbyBlocks(Location location) {
        // Safely ignite nearby flammable blocks (respecting protection plugins)
        // Implementation would check for flammable blocks and set them on fire
        // This is a placeholder for the actual implementation
    }
    
    private void createTemporaryIceStructure(Location location) {
        // Create temporary ice blocks around the target location
        // Implementation would create temporary blocks that disappear after a time
        // This is a placeholder for the actual implementation
    }
    
    private void chainLightningToNearbyEnemies(LivingEntity originalTarget, Player caster, FxService fxService) {
        // Chain lightning effect that jumps to nearby enemies
        Collection<org.bukkit.entity.Entity> nearbyEntities = originalTarget.getWorld()
            .getNearbyEntities(originalTarget.getLocation(), 5, 5, 5);
            
        int chainCount = 0;
        for (org.bukkit.entity.Entity entity : nearbyEntities) {
            if (chainCount >= 3) break; // Limit chain count
            if (!(entity instanceof LivingEntity)) continue;
            if (entity.equals(originalTarget)) continue;
            if (entity.equals(caster)) continue;
            
            LivingEntity chainTarget = (LivingEntity) entity;
            
            // Visual chain effect
            drawLightningArc(originalTarget.getLocation(), chainTarget.getLocation(), fxService);
            
            // Apply reduced damage
            chainTarget.damage(3.0, caster);
            chainTarget.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, true));
            
            chainCount++;
        }
    }
    
    private void drawLightningArc(Location from, Location to, FxService fxService) {
        // Draw particle line between two points to simulate lightning arc
        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();
        
        for (double i = 0; i < distance; i += 0.3) {
            Location particleLoc = from.clone().add(direction.clone().multiply(i));
            
            // Add random offset for lightning effect
            particleLoc.add(
                (Math.random() - 0.5) * 0.3,
                (Math.random() - 0.5) * 0.3,
                (Math.random() - 0.5) * 0.3
            );
            
            fxService.spawnParticles(particleLoc, Particle.ELECTRIC_SPARK, 1, 0.0, 0.0, 0.0, 0.1);
        }
    }
    
    // Getters for accessing effect properties
    public Particle getMainParticle() { return mainParticle; }
    public Particle getTrailParticle() { return trailParticle; }
    public Particle getImpactParticle() { return impactParticle; }
    public Sound getLaunchSound() { return launchSound; }
    public Sound getImpactSound() { return impactSound; }
    public double[] getPrimaryColor() { return primaryColor.clone(); }
    public double[] getSecondaryColor() { return secondaryColor.clone(); }
}