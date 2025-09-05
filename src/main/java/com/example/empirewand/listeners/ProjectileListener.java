package com.example.empirewand.listeners;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.ProjectileSpell;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Routes projectile hit events back to the originating spell.
 */
public class ProjectileListener implements Listener {

    private final EmpireWandPlugin plugin;

    public ProjectileListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        String spellName = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_SPELL, PersistentDataType.STRING);
        if (spellName == null) {
            return;
        }

        Spell spell = plugin.getSpellRegistry().getSpell(spellName);
        if (!(spell instanceof ProjectileSpell projectileSpell)) {
            return;
        }

        Player caster = null;
        String ownerId = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER, PersistentDataType.STRING);
        if (ownerId != null) {
            caster = Bukkit.getPlayer(UUID.fromString(ownerId));
        }

        SpellContext context = new SpellContext(plugin, caster, plugin.getConfigService(), plugin.getFxService());
        projectileSpell.onProjectileHit(context, projectile, event);
    }
}
