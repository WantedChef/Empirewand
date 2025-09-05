package com.example.empirewand.spell.implementation;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class Explosive implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        var spells = context.config().getSpellsConfig();
        float yield = (float) spells.getDouble("explosive.values.radius", 4.0);
        boolean setsFire = spells.getBoolean("explosive.flags.sets-fire", false);

        WitherSkull skull = player.launchProjectile(WitherSkull.class);
        skull.setYield(Math.max(0.0f, yield));
        skull.setIsIncendiary(setsFire);
        skull.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, getName());
        skull.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());

        // Subtle smoke trail while travelling
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!skull.isValid() || skull.isDead()) {
                    this.cancel();
                    return;
                }
                context.fx().spawnParticles(skull.getLocation(), Particle.SMOKE, 6, 0.12, 0.12, 0.12, 0.02);
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    @Override
    public String getName() {
        return "explosive";
    }
}
