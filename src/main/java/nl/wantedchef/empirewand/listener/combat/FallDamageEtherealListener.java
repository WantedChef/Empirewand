package nl.wantedchef.empirewand.listener.combat;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.framework.service.FxService;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;

/**
 * Cancel fall-damage wanneer Ethereal actief is (met failsafe cleanup).
 */
public final class FallDamageEtherealListener implements Listener {
    private final EmpireWandPlugin plugin;

    public FallDamageEtherealListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onFallDamage(EntityDamageEvent event) {
        // Early exit for performance - check cause first
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        
        // Pattern matching for better performance
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        var booleanType = Keys.BOOLEAN_TYPE.getType();
        var longType = Keys.LONG_TYPE.getType();
        
        // Defensive programming - ensure types are available
        if (booleanType == null || longType == null) {
            return;
        }
        
        // Check if ethereal is active
        Byte activeValue = pdc.get(Keys.ETHEREAL_ACTIVE, booleanType);
        boolean active = (activeValue != null && activeValue == (byte) 1);
        
        if (!active) {
            return; // Early exit if not ethereal
        }
        
        Long expires = pdc.get(Keys.ETHEREAL_EXPIRES_TICK, longType);
        long now = player.getWorld().getFullTime();

        // Check if ethereal effect has expired
        if (expires != null && now > expires) {
            // Cleanup expired ethereal state
            pdc.remove(Keys.ETHEREAL_ACTIVE);
            pdc.remove(Keys.ETHEREAL_EXPIRES_TICK);
            return; // Don't cancel damage if expired
        }

        // Ethereal is active and not expired, cancel fall damage
        event.setCancelled(true);
        
        // Show ethereal effect
        FxService fx = plugin.getFxService();
        var location = player.getLocation();
        if (location != null) {
            fx.spawnParticles(location, Particle.END_ROD, 10, 0.3, 0.1, 0.3, 0.0);
        }
        fx.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_FALL, 0.6f, 1.6f);
    }
}





