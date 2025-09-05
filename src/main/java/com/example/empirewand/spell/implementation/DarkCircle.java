package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class DarkCircle implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Location center = player.getLocation();

        // Config values with defaults
        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("dark-circle.values.radius", 10.0);
        double pullStrength = spells.getDouble("dark-circle.values.pull-strength", 0.6);
        double launchPower = spells.getDouble("dark-circle.values.launch-power", 2.2);
        int pullDuration = spells.getInt("dark-circle.values.pull-duration-ticks", 30);
        int launchDelay = spells.getInt("dark-circle.values.launch-delay-ticks", 10); // offset after pull ends
        double detonationDamage = spells.getDouble("dark-circle.values.detonation-damage", 4.0);
        double radialKnockback = spells.getDouble("dark-circle.values.radial-knockback", 0.7);
        int slowAmplifier = spells.getInt("dark-circle.values.slow-amplifier", 0);
        int witherDuration = spells.getInt("dark-circle.values.wither-duration-ticks", 60);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Get nearby entities
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.equals(player) && !friendlyFire) continue; // Self-immunity unless friendly-fire enabled
            if (living.isDead() || !living.isValid()) continue;

            // Start pull effect
            startPullEffect(living, center, pullStrength, pullDuration, launchPower, launchDelay,
                    detonationDamage, radialKnockback, slowAmplifier, witherDuration, friendlyFire, player, context);
        }

        // Visual circle effect
        createCircleEffect(center, radius, context);

        // Cast sound
        context.fx().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
    }

    private void startPullEffect(LivingEntity target, Location center, double pullStrength, int pullDuration,
                                double launchPower, int launchDelay, double detonationDamage, double radialKnockback,
                                int slowAmplifier, int witherDuration, boolean friendlyFire, Player caster,
                                SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final int launchTick = pullDuration + Math.max(0, launchDelay);

            @Override
            public void run() {
                if (ticks >= launchTick + 2 || target.isDead() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                if (ticks < pullDuration) {
                    // Pull towards center
                    Vector direction = center.toVector().subtract(target.getLocation().toVector()).normalize();
                    target.setVelocity(direction.multiply(pullStrength));

                    // Pull particles
                    context.fx().spawnParticles(target.getLocation(), Particle.SMOKE, 2, 0.1, 0.1, 0.1, 0.05);
                } else if (ticks == launchTick) {
                    // Launch up
                    target.setVelocity(new Vector(0, launchPower, 0));

                    // Launch effects
                    context.fx().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    context.fx().spawnParticles(target.getLocation(), Particle.EXPLOSION, 10, 0.5, 0.5, 0.5, 0.1);

                    // Detonation follow-up: small radial knock + optional damage/debuffs
                    if (!(target.equals(caster) && !friendlyFire)) {
                        if (detonationDamage > 0) {
                            target.damage(detonationDamage, caster);
                        }
                        if (radialKnockback > 0) {
                            Vector away = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(radialKnockback).setY(0.15);
                            target.setVelocity(target.getVelocity().add(away));
                        }
                        if (witherDuration > 0) {
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WITHER, witherDuration, 0, false, true));
                        }
                        if (slowAmplifier >= 0) {
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, slowAmplifier, false, true));
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    private void createCircleEffect(Location center, double radius, SpellContext context) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final int duration = 40; // 2 seconds

            @Override
            public void run() {
                if (ticks >= duration) {
                    this.cancel();
                    return;
                }

                // Create circle particles
                for (int i = 0; i < 360; i += 8) {
                    double angle = Math.toRadians(i);
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location particleLoc = new Location(center.getWorld(), x, center.getY(), z);

                    context.fx().spawnParticles(particleLoc, Particle.SOUL_FIRE_FLAME, 1, 0, 0, 0, 0);
                }

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    @Override
    public String getName() {
        return "dark-circle";
    }

    @Override
    public String key() {
        return "dark-circle";
    }

    @Override
    public Component displayName() {
        return Component.text("Dark Circle");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
