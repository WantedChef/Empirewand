package nl.wantedchef.empirewand.spell.enhanced;

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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A powerful spell that dilates time in an area,
 * drastically slowing enemies while allowing the caster to move at normal speed.
 * <p>
 * This spell creates a time dilation field that applies extreme slowness, jump boost,
 * and mining fatigue effects to enemies within the radius while leaving the caster
 * unaffected. The spell includes visual effects with portal and cloud particles to
 * enhance the time distortion experience.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Area of effect time dilation</li>
 *   <li>Extreme slowness for affected entities</li>
 *   <li>Jump boost and mining fatigue effects</li>
 *   <li>Animated particle visual effects</li>
 *   <li>Audio feedback for activation and deactivation</li>
 *   <li>Configurable radius, duration, and amplifier</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell timeDilation = new TimeDilation.Builder(api)
 *     .name("Time Dilation")
 *     .description("Dilates time in an area, drastically slowing enemies while you move at normal speed.")
 *     .cooldown(Duration.ofSeconds(65))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class TimeDilation extends Spell<Void> {

    /**
     * Builder for creating TimeDilation spell instances.
     * <p>
     * Provides a fluent API for configuring the time dilation spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new TimeDilation spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Time Dilation";
            this.description = "Dilates time in an area, drastically slowing enemies while you move at normal speed.";
            this.cooldown = Duration.ofSeconds(65);
            this.spellType = SpellType.CONTROL;
        }

        /**
         * Builds and returns a new TimeDilation spell instance.
         *
         * @return the constructed TimeDilation spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new TimeDilation(this);
        }
    }

    /**
     * Constructs a new TimeDilation spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private TimeDilation(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "time-dilation"
     */
    @Override
    @NotNull
    public String key() {
        return "time-dilation";
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
     * Executes the time dilation spell logic.
     * <p>
     * This method creates a time dilation effect that drastically slows enemies
     * within a radius while leaving the caster unaffected.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 20.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 180);
        int slowAmplifier = spellConfig.getInt("values.slow-amplifier", 5);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);

        // Play initial sound
        var world = player.getWorld();
        var playerLocation = player.getLocation();
        
        if (world != null && playerLocation != null) {
            world.playSound(playerLocation, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.3f);
        }

        // Start time dilation effect
        if (playerLocation != null) {
            BukkitRunnable timeDilationTask = new TimeDilationTask(context, playerLocation, radius, durationTicks, slowAmplifier, affectsPlayers);
            context.plugin().getTaskManager().runTaskTimer(timeDilationTask, 0L, 3L);
        }
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
     * A runnable that handles the time dilation effects over time.
     * <p>
     * This task manages the application of time dilation effects to entities and
     * creates visual particle effects for the distortion field.
     */
    private static class TimeDilationTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int durationTicks;
        private final int slowAmplifier;
        private final boolean affectsPlayers;
        private final World world;
        private int ticks = 0;
        private final int maxTicks;

        /**
         * Creates a new TimeDilationTask instance.
         *
         * @param context the spell context
         * @param center the center location of the time dilation effect
         * @param radius the radius of the time dilation effect
         * @param durationTicks the duration of the effect in ticks
         * @param slowAmplifier the amplifier for the slowness effect
         * @param affectsPlayers whether the effect affects players
         */
        public TimeDilationTask(@NotNull SpellContext context, @NotNull Location center, double radius, 
                               int durationTicks, int slowAmplifier, boolean affectsPlayers) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.slowAmplifier = slowAmplifier;
            this.affectsPlayers = affectsPlayers;
            this.world = center.getWorld();
            this.maxTicks = durationTicks / 3; // Convert to our tick interval
        }

        /**
         * Runs the time dilation task, applying effects to entities and creating
         * visual effects for the time distortion field.
         */
        @Override
        public void run() {
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= maxTicks) {
                this.cancel();
                // Play end sound
                world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 0.3f);
                return;
            }

            // Apply time dilation effects
            applyTimeDilationEffects();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        /**
         * Applies time dilation effects to entities within the radius.
         * <p>
         * This method applies extreme slowness, jump boost, and mining fatigue
         * potion effects to affected entities and creates periodic visual effects.
         */
        private void applyTimeDilationEffects() {
            if (world == null) {
                return;
            }
            
            Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(center, radius, radius, radius);
            
            for (LivingEntity entity : nearbyEntities) {
                // Skip the caster (they are unaffected)
                if (entity.equals(context.caster())) continue;
                
                // Skip players if not affecting players
                if (entity instanceof Player && !affectsPlayers) continue;
                
                // Skip dead or invalid entities
                if (entity.isDead() || !entity.isValid()) continue;

                // Apply extreme slowness
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, slowAmplifier, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10, 200, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 10, 5, false, false));
                
                // Visual effect for time-slowed entities
                if (ticks % 5 == 0) {
                    var entityLocation = entity.getLocation();
                    if (entityLocation != null) {
                        world.spawnParticle(Particle.CLOUD, entityLocation.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.01);
                        world.spawnParticle(Particle.PORTAL, entityLocation.add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);
                    }
                }
            }
        }

        /**
         * Creates visual effects for the time dilation field.
         * <p>
         * This method generates animated portal and cloud particles to visualize
         * the time distortion field and includes a central time vortex effect.
         */
        private void createVisualEffects() {
            if (world == null) {
                return;
            }
            
            // Create time distortion field
            double currentRadius = radius * (1.0 - (ticks / (double) maxTicks) * 0.5);
            
            // Outer ring
            for (int i = 0; i < 36; i++) {
                double angle = 2 * Math.PI * i / 36;
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Inner ring with faster rotation
            double innerRadius = currentRadius * 0.7;
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i / 24) + (ticks * 0.3);
                double x = innerRadius * Math.cos(angle);
                double z = innerRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Central time vortex
            if (ticks % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double angle = (2 * Math.PI * i / 10) + (ticks * 0.5);
                    double distance = Math.sin(ticks * 0.1) * 2;
                    double x = distance * Math.cos(angle);
                    double z = distance * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
            }
            
            // Time particles floating upward
            if (ticks % 4 == 0) {
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double distance = Math.random() * radius;
                    double x = center.getX() + Math.cos(angle) * distance;
                    double z = center.getZ() + Math.sin(angle) * distance;
                    Location particleLoc = new Location(world, x, center.getY() + Math.random() * 5, z);
                    world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, 0, 0, 0, 0);
                }
            }
        }
    }
}