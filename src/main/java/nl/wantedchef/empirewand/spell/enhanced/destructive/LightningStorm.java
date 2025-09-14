package nl.wantedchef.empirewand.spell.enhanced.destructive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A devastating spell that summons a lightning storm over a large area,
 * with chain lightning that jumps between enemies.
 * <p>
 * This spell creates a lightning storm that strikes multiple enemies within a large radius.
 * Each lightning strike can chain to additional nearby enemies, dealing damage to each target
 * in the chain. The spell includes visual effects for lightning arcs and audio feedback for
 * thunder sounds.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Area of effect lightning strikes</li>
 *   <li>Chain lightning that jumps between enemies</li>
 *   <li>Configurable number of lightning strikes</li>
 *   <li>Random ground strikes within the area</li>
 *   <li>Lightning arc visual effects</li>
 *   <li>Audio feedback with thunder sounds</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell lightningStorm = new LightningStorm.Builder(api)
 *     .name("Lightning Storm")
 *     .description("Summons a devastating lightning storm that strikes multiple enemies with chain lightning.")
 *     .cooldown(Duration.ofSeconds(50))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class LightningStorm extends Spell<Void> {

    /**
     * Configuration record containing all LightningStorm spell settings.
     * <p>
     * This record holds all configuration values loaded from the spell configuration.
     * Using a record provides immutable configuration access and better performance
     * compared to repeated map lookups.
     *
     * @param radius        the radius of the lightning storm effect
     * @param lightningCount the number of lightning bolts to spawn
     * @param damage        the damage dealt by each lightning strike
     * @param chainJumps    the maximum number of times lightning can chain
     * @param chainRadius   the radius for chaining lightning between entities
     * @param durationTicks the duration of the storm in ticks
     */
    private record Config(
        double radius,
        int lightningCount,
        double damage,
        int chainJumps,
        double chainRadius,
        int durationTicks
    ) {}

    /**
     * Default configuration values.
     */
    private static final double DEFAULT_RADIUS = 25.0;
    private static final int DEFAULT_LIGHTNING_COUNT = 15;
    private static final double DEFAULT_DAMAGE = 8.0;
    private static final int DEFAULT_CHAIN_JUMPS = 3;
    private static final double DEFAULT_CHAIN_RADIUS = 6.0;
    private static final int DEFAULT_DURATION_TICKS = 120;

    /**
     * Current spell configuration.
     */
    private Config config = new Config(
        DEFAULT_RADIUS,
        DEFAULT_LIGHTNING_COUNT,
        DEFAULT_DAMAGE,
        DEFAULT_CHAIN_JUMPS,
        DEFAULT_CHAIN_RADIUS,
        DEFAULT_DURATION_TICKS
    );

    /**
     * Builder for creating LightningStorm spell instances.
     * <p>
     * Provides a fluent API for configuring the lightning storm spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new LightningStorm spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Lightning Storm";
            this.description = "Summons a devastating lightning storm that strikes multiple enemies with chain lightning.";
            this.cooldown = Duration.ofSeconds(50);
            this.spellType = SpellType.LIGHTNING;
        }

        /**
         * Builds and returns a new LightningStorm spell instance.
         *
         * @return the constructed LightningStorm spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new LightningStorm(this);
        }
    }

    /**
     * Constructs a new LightningStorm spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private LightningStorm(@NotNull Builder builder) {
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
            spellConfig.getInt("values.lightning-count", DEFAULT_LIGHTNING_COUNT),
            spellConfig.getDouble("values.damage", DEFAULT_DAMAGE),
            spellConfig.getInt("values.chain-jumps", DEFAULT_CHAIN_JUMPS),
            spellConfig.getDouble("values.chain-radius", DEFAULT_CHAIN_RADIUS),
            spellConfig.getInt("values.duration-ticks", DEFAULT_DURATION_TICKS)
        );
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "lightning-storm"
     */
    @Override
    @NotNull
    public String key() {
        return "lightning-storm";
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
     * Executes the lightning storm spell logic.
     * <p>
     * This method creates a lightning storm effect that strikes multiple enemies
     * within a radius and chains lightning between nearby entities.
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
        int lightningCount = config.lightningCount;
        double damage = config.damage;
        int chainJumps = config.chainJumps;
        double chainRadius = config.chainRadius;
        int durationTicks = config.durationTicks;

        // Find all entities in radius
        List<LivingEntity> potentialTargets = new ArrayList<>();
        if (world != null) {
            potentialTargets = world.getNearbyLivingEntities(player.getLocation(), radius, radius, radius)
                    .stream()
                    .filter(entity -> !entity.equals(player))
                    .filter(entity -> !entity.isDead() && entity.isValid())
                    .collect(Collectors.toList());
        }

        // Play initial sound
        if (world != null) {
            world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.5f);
        }

        // Start lightning storm effect
        context.plugin().getTaskManager().runTaskTimer(
            new LightningStormTask(context, player.getLocation(), potentialTargets, lightningCount, damage, chainJumps, chainRadius, durationTicks),
            0L, 3L
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
     * A runnable that handles the lightning storm's effects over time.
     * <p>
     * This task manages the lightning strikes, chaining effects, and visual feedback.
     */
    private static class LightningStormTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final List<LivingEntity> potentialTargets;
        private final int lightningCount;
        private final double damage;
        private final int chainJumps;
        private final double chainRadius;
        private final int durationTicks;
        private final Random random = new Random();
        private int ticks = 0;
        private int lightningsSpawned = 0;
        private final Set<LivingEntity> struckEntities = new HashSet<>();

        /**
         * Creates a new LightningStormTask instance.
         *
         * @param context the spell context
         * @param center the center location of the lightning storm
         * @param potentialTargets the list of potential targets for lightning strikes
         * @param lightningCount the number of lightning strikes to spawn
         * @param damage the damage to apply to each struck entity
         * @param chainJumps the number of times lightning can chain to new targets
         * @param chainRadius the radius within which lightning can chain to new targets
         * @param durationTicks the duration of the lightning storm in ticks
         */
        public LightningStormTask(@NotNull SpellContext context, @NotNull Location center, @NotNull List<LivingEntity> potentialTargets, 
                                  int lightningCount, double damage, int chainJumps, double chainRadius, int durationTicks) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.potentialTargets = new ArrayList<>(potentialTargets);
            this.lightningCount = lightningCount;
            this.damage = damage;
            this.chainJumps = chainJumps;
            this.chainRadius = chainRadius;
            this.durationTicks = durationTicks;
        }

        /**
         * Runs the lightning storm task, spawning lightning strikes at random intervals.
         */
        @Override
        public void run() {
            var world = center.getWorld();
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= durationTicks || lightningsSpawned >= lightningCount) {
                this.cancel();
                return;
            }

            // Spawn lightning at random intervals
            if (ticks % 8 == 0 && lightningsSpawned < lightningCount && !potentialTargets.isEmpty()) {
                // Select a random target or random location
                LivingEntity target = null;
                Location lightningLocation;
                
                if (!potentialTargets.isEmpty() && random.nextBoolean()) {
                    target = potentialTargets.get(random.nextInt(potentialTargets.size()));
                    lightningLocation = target.getLocation();
                } else {
                    // Random location within radius
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double distance = random.nextDouble() * 20;
                    double x = center.getX() + Math.cos(angle) * distance;
                    double z = center.getZ() + Math.sin(angle) * distance;
                    lightningLocation = new Location(center.getWorld(), x, center.getWorld().getHighestBlockYAt((int)x, (int)z) + 1, z);
                }

                strikeLightning(lightningLocation, target);
                lightningsSpawned++;
            }

            ticks++;
        }

        /**
         * Strikes lightning at a specific location and applies damage to an initial target.
         * <p>
         * This method creates a lightning strike and initiates chain lightning effects
         * if an initial target is provided.
         *
         * @param location the location to strike lightning
         * @param initialTarget the initial target entity, or null for ground strike
         */
        private void strikeLightning(@NotNull Location location, @Nullable LivingEntity initialTarget) {
            Objects.requireNonNull(location, "Location cannot be null");
            
            World world = location.getWorld();
            if (world == null) return;

            // Strike initial lightning
            world.strikeLightning(location);
            
            // Damage initial target if present
            if (initialTarget != null && !struckEntities.contains(initialTarget) && initialTarget.isValid() && !initialTarget.isDead()) {
                initialTarget.damage(damage, context.caster());
                context.fx().spawnParticles(initialTarget.getLocation(), Particle.ELECTRIC_SPARK, 20, 0.3, 0.6, 0.3, 0.1);
                struckEntities.add(initialTarget);
                
                // Chain lightning to nearby entities
                chainLightning(initialTarget);
            } else {
                // Visual effect for ground strike
                context.fx().spawnParticles(location, Particle.ELECTRIC_SPARK, 30, 1, 1, 1, 0.1);
            }

            // Visual effects
            context.fx().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
        }

        /**
         * Chains lightning from a current entity to nearby entities.
         * <p>
         * This method creates lightning arcs between entities and applies damage to
         * each entity in the chain.
         *
         * @param current the entity to chain lightning from
         */
        private void chainLightning(@NotNull LivingEntity current) {
            Objects.requireNonNull(current, "Current entity cannot be null");
            
            Set<LivingEntity> hit = new HashSet<>();
            hit.add(current);
            
            LivingEntity chainCurrent = current;
            
            for (int i = 0; i < chainJumps && chainCurrent != null; i++) {
                final LivingEntity finalChainCurrent = chainCurrent;
                LivingEntity next = current.getWorld().getNearbyLivingEntities(chainCurrent.getLocation(), chainRadius).stream()
                        .filter(e -> !hit.contains(e) && !e.equals(context.caster()) && e.isValid() && !e.isDead())
                        .min(Comparator.comparingDouble(l -> l.getLocation().distanceSquared(finalChainCurrent.getLocation())))
                        .orElse(null);

                if (next != null) {
                    var currentLocation = chainCurrent.getEyeLocation();
                    var nextLocation = next.getEyeLocation();
                    
                    if (currentLocation != null && nextLocation != null) {
                        // Render arc between entities
                        renderArc(currentLocation, nextLocation);
                    }
                    
                    // Damage the next entity
                    next.damage(damage * (0.7 * (i + 1)), context.caster());
                    context.fx().spawnParticles(next.getLocation(), Particle.ELECTRIC_SPARK, 15, 0.3, 0.6, 0.3, 0.1);
                    
                    hit.add(next);
                    chainCurrent = next;
                } else {
                    chainCurrent = null;
                }
            }
        }

        /**
         * Renders a lightning arc between two locations.
         * <p>
         * This method creates a zigzag pattern of electric spark particles between
         * the from and to locations to visualize the lightning arc.
         *
         * @param from the starting location of the arc
         * @param to the ending location of the arc
         */
        private void renderArc(@NotNull Location from, @NotNull Location to) {
            Objects.requireNonNull(from, "From location cannot be null");
            Objects.requireNonNull(to, "To location cannot be null");
            
            Vector full = to.toVector().subtract(from.toVector());
            double length = full.length();
            if (length < 0.01) return;

            Vector step = full.clone().multiply(1.0 / 12);
            Location cursor = from.clone();

            for (int i = 0; i <= 12; i++) {
                Location point = cursor.clone();
                if (i != 0 && i != 12) {
                    point.add((Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3);
                }
                
                var world = from.getWorld();
                if (world != null) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, point, 3, 0.05, 0.05, 0.05, 0.02);
                }
                cursor.add(step);
            }
        }
    }
}