package nl.wantedchef.empirewand.spell.control;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A control spell that confuses and slows a target entity.
 * <p>
 * This spell applies confusion and slowness effects to a targeted living entity,
 * making them less effective in combat by disrupting their movement and actions.
 * The spell requires line of sight to the target and has a moderate cooldown.
 * <p>
 * <strong>Effects:</strong>
 * <ul>
 *   <li>Confusion (Nausea): 10 seconds</li>
 *   <li>Slowness: 8 seconds</li>
 *   <li>Visual purple particle effects</li>
 *   <li>Sound feedback on cast</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell confuse = new Confuse.Builder(api)
 *     .name("Confuse")
 *     .description("Confuses and slows a target entity")
 *     .cooldown(Duration.ofSeconds(15))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class Confuse extends Spell<Void> {

    /** Default duration for confusion effect */
    private static final Duration CONFUSION_DURATION = Duration.ofSeconds(10);
    
    /** Default duration for slowness effect */
    private static final Duration SLOWNESS_DURATION = Duration.ofSeconds(8);
    
    /** Default effect amplifier for slowness */
    private static final int SLOWNESS_AMPLIFIER = 1;

    /**
     * Builder for creating Confuse spell instances.
     * <p>
     * Provides a fluent API for configuring the confuse spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        
        /**
         * Creates a new Confuse spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Confuse";
            this.description = "Confuses and slows a target entity, disrupting their movement and actions.";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.CONTROL;
        }

        /**
         * Builds and returns a new Confuse spell instance.
         *
         * @return the constructed Confuse spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new Confuse(this);
        }
    }

    /**
     * Constructs a new Confuse spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private Confuse(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "confuse"
     */
    @Override
    @NotNull
    public String key() {
        return "confuse";
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
     * Executes the confuse spell logic.
     * <p>
     * This method applies confusion and slowness effects to the target entity.
     * If no target is specified, the spell will attempt to target the entity
     * the caster is looking at within a reasonable range.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player caster = context.caster();
        LivingEntity target = context.target();
        
        // If no explicit target, try to get the entity the caster is looking at
        if (target == null) {
            target = getTargetedEntity(caster);
        }
        
        if (target == null || !target.isValid()) {
            return null;
        }

        applyConfuseEffects(context, target);
        playConfuseEffects(context, target);
        
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are applied synchronously during executeSpell
    }

    /**
     * Applies confusion and slowness effects to the target entity.
     *
     * @param context the spell context
     * @param target the entity to confuse
     */
    private void applyConfuseEffects(@NotNull SpellContext context, @NotNull LivingEntity target) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(target, "Target cannot be null");
        
        // Apply confusion (nausea) effect
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.NAUSEA,
            (int) (CONFUSION_DURATION.getSeconds() * 20), // Convert to ticks
            0, // Amplifier 0 (base level)
            false, // Not ambient
            true, // Show particles
            true  // Show icon
        ));

        // Apply slowness effect
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            (int) (SLOWNESS_DURATION.getSeconds() * 20), // Convert to ticks
            SLOWNESS_AMPLIFIER,
            false, // Not ambient
            true,  // Show particles
            true   // Show icon
        ));
    }

    /**
     * Plays visual and audio effects for the confuse spell.
     *
     * @param context the spell context
     * @param target the confused entity
     */
    private void playConfuseEffects(@NotNull SpellContext context, @NotNull LivingEntity target) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(target, "Target cannot be null");
        
        // Play sound effect
        target.getWorld().playSound(
            target.getLocation(),
            Sound.ENTITY_EVOKER_CAST_SPELL,
            1.0f, // Volume
            0.8f  // Pitch (lower for confusion effect)
        );

        // Play particle effect
        target.getWorld().spawnParticle(
            Particle.WITCH,
            target.getLocation().add(0, 1, 0), // Above entity
            20,  // Count
            0.5, // Offset X
            0.5, // Offset Y
            0.5, // Offset Z
            0.1  // Speed
        );
    }

    /**
     * Gets the entity that the caster is currently targeting.
     * <p>
     * Uses ray tracing to find the nearest living entity within range.
     *
     * @param caster the player casting the spell
     * @return the targeted entity, or null if no valid target found
     */
    @Nullable
    private LivingEntity getTargetedEntity(@NotNull Player caster) {
        Objects.requireNonNull(caster, "Caster cannot be null");
        
        // Ray trace for entities within 20 blocks
        var target = caster.getTargetEntity(20);
        return target instanceof LivingEntity ? (LivingEntity) target : null;
    }
}
