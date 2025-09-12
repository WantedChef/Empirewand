package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.common.visual.Afterimages;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A dark spell that allows short-range teleportation with afterimage effects.
 * <p>
 * This spell enables the caster to quickly teleport a short distance forward,
 * leaving behind visual afterimages. It's useful for evading attacks or repositioning
 * in combat. The spell includes safety checks to prevent teleporting into solid blocks.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Short-range teleportation (blink)</li>
 *   <li>Visual afterimage effects</li>
 *   <li>Safety checks to prevent clipping into blocks</li>
 *   <li>Particle effects at departure and arrival locations</li>
 *   <li>Audio feedback with enderman teleport sounds</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell shadowStep = new ShadowStep.Builder(api)
 *     .name("Shadow Step")
 *     .description("Short-range blink with afterimages.")
 *     .cooldown(Duration.ofSeconds(12))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class ShadowStep extends Spell<Void> {

    /**
     * Builder for creating ShadowStep spell instances.
     * <p>
     * Provides a fluent API for configuring the shadow step spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new ShadowStep spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Shadow Step";
            this.description = "Short-range blink with afterimages.";
            this.cooldown = Duration.ofSeconds(12);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds and returns a new ShadowStep spell instance.
         *
         * @return the constructed ShadowStep spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new ShadowStep(this);
        }
    }

    /**
     * Constructs a new ShadowStep spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private ShadowStep(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "shadow-step"
     */
    @Override
    @NotNull
    public String key() {
        return "shadow-step";
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
     * Executes the shadow step spell logic.
     * <p>
     * This method teleports the player forward a specified distance, creating afterimage
     * effects and particle feedback at both departure and arrival locations.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        double range = spellConfig.getDouble("values.range", 10.0);
        int echoSamples = spellConfig.getInt("visual.echo-samples", 6);

        Location target = getTargetLocation(player, range);
        if (!isLocationSafe(target)) {
            context.fx().fizzle(player);
            return null;
        }

        Location from = player.getLocation().clone();
        for (int i = 0; i < echoSamples; i++) {
            double t = (i + 1) / (double) (echoSamples + 1);
            Afterimages.record(lerp(from, target, t));
        }
        Afterimages.record(from);

        context.fx().spawnParticles(from, Particle.CLOUD, 25, 0.4, 0.6, 0.4, 0.02);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.4f);

        player.teleport(target);

        context.fx().spawnParticles(target, Particle.CLOUD, 30, 0.5, 0.8, 0.5, 0.05);
        context.fx().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.7f);

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
    protected void handleEffect(@NotNull SpellContext context, @Nullable Void result) {
        // Instant effect.
    }

    /**
     * Linearly interpolates between two locations.
     * <p>
     * This method calculates intermediate positions between the start and end locations
     * for creating afterimage effects.
     *
     * @param a the starting location
     * @param b the ending location
     * @param t the interpolation factor (0.0 to 1.0)
     * @return the interpolated location
     */
    private @NotNull Location lerp(@NotNull Location a, @NotNull Location b, double t) {
        Objects.requireNonNull(a, "Location a cannot be null");
        Objects.requireNonNull(b, "Location b cannot be null");
        
        return new Location(a.getWorld(),
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t,
                a.getZ() + (b.getZ() - a.getZ()) * t,
                a.getYaw() + (b.getYaw() - a.getYaw()) * (float) t,
                a.getPitch() + (b.getPitch() - a.getPitch()) * (float) t);
    }

    /**
     * Gets the target location for teleportation.
     * <p>
     * This method calculates the destination location, taking into account solid blocks
     * that would prevent safe teleportation.
     *
     * @param player the player casting the spell
     * @param range the maximum range for teleportation
     * @return the target location for teleportation
     */
    private @NotNull Location getTargetLocation(@NotNull Player player, double range) {
        Objects.requireNonNull(player, "Player cannot be null");
        
        BlockIterator iterator = new BlockIterator(player, (int) range);
        Location targetLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(range));
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getType().isSolid()) {
                targetLoc = block.getLocation();
                targetLoc.setY(targetLoc.getY() + 1);
                break;
            }
        }
        
        var world = player.getWorld();
        if (world != null) {
            targetLoc.setY(Math.max(world.getMinHeight(), Math.min(world.getMaxHeight(), targetLoc.getY())));
        }
        return targetLoc;
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
        
        var feet = location.getBlock();
        var head = location.clone().add(0, 1, 0).getBlock();
        var ground = location.clone().add(0, -1, 0).getBlock();
        return !feet.getType().isSolid() && !head.getType().isSolid() && ground.getType().isSolid();
    }
}
