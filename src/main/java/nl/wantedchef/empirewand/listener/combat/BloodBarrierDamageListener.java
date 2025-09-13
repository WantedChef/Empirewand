package nl.wantedchef.empirewand.listener.combat;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.spell.life.BloodBarrier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener for BloodBarrier spell damage events.
 * Handles damage reduction and thorns damage when a player with an active BloodBarrier is attacked.
 */
public class BloodBarrierDamageListener implements Listener {

    public BloodBarrierDamageListener(EmpireWandPlugin plugin) {
        // Plugin reference not needed since BloodBarrier.handleDamageEvent is now static
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        BloodBarrier.handleDamageEvent(event);
    }
}