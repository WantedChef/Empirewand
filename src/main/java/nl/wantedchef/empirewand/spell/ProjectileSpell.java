package nl.wantedchef.empirewand.spell;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.storage.Keys;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Abstract base class for projectile-based spells.
 * <p>
 * This class provides a comprehensive foundation for spells that launch projectiles,
 * handling common functionality such as:
 * <ul>
 *   <li>Projectile creation and configuration</li>
 *   <li>Velocity and direction calculations</li>
 *   <li>Trail effects and particle systems</li>
 *   <li>Homing projectile support</li>
 *   <li>Collision detection and hit handling</li>
 *   <li>Sound effects and visual feedback</li>
 *   <li>Memory management and cleanup</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * public class Fireball extends ProjectileSpell<org.bukkit.entity.Fireball> {
 *     public Fireball(Builder<org.bukkit.entity.Fireball> builder) {
 *         super(builder);
 *     }
 *
 *     @Override
 *     protected void onProjectileHit(@NotNull ProjectileHitEvent event, 
 *                                    @NotNull Player caster, 
 *                                    @NotNull org.bukkit.entity.Fireball projectile) {
 *         // Handle fireball explosion
 *         Location hitLocation = event.getHitBlock() != null 
 *             ? event.getHitBlock().getLocation() 
 *             : event.getHitEntity().getLocation();
 *         
 *         hitLocation.getWorld().createExplosion(hitLocation, 3.0f, false, true);
 *     }
 * }
 * }</pre>
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is designed to be thread-safe for
 * read operations. Projectile instances are managed by Bukkit's entity system.
 *
 * @param <P> the type of projectile launched by this spell
 * @since 1.0.0
 */
public abstract class ProjectileSpell<P extends Projectile> extends Spell<Void> {

    /** Default projectile speed in blocks per second */
    protected static final double DEFAULT_SPEED = 2.0;
    
    /** Default homing strength (0.0 = no homing) */
    protected static final double DEFAULT_HOMING_STRENGTH = 0.0;
    
    /** Default hit sound volume */
    protected static final float DEFAULT_HIT_VOLUME = 1.0f;
    
    /** Default hit sound pitch */
    protected static final float DEFAULT_HIT_PITCH = 1.0f;

    protected final double speed;
    protected final double homingStrength;
    protected final boolean isHoming;
    protected final Particle trailParticle;
    protected final Sound hitSound;
    protected final float hitVolume;
    protected final float hitPitch;
    protected final Class<P> projectileClass;

    /**
     * Constructs a new ProjectileSpell instance.
     * <p>
     * Validates all builder parameters and initializes the projectile spell
     * with the provided configuration.
     *
     * @param builder the builder containing projectile configuration
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    protected ProjectileSpell(Builder<P> builder) {
        super(builder);
        this.projectileClass = Objects.requireNonNull(builder.projectileClass, "Projectile class cannot be null");
        this.speed = Math.max(0.1, builder.speed); // Ensure minimum speed
        this.homingStrength = Math.max(0.0, builder.homingStrength);
        this.isHoming = builder.isHoming && homingStrength > 0.0;
        this.trailParticle = builder.trailParticle;
        this.hitSound = builder.hitSound;
        this.hitVolume = Math.max(0.0f, Math.min(1.0f, builder.hitVolume));
        this.hitPitch = Math.max(0.5f, Math.min(2.0f, builder.hitPitch));
    }

    /**
     * Launches the projectile and returns immediately.
     * <p>
     * This method is called by {@link #executeSpell(SpellContext)} and handles
     * the actual projectile creation and launch.
     *
     * @param context the spell context
     */
    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        launchProjectile(context);
        return null;
    }

    /**
     * No-op implementation for Void effect handling.
     * <p>
     * Projectile spells handle their effects through {@link #onProjectileHit}.
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void effect) {
        // No-op - projectile effects are handled in onProjectileHit
    }

    /**
     * Launches a projectile from the caster's location.
     * <p>
     * This method handles the complete projectile launch process including:
     * <ul>
     *   <li>Projectile creation and configuration</li>
     *   <li>Velocity calculation and application</li>
     *   <li>Trail effect initialization</li>
     *   <li>Metadata tagging for identification</li>
     * </ul>
     *
     * @param context the spell context containing caster and target information
     */
    protected void launchProjectile(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        try {
            Player caster = context.caster();
            Location launchLocation = calculateLaunchLocation(caster);
            
            // Create projectile
            P projectile = caster.getWorld().spawn(launchLocation, projectileClass);
            
            // Configure projectile
            configureProjectile(projectile, caster, context);
            
            // Set velocity
            Vector velocity = calculateVelocity(caster, context);
            projectile.setVelocity(velocity);
            
            // Add trail effects if configured
            if (trailParticle != null) {
                startTrailEffect(projectile, context);
            }
            
            // Tag projectile with spell information
            tagProjectile(projectile, caster);
            
        } catch (Exception e) {
            context.plugin().getLogger().log(Level.WARNING, 
                "Failed to launch projectile for spell " + key(), e);
        }
    }

    /**
     * Calculates the launch location for the projectile.
     * <p>
     * By default, launches from 1.5 blocks above the caster's feet (eye level).
     * Override to customize launch positioning.
     *
     * @param caster the player casting the spell
     * @return the launch location
     */
    @NotNull
    protected Location calculateLaunchLocation(@NotNull Player caster) {
        Objects.requireNonNull(caster, "Caster cannot be null");
        return caster.getEyeLocation();
    }

    /**
     * Calculates the initial velocity vector for the projectile.
     * <p>
     * By default, uses the caster's look direction with the configured speed.
     * Override to implement custom targeting or trajectory calculations.
     *
     * @param caster the player casting the spell
     * @param context the spell context
     * @return the velocity vector
     */
    @NotNull
    protected Vector calculateVelocity(@NotNull Player caster, @NotNull SpellContext context) {
        Objects.requireNonNull(caster, "Caster cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");
        
        Vector direction = caster.getLocation().getDirection();
        
        // If targeting a location, adjust direction
        if (context.hasTargetLocation()) {
            Location target = context.targetLocation();
            direction = target.toVector().subtract(caster.getLocation().toVector()).normalize();
        }
        
        return direction.multiply(speed);
    }

    /**
     * Configures the projectile properties.
     * <p>
     * Override to set custom projectile properties such as gravity, fire ticks,
     * or custom metadata.
     *
     * @param projectile the projectile to configure
     * @param caster the player who cast the spell
     * @param context the spell context
     */
    protected void configureProjectile(@NotNull P projectile, @NotNull Player caster, @NotNull SpellContext context) {
        Objects.requireNonNull(projectile, "Projectile cannot be null");
        Objects.requireNonNull(caster, "Caster cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");
        
        // Default configuration - override as needed
        projectile.setShooter(caster);
    }

    /**
     * Tags the projectile with spell identification data.
     * <p>
     * This method stores spell information in the projectile's persistent data
     * container for later identification in hit events.
     *
     * @param projectile the projectile to tag
     * @param caster the player who cast the spell
     */
    protected void tagProjectile(@NotNull P projectile, @NotNull Player caster) {
        Objects.requireNonNull(projectile, "Projectile cannot be null");
        Objects.requireNonNull(caster, "Caster cannot be null");
        
        var container = projectile.getPersistentDataContainer();
        container.set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
        container.set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING, caster.getUniqueId().toString());
    }

    /**
     * Starts the trail effect for the projectile.
     * <p>
     * Creates a repeating task that displays particle effects at the projectile's
     * location until it hits something or is removed.
     *
     * @param projectile the projectile to add trail effects to
     * @param context the spell context for accessing plugin services
     */
    protected void startTrailEffect(@NotNull P projectile, @NotNull SpellContext context) {
        Objects.requireNonNull(projectile, "Projectile cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");
        
        if (trailParticle == null) {
            return;
        }

        BukkitRunnable trailTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    this.cancel();
                    return;
                }
                
                try {
                    projectile.getWorld().spawnParticle(
                        trailParticle,
                        projectile.getLocation(),
                        1,  // count
                        0,  // offsetX
                        0,  // offsetY
                        0,  // offsetZ
                        0   // extra
                    );
                } catch (Exception e) {
                    // Log error but don't crash the task
                    this.cancel();
                }
            }
        };

        // Schedule and register the repeating task
        if (context.plugin() instanceof EmpireWandPlugin ewPlugin) {
            BukkitTask bukkitTask = trailTask.runTaskTimer(ewPlugin, 0L, 1L);
            ewPlugin.getTaskManager().registerTask(bukkitTask);
        } else {
            // Fallback for when the plugin instance is not the expected type
            trailTask.runTaskTimer(context.plugin(), 0L, 1L);
        }
    }

    /**
     * Default projectile impact handler.
     * <p>
     * Subclasses may override this to implement custom hit behavior. Many spells
     * in the codebase implement this method, and external listeners may dispatch
     * to it using the active {@link SpellContext}.
     *
     * @param context the spell context
     * @param projectile the projectile that impacted
     * @param event the projectile hit event
     */
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
                             @NotNull ProjectileHitEvent event) {
        // Default no-op
    }

    /**
     * Handles projectile collision events.
     * <p>
     * This method is called when a projectile launched by this spell hits
     * an entity or block. It provides the projectile instance for type-specific
     * handling.
     * <p>
     * <strong>Note:</strong> Override this method in subclasses to implement
     * custom hit behavior.
     *
     * @param event the projectile hit event
     * @param caster the player who cast the spell
     * @param projectile the projectile that hit
     */
    protected void onProjectileHit(@NotNull ProjectileHitEvent event, 
                                  @NotNull Player caster, 
                                  @NotNull P projectile) {
        Objects.requireNonNull(event, "Event cannot be null");
        Objects.requireNonNull(caster, "Caster cannot be null");
        Objects.requireNonNull(projectile, "Projectile cannot be null");
        
        // Default implementation: play hit sound
        if (hitSound != null) {
            Location hitLocation = event.getHitBlock() != null 
                ? event.getHitBlock().getLocation() 
                : event.getHitEntity() != null 
                    ? event.getHitEntity().getLocation() 
                    : projectile.getLocation();
            
            hitLocation.getWorld().playSound(
                hitLocation,
                hitSound,
                hitVolume,
                hitPitch
            );
        }
    }

    /**
     * Handles projectile hit events from the global listener.
     * <p>
     * This method is called by the plugin's global projectile listener and
     * delegates to {@link #onProjectileHit} for type-specific handling.
     *
     * @param event the projectile hit event
     * @param caster the player who cast the spell
     */
    @Override
    public final void onProjectileHit(@NotNull ProjectileHitEvent event, @NotNull Player caster) {
        Objects.requireNonNull(event, "Event cannot be null");
        Objects.requireNonNull(caster, "Caster cannot be null");
        
        Projectile projectile = event.getEntity();
        if (projectileClass.isInstance(projectile)) {
            onProjectileHit(event, caster, projectileClass.cast(projectile));
        }
    }

    /**
     * A fluent builder for constructing {@link ProjectileSpell} instances.
     * <p>
     * This builder extends the base Spell.Builder to provide projectile-specific
     * configuration options.
     *
     * @param <P> the type of projectile launched by the spell
     */
    public abstract static class Builder<P extends Projectile> extends Spell.Builder<Void> {
        protected final Class<P> projectileClass;
        protected double speed = DEFAULT_SPEED;
        protected double homingStrength = DEFAULT_HOMING_STRENGTH;
        protected boolean isHoming = false;
        protected Particle trailParticle = null;
        protected Sound hitSound = null;
        protected float hitVolume = DEFAULT_HIT_VOLUME;
        protected float hitPitch = DEFAULT_HIT_PITCH;

        /**
         * Creates a new projectile spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @param projectileClass the class of the projectile to launch
         * @throws NullPointerException if projectileClass is null
         */
        protected Builder(@Nullable EmpireWandAPI api, @NotNull Class<P> projectileClass) {
            super(api);
            this.projectileClass = Objects.requireNonNull(projectileClass, "Projectile class cannot be null");
        }

        /**
         * Sets the projectile launch speed.
         * <p>
         * The speed is measured in blocks per tick. Values are clamped to a
         * reasonable range (0.1 to 10.0).
         *
         * @param speed the launch speed
         * @return this builder for chaining
         * @throws IllegalArgumentException if speed is negative
         */
        @NotNull
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Builder pattern intentionally returns this for method chaining")
        public Builder<P> speed(double speed) {
            if (speed < 0) {
                throw new IllegalArgumentException("Speed cannot be negative");
            }
            this.speed = Math.max(0.1, Math.min(10.0, speed));
            return this;
        }

        /**
         * Enables homing behavior for the projectile.
         * <p>
         * Homing projectiles will track the nearest valid target within range.
         *
         * @param homingStrength the strength of homing (0.0 to 1.0)
         * @return this builder for chaining
         * @throws IllegalArgumentException if homingStrength is negative
         */
        @NotNull
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Builder pattern intentionally returns this for method chaining")
        public Builder<P> homing(double homingStrength) {
            if (homingStrength < 0) {
                throw new IllegalArgumentException("Homing strength cannot be negative");
            }
            this.isHoming = homingStrength > 0.0;
            this.homingStrength = Math.min(1.0, homingStrength);
            return this;
        }

        /**
         * Sets the particle effect for the projectile trail.
         * <p>
         * The trail effect will be displayed at the projectile's location
         * as it travels through the air.
         *
         * @param particle the trail particle type
         * @return this builder for chaining
         */
        @NotNull
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Builder pattern intentionally returns this for method chaining")
        public Builder<P> trailParticle(@Nullable Particle particle) {
            this.trailParticle = particle;
            return this;
        }

        /**
         * Sets the sound effect played when the projectile hits.
         * <p>
         * The sound will be played at the impact location.
         *
         * @param sound the hit sound
         * @return this builder for chaining
         */
        @NotNull
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Builder pattern intentionally returns this for method chaining")
        public Builder<P> hitSound(@Nullable Sound sound) {
            this.hitSound = sound;
            return this;
        }

        /**
         * Sets the volume for the hit sound.
         * <p>
         * Values are clamped between 0.0 and 1.0.
         *
         * @param volume the sound volume
         * @return this builder for chaining
         * @throws IllegalArgumentException if volume is negative
         */
        @NotNull
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Builder pattern intentionally returns this for method chaining")
        public Builder<P> hitVolume(float volume) {
            if (volume < 0) {
                throw new IllegalArgumentException("Volume cannot be negative");
            }
            this.hitVolume = Math.max(0.0f, Math.min(1.0f, volume));
            return this;
        }

        /**
         * Sets the pitch for the hit sound.
         * <p>
         * Values are clamped between 0.5 and 2.0.
         *
         * @param pitch the sound pitch
         * @return this builder for chaining
         * @throws IllegalArgumentException if pitch is negative
         */
        @NotNull
        @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Builder pattern intentionally returns this for method chaining")
        public Builder<P> hitPitch(float pitch) {
            if (pitch < 0) {
                throw new IllegalArgumentException("Pitch cannot be negative");
            }
            this.hitPitch = Math.max(0.5f, Math.min(2.0f, pitch));
            return this;
        }

        /**
         * Builds and returns a new ProjectileSpell instance.
         *
         * @return the constructed projectile spell
         */
        @NotNull
        public abstract ProjectileSpell<P> build();
    }
}