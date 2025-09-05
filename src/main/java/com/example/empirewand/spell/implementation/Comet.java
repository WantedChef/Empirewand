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
        var spells = context.config().getSpellsConfig();
        float yield = (float) spells.getDouble("comet.values.yield", 2.5);

        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(Math.max(0.0f, yield));
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
