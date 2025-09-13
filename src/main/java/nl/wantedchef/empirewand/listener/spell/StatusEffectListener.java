package nl.wantedchef.empirewand.listener.spell;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.framework.service.FxService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


import java.util.Set;

/**
 * Handles spell-related status effects and their interactions.
 * Monitors spell-applied effects and handles cleanup/interactions.
 */
public class StatusEffectListener implements Listener {
    private final EmpireWandPlugin plugin;
    private final FxService fxService;
    
    // Status effects that should trigger special handling
    private static final Set<PotionEffectType> SPELL_EFFECTS = Set.of(
        PotionEffectType.SLOWNESS,      // Ice/control spells
        PotionEffectType.POISON,        // Poison spells
        PotionEffectType.WITHER,        // Dark spells
        PotionEffectType.BLINDNESS,     // Control spells
        PotionEffectType.INVISIBILITY,  // Shadow cloak
        PotionEffectType.LEVITATION,    // Movement spells
        PotionEffectType.GLOWING,       // Lightning/marking spells
        PotionEffectType.WEAKNESS,      // Debuff spells
        PotionEffectType.MINING_FATIGUE // Ice/control spells
    );
    
    public StatusEffectListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.fxService = plugin.getFxService();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPotionEffectAdd(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) {
            return;
        }
        
        PotionEffect effect = event.getNewEffect();
        if (effect == null || !SPELL_EFFECTS.contains(effect.getType())) {
            return;
        }
        
        // Track spell-applied effects for cleanup
        handleSpellEffectApplied(player, effect);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPotionEffectRemove(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        if (event.getAction() != EntityPotionEffectEvent.Action.REMOVED) {
            return;
        }
        
        PotionEffect effect = event.getOldEffect();
        if (effect == null || !SPELL_EFFECTS.contains(effect.getType())) {
            return;
        }
        
        handleSpellEffectRemoved(player, effect);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check for hemorrhage movement detection
        if (hasHemorrhageEffect(player)) {
            handleHemorrhageMovement(player, event);
        }
        
        // Check for movement-canceling effects
        if (hasRootingEffect(player)) {
            handleRootingMovement(player, event);
        }
        
        // Check for shadow cloak light level cancellation
        if (hasShadowCloakEffect(player)) {
            handleShadowCloakMovement(player);
        }
    }
    
    private void handleSpellEffectApplied(Player player, PotionEffect effect) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        // Mark effect as spell-applied for proper cleanup
        String effectKey = "spell_effect_" + effect.getType().getKey().getKey();
        pdc.set(Keys.createKey(effectKey), Keys.LONG_TYPE.getType(), 
                System.currentTimeMillis() + (effect.getDuration() * 50L));
        
        // Special handling for specific effects
        switch (effect.getType().getKey().getKey().toUpperCase()) {
            case "POISON" -> handlePoisonEffectApplied(player, effect);
            case "WITHER" -> handleWitherEffectApplied(player, effect);
            case "BLINDNESS" -> handleBlindnessEffectApplied(player, effect);
            case "INVISIBILITY" -> handleInvisibilityEffectApplied(player, effect);
        }
    }
    
    private void handleSpellEffectRemoved(Player player, PotionEffect effect) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        // Remove spell effect tracking
        String effectKey = "spell_effect_" + effect.getType().getKey().getKey();
        pdc.remove(Keys.createKey(effectKey));
        
        // Special cleanup for specific effects
        switch (effect.getType().getKey().getKey().toUpperCase()) {
            case "INVISIBILITY" -> handleInvisibilityEffectRemoved(player);
            case "LEVITATION" -> handleLevitationEffectRemoved(player);
            case "SLOWNESS" -> handleSlownessEffectRemoved(player);
        }
    }
    
    private boolean hasHemorrhageEffect(Player player) {
        return player.getPersistentDataContainer()
                .has(Keys.createKey("hemorrhage_active"), Keys.BOOLEAN_TYPE.getType());
    }
    
    private void handleHemorrhageMovement(Player player, PlayerMoveEvent event) {
        // Get movement distance
        double distance = event.getFrom().distance(event.getTo());
        
        if (distance > 0.8) { // Threshold for "significant" movement
            // Apply additional hemorrhage damage
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            Double baseDamage = pdc.get(Keys.createKey("hemorrhage_base_damage"), Keys.DOUBLE_TYPE.getType());
            Double bonusDamage = pdc.get(Keys.createKey("hemorrhage_movement_bonus"), Keys.DOUBLE_TYPE.getType());
            
            if (baseDamage != null && bonusDamage != null) {
                double totalDamage = baseDamage + bonusDamage;
                
                // Apply damage
                plugin.getTaskManager().runTask(() -> {
                    if (player.isOnline() && player.isValid()) {
                        player.damage(totalDamage);
                        
                        // Show blood particle effects
                        fxService.spawnParticle("REDSTONE", player.getLocation().add(0, 1, 0), 
                                5, 0.3, 0.3, 0.3, 0);
                    }
                });
            }
        }
    }
    
    private boolean hasRootingEffect(Player player) {
        return player.hasPotionEffect(PotionEffectType.SLOWNESS) &&
               player.getPotionEffect(PotionEffectType.SLOWNESS) != null &&
               player.getPotionEffect(PotionEffectType.SLOWNESS).getAmplifier() >= 250;
    }
    
    private void handleRootingMovement(Player player, PlayerMoveEvent event) {
        // Cancel horizontal movement for extreme slowness (rooting)
        if (event.getFrom().getX() != event.getTo().getX() || 
            event.getFrom().getZ() != event.getTo().getZ()) {
            
            event.getTo().setX(event.getFrom().getX());
            event.getTo().setZ(event.getFrom().getZ());
            
            // Show rooting particles
            fxService.spawnParticle("VILLAGER_ANGRY", player.getLocation(), 2, 0.5, 0.2, 0.5, 0);
        }
    }
    
    private boolean hasShadowCloakEffect(Player player) {
        return player.getPersistentDataContainer()
                .has(Keys.createKey("shadow_cloak_active"), Keys.BOOLEAN_TYPE.getType());
    }
    
    private void handleShadowCloakMovement(Player player) {
        // Check light level and cancel shadow cloak if too bright
        int lightLevel = player.getLocation().getBlock().getLightLevel();
        int maxLightLevel = player.getPersistentDataContainer()
                .getOrDefault(Keys.createKey("shadow_cloak_max_light"), Keys.INTEGER_TYPE.getType(), 4);
        
        if (lightLevel > maxLightLevel) {
            // Cancel shadow cloak
            player.getPersistentDataContainer().remove(Keys.createKey("shadow_cloak_active"));
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            
            fxService.sendActionBar(player, "&c&lYour cloak was ripped away by the light!");
            fxService.spawnParticle("SMOKE_LARGE", player.getLocation(), 20, 1, 1, 1, 0.1);
        }
    }
    
    private void handlePoisonEffectApplied(Player player, PotionEffect effect) {
        // Show poison application effects
        fxService.spawnParticle("VILLAGER_HAPPY", player.getLocation().add(0, 1, 0), 
                10, 0.5, 0.5, 0.5, 0);
        fxService.playSound(player, "ENTITY_WITCH_HURT", 0.7f, 1.2f);
    }
    
    private void handleWitherEffectApplied(Player player, PotionEffect effect) {
        // Show wither application effects
        fxService.spawnParticle("SMOKE_NORMAL", player.getLocation().add(0, 1, 0), 
                15, 0.3, 0.5, 0.3, 0.05);
        fxService.playSound(player, "ENTITY_WITHER_AMBIENT", 0.5f, 1.5f);
    }
    
    private void handleBlindnessEffectApplied(Player player, PotionEffect effect) {
        // Show blindness application effects
        fxService.spawnParticle("SMOKE_LARGE", player.getEyeLocation(), 
                8, 0.2, 0.2, 0.2, 0);
    }
    
    private void handleInvisibilityEffectApplied(Player player, PotionEffect effect) {
        // Shadow cloak activation effects
        if (hasShadowCloakEffect(player)) {
            fxService.spawnParticle("PORTAL", player.getLocation(), 25, 1, 1, 1, 0.5);
            fxService.playSound(player, "ENTITY_ENDERMAN_TELEPORT", 0.6f, 1.3f);
        }
    }
    
    private void handleInvisibilityEffectRemoved(Player player) {
        // Shadow cloak deactivation effects
        if (player.getPersistentDataContainer().has(Keys.createKey("shadow_cloak_deactivating"), Keys.BOOLEAN_TYPE.getType())) {
            fxService.spawnParticle("SMOKE_LARGE", player.getLocation(), 20, 0.5, 1, 0.5, 0.1);
            player.getPersistentDataContainer().remove(Keys.createKey("shadow_cloak_deactivating"));
        }
    }
    
    private void handleLevitationEffectRemoved(Player player) {
        // Add slow falling to prevent fall damage
        if (player.getPersistentDataContainer().has(Keys.createKey("spell_levitation_protection"), Keys.BOOLEAN_TYPE.getType())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
            player.getPersistentDataContainer().remove(Keys.createKey("spell_levitation_protection"));
        }
    }
    
    private void handleSlownessEffectRemoved(Player player) {
        // Remove slow falling effect
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }
}