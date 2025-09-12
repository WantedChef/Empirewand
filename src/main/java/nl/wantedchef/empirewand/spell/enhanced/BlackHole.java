package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
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

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/**
 * A devastating spell that creates a black hole, pulling in all entities and crushing them with
 * immense gravitational force.
 * <p>
 * This spell creates a powerful black hole at a target location that pulls nearby entities
 * toward its center with increasing force. Entities that get too close are consumed and destroyed
 * by the black hole's immense gravitational force. The spell includes visual effects for the
 * accretion disk, event horizon, and gravitational lensing.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Powerful gravitational pull effect</li>
 *   <li>Entity consumption within event horizon</li>
 *   <li>Visual effects including accretion disk and jets</li>
 *   <li>Audio feedback with wither spawn and dragon death sounds</li>
 *   <li>Configurable radius, duration, and pull strength</li>
 *   <li>Damage scaling based on time entities are pulled</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell blackHole = new BlackHole.Builder(api)
 *     .name("Black Hole")
 *     .description("Creates a devastating black hole that pulls in all entities and crushes them with immense gravitational force.")
 *     .cooldown(Duration.ofSeconds(90))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class BlackHole extends Spell<Void> {

    /**
     * Builder for creating BlackHole spell instances.
     * <p>
     * Provides a fluent API for configuring the black hole spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new BlackHole spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@NotNull EmpireWandAPI api) {
            super(api);
            this.name = "Black Hole";
            this.description =
                    "Creates a devastating black hole that pulls in all entities and crushes them with immense gravitational force.";
            this.cooldown = Duration.ofSeconds(90);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds and returns a new BlackHole spell instance.
         *
         * @return the constructed BlackHole spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new BlackHole(this);
        }
    }

    /**
     * Configuration for the BlackHole spell.
     *
     * @param radius The radius of the black hole's effect
     * @param durationTicks The duration of the black hole in ticks
     * @param affectsPlayers Whether the black hole affects players
     * @param maxPullStrength The maximum pull strength
     * @param eventHorizonRadius The radius of the event horizon
     */
    private record Config(double radius, int durationTicks, boolean affectsPlayers,
                         double maxPullStrength, double eventHorizonRadius) {}

    private Config config = new Config(25.0, 200, true, 2.0, 3.0);

    /**
     * Constructs a new BlackHole spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private BlackHole(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "black-hole"
     */
    @Override
    @NotNull
    public String key() {
        return "black-hole";
    }

    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        super.loadConfig(spellConfig);
        this.config = new Config(
                spellConfig.getDouble("values.radius", 25.0),
                spellConfig.getInt("values.duration-ticks", 200),
                spellConfig.getBoolean("flags.affects-players", true),
                spellConfig.getDouble("values.max-pull-strength", 2.0),
                spellConfig.getDouble("values.event-horizon-radius", 3.0)
        );
    }

    /**
     * Returns the prerequisites for casting this spell.
     * <p>
     * Currently, this spell has no prerequisites beyond standard casting requirements.
     *
     * @return a no-op prerequisite
     */
    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Executes the black hole spell logic.
     * <p>
     * This method creates a black hole at the target location that pulls entities
     * toward its center and consumes those that get too close.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        // Configuration
        double radius = config.radius;
        int durationTicks = config.durationTicks;
        boolean affectsPlayers = config.affectsPlayers;
        double maxPullStrength = config.maxPullStrength;
        double eventHorizonRadius = config.eventHorizonRadius;

        // Get target location
        Location targetLocation = player.getTargetBlock(null, 40).getLocation();
        if (targetLocation == null) {
            targetLocation = player.getLocation();
        }

        // Play initial sound
        var world = player.getWorld();
        if (world != null) {
            world.playSound(targetLocation, Sound.ENTITY_WITHER_SPAWN, 3.0f, 0.3f);
        }

        // Start black hole effect
        BukkitRunnable blackHoleTask = new BlackHoleTask(context, targetLocation, radius, durationTicks, affectsPlayers,
                maxPullStrength, eventHorizonRadius);
        context.plugin().getTaskManager().runTaskTimer(blackHoleTask, 0L, 1L);
        return null;
    }

    /**
     * Handles the spell effect after execution.
     * <p>
     * This spell's effects are handled asynchronously through BukkitRunnables.
     *
     * @param context the spell context
     * @param result the result of the spell execution (always null for this spell)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    /**
     * A runnable that handles the black hole's gravitational effects and visual feedback.
     * <p>
     * This task manages the black hole's behavior over time, including pulling entities,
     * consuming those within the event horizon, and creating visual effects.
     */
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

        /**
         * Creates a new BlackHoleTask instance.
         *
         * @param context the spell context
         * @param center the center location of the black hole
         * @param radius the radius of the black hole's effect
         * @param durationTicks the duration of the black hole in ticks
         * @param affectsPlayers whether the black hole affects players
         * @param maxPullStrength the maximum pull strength
         * @param eventHorizonRadius the radius of the event horizon
         */
        public BlackHoleTask(@NotNull SpellContext context, @NotNull Location center, double radius,
                int durationTicks, boolean affectsPlayers, double maxPullStrength,
                double eventHorizonRadius) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.affectsPlayers = affectsPlayers;
            this.maxPullStrength = maxPullStrength;
            this.eventHorizonRadius = eventHorizonRadius;
            this.world = center.getWorld();
        }

        /**
         * Runs the black hole task, applying gravitational effects and visual feedback.
         */
        @Override
        public void run() {
            if (world == null) {
                this.cancel();
                return;
            }
            
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

        /**
         * Applies the black hole's gravitational effects to nearby entities.
         * <p>
         * This method pulls entities toward the black hole's center and consumes
         * those that get too close to the event horizon.
         */
        private void applyBlackHoleEffects() {
            if (world == null) {
                return;
            }
            
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

                var entityLocation = entity.getLocation();
                if (entityLocation == null) {
                    continue;
                }
                
                // Calculate distance
                double distance = entityLocation.distance(center);
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
                Vector pull = center.toVector().subtract(entityLocation.toVector())
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
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.damage(damage, context.caster());
                    }
                }

                // Visual effect for pulled entities
                if (ticks % 3 == 0) {
                    world.spawnParticle(Particle.SQUID_INK, entityLocation.add(0, 1, 0), 3,
                            0.2, 0.3, 0.2, 0.01);
                }
            }
        }

        /**
         * Consumes an entity that has crossed the event horizon.
         * <p>
         * This method applies massive damage to the entity and removes it after
         * a short delay, creating visual and audio feedback for the consumption.
         *
         * @param entity the entity to consume
         */
        private void consumeEntity(@NotNull Entity entity) {
            Objects.requireNonNull(entity, "Entity cannot be null");
            
            if (consumedEntities.contains(entity))
                return;

            consumedEntities.add(entity);

            var entityLocation = entity.getLocation();
            if (world == null || entityLocation == null) {
                return;
            }
            
            // Visual effect for consumption
            world.spawnParticle(Particle.EXPLOSION_EMITTER, entityLocation, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.SMOKE, entityLocation, 50, 1, 1, 1, 0.1);

            // Sound effect
            world.playSound(entityLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.3f);

            // Damage and remove entity
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.damage(1000.0, context.caster()); // Massive damage to
                                                                          // ensure death
            }

            // Remove entity after a short delay
            BukkitRunnable removeTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isValid()) {
                        entity.remove();
                    }
                }
            };
            context.plugin().getTaskManager().runTaskLater(removeTask, 2L);
        }

        /**
         * Creates the black hole's visual effects.
         * <p>
         * This method generates particles for the accretion disk, event horizon,
         * gravitational lensing, and periodic jets.
         */
        private void createVisualEffects() {
            if (world == null) {
                return;
            }
            
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
