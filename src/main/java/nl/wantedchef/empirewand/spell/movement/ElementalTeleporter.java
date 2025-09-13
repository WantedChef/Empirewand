package nl.wantedchef.empirewand.spell.movement;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A teleportation spell that safely teleports the player to their target location.
 * <p>
 * This spell uses modern ray tracing to find the target block and teleports the player
 * to a safe location above it, with comprehensive safety checks to prevent teleporting
 * into solid blocks or dangerous locations.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Modern ray tracing for accurate target selection</li>
 *   <li>Comprehensive safety checks for teleportation</li>
 *   <li>Visual and audio feedback for teleportation</li>
 *   <li>Portal particle effects at origin and destination</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ElementalTeleporter extends Spell<Void> {

    /**
     * Configuration record for the ElementalTeleporter spell.
     */
    private record Config(
        double maxRange
    ) {}

    private static final double DEFAULT_MAX_RANGE = 25.0;

    private Config config = new Config(DEFAULT_MAX_RANGE);

    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new ElementalTeleporter spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Elemental Teleporter";
            this.description = "Safely teleports to the target location using ray tracing.";
            this.cooldown = Duration.ofSeconds(3);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ElementalTeleporter(this);
        }
    }

    private ElementalTeleporter(Builder builder) {
        super(builder);
    }

    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        Objects.requireNonNull(spellConfig, "SpellConfig cannot be null");
        super.loadConfig(spellConfig);
        
        this.config = new Config(
            spellConfig.getDouble("values.max-range", DEFAULT_MAX_RANGE)
        );
    }

    @Override
    @NotNull
    public String key() {
        return "elemental-teleporter";
    }

    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    @Nullable
    protected Void executeSpell(SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        Location originalLoc = player.getLocation();
        
        RayTraceResult result = player.rayTraceBlocks(config.maxRange, FluidCollisionMode.NEVER);
        if (result == null || result.getHitBlock() == null) {
            player.sendMessage("No valid teleport target within range.");
            context.fx().fizzle(player);
            return null;
        }
        
        Location destination = result.getHitBlock().getLocation().add(0.5, 1, 0.5);
        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());
        
        if (!isSafeLocation(destination)) {
            player.sendMessage("Cannot teleport to that location - it's not safe!");
            context.fx().fizzle(player);
            return null;
        }
        
        context.fx().spawnParticles(originalLoc, Particle.PORTAL, 50, 1, 1, 1, 0.1);
        context.fx().playSound(originalLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        boolean success = player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        
        if (success) {
            context.fx().spawnParticles(destination, Particle.PORTAL, 50, 1, 1, 1, 0.1);
            context.fx().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        } else {
            player.sendMessage("Teleportation failed!");
            context.fx().fizzle(player);
        }
        
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    /**
     * Checks if a location is safe for teleportation.
     * <p>
     * This method verifies that the feet and head positions are not solid blocks
     * and that the destination is within world boundaries.
     *
     * @param destination the location to check
     * @return true if the location is safe for teleportation
     */
    private boolean isSafeLocation(Location destination) {
        var world = destination.getWorld();
        if (world == null) {
            return false;
        }
        
        Block feetBlock = destination.getBlock();
        Block headBlock = destination.clone().add(0, 1, 0).getBlock();
        
        if (feetBlock.getType().isSolid() || headBlock.getType().isSolid()) {
            return false;
        }
        
        if (feetBlock.getType() == Material.LAVA || headBlock.getType() == Material.LAVA) {
            return false;
        }
        
        if (feetBlock.getType() == Material.FIRE || headBlock.getType() == Material.FIRE) {
            return false;
        }
        
        double y = destination.getY();
        return y >= world.getMinHeight() && y <= world.getMaxHeight();
    }
}