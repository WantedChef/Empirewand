package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.Material;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Random;
import java.util.Objects;

/**
 * A devastating ice spell that creates a blizzard in a large area,
 * slowing and damaging all enemies while creating icy terrain.
 * <p>
 * This spell creates a powerful blizzard effect that covers a large area with snow
 * and ice particles, applies slowness effects to entities, and temporarily transforms
 * ground blocks into ice. The spell includes visual and audio feedback for the blizzard
 * effects and automatically cleans up the ice blocks when the spell ends.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Area of effect snow and ice particles</li>
 *   <li>Slowness potion effects on entities</li>
 *   <li>Periodic damage to affected entities</li>
 *   <li>Temporary ice block creation</li>
 *   <li>Automatic cleanup of ice blocks</li>
 *   <li>Wind effect pushing entities in random directions</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell blizzard = new Blizzard.Builder(api)
 *     .name("Blizzard")
 *     .description("Creates a devastating blizzard that slows and damages enemies while covering the area in ice.")
 *     .cooldown(Duration.ofSeconds(55))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class Blizzard extends Spell<Void> {

    /**
     * Configuration record containing all Blizzard spell settings.
     * <p>
     * This record holds all configuration values loaded from the spell configuration.
     * Using a record provides immutable configuration access and better performance
     * compared to repeated map lookups.
     *
     * @param radius            the radius of the blizzard effect
     * @param damage            the damage dealt to entities per tick
     * @param slowDuration      the duration of slowness effects in ticks
     * @param slowAmplifier     the amplifier level for slowness effects
     * @param durationTicks     the total duration of the blizzard in ticks
     * @param createIce         whether to create temporary ice blocks
     */
    private record Config(
        double radius,
        double damage,
        int slowDuration,
        int slowAmplifier,
        int durationTicks,
        boolean createIce
    ) {}

    /**
     * Default configuration values.
     */
    private static final double DEFAULT_RADIUS = 20.0;
    private static final double DEFAULT_DAMAGE = 1.5;
    private static final int DEFAULT_SLOW_DURATION = 60;
    private static final int DEFAULT_SLOW_AMPLIFIER = 3;
    private static final int DEFAULT_DURATION_TICKS = 150;
    private static final boolean DEFAULT_CREATE_ICE = true;

    /**
     * Current spell configuration.
     */
    private Config config = new Config(
        DEFAULT_RADIUS,
        DEFAULT_DAMAGE,
        DEFAULT_SLOW_DURATION,
        DEFAULT_SLOW_AMPLIFIER,
        DEFAULT_DURATION_TICKS,
        DEFAULT_CREATE_ICE
    );

    /**
     * Builder for creating Blizzard spell instances.
     * <p>
     * Provides a fluent API for configuring the blizzard spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new Blizzard spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@NotNull EmpireWandAPI api) {
            super(api);
            this.name = "Blizzard";
            this.description = "Creates a devastating blizzard that slows and damages enemies while covering the area in ice.";
            this.cooldown = Duration.ofSeconds(55);
            this.spellType = SpellType.ICE;
        }

        /**
         * Builds and returns a new Blizzard spell instance.
         *
         * @return the constructed Blizzard spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new Blizzard(this);
        }
    }

    /**
     * Constructs a new Blizzard spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private Blizzard(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Loads configuration values from the provided ReadableConfig.
     * <p>
     * This method is called during spell initialization to load spell-specific
     * configuration values from the configuration file. It creates an immutable
     * Config record for efficient access during spell execution.
     *
     * @param spellConfig the configuration section for this spell
     * @throws NullPointerException if spellConfig is null
     */
    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        Objects.requireNonNull(spellConfig, "SpellConfig cannot be null");
        super.loadConfig(spellConfig);
        
        this.config = new Config(
            spellConfig.getDouble("values.radius", DEFAULT_RADIUS),
            spellConfig.getDouble("values.damage", DEFAULT_DAMAGE),
            spellConfig.getInt("values.slow-duration-ticks", DEFAULT_SLOW_DURATION),
            spellConfig.getInt("values.slow-amplifier", DEFAULT_SLOW_AMPLIFIER),
            spellConfig.getInt("values.duration-ticks", DEFAULT_DURATION_TICKS),
            spellConfig.getBoolean("flags.create-ice", DEFAULT_CREATE_ICE)
        );
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "blizzard"
     */
    @Override
    @NotNull
    public String key() {
        return "blizzard";
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
     * Executes the blizzard spell logic.
     * <p>
     * This method creates a blizzard effect at the caster's location that applies
     * slowness and damage to nearby entities and creates temporary ice blocks.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        var world = player.getWorld();

        // Use pre-loaded configuration for better performance
        double radius = config.radius;
        double damage = config.damage;
        int slowDuration = config.slowDuration;
        int slowAmplifier = config.slowAmplifier;
        int durationTicks = config.durationTicks;
        boolean createIce = config.createIce;

        // Play initial sound
        if (world != null) {
            world.playSound(player.getLocation(), Sound.AMBIENT_UNDERWATER_LOOP, 2.0f, 0.3f);
        }

        // Start blizzard effect
        BukkitRunnable blizzardTask = new BlizzardTask(context, player.getLocation(), radius, damage, slowDuration, slowAmplifier, durationTicks, createIce);
        context.plugin().getTaskManager().runTaskTimer(blizzardTask, 0L, 3L);
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
     * A runnable that handles the blizzard's effects over time.
     * <p>
     * This task manages the blizzard's behavior including particle effects, entity
     * effects, ice block creation, and cleanup.
     */
    private static class BlizzardTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final double damage;
        private final int slowDuration;
        private final int slowAmplifier;
        private final int durationTicks;
        private final boolean createIce;
        private final World world;
        private final Random random = new Random();
        private final Set<Location> iceBlocks = new HashSet<>();
        private final Map<Block, BlockData> originalBlocks = new HashMap<>();
        private int ticks = 0;

        /**
         * Creates a new BlizzardTask instance.
         *
         * @param context the spell context
         * @param center the center location of the blizzard
         * @param radius the radius of the blizzard effect
         * @param damage the damage to apply to entities
         * @param slowDuration the duration of slowness effects in ticks
         * @param slowAmplifier the amplifier for slowness effects
         * @param durationTicks the duration of the blizzard in ticks
         * @param createIce whether to create ice blocks
         */
        public BlizzardTask(@NotNull SpellContext context, @NotNull Location center, double radius, double damage,
                           int slowDuration, int slowAmplifier, int durationTicks, boolean createIce) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.radius = radius;
            this.damage = damage;
            this.slowDuration = slowDuration;
            this.slowAmplifier = slowAmplifier;
            this.durationTicks = durationTicks;
            this.createIce = createIce;
            this.world = center.getWorld();
        }

        /**
         * Runs the blizzard task, creating visual effects and applying entity effects.
         */
        @Override
        public void run() {
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= durationTicks) {
                this.cancel();
                cleanupIce();
                return;
            }

            // Create blizzard effects
            createBlizzardEffects();

            // Apply effects to entities
            if (ticks % 10 == 0) {
                applyBlizzardEffects();
            }

            // Create ice blocks
            if (createIce && ticks % 15 == 0) {
                createIceBlocks();
            }

            ticks++;
        }

        /**
         * Creates the blizzard's visual particle effects.
         * <p>
         * This method spawns snow and cloud particles throughout the blizzard area
         * and periodically pushes entities with wind effects.
         */
        private void createBlizzardEffects() {
            if (world == null) {
                return;
            }
            
            // Create snow particles in a large area
            for (int i = 0; i < 30; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius;
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                double y = center.getY() + random.nextDouble() * 10;
                
                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.3, 0.3, 0.3, 0.01);
            }

            // Create wind effect by pushing entities
            if (ticks % 20 == 0) {
                for (LivingEntity entity : world.getNearbyLivingEntities(center, radius, 10, radius)) {
                    if (entity.equals(context.caster())) continue;
                    
                    // Push entity in random direction
                    Vector push = new Vector(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize().multiply(0.5);
                    entity.setVelocity(entity.getVelocity().add(push));
                }
            }
        }

        /**
         * Applies the blizzard's effects to nearby entities.
         * <p>
         * This method damages entities and applies slowness potion effects to them.
         *
         * @param context the spell context
         * @param result the result of the spell execution (always null for this spell)
         */
        private void applyBlizzardEffects() {
            if (world == null) {
                return;
            }
            
            for (LivingEntity entity : world.getNearbyLivingEntities(center, radius, 10, radius)) {
                if (entity.equals(context.caster())) continue;
                if (entity.isDead() || !entity.isValid()) continue;

                // Damage entity
                entity.damage(damage, context.caster());

                // Apply slowness
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, false));

                // Visual effects
                var entityLocation = entity.getLocation();
                if (entityLocation != null) {
                    world.spawnParticle(Particle.SNOWFLAKE, entityLocation.add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
                }
            }
        }

        /**
         * Creates temporary ice blocks in the blizzard area.
         * <p>
         * This method transforms ground blocks into ice blocks and stores their
         * original state for later cleanup.
         */
        private void createIceBlocks() {
            if (world == null) {
                return;
            }
            
            for (int i = 0; i < 15; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * (radius - 3);
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                
                Location groundLoc = new Location(world, x, center.getY(), z);
                groundLoc.setY(world.getHighestBlockYAt(groundLoc));
                
                Block block = groundLoc.getBlock();
                if (block.getType().isSolid() && !block.getType().isAir() && block.getType() != Material.ICE && 
                    block.getType() != Material.PACKED_ICE && block.getType() != Material.BEDROCK) {
                    
                    // Store original block
                    originalBlocks.put(block, block.getBlockData());
                    
                    // Turn to ice
                    block.setType(Material.ICE);
                    iceBlocks.add(block.getLocation());
                    
                    // Particle effect
                    world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 1, 0.5), 5, 0.2, 0.2, 0.2, 0.01, block.getBlockData());
                }
            }
        }

        /**
         * Cleans up the temporary ice blocks created by the blizzard.
         * <p>
         * This method restores the original block states and plays a cleanup sound.
         */
        private void cleanupIce() {
            if (world == null) {
                return;
            }
            
            // Restore original blocks
            for (Location loc : iceBlocks) {
                Block block = world.getBlockAt(loc);
                if (block.getType() == Material.ICE) {
                    BlockData original = originalBlocks.get(block);
                    if (original != null) {
                        block.setBlockData(original);
                    } else {
                        block.setType(Material.AIR);
                    }
                }
            }
            
            // Play cleanup sound
            world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
        }
    }
}