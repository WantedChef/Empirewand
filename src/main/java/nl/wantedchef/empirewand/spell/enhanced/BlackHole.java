package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A devastating spell that creates a black hole, pulling in all entities and crushing them with
 * immense gravitational force.
 */
public class BlackHole extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Black Hole";
            this.description =
                    "Creates a devastating black hole that pulls in all entities and crushes them with immense gravitational force.";
            this.cooldown = java.time.Duration.ofSeconds(90);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new BlackHole(this);
        }
    }

    private BlackHole(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "black-hole";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 25.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 200);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);
        double maxPullStrength = spellConfig.getDouble("values.max-pull-strength", 2.0);
        double eventHorizonRadius = spellConfig.getDouble("values.event-horizon-radius", 3.0);

        // Get target location
        Location targetLocation = player.getTargetBlock(null, 40).getLocation();
        if (targetLocation == null) {
            targetLocation = player.getLocation();
        }

        // Play initial sound
        player.getWorld().playSound(targetLocation, Sound.ENTITY_WITHER_SPAWN, 3.0f, 0.3f);

        // Start black hole effect
        new BlackHoleTask(context, targetLocation, radius, durationTicks, affectsPlayers,
                maxPullStrength, eventHorizonRadius).runTaskTimer(context.plugin(), 0L, 1L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private static class BlackHoleTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int durationTicks;
        private final boolean affectsPlayers;
        private final double maxPullStrength;
        private final double eventHorizonRadius;
        private final World world;
        private int ticks = 0;
        private final Set<Entity> consumedEntities = new HashSet<>();
        private final Map<Entity, Integer> pullTimers = new HashMap<>();

        public BlackHoleTask(SpellContext context, Location center, double radius,
                int durationTicks, boolean affectsPlayers, double maxPullStrength,
                double eventHorizonRadius) {
            this.context = context;
            this.center = center;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.affectsPlayers = affectsPlayers;
            this.maxPullStrength = maxPullStrength;
            this.eventHorizonRadius = eventHorizonRadius;
            this.world = center.getWorld();
        }

        @Override
        public void run() {
            if (ticks >= durationTicks) {
                this.cancel();
                // Play end sound
                world.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 3.0f, 0.5f);
                return;
            }

            // Apply black hole effects
            applyBlackHoleEffects();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        private void applyBlackHoleEffects() {
            Collection<Entity> nearbyEntities =
                    world.getNearbyEntities(center, radius, radius, radius);

            for (Entity entity : nearbyEntities) {
                // Skip already consumed entities
                if (consumedEntities.contains(entity))
                    continue;

                // Skip the caster
                if (entity.equals(context.caster()))
                    continue;

                // Skip players if not affecting players
                if (entity instanceof Player && !affectsPlayers)
                    continue;

                // Skip dead or invalid entities
                if (entity.isDead() || !entity.isValid())
                    continue;

                // Calculate distance
                double distance = entity.getLocation().distance(center);
                if (distance > radius)
                    continue;

                // Check if entity is within event horizon
                if (distance <= eventHorizonRadius) {
                    // Consume the entity
                    consumeEntity(entity);
                    continue;
                }

                // Apply gravitational pull
                double pullStrength = maxPullStrength * (1.0 - (distance / radius)) * 2;

                // Calculate pull vector
                Vector pull = center.toVector().subtract(entity.getLocation().toVector())
                        .normalize().multiply(pullStrength);

                // Apply pull velocity
                entity.setVelocity(entity.getVelocity().add(pull));

                // Increase pull timer
                Integer pullTimer = pullTimers.getOrDefault(entity, 0);
                pullTimer++;
                pullTimers.put(entity, pullTimer);

                // Apply damage based on time being pulled
                if (pullTimer % 20 == 0) {
                    double damage = Math.min(8.0, pullTimer / 40.0);
                    if (entity instanceof LivingEntity) {
                        ((LivingEntity) entity).damage(damage, context.caster());
                    }
                }

                // Visual effect for pulled entities
                if (ticks % 3 == 0) {
                    world.spawnParticle(Particle.SQUID_INK, entity.getLocation().add(0, 1, 0), 3,
                            0.2, 0.3, 0.2, 0.01);
                }
            }
        }

        private void consumeEntity(Entity entity) {
            if (consumedEntities.contains(entity))
                return;

            consumedEntities.add(entity);

            // Visual effect for consumption
            world.spawnParticle(Particle.EXPLOSION_EMITTER, entity.getLocation(), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.SMOKE, entity.getLocation(), 50, 1, 1, 1, 0.1);

            // Sound effect
            world.playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.3f);

            // Damage and remove entity
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).damage(1000.0, context.caster()); // Massive damage to
                                                                          // ensure death
            }

            // Remove entity after a short delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isValid()) {
                        entity.remove();
                    }
                }
            }.runTaskLater(context.plugin(), 2L);
        }

        private void createVisualEffects() {
            // Create accretion disk
            double diskRadius = eventHorizonRadius + 2 + Math.sin(ticks * 0.2) * 1.5;
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i / 24) + (ticks * 0.1);
                double x = diskRadius * Math.cos(angle);
                double z = diskRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, Math.sin(ticks * 0.3) * 0.5, z);
                world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
            }

            // Create event horizon
            for (int i = 0; i < 16; i++) {
                double angle = 2 * Math.PI * i / 16;
                double x = eventHorizonRadius * Math.cos(angle);
                double z = eventHorizonRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0, z);
                world.spawnParticle(Particle.SQUID_INK, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
            }

            // Create gravitational lensing effect
            double lensRadius = radius * (1.0 - (ticks / (double) durationTicks));
            for (int i = 0; i < 36; i++) {
                double angle = 2 * Math.PI * i / 36;
                double x = lensRadius * Math.cos(angle);
                double z = lensRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0, z);
                world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
            }

            // Create jets (periodically)
            if (ticks % 15 == 0) {
                // North jet
                for (int i = 0; i < 15; i++) {
                    Location jetLoc = center.clone().add(0, i * 0.8, 0);
                    world.spawnParticle(Particle.FLAME, jetLoc, 5, 0.2, 0.2, 0.2, 0.01);
                }

                // South jet
                for (int i = 0; i < 15; i++) {
                    Location jetLoc = center.clone().add(0, -i * 0.8, 0);
                    world.spawnParticle(Particle.FLAME, jetLoc, 5, 0.2, 0.2, 0.2, 0.01);
                }

                // Sound effect
                world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.3f);
            }
        }
    }
}
