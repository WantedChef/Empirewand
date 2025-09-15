package nl.wantedchef.empirewand.spell.weather;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates a devastating hailstorm with realistic physics and immersive effects:
 * - Variable-sized hailstones with realistic falling physics and terminal velocity
 * - Dynamic impact effects with surface-dependent sounds and particle bursts
 * - Atmospheric storm effects including wind sounds and cloud formations
 * - Area damage visualization with danger zones and impact predictions
 * - Performance-optimized particle batching and async calculations
 * - Configurable storm intensity, duration, and hailstone characteristics
 *
 * @author WantedChef
 */
public class HailStorm extends Spell<Void> {

    /**
     * Represents a single hailstone with physical properties.
     */
    private static class Hailstone {
        final Location location;
        final Vector velocity;
        final double size;
        final double mass;
        final double damage;
        boolean hasImpacted = false;

        Hailstone(Location location, double size, double damage) {
            this.location = location.clone();
            this.size = size;
            this.mass = Math.pow(size, 3) * 0.1; // Mass scales with volume
            this.damage = damage;

            // Terminal velocity calculation (realistic physics)
            double terminalVelocity = Math.sqrt((2 * mass * 9.81) / (1.225 * 0.47 * Math.PI * Math.pow(size, 2)));
            terminalVelocity = Math.min(terminalVelocity * 0.1, 2.5); // Scale for game balance

            // Random wind effect
            double windX = ThreadLocalRandom.current().nextGaussian() * 0.1;
            double windZ = ThreadLocalRandom.current().nextGaussian() * 0.1;
            this.velocity = new Vector(windX, -terminalVelocity, windZ);
        }
    }

    /**
     * Builder for {@link HailStorm}.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a builder for the spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Hail Storm";
            this.description = "Creates a devastating hailstorm with realistic physics, dynamic impacts, and immersive atmospheric effects.";
            this.cooldown = Duration.ofMillis(8000);
            this.spellType = SpellType.WEATHER;
        }

        /**
         * Builds the spell instance.
         *
         * @return a new {@link HailStorm}
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new HailStorm(this);
        }
    }

    private final Map<UUID, List<Hailstone>> activeStorms = new ConcurrentHashMap<>();
    private final Set<Location> impactZones = ConcurrentHashMap.newKeySet();

    private HailStorm(Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String key() {
        return "hailstorm";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Creates a realistic hailstorm with physics-based hailstones, atmospheric effects,
     * and dynamic impact visualization.
     *
     * @param context the spell context
     * @return always {@code null}
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        World world = player.getWorld();
        Location centerLoc = player.getLocation();

        // Configuration values with enhanced defaults
        double radius = spellConfig.getDouble("values.radius", 8.0);
        double baseDamage = spellConfig.getDouble("values.damage", 3.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 120); // 6 seconds
        int hailstoneCount = spellConfig.getInt("values.hailstone-count", 25);
        double maxHailSize = spellConfig.getDouble("values.max-hail-size", 0.8);
        double minHailSize = spellConfig.getDouble("values.min-hail-size", 0.2);
        boolean createWindEffect = spellConfig.getBoolean("effects.wind-effect", true);
        boolean showDamageZones = spellConfig.getBoolean("effects.damage-zones", true);

        UUID stormId = UUID.randomUUID();
        List<Hailstone> hailstones = new ArrayList<>();

        // Pre-calculate storm area and create atmospheric effects
        createStormAtmosphere(context, centerLoc, radius, durationTicks);

        // Generate initial hailstones at varying heights
        for (int i = 0; i < hailstoneCount; i++) {
            // Random position within radius
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double distance = ThreadLocalRandom.current().nextDouble() * radius;
            double x = centerLoc.getX() + Math.cos(angle) * distance;
            double z = centerLoc.getZ() + Math.sin(angle) * distance;

            // Spawn hailstones at various heights for staggered impact
            double height = centerLoc.getY() + ThreadLocalRandom.current().nextDouble(15, 30);

            // Variable hailstone size affects damage and physics
            double hailSize = ThreadLocalRandom.current().nextDouble(minHailSize, maxHailSize);
            double hailDamage = baseDamage * (hailSize / maxHailSize) * ThreadLocalRandom.current().nextDouble(0.8, 1.2);

            Location hailLoc = new Location(world, x, height, z);
            Hailstone hailstone = new Hailstone(hailLoc, hailSize, hailDamage);
            hailstones.add(hailstone);
        }

        activeStorms.put(stormId, hailstones);

        // Start the physics simulation and effect loop
        startHailstormSimulation(context, stormId, durationTicks, showDamageZones);

        // Play initial storm sounds
        context.fx().playSound(centerLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.8f);
        context.fx().playSound(centerLoc, Sound.WEATHER_RAIN_ABOVE, 1.2f, 1.0f);

        if (createWindEffect) {
            createWindEffects(context, centerLoc, radius, durationTicks);
        }

        return null;
    }

    /**
     * Creates atmospheric storm effects including dark clouds and wind particles.
     */
    private void createStormAtmosphere(SpellContext context, Location center, double radius, int durationTicks) {
        World world = center.getWorld();
        if (world == null) return;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }

                // Create storm cloud particles above the area
                for (int i = 0; i < 8; i++) {
                    double angle = (ticks * 0.1 + i * Math.PI / 4);
                    double x = center.getX() + Math.cos(angle) * radius * 0.8;
                    double z = center.getZ() + Math.sin(angle) * radius * 0.8;
                    double y = center.getY() + 25 + Math.sin(ticks * 0.05) * 3;

                    Location cloudLoc = new Location(world, x, y, z);
                    context.fx().batchParticles(cloudLoc, Particle.CLOUD, 3, 2.0, 1.0, 2.0, 0.02);
                    context.fx().batchParticles(cloudLoc, Particle.SMOKE, 2, 1.5, 0.5, 1.5, 0.01);
                }

                // Periodic thunder rumbles
                if (ticks % 40 == 0) {
                    context.fx().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 0.6f);
                }

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * Creates wind particle effects around the storm perimeter.
     */
    private void createWindEffects(SpellContext context, Location center, double radius, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }

                // Create swirling wind particles
                for (int i = 0; i < 12; i++) {
                    double angle = ticks * 0.05 + i * Math.PI / 6;
                    double windRadius = radius * 1.1;
                    double x = center.getX() + Math.cos(angle) * windRadius;
                    double z = center.getZ() + Math.sin(angle) * windRadius;
                    double y = center.getY() + ThreadLocalRandom.current().nextDouble(1, 4);

                    Location windLoc = new Location(center.getWorld(), x, y, z);

                    // Wind velocity towards the storm center
                    Vector windVel = center.toVector().subtract(windLoc.toVector()).normalize().multiply(0.3);
                    context.fx().batchParticles(windLoc, Particle.FALLING_DUST, 1,
                        windVel.getX(), windVel.getY(), windVel.getZ(), 0.1, Material.GRAY_CONCRETE.createBlockData());
                }

                // Wind sound effects
                if (ticks % 60 == 0) {
                    context.fx().playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.4f, 0.3f);
                }

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    /**
     * Starts the main hailstorm simulation with physics and impact detection.
     */
    private void startHailstormSimulation(SpellContext context, UUID stormId, int maxTicks, boolean showDamageZones) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                List<Hailstone> hailstones = activeStorms.get(stormId);
                if (hailstones == null || ticks >= maxTicks) {
                    cleanup(stormId);
                    cancel();
                    return;
                }

                Player player = context.caster();
                Iterator<Hailstone> iterator = hailstones.iterator();

                while (iterator.hasNext()) {
                    Hailstone hail = iterator.next();

                    if (hail.hasImpacted) {
                        iterator.remove();
                        continue;
                    }

                    // Update hailstone position with physics
                    hail.location.add(hail.velocity.clone().multiply(0.05));

                    // Apply air resistance (gradual velocity reduction)
                    hail.velocity.multiply(0.995);

                    // Create falling particle trail
                    createHailstoneTrail(context, hail);

                    // Check for impact with ground or entities
                    if (hail.location.getY() <= hail.location.getWorld().getHighestBlockYAt(hail.location) + 1) {
                        handleHailstoneImpact(context, hail, player);
                        hail.hasImpacted = true;
                    }
                }

                // Visualize damage zones
                if (showDamageZones && ticks % 10 == 0) {
                    visualizeDamageZones(context, player.getLocation());
                }

                // Flush batched particles for performance
                context.fx().flushParticleBatch();

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * Creates visual trail effects for falling hailstones.
     */
    private void createHailstoneTrail(SpellContext context, Hailstone hail) {
        // Size-dependent particle effects
        int particleCount = (int) Math.ceil(hail.size * 8);
        double spread = hail.size * 0.3;

        // Ice crystal particles for the hailstone itself
        context.fx().batchParticles(hail.location, Particle.SNOWFLAKE, particleCount,
            spread, spread, spread, 0.02);

        // Larger hailstones get more dramatic effects
        if (hail.size > 0.5) {
            context.fx().batchParticles(hail.location, Particle.ITEM, 2,
                spread * 0.5, spread * 0.5, spread * 0.5, 0.01, new org.bukkit.inventory.ItemStack(Material.SNOWBALL));
            context.fx().batchParticles(hail.location, Particle.ASH, 1,
                spread * 0.3, spread * 0.3, spread * 0.3, 0.005);
        }
    }

    /**
     * Handles hailstone impact with sophisticated effects and damage calculation.
     */
    private void handleHailstoneImpact(SpellContext context, Hailstone hail, Player caster) {
        World world = hail.location.getWorld();
        if (world == null) return;

        Location impactLoc = hail.location.clone();
        impactLoc.setY(world.getHighestBlockYAt(impactLoc) + 0.1);

        // Surface-dependent impact effects
        Material surfaceMaterial = world.getBlockAt(impactLoc.clone().subtract(0, 1, 0)).getType();
        createSurfaceImpactEffect(context, impactLoc, surfaceMaterial, hail.size);

        // Damage calculation with impact radius
        double impactRadius = hail.size * 1.5;
        Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(impactLoc, impactRadius);

        for (LivingEntity entity : nearbyEntities) {
            if (entity.equals(caster)) continue;

            double distance = entity.getLocation().distance(impactLoc);
            double damageFalloff = Math.max(0.1, 1.0 - (distance / impactRadius));
            double finalDamage = hail.damage * damageFalloff;

            // Apply damage and knockdown effect
            entity.damage(finalDamage, caster);

            // Knockdown effect based on hailstone size
            Vector knockdown = new Vector(0, -hail.size * 0.3, 0);
            Vector currentVel = entity.getVelocity();
            entity.setVelocity(currentVel.add(knockdown));

            // Apply temporary slowness for larger hailstones
            if (hail.size > 0.4 && entity instanceof LivingEntity) {
                // Larger hailstones cause brief stunning
                int stunDuration = (int) (hail.size * 30); // 0.6 to 1.2 seconds
                entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, stunDuration, 1));
            }

            // Impact feedback particles on the entity
            context.fx().batchParticles(entity.getLocation().add(0, 1, 0),
                Particle.DAMAGE_INDICATOR, 3, 0.3, 0.5, 0.3, 0.1);
        }

        // Mark impact zone for visualization
        impactZones.add(impactLoc.clone());

        // Schedule impact zone cleanup
        new BukkitRunnable() {
            @Override
            public void run() {
                impactZones.remove(impactLoc);
            }
        }.runTaskLater(context.plugin(), 40L); // Remove after 2 seconds
    }

    /**
     * Creates surface-specific impact effects based on the material hit.
     */
    private void createSurfaceImpactEffect(SpellContext context, Location impact, Material surface, double hailSize) {
        int particleCount = (int) Math.ceil(hailSize * 15);
        double spread = hailSize * 0.6;

        // Ice impact particles (common to all surfaces)
        context.fx().batchParticles(impact, Particle.SNOWFLAKE, particleCount,
            spread, 0.2, spread, 0.1);
        context.fx().batchParticles(impact, Particle.ASH, particleCount / 2,
            spread * 0.8, 0.1, spread * 0.8, 0.05);

        // Surface-specific effects and sounds
        switch (surface.toString()) {
            case "STONE", "COBBLESTONE", "ANDESITE", "GRANITE", "DIORITE" -> {
                context.fx().batchParticles(impact, Particle.BLOCK, particleCount / 3,
                    spread * 0.5, 0.1, spread * 0.5, 0.1, surface.createBlockData());
                context.fx().playSound(impact, Sound.BLOCK_STONE_HIT,
                    (float) (0.8 + hailSize * 0.4), (float) (0.8 - hailSize * 0.2));
            }
            case "GRASS_BLOCK", "DIRT", "COARSE_DIRT" -> {
                context.fx().batchParticles(impact, Particle.BLOCK, particleCount / 4,
                    spread * 0.7, 0.1, spread * 0.7, 0.05, surface.createBlockData());
                context.fx().playSound(impact, Sound.BLOCK_GRASS_HIT,
                    (float) (0.6 + hailSize * 0.3), (float) (1.0 - hailSize * 0.1));
            }
            case "WATER" -> {
                context.fx().batchParticles(impact, Particle.SPLASH, particleCount,
                    spread, 0.3, spread, 0.1);
                context.fx().playSound(impact, Sound.ENTITY_GENERIC_SPLASH,
                    (float) (0.7 + hailSize * 0.2), (float) (1.2 - hailSize * 0.3));
            }
            default -> {
                // Generic impact
                context.fx().playSound(impact, Sound.BLOCK_GLASS_HIT,
                    (float) (0.5 + hailSize * 0.3), (float) (0.9 - hailSize * 0.2));
            }
        }

        // Dramatic shattering effect for large hailstones
        if (hailSize > 0.6) {
            context.fx().batchParticles(impact, Particle.ITEM, 8,
                spread * 1.2, 0.3, spread * 1.2, 0.15, new org.bukkit.inventory.ItemStack(Material.SNOWBALL));
            context.fx().playSound(impact, Sound.BLOCK_GLASS_BREAK,
                (float) (0.4 + hailSize * 0.2), (float) (0.6 + hailSize * 0.2));
        }
    }

    /**
     * Visualizes active damage zones with warning particles.
     */
    private void visualizeDamageZones(SpellContext context, Location center) {
        double radius = spellConfig.getDouble("values.radius", 8.0);

        // Create perimeter warning effects
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 8) {
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY();

            Location perimeterLoc = new Location(center.getWorld(), x, y, z);
            context.fx().batchParticles(perimeterLoc, Particle.DUST, 2,
                0.2, 0.5, 0.2, 0.1, new Particle.DustOptions(Color.ORANGE, 1.0f));
        }

        // Mark recent impact zones
        for (Location impactZone : impactZones) {
            context.fx().batchParticles(impactZone, Particle.CRIT, 3,
                0.5, 0.1, 0.5, 0.05);
        }
    }

    /**
     * Cleans up storm data to prevent memory leaks.
     */
    private void cleanup(UUID stormId) {
        activeStorms.remove(stormId);
        // Impact zones are cleaned up individually with their own timers
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}

