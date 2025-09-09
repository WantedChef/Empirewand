package com.example.empirewand.listeners.combat;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.storage.Keys;
import com.example.empirewand.core.services.FxService;
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
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;
        if (!(event.getEntity() instanceof Player player))
            return;

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        var booleanType = Keys.BOOLEAN_TYPE.getType();
        var longType = Keys.LONG_TYPE.getType();
        if (booleanType == null || longType == null) {
            return; // Types not available, skip processing
        }
        boolean active = (pdc.get(Keys.ETHEREAL_ACTIVE, booleanType) != null
                && pdc.get(Keys.ETHEREAL_ACTIVE, booleanType) == (byte) 1);
        Long expires = pdc.get(Keys.ETHEREAL_EXPIRES_TICK, longType);
        long now = player.getWorld().getFullTime();

        if (active && (expires == null || now <= expires)) {
            event.setCancelled(true);
            FxService fx = plugin.getFxService();
            if (player.getLocation() != null) {
                fx.spawnParticles(player.getLocation(), Particle.END_ROD, 10, 0.3, 0.1, 0.3, 0.0);
            }
            fx.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_FALL, 0.6f, 1.6f);
        }

        if (active && expires != null && now > expires) {
            pdc.remove(Keys.ETHEREAL_ACTIVE);
            pdc.remove(Keys.ETHEREAL_EXPIRES_TICK);
        }
    }
}