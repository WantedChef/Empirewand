package nl.wantedchef.empirewand.listener.spell;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.framework.service.FxService;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles wave spell effects including projectile waves, cone attacks, and multi-hit mechanics.
 * Manages wave formation patterns and ensures proper hit detection.
 */
public class WaveSpellListener implements Listener {
    private final EmpireWandPlugin plugin;
    private final FxService fxService;
    
    // Track wave projectiles to prevent double-hits
    private final Map<UUID, Set<UUID>> waveProjectileHits = new ConcurrentHashMap<>();
    
    // Track wave spell effects for coordination
    private final Map<UUID, WaveSpellData> activeWaveSpells = new ConcurrentHashMap<>();
    
    // Track lingering wave effects
    private final Map<Location, LingeringWaveEffect> lingeringEffects = new ConcurrentHashMap<>();
    
    public WaveSpellListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.fxService = plugin.getFxService();
        
        // Cleanup task for wave tracking
        plugin.getTaskManager().runTaskTimerAsynchronously(this::cleanupWaveTracking, 100L, 100L);
        
        // Process lingering effects
        plugin.getTaskManager().runTaskTimer(this::processLingeringEffects, 20L, 20L);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        
        // Check if this is a wave projectile
        if (isWaveProjectile(projectile)) {
            handleWaveProjectileHit(event);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        
        Entity damager = event.getDamager();
        
        // Handle wave projectile damage
        if (damager instanceof Projectile projectile && isWaveProjectile(projectile)) {
            handleWaveProjectileDamage(event, projectile, target);
        }
        
        // Handle direct wave spell damage (non-projectile)
        if (damager instanceof Player caster) {
            handleDirectWaveSpellDamage(event, caster, target);
        }
    }
    
    private boolean isWaveProjectile(Projectile projectile) {
        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        return pdc.has(Keys.createKey("wave_spell"), Keys.STRING_TYPE.getType()) ||
               pdc.has(Keys.createKey("wave_formation"), Keys.STRING_TYPE.getType());
    }
    
    private void handleWaveProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        
        String spellKey = pdc.get(Keys.createKey("wave_spell"), Keys.STRING_TYPE.getType());
        String formation = pdc.get(Keys.createKey("wave_formation"), Keys.STRING_TYPE.getType());
        
        if (spellKey == null) {
            return;
        }
        
        // Get caster information
        String casterUUID = pdc.get(Keys.PROJECTILE_OWNER, Keys.STRING_TYPE.getType());
        if (casterUUID == null) {
            return;
        }
        
        try {
            UUID casterId = UUID.fromString(casterUUID);
            Player caster = plugin.getServer().getPlayer(casterId);
            
            if (caster != null) {
                handleSpecificWaveHit(event, caster, spellKey, formation);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid caster UUID for wave projectile: " + casterUUID);
        }
    }
    
    private void handleWaveProjectileDamage(EntityDamageByEntityEvent event, Projectile projectile, LivingEntity target) {
        UUID projectileId = projectile.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        // Prevent multiple hits from same projectile
        Set<UUID> hits = waveProjectileHits.computeIfAbsent(projectileId, k -> new HashSet<>());
        if (hits.contains(targetId)) {
            event.setCancelled(true);
            return;
        }
        
        hits.add(targetId);
        
        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        String spellKey = pdc.get(Keys.createKey("wave_spell"), Keys.STRING_TYPE.getType());
        
        if (spellKey != null) {
            applyWaveSpellEffects(event, target, spellKey);
        }
        
        // Check for piercing
        Integer maxPierces = pdc.get(Keys.createKey("max_pierces"), Keys.INTEGER_TYPE.getType());
        if (maxPierces != null && hits.size() >= maxPierces) {
            // Remove projectile after max pierces
            plugin.getTaskManager().runTask(projectile::remove);
        }
    }
    
    private void handleDirectWaveSpellDamage(EntityDamageByEntityEvent event, Player caster, LivingEntity target) {
        PersistentDataContainer pdc = caster.getPersistentDataContainer();
        
        // Check for active wave spell effects
        if (pdc.has(Keys.createKey("wave_spell_active"), Keys.STRING_TYPE.getType())) {
            String spellKey = pdc.get(Keys.createKey("wave_spell_active"), Keys.STRING_TYPE.getType());
            if (spellKey != null) {
                applyWaveSpellEffects(event, target, spellKey);
            }
        }
    }
    
    private void handleSpecificWaveHit(ProjectileHitEvent event, Player caster, String spellKey, String formation) {
        Location hitLocation = event.getEntity().getLocation();
        
        switch (spellKey) {
            case "bloodwave", "blood-wave" -> handleBloodwaveHit(event, caster, hitLocation);
            case "poison-wave" -> handlePoisonWaveHit(event, caster, hitLocation);
            case "flame-wave", "flame-wave-enhanced" -> handleFlameWaveHit(event, caster, hitLocation);
            case "ice-wave" -> handleIceWaveHit(event, caster, hitLocation);
            case "lightning-wave" -> handleLightningWaveHit(event, caster, hitLocation);
        }
        
        // Formation-specific effects
        if (formation != null) {
            handleFormationEffects(hitLocation, formation, spellKey);
        }
    }
    
    private void applyWaveSpellEffects(EntityDamageByEntityEvent event, LivingEntity target, String spellKey) {
        switch (spellKey) {
            case "bloodwave", "blood-wave" -> applyBloodwaveEffects(event, target);
            case "poison-wave" -> applyPoisonWaveEffects(event, target);
            case "flame-wave", "flame-wave-enhanced" -> applyFlameWaveEffects(event, target);
            case "ice-wave" -> applyIceWaveEffects(event, target);
            case "lightning-wave" -> applyLightningWaveEffects(event, target);
            case "explosion-wave" -> applyExplosionWaveEffects(event, target);
        }
    }
    
    private void handleBloodwaveHit(ProjectileHitEvent event, Player caster, Location hitLocation) {
        // Life steal healing for caster
        Projectile projectile = event.getEntity();
        Double lifeStealPercentage = projectile.getPersistentDataContainer()
                .get(Keys.createKey("life_steal_percentage"), Keys.DOUBLE_TYPE.getType());
        
        if (lifeStealPercentage != null) {
            double damage = projectile.getPersistentDataContainer()
                    .getOrDefault(Keys.DAMAGE, Keys.DOUBLE_TYPE.getType(), 8.0);
            double healAmount = damage * (lifeStealPercentage / 100.0);
            
            // Heal caster
            plugin.getTaskManager().runTask(() -> {
                if (caster.isOnline() && caster.isValid()) {
                    caster.setHealth(Math.min(caster.getMaxHealth(), caster.getHealth() + healAmount));
                    
                    // Healing particles
                    fxService.spawnParticle("HEART", caster.getLocation().add(0, 2, 0), 
                            3, 0.5, 0.5, 0.5, 0);
                }
            });
        }
        
        // Blood particle explosion
        fxService.spawnParticle("REDSTONE", hitLocation, 15, 1, 1, 1, 0.1);
        fxService.playSound(hitLocation, "ENTITY_PLAYER_HURT", 0.8f, 0.9f);
    }
    
    private void handlePoisonWaveHit(ProjectileHitEvent event, Player caster, Location hitLocation) {
        // Create lingering poison cloud
        Byte createsCloudByte = event.getEntity().getPersistentDataContainer()
                .get(Keys.createKey("creates_lingering_cloud"), Keys.BOOLEAN_TYPE.getType());
        boolean createsCloud = createsCloudByte != null && createsCloudByte != 0;
        
        if (createsCloud) {
            createLingeringPoisonCloud(hitLocation, caster);
        }
        
        // Toxic particle explosion
        fxService.spawnParticle("VILLAGER_HAPPY", hitLocation, 20, 1.5, 1.5, 1.5, 0.1);
        fxService.playSound(hitLocation, "ENTITY_WITCH_HURT", 0.8f, 1.1f);
    }
    
    private void handleFlameWaveHit(ProjectileHitEvent event, Player caster, Location hitLocation) {
        // Ignite blocks if enabled
        Byte igniteBlocksByte = event.getEntity().getPersistentDataContainer()
                .get(Keys.createKey("ignite_blocks"), Keys.BOOLEAN_TYPE.getType());
        boolean igniteBlocks = igniteBlocksByte != null && igniteBlocksByte != 0;
        
        if (igniteBlocks) {
            igniteNearbyBlocks(hitLocation, 2);
        }
        
        // Fire particle explosion
        fxService.spawnParticle("FLAME", hitLocation, 25, 1, 1, 1, 0.2);
        fxService.spawnParticle("SMOKE_LARGE", hitLocation, 10, 0.8, 0.8, 0.8, 0.1);
        fxService.playSound(hitLocation, "ENTITY_GENERIC_BURN", 1.0f, 0.8f);
    }
    
    private void handleIceWaveHit(ProjectileHitEvent event, Player caster, Location hitLocation) {
        // Create ice structures if enabled
        Byte createsIceByte = event.getEntity().getPersistentDataContainer()
                .get(Keys.createKey("creates_ice_structures"), Keys.BOOLEAN_TYPE.getType());
        boolean createsIce = createsIceByte != null && createsIceByte != 0;
        
        if (createsIce) {
            createIceStructures(hitLocation, caster);
        }
        
        // Frost particle explosion
        fxService.spawnParticle("SNOWFLAKE", hitLocation, 20, 1.2, 1.2, 1.2, 0.1);
        fxService.playSound(hitLocation, "BLOCK_GLASS_BREAK", 0.9f, 1.3f);
    }
    
    private void handleLightningWaveHit(ProjectileHitEvent event, Player caster, Location hitLocation) {
        // Chain lightning to nearby entities
        Byte chainsLightningByte = event.getEntity().getPersistentDataContainer()
                .get(Keys.createKey("chains_lightning"), Keys.BOOLEAN_TYPE.getType());
        boolean chainsLightning = chainsLightningByte != null && chainsLightningByte != 0;
        
        if (chainsLightning) {
            triggerChainLightning(hitLocation, caster);
        }
        
        // Electrical particle explosion
        fxService.spawnParticle("ELECTRIC_SPARK", hitLocation, 30, 1.5, 1.5, 1.5, 0.3);
        fxService.playSound(hitLocation, "ENTITY_LIGHTNING_BOLT_THUNDER", 0.7f, 1.5f);
    }
    
    private void applyBloodwaveEffects(EntityDamageByEntityEvent event, LivingEntity target) {
        // Enhanced damage and brief weakness
        event.setDamage(event.getDamage() * 1.2);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
        
        // Blood particles
        fxService.spawnParticle("REDSTONE", target.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0);
    }
    
    private void applyPoisonWaveEffects(EntityDamageByEntityEvent event, LivingEntity target) {
        // Poison and nausea effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 140, 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
        
        // Poison particles
        fxService.spawnParticle("VILLAGER_HAPPY", target.getLocation().add(0, 1, 0), 6, 0.3, 0.5, 0.3, 0);
    }
    
    private void applyFlameWaveEffects(EntityDamageByEntityEvent event, LivingEntity target) {
        // Fire damage and ignition
        target.setFireTicks(100);
        
        // Fire particles
        fxService.spawnParticle("FLAME", target.getLocation().add(0, 1, 0), 10, 0.4, 0.5, 0.4, 0.1);
    }
    
    private void applyIceWaveEffects(EntityDamageByEntityEvent event, LivingEntity target) {
        // Slowness and mining fatigue
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 2));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 0));
        
        // Ice particles
        fxService.spawnParticle("SNOWFLAKE", target.getLocation().add(0, 1, 0), 8, 0.4, 0.5, 0.4, 0);
    }
    
    private void applyLightningWaveEffects(EntityDamageByEntityEvent event, LivingEntity target) {
        // Glowing and brief paralysis
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 3));
        
        // Lightning particles
        fxService.spawnParticle("ELECTRIC_SPARK", target.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.2);
    }
    
    private void applyExplosionWaveEffects(EntityDamageByEntityEvent event, LivingEntity target) {
        // Knockback and explosion particles
        org.bukkit.util.Vector knockback = target.getLocation().subtract(event.getDamager().getLocation())
                .toVector().normalize().multiply(1.2);
        knockback.setY(Math.max(0.3, knockback.getY()));
        target.setVelocity(knockback);
        
        // Explosion particles
        fxService.spawnParticle("EXPLOSION_NORMAL", target.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
    }
    
    private void handleFormationEffects(Location hitLocation, String formation, String spellKey) {
        switch (formation.toUpperCase()) {
            case "SPIRAL" -> createSpiralEffect(hitLocation, spellKey);
            case "BURST" -> createBurstEffect(hitLocation, spellKey);
            case "ARC" -> createArcEffect(hitLocation, spellKey);
            case "SINE_WAVE" -> createSineWaveEffect(hitLocation, spellKey);
        }
    }
    
    private void createLingeringPoisonCloud(Location center, Player caster) {
        LingeringWaveEffect effect = new LingeringWaveEffect(
                "poison-cloud",
                caster.getUniqueId(),
                System.currentTimeMillis() + 10000L, // 10 seconds
                2.5 // radius
        );
        
        lingeringEffects.put(center, effect);
        
        plugin.getLogger().info("Created lingering poison cloud at " + center);
    }
    
    private void igniteNearbyBlocks(Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location blockLoc = center.clone().add(x, 0, z);
                if (blockLoc.getBlock().getType().isFlammable()) {
                    blockLoc.getBlock().setType(org.bukkit.Material.FIRE);
                }
            }
        }
    }
    
    private void createIceStructures(Location center, Player caster) {
        // Create small ice structure
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x * x + z * z <= 1) {
                    Location blockLoc = center.clone().add(x, 0, z);
                    if (blockLoc.getBlock().getType() == org.bukkit.Material.AIR ||
                        blockLoc.getBlock().getType() == org.bukkit.Material.WATER) {
                        
                        // Use environmental listener to create temporary ice
                        if (plugin.getServer().getPluginManager().isPluginEnabled(plugin)) {
                            // This would integrate with EnvironmentalSpellListener
                            blockLoc.getBlock().setType(org.bukkit.Material.ICE);
                        }
                    }
                }
            }
        }
    }
    
    private void triggerChainLightning(Location center, Player caster) {
        double chainRange = 12.0;
        int maxTargets = 3;
        
        List<LivingEntity> nearbyEntities = center.getWorld().getLivingEntities().stream()
                .filter(entity -> entity != caster)
                .filter(entity -> entity.getLocation().distance(center) <= chainRange)
                .sorted(Comparator.comparing(entity -> entity.getLocation().distance(center)))
                .limit(maxTargets)
                .toList();
        
        for (LivingEntity target : nearbyEntities) {
            // Lightning strike at target
            plugin.getTaskManager().runTaskLater(() -> {
                if (target.isValid() && !target.isDead()) {
                    target.getWorld().strikeLightning(target.getLocation());
                    target.damage(3.0, caster);
                }
            }, 10L);
        }
    }
    
    private void createSpiralEffect(Location center, String spellKey) {
        for (int i = 0; i < 20; i++) {
            double angle = (i * Math.PI * 2) / 10;
            double radius = i * 0.2;
            
            Location effectLoc = center.clone().add(
                    Math.cos(angle) * radius,
                    0.1,
                    Math.sin(angle) * radius
            );
            
            plugin.getTaskManager().runTaskLater(() -> {
                fxService.spawnParticle("FIREWORKS_SPARK", effectLoc, 1, 0, 0, 0, 0);
            }, i * 2L);
        }
    }
    
    private void createBurstEffect(Location center, String spellKey) {
        for (int i = 0; i < 12; i++) {
            double angle = (i * Math.PI * 2) / 12;
            Location effectLoc = center.clone().add(
                    Math.cos(angle) * 2,
                    0.5,
                    Math.sin(angle) * 2
            );
            
            plugin.getTaskManager().runTaskLater(() -> {
                fxService.spawnParticle("EXPLOSION_NORMAL", effectLoc, 2, 0.2, 0.2, 0.2, 0);
            }, 5L);
        }
    }
    
    private void createArcEffect(Location center, String spellKey) {
        for (int i = 0; i < 8; i++) {
            double angle = (i * Math.PI) / 7 - Math.PI / 2; // Arc from -90 to +90 degrees
            Location effectLoc = center.clone().add(
                    Math.cos(angle) * 3,
                    Math.sin(Math.abs(angle)) * 1,
                    Math.sin(angle) * 3
            );
            
            plugin.getTaskManager().runTaskLater(() -> {
                fxService.spawnParticle("CRIT", effectLoc, 2, 0.1, 0.1, 0.1, 0);
            }, i * 3L);
        }
    }
    
    private void createSineWaveEffect(Location center, String spellKey) {
        for (int i = 0; i < 15; i++) {
            double progress = (double) i / 14;
            double waveHeight = Math.sin(progress * Math.PI * 2) * 1.5;
            
            Location effectLoc = center.clone().add(
                    progress * 5 - 2.5,
                    waveHeight,
                    0
            );
            
            plugin.getTaskManager().runTaskLater(() -> {
                fxService.spawnParticle("ENCHANTMENT_TABLE", effectLoc, 1, 0, 0, 0, 0);
            }, i * 2L);
        }
    }
    
    private void processLingeringEffects() {
        long now = System.currentTimeMillis();
        
        lingeringEffects.entrySet().removeIf(entry -> {
            Location location = entry.getKey();
            LingeringWaveEffect effect = entry.getValue();
            
            if (now >= effect.expiryTime()) {
                // Effect expired
                return true;
            }
            
            // Process ongoing effect
            switch (effect.effectType()) {
                case "poison-cloud" -> {
                    // Damage nearby entities
                    location.getWorld().getNearbyEntities(location, effect.radius(), effect.radius(), effect.radius())
                            .stream()
                            .filter(entity -> entity instanceof LivingEntity)
                            .filter(entity -> !entity.getUniqueId().equals(effect.casterUUID()))
                            .forEach(entity -> {
                                LivingEntity target = (LivingEntity) entity;
                                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
                                
                                // Poison particles
                                fxService.spawnParticle("VILLAGER_HAPPY", target.getLocation().add(0, 1, 0), 
                                        2, 0.2, 0.3, 0.2, 0);
                            });
                    
                    // Ambient poison particles
                    fxService.spawnParticle("VILLAGER_HAPPY", location, 8, effect.radius(), 1, effect.radius(), 0.1);
                }
            }
            
            return false;
        });
    }
    
    private void cleanupWaveTracking() {
        // Remove tracking for projectiles that no longer exist
        waveProjectileHits.entrySet().removeIf(entry -> {
            UUID projectileId = entry.getKey();
            Entity entity = plugin.getServer().getEntity(projectileId);
            return entity == null || !entity.isValid();
        });
        
        // Cleanup expired wave spells
        long now = System.currentTimeMillis();
        activeWaveSpells.entrySet().removeIf(entry -> 
                now - entry.getValue().startTime() > 30000L); // 30 second max tracking
    }
    
    /**
     * Data record for wave spell tracking
     */
    private record WaveSpellData(String spellKey, String formation, long startTime) {}
    
    /**
     * Data record for lingering wave effects
     */
    private record LingeringWaveEffect(String effectType, UUID casterUUID, long expiryTime, double radius) {}
}