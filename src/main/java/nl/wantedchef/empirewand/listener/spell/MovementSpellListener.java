package nl.wantedchef.empirewand.listener.spell;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.framework.service.FxService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.attribute.Attribute;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles movement-related spell effects including teleportation safety, 
 * dash mechanics, and movement-based spell triggers.
 */
public class MovementSpellListener implements Listener {
    private final EmpireWandPlugin plugin;
    private final FxService fxService;
    
    // Track teleportation for safety checks
    private final Set<UUID> pendingTeleports = ConcurrentHashMap.newKeySet();
    
    // Safe teleportation materials
    private static final Set<Material> SAFE_TELEPORT_MATERIALS = EnumSet.of(
        Material.AIR, Material.VOID_AIR, Material.CAVE_AIR,
        Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN,
        Material.SNOW, Material.DEAD_BUSH, Material.BAMBOO_SAPLING
    );
    
    // Unsafe teleportation materials
    private static final Set<Material> UNSAFE_TELEPORT_MATERIALS = EnumSet.of(
        Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
        Material.CACTUS, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE,
        Material.POWDER_SNOW, Material.POINTED_DRIPSTONE
    );
    
    public MovementSpellListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.fxService = plugin.getFxService();
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        
        // Check if this is a spell teleportation
        if (isSpellTeleport(event)) {
            handleSpellTeleport(event);
        }
        
        // Handle specific teleport spell effects
        handleTeleportSpellEffects(event);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Handle movement-based spell effects
        handleMovementSpellEffects(player, event);
        
        // Check for dash completion
        handleDashMovement(player, event);
        
        // Handle teleport trail effects
        handleTeleportTrails(player, event);
        
        // Handle movement speed modifications
        handleMovementSpeedEffects(player, event);
    }
    
    private boolean isSpellTeleport(PlayerTeleportEvent event) {
        return event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN ||
               event.getPlayer().getPersistentDataContainer()
                       .has(Keys.createKey("pending_spell_teleport"), Keys.BOOLEAN_TYPE.getType());
    }
    
    private void handleSpellTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location destination = event.getTo();
        
        if (destination == null) {
            return;
        }
        
        // Perform safety checks for spell teleportation
        if (!isSafeTeleportLocation(destination)) {
            Location safeLocation = findSafeTeleportLocation(destination);
            if (safeLocation != null) {
                event.setTo(safeLocation);
                fxService.sendActionBar(player, "&6Teleport destination adjusted for safety!");
            } else {
                event.setCancelled(true);
                fxService.showError(player, "teleport.unsafe-destination");
                return;
            }
        }
        
        // Mark player as having teleported for tracking
        pendingTeleports.add(player.getUniqueId());
        
        // Add teleport protection
        addTeleportProtection(player);
        
        // Cleanup tracking after delay
        plugin.getTaskManager().runTaskLater(() -> {
            pendingTeleports.remove(player.getUniqueId());
        }, 20L);
    }
    
    private void handleTeleportSpellEffects(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        // Check for specific teleport spell effects
        if (pdc.has(Keys.createKey("blink_strike_active"), Keys.BOOLEAN_TYPE.getType())) {
            handleBlinkStrikeEffect(event);
        }
        
        if (pdc.has(Keys.createKey("void_swap_active"), Keys.BOOLEAN_TYPE.getType())) {
            handleVoidSwapEffect(event);
        }
        
        if (pdc.has(Keys.createKey("shadow_step_active"), Keys.BOOLEAN_TYPE.getType())) {
            handleShadowStepEffect(event);
        }
        
        if (pdc.has(Keys.createKey("stellar_dash_active"), Keys.BOOLEAN_TYPE.getType())) {
            handleStellarDashEffect(event);
        }
    }
    
    private void handleMovementSpellEffects(Player player, PlayerMoveEvent event) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        // Handle lifewalk healing
        if (pdc.has(Keys.createKey("lifewalk_active"), Keys.BOOLEAN_TYPE.getType())) {
            handleLifewalkMovement(player, event);
        }
        
        // Handle ethereal form movement
        if (pdc.has(Keys.createKey("ethereal_form_active"), Keys.BOOLEAN_TYPE.getType())) {
            handleEtherealFormMovement(player, event);
        }
        
        // Handle shadow cloak movement effects
        if (pdc.has(Keys.createKey("shadow_cloak_active"), Keys.BOOLEAN_TYPE.getType())) {
            handleShadowCloakMovement(player, event);
        }
        
        // Handle movement-triggered spell effects
        handleMovementTriggers(player, event);
    }
    
    private void handleDashMovement(Player player, PlayerMoveEvent event) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        if (pdc.has(Keys.createKey("dash_active"), Keys.BOOLEAN_TYPE.getType())) {
            Vector velocity = player.getVelocity();
            
            // Check if dash has ended (low velocity)
            if (velocity.length() < 0.3) {
                // End dash effect
                pdc.remove(Keys.createKey("dash_active"));
                
                // Apply landing effects
                fxService.spawnParticle("CLOUD", player.getLocation(), 10, 1, 0.2, 1, 0.1);
                
                // Remove dash protection
                player.removePotionEffect(PotionEffectType.RESISTANCE);
            } else {
                // Continue dash particle trail
                fxService.spawnParticle("FIREWORKS_SPARK", 
                        player.getLocation().subtract(velocity.clone().normalize()), 
                        3, 0.2, 0.2, 0.2, 0);
            }
        }
    }
    
    private void handleTeleportTrails(Player player, PlayerMoveEvent event) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        if (pdc.has(Keys.createKey("teleport_trail_active"), Keys.BOOLEAN_TYPE.getType())) {
            Long trailEnd = pdc.get(Keys.createKey("teleport_trail_end"), Keys.LONG_TYPE.getType());
            
            if (trailEnd != null && System.currentTimeMillis() < trailEnd) {
                // Create teleport particle trail
                String trailType = pdc.getOrDefault(Keys.createKey("teleport_trail_type"), 
                        Keys.STRING_TYPE.getType(), "PORTAL");
                
                fxService.spawnParticle(trailType, 
                        event.getFrom().add(0, 1, 0), 
                        3, 0.3, 0.3, 0.3, 0.1);
            } else {
                // Trail expired
                pdc.remove(Keys.createKey("teleport_trail_active"));
                pdc.remove(Keys.createKey("teleport_trail_end"));
                pdc.remove(Keys.createKey("teleport_trail_type"));
            }
        }
    }
    
    private void handleMovementSpeedEffects(Player player, PlayerMoveEvent event) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        // Handle speed boost effects from movement spells
        if (pdc.has(Keys.createKey("speed_boost_active"), Keys.BOOLEAN_TYPE.getType())) {
            Double speedMultiplier = pdc.get(Keys.createKey("speed_boost_multiplier"), Keys.DOUBLE_TYPE.getType());
            
            if (speedMultiplier != null && speedMultiplier > 1.0) {
                Vector velocity = event.getPlayer().getVelocity();
                
                // Only boost horizontal movement
                velocity.setX(velocity.getX() * speedMultiplier);
                velocity.setZ(velocity.getZ() * speedMultiplier);
                
                // Apply enhanced velocity on next tick
                plugin.getTaskManager().runTask(() -> {
                    if (player.isOnline() && player.isValid()) {
                        player.setVelocity(velocity);
                    }
                });
            }
        }
    }
    
    private boolean isSafeTeleportLocation(Location location) {
        Block feetBlock = location.getBlock();
        Block headBlock = location.add(0, 1, 0).getBlock();
        Block groundBlock = location.subtract(0, 2, 0).getBlock();
        
        // Check for unsafe materials
        if (UNSAFE_TELEPORT_MATERIALS.contains(feetBlock.getType()) ||
            UNSAFE_TELEPORT_MATERIALS.contains(headBlock.getType()) ||
            UNSAFE_TELEPORT_MATERIALS.contains(groundBlock.getType())) {
            return false;
        }
        
        // Feet and head must be air or safe materials
        if (!SAFE_TELEPORT_MATERIALS.contains(feetBlock.getType()) && !feetBlock.getType().isAir()) {
            return false;
        }
        
        if (!SAFE_TELEPORT_MATERIALS.contains(headBlock.getType()) && !headBlock.getType().isAir()) {
            return false;
        }
        
        // Must have solid ground (unless flying)
        if (groundBlock.getType().isAir() && location.getY() > 1) {
            return false;
        }
        
        return true;
    }
    
    private Location findSafeTeleportLocation(Location original) {
        // Search in expanding radius for safe location
        for (int radius = 1; radius <= 5; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int y = -2; y <= 2; y++) {
                        Location candidate = original.clone().add(x, y, z);
                        if (isSafeTeleportLocation(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private void addTeleportProtection(Player player) {
        // Brief damage resistance after teleportation
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0));
        
        // Mark protection for tracking
        player.getPersistentDataContainer().set(Keys.createKey("teleport_protection"), 
                Keys.LONG_TYPE.getType(), System.currentTimeMillis() + 2000L);
    }
    
    private void handleBlinkStrikeEffect(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Create blink visual effects
        fxService.spawnParticle("PORTAL", event.getFrom(), 15, 0.5, 0.5, 0.5, 0.3);
        fxService.spawnParticle("PORTAL", event.getTo(), 15, 0.5, 0.5, 0.5, 0.3);
        
        // Invisibility flash
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0));
        
        // Sound effects
        fxService.playSound(event.getFrom(), "ENTITY_ENDERMAN_TELEPORT", 0.8f, 1.2f);
        fxService.playSound(event.getTo(), "ENTITY_ENDERMAN_TELEPORT", 0.8f, 1.2f);
        
        // Cleanup
        player.getPersistentDataContainer().remove(Keys.createKey("blink_strike_active"));
    }
    
    private void handleVoidSwapEffect(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Void particle effects
        fxService.spawnParticle("PORTAL", event.getFrom(), 25, 1, 1, 1, 0.5);
        fxService.spawnParticle("DRAGON_BREATH", event.getTo(), 20, 0.8, 0.8, 0.8, 0.1);
        
        // Brief weakness from void exposure
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
        
        // Cleanup
        player.getPersistentDataContainer().remove(Keys.createKey("void_swap_active"));
    }
    
    private void handleShadowStepEffect(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Shadow particle effects
        fxService.spawnParticle("SMOKE_LARGE", event.getFrom(), 20, 0.5, 0.5, 0.5, 0.1);
        fxService.spawnParticle("SMOKE_LARGE", event.getTo(), 20, 0.5, 0.5, 0.5, 0.1);
        
        // Brief invisibility
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0));
        
        // Shadow echoes
        createShadowEchoes(event.getFrom(), event.getTo());
        
        // Cleanup
        player.getPersistentDataContainer().remove(Keys.createKey("shadow_step_active"));
    }
    
    private void handleStellarDashEffect(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Stellar particle trail
        createStellarTrail(event.getFrom(), event.getTo());
        
        // Star-like effects
        fxService.spawnParticle("FIREWORKS_SPARK", event.getTo(), 30, 1, 1, 1, 0.3);
        
        // Brief speed boost
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
        
        // Cleanup
        player.getPersistentDataContainer().remove(Keys.createKey("stellar_dash_active"));
    }
    
    private void handleLifewalkMovement(Player player, PlayerMoveEvent event) {
        // Heal player gradually while moving
        if (event.getFrom().distance(event.getTo()) > 0.1) { // Only when actually moving
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            Long lastHeal = pdc.get(Keys.createKey("lifewalk_last_heal"), Keys.LONG_TYPE.getType());
            long now = System.currentTimeMillis();
            
            if (lastHeal == null || now - lastHeal > 1000) { // Heal every second
                double healAmount = pdc.getOrDefault(Keys.createKey("lifewalk_heal_rate"), 
                        Keys.DOUBLE_TYPE.getType(), 0.5);
                
                double max = getMaxHealth(player);
                if (player.getHealth() < max) {
                    player.setHealth(Math.min(max, player.getHealth() + healAmount));
                    
                    // Healing particles
                    fxService.spawnParticle("HEART", player.getLocation().add(0, 2, 0), 
                            2, 0.3, 0.3, 0.3, 0);
                }
                
                pdc.set(Keys.createKey("lifewalk_last_heal"), Keys.LONG_TYPE.getType(), now);
            }
        }
    }
    
    private void handleEtherealFormMovement(Player player, PlayerMoveEvent event) {
        // Create ethereal particle trail
        fxService.spawnParticle("SOUL", event.getFrom().add(0, 1, 0), 
                2, 0.3, 0.3, 0.3, 0.05);
        
        // Allow movement through certain blocks
        Block block = event.getTo().getBlock();
        if (block.getType().isSolid() && !block.getType().equals(Material.BEDROCK)) {
            // Phase through non-bedrock solid blocks
            event.getTo().add(0, 1, 0); // Boost upward to avoid getting stuck
        }
    }
    
    private void handleShadowCloakMovement(Player player, PlayerMoveEvent event) {
        // Random shadow step chance
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Double stepChance = pdc.get(Keys.createKey("shadow_step_chance_per_second"), Keys.DOUBLE_TYPE.getType());
        
        if (stepChance != null && Math.random() < stepChance / 20.0) { // Per tick chance
            // Trigger shadow step
            performShadowStep(player);
        }
        
        // Dark particle trail
        if (Math.random() < 0.3) { // 30% chance per move
            fxService.spawnParticle("SMOKE_NORMAL", 
                    event.getFrom().add(0, 0.1, 0), 
                    1, 0.1, 0.1, 0.1, 0);
        }
    }
    
    private void handleMovementTriggers(Player player, PlayerMoveEvent event) {
        // Check for movement-triggered spell effects
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        // Explosion trail
        if (pdc.has(Keys.createKey("explosion_trail_active"), Keys.BOOLEAN_TYPE.getType())) {
            Long lastExplosion = pdc.get(Keys.createKey("explosion_trail_last"), Keys.LONG_TYPE.getType());
            long now = System.currentTimeMillis();
            
            if (lastExplosion == null || now - lastExplosion > 500) { // Every 0.5 seconds
                // Create small explosion at old location
                Location explosionLoc = event.getFrom();
                fxService.spawnParticle("EXPLOSION_NORMAL", explosionLoc, 5, 0.5, 0.5, 0.5, 0);
                
                // Damage nearby enemies
                explosionLoc.getWorld().getLivingEntities().stream()
                        .filter(entity -> entity != player)
                        .filter(entity -> entity.getLocation().distance(explosionLoc) <= 3.0)
                        .forEach(entity -> entity.damage(2.0, player));
                
                pdc.set(Keys.createKey("explosion_trail_last"), Keys.LONG_TYPE.getType(), now);
            }
        }
    }
    
    private void createShadowEchoes(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        int echoes = 6;
        
        for (int i = 0; i < echoes; i++) {
            double progress = (double) i / echoes;
            Location echoLocation = from.clone().add(direction.clone().multiply(progress));
            
            plugin.getTaskManager().runTaskLater(() -> {
                fxService.spawnParticle("SMOKE_NORMAL", echoLocation, 3, 0.2, 0.5, 0.2, 0);
            }, i * 2L);
        }
    }
    
    private void createStellarTrail(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        int points = 10;
        
        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            Location trailLocation = from.clone().add(direction.clone().multiply(progress));
            
            plugin.getTaskManager().runTaskLater(() -> {
                fxService.spawnParticle("FIREWORKS_SPARK", trailLocation, 2, 0.1, 0.1, 0.1, 0.1);
                fxService.spawnParticle("END_ROD", trailLocation, 1, 0, 0, 0, 0);
            }, i);
        }
    }
    
    private void performShadowStep(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Double maxDistance = pdc.get(Keys.createKey("shadow_step_max_distance"), Keys.DOUBLE_TYPE.getType());
        Long cooldownTicks = pdc.get(Keys.createKey("shadow_step_cooldown_ticks"), Keys.LONG_TYPE.getType());
        Long lastStep = pdc.get(Keys.createKey("shadow_step_last"), Keys.LONG_TYPE.getType());
        
        long now = player.getWorld().getFullTime();
        
        if (lastStep != null && cooldownTicks != null && now - lastStep < cooldownTicks) {
            return; // On cooldown
        }
        
        if (maxDistance == null) {
            maxDistance = 6.0;
        }
        
        // Find random location within range
        double angle = Math.random() * 2 * Math.PI;
        double distance = 2 + Math.random() * (maxDistance - 2);
        
        Location newLocation = player.getLocation().add(
                Math.cos(angle) * distance,
                0,
                Math.sin(angle) * distance
        );
        
        if (isSafeTeleportLocation(newLocation)) {
            // Mark as shadow step teleport
            pdc.set(Keys.createKey("shadow_step_active"), Keys.BOOLEAN_TYPE.getType(), (byte) 1);
            player.teleport(newLocation);
            
            // Update cooldown
            pdc.set(Keys.createKey("shadow_step_last"), Keys.LONG_TYPE.getType(), now);
            
            pdc.set(Keys.createKey("lifewalk_last_heal"), Keys.LONG_TYPE.getType(), now);
        }
    }
    
    private double getMaxHealth(org.bukkit.entity.LivingEntity entity) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
}

