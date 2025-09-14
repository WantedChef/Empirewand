package nl.wantedchef.empirewand.spell.enhanced.cosmic;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A powerful spell that freezes time for all enemies in a large radius,
 * rendering them completely immobile for a duration.
 * <p>
 * This spell applies powerful slowness, jump boost, and blindness effects to
 * enemies within a radius to simulate temporal stasis. Affected entities become
 * completely immobile and unable to see clearly. The spell includes visual
 * effects with cloud and end rod particles and audio feedback for activation
 * and deactivation.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Area of effect temporal freezing</li>
 *   <li>Powerful slowness and jump boost effects</li>
 *   <li>Blindness effect on affected entities</li>
 *   <li>Animated particle visual effects</li>
 *   <li>Audio feedback for activation and deactivation</li>
 *   <li>Configurable radius and duration</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell temporalStasis = new TemporalStasis.Builder(api)
 *     .name("Temporal Stasis")
 *     .description("Freezes time for all enemies in a large radius, rendering them completely immobile.")
 *     .cooldown(Duration.ofSeconds(60))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class TemporalStasis extends Spell<Void> {

    /**
     * Builder for creating TemporalStasis spell instances.
     * <p>
     * Provides a fluent API for configuring the temporal stasis spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new TemporalStasis spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Temporal Stasis";
            this.description = "Freezes time for all enemies in a large radius, rendering them completely immobile.";
            this.cooldown = Duration.ofSeconds(60);
            this.spellType = SpellType.CONTROL;
        }

        /**
         * Builds and returns a new TemporalStasis spell instance.
         *
         * @return the constructed TemporalStasis spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new TemporalStasis(this);
        }
    }

    /**
     * Constructs a new TemporalStasis spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private TemporalStasis(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "temporal-stasis"
     */
    @Override
    @NotNull
    public String key() {
        return "temporal-stasis";
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
     * Executes the temporal stasis spell logic.
     * <p>
     * This method applies temporal freezing effects to entities within the radius
     * and creates visual and audio feedback for the spell activation.
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
        int durationTicks = spellConfig.getInt("values.duration-ticks", 100);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);

        // Find all entities in radius
        List<LivingEntity> affectedEntities = new ArrayList<>();
        var world = player.getWorld();
        var playerLocation = player.getLocation();
        
        if (world != null && playerLocation != null) {
            for (LivingEntity entity : world.getNearbyLivingEntities(playerLocation, radius, radius, radius)) {
                if (entity.equals(player)) continue;
                if (entity instanceof Player && !affectsPlayers) continue;
                if (entity.isDead() || !entity.isValid()) continue;
                affectedEntities.add(entity);
            }
        }

        // Apply temporal stasis effect
        for (LivingEntity entity : affectedEntities) {
            // Apply slowness and jump boost potions to simulate frozen movement
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 9, false, false));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 128, false, false));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, 1, false, false));
            
            // Visual effects
            var entityLocation = entity.getLocation();
            if (entityLocation != null) {
                context.fx().spawnParticles(entityLocation, Particle.CLOUD, 20, 0.5, 1, 0.5, 0.05);
                context.fx().spawnParticles(entityLocation, Particle.END_ROD, 5, 0.3, 1, 0.3, 0.01);
            }
        }

        // Visual effect for the caster
        if (playerLocation != null) {
            context.fx().spawnParticles(playerLocation, Particle.PORTAL, 50, 2, 2, 2, 0.1);
            context.fx().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);
        }

        // Create a visual representation of the time freeze
        if (playerLocation != null) {
            context.plugin().getTaskManager().runTaskTimer(
                new TimeFreezeVisual(context, playerLocation, radius, durationTicks),
                0L, 5L
            );
        }

        return null;
    }

    /**
     * Handles the spell effect after execution.
     * <p>
     * This spell's effects are handled in the executeSpell method.
     *
     * @param context the spell context
     * @param result the result of the spell execution (always null for this spell)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in executeSpell
    }

    /**
     * A runnable that handles the temporal stasis visual effects over time.
     * <p>
     * This task creates animated particle effects to visualize the time freeze
     * area and provides audio feedback for deactivation.
     */
    private static class TimeFreezeVisual extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int durationTicks;
        private int ticks = 0;
        private final int maxTicks;

        /**
         * Creates a new TimeFreezeVisual instance.
         *
         * @param context the spell context
         * @param center the center location of the time freeze effect
         * @param radius the radius of the time freeze effect
         * @param durationTicks the duration of the effect in ticks
         */
        public TimeFreezeVisual(@NotNull SpellContext context, @NotNull Location center, double radius, int durationTicks) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.maxTicks = durationTicks / 5; // Convert to our tick interval
        }

        /**
         * Runs the time freeze visual task, creating expanding rings and vertical
         * particle streams to visualize the temporal stasis effect.
         */
        @Override
        public void run() {
            var world = center.getWorld();
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= maxTicks) {
                this.cancel();
                context.fx().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                return;
            }

            // Create expanding rings of particles
            double currentRadius = radius * (ticks / (double) maxTicks);
            for (int i = 0; i < 36; i++) {
                double angle = 2 * Math.PI * i / 36;
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 1, z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }

            // Create vertical particle streams
            if (ticks % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = 2 * Math.PI * i / 8;
                    double x = (radius * 0.5) * Math.cos(angle);
                    double z = (radius * 0.5) * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0, z);
                    
                    for (int y = 0; y < 10; y++) {
                        Location streamLoc = particleLoc.clone().add(0, y * 0.5, 0);
                        world.spawnParticle(Particle.CLOUD, streamLoc, 1, 0, 0, 0, 0);
                    }
                }
            }

            ticks++;
        }
    }
}