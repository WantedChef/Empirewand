package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class BlazeLaunch implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double power = spells.getDouble("blaze-launch.values.power", 1.8);
        int trailDuration = spells.getInt("blaze-launch.values.trail-duration-ticks", 40); // 2 seconds

        // Launch player
        Vector direction = player.getEyeLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(power));

        // Start fire trail effect
        startFireTrail(player, trailDuration, context);

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }

    private void startFireTrail(Player player, int duration, SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isValid() || player.isDead()) {
                    this.cancel();
                    return;
                }

                Location playerLoc = player.getLocation();

                // Fire particles trail
                context.fx().spawnParticles(playerLoc, Particle.FLAME, 10, 0.2, 0.2, 0.2, 0.05);

                // Ambient sound
                if (ticks % 20 == 0) { // Every second
                    context.fx().playSound(playerLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.0f);
                }

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    @Override
    public String getName() {
        return "blaze-launch";
    }

    @Override
    public String key() {
        return "blaze-launch";
    }

    @Override
    public Component displayName() {
        return Component.text("Blaze Launch");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}