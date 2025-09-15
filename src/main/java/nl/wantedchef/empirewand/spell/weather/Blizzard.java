package nl.wantedchef.empirewand.spell.weather;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
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

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.util.SpellUtils;

/**
 * A devastating, cinematic ice spell that unleashes a roaring blizzard of epic proportions,
 * freezing the battlefield with crystalline ice structures, crushing enemies with frost damage,
 * and summoning a howling storm that bends reality.
 * <p>
 * This is no ordinary snowstorm — it's an ancient glacial wrath made manifest.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Dynamic, multi-layered particle storms (snow, ice shards, frost waves)</li>
 *   <li>Cinematic ice crystal growth with realistic block transformation</li>
 *   <li>Powerful directional wind blasts that fling entities like leaves</li>
 *   <li>Pulsating frost aura around caster</li>
 *   <li>Ice shatters with glittering debris on cleanup</li>
 *   <li>Deep, immersive sound design: distant thunder → howling wind → icy crack</li>
 *   <li>Periodic damage with chilling visual feedback</li>
 *   <li>Frozen ground glows faintly blue during effect</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell blizzard = new Blizzard.Builder(api)
 *     .name("Glacial Wrath")
 *     .description("Unleash the fury of the frozen heavens. The earth freezes, the wind screams, and all who stand are shattered.")
 *     .cooldown(Duration.ofSeconds(60))
 *     .build();
 * }</pre>
 *
 * @since 1.5.0
 */
public class Blizzard extends Spell<Void> {

    /**
     * Configuration record for enhanced Blizzard spell settings.
     */
    private record Config(
        double radius,
        double damage,
        int slowDuration,
        int slowAmplifier,
        int durationTicks,
        boolean createIce,
        int iceGrowthAttempts,
        int particleDensity,
        double windForce,
        boolean pulseLightning
    ) {}

    /**
     * Default configuration values — tuned for maximum impact.
     */
    private static final double DEFAULT_RADIUS = 25.0;
    private static final double DEFAULT_DAMAGE = 2.5;
    private static final int DEFAULT_SLOW_DURATION = 80;
    private static final int DEFAULT_SLOW_AMPLIFIER = 4;
    private static final int DEFAULT_DURATION_TICKS = 180; // 9 seconds
    private static final boolean DEFAULT_CREATE_ICE = true;
    private static final int DEFAULT_ICE_GROWTH_ATTEMPTS = 25;
    private static final int DEFAULT_PARTICLE_DENSITY = 15; // Reduced from 50 for performance
    private static final double DEFAULT_WIND_FORCE = 1.2;
    private static final boolean DEFAULT_PULSE_LIGHTNING = true;

    /**
     * Current spell configuration.
     */
    private Config config = new Config(
        DEFAULT_RADIUS,
        DEFAULT_DAMAGE,
        DEFAULT_SLOW_DURATION,
        DEFAULT_SLOW_AMPLIFIER,
        DEFAULT_DURATION_TICKS,
        DEFAULT_CREATE_ICE,
        DEFAULT_ICE_GROWTH_ATTEMPTS,
        DEFAULT_PARTICLE_DENSITY,
        DEFAULT_WIND_FORCE,
        DEFAULT_PULSE_LIGHTNING
    );

    /**
     * Builder for creating enhanced Blizzard spell instances.
     */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Glacial Wrath";
            this.description = "Unleash the fury of the frozen heavens. The earth freezes, the wind screams, and all who stand are shattered.";
            this.cooldown = Duration.ofSeconds(60);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Blizzard(this);
        }
    }

    /**
     * Constructs a new enhanced Blizzard spell instance.
     */
    private Blizzard(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Loads configuration from file with enhanced defaults.
     */
    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        Objects.requireNonNull(spellConfig, "SpellConfig cannot be null");
        super.loadConfig(spellConfig);

        this.config = new Config(
            SpellUtils.getConfigDouble(spellConfig, "values.radius", DEFAULT_RADIUS),
            SpellUtils.getConfigDouble(spellConfig, "values.damage", DEFAULT_DAMAGE),
            SpellUtils.getConfigInt(spellConfig, "values.slow-duration-ticks", DEFAULT_SLOW_DURATION),
            SpellUtils.getConfigInt(spellConfig, "values.slow-amplifier", DEFAULT_SLOW_AMPLIFIER),
            SpellUtils.getConfigInt(spellConfig, "values.duration-ticks", DEFAULT_DURATION_TICKS),
            SpellUtils.getConfigBoolean(spellConfig, "flags.create-ice", DEFAULT_CREATE_ICE),
            SpellUtils.getConfigInt(spellConfig, "values.ice-growth-attempts", DEFAULT_ICE_GROWTH_ATTEMPTS),
            SpellUtils.getConfigInt(spellConfig, "values.particle-density", DEFAULT_PARTICLE_DENSITY),
            SpellUtils.getConfigDouble(spellConfig, "values.wind-force", DEFAULT_WIND_FORCE),
            SpellUtils.getConfigBoolean(spellConfig, "flags.pulse-lightning", DEFAULT_PULSE_LIGHTNING)
        );
    }

    /**
     * Unique key for this spell.
     */
    @Override
    @NotNull
    public String key() {
        return "blizzard";
    }

    /**
     * No prerequisites beyond standard casting.
     */
    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Executes the spell with cinematic flair.
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");

        Player player = context.caster();
        World world = player.getWorld();

        // Epic initial sound: low rumble building up
        world.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 3.0f, 0.4f);
        world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 1.8f);

        // Create pulsing frost aura around caster
        new FrostAuraTask(player).runTaskTimer(context.plugin(), 0L, 2L);

        // Start main blizzard task
        BlizzardTask blizzardTask = new BlizzardTask(
            context, player.getLocation(),
            config.radius, config.damage, config.slowDuration, config.slowAmplifier,
            config.durationTicks, config.createIce, config.iceGrowthAttempts,
            config.particleDensity, config.windForce, config.pulseLightning
        );

        context.plugin().getTaskManager().runTaskTimer(blizzardTask, 0L, 1L); // 1-tick interval for smoothness
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled entirely in BlizzardTask
    }

    /**
     * A cinematic, high-performance task managing every aspect of the blizzard.
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
        private final int iceGrowthAttempts;
        private final int particleDensity;
        private final double windForce;
        private final boolean pulseLightning;

        private final World world;
        private final Random random = ThreadLocalRandom.current();

        // Performance optimization: cache entity lookups
        private static final int ENTITY_CACHE_DURATION = 4; // Cache for 4 ticks
        private Collection<LivingEntity> cachedEntities = new ArrayList<>();
        private long lastEntityCacheTime = 0;
        private final Set<Location> iceBlocks = new HashSet<>();
        private final Map<Block, BlockData> originalBlocks = new HashMap<>();
        private int ticks = 0;
        private boolean hasPlayedWindCrescendo = false;

        public BlizzardTask(
                @NotNull SpellContext context, @NotNull Location center,
                double radius, double damage, int slowDuration, int slowAmplifier,
                int durationTicks, boolean createIce, int iceGrowthAttempts,
                int particleDensity, double windForce, boolean pulseLightning) {

            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.radius = radius;
            this.damage = damage;
            this.slowDuration = slowDuration;
            this.slowAmplifier = slowAmplifier;
            this.durationTicks = durationTicks;
            this.createIce = createIce;
            this.iceGrowthAttempts = iceGrowthAttempts;
            this.particleDensity = particleDensity;
            this.windForce = windForce;
            this.pulseLightning = pulseLightning;
            this.world = center.getWorld();
        }

        @Override
        public void run() {
            if (world == null || !world.isChunkLoaded(center.getChunk())) {
                cancel();
                return;
            }

            // End condition
            if (ticks >= durationTicks) {
                cleanupIce();
                cancel();
                return;
            }

            // === VISUAL & AUDIO CINEMATICS ===

            // 1. Pulsing core glow (every 5 ticks)
            if (ticks % 5 == 0) {
                spawnPulseParticles();
            }

            // 2. Intensify wind at halfway point
            if (!hasPlayedWindCrescendo && ticks >= durationTicks / 2) {
                hasPlayedWindCrescendo = true;
                world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 4.0f, 0.7f);
                world.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.5f, 1.5f);
            }

            // 3. Spawn particle storm (throttled for performance)
            if (ticks % 2 == 0) { // Only every other tick
                createBlizzardEffects();
            }

            // 4. Apply damage & slowness every 4 ticks for performance
            if (ticks % 4 == 0) {
                applyBlizzardEffects();
            }

            // 5. Grow ice crystals every 2 ticks
            if (createIce && ticks % 2 == 0) {
                createIceCrystals();
            }

            // 6. Pulse lightning spikes (optional)
            if (pulseLightning && ticks % 7 == 0) {
                spawnLightningSpikes();
            }

            ticks++;
        }

        /**
         * Creates layered, cinematic particle effects.
         */
        private void createBlizzardEffects() {
            if (world == null) return;

            // Base snowstorm
            for (int i = 0; i < particleDensity; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = random.nextDouble() * radius;
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                double y = center.getY() + random.nextDouble() * 12 - 6; // Vertical spread

                Location loc = new Location(world, x, y, z);

                // Layer 1: Snowflakes
                world.spawnParticle(Particle.SNOWFLAKE, loc, 1, 0, 0, 0, 0.01);
                // Layer 2: Ice shards (glittering)
                world.spawnParticle(Particle.CRIT, loc, 1, 0, 0, 0, 0.02);
                // Layer 3: Frost wave (slow-moving)
                world.spawnParticle(Particle.SNOWFLAKE, loc, 1, 0.1, 0.1, 0.1, 0.005);
            }

            // Wind push: less frequent for performance, uses cached entities
            if (ticks % 3 == 0) { // Only every 3 ticks
                double windMultiplier = 1.0 + (ticks * 0.01); // Slower increase
                // Reuse cached entities from damage application for wind effects
                for (LivingEntity entity : cachedEntities) {
                    if (entity.equals(context.caster()) || entity.isDead() || !entity.isValid()) continue;
                    // Additional distance check since cached entities might be slightly outdated
                    if (entity.getLocation().distance(center) > radius) continue;

                    Vector direction = new Vector(
                        random.nextDouble() - 0.5,
                        random.nextDouble() * 0.2, // Reduced upward bias
                        random.nextDouble() - 0.5
                    ).normalize().multiply(windMultiplier * windForce * 0.7); // Reduced force

                    entity.setVelocity(entity.getVelocity().add(direction));
                    // Removed particle spawn from wind effect for performance
                }
            }
        }

        /**
         * Applies damage and slowness to all affected entities with caching optimization.
         */
        private void applyBlizzardEffects() {
            if (world == null) return;

            // Use cached entities if still valid, otherwise refresh
            Collection<LivingEntity> affectedEntities;
            if (ticks - lastEntityCacheTime >= ENTITY_CACHE_DURATION) {
                affectedEntities = world.getNearbyLivingEntities(center, radius, 10, radius);
                cachedEntities = new ArrayList<>(affectedEntities); // Cache the result
                lastEntityCacheTime = ticks;
            } else {
                affectedEntities = cachedEntities;
            }

            for (LivingEntity entity : affectedEntities) {
                if (entity.equals(context.caster()) || entity.isDead() || !entity.isValid()) continue;

                // Damage
                entity.damage(damage, context.caster());

                // Slowness — always applied, even if already present (refreshes duration)
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, false));

                // Visual feedback: Frost trail behind entity
                Location loc = entity.getLocation().add(0, 0.5, 0);
                world.spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.1, 0.1, 0.1, 0.01);
                world.spawnParticle(Particle.SNOWFLAKE, loc, 2, 0, 0, 0, 0);

                // Chill aura around entity
                if (ticks % 3 == 0) {
                    world.spawnParticle(Particle.DRIPPING_WATER, loc, 1, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }

        /**
         * Creates stunning, crystalline ice structures — not just flat blocks.
         * Ice grows upward like frozen spikes or columns.
         */
        private void createIceCrystals() {
            if (world == null || !createIce) return;

            for (int i = 0; i < iceGrowthAttempts; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = random.nextDouble() * (radius - 5);
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;

                // Find ground level
                Location groundLoc = new Location(world, x, center.getY(), z);
                int groundY = world.getHighestBlockYAt(groundLoc);

                // Only grow ice where there's solid ground
                Block baseBlock = world.getBlockAt((int)x, groundY, (int)z);
                Material baseType = baseBlock.getType();

                if (!baseType.isSolid() || baseType.isAir() || baseType == Material.BEDROCK) continue;

                // Skip if already ice
                if (baseType == Material.ICE || baseType == Material.PACKED_ICE || baseType == Material.BLUE_ICE) continue;

                // Transform into ice over multiple layers
                int height = 1 + random.nextInt(3); // 1-4 blocks tall
                for (int h = 0; h < height; h++) {
                    Location loc = new Location(world, x, groundY + h, z);
                    Block block = loc.getBlock();

                    if (block.getType().isAir() || block.getType() == Material.ICE || block.getType() == Material.PACKED_ICE) {
                        // Store original if first layer
                        if (h == 0 && !originalBlocks.containsKey(block)) {
                            originalBlocks.put(block, block.getBlockData());
                        }

                        // Transform to ice
                        block.setType(Material.ICE);

                        // Add to tracked set
                        iceBlocks.add(loc);

                        // Particle effect: ice growing
                        if (h == 0) {
                            world.spawnParticle(Particle.BLOCK, loc.add(0.5, 0.5, 0.5), 8, 0.1, 0.1, 0.1, 0.01, Material.ICE.createBlockData());
                        } else {
                            world.spawnParticle(Particle.SNOWFLAKE, loc.add(0.5, 0.5, 0.5), 3, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }

        /**
         * Creates a pulsing, radiant glow at the center of the blizzard.
         */
        private void spawnPulseParticles() {
            if (world == null) return;

            // Pulse light ring
            for (int i = 0; i < 24; i++) {
                double angle = (i / 24.0) * Math.PI * 2;
                double x = center.getX() + Math.cos(angle) * (radius * 0.8);
                double z = center.getZ() + Math.sin(angle) * (radius * 0.8);
                double y = center.getY() + 2;

                Location loc = new Location(world, x, y, z);
                world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0.01, new Particle.DustOptions(Color.AQUA, 1.5f));
            }

            // Center glow
            world.spawnParticle(Particle.ENCHANT, center.clone().add(0, 1, 0), 10, 0.1, 0.1, 0.1, 0.01);
            world.spawnParticle(Particle.FLAME, center.clone().add(0, 0.5, 0), 5, 0.1, 0.1, 0.1, 0);
        }

        /**
         * Spawns dramatic lightning-like frost spikes radiating outward.
         */
        private void spawnLightningSpikes() {
            if (world == null) return;

            for (int i = 0; i < 5; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = radius * 0.8;
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                double y = center.getY() + 3;

                Location start = new Location(world, center.getX(), center.getY() + 3, center.getZ());
                Location end = new Location(world, x, y, z);

                // Draw line of frost particles
                Vector direction = end.toVector().subtract(start.toVector()).normalize();
                for (double d = 0; d <= 1.0; d += 0.1) {
                    Location pos = start.clone().add(direction.clone().multiply(d));
                    world.spawnParticle(Particle.SNOWFLAKE, pos, 1, 0, 0, 0, 0.01);
                    world.spawnParticle(Particle.CRIT, pos, 1, 0, 0, 0, 0);
                }

                // Final spark
                world.spawnParticle(Particle.EXPLOSION, end, 1, 0, 0, 0, 0);
                world.playSound(end, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.2f);
            }
        }

        /**
         * Cleans up the blizzard with a dramatic, cinematic finale.
         */
        private void cleanupIce() {
            if (world == null) return;

            // Play epic cleanup sound
            world.playSound(center, Sound.ENTITY_WITHER_DEATH, 5.0f, 0.5f);
            world.playSound(center, Sound.BLOCK_GLASS_BREAK, 3.0f, 1.8f);

            // Shatter ice crystals with glittering debris
            for (Location loc : iceBlocks) {
                Block block = world.getBlockAt(loc);
                if (block.getType() == Material.ICE || block.getType() == Material.PACKED_ICE) {
                    BlockData original = originalBlocks.get(block);
                    if (original != null) {
                        // Before replacing, spawn ice shard explosion
                        for (int i = 0; i < 15; i++) {
                            double offsetX = random.nextDouble() * 0.8 - 0.4;
                            double offsetY = random.nextDouble() * 0.8 - 0.4;
                            double offsetZ = random.nextDouble() * 0.8 - 0.4;
                            Location particleLoc = loc.clone().add(offsetX, offsetY, offsetZ);
                            world.spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0.01);
                            world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0.01);
                        }
                        block.setBlockData(original);
                    } else {
                        block.setType(Material.AIR);
                    }
                }
            }

            // Final flash of frost
            for (int i = 0; i < 50; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = random.nextDouble() * radius;
                double x = center.getX() + Math.cos(angle) * dist;
                double z = center.getZ() + Math.sin(angle) * dist;
                double y = center.getY() + random.nextDouble() * 10;

                Location loc = new Location(world, x, y, z);
                world.spawnParticle(Particle.SNOWFLAKE, loc, 2, 0, 0, 0, 0.01);
                world.spawnParticle(Particle.SNOWFLAKE, loc, 1, 0, 0, 0, 0);
            }

            // Fade out lingering particles
            world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
        }
    }

    /**
     * Optional: Adds a subtle, persistent frost aura around the caster.
     */
    private static class FrostAuraTask extends BukkitRunnable {
        private final Player player;
        private final World world;
        private int phase = 0;

        public FrostAuraTask(Player player) {
            this.player = player;
            this.world = player.getWorld();
        }

        @Override
        public void run() {
            if (player == null || !player.isValid() || player.isDead() || world == null) {
                cancel();
                return;
            }

            Location loc = player.getLocation();
            double radius = 3.0;

            // Alternate between two aura patterns
            phase = (phase + 1) % 2;

            for (int i = 0; i < 12; i++) {
                double angle = (i / 12.0) * Math.PI * 2;
                double x = loc.getX() + Math.cos(angle) * radius;
                double z = loc.getZ() + Math.sin(angle) * radius;
                double y = loc.getY() + (phase == 0 ? 0.5 : 1.2);

                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0.01);
                world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
            }

            // Glow effect under feet
            world.spawnParticle(Particle.DRIPPING_WATER, loc.clone().add(0, -0.5, 0), 8, 0.2, 0.2, 0.2, 0.01);
        }
    }
}