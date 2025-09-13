package nl.wantedchef.empirewand.listener.spell;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.framework.service.FxService;
import org.bukkit.Location;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles lightning spell effects including chain lightning, lightning strikes, and electrical damage.
 * Manages chain lightning propagation and prevents infinite loops.
 */
public class LightningEffectListener implements Listener {
    private final EmpireWandPlugin plugin;
    private final FxService fxService;
    
    // Track chain lightning to prevent infinite loops
    private final Map<UUID, Set<UUID>> chainLightningTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Long> chainLightningStartTime = new ConcurrentHashMap<>();
    private static final long CHAIN_RESET_TIME = 5000; // 5 seconds
    
    // Track lightning strikes to apply spell effects
    private final Map<Location, LightningSpellData> lightningStrikes = new ConcurrentHashMap<>();
    private static final double LIGHTNING_EFFECT_RADIUS = 5.0;
    
    public LightningEffectListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.fxService = plugin.getFxService();
        
        // Cleanup task for chain lightning tracking
        plugin.getTaskManager().runTaskTimerAsynchronously(this::cleanupChainTracking, 100L, 100L);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onLightningStrike(LightningStrikeEvent event) {
        Location strike = event.getLightning().getLocation();
        
        // Check if this is a spell-generated lightning
        if (event.getLightning().getPersistentDataContainer().has(Keys.createKey("spell_lightning"), Keys.STRING_TYPE.getType())) {
            String spellKey = event.getLightning().getPersistentDataContainer()
                    .get(Keys.createKey("spell_lightning"), Keys.STRING_TYPE.getType());
            String casterUUID = event.getLightning().getPersistentDataContainer()
                    .get(Keys.createKey("spell_caster"), Keys.STRING_TYPE.getType());
            
            if (spellKey != null && casterUUID != null) {
                try {
                    UUID caster = UUID.fromString(casterUUID);
                    Player casterPlayer = plugin.getServer().getPlayer(caster);
                    
                    if (casterPlayer != null) {
                        // Store lightning data for damage processing
                        LightningSpellData data = new LightningSpellData(spellKey, caster, System.currentTimeMillis());
                        lightningStrikes.put(strike, data);
                        
                        // Handle specific lightning spell effects
                        handleLightningSpellStrike(event, casterPlayer, spellKey, strike);
                        
                        // Schedule cleanup
                        plugin.getTaskManager().runTaskLater(() -> lightningStrikes.remove(strike), 100L);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid caster UUID for lightning spell: " + casterUUID);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByLightning(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) {
            return;
        }
        
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        
        // Check if this damage is from a spell lightning
        Location targetLoc = target.getLocation();
        LightningSpellData lightningData = findNearbyLightningSpell(targetLoc);
        
        if (lightningData != null) {
            Player caster = plugin.getServer().getPlayer(lightningData.casterUUID());
            if (caster != null) {
                // Apply spell-specific lightning effects
                handleLightningSpellDamage(event, target, caster, lightningData.spellKey());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        
        if (!(event.getDamager() instanceof Player caster)) {
            return;
        }
        
        // Check for chain lightning propagation
        if (shouldPropagateChainLightning(caster, target)) {
            handleChainLightningPropagation(caster, target, event.getDamage());
        }
        
        // Check for lightning arrow effects
        if (hasLightningArrowEffect(event)) {
            handleLightningArrowHit(event, caster, target);
        }
    }
    
    private void handleLightningSpellStrike(LightningStrikeEvent event, Player caster, String spellKey, Location strike) {
        switch (spellKey) {
            case "thunder-blast" -> handleThunderBlastStrike(event, caster, strike);
            case "lightning-storm" -> handleLightningStormStrike(event, caster, strike);
            case "chain-lightning-enhanced" -> handleChainLightningStrike(event, caster, strike);
            case "elemental-lightning-attack" -> handleElementalLightningStrike(event, caster, strike);
        }
    }
    
    private void handleLightningSpellDamage(EntityDamageEvent event, LivingEntity target, Player caster, String spellKey) {
        // Apply spell-specific damage modifiers and effects
        switch (spellKey) {
            case "thunder-blast" -> {
                // Multiple strike damage with knockback
                event.setDamage(16.0);
                if (target instanceof Player) {
                    target.setVelocity(target.getVelocity().add(
                            target.getLocation().subtract(caster.getLocation()).toVector().normalize().multiply(0.8)));
                }
            }
            case "lightning-storm" -> {
                // Area lightning with chaining
                event.setDamage(16.0);
                scheduleChainLightning(caster, target, 3);
            }
            case "little-spark" -> {
                // Fast, light damage
                event.setDamage(4.0);
                applyLightningKnockback(target, caster, 0.5);
            }
            case "elemental-lightning-attack" -> {
                // High damage with area effect
                event.setDamage(12.0);
                damageNearbyEntities(target.getLocation(), caster, 3.0, 6.0);
            }
        }
        
        // Common lightning effects
        applyLightningStatusEffects(target, spellKey);
    }
    
    private boolean shouldPropagateChainLightning(Player caster, LivingEntity target) {
        PersistentDataContainer pdc = caster.getPersistentDataContainer();
        return pdc.has(Keys.createKey("chain_lightning_active"), Keys.BOOLEAN_TYPE.getType()) &&
               !isAlreadyChainTarget(caster.getUniqueId(), target.getUniqueId());
    }
    
    private void handleChainLightningPropagation(Player caster, LivingEntity target, double baseDamage) {
        UUID casterId = caster.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        // Add to chain targets
        chainLightningTargets.computeIfAbsent(casterId, k -> new HashSet<>()).add(targetId);
        chainLightningStartTime.put(casterId, System.currentTimeMillis());
        
        // Get chain parameters
        PersistentDataContainer pdc = caster.getPersistentDataContainer();
        double chainRange = pdc.getOrDefault(Keys.createKey("chain_lightning_range"), Keys.DOUBLE_TYPE.getType(), 12.0);
        int maxChains = pdc.getOrDefault(Keys.createKey("chain_lightning_jumps"), Keys.INTEGER_TYPE.getType(), 3);
        double damageMultiplier = pdc.getOrDefault(Keys.createKey("chain_lightning_damage_mult"), Keys.DOUBLE_TYPE.getType(), 0.7);
        
        Set<UUID> currentTargets = chainLightningTargets.get(casterId);
        if (currentTargets.size() >= maxChains) {
            return; // Max chains reached
        }
        
        // Find next chain target
        List<LivingEntity> nearbyEntities = target.getWorld().getLivingEntities().stream()
                .filter(entity -> entity != target && entity != caster)
                .filter(entity -> entity.getLocation().distance(target.getLocation()) <= chainRange)
                .filter(entity -> !currentTargets.contains(entity.getUniqueId()))
                .sorted(Comparator.comparing(entity -> entity.getLocation().distance(target.getLocation())))
                .toList();
        
        if (!nearbyEntities.isEmpty()) {
            LivingEntity nextTarget = nearbyEntities.get(0);
            double chainDamage = baseDamage * damageMultiplier;
            
            // Create lightning arc effect
            createLightningArc(target.getLocation(), nextTarget.getLocation());
            
            // Damage next target
            plugin.getTaskManager().runTaskLater(() -> {
                if (nextTarget.isValid() && !nextTarget.isDead()) {
                    nextTarget.damage(chainDamage, caster);
                    
                    // Apply glowing effect
                    nextTarget.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.GLOWING, 120, 0));
                }
            }, 5L);
        }
    }
    
    private void handleLightningArrowHit(EntityDamageByEntityEvent event, Player caster, LivingEntity target) {
        // Apply additional lightning damage
        double lightningDamage = caster.getPersistentDataContainer()
                .getOrDefault(Keys.createKey("lightning_arrow_bonus"), Keys.DOUBLE_TYPE.getType(), 12.0);
        
        target.damage(lightningDamage, caster);
        
        // Strike lightning at target
        Location strikeLocation = target.getLocation();
        plugin.getTaskManager().runTask(() -> {
            target.getWorld().strikeLightning(strikeLocation);
            
            // Lightning particle trail
            fxService.spawnParticle("ELECTRIC_SPARK", strikeLocation.add(0, 2, 0), 15, 0.5, 1.5, 0.5, 0.2);
        });
    }
    
    private void scheduleChainLightning(Player caster, LivingEntity initialTarget, int maxJumps) {
        // Mark caster as having active chain lightning
        caster.getPersistentDataContainer().set(Keys.createKey("chain_lightning_active"), Keys.BOOLEAN_TYPE.getType(), (byte) 1);
        caster.getPersistentDataContainer().set(Keys.createKey("chain_lightning_jumps"), Keys.INTEGER_TYPE.getType(), maxJumps);
        caster.getPersistentDataContainer().set(Keys.createKey("chain_lightning_range"), Keys.DOUBLE_TYPE.getType(), 18.0);
        caster.getPersistentDataContainer().set(Keys.createKey("chain_lightning_damage_mult"), Keys.DOUBLE_TYPE.getType(), 0.4);
        
        // Clear after duration
        plugin.getTaskManager().runTaskLater(() -> {
            caster.getPersistentDataContainer().remove(Keys.createKey("chain_lightning_active"));
            chainLightningTargets.remove(caster.getUniqueId());
            chainLightningStartTime.remove(caster.getUniqueId());
        }, 100L);
    }
    
    private void applyLightningKnockback(LivingEntity target, Player caster, double strength) {
        if (target instanceof Player && caster.equals(target)) {
            return; // Don't knockback self
        }
        
        org.bukkit.util.Vector direction = target.getLocation().subtract(caster.getLocation()).toVector().normalize();
        direction.setY(Math.max(0.1, direction.getY())); // Ensure upward component
        target.setVelocity(direction.multiply(strength));
    }
    
    private void damageNearbyEntities(Location center, Player caster, double radius, double damage) {
        center.getWorld().getLivingEntities().stream()
                .filter(entity -> entity != caster)
                .filter(entity -> entity.getLocation().distance(center) <= radius)
                .forEach(entity -> {
                    // Check if friendly fire is disabled
                    if (entity instanceof Player && !allowFriendlyFire(caster)) {
                        return;
                    }
                    entity.damage(damage, caster);
                    
                    // Lightning particle effects
                    fxService.spawnParticle("ELECTRIC_SPARK", entity.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.1);
                });
    }
    
    private void applyLightningStatusEffects(LivingEntity target, String spellKey) {
        // Common glowing effect for lightning spells
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.GLOWING, 120, 0));
        
        // Spell-specific effects
        switch (spellKey) {
            case "chain-lightning-enhanced" -> {
                // Brief paralysis effect
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 20, 2));
            }
            case "lightning-storm" -> {
                // Weakness from electrical overload
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.WEAKNESS, 60, 0));
            }
        }
    }
    
    private void createLightningArc(Location from, Location to) {
        // Create particle arc between locations
        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();
        
        int particles = (int) (distance * 2);
        for (int i = 0; i < particles; i++) {
            double progress = (double) i / particles;
            Location particleLocation = from.clone().add(direction.clone().multiply(distance * progress));
            
            // Add some randomness for electrical effect
            particleLocation.add(
                    (Math.random() - 0.5) * 0.3,
                    (Math.random() - 0.5) * 0.3,
                    (Math.random() - 0.5) * 0.3
            );
            
            fxService.spawnParticle("ELECTRIC_SPARK", particleLocation, 1, 0, 0, 0, 0);
        }
        
        // Sound effect
        fxService.playSound(to, "ENTITY_LIGHTNING_BOLT_THUNDER", 0.3f, 1.8f);
    }
    
    private LightningSpellData findNearbyLightningSpell(Location location) {
        return lightningStrikes.entrySet().stream()
                .filter(entry -> entry.getKey().distance(location) <= LIGHTNING_EFFECT_RADIUS)
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
    
    private boolean isAlreadyChainTarget(UUID casterId, UUID targetId) {
        Set<UUID> targets = chainLightningTargets.get(casterId);
        return targets != null && targets.contains(targetId);
    }
    
    private boolean hasLightningArrowEffect(EntityDamageByEntityEvent event) {
        return event.getDamager().getPersistentDataContainer()
                .has(Keys.createKey("lightning_arrow"), Keys.BOOLEAN_TYPE.getType());
    }
    
    private boolean allowFriendlyFire(Player caster) {
        return plugin.getConfigService().getSpellsConfig()
                .getBoolean("elemental-lightning-attack.values.friendly-fire", false);
    }
    
    private void cleanupChainTracking() {
        long now = System.currentTimeMillis();
        chainLightningStartTime.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > CHAIN_RESET_TIME) {
                chainLightningTargets.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    private void handleThunderBlastStrike(LightningStrikeEvent event, Player caster, Location strike) {
        // Multiple strikes in radius
        plugin.getTaskManager().runTaskLater(() -> {
            for (int i = 0; i < 2; i++) {
                Location randomStrike = strike.clone().add(
                        (Math.random() - 0.5) * 6,
                        0,
                        (Math.random() - 0.5) * 6
                );
                strike.getWorld().strikeLightning(randomStrike);
            }
        }, 10L);
    }
    
    private void handleLightningStormStrike(LightningStrikeEvent event, Player caster, Location strike) {
        // Create electrical storm field
        for (int i = 0; i < 20; i++) {
            plugin.getTaskManager().runTaskLater(() -> {
                fxService.spawnParticle("ELECTRIC_SPARK", 
                        strike.clone().add(Math.random() * 10 - 5, Math.random() * 3, Math.random() * 10 - 5),
                        3, 0.2, 0.2, 0.2, 0.1);
            }, i * 2L);
        }
    }
    
    private void handleChainLightningStrike(LightningStrikeEvent event, Player caster, Location strike) {
        // Enhanced chain effect
        scheduleChainLightning(caster, null, 4);
        
        // Area electrical field
        fxService.spawnParticle("ELECTRIC_SPARK", strike, 25, 3, 1, 3, 0.3);
    }
    
    private void handleElementalLightningStrike(LightningStrikeEvent event, Player caster, Location strike) {
        // High-power concentrated strike
        damageNearbyEntities(strike, caster, 3.0, 12.0);
        
        // Intense electrical effects
        fxService.spawnParticle("ELECTRIC_SPARK", strike, 40, 2, 2, 2, 0.5);
        fxService.playSound(strike, "ENTITY_LIGHTNING_BOLT_THUNDER", 1.0f, 0.8f);
    }
    
    /**
     * Data record for tracking spell-generated lightning strikes
     */
    private record LightningSpellData(String spellKey, UUID casterUUID, long timestamp) {}
}