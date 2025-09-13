package nl.wantedchef.empirewand.listener.spell;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.framework.service.FxService;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles aura spell effects including passive damage auras, protective auras,
 * and toggle-based aura management.
 */
public class AuraSpellListener implements Listener {
    private final EmpireWandPlugin plugin;
    private final FxService fxService;
    
    // Track active auras
    private final Map<UUID, Set<AuraEffect>> activeAuras = new ConcurrentHashMap<>();
    
    // Track aura participants for efficient proximity checking
    private final Set<UUID> auraParticipants = ConcurrentHashMap.newKeySet();
    
    // Aura processing task
    private static final long AURA_TICK_INTERVAL = 15L; // 0.75 seconds
    
    public AuraSpellListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.fxService = plugin.getFxService();
        
        // Main aura processing task
        plugin.getTaskManager().runTaskTimer(this::processAuras, 20L, AURA_TICK_INTERVAL);
        
        // Cleanup task for expired auras
        plugin.getTaskManager().runTaskTimer(this::cleanupExpiredAuras, 200L, 200L);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Restore any persistent auras
        restorePersistentAuras(player);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Save persistent auras
        savePersistentAuras(player);
        
        // Remove from active tracking
        activeAuras.remove(playerId);
        auraParticipants.remove(playerId);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Only process if player has moved significantly or has auras
        boolean moved = event.getFrom().distanceSquared(event.getTo()) >= 0.01;
        if (moved || hasActiveAuras(player)) {
            // Check for entering/leaving aura ranges
            checkAuraProximity(player);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // Check for aura-based damage reduction or reflection
        handleAuraProtection(event, player);
    }
    
    /**
     * Activates an aura spell for a player
     */
    public void activateAura(Player player, String spellKey, long durationTicks) {
        UUID playerId = player.getUniqueId();
        
        // Get aura configuration from spell config
        var spellConfig = plugin.getConfigService().getSpellsConfig();
        String configPath = spellKey + ".values.";
        
        double radius = spellConfig.getDouble(configPath + "radius", 5.0);
        double damage = spellConfig.getDouble(configPath + "damage-per-tick", 2.0);
        double healAmount = spellConfig.getDouble(configPath + "heal-amount", 1.0);
        int tickInterval = spellConfig.getInt(configPath + "tick-interval", 20);
        
        AuraEffect aura = new AuraEffect(
                spellKey,
                AuraType.fromSpellKey(spellKey),
                radius,
                damage,
                healAmount,
                tickInterval,
                System.currentTimeMillis() + (durationTicks * 50L)
        );
        
        // Add to active auras
        activeAuras.computeIfAbsent(playerId, k -> new HashSet<>()).add(aura);
        auraParticipants.add(playerId);
        
        // Mark player as having active aura
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(Keys.createKey("aura_" + spellKey), Keys.LONG_TYPE.getType(), aura.expiryTime());
        
        // Activation effects
        fxService.spawnParticles(player.getLocation().add(0, 2, 0), 
                Particle.FIREWORK, 15, 1, 1, 1, 0.2);
        fxService.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
        
        plugin.getLogger().info(String.format("Activated %s aura for player %s", spellKey, player.getName()));
    }
    
    /**
     * Deactivates a specific aura for a player
     */
    public void deactivateAura(Player player, String spellKey) {
        UUID playerId = player.getUniqueId();
        
        Set<AuraEffect> playerAuras = activeAuras.get(playerId);
        if (playerAuras != null) {
            playerAuras.removeIf(aura -> aura.spellKey().equals(spellKey));
            
            if (playerAuras.isEmpty()) {
                activeAuras.remove(playerId);
                auraParticipants.remove(playerId);
            }
        }
        
        // Remove persistent data
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.remove(Keys.createKey("aura_" + spellKey));
        
        // Deactivation effects
        fxService.spawnParticles(player.getLocation().add(0, 1, 0), 
                Particle.SMOKE, 10, 0.5, 0.5, 0.5, 0.1);
        
        plugin.getLogger().info(String.format("Deactivated %s aura for player %s", spellKey, player.getName()));
    }
    
    /**
     * Toggles an aura spell (for toggle-type auras)
     */
    @SuppressWarnings("unused")
    public boolean toggleAura(Player player, String spellKey) {
        if (hasActiveAura(player, spellKey)) {
            deactivateAura(player, spellKey);
            return false;
        } else {
            // Use long duration for toggle auras
            activateAura(player, spellKey, 36000L); // 30 minutes
            return true;
        }
    }
    
    /**
     * Checks if a player has any active auras
     */
    public boolean hasActiveAuras(Player player) {
        Set<AuraEffect> playerAuras = activeAuras.get(player.getUniqueId());
        return playerAuras != null && !playerAuras.isEmpty();
    }
    
    /**
     * Checks if a player has a specific active aura
     */
    public boolean hasActiveAura(Player player, String spellKey) {
        Set<AuraEffect> playerAuras = activeAuras.get(player.getUniqueId());
        if (playerAuras == null) {
            return false;
        }
        
        return playerAuras.stream().anyMatch(aura -> aura.spellKey().equals(spellKey));
    }
    
    private void processAuras() {
        long currentTime = System.currentTimeMillis();
        
        for (UUID playerId : new HashSet<>(auraParticipants)) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            
            Set<AuraEffect> playerAuras = activeAuras.get(playerId);
            if (playerAuras == null || playerAuras.isEmpty()) {
                continue;
            }
            
            Location playerLocation = player.getLocation();
            
            for (AuraEffect aura : new HashSet<>(playerAuras)) {
                // Check if it's time to tick this aura
                if (currentTime % (aura.tickInterval() * 50L) != 0) {
                    continue;
                }
                
                processAuraEffect(player, playerLocation, aura);
            }
        }
    }
    
    private void processAuraEffect(Player player, Location center, AuraEffect aura) {
        switch (aura.type()) {
            case DAMAGE -> processDamageAura(player, center, aura);
            case HEAL -> processHealAura(player, center, aura);
            case PROTECTION -> processProtectionAura(player, center, aura);
            case DIVINE -> processDivineAura(player, center, aura);
            case EVIL -> processEvilAura(player, center, aura);
            case EMPIRE -> processEmpireAura(player, center, aura);
        }
        
        // Common aura visual effects
        createAuraVisuals(center, aura);
    }
    
    private void processDamageAura(Player caster, Location center, AuraEffect aura) {
        List<LivingEntity> targets = center.getWorld().getLivingEntities().stream()
                .filter(entity -> entity != caster)
                .filter(entity -> entity.getLocation().distance(center) <= aura.radius())
                .filter(this::isValidAuraTarget)
                .toList();
        
        for (LivingEntity target : targets) {
            // Apply damage
            target.damage(aura.damage(), caster);
            
            // Damage particles
            fxService.spawnParticles(target.getLocation().add(0, 1, 0), 
                    Particle.CRIT, 3, 0.3, 0.3, 0.3, 0.1);
            
            // Special aura-specific effects
            applyAuraSpecificEffects(target, aura);
        }
    }
    
    private void processHealAura(Player caster, Location center, AuraEffect aura) {
        List<Player> allies = center.getWorld().getPlayers().stream()
                .filter(player -> player.getLocation().distance(center) <= aura.radius())
                .filter(player -> isAlly(caster, player))
                .toList();
        
        for (Player ally : allies) {
            double max = getMaxHealth(ally);
            if (ally.getHealth() < max) {
                ally.setHealth(Math.min(max, ally.getHealth() + aura.healAmount()));
                
                // Healing particles
                fxService.spawnParticles(ally.getLocation().add(0, 2, 0), 
                        Particle.HEART, 2, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    private double getMaxHealth(LivingEntity entity) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
    
    private void processProtectionAura(Player caster, Location center, AuraEffect aura) {
        List<Player> allies = center.getWorld().getPlayers().stream()
                .filter(player -> player.getLocation().distance(center) <= aura.radius())
                .filter(player -> isAlly(caster, player))
                .toList();
        
        for (Player ally : allies) {
            // Apply temporary resistance
            ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 
                    aura.tickInterval() + 10, 0));
            
            // Protection particles
            fxService.spawnParticles(ally.getLocation().add(0, 1.5, 0), 
                    Particle.END_ROD, 2, 0.3, 0.3, 0.3, 0.05);
        }
    }
    
    private void processDivineAura(Player caster, Location center, AuraEffect aura) {
        // Divine aura: heal allies, damage undead
        List<LivingEntity> entities = center.getWorld().getLivingEntities().stream()
                .filter(entity -> entity.getLocation().distance(center) <= aura.radius())
                .toList();
        
        for (LivingEntity entity : entities) {
            if (entity instanceof Player player && isAlly(caster, player)) {
                // Heal allies
                double max = getMaxHealth(player);
                if (player.getHealth() < max) {
                    player.setHealth(Math.min(max, player.getHealth() + aura.healAmount()));
                }
                
                // Divine blessing effects
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 
                        aura.tickInterval(), 0));
                
            } else if (isUndead(entity)) {
                // Damage undead
                entity.damage(aura.damage() * 1.5, caster);
                
                // Smite particles
                fxService.spawnParticles(entity.getLocation().add(0, 1, 0), 
                        Particle.END_ROD, 8, 0.4, 0.5, 0.4, 0.2);
            }
        }
        
        // Divine light particles
        fxService.spawnParticles(center.clone().add(0, 2, 0), 
                Particle.END_ROD, 12, aura.radius() / 2, 1, aura.radius() / 2, 0.1);
    }
    
    private void processEvilAura(Player caster, Location center, AuraEffect aura) {
        // Evil aura: damage based on distance, apply fear effects
        List<LivingEntity> targets = center.getWorld().getLivingEntities().stream()
                .filter(entity -> entity != caster)
                .filter(entity -> entity.getLocation().distance(center) <= aura.radius())
                .filter(this::isValidAuraTarget)
                .toList();
        
        for (LivingEntity target : targets) {
            double distance = target.getLocation().distance(center);
            double distanceBasedDamage = aura.damage() * (1.0 - (distance / aura.radius()));
            
            // Apply distance-based damage
            target.damage(Math.max(0.5, distanceBasedDamage), caster);
            
            // Fear effects
            if (target instanceof Player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
            }
            
            // Dark particles
            fxService.spawnParticles(target.getLocation().add(0, 1, 0), 
                    Particle.SMOKE, 5, 0.3, 0.3, 0.3, 0.05);
        }
    }
    
    private void processEmpireAura(Player caster, Location center, AuraEffect aura) {
        // Empire aura: powerful damage with golden effects
        List<LivingEntity> targets = center.getWorld().getLivingEntities().stream()
                .filter(entity -> entity != caster)
                .filter(entity -> entity.getLocation().distance(center) <= aura.radius())
                .filter(this::isValidAuraTarget)
                .toList();
        
        for (LivingEntity target : targets) {
            // Enhanced damage
            target.damage(aura.damage() * 1.3, caster);
            
            // Empire effects
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0));
            
            // Golden particles
            fxService.spawnParticles(target.getLocation().add(0, 1, 0), 
                    Particle.CRIT, 8, 0.4, 0.5, 0.4, 0.15);
        }
        
        // Imperial aura visuals
        fxService.spawnParticles(center.clone().add(0, 1, 0), 
                Particle.CRIT, 15, aura.radius() / 2, 0.5, aura.radius() / 2, 0.1);
    }
    
    private void applyAuraSpecificEffects(LivingEntity target, AuraEffect aura) {
        switch (aura.spellKey()) {
            case "evil-goons-aura" -> {
                // Apply fear and weakness
                if (target instanceof Player) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                }
            }
            case "empire-aura" -> {
                // Apply glowing mark
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
            }
            case "divine-aura" -> {
                // Smite effect on undead
                if (isUndead(target)) {
                    target.damage(aura.damage() * 0.5);
                }
            }
        }
    }
    
    private void createAuraVisuals(Location center, AuraEffect aura) {
        Particle particleType = getAuraParticleType(aura.type());
        
        // Create circular particle effect
        for (int i = 0; i < 8; i++) {
            double angle = (i * Math.PI * 2) / 8;
            Location particleLocation = center.clone().add(
                    Math.cos(angle) * aura.radius(),
                    0.5,
                    Math.sin(angle) * aura.radius()
            );
            
            fxService.spawnParticles(particleLocation, particleType, 1, 0.1, 0.1, 0.1, 0);
        }
        
        // Central aura effect
        fxService.spawnParticles(center.clone().add(0, 1, 0), particleType, 
                3, 0.3, 0.3, 0.3, 0.05);
    }
    
    private Particle getAuraParticleType(AuraType type) {
        return switch (type) {
            case DAMAGE -> Particle.CRIT;
            case HEAL -> Particle.HEART;
            case PROTECTION -> Particle.END_ROD;
            case DIVINE -> Particle.END_ROD;
            case EVIL -> Particle.SMOKE;
            case EMPIRE -> Particle.CRIT;
        };
    }
    
    private void checkAuraProximity(Player player) {
        // Placeholder for future proximity-based aura events.
        if (hasActiveAuras(player)) {
            // future: entry/exit effects
        }
    }
    
    private void handleAuraProtection(EntityDamageEvent event, Player player) {
        Set<AuraEffect> playerAuras = activeAuras.get(player.getUniqueId());
        if (playerAuras == null) {
            return;
        }
        
        for (AuraEffect aura : playerAuras) {
            if (aura.type() == AuraType.PROTECTION || aura.type() == AuraType.DIVINE) {
                // Reduce damage by 30%
                event.setDamage(event.getDamage() * 0.7);
                
                // Protection visual effect
                fxService.spawnParticles(player.getLocation().add(0, 1, 0), 
                        Particle.END_ROD, 5, 0.3, 0.3, 0.3, 0.1);
                break;
            }
        }
    }
    
    private boolean isValidAuraTarget(LivingEntity entity) {
        // Exclude players in creative/spectator mode
        if (entity instanceof Player player) {
            GameMode mode = player.getGameMode();
            return mode != GameMode.CREATIVE && 
                   mode != GameMode.SPECTATOR;
        }
        return true;
    }
    
    private boolean isAlly(Player caster, Player target) {
        // Basic ally check - can be expanded with party/faction systems
        return caster.equals(target) || 
               (plugin.getConfigService().getConfig().getBoolean("auras.heal-all-players", false));
    }
    
    private boolean isUndead(LivingEntity entity) {
        String entityType = entity.getType().name().toLowerCase(Locale.ROOT);
        return entityType.contains("zombie") || entityType.contains("skeleton") || 
               entityType.contains("wither") || entityType.contains("phantom");
    }
    
    private void cleanupExpiredAuras() {
        long currentTime = System.currentTimeMillis();
        
        for (UUID playerId : new HashSet<>(activeAuras.keySet())) {
            Set<AuraEffect> playerAuras = activeAuras.get(playerId);
            if (playerAuras == null) {
                continue;
            }
            
            // Remove expired auras
            boolean removed = playerAuras.removeIf(aura -> currentTime >= aura.expiryTime());
            
            if (removed) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) {
                    // Cleanup persistent data for expired auras
                    PersistentDataContainer pdc = player.getPersistentDataContainer();
                    for (AuraEffect aura : new HashSet<>(playerAuras)) {
                        if (currentTime >= aura.expiryTime()) {
                            pdc.remove(Keys.createKey("aura_" + aura.spellKey()));
                        }
                    }
                }
            }
            
            // Remove player from tracking if no auras remain
            if (playerAuras.isEmpty()) {
                activeAuras.remove(playerId);
                auraParticipants.remove(playerId);
            }
        }
    }
    
    private void restorePersistentAuras(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        // Check for persistent aura data
        for (String spellKey : Arrays.asList("divine-aura", "evil-goons-aura", "empire-aura")) {
            Long expiryTime = pdc.get(Keys.createKey("aura_" + spellKey), Keys.LONG_TYPE.getType());
            if (expiryTime != null && System.currentTimeMillis() < expiryTime) {
                // Restore aura
                long remainingTime = (expiryTime - System.currentTimeMillis()) / 50L;
                activateAura(player, spellKey, remainingTime);
            }
        }
    }
    
    private void savePersistentAuras(@SuppressWarnings("unused") Player player) {
        // Persistent data is already saved in PersistentDataContainer
        // This method could be expanded for additional persistence needs
    }
    
    /**
     * Aura effect data record
     */
    private record AuraEffect(
            String spellKey,
            AuraType type,
            double radius,
            double damage,
            double healAmount,
            int tickInterval,
            long expiryTime
    ) {}
    
    /**
     * Aura type enumeration
     */
    private enum AuraType {
        DAMAGE, HEAL, PROTECTION, DIVINE, EVIL, EMPIRE;
        
        public static AuraType fromSpellKey(String spellKey) {
            return switch (spellKey.toLowerCase()) {
                case "divine-aura" -> DIVINE;
                case "evil-goons-aura" -> EVIL;
                case "empire-aura" -> EMPIRE;
                default -> DAMAGE;
            };
        }
    }
}