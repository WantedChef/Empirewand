package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" }, justification = "Projectile/caster references validated via Bukkit API; additional guards retained for defensive clarity.")
public class CrimsonChains implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        if (player == null || !player.isValid()) {
            return; // Defensive: context should always supply caster
        }

        // Config values
        var spells = context.config().getSpellsConfig();
        double pullStrength = spells.getDouble("crimson-chains.values.pull-strength", 0.5);
        int slownessDuration = spells.getInt("crimson-chains.values.slowness-duration-ticks", 40);
        int slownessAmplifier = spells.getInt("crimson-chains.values.slowness-amplifier", 1);
        double projectileSpeed = spells.getDouble("crimson-chains.values.projectile-speed", 1.5);
        boolean hitPlayers = spells.getBoolean("crimson-chains.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("crimson-chains.flags.hit-mobs", true);

        // Launch snowball projectile
        Snowball projectile = player.launchProjectile(Snowball.class);
        if (projectile == null) {
            return; // Could not spawn
        }
        projectile.setVelocity(player.getLocation().getDirection().multiply(projectileSpeed));

        // Track the projectile
        new ProjectileTracker(projectile, player, pullStrength, slownessDuration, slownessAmplifier, hitPlayers,
                hitMobs, context).runTaskTimer(context.plugin(), 0L, 1L);

        // Visuals
        spawnChainTrail(projectile.getLocation(), context);
    }

    private static class ProjectileTracker extends BukkitRunnable {
        private final Snowball projectile;
        private final Player caster;
        private final double pullStrength;
        private final int slownessDuration;
        private final int slownessAmplifier;
        private final boolean hitPlayers;
        private final boolean hitMobs;
        private final SpellContext context;
        private Location lastLocation;

        public ProjectileTracker(Snowball projectile, Player caster, double pullStrength, int slownessDuration,
                int slownessAmplifier, boolean hitPlayers, boolean hitMobs, SpellContext context) {
            this.projectile = projectile;
            this.caster = caster;
            this.pullStrength = pullStrength;
            this.slownessDuration = slownessDuration;
            this.slownessAmplifier = slownessAmplifier;
            this.hitPlayers = hitPlayers;
            this.hitMobs = hitMobs;
            this.context = context;
            this.lastLocation = projectile.getLocation().clone();
        }

        @Override
        public void run() {
            if (projectile.isDead() || !projectile.isValid()) {
                this.cancel();
                return;
            }

            // Check for hits (projectile world guaranteed non-null)
            var world = projectile.getWorld();
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity.getLocation().distance(projectile.getLocation()) <= 1.5 && !entity.equals(caster)) {
                    if (entity instanceof Player && !hitPlayers)
                        continue;
                    if (!(entity instanceof Player) && !hitMobs)
                        continue;

                    // Apply effects
                    applyChainEffects(entity, caster, pullStrength, slownessDuration, slownessAmplifier);

                    // Remove projectile
                    projectile.remove();
                    this.cancel();
                    return;
                }
            }

            // Spawn trail particles
            spawnChainTrail(projectile.getLocation(), lastLocation);
            lastLocation = projectile.getLocation().clone();
        }

        private void applyChainEffects(LivingEntity target, Player caster, double pullStrength, int slownessDuration,
                int slownessAmplifier) {
            // Apply slowness
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, slownessAmplifier));

            // Apply pull (unless it's a boss - simplified check)
            if (!isBoss(target)) {
                Vector pullVector = caster.getLocation().toVector().subtract(target.getLocation().toVector())
                        .normalize();
                pullVector.multiply(pullStrength);
                target.setVelocity(target.getVelocity().add(pullVector));
            }

            // SFX
            context.fx().playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.8f);
        }

        private boolean isBoss(LivingEntity entity) {
            // Simplified boss check - in practice you'd check for specific boss entities
            var attr = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
            double maxHealth = attr != null ? attr.getValue() : 0.0;
            return maxHealth > 100;
        }

        private void spawnChainTrail(Location current, Location last) {
            if (current == null || last == null || current.getWorld() == null) {
                return;
            }
            Vector direction = current.toVector().subtract(last.toVector());
            double distance = direction.length();
            if (distance == 0) {
                return;
            }
            direction.normalize();

            for (double d = 0; d <= distance; d += 0.2) {
                Location particleLoc = last.clone().add(direction.clone().multiply(d));
                particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 0), 1.0f));
            }
        }
    }

    private void spawnChainTrail(Location location, SpellContext context) {
        // Initial trail
        // Location/world from projectile spawn are guaranteed non-null
        location.getWorld().spawnParticle(Particle.DUST, location, 3,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 0), 1.0f));
    }

    @Override
    public String getName() {
        return "crimson-chains";
    }

    @Override
    public String key() {
        return "crimson-chains";
    }

    @Override
    public Component displayName() {
        return Component.text("Crimson Chains");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}