package com.example.empirewand.spell.implementation;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class GodCloud implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        int durationTicks = spells.getInt("god-cloud.values.duration-ticks", 600); // 30 seconds
        int particleInterval = spells.getInt("god-cloud.values.particle-interval", 2);
        int particleCount = spells.getInt("god-cloud.values.particle-count", 6);

        // Start cloud effect task
        startCloudEffect(caster, durationTicks, particleInterval, particleCount, context);
    }

    private void startCloudEffect(Player player, int duration, int interval, int count, SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isValid() || player.isDead()) {
                    this.cancel();
                    return;
                }

                // Only spawn particles if player is flying or gliding
                if (player.isFlying() || player.isGliding()) {
                    // Spawn cloud particles under player
                    context.fx().spawnParticles(
                        player.getLocation().add(0, -0.9, 0),
                        org.bukkit.Particle.CLOUD,
                        count,
                        0.3, 0.05, 0.3,
                        0
                    );

                    // Optional: play gentle wind sound occasionally
                    if (ticks % 40 == 0) { // Every 2 seconds
                        context.fx().playSound(player.getLocation(), org.bukkit.Sound.AMBIENT_CAVE, 0.3f, 1.0f);
                    }
                }

                ticks += interval;
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }

    @Override
    public String getName() {
        return "god-cloud";
    }

    @Override
    public String key() {
        return "god-cloud";
    }

    @Override
    public Component displayName() {
        return Component.text("God Cloud");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}