package com.example.empirewand.spell.implementation.heal;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

import java.util.List;

public class RadiantBeacon implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        if (player == null || !player.isValid()) {
            return;
        }

        // Config values
        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("radiant-beacon.values.radius", 6.0);
        double healAmount = spells.getDouble("radiant-beacon.values.heal-amount", 1.0);
        double damageAmount = spells.getDouble("radiant-beacon.values.damage-amount", 1.0);
        int durationPulses = spells.getInt("radiant-beacon.values.duration-pulses", 8);
        int pulseInterval = spells.getInt("radiant-beacon.values.pulse-interval-ticks", 20);
        int maxTargets = spells.getInt("radiant-beacon.values.max-targets", 8);
        boolean hitPlayers = spells.getBoolean("radiant-beacon.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("radiant-beacon.flags.hit-mobs", true);

        var plLoc = player.getLocation();
        Location beaconLocation = plLoc.clone();

        // Spawn invisible armor stand as anchor (player world guaranteed non-null)
        var world = player.getWorld();
        ArmorStand beacon = world.spawn(beaconLocation, ArmorStand.class);
        beacon.setInvisible(true);
        beacon.setMarker(true);
        beacon.setGravity(false);
        beacon.setInvulnerable(true);

        // Start beacon task
        new BeaconTask(beacon, beaconLocation, radius, healAmount, damageAmount, durationPulses, maxTargets, hitPlayers,
                hitMobs, context).runTaskTimer(context.plugin(), 0L, pulseInterval);
    }

    private static class BeaconTask extends BukkitRunnable {
        private final ArmorStand beacon;
        private final Location beaconLocation;
        private final double radius;
        private final double healAmount;
        private final double damageAmount;
        private final int durationPulses;
        private final int maxTargets;
        private final boolean hitPlayers;
        private final boolean hitMobs;
        private final SpellContext context;
        private int pulsesCompleted = 0;

        public BeaconTask(ArmorStand beacon, Location beaconLocation, double radius, double healAmount,
                double damageAmount,
                int durationPulses, int maxTargets, boolean hitPlayers, boolean hitMobs, SpellContext context) {
            this.beacon = beacon;
            this.beaconLocation = beaconLocation;
            this.radius = radius;
            this.healAmount = healAmount;
            this.damageAmount = damageAmount;
            this.durationPulses = durationPulses;
            this.maxTargets = maxTargets;
            this.hitPlayers = hitPlayers;
            this.hitMobs = hitMobs;
            this.context = context;
        }

        @Override
        public void run() {
            if (!beacon.isValid() || pulsesCompleted >= durationPulses) {
                beacon.remove();
                this.cancel();
                return;
            }

            // Find nearby entities
            org.bukkit.World world = beacon.getWorld();
            List<LivingEntity> nearbyEntities = world.getLivingEntities().stream()
                    .filter(entity -> entity.getLocation().distance(beaconLocation) <= radius)
                    .filter(entity -> entity != beacon)
                    .toList();

            int targetsProcessed = 0;

            for (LivingEntity entity : nearbyEntities) {
                if (targetsProcessed >= maxTargets)
                    break;

                boolean isPlayer = entity instanceof Player;
                // Self counts as ally; simple UUID equality
                var caster = context.caster();
                boolean isAlly = isPlayer && caster != null
                        && caster.getUniqueId().equals(((Player) entity).getUniqueId());

                if (isAlly) {
                    // Heal and cleanse allies
                    var attr = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                    double maxHealth = attr != null ? attr.getValue() : entity.getHealth();
                    double newHealth = Math.min(maxHealth, entity.getHealth() + healAmount);
                    entity.setHealth(newHealth);

                    // Remove one negative effect
                    for (PotionEffect effect : entity.getActivePotionEffects()) {
                        var type = effect.getType();
                        if (isNegativeEffect(type)) {
                            entity.removePotionEffect(effect.getType());
                            break;
                        }
                    }
                } else {
                    // Damage enemies
                    if ((isPlayer && hitPlayers) || (!isPlayer && hitMobs)) {
                        entity.damage(damageAmount, context.caster());
                    }
                }

                targetsProcessed++;
            }

            // Visuals and SFX
            spawnBeaconParticles(beaconLocation, radius);
            if (pulsesCompleted % 2 == 0) { // Every other pulse
                context.fx().playSound(beaconLocation, Sound.BLOCK_BELL_USE, 0.5f, 1.5f);
            }

            pulsesCompleted++;
        }

        private boolean isNegativeEffect(PotionEffectType type) {
            return type == PotionEffectType.POISON ||
                    type == PotionEffectType.WITHER ||
                    type == PotionEffectType.SLOWNESS ||
                    type == PotionEffectType.WEAKNESS ||
                    type == PotionEffectType.BLINDNESS ||
                    type == PotionEffectType.NAUSEA;
        }

        private void spawnBeaconParticles(Location location, double radius) {
            if (location.getWorld() == null) {
                return; // Invalid world
            }
            // Beam particles (location/world guaranteed)
            for (int i = 0; i < 10; i++) {
                double y = i * 0.5;
                location.getWorld().spawnParticle(Particle.END_ROD, location.clone().add(0, y, 0), 1, 0, 0, 0, 0);
            }

            // Glow particles around
            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                double x = radius * 0.8 * Math.cos(angle);
                double z = radius * 0.8 * Math.sin(angle);
                location.getWorld().spawnParticle(Particle.GLOW, location.clone().add(x, 1, z), 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public String getName() {
        return "radiant-beacon";
    }

    @Override
    public String key() {
        return "radiant-beacon";
    }

    @Override
    public Component displayName() {
        return Component.text("Stralingsbaken");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
