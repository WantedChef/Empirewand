package nl.wantedchef.empirewand.listener.combat;

import nl.wantedchef.empirewand.core.storage.Keys;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

/**
 * Prevents swarm minions from targeting or damaging their caster.
 */
public final class MinionFriendlyFireListener implements Listener {

    private static boolean isOwnedBy(@NotNull Entity entity, @NotNull Player player) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            String owner = pdc.get(Keys.createKey("minion.owner"), Keys.STRING_TYPE.getType());
            return owner != null && owner.equals(player.getUniqueId().toString());
        } catch (Exception e) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        Entity source = event.getEntity();
        Entity target = event.getTarget();
        if (!(source instanceof Vex) || !(target instanceof Player player)) {
            return;
        }
        if (isOwnedBy(source, player)) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        if (!(damager instanceof Vex) || !(victim instanceof Player player)) {
            return;
        }
        if (isOwnedBy(damager, player)) {
            event.setCancelled(true);
        }
    }
}


