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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        
        // Early null check for performance
        if (projectile == null) {
            return;
        }
        
        // Get spell key with null safety
        var stringType = Keys.STRING_TYPE.getType();
        if (stringType == null) {
            return;
        }
        
        String spellKey = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_SPELL, stringType);
        if (spellKey == null || spellKey.isEmpty()) {
            return;
        }

        // Validate spell exists and is ProjectileSpell
        Optional<Spell<?>> spellOpt = plugin.getSpellRegistry().getSpell(spellKey);
        if (spellOpt.isEmpty()) {
            return;
        }
        
        Spell<?> spell = spellOpt.get();
        if (!(spell instanceof ProjectileSpell<?> pSpell)) {
            return;
        }

        // Get caster with improved error handling
        Player caster = null;
        String ownerId = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER, stringType);
        if (ownerId != null && !ownerId.isEmpty()) {
            try {
                UUID ownerUUID = UUID.fromString(ownerId);
                caster = Bukkit.getPlayer(ownerUUID);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(String.format("Invalid UUID format for projectile owner: %s", ownerId));
                return;
            }
        }

        // Only proceed if we have a valid caster
        if (caster == null) {
            return;
        }

        // Handle projectile hit with comprehensive error handling
        try {
            pSpell.onProjectileHit(event, caster);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, 
                String.format("ProjectileSpell '%s' threw exception during hit handling: %s", spellKey, e.getMessage()), e);
            
            // Fire SpellFailEvent for error tracking
            var failEvent = new nl.wantedchef.empirewand.api.event.SpellFailEvent(
                caster, spell, spellKey, nl.wantedchef.empirewand.api.event.SpellFailEvent.FailReason.OTHER, "ProjectileSpell hit handling failed: " + e.getMessage()
            );
            plugin.getServer().getPluginManager().callEvent(failEvent);
        }
    }
}





