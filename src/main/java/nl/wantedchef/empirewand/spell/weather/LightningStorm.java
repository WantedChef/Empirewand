package nl.wantedchef.empirewand.spell.weather;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.util.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Creates a dramatic lightning storm with spectacular electrical effects, including:
 * - Chain lightning that arcs between entities
 * - Storm cloud particle effects with thunder sounds
 * - Electrical charge buildup and branching patterns
 * - Powerful lightning strikes with dramatic visual impact
 *
 * @author WantedChef
 */
public class LightningStorm extends Spell<Void> {

    /**
     * Builder for {@link LightningStorm}.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a builder for the spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lightning Storm";
            this.description = "Creates a spectacular lightning storm with chain lightning, storm clouds, and dramatic electrical effects.";
            this.cooldown = Duration.ofMillis(12000);
            this.spellType = SpellType.WEATHER;
        }

        /**
         * Builds the spell instance.
         *
         * @return a new {@link LightningStorm}
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new LightningStorm(this);
        }
    }

    private LightningStorm(Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String key() {
        return "lightningstorm";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Creates a spectacular lightning storm with enhanced visual and audio effects.
     *
     * @param context the spell context
     * @return always {@code null}
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        World world = player.getWorld();
        Location center = player.getLocation().clone();

        // Configuration values
        double radius = SpellUtils.getConfigDouble(spellConfig, "values.radius", 12.0);
        int chainStrikes = SpellUtils.getConfigInt(spellConfig, "values.chain-strikes", 3);
        double chainRange = SpellUtils.getConfigDouble(spellConfig, "values.chain-range", 8.0);
        double stormDuration = SpellUtils.getConfigDouble(spellConfig, "values.duration", 6.0);
        boolean damageEntities = SpellUtils.getConfigBoolean(spellConfig, "values.damage-entities", true);
        double damage = SpellUtils.getConfigDouble(spellConfig, "values.damage", 8.0);

        // Create storm clouds above the area
        createStormClouds(context, world, center, radius);

        // Start the lightning storm sequence
        new BukkitRunnable() {
            int ticksElapsed = 0;
            final int maxTicks = (int) (stormDuration * 20); // Convert seconds to ticks
            final List<Location> previousStrikes = new ArrayList<>();

            @Override
            public void run() {
                if (ticksElapsed >= maxTicks) {
                    // Final dramatic strikes
                    createFinalLightningBurst(context, world, center, radius);
                    this.cancel();
                    return;
                }

                // Create electrical charge buildup effects
                if (ticksElapsed % 10 == 0) {
                    createElectricalChargeBuildup(world, center, radius);
                }

                // Strike lightning at intervals
                if (ticksElapsed % 25 == 0) { // Every 1.25 seconds
                    Location strikeLocation = generateSmartStrikeLocation(center, radius, previousStrikes);
                    createDramaticLightningStrike(context, world, strikeLocation, damageEntities, damage);
                    previousStrikes.add(strikeLocation.clone());

                    // Chain lightning from this strike
                    if (chainStrikes > 0) {
                        createChainLightning(world, strikeLocation, chainRange, chainStrikes, damageEntities, damage * 0.6);
                    }

                    // Limit previous strikes list size
                    if (previousStrikes.size() > 10) {
                        previousStrikes.remove(0);
                    }
                }

                // Ambient storm effects
                if (ticksElapsed % 5 == 0) {
                    createAmbientStormEffects(world, center, radius);
                }

                ticksElapsed++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);

        // Initial dramatic thunder
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.8f);
        center.getWorld().playSound(center, Sound.AMBIENT_CAVE, 2.0f, 2.0f);

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Storm effects are handled in executeSpell
    }

    /**
     * Creates storm cloud particle effects above the area
     */
    private void createStormClouds(@NotNull SpellContext context, @NotNull World world, @NotNull Location center, double radius) {
        Location cloudCenter = center.clone().add(0, 15, 0);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) { // 2 seconds of cloud formation
                    this.cancel();
                    return;
                }

                // Create expanding cloud formation
                double currentRadius = (radius * 1.5) * (ticks / 40.0);
                int particleCount = (int) (currentRadius * 8);

                for (int i = 0; i < particleCount; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                    double distance = ThreadLocalRandom.current().nextDouble() * currentRadius;
                    double x = cloudCenter.getX() + Math.cos(angle) * distance;
                    double z = cloudCenter.getZ() + Math.sin(angle) * distance;
                    double y = cloudCenter.getY() + ThreadLocalRandom.current().nextDouble(-3, 1);

                    Location particleLoc = new Location(world, x, y, z);

                    // Dense storm clouds
                    world.spawnParticle(Particle.CLOUD, particleLoc, 0, 0, 0, 0, 0);
                    world.spawnParticle(Particle.SMOKE, particleLoc, 0, 0, 0, 0, 0);

                    // Electrical charge in clouds
                    if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                        world.spawnParticle(Particle.END_ROD, particleLoc, 0, 0, 0, 0, 0);
                    }
                }

                // Ambient thunder sounds
                if (ticks % 10 == 0) {
                    world.playSound(cloudCenter, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
                }

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * Creates electrical charge buildup effects throughout the storm area
     */
    private void createElectricalChargeBuildup(@NotNull World world, @NotNull Location center, double radius) {
        int chargePoints = ThreadLocalRandom.current().nextInt(8, 15);

        for (int i = 0; i < chargePoints; i++) {
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double distance = ThreadLocalRandom.current().nextDouble() * radius;
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;
            double y = center.getY() + ThreadLocalRandom.current().nextDouble(1, 8);

            Location chargeLoc = new Location(world, x, y, z);

            // Create electrical sparks
            world.spawnParticle(Particle.END_ROD, chargeLoc, 5, 0.3, 0.3, 0.3, 0.05);
            world.spawnParticle(Particle.ELECTRIC_SPARK, chargeLoc, 8, 0.5, 0.5, 0.5, 0.1);

            // Crackling sound
            if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                world.playSound(chargeLoc, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.5f, 2.0f);
            }
        }
    }

    /**
     * Generates a smart strike location that avoids clustering
     */
    private @NotNull Location generateSmartStrikeLocation(@NotNull Location center, double radius, @NotNull List<Location> previousStrikes) {
        Location bestLocation = null;
        double bestDistance = 0;

        // Try multiple locations and pick the one farthest from previous strikes
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double distance = ThreadLocalRandom.current().nextDouble(2, radius);
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;

            final Location candidate = new Location(center.getWorld(), x, center.getY(), z);
            final Location finalCandidate = candidate.getWorld().getHighestBlockAt(candidate).getLocation().add(0, 1, 0);

            double minDistanceToPrevious = previousStrikes.stream()
                    .mapToDouble(prev -> prev.distance(finalCandidate))
                    .min()
                    .orElse(Double.MAX_VALUE);

            if (minDistanceToPrevious > bestDistance) {
                bestDistance = minDistanceToPrevious;
                bestLocation = finalCandidate;
            }
        }

        return bestLocation != null ? bestLocation :
               center.clone().add(ThreadLocalRandom.current().nextDouble(-radius, radius), 0,
                                ThreadLocalRandom.current().nextDouble(-radius, radius));
    }

    /**
     * Creates a dramatic lightning strike with enhanced visual and audio effects
     */
    private void createDramaticLightningStrike(@NotNull SpellContext context, @NotNull World world, @NotNull Location location, boolean damageEntities, double damage) {
        // Pre-strike buildup
        new BukkitRunnable() {
            int buildup = 0;

            @Override
            public void run() {
                if (buildup >= 15) {
                    // Actual lightning strike
                    world.strikeLightning(location);

                    // Enhanced visual effects
                    createLightningBranchingEffect(world, location);

                    // Multiple thunder sounds with delays for realism
                    world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 1.0f);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0f, 0.8f);
                        }
                    }.runTaskLater(context.plugin(), 10L);

                    // Damage nearby entities
                    if (damageEntities) {
                        damageNearbyEntities(world, location, damage);
                    }

                    this.cancel();
                    return;
                }

                // Buildup effects
                world.spawnParticle(Particle.END_ROD, location.clone().add(0, 10 - buildup * 0.6, 0),
                                  3, 0.2, 0.2, 0.2, 0.1);
                world.spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(0, 5, 0),
                                  buildup, 0.3, 3, 0.3, 0.05);

                if (buildup % 3 == 0) {
                    world.playSound(location, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.0f, 1.5f);
                }

                buildup++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * Creates branching lightning effect patterns
     */
    private void createLightningBranchingEffect(@NotNull World world, @NotNull Location center) {
        int branches = ThreadLocalRandom.current().nextInt(4, 8);

        for (int i = 0; i < branches; i++) {
            Vector direction = new Vector(
                ThreadLocalRandom.current().nextDouble(-1, 1),
                ThreadLocalRandom.current().nextDouble(-0.3, 0.1),
                ThreadLocalRandom.current().nextDouble(-1, 1)
            ).normalize();

            Location current = center.clone().add(0, 8, 0);
            double branchLength = ThreadLocalRandom.current().nextDouble(3, 8);

            // Create jagged lightning branch
            for (double step = 0; step < branchLength; step += 0.3) {
                // Add randomness for jagged appearance
                Vector randomOffset = new Vector(
                    ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                    ThreadLocalRandom.current().nextDouble(-0.2, 0.2),
                    ThreadLocalRandom.current().nextDouble(-0.5, 0.5)
                );

                current.add(direction.clone().multiply(0.3).add(randomOffset));

                // Electric particles along the branch
                world.spawnParticle(Particle.END_ROD, current, 0, 0, 0, 0, 0);
                world.spawnParticle(Particle.ELECTRIC_SPARK, current, 2, 0.1, 0.1, 0.1, 0.1);

                // Occasional sub-branches
                if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                    createLightningSubBranch(world, current.clone(), 2);
                }
            }
        }
    }

    /**
     * Creates smaller sub-branches for more realistic lightning patterns
     */
    private void createLightningSubBranch(@NotNull World world, @NotNull Location start, double length) {
        Vector direction = new Vector(
            ThreadLocalRandom.current().nextDouble(-1, 1),
            ThreadLocalRandom.current().nextDouble(-0.5, 0.2),
            ThreadLocalRandom.current().nextDouble(-1, 1)
        ).normalize();

        Location current = start.clone();

        for (double step = 0; step < length; step += 0.2) {
            Vector randomOffset = new Vector(
                ThreadLocalRandom.current().nextDouble(-0.3, 0.3),
                ThreadLocalRandom.current().nextDouble(-0.1, 0.1),
                ThreadLocalRandom.current().nextDouble(-0.3, 0.3)
            );

            current.add(direction.clone().multiply(0.2).add(randomOffset));
            world.spawnParticle(Particle.END_ROD, current, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Creates chain lightning that arcs between nearby entities
     */
    private void createChainLightning(@NotNull World world, @NotNull Location origin, double range, int maxChains, boolean damageEntities, double damage) {
        List<LivingEntity> nearbyEntities = origin.getNearbyLivingEntities(range).stream()
                .filter(entity -> !(entity instanceof Player) || !((Player) entity).equals(origin.getWorld().getPlayers().get(0)))
                .sorted((a, b) -> Double.compare(a.getLocation().distance(origin), b.getLocation().distance(origin)))
                .limit(maxChains)
                .toList();

        Location currentLocation = origin.clone();

        for (int i = 0; i < Math.min(maxChains, nearbyEntities.size()); i++) {
            LivingEntity target = nearbyEntities.get(i);
            Location targetLocation = target.getLocation().add(0, 1, 0);

            // Create arc effect between locations
            createElectricalArc(world, currentLocation, targetLocation);

            // Sound effect
            world.playSound(targetLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 1.5f);

            // Damage entity
            if (damageEntities) {
                target.damage(damage);

                // Visual effect on target
                world.spawnParticle(Particle.ELECTRIC_SPARK, targetLocation, 20, 0.5, 1, 0.5, 0.1);
            }

            currentLocation = targetLocation;
        }
    }

    /**
     * Creates electrical arc effect between two points
     */
    private void createElectricalArc(@NotNull World world, @NotNull Location from, @NotNull Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = from.distance(to);
        Vector step = direction.normalize().multiply(0.2);

        Location current = from.clone();

        for (double d = 0; d < distance; d += 0.2) {
            // Add electrical zigzag pattern
            Vector randomOffset = new Vector(
                ThreadLocalRandom.current().nextDouble(-0.3, 0.3),
                ThreadLocalRandom.current().nextDouble(-0.2, 0.2),
                ThreadLocalRandom.current().nextDouble(-0.3, 0.3)
            );

            current.add(step.clone().add(randomOffset));

            // Electric particles
            world.spawnParticle(Particle.END_ROD, current, 0, 0, 0, 0, 0);
            world.spawnParticle(Particle.ELECTRIC_SPARK, current, 1, 0.05, 0.05, 0.05, 0.05);
        }
    }

    /**
     * Creates ambient storm effects like rain particles and distant thunder
     */
    private void createAmbientStormEffects(@NotNull World world, @NotNull Location center, double radius) {
        // Scattered rain particles
        for (int i = 0; i < 20; i++) {
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double distance = ThreadLocalRandom.current().nextDouble() * radius * 1.2;
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;
            double y = center.getY() + ThreadLocalRandom.current().nextDouble(8, 15);

            Location rainLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.FALLING_WATER, rainLoc, 0, 0, -0.5, 0, 0.1);
        }

        // Occasional distant thunder
        if (ThreadLocalRandom.current().nextDouble() < 0.1) {
            world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.6f);
        }

        // Wind effects
        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            Location windLoc = center.clone().add(
                ThreadLocalRandom.current().nextDouble(-radius, radius),
                ThreadLocalRandom.current().nextDouble(1, 4),
                ThreadLocalRandom.current().nextDouble(-radius, radius)
            );
            world.spawnParticle(Particle.CLOUD, windLoc, 5, 1, 0.2, 1, 0.1);
        }
    }

    /**
     * Creates a final dramatic burst of lightning strikes
     */
    private void createFinalLightningBurst(@NotNull SpellContext context, @NotNull World world, @NotNull Location center, double radius) {
        int finalStrikes = ThreadLocalRandom.current().nextInt(8, 12);

        for (int i = 0; i < finalStrikes; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                    double distance = ThreadLocalRandom.current().nextDouble() * radius * 1.1;
                    Location strikeLoc = center.clone().add(
                        Math.cos(angle) * distance, 0, Math.sin(angle) * distance);

                    strikeLoc = strikeLoc.getWorld().getHighestBlockAt(strikeLoc).getLocation().add(0, 1, 0);

                    world.strikeLightning(strikeLoc);
                    createLightningBranchingEffect(world, strikeLoc);
                }
            }.runTaskLater(context.plugin(), i * 3L);
        }

        // Final thunder crescendo
        new BukkitRunnable() {
            @Override
            public void run() {
                world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 4.0f, 0.7f);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
            }
        }.runTaskLater(context.plugin(), 20L);
    }

    /**
     * Damages entities near a lightning strike
     */
    private void damageNearbyEntities(@NotNull World world, @NotNull Location location, double damage) {
        location.getNearbyLivingEntities(3.0).stream()
                .filter(entity -> !(entity instanceof Player))
                .forEach(entity -> {
                    entity.damage(damage);
                    // Stun effect
                    entity.setVelocity(new Vector(0, 0.3, 0));

                    // Visual feedback
                    Location entityLoc = entity.getLocation().add(0, 1, 0);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, entityLoc, 15, 0.5, 1, 0.5, 0.1);
                });
    }
}

