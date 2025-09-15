package nl.wantedchef.empirewand.spell.weather;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.util.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.bukkit.Material;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A powerful spell that calls down a meteor shower over a large area, dealing massive damage and
 * creating craters.
 * <p>
 * This spell summons a devastating meteor shower that rains destruction upon enemies within
 * a large area. Each meteor creates a crater on impact and deals damage to nearby entities.
 * The spell concludes with a massive final explosion at the center location.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Multiple falling meteor blocks</li>
 *   <li>Area of effect damage on impact</li>
 *   <li>Circular crater formation</li>
 *   <li>Bowl-shaped excavation pattern</li>
 *   <li>Final massive explosion</li>
 *   <li>Flame and lava particle effects</li>
 *   <li>Audio feedback with thunder and explosion sounds</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell meteorShower = new MeteorShower.Builder(api)
 *     .name("Meteor Shower")
 *     .description("Calls down a devastating meteor shower that rains destruction upon your enemies.")
 *     .cooldown(Duration.ofSeconds(45))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class MeteorShower extends Spell<Void> {

    /**
     * Builder for creating MeteorShower spell instances.
     * <p>
     * Provides a fluent API for configuring the meteor shower spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new MeteorShower spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Meteor Shower";
            this.description =
                    "Calls down a devastating meteor shower that rains destruction upon your enemies.";
            this.cooldown = Duration.ofSeconds(45);
            this.spellType = SpellType.FIRE;
        }

        /**
         * Builds and returns a new MeteorShower spell instance.
         *
         * @return the constructed MeteorShower spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new MeteorShower(this);
        }
    }

    /**
     * Configuration for the MeteorShower spell.
     *
     * @param range The range for targeting the meteor shower
     * @param meteorCount The number of meteors to spawn
     * @param damage The damage dealt to entities on impact
     * @param craterRadius The radius of each crater formed by meteor impacts
     * @param durationTicks The duration of the meteor shower in ticks
     * @param meteorSpeed The speed multiplier for falling meteors
     * @param trailIntensity The intensity of meteor trails (particle count multiplier)
     * @param explosionPower The power of meteor explosions
     * @param shockwaveRadius The radius of the shockwave effect
     */
    private record Config(double range, int meteorCount, double damage,
                         int craterRadius, int durationTicks, double meteorSpeed,
                         double trailIntensity, float explosionPower, double shockwaveRadius) {}

    private Config config = new Config(25.0, 15, 12.0, 3, 80, 2.0, 1.5, 3.5f, 8.0);

    /**
     * Constructs a new MeteorShower spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private MeteorShower(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "meteor-shower"
     */
    @Override
    @NotNull
    public String key() {
        return "meteor-shower";
    }

    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        super.loadConfig(spellConfig);
        this.config = new Config(
                SpellUtils.getConfigDouble(spellConfig, "values.range", 25.0),
                SpellUtils.getConfigInt(spellConfig, "values.meteor-count", 15),
                SpellUtils.getConfigDouble(spellConfig, "values.damage", 12.0),
                SpellUtils.getConfigInt(spellConfig, "values.crater-radius", 3),
                SpellUtils.getConfigInt(spellConfig, "values.duration-ticks", 80),
                SpellUtils.getConfigDouble(spellConfig, "values.meteor-speed", 2.0),
                SpellUtils.getConfigDouble(spellConfig, "values.trail-intensity", 1.5),
                (float) SpellUtils.getConfigDouble(spellConfig, "values.explosion-power", 3.5),
                SpellUtils.getConfigDouble(spellConfig, "values.shockwave-radius", 8.0)
        );
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
     * Executes the meteor shower spell logic.
     * <p>
     * This method creates a meteor shower effect that spawns multiple falling meteors
     * around a target location, each creating craters and dealing damage on impact.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        // Get target location
        Location targetLocation = player.getTargetBlock(null, (int) config.range).getLocation();
        if (targetLocation == null) {
            context.fx().fizzle(player);
            return null;
        }

        // Play dramatic initial sounds and effects
        var world = player.getWorld();
        if (world != null) {
            // Deep rumbling followed by cosmic energy buildup
            world.playSound(targetLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.3f);
            world.playSound(targetLocation, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);
            world.playSound(targetLocation, Sound.AMBIENT_CAVE, 1.5f, 2.0f);

            // Space/cosmic warning effects
            createCosmicWarningEffect(context, world, targetLocation);
        }

        // Start meteor shower effect with enhanced configuration
        BukkitRunnable meteorShowerTask = new MeteorShowerTask(context, targetLocation, config);
        meteorShowerTask.runTaskTimer(context.plugin(), 20L, 3L); // Slight delay for dramatic effect
        return null;
    }

    /**
     * Creates a cosmic warning effect at the target location before meteors begin falling.
     * <p>
     * This creates an ominous buildup with space-themed particles and effects to warn
     * players of the incoming meteor shower.
     *
     * @param context the spell context
     * @param world the world to create effects in
     * @param center the center location for the warning effect
     */
    private void createCosmicWarningEffect(@NotNull SpellContext context, @NotNull World world, @NotNull Location center) {
        // Create a swirling cosmic vortex above the target area
        BukkitRunnable warningEffect = new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 20) { // 1 second warning
                    this.cancel();
                    return;
                }

                double radius = 8.0;
                for (int i = 0; i < 16; i++) {
                    double angle = (ticks * 0.5 + i * Math.PI / 8) % (2 * Math.PI);
                    double x = center.getX() + Math.cos(angle) * radius;
                    double z = center.getZ() + Math.sin(angle) * radius;
                    double y = center.getY() + 15 + Math.sin(ticks * 0.3) * 3;

                    Location particleLoc = new Location(world, x, y, z);

                    // Cosmic energy particles
                    world.spawnParticle(Particle.PORTAL, particleLoc, 3, 0.2, 0.2, 0.2, 0.1);
                    world.spawnParticle(Particle.WITCH, particleLoc, 1, 0.1, 0.1, 0.1, 0);

                    // Dark energy
                    if (ticks % 3 == 0) {
                        world.spawnParticle(Particle.SMOKE, particleLoc, 2, 0.3, 0.3, 0.3, 0.05);
                    }
                }

                // Central energy buildup
                world.spawnParticle(Particle.ENCHANT, center.clone().add(0, 20, 0),
                    5, 2, 2, 2, 0.5);

                ticks++;
            }
        };

        // Schedule the warning effect
        warningEffect.runTaskTimer(context.plugin(), 0L, 1L);
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
     * A runnable that handles the meteor shower's effects over time.
     * <p>
     * This task manages the spawning of meteors, their impact effects, crater formation,
     * spectacular visual effects, and the final cataclysmic explosion.
     */
    private static class MeteorShowerTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final Config config;
        private final Random random = new Random();
        private final Set<Location> craterLocations = new HashSet<>();
        private final Set<BukkitRunnable> activeTasks = ConcurrentHashMap.newKeySet();
        private final List<MeteorTrail> activeMeteors = new ArrayList<>();
        private int ticks = 0;
        private int meteorsSpawned = 0;

        /**
         * Creates a new MeteorShowerTask instance.
         *
         * @param context the spell context
         * @param center the center location for the meteor shower
         * @param config the spell configuration containing all parameters
         */
        public MeteorShowerTask(@NotNull SpellContext context, @NotNull Location center, @NotNull Config config) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.config = Objects.requireNonNull(config, "Config cannot be null");
        }

        /**
         * Represents a meteor with its trajectory and visual effects.
         */
        private static class MeteorTrail {
            final Location currentPos;
            final Vector velocity;
            final double trailIntensity;
            final Material meteorMaterial;
            final int maxAge;
            int age = 0;
            boolean impacted = false;

            MeteorTrail(Location startPos, Vector vel, double intensity) {
                this.currentPos = startPos.clone();
                this.velocity = vel.clone();
                this.trailIntensity = intensity;
                // Randomize meteor materials for variety
                Material[] meteorTypes = {Material.MAGMA_BLOCK, Material.OBSIDIAN,
                    Material.BLACKSTONE, Material.NETHERITE_BLOCK};
                this.meteorMaterial = meteorTypes[new Random().nextInt(meteorTypes.length)];
                this.maxAge = 200; // Maximum age before cleanup
            }
        }

        /**
         * Runs the meteor shower task, spawning meteors and updating visual effects.
         */
        @Override
        public void run() {
            var world = center.getWorld();
            if (world == null) {
                this.cancel();
                return;
            }

            if (ticks >= config.durationTicks || meteorsSpawned >= config.meteorCount) {
                this.cancel();
                createFinalCataclysmicExplosion(world);
                return;
            }

            // Update existing meteors and their trails
            updateMeteorTrails(world);

            // Spawn new meteors with varied timing for dramatic effect
            int spawnInterval = Math.max(3, 8 - (ticks / 10)); // Accelerating spawn rate
            if (ticks % spawnInterval == 0 && meteorsSpawned < config.meteorCount) {
                spawnSpectacularMeteor(world);
                meteorsSpawned++;
            }

            // Ambient space effects during shower
            if (ticks % 5 == 0) {
                createAmbientCosmicEffects(world);
            }

            ticks++;
        }

        @Override
        public void cancel() throws IllegalStateException {
            // Cancel all active meteor tracking tasks
            activeTasks.forEach(task -> {
                if (!task.isCancelled()) {
                    task.cancel();
                }
            });
            activeTasks.clear();
            super.cancel();
        }

        /**
         * Updates all active meteor trails and their spectacular visual effects.
         *
         * @param world the world to update meteors in
         */
        private void updateMeteorTrails(@NotNull World world) {
            activeMeteors.removeIf(meteor -> {
                if (meteor.impacted || meteor.age >= meteor.maxAge) {
                    return true;
                }

                // Update meteor position with realistic physics
                meteor.velocity.add(new Vector(0, -0.1, 0)); // Gravity acceleration
                meteor.currentPos.add(meteor.velocity);
                meteor.age++;

                // Check for ground impact
                Block groundBlock = world.getBlockAt(meteor.currentPos);
                if (!groundBlock.getType().isAir() && !groundBlock.isLiquid()) {
                    createDevastatingImpact(world, meteor.currentPos, meteor);
                    meteor.impacted = true;
                    return true;
                }

                // Create spectacular fire/plasma trail
                createMeteorTrail(world, meteor);

                return false;
            });
        }

        /**
         * Spawns a spectacular meteor with realistic trajectory and plasma effects.
         *
         * @param world the world to spawn the meteor in
         */
        private void spawnSpectacularMeteor(@NotNull World world) {
            // Calculate varied spawn positions for cinematic effect
            double offsetX = (random.nextDouble() - 0.5) * 25;
            double offsetZ = (random.nextDouble() - 0.5) * 25;
            double spawnHeight = 35 + random.nextDouble() * 15; // Varied height for realism

            Location meteorStartPos = center.clone().add(offsetX, spawnHeight, offsetZ);

            // Calculate realistic trajectory with slight randomization
            Vector targetDirection = center.toVector().subtract(meteorStartPos.toVector()).normalize();
            targetDirection.multiply(config.meteorSpeed);

            // Add slight randomization to trajectory for natural feel
            targetDirection.add(new Vector(
                (random.nextDouble() - 0.5) * 0.3,
                -0.2, // Slight downward bias
                (random.nextDouble() - 0.5) * 0.3
            ));

            // Create meteor trail object
            MeteorTrail meteor = new MeteorTrail(meteorStartPos, targetDirection, config.trailIntensity);
            activeMeteors.add(meteor);

            // Play incoming meteor sound with spatial audio
            world.playSound(meteorStartPos, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.3f);
            world.playSound(meteorStartPos, Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.5f);
        }

        /**
         * Creates spectacular fire and plasma trail effects for a meteor.
         *
         * @param world the world to create effects in
         * @param meteor the meteor to create trail for
         */
        private void createMeteorTrail(@NotNull World world, @NotNull MeteorTrail meteor) {
            Location pos = meteor.currentPos;
            int particleCount = (int) (meteor.trailIntensity * 10);

            // Intense fire core
            world.spawnParticle(Particle.FLAME, pos, particleCount,
                0.3, 0.3, 0.3, 0.15);

            // Plasma outer layer
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, particleCount / 2,
                0.5, 0.5, 0.5, 0.1);

            // Superheated debris
            world.spawnParticle(Particle.LAVA, pos, particleCount / 3,
                0.4, 0.4, 0.4, 0.08);

            // Trailing smoke and embers
            world.spawnParticle(Particle.LARGE_SMOKE, pos.clone().subtract(meteor.velocity.clone().multiply(0.5)),
                particleCount / 4, 0.3, 0.3, 0.3, 0.05);

            // Cosmic energy particles for large meteors
            if (meteor.trailIntensity > 1.0) {
                world.spawnParticle(Particle.ENCHANT, pos, 3, 0.2, 0.2, 0.2, 0.3);
                world.spawnParticle(Particle.PORTAL, pos, 2, 0.1, 0.1, 0.1, 0.2);
            }

            // Atmospheric heating sound effects
            if (meteor.age % 10 == 0) {
                world.playSound(pos, Sound.BLOCK_FIRE_AMBIENT, 1.0f,
                    1.5f + random.nextFloat() * 0.5f);
            }
        }

        /**
         * Creates devastating impact effects with massive explosions and environmental destruction.
         *
         * @param world the world where the impact occurs
         * @param impactLocation the location of the meteor impact
         * @param meteor the meteor that impacted
         */
        private void createDevastatingImpact(@NotNull World world, @NotNull Location impactLocation, @NotNull MeteorTrail meteor) {
            // Massive explosion with configurable power
            world.createExplosion(impactLocation, config.explosionPower, true, true);

            // Create shockwave effect
            createShockwave(world, impactLocation);

            // Apply devastating damage to entities
            applyImpactDamage(world, impactLocation, meteor);

            // Create enhanced crater with realistic formation
            createAdvancedCrater(world, impactLocation, meteor);

            // Spectacular visual effects
            createImpactVisualEffects(world, impactLocation, meteor);

            // Multi-layered audio feedback
            createImpactAudioEffects(world, impactLocation);

            // Environmental effects (fire spread, block transformation)
            createEnvironmentalDestruction(world, impactLocation);
        }

        /**
         * Creates a devastating shockwave that propagates outward from the impact.
         *
         * @param world the world to create the shockwave in
         * @param center the center of the shockwave
         */
        private void createShockwave(@NotNull World world, @NotNull Location center) {
            BukkitRunnable shockwaveTask = new BukkitRunnable() {
                private double radius = 0;
                private final double maxRadius = config.shockwaveRadius;

                @Override
                public void run() {
                    if (radius >= maxRadius) {
                        this.cancel();
                        return;
                    }

                    // Create shockwave ring particles
                    for (int i = 0; i < 32; i++) {
                        double angle = (i * Math.PI * 2) / 32;
                        double x = center.getX() + Math.cos(angle) * radius;
                        double z = center.getZ() + Math.sin(angle) * radius;
                        Location ringPos = new Location(world, x, center.getY() + 0.1, z);

                        // Shockwave particles
                        world.spawnParticle(Particle.EXPLOSION, ringPos, 1, 0, 0, 0, 0);
                        world.spawnParticle(Particle.LARGE_SMOKE, ringPos, 2, 0.2, 0.2, 0.2, 0.05);
                        world.spawnParticle(Particle.ASH, ringPos, 3, 0.3, 0.1, 0.3, 0.1);

                        // Knockback entities in shockwave path
                        for (Entity entity : world.getNearbyEntities(ringPos, 1.5, 2, 1.5)) {
                            if (entity instanceof LivingEntity living && !entity.equals(context.caster())) {
                                Vector knockback = entity.getLocation().toVector().subtract(center.toVector()).normalize();
                                knockback.multiply(0.8).setY(0.3);
                                entity.setVelocity(knockback);

                                // Apply minor shockwave damage
                                living.damage(config.damage * 0.3, context.caster());
                            }
                        }
                    }

                    radius += 1.2;
                }
            };

            activeTasks.add(shockwaveTask);
            shockwaveTask.runTaskTimer(context.plugin(), 1L, 2L);
        }

        /**
         * Applies devastating damage to entities within the impact radius.
         *
         * @param world the world containing the entities
         * @param impactLocation the location of the meteor impact
         * @param meteor the meteor that impacted
         */
        private void applyImpactDamage(@NotNull World world, @NotNull Location impactLocation, @NotNull MeteorTrail meteor) {
            double damageRadius = 6.0;

            for (LivingEntity entity : world.getNearbyLivingEntities(impactLocation, damageRadius)) {
                if (entity.equals(context.caster())) continue;

                double distance = entity.getLocation().distance(impactLocation);
                double damageScale = 1.0 - (distance / damageRadius);

                if (damageScale > 0) {
                    // Scale damage based on distance and meteor intensity
                    double totalDamage = config.damage * damageScale * meteor.trailIntensity;
                    entity.damage(totalDamage, context.caster());

                    // Apply fire damage and effects
                    entity.setFireTicks(100); // 5 seconds of fire
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1)); // Stun effect

                    // Launch entities away from impact
                    Vector knockback = entity.getLocation().toVector().subtract(impactLocation.toVector()).normalize();
                    knockback.multiply(1.5).setY(0.8);
                    entity.setVelocity(knockback);
                }
            }
        }

        /**
         * Creates an advanced crater with realistic formation and material transformation.
         *
         * @param world the world to create the crater in
         * @param center the center location of the crater
         * @param meteor the meteor that created the crater
         */
        private void createAdvancedCrater(@NotNull World world, @NotNull Location center, @NotNull MeteorTrail meteor) {
            int craterRadius = config.craterRadius;
            int y = center.getBlockY();
            craterLocations.add(center);

            // Create a more realistic crater with varied depth and materials
            for (int x = -craterRadius * 2; x <= craterRadius * 2; x++) {
                for (int z = -craterRadius * 2; z <= craterRadius * 2; z++) {
                    double distance = Math.sqrt(x * x + z * z);

                    if (distance <= craterRadius * 2) {
                        Block block = world.getBlockAt(center.getBlockX() + x, y, center.getBlockZ() + z);

                        if (block.getType() != Material.BEDROCK && !block.getType().isAir()) {
                            // Calculate crater depth with realistic bowl shape
                            double normalizedDistance = distance / (craterRadius * 2);
                            int depth = (int) ((1 - normalizedDistance * normalizedDistance) * (craterRadius + 2));

                            for (int d = 0; d < depth; d++) {
                                Block toModify = block.getRelative(BlockFace.DOWN, d);
                                if (toModify.getType() != Material.BEDROCK) {
                                    // Transform blocks based on distance from center
                                    if (distance < craterRadius * 0.3) {
                                        // Center: molten/glass formation
                                        if (random.nextDouble() < 0.7) {
                                            toModify.setType(Material.MAGMA_BLOCK);
                                        } else {
                                            toModify.setType(Material.AIR);
                                        }
                                    } else if (distance < craterRadius * 0.8) {
                                        // Middle ring: scorched earth
                                        if (random.nextDouble() < 0.5) {
                                            toModify.setType(Material.BLACKSTONE);
                                        } else {
                                            toModify.setType(Material.AIR);
                                        }
                                    } else {
                                        // Outer ring: just excavation
                                        toModify.setType(Material.AIR);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Place meteor fragments
            placeMeteorFragments(world, center, meteor);
        }

        /**
         * Places realistic meteor fragments around the crater.
         *
         * @param world the world to place fragments in
         * @param center the center of the impact
         * @param meteor the meteor that impacted
         */
        private void placeMeteorFragments(@NotNull World world, @NotNull Location center, @NotNull MeteorTrail meteor) {
            for (int i = 0; i < 5 + random.nextInt(8); i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = 2 + random.nextDouble() * 8;

                Location fragmentLoc = center.clone().add(
                    Math.cos(angle) * distance,
                    0,
                    Math.sin(angle) * distance
                );

                Block fragmentBlock = world.getHighestBlockAt(fragmentLoc);
                if (fragmentBlock.getType().isAir()) {
                    fragmentBlock = fragmentBlock.getRelative(BlockFace.DOWN);
                }

                if (!fragmentBlock.getType().isAir() && fragmentBlock.getType() != Material.BEDROCK) {
                    fragmentBlock.getRelative(BlockFace.UP).setType(meteor.meteorMaterial);
                }
            }
        }

        /**
         * Creates spectacular visual effects for meteor impacts.
         *
         * @param world the world to create effects in
         * @param impactLocation the location of the impact
         * @param meteor the meteor that impacted
         */
        private void createImpactVisualEffects(@NotNull World world, @NotNull Location impactLocation, @NotNull MeteorTrail meteor) {
            // Massive explosion particles
            world.spawnParticle(Particle.EXPLOSION_EMITTER, impactLocation, 3, 1, 1, 1, 0);
            world.spawnParticle(Particle.EXPLOSION, impactLocation, 15, 2, 2, 2, 0.3);

            // Lava and magma effects
            world.spawnParticle(Particle.LAVA, impactLocation, 25, 3, 2, 3, 0.2);
            world.spawnParticle(Particle.DRIPPING_LAVA, impactLocation, 20, 2, 3, 2, 0.1);

            // Fire and plasma
            world.spawnParticle(Particle.FLAME, impactLocation, 30, 2.5, 2.5, 2.5, 0.2);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, impactLocation, 20, 2, 2, 2, 0.15);

            // Smoke and debris
            world.spawnParticle(Particle.LARGE_SMOKE, impactLocation, 20, 3, 3, 3, 0.1);
            world.spawnParticle(Particle.ASH, impactLocation, 30, 4, 3, 4, 0.2);

            // Cosmic energy remnants
            world.spawnParticle(Particle.ENCHANT, impactLocation, 15, 2, 2, 2, 0.8);
            world.spawnParticle(Particle.PORTAL, impactLocation, 10, 1.5, 1.5, 1.5, 0.5);

            // Lingering fire effects
            BukkitRunnable fireEffect = new BukkitRunnable() {
                private int ticks = 0;

                @Override
                public void run() {
                    if (ticks >= 40) { // 2 seconds
                        this.cancel();
                        return;
                    }

                    world.spawnParticle(Particle.FLAME, impactLocation, 5, 1.5, 0.5, 1.5, 0.05);
                    world.spawnParticle(Particle.LAVA, impactLocation, 2, 1, 0.2, 1, 0.02);

                    if (ticks % 10 == 0) {
                        world.spawnParticle(Particle.EXPLOSION, impactLocation, 1, 0.5, 0.5, 0.5, 0);
                    }

                    ticks++;
                }
            };

            activeTasks.add(fireEffect);
            fireEffect.runTaskTimer(context.plugin(), 5L, 1L);
        }

        /**
         * Creates multi-layered audio effects for meteor impacts.
         *
         * @param world the world to play sounds in
         * @param impactLocation the location of the impact
         */
        private void createImpactAudioEffects(@NotNull World world, @NotNull Location impactLocation) {
            // Primary explosion sound
            world.playSound(impactLocation, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.6f);

            // Thunder crash
            world.playSound(impactLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.5f, 0.8f);

            // Wither explosion for added intensity
            world.playSound(impactLocation, Sound.ENTITY_WITHER_BREAK_BLOCK, 2.0f, 0.7f);

            // Dragon roar for epic feel
            world.playSound(impactLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);

            // Delayed rumble effect
            BukkitRunnable rumbleEffect = new BukkitRunnable() {
                private int count = 0;

                @Override
                public void run() {
                    if (count >= 3) {
                        this.cancel();
                        return;
                    }

                    world.playSound(impactLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                        2.0f - (count * 0.5f), 0.4f + (count * 0.2f));
                    count++;
                }
            };

            activeTasks.add(rumbleEffect);
            rumbleEffect.runTaskTimer(context.plugin(), 10L, 8L);
        }

        /**
         * Creates environmental destruction and fire spread effects.
         *
         * @param world the world to modify
         * @param center the center of the destruction
         */
        private void createEnvironmentalDestruction(@NotNull World world, @NotNull Location center) {
            // Spread fire in random locations around impact
            for (int i = 0; i < 8 + random.nextInt(12); i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = 3 + random.nextDouble() * 8;

                Location fireLoc = center.clone().add(
                    Math.cos(angle) * distance,
                    0,
                    Math.sin(angle) * distance
                );

                Block fireBlock = world.getHighestBlockAt(fireLoc);
                if (fireBlock.getType().isAir()) {
                    fireBlock.setType(Material.FIRE);
                }
            }

            // Transform nearby grass/vegetation to scorched earth
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    if (random.nextDouble() < 0.3) {
                        Block block = world.getBlockAt(center.getBlockX() + x, center.getBlockY(), center.getBlockZ() + z);
                        if (block.getType() == Material.GRASS_BLOCK) {
                            block.setType(Material.COARSE_DIRT);
                        } else if (block.getType() == Material.DIRT) {
                            block.setType(Material.COARSE_DIRT);
                        }
                    }
                }
            }
        }

        /**
         * Creates ambient cosmic effects during the meteor shower.
         *
         * @param world the world to create effects in
         */
        private void createAmbientCosmicEffects(@NotNull World world) {
            // Random cosmic energy above the battlefield
            for (int i = 0; i < 3; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 30;
                double offsetZ = (random.nextDouble() - 0.5) * 30;
                Location cosmicLoc = center.clone().add(offsetX, 20 + random.nextDouble() * 10, offsetZ);

                world.spawnParticle(Particle.PORTAL, cosmicLoc, 2, 0.3, 0.3, 0.3, 0.1);
                world.spawnParticle(Particle.WITCH, cosmicLoc, 1, 0.1, 0.1, 0.1, 0);
            }

            // Distant thunder rumbles
            if (random.nextDouble() < 0.3) {
                world.playSound(center, Sound.AMBIENT_CAVE, 0.8f, 0.5f + random.nextFloat());
            }
        }

        /**
         * Creates the final cataclysmic explosion when the meteor shower ends.
         * <p>
         * This creates a massive, devastating finale with multiple explosion waves
         * and spectacular effects.
         */
        private void createFinalCataclysmicExplosion(@NotNull World world) {
            // Multiple explosion waves for dramatic effect
            BukkitRunnable finalExplosion = new BukkitRunnable() {
                private int wave = 0;
                private final int maxWaves = 3;

                @Override
                public void run() {
                    if (wave >= maxWaves) {
                        this.cancel();
                        return;
                    }

                    float explosionPower = (wave == maxWaves - 1) ? config.explosionPower * 1.5f : config.explosionPower;

                    // Create massive explosion
                    world.createExplosion(center, explosionPower, true, true);

                    // Spectacular particle effects
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 5 + wave * 2, 3, 3, 3, 0);
                    world.spawnParticle(Particle.LAVA, center, 30 + wave * 10, 4, 3, 4, 0.3);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 25, 3, 3, 3, 0.2);

                    // Epic sound effects
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.5f, 0.4f - wave * 0.1f);
                    world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.3f);
                    world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.6f);

                    // Damage entities with scaling based on wave
                    double damageRadius = 18 + wave * 3;
                    for (LivingEntity entity : world.getNearbyLivingEntities(center, damageRadius)) {
                        if (entity.equals(context.caster())) continue;

                        double distance = entity.getLocation().distance(center);
                        double scaledDamage = config.damage * 1.5 * (1 - (distance / damageRadius));

                        if (scaledDamage > 0) {
                            entity.damage(scaledDamage, context.caster());
                            entity.setFireTicks(120);

                            // Massive knockback on final wave
                            if (wave == maxWaves - 1) {
                                Vector knockback = entity.getLocation().toVector().subtract(center.toVector()).normalize();
                                knockback.multiply(2.5).setY(1.2);
                                entity.setVelocity(knockback);
                            }
                        }
                    }

                    wave++;
                }
            };

            activeTasks.add(finalExplosion);
            finalExplosion.runTaskTimer(context.plugin(), 0L, 10L);
        }
    }
}
