package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class Spark implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double damage = spells.getDouble("spark.values.damage", 6.0); // 3 hearts
        double knockbackStrength = spells.getDouble("spark.values.knockback-strength", 0.45);
        double speed = spells.getDouble("spark.values.speed", 0.96);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Launch small fireball projectile
        Location spawnLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5));
        SmallFireball proj = player.getWorld().spawn(spawnLoc, SmallFireball.class);
        proj.setVelocity(player.getEyeLocation().getDirection().multiply(speed));
        proj.setShooter(player);

        // Tag projectile
        proj.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL,
                Keys.STRING_TYPE.getType(), "spark");

        // Visual trail settings
        int trailSteps = spells.getInt("spark.values.trail-steps", 8);
        int particleCount = spells.getInt("spark.values.particle-count", 6);
        int burstInterval = spells.getInt("spark.values.burst-interval-ticks", 10); // small crit burst cadence

        new ParticleTrail(context, proj, particleCount, trailSteps, burstInterval).runTaskTimer(context.plugin(), 0L,
                1L);

        // Register listener for hit detection
        context.plugin().getServer().getPluginManager().registerEvents(
                new SparkListener(context, damage, knockbackStrength, friendlyFire), context.plugin());

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.2f);
    }

    private static class ParticleTrail extends BukkitRunnable {
        private final SpellContext context;
        private final SmallFireball projectile;
        private final int particleCount;
        private final int maxSteps;
        private final int burstInterval;
        private final java.util.Deque<Location> history = new java.util.ArrayDeque<>();
        private int ticks = 0;

        ParticleTrail(SpellContext context, SmallFireball projectile, int particleCount, int maxSteps,
                int burstInterval) {
            this.context = context;
            this.projectile = projectile;
            this.particleCount = particleCount;
            this.maxSteps = Math.max(1, maxSteps);
            this.burstInterval = Math.max(0, burstInterval);
        }

        @Override
        public void run() {
            if (!projectile.isValid() || projectile.isDead() || projectile.isOnGround()) {
                cancel();
                return;
            }
            if (ticks++ > 20 * 6) { // safety (6s)
                cancel();
                return;
            }

            Location current = projectile.getLocation().clone();
            history.addFirst(current);
            while (history.size() > maxSteps)
                history.removeLast();

            int idx = 0;
            for (Location loc : history) {
                double scale = 1.0 - (idx / (double) maxSteps);
                int count = (int) Math.max(1, Math.round(particleCount * scale));
                context.fx().spawnParticles(loc, Particle.ELECTRIC_SPARK, count, 0.06, 0.06, 0.06, 0.015);
                if (idx == 0) {
                    context.fx().spawnParticles(loc, Particle.CRIT, 1, 0, 0, 0, 0);
                }
                idx++;
            }

            if (burstInterval > 0 && ticks % burstInterval == 0) {
                Location loc = projectile.getLocation();
                context.fx().spawnParticles(loc, Particle.CRIT, 4, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    private static class SparkListener implements Listener {
        private final SpellContext context;
        private final double damage;
        private final double knockbackStrength;
        private final boolean friendlyFire;

        public SparkListener(SpellContext context, double damage, double knockbackStrength, boolean friendlyFire) {
            this.context = context;
            this.damage = damage;
            this.knockbackStrength = knockbackStrength;
            this.friendlyFire = friendlyFire;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof SmallFireball))
                return;

            var pdc = projectile.getPersistentDataContainer();
            String spellType = pdc.get(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType());
            if (!"spark".equals(spellType))
                return;

            // Check if hit entity
            if (event.getHitEntity() instanceof LivingEntity living) {
                if (living.equals(projectile.getShooter()) && !friendlyFire)
                    return;
                if (living.isDead() || !living.isValid())
                    return;

                // Apply damage
                living.damage(damage, context.caster());

                // Apply knockback
                Vector direction = living.getLocation().toVector()
                        .subtract(projectile.getLocation().toVector()).normalize();
                living.setVelocity(direction.multiply(knockbackStrength));

                // Effects
                context.fx().spawnParticles(living.getLocation(), Particle.ELECTRIC_SPARK, 15, 0.3, 0.3, 0.3, 0.1);
                context.fx().playSound(living.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.7f, 0.8f);
            }

            // Remove projectile
            projectile.remove();
        }
    }

    @Override
    public String getName() {
        return "spark";
    }

    @Override
    public String key() {
        return "spark";
    }

    @Override
    public Component displayName() {
        return Component.text("Spark");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}