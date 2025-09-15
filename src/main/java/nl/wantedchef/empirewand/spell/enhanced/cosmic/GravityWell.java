package nl.wantedchef.empirewand.spell.enhanced.cosmic;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * A powerful spell that creates a gravity well,
 * pulling all entities toward the center and crushing them with increasing force.
 * <p>
 * This spell creates a gravitational field that pulls nearby entities toward its center
 * with increasing strength as they get closer. Entities that get too close to the center
 * suffer crushing damage that increases over time. The spell includes visual effects with
 * spiraling smoke particles and a vertical beam, and provides audio feedback for both
 * activation and deactivation.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Area of effect gravitational pull</li>
 *   <li>Increasing pull strength toward center</li>
 *   <li>Crushing damage for entities near center</li>
 *   <li>Spiraling particle visual effects</li>
 *   <li>Periodic shockwave effects</li>
 *   <li>Audio feedback for activation and deactivation</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell gravityWell = new GravityWell.Builder(api)
 *     .name("Gravity Well")
 *     .description("Creates a powerful gravity well that pulls all entities toward the center and crushes them.")
 *     .cooldown(Duration.ofSeconds(60))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class GravityWell extends Spell<Void> {

    /**
     * Builder for creating GravityWell spell instances.
     * <p>
     * Provides a fluent API for configuring the gravity well spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new GravityWell spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Gravity Well";
            this.description = "Creates a powerful gravity well that pulls all entities toward the center and crushes them.";
            this.cooldown = Duration.ofSeconds(60);
            this.spellType = SpellType.CONTROL;
        }

        /**
         * Builds and returns a new GravityWell spell instance.
         *
         * @return the constructed GravityWell spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new GravityWell(this);
        }
    }

    /**
     * Constructs a new GravityWell spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private GravityWell(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "gravity-well"
     */
    @Override
    @NotNull
    public String key() {
        return "gravity-well";
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
     * Executes the gravity well spell logic.
     * <p>
     * This method creates a gravitational field at the target location that pulls
     * entities toward its center and applies crushing damage to those that get too close.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        var world = player.getWorld();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 20.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 150);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);
        double maxPullStrength = spellConfig.getDouble("values.max-pull-strength", 1.2);

        // Get target location (or use player location)
        Location targetLocation = player.getTargetBlock(null, 30).getLocation();
        if (targetLocation == null) {
            targetLocation = player.getLocation();
        }

        // Play initial sound
        if (world != null) {
            world.playSound(targetLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
        }

        // Start gravity well effect
        context.plugin().getTaskManager().runTaskTimer(
            new GravityWellTask(context, targetLocation, radius, durationTicks, affectsPlayers, maxPullStrength),
            0L, 1L
        );
        
        // Inform player what the spell does
        player.sendMessage("§5§lGRAVITY WELL §7- A gravitational anomaly pulls enemies toward the center!");
        player.sendMessage("§7Entities too close to the center will be crushed by the intense gravity.");
        
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
     * A runnable that handles the gravity well's effects over time.
     * <p>
     * This task manages the gravitational pull effects, crushing damage, and visual
     * particle effects for the gravity well.
     */
    private static class GravityWellTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int durationTicks;
        private final boolean affectsPlayers;
        private final double maxPullStrength;
        private final World world;
        private int ticks = 0;
        private final Map<LivingEntity, Integer> crushTimers = new HashMap<>();

        /**
         * Creates a new GravityWellTask instance.
         *
         * @param context the spell context
         * @param center the center location of the gravity well
         * @param radius the radius of the gravity well effect
         * @param durationTicks the duration of the gravity well in ticks
         * @param affectsPlayers whether the gravity well affects players
         * @param maxPullStrength the maximum pull strength toward the center
         */
        public GravityWellTask(@NotNull SpellContext context, @NotNull Location center, double radius, 
                              int durationTicks, boolean affectsPlayers, double maxPullStrength) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.affectsPlayers = affectsPlayers;
            this.maxPullStrength = maxPullStrength;
            this.world = center.getWorld();
        }

        /**
         * Runs the gravity well task, applying gravitational effects and creating visual effects.
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
                world.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.8f);
                return;
            }

            // Apply gravity well effects
            applyGravityEffects();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        /**
         * Applies the gravitational pull effects to entities within the radius.
         * <p>
         * This method pulls entities toward the center with increasing strength as
         * they get closer and applies crushing damage to those near the center.
         */
        private void applyGravityEffects() {
            if (world == null) {
                return;
            }
            
            Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(center, radius, radius, radius);
            
            for (LivingEntity entity : nearbyEntities) {
                // Skip the caster
                if (entity.equals(context.caster())) continue;
                
                // Skip players if not affecting players
                if (entity instanceof Player && !affectsPlayers) continue;
                
                // Skip dead or invalid entities
                if (entity.isDead() || !entity.isValid()) continue;
                
                var entityLocation = entity.getLocation();
                if (entityLocation == null) continue;

                // Calculate distance and pull strength
                double distance = entityLocation.distance(center);
                if (distance > radius) continue;
                
                // Pull strength increases as entity gets closer to center
                double pullStrength = maxPullStrength * (1.0 - (distance / radius));
                
                // Calculate pull vector
                Vector pull = center.toVector().subtract(entityLocation.toVector()).normalize().multiply(pullStrength);
                
                // Apply pull velocity
                entity.setVelocity(entity.getVelocity().add(pull));
                
                // Apply crushing damage when very close to center
                if (distance < radius * 0.2) {
                    Integer crushTimer = crushTimers.getOrDefault(entity, 0);
                    crushTimer++;
                    crushTimers.put(entity, crushTimer);
                    
                    // Damage increases with time in center
                    if (crushTimer % 10 == 0) {
                        double damage = Math.min(10.0, crushTimer / 20.0);
                        entity.damage(damage, context.caster());
                        
                        // Visual effect for crushing
                        world.spawnParticle(Particle.CRIT, entityLocation.add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
                    }
                } else {
                    // Reset crush timer when not in center
                    crushTimers.remove(entity);
                }
                
                // Visual effect for pulled entities
                if (ticks % 10 == 0) { // Reduced frequency from every 5 ticks to every 10 ticks
                    world.spawnParticle(Particle.PORTAL, entityLocation.add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.01);
                }
            }
        }

        /**
         * Creates the gravity well's visual particle effects.
         * <p>
         * This method generates spiraling smoke particles, a vertical beam, and
         * periodic shockwave effects to visualize the gravity well.
         */
        private void createVisualEffects() {
            if (world == null) {
                return;
            }
            
            // Create spiral effect pulling toward center
            double spiralRadius = radius * (1.0 - (ticks / (double) durationTicks));
            
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i / 8) + (ticks * 0.2);
                double x = spiralRadius * Math.cos(angle);
                double z = spiralRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, Math.sin(ticks * 0.1) * 3, z);
                world.spawnParticle(Particle.SMOKE, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
            }
            
            // Create vertical beam
            if (ticks % 5 == 0) {
                for (int i = 0; i < 20; i += 2) { // Increased spacing from every 0.5 to every 1.0 blocks
                    double y = i * 1.0;
                    Location beamLoc = center.clone().add(0, y, 0);
                    world.spawnParticle(Particle.PORTAL, beamLoc, 2, 0.3, 0.3, 0.3, 0.01);
                }
            }
            
            // Create shockwave periodically
            if (ticks % 30 == 0) {
                double shockwaveRadius = radius * 0.3 + (ticks % 60) / 60.0 * radius * 0.7;
                for (int i = 0; i < 36; i++) {
                    double angle = 2 * Math.PI * i / 36;
                    double x = shockwaveRadius * Math.cos(angle);
                    double z = shockwaveRadius * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.CRIT, particleLoc, 2, 0, 0, 0, 0);
                }
                
                // Sound effect
                world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.3f);
            }
        }
    }
}