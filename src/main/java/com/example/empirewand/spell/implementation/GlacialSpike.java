package com.example.empirewand.spell.implementation;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class GlacialSpike implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Arrow arrow = player.launchProjectile(Arrow.class);
        double damage = context.config().getSpellsConfig().getDouble("glacial-spike.values.damage", 8.0);
        arrow.setDamage(damage);
        // Prevent pickup and add a slightly punchier feel
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setCritical(true);
        arrow.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType(), getName());
        arrow.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, Keys.STRING_TYPE.getType(),
                player.getUniqueId().toString());

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

    @Override
    public String key() {
        return "glacial-spike";
    }

    @Override
    public Component displayName() {
        return Component.text("Glacial Spike");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
