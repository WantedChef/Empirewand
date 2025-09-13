package nl.wantedchef.empirewand.spell.enhanced;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

import java.time.Duration;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

import java.time.Duration;

/**
 * A defensive spell that creates a stone fortress around the caster, providing protection and
 * damaging nearby enemies.
 * <p>
 * This spell constructs a stone fortress around the caster with walls and an optional roof.
 * The fortress is built progressively over time and damages nearby enemies when complete.
 * Visual effects include block particles during construction and periodic fortress effects.
 * The spell automatically cleans up the fortress blocks when it expires.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Progressive stone wall construction</li>
 *   <li>Optional roof for complete enclosure</li>
 *   <li>Area of effect damage to nearby enemies</li>
 *   <li>Knockback effects pushing enemies away</li>
 *   <li>Block particle visual effects</li>
 *   <li>Automatic cleanup of placed blocks</li>
 *   <li>Audio feedback for construction and destruction</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell stoneFortress = new StoneFortress.Builder(api)
 *     .name("Stone Fortress")
 *     .description("Creates a protective stone fortress around you that damages nearby enemies.")
 *     .cooldown(Duration.ofSeconds(40))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class StoneFortress extends Spell<Void> {

    /**
     * Builder for creating StoneFortress spell instances.
     * <p>
     * Provides a fluent API for configuring the stone fortress spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new StoneFortress spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Stone Fortress";
            this.description =
                    "Creates a protective stone fortress around you that damages nearby enemies.";
            this.cooldown = Duration.ofSeconds(40);
            this.spellType = SpellType.EARTH;
        }

        /**
         * Builds and returns a new StoneFortress spell instance.
         *
         * @return the constructed StoneFortress spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new StoneFortress(this);
        }
    }

    /**
     * Constructs a new StoneFortress spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private StoneFortress(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "stone-fortress"
     */
    @Override
    @NotNull
    public String key() {
        return "stone-fortress";
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
     * Executes the stone fortress spell logic.
     * <p>
     * This method creates a stone fortress structure around the caster that is built
     * progressively and damages nearby enemies.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        // Configuration
        int wallRadius = spellConfig.getInt("values.wall-radius", 8);
        int wallHeight = spellConfig.getInt("values.wall-height", 6);
        double damage = spellConfig.getDouble("values.damage", 8.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 300);
        boolean includeRoof = spellConfig.getBoolean("flags.include-roof", true);

        // Create fortress structure
        context.plugin().getTaskManager().runTaskTimer(
            new FortressTask(context, player.getLocation(), wallRadius, wallHeight, damage,
                    durationTicks, includeRoof),
            0L, 2L
        );
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
     * A runnable that handles the stone fortress construction and effects over time.
     * <p>
     * This task manages the progressive building of the fortress, periodic damage to
     * nearby enemies, and cleanup of the fortress blocks.
     */
    private static class FortressTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final int wallRadius;
        private final int wallHeight;
        private final double damage;
        private final int durationTicks;
        private final boolean includeRoof;
        private final World world;
        private final List<BlockRecord> placedBlocks = new ArrayList<>();
        private final Map<Block, BlockData> originalBlocks = new HashMap<>();
        private int ticks = 0;
        private final int buildDuration = 40; // 2 seconds to build
        private boolean isBuilt = false;

        /**
         * Creates a new FortressTask instance.
         *
         * @param context the spell context
         * @param center the center location for the fortress
         * @param wallRadius the radius of the fortress walls
         * @param wallHeight the height of the fortress walls
         * @param damage the damage to apply to nearby enemies
         * @param durationTicks the duration of the fortress in ticks
         * @param includeRoof whether to include a roof in the fortress
         */
        public FortressTask(@NotNull SpellContext context, @NotNull Location center, int wallRadius, int wallHeight,
                double damage, int durationTicks, boolean includeRoof) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.wallRadius = wallRadius;
            this.wallHeight = wallHeight;
            this.damage = damage;
            this.durationTicks = durationTicks;
            this.includeRoof = includeRoof;
            this.world = center.getWorld();
        }

        /**
         * Runs the fortress task, building the structure progressively and applying
         * periodic effects while it exists.
         */
        @Override
        public void run() {
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= durationTicks + buildDuration) {
                this.cancel();
                removeFortress();
                return;
            }

            if (!isBuilt && ticks >= buildDuration) {
                isBuilt = true;
                // Damage nearby enemies when fortress is complete
                damageNearbyEnemies();
            }

            if (!isBuilt) {
                buildFortressProgressively();
            }

            // Periodic effects while fortress exists
            if (isBuilt && ticks % 20 == 0) {
                // Enhanced particle effects around walls
                for (int i = 0; i < 36; i++) {
                    double angle = 2 * Math.PI * i / 36;
                    double x = wallRadius * Math.cos(angle);
                    double z = wallRadius * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 1, z);
                    world.spawnParticle(Particle.BLOCK, particleLoc, 3, 0.1, 0.1, 0.1, 0.01,
                            Material.STONE.createBlockData());
                    // Add CRIT particles for more impressive effect
                    world.spawnParticle(Particle.CRIT, particleLoc, 2, 0.2, 0.2, 0.2, 0.1);
                }

                // Enchantment table particles inside fortress for magical atmosphere
                for (int i = 0; i < 8; i++) {
                    double angle = 2 * Math.PI * Math.random();
                    double distance = wallRadius * 0.7 * Math.random();
                    double x = distance * Math.cos(angle);
                    double z = distance * Math.sin(angle);
                    Location magicLoc = center.clone().add(x, 1.5, z);
                    world.spawnParticle(Particle.ENCHANT, magicLoc, 1, 0.1, 0.1, 0.1, 0.02);
                }

                // Occasional damage
                if (ticks % 40 == 0) {
                    damageNearbyEnemies();
                }
            }

            ticks++;
        }

        /**
         * Builds the fortress structure progressively over time.
         * <p>
         * This method places stone blocks to form the fortress walls and roof based
         * on the current build progress.
         */
        private void buildFortressProgressively() {
            int buildProgress = Math.min(ticks, buildDuration);
            int totalBlocks = (wallRadius * 2 + 1) * wallHeight * 4; // 4 walls
            if (includeRoof) {
                totalBlocks += (wallRadius * 2 + 1) * (wallRadius * 2 + 1); // roof
            }

            int blocksToPlace = (int) (totalBlocks * ((double) buildProgress / buildDuration));

            // Place wall blocks progressively
            int blocksPlaced = 0;

            // Build four walls
            for (int layer = 0; layer < wallHeight && blocksPlaced < blocksToPlace; layer++) {
                for (int x = -wallRadius; x <= wallRadius && blocksPlaced < blocksToPlace; x++) {
                    // Front and back walls (z = -radius and z = radius)
                    placeBlockIfAir(center.getBlockX() + x, center.getBlockY() + layer,
                            center.getBlockZ() - wallRadius);
                    blocksPlaced++;
                    if (blocksPlaced >= blocksToPlace)
                        break;

                    placeBlockIfAir(center.getBlockX() + x, center.getBlockY() + layer,
                            center.getBlockZ() + wallRadius);
                    blocksPlaced++;
                    if (blocksPlaced >= blocksToPlace)
                        break;
                }

                for (int z = -wallRadius + 1; z < wallRadius && blocksPlaced < blocksToPlace; z++) {
                    // Left and right walls (x = -radius and x = radius)
                    placeBlockIfAir(center.getBlockX() - wallRadius, center.getBlockY() + layer,
                            center.getBlockZ() + z);
                    blocksPlaced++;
                    if (blocksPlaced >= blocksToPlace)
                        break;

                    placeBlockIfAir(center.getBlockX() + wallRadius, center.getBlockY() + layer,
                            center.getBlockZ() + z);
                    blocksPlaced++;
                    if (blocksPlaced >= blocksToPlace)
                        break;
                }
            }

            // Build roof if enabled
            if (includeRoof && blocksPlaced < blocksToPlace) {
                int roofLevel = center.getBlockY() + wallHeight;
                for (int x = -wallRadius; x <= wallRadius && blocksPlaced < blocksToPlace; x++) {
                    for (int z = -wallRadius; z <= wallRadius
                            && blocksPlaced < blocksToPlace; z++) {
                        placeBlockIfAir(center.getBlockX() + x, roofLevel, center.getBlockZ() + z);
                        blocksPlaced++;
                    }
                }
            }
        }

        /**
         * Places a stone block at the specified coordinates if the location is air
         * or liquid.
         * <p>
         * This method stores the original block data for later restoration and
         * creates particle effects for visual feedback.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         * @param z the z coordinate
         */
        private void placeBlockIfAir(int x, int y, int z) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isAir() || block.getType() == Material.WATER
                    || block.getType() == Material.LAVA) {
                // Store original block data
                originalBlocks.put(block, block.getBlockData());

                // Place stone block
                block.setType(Material.STONE);
                placedBlocks.add(new BlockRecord(block.getLocation(), Material.STONE));

                // Particle effect
                world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.1,
                        0.1, 0.1, 0.01, Material.STONE.createBlockData());
            }
        }

        /**
         * Damages nearby enemies and applies knockback effects.
         * <p>
         * This method applies damage to all living entities within the fortress
         * radius and pushes them away from the center with knockback velocity.
         */
        private void damageNearbyEnemies() {
            if (world == null) {
                return;
            }
            
            for (LivingEntity entity : world.getNearbyLivingEntities(center, wallRadius + 2,
                    wallHeight + 2, wallRadius + 2)) {
                if (entity instanceof Player && entity.equals(context.caster()))
                    continue;
                if (entity.isDead() || !entity.isValid())
                    continue;

                entity.damage(damage, context.caster());

                // Knockback away from center
                Vector knockback = entity.getLocation().toVector().subtract(center.toVector())
                        .normalize().multiply(0.8);
                knockback.setY(0.3);
                entity.setVelocity(knockback);

                // Particle effect
                world.spawnParticle(Particle.BLOCK, entity.getLocation(), 10, 0.3, 0.3, 0.3, 0.01,
                        Material.STONE.createBlockData());
            }

            // Sound effect
            world.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
        }

        /**
         * Removes the fortress structure and restores original blocks.
         * <p>
         * This method removes all placed stone blocks and restores the original
         * block data, creating particle effects for visual feedback.
         */
        private void removeFortress() {
            if (world == null) {
                return;
            }
            
            // Remove placed blocks and restore original blocks
            for (BlockRecord record : placedBlocks) {
                Block block = world.getBlockAt(record.location);
                if (block.getType() == record.material) {
                    BlockData original = originalBlocks.get(block);
                    if (original != null) {
                        block.setBlockData(original);
                    } else {
                        block.setType(Material.AIR);
                    }

                    // Particle effect
                    world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 5,
                            0.1, 0.1, 0.1, 0.01, record.material.createBlockData());
                }
            }

            // Sound effect
            world.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
        }

        /**
         * A record class for tracking placed fortress blocks.
         * <p>
         * This class holds information about the location and material of placed blocks
         * for later cleanup and restoration.
         */
        private static class BlockRecord {
            final Location location;
            final Material material;

            /**
             * Creates a new BlockRecord instance.
             *
             * @param location the location of the block
             * @param material the material of the block
             */
            BlockRecord(@NotNull Location location, @NotNull Material material) {
                this.location = Objects.requireNonNull(location, "Location cannot be null");
                this.material = Objects.requireNonNull(material, "Material cannot be null");
            }
        }
    }
}
