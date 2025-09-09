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
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RadiantBeacon extends Spell<Void> {

    private double radius;
    private double healAmount;
    private double damageAmount;
    private int totalPulses;
    private int pulseInterval;
    private int maxTargets;
    private boolean hitPlayers;
    private boolean hitMobs;
    private boolean damagePlayers;

    private static final Set<PotionEffectType> NEGATIVE_EFFECTS = Set.of(
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA);

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Radiant Beacon";
            this.description = "Creates a beacon that heals allies and damages enemies.";
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

        this.radius = spellConfig.getDouble("values.radius", 6.0);
        this.healAmount = spellConfig.getDouble("values.heal-amount", 1.0);
        this.damageAmount = spellConfig.getDouble("values.damage-amount", 1.0);
        this.totalPulses = spellConfig.getInt("values.duration-pulses", 8);
        this.pulseInterval = spellConfig.getInt("values.pulse-interval-ticks", 20);
        this.maxTargets = spellConfig.getInt("values.max-targets", 8);
        this.hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        this.hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);
        this.damagePlayers = spellConfig.getBoolean("flags.damage-players", false);

        ArmorStand beacon = player.getWorld().spawn(player.getLocation(), ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setPersistent(false);
        });

        // FIX 1: Null-check voor de plugin instance
        Plugin plugin = context.plugin();
        if (plugin == null) {
            beacon.remove(); // Ruim de armor stand op als de taak niet kan starten
            return null;
        }

        new BeaconTask(beacon, context).runTaskTimer(plugin, 0L, this.pulseInterval);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private class BeaconTask extends BukkitRunnable {
        private final ArmorStand beacon;
        private final SpellContext context;
        private int pulsesCompleted = 0;

        public BeaconTask(ArmorStand beacon, SpellContext context) {
            this.beacon = beacon;
            this.context = context;
        }

        @Override
        public synchronized void cancel() throws IllegalStateException {
            super.cancel();
            if (beacon.isValid()) {
                beacon.remove();
            }
        }

        @Override
        public void run() {
            if (!beacon.isValid() || pulsesCompleted >= totalPulses) {
                this.cancel();
                return;
            }

            List<LivingEntity> nearbyEntities = beacon.getWorld()
                    .getNearbyLivingEntities(beacon.getLocation(), radius, entity -> !entity.equals(beacon) &&
                            ((entity instanceof Player && hitPlayers) || (!(entity instanceof Player) && hitMobs)))
                    .stream()
                    .limit(maxTargets)
                    .collect(Collectors.toList());

            for (LivingEntity entity : nearbyEntities) {
                if (entity instanceof Player playerTarget) {
                    if (damagePlayers && !playerTarget.equals(context.caster())) {
                        entity.damage(damageAmount, context.caster());
                    } else {
                        // FIX 3: Veilige null-check voor max health attribute
                        AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                        if (maxHealthAttribute != null) {
                            double maxHealth = maxHealthAttribute.getValue();
                            entity.setHealth(Math.min(maxHealth, entity.getHealth() + healAmount));
                        }

                        entity.getActivePotionEffects().stream()
                                .filter(e -> NEGATIVE_EFFECTS.contains(e.getType()))
                                .findFirst()
                                .ifPresent(e -> entity.removePotionEffect(e.getType()));
                    }
                } else {
                    entity.damage(damageAmount, context.caster());
                }
            }

            spawnBeaconParticles(beacon.getLocation(), radius);
            if (pulsesCompleted % 2 == 0) {
                context.fx().playSound(beacon.getLocation(), Sound.BLOCK_BELL_USE, 0.5f, 1.5f);
            }
            pulsesCompleted++;
        }

        private void spawnBeaconParticles(Location location, double radius) {
            final int BEACON_HEIGHT_PARTICLES = 10;
            final double BEACON_PARTICLE_STEP = 0.5;
            final int RING_PARTICLE_COUNT = 20;
            final double RING_RADIUS_MODIFIER = 0.8;

            for (int i = 0; i < BEACON_HEIGHT_PARTICLES; i++) {
                context.fx().spawnParticles(location.clone().add(0, i * BEACON_PARTICLE_STEP, 0), Particle.END_ROD, 1,
                        0, 0, 0, 0);
            }

            for (int i = 0; i < RING_PARTICLE_COUNT; i++) {
                double angle = 2 * Math.PI * i / RING_PARTICLE_COUNT;
                double x = radius * RING_RADIUS_MODIFIER * Math.cos(angle);
                double z = radius * RING_RADIUS_MODIFIER * Math.sin(angle);
                context.fx().spawnParticles(location.clone().add(x, 1, z), Particle.GLOW, 1, 0, 0, 0, 0);
            }
        }
    }
}