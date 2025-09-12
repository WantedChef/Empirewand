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
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Objects;

/**
 * A powerful spell that calls down a meteor shower over a large area, dealing massive damage and
 * creating craters with enhanced visual effects.
 * <p>
 * This enhanced version of the meteor shower spell creates a more visually impressive effect
 * with warning rings, ambient meteor streaks, shockwaves, and a mushroom cloud explosion.
 * Each meteor creates a crater on impact and deals damage to nearby entities. The spell
 * concludes with a massive final explosion and mushroom cloud visualization.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Multiple falling meteor blocks with particle trails</li>
 *   <li>Area of effect damage on impact</li>
 *   <li>Circular crater formation with debris particles</li>
 *   <li>Bowl-shaped excavation pattern with depth variation</li>
 *   <li>Warning ring effect before meteor impacts</li>
 *   <li>Ambient meteor streak effects in the sky</li>
 *   <li>Shockwave expansion effects</li>
 *   <li>Mushroom cloud visualization</li>
 *   <li>Final massive explosion</li>
 *   <li>Enhanced flame and lava particle effects</li>
 *   <li>Audio feedback with thunder and explosion sounds</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell meteorShower = new MeteorShowerEnhanced.Builder(api)
 *     .name("Meteor Shower")
 *     .description("Calls down a devastating meteor shower that rains destruction upon your enemies with enhanced visuals.")
 *     .cooldown(Duration.ofSeconds(45))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class MeteorShowerEnhanced extends Spell<Void> {

    /**
     * Builder for creating MeteorShowerEnhanced spell instances.
     * <p>
     * Provides a fluent API for configuring the enhanced meteor shower spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new MeteorShowerEnhanced spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Meteor Shower";
            this.description =
                    "Calls down a devastating meteor shower that rains destruction upon your enemies with enhanced visuals.";
            this.cooldown = Duration.ofSeconds(45);
            this.spellType = SpellType.FIRE;
        }

        /**
         * Builds and returns a new MeteorShowerEnhanced spell instance.
         *
         * @return the constructed MeteorShowerEnhanced spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new MeteorShowerEnhanced(this);
        }
    }

    /**
     * Constructs a new MeteorShowerEnhanced spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private MeteorShowerEnhanced(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "meteor-shower-enhanced"
     */
    @Override
    @NotNull
    public String key() {
        return "meteor-shower-enhanced";
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
     * Executes the enhanced meteor shower spell logic.
     * <p>
     * This method creates an enhanced meteor shower effect that spawns multiple falling meteors
     * around a target location, each creating craters and dealing damage on impact. The spell
     * includes warning effects and concludes with a massive explosion and mushroom cloud.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
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
        var world = player.getWorld();
        if (world != null) {
            world.playSound(targetLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.5f);
        }

        // Create warning effect
        createWarningEffect(context, targetLocation, craterRadius);

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
     * Creates a warning effect before the meteor shower begins.
     * <p>
     * This method creates an expanding ring of flame particles to warn players of
     * the incoming meteor impacts.
     *
     * @param context the spell context
     * @param center the center location of the meteor shower
     * @param radius the radius of the warning effect
     */
    private void createWarningEffect(@NotNull SpellContext context, @NotNull Location center, int radius) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(center, "Center location cannot be null");
        
        World world = center.getWorld();
        if (world == null)
            return;

        // Create expanding ring to warn of incoming meteors
        BukkitRunnable warningTask = new BukkitRunnable() {
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
        };
        context.plugin().getTaskManager().runTaskTimer(warningTask, 0L, 1L);
    }

    /**
     * A runnable that handles the enhanced meteor shower's effects over time.
     * <p>
     * This task manages the spawning of meteors, their impact effects, crater formation,
     * ambient effects, shockwaves, and the final explosion with mushroom cloud.
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
         * Runs the enhanced meteor shower task, spawning meteors and creating ambient effects.
         */
        @Override
        public void run() {
            World world = center.getWorld();
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

            // Create ambient effects
            createAmbientEffects();

            ticks++;
        }

        /**
         * Spawns a meteor at the specified location with enhanced visual effects.
         * <p>
         * This method creates a falling block that acts as a meteor with flame and
         * lava particle trails, and tracks its impact to apply effects.
         *
         * @param location the location to spawn the meteor
         */
        private void spawnMeteor(@NotNull Location location) {
            Objects.requireNonNull(location, "Location cannot be null");
            
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
            BukkitRunnable trailTask = new BukkitRunnable() {
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
            };
            context.plugin().getTaskManager().runTaskTimer(trailTask, 1L, 1L);
        }

        /**
         * Handles the meteor impact effects at the specified location.
         * <p>
         * This method applies damage to nearby entities, creates a crater, generates
         * enhanced visual effects, and creates a shockwave.
         *
         * @param impactLocation the location where the meteor impacted
         */
        private void onMeteorImpact(@NotNull Location impactLocation) {
            Objects.requireNonNull(impactLocation, "Impact location cannot be null");
            
            World world = impactLocation.getWorld();
            if (world == null)
                return;

            // Damage entities in area
            for (LivingEntity entity : world.getNearbyLivingEntities(impactLocation, 4, 4, 4)) {
                if (entity instanceof Player && entity.equals(context.caster()))
                    continue;
                if (entity.isValid() && !entity.isDead()) {
                    entity.damage(damage, context.caster());
                    entity.setFireTicks(60); // 3 seconds of fire
                }
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

        /**
         * Creates a crater at the specified location with enhanced effects.
         * <p>
         * This method excavates a circular bowl-shaped crater around the impact point
         * with varying depth and creates debris particles for visual feedback.
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

        /**
         * Creates a shockwave effect expanding from the impact location.
         * <p>
         * This method creates an expanding ring of explosion particles around the
         * meteor impact point.
         *
         * @param center the center location of the shockwave
         */
        private void createShockwave(@NotNull Location center) {
            Objects.requireNonNull(center, "Center location cannot be null");
            
            World world = center.getWorld();
            if (world == null)
                return;

            // Create expanding shockwave
            BukkitRunnable shockwaveTask = new BukkitRunnable() {
                int radius = 1;
                final int maxRadius = 10;

                @Override
                public void run() {
                    if (radius > maxRadius) {
                        this.cancel();
                        return;
                    }

                    // Create ring of particles
                    RingRenderer.renderRing(center, radius, 36, (loc, vec) -> {
                        if (world != null) {
                            world.spawnParticle(Particle.EXPLOSION, loc, 2, 0, 0, 0, 0);
                        }
                    });

                    radius++;
                }
            };
            context.plugin().getTaskManager().runTaskTimer(shockwaveTask, 0L, 1L);
        }

        /**
         * Creates the final massive explosion when the meteor shower ends.
         * <p>
         * This method creates a large explosion at the center location, applies
         * scaled damage to all entities within the blast radius, and generates a
         * mushroom cloud visualization.
         */
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
                
                var entityLocation = entity.getLocation();
                if (entityLocation == null) continue;
                
                double distance = entityLocation.distance(center);
                double scaledDamage = damage * (1 - (distance / 15));
                if (scaledDamage > 0) {
                    entity.damage(scaledDamage, context.caster());
                }
            }

            // Create mushroom cloud effect
            createMushroomCloud();
        }

        /**
         * Creates a mushroom cloud effect after the final explosion.
         * <p>
         * This method creates a rising pillar of lava and smoke particles followed
         * by an expanding mushroom cap.
         */
        private void createMushroomCloud() {
            World world = center.getWorld();
            if (world == null)
                return;

            // Create rising pillar
            BukkitRunnable pillarTask = new BukkitRunnable() {
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
                    if (world != null) {
                        world.spawnParticle(Particle.LAVA, pillarLoc, 15, 0.5, 0.5, 0.5, 0.1);
                        world.spawnParticle(Particle.SMOKE, pillarLoc, 20, 1, 1, 1, 0.05);
                    }

                    height++;
                }
            };
            context.plugin().getTaskManager().runTaskTimer(pillarTask, 0L, 1L);
        }

        /**
         * Creates the mushroom cap effect for the mushroom cloud.
         * <p>
         * This method creates an expanding ring of smoke particles with occasional
         * lava particles to form the mushroom cap visualization.
         */
        private void createMushroomCap() {
            World world = center.getWorld();
            if (world == null)
                return;

            Location capCenter = center.clone().add(0, 15, 0);

            // Create expanding mushroom cap
            BukkitRunnable mushroomCapTask = new BukkitRunnable() {
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
                                if (world != null) {
                                    world.spawnParticle(Particle.SMOKE, loc, 3, 0.2, 0.2, 0.2, 0.02);
                                    if (radius == maxRadius && random.nextDouble() < 0.3) {
                                        world.spawnParticle(Particle.LAVA, loc, 1, 0.1, 0.1, 0.1, 0.01);
                                    }
                                }
                            });

                    radius++;
                }
            };
            context.plugin().getTaskManager().runTaskTimer(mushroomCapTask, 0L, 1L);
        }

        /**
         * Creates ambient meteor streak effects in the sky during the meteor shower.
         * <p>
         * This method periodically creates streaks of flame particles in the sky
         * to enhance the visual atmosphere of the meteor shower.
         */
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
