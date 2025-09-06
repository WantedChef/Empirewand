package com.example.empirewand.spell.implementation;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class LifeSteal implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType(), getName());
        snowball.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, Keys.STRING_TYPE.getType(),
                player.getUniqueId().toString());
        // Give a crisp initial speed for better feel
        Vector dir = player.getEyeLocation().getDirection().normalize().multiply(1.2);
        snowball.setVelocity(dir);

        // Light cast SFX
        context.fx().playSound(player, Sound.ENTITY_WITCH_THROW, 0.8f, 1.2f);

        context.fx().followParticles(
                context.plugin(),
                snowball,
                Particle.DUST,
                8,
                0.08, 0.08, 0.08,
                0,
                new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.0f),
                1L);
    }

    @Override
    public String getName() {
        return "lifesteal";
    }

    @Override
    public String key() {
        return "lifesteal";
    }

    @Override
    public Component displayName() {
        return Component.text("Life Steal");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
