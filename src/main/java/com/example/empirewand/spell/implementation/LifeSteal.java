package com.example.empirewand.spell.implementation;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;

public class LifeSteal implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, getName());
        snowball.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());
        // Give a crisp initial speed for better feel
        Vector dir = player.getEyeLocation().getDirection().normalize().multiply(1.2);
        snowball.setVelocity(dir);

        // Light cast SFX
        context.fx().playSound(player, Sound.ENTITY_WITCH_THROW, 0.8f, 1.2f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!snowball.isValid() || snowball.isDead() || snowball.isOnGround()) {
                    this.cancel();
                    return;
                }
                context.fx().spawnParticles(
                        snowball.getLocation(),
                        Particle.DUST,
                        8,
                        0.08, 0.08, 0.08,
                        0,
                        new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.0f)
                );
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    @Override
    public String getName() {
        return "lifesteal";
    }
}
