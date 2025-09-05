package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Location;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class Lightwall implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double width = spells.getDouble("lightwall.values.width", 6.0);
        double height = spells.getDouble("lightwall.values.height", 3.0);
        int duration = spells.getInt("lightwall.values.duration-ticks", 100);
        double knockbackStrength = spells.getDouble("lightwall.values.knockback-strength", 0.5);
        int blindnessDuration = spells.getInt("lightwall.values.blindness-duration-ticks", 30);
        boolean hitPlayers = spells.getBoolean("lightwall.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("lightwall.flags.hit-mobs", true);

        Location center = player.getLocation().clone().add(player.getLocation().getDirection().multiply(3));

        // Create wall of invisible armor stands
        List<ArmorStand> wallStands = new ArrayList<>();
        Vector direction = player.getLocation().getDirection().normalize();
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                Vector offset = right.clone().multiply(w - width/2).add(new Vector(0, h, 0));
                Location standLocation = center.clone().add(offset);

                ArmorStand stand = player.getWorld().spawn(standLocation, ArmorStand.class);
                stand.setInvisible(true);
                stand.setMarker(true);
                stand.setGravity(false);
                stand.setInvulnerable(true);
                wallStands.add(stand);
            }
        }

        // Start wall task
        new WallTask(wallStands, center, width, height, knockbackStrength, blindnessDuration, hitPlayers, hitMobs).runTaskTimer(context.plugin(), 0L, 1L);

        // Auto-cleanup after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ArmorStand stand : wallStands) {
                    if (stand.isValid()) {
                        stand.remove();
                    }
                }
            }
        }.runTaskLater(context.plugin(), duration);

        // Initial visuals
        spawnWallParticles(center, width, height);
        context.fx().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
    }

    private static class WallTask extends BukkitRunnable {
        private final List<ArmorStand> wallStands;
        private final Location center;
        private final double width;
        private final double height;
        private final double knockbackStrength;
        private final int blindnessDuration;
        private final boolean hitPlayers;
        private final boolean hitMobs;

        public WallTask(List<ArmorStand> wallStands, Location center, double width, double height,
                       double knockbackStrength, int blindnessDuration, boolean hitPlayers, boolean hitMobs) {
            this.wallStands = wallStands;
            this.center = center;
            this.width = width;
            this.height = height;
            this.knockbackStrength = knockbackStrength;
            this.blindnessDuration = blindnessDuration;
            this.hitPlayers = hitPlayers;
            this.hitMobs = hitMobs;
        }

        @Override
        public void run() {
            // Check for entities near the wall
            for (ArmorStand stand : wallStands) {
                if (!stand.isValid()) continue;

                for (LivingEntity entity : stand.getWorld().getLivingEntities()) {
                    if (entity.getLocation().distance(stand.getLocation()) <= 1.5) {
                        boolean isPlayer = entity instanceof Player;
                        if ((isPlayer && hitPlayers) || (!isPlayer && hitMobs)) {
                            // Apply knockback away from wall
                            Vector knockback = entity.getLocation().toVector().subtract(stand.getLocation().toVector()).normalize();
                            knockback.multiply(knockbackStrength);
                            knockback.setY(0.2);
                            entity.setVelocity(entity.getVelocity().add(knockback));

                            // Apply blindness
                            entity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDuration, 0));
                        }
                    }
                }
            }

            // Periodic particles
            if (System.currentTimeMillis() % 1000 < 50) { // Roughly every second
                spawnWallParticles(center, width, height);
            }
        }
    }

    private static void spawnWallParticles(Location center, double width, double height) {
        Vector direction = center.getDirection();
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                Vector offset = right.clone().multiply(w - width/2).add(new Vector(0, h, 0));
                Location particleLoc = center.clone().add(offset);

                // White ash particles
                particleLoc.getWorld().spawnParticle(Particle.WHITE_ASH, particleLoc, 2, 0.1, 0.1, 0.1, 0);

                // Glow particles
                particleLoc.getWorld().spawnParticle(Particle.GLOW, particleLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public String getName() {
        return "lightwall";
    }

    @Override
    public String key() {
        return "lightwall";
    }

    @Override
    public Component displayName() {
        return Component.text("Lichtmuur");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}