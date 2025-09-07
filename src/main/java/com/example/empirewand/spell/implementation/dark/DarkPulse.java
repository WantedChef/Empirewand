package com.example.empirewand.spell.implementation.dark;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.example.empirewand.core.storage.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class DarkPulse implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("dark-pulse.values.range", 24.0);
        int witherDuration = spells.getInt("dark-pulse.values.wither-duration-ticks", 120);
        int witherAmplifier = spells.getInt("dark-pulse.values.wither-amplifier", 1);
        int blindDuration = spells.getInt("dark-pulse.values.blind-duration-ticks", 60);
        double speed = spells.getDouble("dark-pulse.values.speed", 1.8);
        double radius = spells.getDouble("dark-pulse.values.explosion-radius", 4.0);
        double damage = spells.getDouble("dark-pulse.values.damage", 6.0);
        double knockback = spells.getDouble("dark-pulse.values.knockback", 0.6);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Get target
        var targetEntity = player.getTargetEntity((int) range);
        if (!(targetEntity instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return;
        }

        // Launch projectile
        Vector direction = target.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
        Location spawnLoc = player.getEyeLocation().add(direction.clone().multiply(0.5));

        WitherSkull skull = player.getWorld().spawn(spawnLoc, WitherSkull.class);
        skull.setCharged(true);
        skull.setVelocity(direction.multiply(speed));
        skull.setShooter(player);

        // Tag projectile
        skull.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL,
                Keys.STRING_TYPE.getType(), "dark-pulse");

        // Visual flight config
        int ringParticleCount = spells.getInt("dark-pulse.values.ring-particle-count", 14);
        double ringRadiusStep = spells.getDouble("dark-pulse.values.ring-radius-step", 0.25);
        int ringEveryTicks = spells.getInt("dark-pulse.values.ring-interval-ticks", 2);

        // Attach ring pulse runnable (purely cosmetic)
        new RingPulse(context, skull, ringParticleCount, ringRadiusStep, ringEveryTicks).runTaskTimer(context.plugin(),
                0L, 1L);

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);

        // Register listener
        context.plugin().getServer().getPluginManager().registerEvents(
                new PulseListener(context, witherDuration, witherAmplifier, blindDuration, radius, damage, knockback,
                        friendlyFire),
                context.plugin());
    }

    /**
     * Emits concentric shrinking dark rings behind the wither skull to give a
     * pulsing void ripple effect.
     */
    private static class RingPulse extends BukkitRunnable {
        private final SpellContext context;
        private final WitherSkull projectile;
        private final int ringParticleCount;
        private final double radiusStep;
        private final int interval;
        private int ticks = 0;
        private double accumRadius = 0;

        RingPulse(SpellContext context, WitherSkull projectile, int ringParticleCount, double radiusStep,
                int interval) {
            this.context = context;
            this.projectile = projectile;
            this.ringParticleCount = Math.max(4, ringParticleCount);
            this.radiusStep = Math.max(0.05, radiusStep);
            this.interval = Math.max(1, interval);
        }

        @Override
        public void run() {
            if (!projectile.isValid() || projectile.isDead()) {
                cancel();
                return;
            }
            if (ticks++ > 20 * 6) { // 6s safety
                cancel();
                return;
            }

            if (ticks % interval != 0)
                return;

            // Increase radius accumulation then draw ring
            accumRadius += radiusStep;
            double radius = accumRadius;
            Location center = projectile.getLocation();

            for (int i = 0; i < ringParticleCount; i++) {
                double angle = (Math.PI * 2 * i) / ringParticleCount;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                Location p = new Location(center.getWorld(), x, center.getY() + 0.15, z);
                context.fx().spawnParticles(p, Particle.SMOKE, 1, 0, 0, 0, 0);
                if (i % 3 == 0) {
                    context.fx().spawnParticles(p, Particle.SOUL_FIRE_FLAME, 1, 0, 0, 0, 0.0);
                }
            }

            // Gentle inner void spark
            context.fx().spawnParticles(center, Particle.REVERSE_PORTAL, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private static class PulseListener implements Listener {
        private final SpellContext context;
        private final int witherDuration;
        private final int witherAmplifier;
        private final int blindDuration;
        private final double radius;
        private final double damage;
        private final double knockback;
        private final boolean friendlyFire;

        public PulseListener(SpellContext context, int witherDuration, int witherAmplifier, int blindDuration,
                double radius, double damage, double knockback, boolean friendlyFire) {
            this.context = context;
            this.witherDuration = witherDuration;
            this.witherAmplifier = witherAmplifier;
            this.blindDuration = blindDuration;
            this.radius = radius;
            this.damage = damage;
            this.knockback = knockback;
            this.friendlyFire = friendlyFire;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof WitherSkull))
                return;

            var pdc = projectile.getPersistentDataContainer();
            String spellType = pdc.get(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType());
            if (!"dark-pulse".equals(spellType))
                return;

            Location hitLoc = projectile.getLocation();

            // Impact FX (batched)
            context.fx().impact(hitLoc, Particle.SOUL_FIRE_FLAME, 50, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.7f);
            context.fx().spawnParticles(hitLoc, Particle.SMOKE, 30, 0.6, 0.6, 0.6, 0.02);

            // Apply effects to nearby entities
            for (var entity : hitLoc.getWorld().getNearbyEntities(hitLoc, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living))
                    continue;
                if (living.equals(projectile.getShooter()) && !friendlyFire)
                    continue;
                if (living.isDead() || !living.isValid())
                    continue;

                // Damage and debuffs
                if (damage > 0) {
                    living.damage(damage, (projectile.getShooter() instanceof Player p) ? p : null);
                }
                living.addPotionEffect(
                        new PotionEffect(PotionEffectType.WITHER, witherDuration, witherAmplifier, false, true));
                if (blindDuration > 0) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindDuration, 0, false, true));
                }

                // Knockback away from center
                if (knockback > 0) {
                    Vector kb = living.getLocation().toVector().subtract(hitLoc.toVector()).normalize()
                            .multiply(knockback).setY(0.2);
                    living.setVelocity(kb);
                }
            }

            projectile.remove();

            // Clean up listener to avoid leaks
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public String getName() {
        return "dark-pulse";
    }

    @Override
    public String key() {
        return "dark-pulse";
    }

    @Override
    public Component displayName() {
        return Component.text("Dark Pulse");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
