package com.example.empirewand.spell.implementation;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class GlacialSpike implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setDamage(8.0);
        // Prevent pickup and add a slightly punchier feel
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setCritical(true);
        arrow.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, getName());
        arrow.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());

        new BukkitRunnable() {
            @Override
            public void run() {
                // Stop trail when the projectile is no longer valid (hit/removed)
                if (!arrow.isValid() || arrow.isDead()) {
                    this.cancel();
                    return;
                }
                context.fx().spawnParticles(arrow.getLocation(), Particle.SNOWFLAKE, 5, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    @Override
    public String getName() {
        return "glacial-spike";
    }
}
