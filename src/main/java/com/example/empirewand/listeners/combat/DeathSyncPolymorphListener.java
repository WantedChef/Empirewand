package com.example.empirewand.listeners.combat;

import com.example.empirewand.EmpireWandPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Synchroniseert dood van polymorphed entiteiten terug naar het origineel.
 * Reflections voorkomen harde koppeling aan een concrete Polymorph-impl.
 */
public final class DeathSyncPolymorphListener implements Listener {
    private final EmpireWandPlugin plugin;

    public DeathSyncPolymorphListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        var spell = plugin.getSpellRegistry().getSpell("polymorph");
        if (!spell.isPresent())
            return;
        try {
            var methodMap = spell.get().getClass().getMethod("getPolymorphedEntities");
            Object mapObj = methodMap.invoke(spell.get());
            if (mapObj instanceof java.util.Map<?, ?> map) {
                Object originalId = map.get(dead.getUniqueId());
                if (originalId instanceof java.util.UUID uuid) {
                    var original = dead.getServer().getEntity(uuid);
                    if (original instanceof LivingEntity victim) {
                        victim.setHealth(0.0);
                    }
                    map.remove(dead.getUniqueId());
                }
            }
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException
                | ClassCastException ignored) {
            // Als Polymorph ontbreekt of API anders is: veilig negeren.
        }
    }
}