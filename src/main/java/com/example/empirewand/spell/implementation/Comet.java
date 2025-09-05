package com.example.empirewand.spell.implementation;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.Particle;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class Comet implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(2.5f);
        fireball.setIsIncendiary(false);
        fireball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, getName());
        fireball.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (fireball.isDead()) {
                    this.cancel();
                    return;
                }
                context.fx().spawnParticles(fireball.getLocation(), Particle.SOUL_FIRE_FLAME, 10, 0.1, 0.1, 0.1, 0.05);
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    @Override
    public String getName() {
        return "comet";
    }
}
