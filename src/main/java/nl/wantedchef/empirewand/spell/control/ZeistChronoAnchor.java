package nl.wantedchef.empirewand.spell.control;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * A control spell that creates a temporal distortion field.
 * <p>
 * This spell creates a localized time bubble that significantly slows down
 * entities and projectiles within its radius, providing tactical advantages
 * in combat situations. The effect creates visual time distortion effects.
 * <p>
 * <strong>Effects:</strong>
 * <ul>
 *   <li>Slows entities within 5-block radius</li>
 *   <li>Reduces movement speed by 50%</li>
 *   <li>Creates visual time distortion particles</li>
 *   <li>Lasts for 8 seconds</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell chronoAnchor = new ZeistChronoAnchor.Builder(api)
 *     .name("Zeist Chrono Anchor")
 *     .description("Creates a time distortion field")
 *     .cooldown(Duration.ofSeconds(20))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class ZeistChronoAnchor extends Spell<Void> {

    /** Default radius of the time bubble */
    private static final double EFFECT_RADIUS = 5.0;
    
    /** Default duration of the time bubble */
    private static final Duration EFFECT_DURATION = Duration.ofSeconds(8);
    
    /** Slowness effect amplifier */
    private static final int SLOWNESS_AMPLIFIER = 2;

    /**
     * Builder for creating ZeistChronoAnchor spell instances.
     * <p>
     * Provides a fluent API for configuring the chrono anchor spell.
     */
    public static class Builder extends Spell.Builder<Void> {
        
        /**
         * Creates a new ZeistChronoAnchor spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@NotNull EmpireWandAPI api) {
            super(api);
            this.name = "Zeist Chrono Anchor";
            this.description = "Creates a time bubble that slows entities and projectiles within its radius.";
            this.cooldown = Duration.ofSeconds(20);
            this.spellType = SpellType.CONTROL;
        }

        /**
         * Builds and returns a new ZeistChronoAnchor spell instance.
         *
         * @return the constructed ZeistChronoAnchor spell
         */
        @Override
        @NotNull
        public ZeistChronoAnchor build() {
            return new ZeistChronoAnchor(this);
        }
    }

    /**
     * Constructs a new ZeistChronoAnchor spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private ZeistChronoAnchor(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "zeist-chrono-anchor"
     */
    @Override
    @NotNull
    public String key() {
        return "zeist-chrono-anchor";
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
     * Executes the chrono anchor spell logic.
     * <p>
     * This method creates a time distortion field at the target location,
     * applying slowness effects to all entities within the radius.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Location targetLocation = context.hasTargetLocation() 
            ? context.targetLocation() 
            : context.caster().getLocation();

        createTimeBubble(context, targetLocation);
        
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are applied in executeSpell/createTimeBubble
    }

    /**
     * Creates a time bubble at the specified location.
     * <p>
     * Applies slowness effects to all entities within the radius and creates
     * visual time distortion effects.
     *
     * @param context the spell context
     * @param location the center of the time bubble
     */
    private void createTimeBubble(@NotNull SpellContext context, @NotNull Location location) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");
        
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        // Get all entities within radius
        List<Entity> nearbyEntities = world.getNearbyEntities(location, EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS)
            .stream()
            .filter(entity -> entity instanceof LivingEntity && entity != context.caster())
            .toList();

        // Apply slowness effects
        for (Entity entity : nearbyEntities) {
            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                (int) (EFFECT_DURATION.getSeconds() * 20), // Convert to ticks
                SLOWNESS_AMPLIFIER,
                false, // Not ambient
                true,  // Show particles
                true   // Show icon
            ));
        }

        // Play creation effects
        playTimeBubbleEffects(context, location);
    }

    /**
     * Plays visual and audio effects for the time bubble creation.
     *
     * @param context the spell context
     * @param location the center of the time bubble
     */
    private void playTimeBubbleEffects(@NotNull SpellContext context, @NotNull Location location) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");
        
        // Play sound effect
        location.getWorld().playSound(
            location,
            Sound.BLOCK_BEACON_ACTIVATE,
            1.0f, // Volume
            0.5f  // Pitch (lower for time distortion)
        );

        // Create particle ring effect
        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            double x = Math.cos(angle) * EFFECT_RADIUS;
            double z = Math.sin(angle) * EFFECT_RADIUS;
            
            Location particleLocation = location.clone().add(x, 0, z);
            location.getWorld().spawnParticle(
                Particle.CRIT,
                particleLocation,
                5,   // Count
                0.1, // Offset X
                0.1, // Offset Y
                0.1, // Offset Z
                0.05 // Speed
            );
        }
    }
}
