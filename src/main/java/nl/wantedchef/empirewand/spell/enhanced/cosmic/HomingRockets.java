package nl.wantedchef.empirewand.spell.enhanced.cosmic;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.entity.Fireball;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A powerful spell that fires multiple homing rockets that seek out targets
 * and explode on impact, dealing area damage.
 * <p>
 * This spell launches multiple fireball projectiles that home in on nearby entities.
 * The rockets explode on impact, dealing area damage to entities in the blast radius.
 * Visual effects include flame particle trails and explosion particles, with audio
 * feedback for both launch and impact.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Multiple homing fireball projectiles</li>
 *   <li>Target-seeking behavior with configurable strength</li>
 *   <li>Area of effect damage on explosion</li>
 *   <li>Flame particle trails</li>
 *   <li>Audio feedback for launch and impact</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell homingRockets = new HomingRockets.Builder(api)
 *     .name("Homing Rockets")
 *     .description("Fires multiple homing rockets that seek out targets and explode on impact.")
 *     .cooldown(Duration.ofSeconds(25))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class HomingRockets extends ProjectileSpell<Fireball> {

    /**
     * Configuration record containing all HomingRockets spell settings.
     * <p>
     * This record holds all configuration values loaded from the spell configuration.
     * Using a record provides immutable configuration access and better performance
     * compared to repeated map lookups.
     *
     * @param rocketCount     the number of rockets to launch
     * @param spread          the spread angle for rockets after the first
     * @param speed           the initial speed of the rockets
     * @param homingStrength  the strength of the homing effect
     */
    private record Config(
        int rocketCount,
        double spread,
        double speed,
        double homingStrength
    ) {}

    /**
     * Default configuration values.
     */
    private static final int DEFAULT_ROCKET_COUNT = 5;
    private static final double DEFAULT_SPREAD = 0.1;
    private static final double DEFAULT_SPEED = 1.5;
    private static final double DEFAULT_HOMING_STRENGTH = 0.2;

    /**
     * Current spell configuration.
     */
    private Config config = new Config(
        DEFAULT_ROCKET_COUNT,
        DEFAULT_SPREAD,
        DEFAULT_SPEED,
        DEFAULT_HOMING_STRENGTH
    );

    /**
     * Builder for creating HomingRockets spell instances.
     * <p>
     * Provides a fluent API for configuring the homing rockets spell with sensible defaults.
     */
    public static class Builder extends ProjectileSpell.Builder<Fireball> {
        /**
         * Creates a new HomingRockets spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api, Fireball.class);
            this.name = "Homing Rockets";
            this.description = "Fires multiple homing rockets that seek out targets and explode on impact.";
            this.cooldown = Duration.ofSeconds(25);
            this.spellType = SpellType.FIRE;
            this.trailParticle = Particle.FLAME;
            this.hitSound = Sound.ENTITY_GENERIC_EXPLODE;
        }

        /**
         * Builds and returns a new HomingRockets spell instance.
         *
         * @return the constructed HomingRockets spell
         */
        @Override
        @NotNull
        public ProjectileSpell<Fireball> build() {
            return new HomingRockets(this);
        }
    }

    /**
     * Constructs a new HomingRockets spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private HomingRockets(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Loads configuration values from the provided ReadableConfig.
     * <p>
     * This method is called during spell initialization to load spell-specific
     * configuration values from the configuration file. It creates an immutable
     * Config record for efficient access during spell execution.
     *
     * @param spellConfig the configuration section for this spell
     * @throws NullPointerException if spellConfig is null
     */
    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        Objects.requireNonNull(spellConfig, "SpellConfig cannot be null");
        super.loadConfig(spellConfig);
        
        this.config = new Config(
            spellConfig.getInt("values.rocket-count", DEFAULT_ROCKET_COUNT),
            spellConfig.getDouble("values.spread", DEFAULT_SPREAD),
            spellConfig.getDouble("values.speed", DEFAULT_SPEED),
            spellConfig.getDouble("values.homing-strength", DEFAULT_HOMING_STRENGTH)
        );
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "homing-rockets"
     */
    @Override
    @NotNull
    public String key() {
        return "homing-rockets";
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
     * Launches the homing rocket projectiles.
     * <p>
     * This method launches multiple fireballs with spread that home in on targets
     * and creates particle trails for visual feedback.
     *
     * @param context the spell context containing caster and target information
     */
    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        // Use pre-loaded configuration for better performance
        int rocketCount = config.rocketCount;
        double spread = config.spread;
        double speed = config.speed;
        double homingStrength = config.homingStrength;

        // Play launch sound
        context.fx().playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);

        // Launch multiple rockets with spread
        for (int i = 0; i < rocketCount; i++) {
            // Calculate spread direction
            Vector direction = player.getEyeLocation().getDirection().clone();
            if (i > 0) {
                // Apply spread to rockets after the first
                direction.rotateAroundX((Math.random() - 0.5) * spread);
                direction.rotateAroundY((Math.random() - 0.5) * spread);
            }
            
            direction.multiply(speed);

            Fireball fireball = player.launchProjectile(Fireball.class, direction);
            fireball.setYield(2.0f);
            fireball.setIsIncendiary(true);

            // Start homing behavior
            context.plugin().getTaskManager().runTaskTimer(
                new HomingTask(context, fireball, homingStrength),
                5L, 2L
            );
            
            // Visual trail
            context.fx().followParticles(context.plugin(), fireball, Particle.FLAME, 5, 0.1, 0.1, 0.1, 0.01, null, 1L);
        }
    }

    /**
     * Handles the projectile hit event.
     * <p>
     * This method creates additional explosion effects and sound feedback when
     * the fireball projectile hits a target or block.
     *
     * @param context the spell context
     * @param projectile the projectile that hit
     * @param event the projectile hit event
     */
    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile, @NotNull ProjectileHitEvent event) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(projectile, "Projectile cannot be null");
        Objects.requireNonNull(event, "Event cannot be null");
        
        // The default fireball explosion handles most of the effects
        // We'll add some additional particle effects
        
        Location hitLocation = projectile.getLocation();
        World world = hitLocation.getWorld();
        if (world != null) {
            // Additional explosion effects
            world.spawnParticle(Particle.EXPLOSION_EMITTER, hitLocation, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, hitLocation, 50, 1, 1, 1, 0.1);
            
            // Sound effect
            world.playSound(hitLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        }
    }

    /**
     * A runnable that handles the homing behavior of fireball projectiles.
     * <p>
     * This task makes fireball projectiles home in on nearby entities by adjusting
     * their velocity toward the nearest valid target.
     */
    private static class HomingTask extends BukkitRunnable {
        private final SpellContext context;
        private final Fireball fireball;
        private final double homingStrength;
        private int ticks = 0;
        private static final int MAX_LIFETIME = 100; // 5 seconds

        /**
         * Creates a new HomingTask instance.
         *
         * @param context the spell context
         * @param fireball the fireball projectile to apply homing to
         * @param homingStrength the strength of the homing effect
         */
        public HomingTask(@NotNull SpellContext context, @NotNull Fireball fireball, double homingStrength) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.fireball = Objects.requireNonNull(fireball, "Fireball cannot be null");
            this.homingStrength = homingStrength;
        }

        /**
         * Runs the homing task, adjusting the fireball's velocity toward the nearest target.
         */
        @Override
        public void run() {
            if (!fireball.isValid() || fireball.isDead() || ticks > MAX_LIFETIME) {
                this.cancel();
                return;
            }

            // Find nearest target
            Entity target = findNearestTarget();
            if (target != null) {
                var fireballLocation = fireball.getLocation();
                var targetLocation = target.getLocation();
                
                if (fireballLocation != null && targetLocation != null) {
                    // Calculate homing direction
                    Vector toTarget = targetLocation.toVector().subtract(fireballLocation.toVector()).normalize();
                    Vector currentVelocity = fireball.getVelocity().normalize();
                    Vector newVelocity = currentVelocity.clone().add(toTarget.clone().multiply(homingStrength)).normalize();
                    
                    // Apply velocity change
                    fireball.setVelocity(newVelocity.multiply(currentVelocity.length()));
                    
                    // Visual effect showing homing
                    fireball.getWorld().spawnParticle(Particle.CRIT, fireballLocation, 3, 0.1, 0.1, 0.1, 0.01);
                }
            }

            ticks++;
        }

        /**
         * Finds the nearest valid target for the fireball to home in on.
         * <p>
         * This method searches for nearby living entities that are not the caster
         * and returns the closest one.
         *
         * @return the nearest valid target entity, or null if none found
         */
        private @Nullable Entity findNearestTarget() {
            Location fireballLocation = fireball.getLocation();
            if (fireballLocation == null) {
                return null;
            }
            
            double closestDistance = 20.0; // Max homing range
            Entity closestEntity = null;

            for (Entity entity : fireball.getNearbyEntities(15, 15, 15)) {
                // Skip non-living entities, the caster, and invalid entities
                if (!(entity instanceof LivingEntity) || entity.equals(context.caster()) || 
                    entity.isDead() || !entity.isValid()) {
                    continue;
                }

                var entityLocation = entity.getLocation();
                if (entityLocation == null) {
                    continue;
                }
                
                double distance = entityLocation.distance(fireballLocation);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }

            return closestEntity;
        }
    }
}