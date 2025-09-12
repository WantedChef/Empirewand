package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashSet;
import java.util.Random;
import org.bukkit.Material;
import java.util.Set;
import java.util.Objects;

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
     */
    private record Config(double range, int meteorCount, double damage,
                         int craterRadius, int durationTicks) {}

    private Config config = new Config(25.0, 25, 12.0, 3, 100);

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
                spellConfig.getDouble("values.range", 25.0),
                spellConfig.getInt("values.meteor-count", 25),
                spellConfig.getDouble("values.damage", 12.0),
                spellConfig.getInt("values.crater-radius", 3),
                spellConfig.getInt("values.duration-ticks", 100)
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

        // Configuration
        double range = config.range;
        int meteorCount = config.meteorCount;
        double damage = config.damage;
        int craterRadius = config.craterRadius;
        int durationTicks = config.durationTicks;

        // Get target location
        Location targetLocation = player.getTargetBlock(null, (int) range).getLocation();
        if (targetLocation == null) {
            context.fx().fizzle(player);
            return null;
        }

        // Play initial sound
        var world = player.getWorld();
        if (world != null) {
            world.playSound(targetLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.5f);
        }

        // Start meteor shower effect
        BukkitRunnable meteorShowerTask = new MeteorShowerTask(context, targetLocation, meteorCount, damage, craterRadius, durationTicks);
        context.plugin().getTaskManager().runTaskTimer(meteorShowerTask, 0L, 4L);
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
     * A runnable that handles the meteor shower's effects over time.
     * <p>
     * This task manages the spawning of meteors, their impact effects, crater formation,
     * and the final explosion.
     */
    private static class MeteorShowerTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final int meteorCount;
        private final double damage;
        private final int craterRadius;
        private final int durationTicks;
        private final Random random = new Random();
        private final Set<Location> craterLocations = new HashSet<>();
        private int ticks = 0;
        private int meteorsSpawned = 0;

        /**
         * Creates a new MeteorShowerTask instance.
         *
         * @param context the spell context
         * @param center the center location for the meteor shower
         * @param meteorCount the number of meteors to spawn
         * @param damage the damage to apply to entities on impact
         * @param craterRadius the radius of each crater formed by meteor impacts
         * @param durationTicks the duration of the meteor shower in ticks
         */
        public MeteorShowerTask(@NotNull SpellContext context, @NotNull Location center, int meteorCount,
                double damage, int craterRadius, int durationTicks) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.meteorCount = meteorCount;
            this.damage = damage;
            this.craterRadius = craterRadius;
            this.durationTicks = durationTicks;
        }

        /**
         * Runs the meteor shower task, spawning meteors at random intervals.
         */
        @Override
        public void run() {
            var world = center.getWorld();
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= durationTicks || meteorsSpawned >= meteorCount) {
                this.cancel();
                createFinalExplosion();
                return;
            }

            // Spawn meteors at random locations around the center
            if (ticks % 5 == 0 && meteorsSpawned < meteorCount) {
                double offsetX = (random.nextDouble() - 0.5) * 20;
                double offsetZ = (random.nextDouble() - 0.5) * 20;
                Location meteorLocation = center.clone().add(offsetX, 30, offsetZ);

                spawnMeteor(meteorLocation);
                meteorsSpawned++;
            }

            ticks++;
        }

        /**
         * Spawns a meteor at the specified location.
         * <p>
         * This method creates a falling block that acts as a meteor and tracks its
         * impact to apply effects.
         *
         * @param location the location to spawn the meteor
         */
        private void spawnMeteor(@NotNull Location location) {
            Objects.requireNonNull(location, "Location cannot be null");
            
            World world = location.getWorld();
            if (world == null)
                return;

            // Create falling block (meteor)
            FallingBlock meteor = world.spawnFallingBlock(location, Material.MAGMA_BLOCK, (byte) 0);
            meteor.setDropItem(false);
            meteor.setHurtEntities(true);

            // Add velocity to make it fall
            meteor.setVelocity(new Vector(0, -1.5, 0));

            // Visual effects
            world.spawnParticle(Particle.FLAME, location, 20, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.LAVA, location, 10, 0.3, 0.3, 0.3, 0.05);

            // Track meteor impact
            BukkitRunnable meteorTrackingTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (meteor.isDead() || !meteor.isValid()) {
                        onMeteorImpact(meteor.getLocation());
                        this.cancel();
                    }
                }
            };
            context.plugin().getTaskManager().runTaskTimer(meteorTrackingTask, 1L, 1L);
        }

        /**
         * Handles the meteor impact effects at the specified location.
         * <p>
         * This method applies damage to nearby entities, creates a crater, and
         * generates visual and audio feedback for the impact.
         *
         * @param impactLocation the location where the meteor impacted
         */
        private void onMeteorImpact(@NotNull Location impactLocation) {
            Objects.requireNonNull(impactLocation, "Impact location cannot be null");
            
            World world = impactLocation.getWorld();
            if (world == null)
                return;

            // Damage entities in area
            for (LivingEntity entity : impactLocation.getWorld()
                    .getNearbyLivingEntities(impactLocation, 4, 4, 4)) {
                if (entity instanceof Player && entity.equals(context.caster()))
                    continue;
                if (entity.isValid() && !entity.isDead()) {
                    entity.damage(damage, context.caster());
                    entity.setFireTicks(60); // 3 seconds of fire
                }
            }

            // Create crater
            createCrater(impactLocation);

            // Visual effects
            world.spawnParticle(Particle.EXPLOSION, impactLocation, 10, 0, 0, 0, 0);
            world.spawnParticle(Particle.LAVA, impactLocation, 30, 1, 1, 1, 0.1);
            world.playSound(impactLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        }

        /**
         * Creates a crater at the specified location.
         * <p>
         * This method excavates a circular bowl-shaped crater around the impact point
         * by setting blocks to air.
         *
         * @param center the center location of the crater
         */
        private void createCrater(@NotNull Location center) {
            Objects.requireNonNull(center, "Center location cannot be null");
            
            World world = center.getWorld();
            if (world == null)
                return;

            int y = center.getBlockY();
            craterLocations.add(center);

            // Create a circular crater
            for (int x = -craterRadius; x <= craterRadius; x++) {
                for (int z = -craterRadius; z <= craterRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= craterRadius) {
                        Block block =
                                world.getBlockAt(center.getBlockX() + x, y, center.getBlockZ() + z);
                        if (!block.getType().isAir() && block.getType() != Material.BEDROCK) {
                            // Create a bowl shape
                            int depth = (int) (craterRadius - distance) + 1;
                            for (int d = 0; d < depth; d++) {
                                Block toModify = block.getRelative(BlockFace.DOWN, d);
                                if (toModify.getType() != Material.BEDROCK) {
                                    toModify.setType(Material.AIR);
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * Creates the final massive explosion when the meteor shower ends.
         * <p>
         * This method creates a large explosion at the center location and applies
         * scaled damage to all entities within the blast radius.
         */
        private void createFinalExplosion() {
            World world = center.getWorld();
            if (world == null)
                return;

            // Create a massive explosion at the center
            world.createExplosion(center, 5.0f, false, false);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 2, 2, 2, 0);
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);

            // Damage all entities in a large radius
            for (LivingEntity entity : world.getNearbyLivingEntities(center, 15, 15, 15)) {
                if (entity instanceof Player && entity.equals(context.caster()))
                    continue;
                
                var entityLocation = entity.getLocation();
                if (entityLocation == null) continue;
                
                double distance = entityLocation.distance(center);
                double scaledDamage = damage * (1 - (distance / 15));
                if (scaledDamage > 0) {
                    entity.damage(scaledDamage, context.caster());
                }
            }
        }
    }
}
