package com.example.empirewand.spell;

import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * Optional extension for spells that spawn projectiles and need hit callbacks.
 */
public interface ProjectileSpell extends Spell {
    void onProjectileHit(SpellContext context, Projectile projectile, ProjectileHitEvent event);
}
