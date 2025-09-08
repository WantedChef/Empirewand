package com.example.empirewand.listeners.combat;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.spell.implementation.control.Polymorph;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Fail-safes voor Polymorph: ruim mappings op wanneer het schaap of het
 * origineel verdwijnt.
 */
public final class PolymorphCleanupListener implements Listener {
    private final EmpireWandPlugin plugin;

    public PolymorphCleanupListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    private Optional<Polymorph> getPolymorph() {
        return plugin.getSpellRegistry().getSpell("polymorph")
                .filter(Polymorph.class::isInstance)
                .map(Polymorph.class::cast);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        var polyOpt = getPolymorph();
        if (polyOpt.isEmpty())
            return;
        var poly = polyOpt.get();
        UUID id = event.getEntity().getUniqueId();
        // Probeer als schaap; anders als origineel
        if (!poly.revertBySheep(id)) {
            poly.revertByOriginal(id);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        var polyOpt = getPolymorph();
        if (polyOpt.isEmpty())
            return;
        var poly = polyOpt.get();
        poly.revertByOriginal(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        var polyOpt = getPolymorph();
        if (polyOpt.isEmpty())
            return;
        var poly = polyOpt.get();
        var map = poly.getPolymorphedEntities();
        // Als een schaap in deze chunk staat, revert het veilig.
        for (Entity e : event.getChunk().getEntities()) {
            UUID id = e.getUniqueId();
            if (map.containsKey(id)) {
                poly.revertBySheep(id);
            } else if (e instanceof LivingEntity) {
                // als het origineel in de chunk staat, niets doen; revert gebeurt bij unload
                // via death/remove events
                // niets
            }
        }
    }
}
