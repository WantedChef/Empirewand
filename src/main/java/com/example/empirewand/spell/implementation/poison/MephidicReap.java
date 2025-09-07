package com.example.empirewand.spell.implementation.poison;

import org.bukkit.Particle;
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

import java.util.HashSet;
import java.util.Set;

public class MephidicReap implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("mephidic-reap.values.range", 8.0);
        double damage = spells.getDouble("mephidic-reap.values.damage", 2.0);
        int slownessDuration = spells.getInt("mephidic-reap.values.slowness-duration-ticks", 20);
        int maxPierce = spells.getInt("mephidic-reap.values.max-pierce", 3);
        int travelTicks = spells.getInt("mephidic-reap.values.travel-ticks", 14);
        boolean hitPlayers = spells.getBoolean("mephidic-reap.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("mephidic-reap.flags.hit-mobs", true);

        Vector direction = player.getLocation().getDirection().normalize();
        Location start = player.getEyeLocation();
        Location end = start.clone().add(direction.clone().multiply(range));

        // Spawn scythe armor stand
        ArmorStand scythe = player.getWorld().spawn(start, ArmorStand.class);
        scythe.setInvisible(true);
        scythe.setMarker(true);
        scythe.setGravity(false);
        scythe.setInvulnerable(true);

        // Start boomerang task
        new BoomerangTask(scythe, start, end, player, damage, slownessDuration, maxPierce, travelTicks, hitPlayers, hitMobs).runTaskTimer(context.plugin(), 0L, 1L);
    }

    private static class BoomerangTask extends BukkitRunnable {
        private final ArmorStand scythe;
        private final Location start;
        private final Location end;
        private final Player caster;
        private final double damage;
        private final int slownessDuration;
        private final int maxPierce;
        private final int travelTicks;
        private final boolean hitPlayers;
        private final boolean hitMobs;
        private int tick = 0;
        private boolean returning = false;
        private Set<LivingEntity> hitEntities = new HashSet<>();

        public BoomerangTask(ArmorStand scythe, Location start, Location end, Player caster, double damage,
                           int slownessDuration, int maxPierce, int travelTicks, boolean hitPlayers, boolean hitMobs) {
            this.scythe = scythe;
            this.start = start;
            this.end = end;
            this.caster = caster;
            this.damage = damage;
            this.slownessDuration = slownessDuration;
            this.maxPierce = maxPierce;
            this.travelTicks = travelTicks;
            this.hitPlayers = hitPlayers;
            this.hitMobs = hitMobs;
        }

        @Override
        public void run() {
            if (!scythe.isValid()) {
                this.cancel();
                return;
            }

            tick++;

            // Calculate position
            double progress = (double) tick / travelTicks;
            if (returning) {
                progress = 1.0 - progress;
            }

            Location targetLocation = interpolateLocation(start, end, progress);
            scythe.teleport(targetLocation);

            // Check for hits
            if (hitEntities.size() < maxPierce) {
                for (LivingEntity entity : scythe.getWorld().getLivingEntities()) {
                    if (entity.getLocation().distance(scythe.getLocation()) <= 1.5 && !entity.equals(caster) && !hitEntities.contains(entity)) {
                        if (entity instanceof Player && !hitPlayers) continue;
                        if (!(entity instanceof Player) && !hitMobs) continue;

                        // Apply damage and effects
                        entity.damage(damage, caster);
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, 0));
                        hitEntities.add(entity);

                        // Visual hit effect
                        spawnHitParticles(entity.getLocation());
                    }
                }
            }

            // Spawn trail particles
            spawnTrailParticles(scythe.getLocation());

            // Check if we need to return or finish
            if (tick >= travelTicks) {
                if (returning) {
                    // Finished
                    scythe.remove();
                    this.cancel();
                } else {
                    // Start returning
                    returning = true;
                    tick = 0;
                }
            }
        }

        private Location interpolateLocation(Location start, Location end, double progress) {
            Vector difference = end.toVector().subtract(start.toVector());
            Vector offset = difference.multiply(progress);
            return start.clone().add(offset);
        }

        private void spawnTrailParticles(Location location) {
            location.getWorld().spawnParticle(Particle.SMOKE, location, 2, 0, 0, 0, 0);
            location.getWorld().spawnParticle(Particle.CRIT, location, 1, 0, 0, 0, 0);
        }

        private void spawnHitParticles(Location location) {
            location.getWorld().spawnParticle(Particle.CRIT, location, 5, 0.2, 0.2, 0.2, 0.1);
        }
    }

    @Override
    public String getName() {
        return "mephidic-reap";
    }

    @Override
    public String key() {
        return "mephidic-reap";
    }

    @Override
    public Component displayName() {
        return Component.text("Mephidic Reap");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
