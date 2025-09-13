package nl.wantedchef.empirewand.listener.spell;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.framework.service.FxService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;


import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles environmental spell effects including weather manipulation,
 * block interactions, world changes, and temporary structures.
 */
public class EnvironmentalSpellListener implements Listener {
    private final EmpireWandPlugin plugin;
    private final FxService fxService;
    
    // Track temporary spell-created blocks for cleanup
    private final Map<Location, TemporaryBlock> temporaryBlocks = new ConcurrentHashMap<>();
    
    // Track weather spell effects
    private final Map<World, WeatherSpellData> activeWeatherSpells = new ConcurrentHashMap<>();
    
    // Track ice-related spell effects
    private final Set<Location> spellIceBlocks = ConcurrentHashMap.newKeySet();
    
    public EnvironmentalSpellListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.fxService = plugin.getFxService();
        
        // Cleanup task for temporary blocks
        plugin.getTaskManager().runTaskTimerAsynchronously(this::cleanupTemporaryBlocks, 200L, 200L);
        
        // Weather effect maintenance task
        plugin.getTaskManager().runTaskTimer(this::processWeatherEffects, 20L, 20L);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onWeatherChange(WeatherChangeEvent event) {
        World world = event.getWorld();
        
        // Check if weather change is from a spell
        WeatherSpellData spellData = activeWeatherSpells.get(world);
        if (spellData != null) {
            // Spell is controlling weather - prevent natural changes
            if (event.toWeatherState() != spellData.targetWeatherState()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        
        // Check if this is a spell-protected block
        if (isSpellProtectedBlock(location)) {
            event.setCancelled(true);
            
            Player player = event.getPlayer();
            fxService.sendActionBar(player, "&cThis block is protected by spell magic!");
            return;
        }
        
        // Check if this is a temporary spell block
        TemporaryBlock tempBlock = temporaryBlocks.get(location);
        if (tempBlock != null) {
            // Allow breaking but handle cleanup
            handleTemporaryBlockBreak(event, tempBlock);
        }
        
        // Handle ice wall breaking
        if (spellIceBlocks.contains(location)) {
            handleSpellIceBreak(event);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        
        // Check if placing over spell effects
        if (hasActiveSpellEffect(location)) {
            // Some spell effects prevent block placement
            if (shouldPreventBlockPlacement(location)) {
                event.setCancelled(true);
                
                Player player = event.getPlayer();
                fxService.sendActionBar(player, "&cCannot place blocks here due to active spell effects!");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        // Initialize world-specific spell tracking
        World world = event.getWorld();
        plugin.getLogger().info("Initializing spell tracking for world: " + world.getName());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        // Cleanup world-specific spell data
        World world = event.getWorld();
        
        // Remove weather spells
        activeWeatherSpells.remove(world);
        
        // Remove temporary blocks in this world
        temporaryBlocks.entrySet().removeIf(entry -> 
                entry.getKey().getWorld().equals(world));
        
        // Remove ice blocks in this world
        spellIceBlocks.removeIf(location -> location.getWorld().equals(world));
        
        plugin.getLogger().info("Cleaned up spell data for unloaded world: " + world.getName());
    }
    
    /**
     * Creates a temporary block that will be automatically removed after a duration
     */
    public void createTemporaryBlock(Location location, Material material, long durationTicks, String spellKey) {
        Block block = location.getBlock();
        Material originalMaterial = block.getType();
        
        // Store original state
        TemporaryBlock tempBlock = new TemporaryBlock(
                originalMaterial,
                System.currentTimeMillis() + (durationTicks * 50L),
                spellKey
        );
        
        temporaryBlocks.put(location, tempBlock);
        
        // Set new block
        block.setType(material);
        
        // Mark as spell block
        markSpellBlock(location, spellKey);
        
        plugin.getLogger().info(String.format("Created temporary %s block at %s for spell %s", 
                material, location, spellKey));
    }
    
    /**
     * Creates temporary ice structures for ice spells
     */
    public void createIceStructure(Location center, int radius, String spellKey) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y <= 2; y++) {
                    if (x * x + z * z <= radius * radius) {
                        Location blockLoc = center.clone().add(x, y, z);
                        Block block = blockLoc.getBlock();
                        
                        if (block.getType() == Material.AIR || block.getType() == Material.WATER) {
                            // Create ice
                            createTemporaryBlock(blockLoc, Material.ICE, 200L, spellKey);
                            spellIceBlocks.add(blockLoc);
                            
                            // Ice creation particles
                            fxService.spawnParticle("SNOWFLAKE", blockLoc.add(0.5, 0.5, 0.5), 
                                    3, 0.3, 0.3, 0.3, 0);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Creates a light wall structure
     */
    public void createLightWall(Location start, Location end, double height, long durationTicks, String spellKey) {
        org.bukkit.util.Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        
        for (double d = 0; d <= distance; d += 0.5) {
            Location wallLoc = start.clone().add(direction.clone().multiply(d));
            
            for (int y = 0; y < height; y++) {
                Location blockLoc = wallLoc.clone().add(0, y, 0);
                Block block = blockLoc.getBlock();
                
                if (block.getType() == Material.AIR) {
                    createTemporaryBlock(blockLoc, Material.WHITE_STAINED_GLASS, durationTicks, spellKey);
                    
                    // Light particles
                    fxService.spawnParticle("FIREWORKS_SPARK", blockLoc.add(0.5, 0.5, 0.5), 
                            2, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
    }
    
    /**
     * Initiates weather spell effect
     */
    public void startWeatherSpell(World world, String spellKey, boolean rainState, long durationTicks) {
        WeatherSpellData data = new WeatherSpellData(
                spellKey,
                rainState,
                System.currentTimeMillis() + (durationTicks * 50L)
        );
        
        activeWeatherSpells.put(world, data);
        
        // Apply weather change
        world.setStorm(rainState);
        if (rainState) {
            world.setThundering(spellKey.contains("lightning") || spellKey.contains("storm"));
        }
        
        plugin.getLogger().info(String.format("Started weather spell %s in world %s", spellKey, world.getName()));
    }
    
    /**
     * Creates a tornado effect with block manipulation
     */
    public void createTornado(Location center, double radius, long durationTicks, String spellKey) {
        // Mark area as tornado zone
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 8) {
            for (double r = 1; r <= radius; r += 1) {
                Location effectLoc = center.clone().add(
                        Math.cos(angle) * r,
                        Math.random() * 6,
                        Math.sin(angle) * r
                );
                
                // Tornado particle effects
                plugin.getTaskManager().runTaskLater(() -> {
                    if (effectLoc.getWorld() != null) {
                        fxService.spawnParticle("CLOUD", effectLoc, 1, 0.1, 0.1, 0.1, 0.1);
                        
                        // Lift nearby entities
                        effectLoc.getWorld().getNearbyEntities(effectLoc, 2, 2, 2).stream()
                                .filter(entity -> entity instanceof LivingEntity)
                                .filter(entity -> !(entity instanceof Player) || 
                                        !((Player) entity).isFlying())
                                .forEach(entity -> {
                                    org.bukkit.util.Vector lift = new org.bukkit.util.Vector(0, 0.3, 0);
                                    entity.setVelocity(entity.getVelocity().add(lift));
                                });
                    }
                }, (long) (angle * 10));
            }
        }
    }
    
    /**
     * Creates an earthquake effect with ground manipulation
     */
    public void createEarthquake(Location center, double radius, String spellKey) {
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 6) {
            for (double r = 1; r <= radius; r += 0.5) {
                Location effectLoc = center.clone().add(
                        Math.cos(angle) * r,
                        0,
                        Math.sin(angle) * r
                );
                
                // Ground shake particles
                fxService.spawnParticle("BLOCK_DUST", effectLoc, 
                        10, 0.5, 0.2, 0.5, 0.1);
                
                // Knock back entities
                effectLoc.getWorld().getNearbyEntities(effectLoc, 2, 3, 2).stream()
                        .filter(entity -> entity instanceof LivingEntity)
                        .forEach(entity -> {
                            org.bukkit.util.Vector knockback = entity.getLocation()
                                    .subtract(center).toVector().normalize();
                            knockback.setY(0.5);
                            entity.setVelocity(knockback.multiply(1.1));
                        });
            }
        }
    }
    
    private boolean isSpellProtectedBlock(Location location) {
        return temporaryBlocks.containsKey(location) ||
               location.getChunk().getPersistentDataContainer()
                       .has(Keys.createKey("spell_protected_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ()), 
                            Keys.BOOLEAN_TYPE.getType());
    }
    
    private boolean hasActiveSpellEffect(Location location) {
        // Check for various active spell effects at location
        return temporaryBlocks.containsKey(location) ||
               spellIceBlocks.contains(location) ||
               isInTornadoZone(location) ||
               isInEarthquakeZone(location);
    }
    
    private boolean shouldPreventBlockPlacement(Location location) {
        // Some spell effects should prevent block placement
        return isInTornadoZone(location) || 
               hasForceFieldEffect(location);
    }
    
    private void handleTemporaryBlockBreak(BlockBreakEvent event, TemporaryBlock tempBlock) {
        // Show special break effects for spell blocks
        Location location = event.getBlock().getLocation();
        
        switch (tempBlock.spellKey()) {
            case "ice-wall", "ice-wave" -> {
                fxService.spawnParticle("SNOWFLAKE", location.add(0.5, 0.5, 0.5), 
                        15, 0.5, 0.5, 0.5, 0.1);
                fxService.playSound(location, "BLOCK_GLASS_BREAK", 0.8f, 1.2f);
            }
            case "lightwall" -> {
                fxService.spawnParticle("FIREWORKS_SPARK", location.add(0.5, 0.5, 0.5), 
                        20, 0.3, 0.3, 0.3, 0.2);
                fxService.playSound(location, "BLOCK_BEACON_DEACTIVATE", 0.6f, 1.5f);
            }
            case "stone-fortress", "wall" -> {
                fxService.spawnParticle("BLOCK_DUST", location.add(0.5, 0.5, 0.5), 
                        12, 0.4, 0.4, 0.4, 0.1);
                fxService.playSound(location, "BLOCK_STONE_BREAK", 1.0f, 0.8f);
            }
        }
        
        // Remove from tracking
        temporaryBlocks.remove(location);
        spellIceBlocks.remove(location);
    }
    
    private void handleSpellIceBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        
        // Ice break effects
        fxService.spawnParticle("SNOWFLAKE", location.add(0.5, 0.5, 0.5), 
                10, 0.3, 0.3, 0.3, 0.1);
        
        // Apply cold damage to breaker
        Player player = event.getPlayer();
        player.damage(1.0);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 0));
        
        spellIceBlocks.remove(location);
    }
    
    private void markSpellBlock(Location location, String spellKey) {
        Block block = location.getBlock();
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        
        // Store spell block data in chunk
        String key = "spell_block_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        pdc.set(Keys.createKey(key), Keys.STRING_TYPE.getType(), spellKey);
    }
    
    private boolean isInTornadoZone(Location location) {
        // Check if location is in an active tornado effect
        // This would be expanded based on actual tornado spell implementation
        return false;
    }
    
    private boolean isInEarthquakeZone(Location location) {
        // Check if location is in an active earthquake effect
        // This would be expanded based on actual earthquake spell implementation
        return false;
    }
    
    private boolean hasForceFieldEffect(Location location) {
        // Check for force field or barrier spells
        return location.getChunk().getPersistentDataContainer()
                .has(Keys.createKey("force_field_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ()), 
                     Keys.BOOLEAN_TYPE.getType());
    }
    
    private void cleanupTemporaryBlocks() {
        long now = System.currentTimeMillis();
        
        temporaryBlocks.entrySet().removeIf(entry -> {
            Location location = entry.getKey();
            TemporaryBlock tempBlock = entry.getValue();
            
            if (now >= tempBlock.expiryTime()) {
                // Restore original block on main thread
                plugin.getTaskManager().runTask(() -> {
                    Block block = location.getBlock();
                    block.setType(tempBlock.originalMaterial());
                    
                    // Removal effects
                    switch (tempBlock.spellKey()) {
                        case "ice-wall", "ice-wave" -> {
                            fxService.spawnParticle("SNOWFLAKE", location.add(0.5, 0.5, 0.5), 
                                    8, 0.3, 0.3, 0.3, 0.05);
                        }
                        case "lightwall" -> {
                            fxService.spawnParticle("FIREWORKS_SPARK", location.add(0.5, 0.5, 0.5), 
                                    12, 0.2, 0.2, 0.2, 0.1);
                        }
                    }
                });
                
                // Remove from ice tracking if applicable
                spellIceBlocks.remove(location);
                return true;
            }
            return false;
        });
    }
    
    private void processWeatherEffects() {
        long now = System.currentTimeMillis();
        
        activeWeatherSpells.entrySet().removeIf(entry -> {
            World world = entry.getKey();
            WeatherSpellData data = entry.getValue();
            
            if (now >= data.expiryTime()) {
                // Restore natural weather
                world.setStorm(false);
                world.setThundering(false);
                
                plugin.getLogger().info(String.format("Weather spell %s expired in world %s", 
                        data.spellKey(), world.getName()));
                return true;
            }
            return false;
        });
    }
    
    /**
     * Data record for temporary spell-created blocks
     */
    private record TemporaryBlock(Material originalMaterial, long expiryTime, String spellKey) {}
    
    /**
     * Data record for weather spell effects
     */
    private record WeatherSpellData(String spellKey, boolean targetWeatherState, long expiryTime) {}
}