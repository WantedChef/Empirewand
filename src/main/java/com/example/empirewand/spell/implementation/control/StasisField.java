package com.example.empirewand.spell.implementation.control;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class StasisField implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("stasis-field.values.radius", 6.0);
        int duration = spells.getInt("stasis-field.values.duration-ticks", 80);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living))
                continue;
            if (living.equals(player) && !friendlyFire)
                continue;
            if (living.isDead() || !living.isValid())
                continue;

            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 250, false, true));
            living.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 5, false, true));
            context.fx().spawnParticles(living.getLocation(), Particle.ENCHANT, 20, 0.4, 0.7, 0.4, 0.0);
        }

        context.fx().playSound(player, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.8f);

        // Visual floating temporal bubbles & sweeping arc
        int bubbleCount = spells.getInt("stasis-field.bubble-count", 5);
        double bubbleRadius = spells.getDouble("stasis-field.bubble-radius", 1.2);
        int sweepInterval = spells.getInt("stasis-field.sweep-interval-ticks", 20);
        Location center = player.getLocation();

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > duration || center.getWorld() == null) {
                    cancel();
                    return;
                }
                // Bubbles
                for (int i = 0; i < bubbleCount; i++) {
                    double ang = (2 * Math.PI * i) / bubbleCount + (t * 0.05);
                    double yOff = 0.5 + Math.sin(t * 0.1 + i) * 0.4;
                    double x = Math.cos(ang) * bubbleRadius;
                    double z = Math.sin(ang) * bubbleRadius;
                    center.getWorld().spawnParticle(Particle.ENCHANT, center.getX() + x, center.getY() + yOff,
                            center.getZ() + z, 1, 0.0, 0.0, 0.0, 0.0);
                }
                // Sweep effect every interval
                if (t % sweepInterval == 0) {
                    for (int i = 0; i < 24; i++) {
                        double a = (2 * Math.PI * i) / 24;
                        double x = Math.cos(a) * (bubbleRadius + 0.4);
                        double z = Math.sin(a) * (bubbleRadius + 0.4);
                        center.getWorld().spawnParticle(Particle.PORTAL, center.getX() + x, center.getY() + 0.05,
                                center.getZ() + z, 2, 0.02, 0.02, 0.02, 0.01);
                    }
                }
                t += 2; // runs every 2 ticks below
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    @Override
    public String getName() {
        return "stasis-field";
    }

    @Override
    public String key() {
        return "stasis-field";
    }

    @Override
    public Component displayName() {
        return Component.text("Stasis Field");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
