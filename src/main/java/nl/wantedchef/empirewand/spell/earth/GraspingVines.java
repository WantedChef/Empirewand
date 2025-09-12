package nl.wantedchef.empirewand.spell.earth;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.EffectService;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * An earth spell that roots a target briefly with conjured vines.
 * <p>
 * This spell creates magical vines that grasp and slow down a targeted entity,
 * effectively rooting them in place for a short duration. The spell provides
 * visual and audio feedback to indicate the vines taking hold of the target.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Roots target entities with slowness effect</li>
 *   <li>Visual spore blossom particles at target location</li>
 *   <li>Audio feedback with vine step sound</li>
 *   <li>Configurable duration and amplifier values</li>
 *   <li>Target filtering for players and mobs</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell graspingVines = new GraspingVines.Builder(api)
 *     .name("Grasping Vines")
 *     .description("Roots a target briefly with conjured vines.")
 *     .cooldown(Duration.ofSeconds(12))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class GraspingVines extends Spell<Void> {

    /**
     * Builder for creating GraspingVines spell instances.
     * <p>
     * Provides a fluent API for configuring the grasping vines spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new GraspingVines spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@NotNull EmpireWandAPI api) {
            super(api);
            this.name = "Grasping Vines";
            this.description = "Roots a target briefly with conjured vines.";
            this.cooldown = Duration.ofSeconds(12);
            this.spellType = SpellType.EARTH;
        }

        /**
         * Builds and returns a new GraspingVines spell instance.
         *
         * @return the constructed GraspingVines spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new GraspingVines(this);
        }
    }

    /**
     * Constructs a new GraspingVines spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private GraspingVines(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "grasping-vines"
     */
    @Override
    @NotNull
    public String key() {
        return "grasping-vines";
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
     * Executes the grasping vines spell logic.
     * <p>
     * This method applies a slowness potion effect to the targeted entity and
     * creates visual and audio feedback to indicate the vines taking hold.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        Entity targetEntity = player.getTargetEntity(10);
        if (!(targetEntity instanceof LivingEntity target)) {
            EmpireWandAPI.getService(EffectService.class).fizzle(player.getLocation());
            return null;
        }

        boolean hitPlayers = this.spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = this.spellConfig.getBoolean("flags.hit-mobs", true);
        if ((target instanceof Player && !hitPlayers) || (!(target instanceof Player) && !hitMobs)) {
            EmpireWandAPI.getService(EffectService.class).fizzle(player.getLocation());
            return null;
        }

        int duration = this.spellConfig.getInt("values.duration-ticks", 60);
        int amplifier = this.spellConfig.getInt("values.slow-amplifier", 250);
        
        // Apply slowness effect to target
        if (target.isValid() && !target.isDead()) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amplifier));
        }

        EmpireWandAPI.getService(EffectService.class).spawnParticles(
                target.getLocation().add(0, 0.2, 0), Particle.SPORE_BLOSSOM_AIR, 18, 0.6, 0.4, 0.6, 0.0);
        EmpireWandAPI.getService(EffectService.class).playSound(target.getLocation(),
                Sound.BLOCK_VINE_STEP, 0.8f, 0.9f);
        return null;
    }

    /**
     * Handles the spell effect after execution.
     * <p>
     * This spell has instant effects that are applied during execution.
     *
     * @param context the spell context
     * @param result the result of the spell execution (always null for this spell)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
