package nl.wantedchef.empirewand.listener.projectile;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.Spell;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Stuurt projectile-hits door naar spells die ProjectileSpell implementeren.
 */
public final class ProjectileHitListener implements Listener {
    private final EmpireWandPlugin plugin;

    public ProjectileHitListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        String spellKey = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_SPELL,
                Keys.STRING_TYPE.getType());
        if (spellKey == null || spellKey.isEmpty())
            return;

        Optional<Spell<?>> spellOpt = plugin.getSpellRegistry().getSpell(spellKey);
        if (spellOpt.isEmpty() || !(spellOpt.get() instanceof ProjectileSpell<?> pSpell))
            return;

        Player caster = null;
        String ownerId = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER, Keys.STRING_TYPE.getType());
        if (ownerId != null) {
            try {
                caster = Bukkit.getPlayer(UUID.fromString(ownerId));
            } catch (IllegalArgumentException ignored) {
            }
        }

        try {
            if (caster != null) {
                pSpell.onProjectileHit(event, caster);
            }
        } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "ProjectileSpell error for '" + spellKey + "'", t);
        }
    }
}





