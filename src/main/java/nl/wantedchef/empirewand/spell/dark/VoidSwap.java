package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A dark spell that swaps positions between the caster and a target entity.
 * <p>
 * This spell allows the caster to instantly swap locations with a targeted entity,
 * making it useful for repositioning in combat or escaping dangerous situations.
 * The spell includes safety checks to prevent swapping into unsafe locations.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Position swapping between caster and target</li>
 *   <li>Safety checks to prevent swapping into solid blocks</li>
 *   <li>Particle effects at both locations during swap</li>
 *   <li>Audio feedback with enderman teleport sounds</li>
 *   <li>Vehicle and passenger state validation</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell voidSwap = new VoidSwap.Builder(api)
 *     .name("Void Swap")
 *     .description("Swap positions with a target.")
 *     .cooldown(Duration.ofSeconds(25))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class VoidSwap extends Spell<Void> {

    /**
     * Builder for creating VoidSwap spell instances.
     * <p>
     * Provides a fluent API for configuring the void swap spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new VoidSwap spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@NotNull EmpireWandAPI api) {
            super(api);
            this.name = "Void Swap";
            this.description = "Swap positions with a target.";
            this.cooldown = Duration.ofSeconds(25);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds and returns a new VoidSwap spell instance.
         *
         * @return the constructed VoidSwap spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new VoidSwap(this);
        }
    }

    /**
     * Constructs a new VoidSwap spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private VoidSwap(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "void-swap"
     */
    @Override
    @NotNull
    public String key() {
        return "void-swap";
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
     * Executes the void swap spell logic.
     * <p>
     * This method swaps the positions of the caster and a targeted entity, with
     * safety checks to prevent swapping into solid blocks or while in vehicles.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 15.0);

        var looked = player.getTargetEntity((int) range);
        if (!(looked instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return null;
        }

        if (player.isInsideVehicle() || target.isInsideVehicle() || !player.getPassengers().isEmpty()
                || !target.getPassengers().isEmpty()) {
            context.fx().fizzle(player);
            return null;
        }

        Location a = player.getLocation();
        Location b = target.getLocation();
        if (!isLocationSafe(a) || !isLocationSafe(b)) {
            context.fx().fizzle(player);
            return null;
        }

        context.fx().spawnParticles(a, Particle.PORTAL, 25, 0.4, 0.8, 0.4, 0.05);
        context.fx().spawnParticles(b, Particle.PORTAL, 25, 0.4, 0.8, 0.4, 0.05);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        player.teleport(b);
        target.teleport(a);
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

    /**
     * Checks if a location is safe for teleportation.
     * <p>
     * This method verifies that the target location has:
     * <ul>
 *   <li>Non-solid block at feet level</li>
 *   <li>Non-solid block at head level</li>
 *   <li>Solid block at ground level</li>
     * </ul>
     *
     * @param location the location to check
     * @return true if the location is safe, false otherwise
     */
    private boolean isLocationSafe(@NotNull Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().add(0, -1, 0).getBlock();
        return !feet.getType().isSolid() && !head.getType().isSolid() && ground.getType().isSolid();
    }
}
