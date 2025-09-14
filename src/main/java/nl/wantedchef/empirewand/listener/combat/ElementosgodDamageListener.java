package nl.wantedchef.empirewand.listener.combat;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.spell.toggle.godmode.Elementosgod;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener that applies Elementosgod's reflective stance when the caster is
 * attacked by another entity.
 */
public class ElementosgodDamageListener implements Listener {

    /**
     * Creates a new listener for Elementosgod damage events.
     *
     * @param plugin the plugin instance
     */
    public ElementosgodDamageListener(EmpireWandPlugin plugin) {
        // Plugin reference not required
    }

    /**
     * Handles incoming damage and triggers reflection.
     *
     * @param event the damage event
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Elementosgod.handleDamageEvent(event);
    }
}
