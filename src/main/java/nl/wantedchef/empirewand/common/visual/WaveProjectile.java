package nl.wantedchef.empirewand.common.visual;

import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.framework.service.FxService;
import org.bukkit.Location;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced projectile-based wave system that creates spectacular visual effects
 * with formation movement, particle trails, and coordinated group behavior.
 */
public class WaveProjectile {

    private final Plugin plugin;
    private final FxService fxService;
    private final WaveConfig config;
    private final List<SmallFireball> projectiles;
    private final Map<UUID, ProjectileData> projectileData;
    private final Set<UUID> hitEntities;
    private final Player caster;
    private final Location origin;
    private BukkitRunnable waveTask;
    private int tickCount = 0;

    /**
     * Configuration for wave projectile behavior and visual effects
     */
    public static class WaveConfig {
        public final WaveFormation formation;
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
        
        private WaveConfig(Builder builder) {
            this.formation = builder.formation;
            this.effectType = builder.effectType;
            this.speed = builder.speed;
            this.maxDistance = builder.maxDistance;
            this.projectileCount = builder.projectileCount;
            this.damage = builder.damage;
            this.lifetimeTicks = builder.lifetimeTicks;
            this.hitRadius = builder.hitRadius;
            this.pierceEntities = builder.pierceEntities;
            this.maxPierces = builder.maxPierces;
            this.particleDensity = builder.particleDensity;
            this.enableScreenShake = builder.enableScreenShake;
            this.screenShakeIntensity = builder.screenShakeIntensity;
        }
        
        public static class Builder {
            private WaveFormation formation = WaveFormation.LINE;
            private WaveEffectType effectType = WaveEffectType.BLOOD;
            private double speed = 1.0;
            private double maxDistance = 20.0;
            private int projectileCount = 5;
            private double damage = 6.0;
            private int lifetimeTicks = 100;
            private double hitRadius = 2.0;
            private boolean pierceEntities = false;
            private int maxPierces = 3;
            private double particleDensity = 1.0;
            private boolean enableScreenShake = true;
            private double screenShakeIntensity = 0.5;
            
            public Builder formation(WaveFormation formation) { this.formation = formation; return this; }
            public Builder effectType(WaveEffectType effectType) { this.effectType = effectType; return this; }
            public Builder speed(double speed) { this.speed = speed; return this; }
            public Builder maxDistance(double maxDistance) { this.maxDistance = maxDistance; return this; }
            public Builder projectileCount(int projectileCount) { this.projectileCount = projectileCount; return this; }
            public Builder damage(double damage) { this.damage = damage; return this; }
            public Builder lifetimeTicks(int lifetimeTicks) { this.lifetimeTicks = lifetimeTicks; return this; }
            public Builder hitRadius(double hitRadius) { this.hitRadius = hitRadius; return this; }
            public Builder pierceEntities(boolean pierceEntities) { this.pierceEntities = pierceEntities; return this; }
            public Builder maxPierces(int maxPierces) { this.maxPierces = maxPierces; return this; }
            public Builder particleDensity(double particleDensity) { this.particleDensity = particleDensity; return this; }
            public Builder enableScreenShake(boolean enableScreenShake) { this.enableScreenShake = enableScreenShake; return this; }
            public Builder screenShakeIntensity(double screenShakeIntensity) { this.screenShakeIntensity = screenShakeIntensity; return this; }
            
            public WaveConfig build() { return new WaveConfig(this); }
        }
    }
    
    /**
     * Wave formation patterns that determine projectile positioning and movement
     */
    public enum WaveFormation {
        LINE,           // Straight line formation
        ARC,            // Expanding arc formation
        SINE_WAVE,      // Sine wave pattern
        SPIRAL,         // Spiral formation
        BURST,          // Radial burst formation
        CHEVRON         // V-shaped formation
    }
    
    /**
     * Internal data for tracking individual projectile state
     */
    private static class ProjectileData {
        final Vector originalDirection;
        final double formationOffset;
        final Location startLocation;
        double distanceTraveled;
        int pierceCount;
        final Set<UUID> hitEntityIds;
        
        ProjectileData(Vector originalDirection, double formationOffset, Location startLocation) {
            this.originalDirection = originalDirection.clone();
            this.formationOffset = formationOffset;
            this.startLocation = startLocation.clone();
            this.distanceTraveled = 0.0;
            this.pierceCount = 0;
            this.hitEntityIds = new HashSet<>();
        }
    }

    /**
     * Creates a new wave projectile system
     */
    public WaveProjectile(@NotNull Plugin plugin, @NotNull FxService fxService, 
                         @NotNull Player caster, @NotNull WaveConfig config) {
        this.plugin = plugin;
        this.fxService = fxService;
        this.caster = caster;
        this.config = config;
        this.origin = caster.getEyeLocation();
        this.projectiles = new ArrayList<>();
        this.projectileData = new ConcurrentHashMap<>();
        this.hitEntities = ConcurrentHashMap.newKeySet();
    }

    /**
     * Launches the wave projectiles with the configured formation
     */
    public void launch() {
        Vector baseDirection = origin.getDirection().normalize();
        
        // Play launch sound effect
        fxService.playSound(origin, config.effectType.getLaunchSound(), 1.0f, 1.0f);
        
        // Create projectiles based on formation
        createProjectilesForFormation(baseDirection);
        
        // Start the wave movement and effects task
        startWaveTask();
    }
    
    private void createProjectilesForFormation(Vector baseDirection) {
        for (int i = 0; i < config.projectileCount; i++) {
            Location spawnLoc = calculateSpawnLocation(i, baseDirection);
            Vector direction = calculateProjectileDirection(i, baseDirection);
            
            SmallFireball projectile = origin.getWorld().spawn(spawnLoc, SmallFireball.class, fireball -> {
                fireball.setDirection(direction);
                fireball.setShooter(caster);
                fireball.setIsIncendiary(false);
                fireball.setYield(0f); // No explosion damage
                
                // Tag projectile for identification
                fireball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, 
                    PersistentDataType.STRING, "wave_projectile");
                fireball.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, 
                    PersistentDataType.STRING, caster.getUniqueId().toString());
            });
            
            projectiles.add(projectile);
            
            // Store projectile data for tracking
            double formationOffset = calculateFormationOffset(i);
            projectileData.put(projectile.getUniqueId(), 
                new ProjectileData(direction, formationOffset, spawnLoc));
        }
    }
    
    private Location calculateSpawnLocation(int index, Vector baseDirection) {
        Location spawnLoc = origin.clone();
        
        switch (config.formation) {
            case LINE:
                // Offset perpendicular to direction
                Vector perpendicular = new Vector(-baseDirection.getZ(), 0, baseDirection.getX()).normalize();
                double offset = (index - config.projectileCount / 2.0) * 1.5;
                spawnLoc.add(perpendicular.multiply(offset));
                break;
                
            case ARC:
                // Arc formation spreading outward
                double arcAngle = Math.toRadians((index - config.projectileCount / 2.0) * 15);
                Vector arcDir = rotateVector(baseDirection, arcAngle);
                spawnLoc.add(arcDir.multiply(0.5));
                break;
                
            case BURST:
                // Radial burst from center
                double burstAngle = Math.toRadians(360.0 * index / config.projectileCount);
                Vector burstDir = rotateVector(baseDirection, burstAngle);
                spawnLoc.add(burstDir.multiply(1.0));
                break;
                
            default:
                // Default to line formation
                break;
        }
        
        return spawnLoc;
    }
    
    private Vector calculateProjectileDirection(int index, Vector baseDirection) {
        switch (config.formation) {
            case ARC:
                // Arc spreads outward
                double arcAngle = Math.toRadians((index - config.projectileCount / 2.0) * 15);
                return rotateVector(baseDirection, arcAngle);
                
            case BURST:
                // Radial directions
                double burstAngle = Math.toRadians(360.0 * index / config.projectileCount);
                return rotateVector(baseDirection, burstAngle);
                
            case SINE_WAVE:
                // Slight angle variation for sine wave
                double sineAngle = Math.toRadians((index - config.projectileCount / 2.0) * 5);
                return rotateVector(baseDirection, sineAngle);
                
            default:
                // Most formations use base direction
                return baseDirection.clone();
        }
    }
    
    private double calculateFormationOffset(int index) {
        return (index - config.projectileCount / 2.0) / config.projectileCount;
    }
    
    private Vector rotateVector(Vector vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        
        return new Vector(x, vector.getY(), z);
    }
    
    private void startWaveTask() {
        waveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (tickCount >= config.lifetimeTicks || projectiles.isEmpty()) {
                    cleanup();
                    cancel();
                    return;
                }
                
                updateProjectiles();
                checkCollisions();
                createTrailEffects();
                
                tickCount++;
            }
        };
        
        waveTask.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void updateProjectiles() {
        Iterator<SmallFireball> iterator = projectiles.iterator();
        
        while (iterator.hasNext()) {
            SmallFireball projectile = iterator.next();
            
            if (!projectile.isValid() || projectile.isDead()) {
                projectileData.remove(projectile.getUniqueId());
                iterator.remove();
                continue;
            }
            
            ProjectileData data = projectileData.get(projectile.getUniqueId());
            if (data == null) continue;
            
            // Update distance traveled
            data.distanceTraveled += config.speed;
            
            // Check if projectile exceeded max distance
            if (data.distanceTraveled >= config.maxDistance) {
                createImpactEffect(projectile.getLocation(), null);
                projectile.remove();
                projectileData.remove(projectile.getUniqueId());
                iterator.remove();
                continue;
            }
            
            // Update projectile position based on formation
            updateProjectilePosition(projectile, data);
        }
    }
    
    private void updateProjectilePosition(SmallFireball projectile, ProjectileData data) {
        if (config.formation == WaveFormation.SINE_WAVE) {
            // Sine wave motion
            Vector direction = data.originalDirection.clone();
            Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
            
            double sineOffset = Math.sin(data.distanceTraveled * 0.5 + data.formationOffset * Math.PI) * 2.0;
            Vector sineMovement = perpendicular.multiply(sineOffset * 0.1);
            
            Vector newVelocity = direction.multiply(config.speed).add(sineMovement);
            projectile.setVelocity(newVelocity);
        }
        else if (config.formation == WaveFormation.SPIRAL) {
            // Spiral motion
            double angle = data.distanceTraveled * 0.2 + data.formationOffset * Math.PI;
            Vector direction = data.originalDirection.clone();
            Vector spiralDir = rotateVector(direction, angle);
            
            projectile.setVelocity(spiralDir.multiply(config.speed));
        }
    }
    
    private void checkCollisions() {
        for (SmallFireball projectile : new ArrayList<>(projectiles)) {
            if (!projectile.isValid()) continue;
            
            ProjectileData data = projectileData.get(projectile.getUniqueId());
            if (data == null) continue;
            
            // Check entity collisions
            Collection<Entity> nearbyEntities = projectile.getWorld()
                .getNearbyEntities(projectile.getLocation(), config.hitRadius, config.hitRadius, config.hitRadius);
            
            for (Entity entity : nearbyEntities) {
                if (!(entity instanceof LivingEntity)) continue;
                if (entity.equals(caster)) continue;
                if (data.hitEntityIds.contains(entity.getUniqueId())) continue;
                
                LivingEntity livingEntity = (LivingEntity) entity;
                
                // Apply wave effects
                applyWaveEffects(livingEntity, projectile.getLocation());
                data.hitEntityIds.add(entity.getUniqueId());
                data.pierceCount++;
                
                // Create impact effect
                createImpactEffect(projectile.getLocation(), livingEntity);
                
                // Check if projectile should be removed
                if (!config.pierceEntities || data.pierceCount >= config.maxPierces) {
                    projectile.remove();
                    projectileData.remove(projectile.getUniqueId());
                    projectiles.remove(projectile);
                    break;
                }
            }
        }
    }
    
    private void applyWaveEffects(LivingEntity target, Location hitLocation) {
        // Apply damage
        target.damage(config.damage, caster);
        
        // Apply type-specific effects
        config.effectType.applyEffects(target, caster, fxService);
        
        // Screen shake for nearby players if enabled
        if (config.enableScreenShake) {
            Collection<Entity> nearbyPlayers = target.getWorld()
                .getNearbyEntities(hitLocation, 10, 10, 10);
            
            for (Entity entity : nearbyPlayers) {
                if (entity instanceof Player) {
                    createScreenShake((Player) entity, hitLocation);
                }
            }
        }
    }
    
    private void createImpactEffect(Location location, @Nullable LivingEntity target) {
        // Create impact particles
        config.effectType.createImpactEffect(location, fxService, config.particleDensity);
        
        // Play impact sound
        fxService.playSound(location, config.effectType.getImpactSound(), 0.8f, 1.2f);
        
        // Additional effects for entity hits
        if (target != null) {
            config.effectType.createEntityHitEffect(target.getLocation(), fxService, config.particleDensity);
        }
    }
    
    private void createTrailEffects() {
        for (SmallFireball projectile : projectiles) {
            if (!projectile.isValid()) continue;
            
            Location loc = projectile.getLocation();
            config.effectType.createTrailEffect(loc, fxService, config.particleDensity);
        }
    }
    
    private void createScreenShake(Player player, Location shakeLocation) {
        double distance = player.getLocation().distance(shakeLocation);
        if (distance > 10.0) return;
        
        double intensity = config.screenShakeIntensity * (1.0 - distance / 10.0);
        
        // Create screen shake effect by temporarily modifying player velocity
        Vector shake = new Vector(
            (Math.random() - 0.5) * intensity,
            (Math.random() - 0.5) * intensity * 0.5,
            (Math.random() - 0.5) * intensity
        );
        
        player.setVelocity(player.getVelocity().add(shake));
    }
    
    /**
     * Stops the wave and cleans up all projectiles
     */
    public void stop() {
        cleanup();
        if (waveTask != null && !waveTask.isCancelled()) {
            waveTask.cancel();
        }
    }
    
    private void cleanup() {
        // Remove all projectiles
        for (SmallFireball projectile : projectiles) {
            if (projectile.isValid()) {
                projectile.remove();
            }
        }
        
        projectiles.clear();
        projectileData.clear();
        hitEntities.clear();
    }
    
    /**
     * Checks if the wave system is still active
     */
    public boolean isActive() {
        return waveTask != null && !waveTask.isCancelled() && !projectiles.isEmpty();
    }
    
    /**
     * Gets the number of active projectiles
     */
    public int getActiveProjectileCount() {
        return projectiles.size();
    }
}