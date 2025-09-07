package com.example.empirewand.spell.implementation.earth;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;
import com.example.empirewand.visual.SpiralEmitter;

public class Sandstorm implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("sandstorm.values.radius", 6.0);
        int blindDur = spells.getInt("sandstorm.values.blind-duration-ticks", 80);
        int slowDur = spells.getInt("sandstorm.values.slow-duration-ticks", 100);
        int slowAmp = spells.getInt("sandstorm.values.slow-amplifier", 1);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        BlockData sand = Material.SAND.createBlockData();

        for (var e : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(e instanceof LivingEntity living))
                continue;
            if (living.equals(player) && !friendlyFire)
                continue;
            if (living.isDead() || !living.isValid())
                continue;

            living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindDur, 0, false, true));
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDur, slowAmp, false, true));
            context.fx().spawnParticles(living.getLocation(), Particle.BLOCK, 20, 0.5, 0.8, 0.5, 0.01, sand);
        }

        context.fx().playSound(player, Sound.BLOCK_SAND_BREAK, 0.8f, 0.8f);

        // Visual sand spiral / haze (purely cosmetic)
        final double spiralHeight = spells.getDouble("sandstorm.spiral-height", 6.0);
        final int spiralDensity = spells.getInt("sandstorm.spiral-density", 14);
        final int gritInterval = spells.getInt("sandstorm.grit-pulse-interval-ticks", 8);
        final double hazeMult = spells.getDouble("sandstorm.haze-particle-multiplier", 1.0);
        final Location center = player.getLocation();
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 60 || center.getWorld() == null) {
                    cancel();
                    return;
                }
                // Vertical grit spiral
                SpiralEmitter.emit(center.clone(), spiralHeight, 1, spiralDensity, radius * 0.6, Particle.FALLING_DUST);
                // Haze shell
                int hazeCount = (int) (20 * hazeMult);
                for (int i = 0; i < hazeCount; i++) {
                    double ang = Math.random() * 2 * Math.PI;
                    double r = Math.random() * radius;
                    double x = Math.cos(ang) * r;
                    double z = Math.sin(ang) * r;
                    center.getWorld().spawnParticle(Particle.FALLING_DUST, center.getX() + x,
                            center.getY() + Math.random() * 1.5, center.getZ() + z, 1, 0.02, 0.05, 0.02, 0.001, sand);
                }
                if (gritInterval > 0 && ticks % gritInterval == 0) {
                    for (int i = 0; i < 8; i++) {
                        double ang = (2 * Math.PI * i) / 8.0;
                        center.getWorld().spawnParticle(Particle.CRIT, center.getX() + Math.cos(ang) * 1.2,
                                center.getY() + 0.2, center.getZ() + Math.sin(ang) * 1.2, 6, 0.1, 0.05, 0.1, 0.0);
                    }
                }
                ticks += 4; // runs every 4 ticks
            }
        }.runTaskTimer(context.plugin(), 0L, 4L);
    }

    @Override
    public String getName() {
        return "sandstorm";
    }

    @Override
    public String key() {
        return "sandstorm";
    }

    @Override
    public Component displayName() {
        return Component.text("Sandstorm");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
