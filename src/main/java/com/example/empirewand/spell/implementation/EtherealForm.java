package com.example.empirewand.spell.implementation;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class EtherealForm implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        player.setCollidable(false);
        int duration = Math.max(1,
                context.config().getSpellsConfig().getInt("ethereal-form.values.duration-ticks", 100)); // default 5s
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0));
        // Fall damage is cancelled in EntityListener.onFallDamage

        // Ambient ethereal FX
        context.fx().spawnParticles(player.getLocation().add(0, 1.0, 0), Particle.END_ROD, 16, 0.4, 0.4, 0.4, 0.01);
        context.fx().playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.3f);

        // Tag player as ethereal via PDC for listeners
        player.getPersistentDataContainer().set(Keys.ETHEREAL_ACTIVE, Keys.BYTE_TYPE.getType(), (byte) 1);
        long nowTicks = player.getWorld().getFullTime();
        player.getPersistentDataContainer().set(Keys.ETHEREAL_EXPIRES_TICK, Keys.LONG_TYPE.getType(),
                nowTicks + duration);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.setCollidable(true);
                player.getPersistentDataContainer().remove(Keys.ETHEREAL_ACTIVE);
            }
        }.runTaskLater(context.plugin(), duration); // 5 seconds
    }

    @Override
    public String getName() {
        return "ethereal-form";
    }

    @Override
    public String key() {
        return "ethereal-form";
    }

    @Override
    public Component displayName() {
        return Component.text("Ethereal Form");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
