package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.common.visual.RingRenderer;
import nl.wantedchef.empirewand.common.visual.SpiralEmitter;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * A powerful spell that calls down a meteor shower over a large area, dealing massive damage and
 * creating craters with enhanced visual effects.
 */
public class MeteorShowerEnhanced extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Meteor Shower";
            this.description =
                    "Calls down a devastating meteor shower that rains destruction upon your enemies with enhanced visuals.";
            this.cooldown = java.time.Duration.ofSeconds(45);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new MeteorShowerEnhanced(this);
        }
    }

    private MeteorShowerEnhanced(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "meteor-shower-enhanced";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double range = spellConfig.getDouble("values.range", 25.0);
        int meteorCount = spellConfig.getInt("values.meteor-count", 25);
        double damage = spellConfig.getDouble("values.damage", 12.0);
        int craterRadius = spellConfig.getInt("values.crater-radius", 3);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 100);

        // Get target location
        Location targetLocation = player.getTargetBlock(null, (int) range).getLocation();
        if (targetLocation == null) {
            context.fx().fizzle(player);
            return null;
        }

        // Play initial sound
        player.getWorld().playSound(targetLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f,
                0.5f);

        // Create warning effect
        createWarningEffect(context, targetLocation, craterRadius);

        // Start meteor shower effect
        new MeteorShowerTask(context, targetLocation, meteorCount, damage, craterRadius,
                durationTicks).runTaskTimer(context.plugin(), 0L, 4L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private void createWarningEffect(SpellContext context, Location center, int radius) {
        World world = center.getWorld();
        if (world == null)
            return;

        // Create expanding ring to warn of incoming meteors
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 20;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                double currentRadius = radius * (ticks / (double) maxTicks);
                RingRenderer.renderRing(center, currentRadius, 36,
                        (loc, vec) -> world.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0));

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

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

        public MeteorShowerTask(SpellContext context, Location center, int meteorCount,
                double damage, int craterRadius, int durationTicks) {
            this.context = context;
            this.center = center;
            this.meteorCount = meteorCount;
            this.damage = damage;
            this.craterRadius = craterRadius;
            this.durationTicks = durationTicks;
        }

        @Override
        public void run() {
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

            // Create ambient effects
            createAmbientEffects();

            ticks++;
        }

        private void spawnMeteor(Location location) {
            World world = location.getWorld();
            if (world == null)
                return;

            // Create falling block (meteor)
            FallingBlock meteor =
                    world.spawnFallingBlock(location, Material.MAGMA_BLOCK.createBlockData());
            meteor.setDropItem(false);
            meteor.setHurtEntities(true);

            // Add velocity to make it fall
            meteor.setVelocity(new Vector(0, -1.5, 0));

            // Enhanced visual effects
            context.fx().followParticles(context.plugin(), meteor, Particle.FLAME, 10, 0.3, 0.3,
                    0.3, 0.1, null, 1L);
            context.fx().followParticles(context.plugin(), meteor, Particle.LAVA, 5, 0.2, 0.2, 0.2,
                    0.05, null, 1L);

            // Trail effect
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (meteor.isDead() || !meteor.isValid()) {
                        onMeteorImpact(meteor.getLocation());
                        this.cancel();
                        return;
                    }

                    // Create spiral trail
                    SpiralEmitter.emit(meteor.getLocation(), 0.5, 1, 8, 0.1, Particle.FLAME);
                }
            }.runTaskTimer(context.plugin(), 1L, 1L);
        }

        private void onMeteorImpact(Location impactLocation) {
            World world = impactLocation.getWorld();
            if (world == null)
                return;

            // Damage entities in area
            for (LivingEntity entity : impactLocation.getWorld()
                    .getNearbyLivingEntities(impactLocation, 4, 4, 4)) {
                if (entity instanceof Player && entity.equals(context.caster()))
                    continue;
                entity.damage(damage, context.caster());
                entity.setFireTicks(60); // 3 seconds of fire
            }

            // Create crater
            createCrater(impactLocation);

            // Enhanced visual effects
            world.spawnParticle(Particle.EXPLOSION_EMITTER, impactLocation, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.LAVA, impactLocation, 50, 1.5, 1.5, 1.5, 0.2);
            world.spawnParticle(Particle.FLAME, impactLocation, 80, 2, 2, 2, 0.1);
            world.playSound(impactLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

            // Shockwave effect
            createShockwave(impactLocation);
        }

        private void createCrater(Location center) {
            World world = center.getWorld();
            if (world == null)
                return;

            int y = center.getBlockY();
            craterLocations.add(center);

            // Create a circular crater with depth variation
            for (int x = -craterRadius; x <= craterRadius; x++) {
                for (int z = -craterRadius; z <= craterRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= craterRadius) {
                        Block block =
                                world.getBlockAt(center.getBlockX() + x, y, center.getBlockZ() + z);
                        if (!block.getType().isAir() && block.getType() != Material.BEDROCK) {
                            // Create a bowl shape with varying depth
                            int depth = (int) Math.max(1, (craterRadius - distance) * 1.5);
                            for (int d = 0; d < depth; d++) {
                                Block toModify = block.getRelative(BlockFace.DOWN, d);
                                if (toModify.getType() != Material.BEDROCK) {
                                    toModify.setType(Material.AIR);
                                }
                            }

                            // Create debris particles
                            Location debrisLoc = block.getLocation().add(0.5, 0.5, 0.5);
                            world.spawnParticle(Particle.BLOCK, debrisLoc, 10, 0.3, 0.3, 0.3, 0.1,
                                    block.getBlockData());
                        }
                    }
                }
            }
        }

        private void createShockwave(Location center) {
            World world = center.getWorld();
            if (world == null)
                return;

            // Create expanding shockwave
            new BukkitRunnable() {
                int radius = 1;
                final int maxRadius = 10;

                @Override
                public void run() {
                    if (radius > maxRadius) {
                        this.cancel();
                        return;
                    }

                    // Create ring of particles
                    RingRenderer.renderRing(center, radius, 36, (loc, vec) -> world
                            .spawnParticle(Particle.EXPLOSION, loc, 2, 0, 0, 0, 0));

                    radius++;
                }
            }.runTaskTimer(context.plugin(), 0L, 1L);
        }

        private void createFinalExplosion() {
            World world = center.getWorld();
            if (world == null)
                return;

            // Create a massive explosion at the center
            world.createExplosion(center, 5.0f, false, false);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 5, 3, 3, 3, 0);
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);

            // Damage all entities in a large radius
            for (LivingEntity entity : world.getNearbyLivingEntities(center, 15, 15, 15)) {
                if (entity instanceof Player && entity.equals(context.caster()))
                    continue;
                double distance = entity.getLocation().distance(center);
                double scaledDamage = damage * (1 - (distance / 15));
                if (scaledDamage > 0) {
                    entity.damage(scaledDamage, context.caster());
                }
            }

            // Create mushroom cloud effect
            createMushroomCloud();
        }

        private void createMushroomCloud() {
            World world = center.getWorld();
            if (world == null)
                return;

            // Create rising pillar
            new BukkitRunnable() {
                int height = 0;
                final int maxHeight = 15;

                @Override
                public void run() {
                    if (height > maxHeight) {
                        // Create mushroom cap
                        createMushroomCap();
                        this.cancel();
                        return;
                    }

                    Location pillarLoc = center.clone().add(0, height, 0);
                    world.spawnParticle(Particle.LAVA, pillarLoc, 15, 0.5, 0.5, 0.5, 0.1);
                    world.spawnParticle(Particle.SMOKE, pillarLoc, 20, 1, 1, 1, 0.05);

                    height++;
                }
            }.runTaskTimer(context.plugin(), 0L, 1L);
        }

        private void createMushroomCap() {
            World world = center.getWorld();
            if (world == null)
                return;

            Location capCenter = center.clone().add(0, 15, 0);

            // Create expanding mushroom cap
            new BukkitRunnable() {
                int radius = 1;
                final int maxRadius = 8;

                @Override
                public void run() {
                    if (radius > maxRadius) {
                        this.cancel();
                        return;
                    }

                    // Create ring for mushroom cap
                    RingRenderer.renderRing(capCenter, radius, Math.max(12, radius * 6),
                            (loc, vec) -> {
                                world.spawnParticle(Particle.SMOKE, loc, 3, 0.2, 0.2, 0.2, 0.02);
                                if (radius == maxRadius && random.nextDouble() < 0.3) {
                                    world.spawnParticle(Particle.LAVA, loc, 1, 0.1, 0.1, 0.1, 0.01);
                                }
                            });

                    radius++;
                }
            }.runTaskTimer(context.plugin(), 0L, 1L);
        }

        private void createAmbientEffects() {
            if (ticks % 10 == 0) {
                World world = center.getWorld();
                if (world == null)
                    return;

                // Create ambient meteor streaks in the sky
                for (int i = 0; i < 3; i++) {
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double distance = random.nextDouble() * 30;
                    double x = center.getX() + Math.cos(angle) * distance;
                    double z = center.getZ() + Math.sin(angle) * distance;
                    Location start = new Location(world, x, center.getY() + 25, z);
                    Location end = new Location(world, x, center.getY() + 5, z);

                    // Create streak effect
                    Vector direction = end.toVector().subtract(start.toVector()).normalize();
                    for (int j = 0; j < 20; j++) {
                        Location particleLoc =
                                start.clone().add(direction.clone().multiply(j * 1.5));
                        world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }
}
