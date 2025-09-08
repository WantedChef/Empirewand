package com.example.empirewand.spell.implementation.heal;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RadiantBeacon extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Radiant Beacon";
            this.description = "Creates a beacon that heals allies and damages enemies.";
            this.manaCost = 20; // Example
            this.cooldown = java.time.Duration.ofSeconds(35);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new RadiantBeacon(this);
        }
    }

    private RadiantBeacon(Builder builder) {
        super(builder);
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
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double radius = spellConfig.getDouble("values.radius", 6.0);
        double healAmount = spellConfig.getDouble("values.heal-amount", 1.0);
        double damageAmount = spellConfig.getDouble("values.damage-amount", 1.0);
        int durationPulses = spellConfig.getInt("values.duration-pulses", 8);
        int pulseInterval = spellConfig.getInt("values.pulse-interval-ticks", 20);
        int maxTargets = spellConfig.getInt("values.max-targets", 8);
        boolean hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);

        ArmorStand beacon = player.getWorld().spawn(player.getLocation(), ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.setInvulnerable(true);
        });

        new BeaconTask(beacon, radius, healAmount, damageAmount, durationPulses, maxTargets, hitPlayers, hitMobs, context).runTaskTimer(context.plugin(), 0L, pulseInterval);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private class BeaconTask extends BukkitRunnable {
        private final ArmorStand beacon;
        private final double radius;
        private final double healAmount;
        private final double damageAmount;
        private final int durationPulses;
        private final int maxTargets;
        private final boolean hitPlayers;
        private final boolean hitMobs;
        private final SpellContext context;
        private int pulsesCompleted = 0;

        public BeaconTask(ArmorStand beacon, double radius, double healAmount, double damageAmount, int durationPulses, int maxTargets, boolean hitPlayers, boolean hitMobs, SpellContext context) {
            this.beacon = beacon;
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
                if (beacon.isValid()) beacon.remove();
                this.cancel();
                return;
            }

            List<LivingEntity> nearbyEntities = beacon.getWorld().getNearbyLivingEntities(beacon.getLocation(), radius).stream().limit(maxTargets).toList();

            for (LivingEntity entity : nearbyEntities) {
                if (entity.equals(beacon)) continue;

                if (entity instanceof Player) { // Simple ally check
                    entity.setHealth(Math.min(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), entity.getHealth() + healAmount));
                    entity.getActivePotionEffects().stream().filter(e -> isNegativeEffect(e.getType())).findFirst().ifPresent(e -> entity.removePotionEffect(e.getType()));
                } else if (hitMobs) {
                    entity.damage(damageAmount, context.caster());
                }
            }

            spawnBeaconParticles(beacon.getLocation(), radius);
            if (pulsesCompleted % 2 == 0) {
                context.fx().playSound(beacon.getLocation(), Sound.BLOCK_BELL_USE, 0.5f, 1.5f);
            }
            pulsesCompleted++;
        }

        private boolean isNegativeEffect(PotionEffectType type) {
            return List.of(PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA).contains(type);
        }

        private void spawnBeaconParticles(Location location, double radius) {
            for (int i = 0; i < 10; i++) {
                context.fx().spawnParticles(location.clone().add(0, i * 0.5, 0), Particle.END_ROD, 1, 0, 0, 0, 0);
            }
            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                context.fx().spawnParticles(location.clone().add(radius * 0.8 * Math.cos(angle), 1, radius * 0.8 * Math.sin(angle)), Particle.GLOW, 1, 0, 0, 0, 0);
            }
        }
    }
}