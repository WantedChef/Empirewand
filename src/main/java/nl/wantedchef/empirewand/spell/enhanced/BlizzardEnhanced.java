package nl.wantedchef.empirewand.spell.enhanced;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.common.visual.RingRenderer;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

import java.time.Duration;

/**
 * A devastating ice spell that creates a blizzard in a large area,
 * slowing and damaging enemies while creating icy terrain with enhanced visual effects.
 * <p>
 * This enhanced version of the blizzard spell creates a more visually impressive effect
 * with animated snowflakes, gusts of wind, and improved particle effects. It applies
 * slowness and occasional blindness effects to entities, and temporarily transforms
 * ground blocks into ice. The spell includes visual and audio feedback for the blizzard
 * effects and automatically cleans up the ice blocks when the spell ends.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Animated snowflake particle effects</li>
 *   <li>Area of effect snow and cloud particles</li>
 *   <li>Slowness and blindness potion effects on entities</li>
 *   <li>Periodic damage to affected entities</li>
 *   <li>Wind gusts that push entities</li>
 *   <li>Temporary ice block creation</li>
 *   <li>Automatic cleanup of ice blocks</li>
 *   <li>Expanding and dissipating ring effects</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell blizzard = new BlizzardEnhanced.Builder(api)
 *     .name("Blizzard")
 *     .description("Creates a devastating blizzard that slows and damages enemies while covering the area in ice with enhanced visuals.")
 *     .cooldown(Duration.ofSeconds(55))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class BlizzardEnhanced extends Spell<Void> {

    /**
     * Builder for creating BlizzardEnhanced spell instances.
     * <p>
     * Provides a fluent API for configuring the enhanced blizzard spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new BlizzardEnhanced spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Blizzard";
            this.description = "Creates a devastating blizzard that slows and damages enemies while covering the area in ice with enhanced visuals.";
            this.cooldown = Duration.ofSeconds(55);
            this.spellType = SpellType.ICE;
        }

        /**
         * Builds and returns a new BlizzardEnhanced spell instance.
         *
         * @return the constructed BlizzardEnhanced spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new BlizzardEnhanced(this);
        }
    }

    /**
     * Constructs a new BlizzardEnhanced spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private BlizzardEnhanced(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "blizzard-enhanced"
     */
    @Override
    @NotNull
    public String key() {
        return "blizzard-enhanced";
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
     * Executes the enhanced blizzard spell logic.
     * <p>
     * This method creates an enhanced blizzard effect at the caster's location that applies
     * slowness and blindness effects to nearby entities and creates temporary ice blocks.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        var world = player.getWorld();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 20.0);
        double damage = spellConfig.getDouble("values.damage", 1.5);
        int slowDuration = spellConfig.getInt("values.slow-duration-ticks", 60);
        int slowAmplifier = spellConfig.getInt("values.slow-amplifier", 3);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 150);
        boolean createIce = spellConfig.getBoolean("flags.create-ice", true);

        // Play initial sound
        if (world != null) {
            world.playSound(player.getLocation(), Sound.AMBIENT_UNDERWATER_LOOP, 2.0f, 0.3f);
        }

        // Create initial blizzard effect
        createInitialEffect(context, player.getLocation(), radius);

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
     * Creates the initial expanding ring effect for the blizzard.
     * <p>
     * This method creates a visual indicator of the blizzard's area of effect
     * by rendering an expanding ring of snowflake particles.
     *
     * @param context the spell context
     * @param center the center location of the blizzard
     * @param radius the radius of the blizzard effect
     */
    private void createInitialEffect(@NotNull SpellContext context, @NotNull Location center, double radius) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(center, "Center location cannot be null");
        
        World world = center.getWorld();
        if (world == null) return;

        // Create expanding ring to indicate blizzard area
        BukkitRunnable expandingRingTask = new BukkitRunnable() {
            int currentRadius = 1;
            
            @Override
            public void run() {
                if (currentRadius > radius) {
                    this.cancel();
                    return;
                }
                
                RingRenderer.renderRing(center, currentRadius, Math.max(12, currentRadius * 2),
                        (loc, vec) -> world.spawnParticle(Particle.SNOWFLAKE, loc, 2, 0, 0, 0, 0));
                
                currentRadius += 2;
            }
        };
        context.plugin().getTaskManager().runTaskTimer(expandingRingTask, 0L, 1L);
    }

    /**
     * A runnable that handles the enhanced blizzard's effects over time.
     * <p>
     * This task manages the blizzard's behavior including animated snowflake effects,
     * particle effects, entity effects, ice block creation, and cleanup.
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
        private final List<Snowflake> snowflakes = new ArrayList<>();

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
            
            // Initialize snowflakes
            for (int i = 0; i < 50; i++) {
                snowflakes.add(new Snowflake(center, radius, random));
            }
        }

        /**
         * Runs the enhanced blizzard task, creating visual effects and applying entity effects.
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

            // Update snowflakes
            updateSnowflakes();

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
         * Updates the animated snowflake particles.
         * <p>
         * This method moves the snowflake particles according to their velocity
         * and spawns them in the world.
         */
        private void updateSnowflakes() {
            for (Snowflake snowflake : snowflakes) {
                snowflake.update();
                if (world != null) {
                    world.spawnParticle(Particle.SNOWFLAKE, snowflake.location, 1, 0, 0, 0, 0);
                }
            }
        }

        /**
         * Creates the blizzard's visual particle effects.
         * <p>
         * This method spawns snow and cloud particles throughout the blizzard area
         * and periodically pushes entities with wind effects and gusts.
         */
        private void createBlizzardEffects() {
            if (world == null) {
                return;
            }
            
            // Create snow particles in a large area
            for (int i = 0; i < 20; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius;
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                double y = center.getY() + random.nextDouble() * 15;
                
                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.5, 0.5, 0.5, 0.02);
            }

            // Create wind effect by pushing entities
            if (ticks % 20 == 0) {
                for (LivingEntity entity : world.getNearbyLivingEntities(center, radius, 15, radius)) {
                    if (entity.equals(context.caster())) continue;
                    
                    // Push entity in random direction with added upward force
                    Vector push = new Vector(random.nextDouble() - 0.5, random.nextDouble() * 0.3, random.nextDouble() - 0.5).normalize().multiply(0.7);
                    entity.setVelocity(entity.getVelocity().add(push));
                }
            }

            // Create occasional gusts
            if (ticks % 40 == 0) {
                double gustAngle = random.nextDouble() * 2 * Math.PI;
                Vector gustDirection = new Vector(Math.cos(gustAngle), 0, Math.sin(gustAngle)).normalize().multiply(1.5);
                
                for (LivingEntity entity : world.getNearbyLivingEntities(center, radius, 15, radius)) {
                    if (entity.equals(context.caster())) continue;
                    entity.setVelocity(entity.getVelocity().add(gustDirection));
                }
                
                // Gust sound
                world.playSound(center, Sound.ENTITY_PLAYER_BREATH, 1.0f, 0.5f);
            }
        }

        /**
         * Applies the blizzard's effects to nearby entities.
         * <p>
         * This method damages entities, applies slowness potion effects, and
         * occasionally applies blindness effects.
         */
        private void applyBlizzardEffects() {
            if (world == null) {
                return;
            }
            
            for (LivingEntity entity : world.getNearbyLivingEntities(center, radius, 15, radius)) {
                if (entity.equals(context.caster())) continue;
                if (entity.isDead() || !entity.isValid()) continue;

                // Damage entity
                entity.damage(damage, context.caster());

                // Apply slowness
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, false));

                // Apply blindness occasionally
                if (random.nextDouble() < 0.3) {
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false));
                }

                // Visual effects
                var entityLocation = entity.getLocation();
                if (entityLocation != null) {
                    world.spawnParticle(Particle.SNOWFLAKE, entityLocation.add(0, 1, 0), 15, 0.5, 0.7, 0.5, 0.02);
                    world.spawnParticle(Particle.CLOUD, entityLocation.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.01);
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
                    world.spawnParticle(Particle.SNOWFLAKE, block.getLocation().add(0.5, 1, 0.5), 8, 0.3, 0.3, 0.3, 0.02);
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
            
            // Create dissipating effect
            createDissipatingEffect();
        }

        /**
         * Creates the dissipating ring effect when the blizzard ends.
         * <p>
         * This method creates a visual indicator that the blizzard is ending
         * by rendering a shrinking ring of cloud particles.
         */
        private void createDissipatingEffect() {
            BukkitRunnable dissipatingTask = new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 20;
                
                @Override
                public void run() {
                    if (ticks >= maxTicks || world == null) {
                        this.cancel();
                        return;
                    }
                    
                    // Create shrinking ring
                    double currentRadius = radius * (1.0 - (ticks / (double) maxTicks));
                    RingRenderer.renderRing(center, currentRadius, Math.max(12, (int)currentRadius * 2),
                            (loc, vec) -> world.spawnParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.01));
                    
                    ticks++;
                }
            };
            context.plugin().getTaskManager().runTaskTimer(dissipatingTask, 0L, 1L);
        }

        /**
         * Represents an animated snowflake particle in the blizzard.
         * <p>
         * This class manages the position and movement of individual snowflake particles
         * to create a more realistic blizzard effect.
         */
        private static class Snowflake {
            Location location;
            Vector velocity;
            final Location center;
            final double radius;
            final Random random;

            /**
             * Creates a new Snowflake instance.
             *
             * @param center the center location of the blizzard
             * @param radius the radius of the blizzard effect
             * @param random the random number generator
             */
            Snowflake(@NotNull Location center, double radius, @NotNull Random random) {
                this.center = Objects.requireNonNull(center, "Center cannot be null");
                this.radius = radius;
                this.random = Objects.requireNonNull(random, "Random cannot be null");
                
                // Random starting position above the blizzard area
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius;
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                double y = center.getY() + 15 + random.nextDouble() * 10;
                
                this.location = new Location(center.getWorld(), x, y, z);
                this.velocity = new Vector(0, -0.3 - random.nextDouble() * 0.4, 0);
            }

            /**
             * Updates the snowflake's position according to its velocity.
             * <p>
             * This method moves the snowflake downward with some horizontal drift,
             * and resets it to the top when it falls too far or moves outside the radius.
             */
            void update() {
                // Apply gravity
                location.add(velocity);
                
                // Apply some horizontal drift
                location.add((random.nextDouble() - 0.5) * 0.1, 0, (random.nextDouble() - 0.5) * 0.1);
                
                // Reset if fallen too far or outside radius
                double distanceFromCenter = Math.sqrt(
                    Math.pow(location.getX() - center.getX(), 2) + 
                    Math.pow(location.getZ() - center.getZ(), 2)
                );
                
                if (location.getY() < center.getY() - 5 || distanceFromCenter > radius * 1.5) {
                    // Reset to top
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double distance = random.nextDouble() * radius;
                    double x = center.getX() + Math.cos(angle) * distance;
                    double z = center.getZ() + Math.sin(angle) * distance;
                    location = new Location(center.getWorld(), x, center.getY() + 20, z);
                }
            }
        }
    }
}