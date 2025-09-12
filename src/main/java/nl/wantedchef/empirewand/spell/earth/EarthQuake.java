package nl.wantedchef.empirewand.spell.earth;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;

/**
 * An earth spell that allows the caster to leap into the air and create a devastating earthquake upon landing.
 * <p>
 * This spell enables the caster to jump high into the air and, upon landing, creates a wave-like
 * earthquake effect that forms a crater and launches nearby enemies. The spell includes visual and
 * audio feedback for both the leap and impact phases.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>High leap in the direction the player is facing</li>
 *   <li>Wave-like earthquake effect upon landing</li>
 *   <li>Crater formation with bowl-shaped excavation</li>
 *   <li>Knockback and knockup effects on nearby entities</li>
 *   <li>Extensive particle and sound effects</li>
 *   <li>Falling block physics for realistic debris</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell earthQuake = new EarthQuake.Builder(api)
 *     .name("EarthQuake")
 *     .description("Leap up and smash the ground: wave-like quake, crater, launch nearby foes.")
 *     .cooldown(Duration.ofSeconds(8))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class EarthQuake extends Spell<Void> {

    /**
     * Configuration record containing all EarthQuake spell settings.
     * <p>
     * This record holds all configuration values loaded from the spell configuration.
     * Using a record provides immutable configuration access and better performance
     * compared to repeated map lookups.
     *
     * @param radius      the radius of the earthquake effect
     * @param minDepth    the minimum depth of the crater
     * @param maxDepth    the maximum depth of the crater
     * @param knockback   the horizontal knockback strength
     * @param knockup     the vertical knockback strength
     * @param leapForward the forward leap distance
     * @param leapUp      the upward leap height
     */
    private record Config(
        int radius,
        int minDepth,
        int maxDepth,
        double knockback,
        double knockup,
        double leapForward,
        double leapUp
    ) {}

    /**
     * Default configuration values.
     */
    private static final int DEFAULT_RADIUS = 6;
    private static final int DEFAULT_MIN_DEPTH = 4;
    private static final int DEFAULT_MAX_DEPTH = 6;
    private static final double DEFAULT_KNOCKBACK = 1.35;
    private static final double DEFAULT_KNOCKUP = 0.6;
    private static final double DEFAULT_LEAP_FORWARD = 1.25;
    private static final double DEFAULT_LEAP_UP = 1.55;

    /**
     * Current spell configuration.
     */
    private Config config = new Config(
        DEFAULT_RADIUS,
        DEFAULT_MIN_DEPTH,
        DEFAULT_MAX_DEPTH,
        DEFAULT_KNOCKBACK,
        DEFAULT_KNOCKUP,
        DEFAULT_LEAP_FORWARD,
        DEFAULT_LEAP_UP
    );

    /**
     * Builder for creating EarthQuake spell instances.
     * <p>
     * Provides a fluent API for configuring the earthquake spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new EarthQuake spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "EarthQuake";
            this.description = "Leap up and smash the ground: wave-like quake, crater, launch nearby foes.";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.EARTH;
        }

        /**
         * Builds and returns a new EarthQuake spell instance.
         *
         * @return the constructed EarthQuake spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new EarthQuake(this);
        }
    }

    /**
     * Constructs a new EarthQuake spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private EarthQuake(@NotNull Builder builder) {
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
            Math.max(3, spellConfig.getInt("values.radius", DEFAULT_RADIUS)),
            Math.max(1, spellConfig.getInt("values.min-depth", DEFAULT_MIN_DEPTH)),
            Math.max(1, spellConfig.getInt("values.max-depth", DEFAULT_MAX_DEPTH)),
            Math.max(0.1, spellConfig.getDouble("values.knockback", DEFAULT_KNOCKBACK)),
            Math.max(0.0, spellConfig.getDouble("values.knockup", DEFAULT_KNOCKUP)),
            spellConfig.getDouble("values.leap-forward", DEFAULT_LEAP_FORWARD),
            spellConfig.getDouble("values.leap-up", DEFAULT_LEAP_UP)
        );
        
        // Ensure maxDepth is at least minDepth
        if (this.config.maxDepth < this.config.minDepth) {
            this.config = new Config(
                this.config.radius,
                this.config.minDepth,
                this.config.minDepth, // Set maxDepth to minDepth
                this.config.knockback,
                this.config.knockup,
                this.config.leapForward,
                this.config.leapUp
            );
        }
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "earthquake"
     */
    @Override
    @NotNull
    public String key() {
        return "earthquake";
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
     * Executes the earthquake spell logic.
     * <p>
     * This method causes the player to leap forward and upward, then creates a runnable
     * that waits for the player to land before executing the earthquake effect.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        final Player caster = context.caster();
        if (caster == null) {
            return null;
        }
        
        final World world = caster.getWorld();
        final Location location = caster.getLocation();
        if (world == null || location == null) {
            return null;
        }

        // Use pre-loaded configuration for better performance (griefing = OK as requested)
        final int radius = config.radius;
        final int minDepth = config.minDepth;
        final int maxDepth = config.maxDepth;
        final double kbOut = config.knockback;
        final double kbUp = config.knockup;
        final double leapFwd = config.leapForward;
        final double leapUp = config.leapUp; // ~12–15 block high

        // Launch in look direction
        Vector dir = location.getDirection().normalize();
        Vector launch = dir.multiply(leapFwd);
        launch.setY(leapUp);
        caster.setVelocity(launch);

        // Direct cast FX
        world.spawnParticle(Particle.CLOUD, location, 40, 0.6, 0.1, 0.6, 0.02);
        world.playSound(location, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.8f, 0.85f);

        // Landing detection and smash execution
        new BukkitRunnable() {
            boolean leftGround = false;
            int safetyTicks = 20 * 6; // failsafe 6s
            final Random rng = new Random();

            @Override
            public void run() {
                if (--safetyTicks <= 0 || !caster.isValid() || caster.isDead()) {
                    cancel();
                    return;
                }

                // Prevent fall damage during jump
                caster.setFallDistance(0f);

                if (!leftGround && !isGrounded(caster)) {
                    leftGround = true; // once airborne
                }

                if (leftGround && isGrounded(caster)) {
                    Location impact = caster.getLocation();

                    // Small correction so player lands just outside the crater:
                    // move the crater center point slightly behind the player.
                    Location center = impact.clone().subtract(dir.clone().setY(0).normalize().multiply(2.0));

                    quakeSmash(center, caster, radius, minDepth, maxDepth, kbOut, kbUp, rng);
                    cancel();
                }
            }

            /**
             * Executes the earthquake smash effect at the specified location.
             * <p>
             * This method creates the visual effects, launches nearby entities, and forms
             * the crater with falling block physics.
             *
             * @param center the center location of the earthquake
             * @param caster the player who cast the spell
             * @param radius the radius of the earthquake effect
             * @param minDepth the minimum depth of the crater
             * @param maxDepth the maximum depth of the crater
             * @param kbOut the horizontal knockback strength
             * @param kbUp the vertical knockback strength
             * @param rng the random number generator for effects
             */
            private void quakeSmash(@NotNull Location center, @NotNull Player caster, int radius, int minDepth, int maxDepth,
                    double kbOut, double kbUp, @NotNull Random rng) {
                Objects.requireNonNull(center, "Center location cannot be null");
                Objects.requireNonNull(caster, "Caster cannot be null");
                Objects.requireNonNull(rng, "Random cannot be null");
                
                World w = center.getWorld();
                if (w == null) {
                    return;
                }

                // Heavy impact FX (realistic dust/explosion + local block dust)
                w.spawnParticle(Particle.EXPLOSION, center, 2, 0, 0, 0, 0.0);
                w.spawnParticle(Particle.EXPLOSION, center, 18, 1.2, 0.4, 1.2, 0.02);
                w.spawnParticle(Particle.LARGE_SMOKE, center, 90, 2.2, 0.8, 2.2, 0.0);
                w.spawnParticle(Particle.CLOUD, center, 120, 2.6, 0.9, 2.6, 0.02);

                Block under = center.clone().subtract(0, 1, 0).getBlock();
                BlockData dustData = under.getType().isAir() ? Material.DIRT.createBlockData() : under.getBlockData();
                // BLOCK particle uses BlockData as data-type (1.20+)
                w.spawnParticle(Particle.BLOCK, center, 220, 2.6, 1.1, 2.6, 0, dustData);

                // Low, heavy rumble
                w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.9f, 0.55f);
                w.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, SoundCategory.PLAYERS, 1.5f, 0.65f);
                w.playSound(center, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1.2f, 0.75f);
                w.playSound(center, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.8f, 0.55f);

                // Launch nearby entities (wave-like feeling with slight vertical boost)
                for (var e : w.getNearbyEntities(center, radius + 2, 5, radius + 2)) {
                    if (e instanceof LivingEntity living && !living.equals(caster) && living.isValid()
                            && !living.isDead()) {
                        Vector out = living.getLocation().toVector().subtract(center.toVector()).normalize();
                        Vector v = out.multiply(kbOut);
                        v.setY(Math.max(v.getY(), kbUp));
                        living.setVelocity(v);
                    }
                }

                // Crater: bowl shape (deeper toward center)
                final int r = Math.max(2, radius);
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        double dist = Math.sqrt(x * x + z * z);
                        if (dist > r)
                            continue;

                        double t = 1.0 - (dist / r); // 1 at center -> 0 at edge
                        int depth = (int) Math.round(minDepth + t * (maxDepth - minDepth));

                        // Wave-like excavation: begin slightly above center so surface moves
                        int surfaceY = center.getBlockY();

                        for (int dy = 0; dy < depth; dy++) {
                            Block b = w.getBlockAt(center.getBlockX() + x, surfaceY - dy, center.getBlockZ() + z);
                            Material type = b.getType();
                            if (type.isAir())
                                continue;

                            BlockData data = b.getBlockData();

                            // Outward velocity + slight random lift
                            Vector out = new Vector(x, 0, z).normalize();
                            double speed = 0.25 + rng.nextDouble() * 0.45; // 0.25..0.7
                            Vector vel = out.multiply(speed).add(new Vector(0, 0.25 + rng.nextDouble() * 0.4, 0));

                            // Modern entity spawn without deprecated overloads
                            w.spawn(b.getLocation().add(0.5, 0.5, 0.5), FallingBlock.class, fb -> {
                                fb.setBlockData(data);
                                fb.setDropItem(false); // no items
                                fb.setHurtEntities(false); // no entity damage
                                fb.setVelocity(vel);
                            });

                            // Remove real block (griefing allowed)
                            b.setType(Material.AIR, false);
                        }
                    }
                }

                // Aftershock (subtle)
                w.spawnParticle(Particle.LARGE_SMOKE, center, 40, 2.0, 0.6, 2.0, 0.0);
                w.playSound(center, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 0.9f, 0.7f);
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);

        return null;
    }

    /**
     * Handles the spell effect after execution.
     * <p>
     * This spell's effects are applied during execution.
     *
     * @param context the spell context
     * @param result the result of the spell execution (always null for this spell)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // visuals/logic are in executeSpell
    }

    /**
     * Checks if a player is grounded using block passability.
     * <p>
     * This is a modern alternative to the deprecated isOnGround() method.
     *
     * @param player the player to check
     * @return true if the player is grounded, false otherwise
     */
    private static boolean isGrounded(@NotNull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        
        Location loc = player.getLocation();
        Block below = loc.clone().subtract(0, 0.2, 0).getBlock();
        return !below.isPassable();
    }
}
