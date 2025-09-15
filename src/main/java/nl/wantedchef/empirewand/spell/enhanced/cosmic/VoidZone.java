package nl.wantedchef.empirewand.spell.enhanced.cosmic;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.Material;
import org.bukkit.Location;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Objects;

/**
 * A terrifying dark spell that creates multiple void zones,
 * pulling enemies in and dealing massive damage.
 * <p>
 * This spell creates multiple void zones that pull enemies toward their centers
 * and deal damage to those who get too close. Affected entities also receive
 * wither effects. The spell includes visual effects with squid ink and smoke
 * particles and audio feedback with ambient sounds.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Multiple void zone creation</li>
 *   <li>Area of effect pull toward zone centers</li>
 *   <li>Damage and wither effects for nearby entities</li>
 *   <li>Animated particle visual effects</li>
 *   <li>Audio feedback with ambient sounds</li>
 *   <li>Configurable zone count, radius, and duration</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell voidZone = new VoidZone.Builder(api)
 *     .name("Void Zone")
 *     .description("Creates multiple void zones that pull enemies in and deal massive damage.")
 *     .cooldown(Duration.ofSeconds(50))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class VoidZone extends Spell<Void> {

    /**
     * Builder for creating VoidZone spell instances.
     * <p>
     * Provides a fluent API for configuring the void zone spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new VoidZone spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Void Zone";
            this.description = "Creates multiple void zones that pull enemies in and deal massive damage.";
            this.cooldown = Duration.ofSeconds(50);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds and returns a new VoidZone spell instance.
         *
         * @return the constructed VoidZone spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new VoidZone(this);
        }
    }

    /**
     * Constructs a new VoidZone spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private VoidZone(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "void-zone"
     */
    @Override
    @NotNull
    public String key() {
        return "void-zone";
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
     * Executes the void zone spell logic.
     * <p>
     * This method creates multiple void zones that pull enemies toward their centers
     * and apply damage and wither effects.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 25.0);
        int zoneCount = spellConfig.getInt("values.zone-count", 5);
        double damage = spellConfig.getDouble("values.damage", 8.0);
        double pullStrength = spellConfig.getDouble("values.pull-strength", 0.3);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 120);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);

        // Play initial sound
        var world = player.getWorld();
        var playerLocation = player.getLocation();
        
        if (world != null && playerLocation != null) {
            world.playSound(playerLocation, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, 2.0f, 0.5f);
        }

        // Create void zones
        if (playerLocation != null) {
            BukkitRunnable voidZoneTask = new VoidZoneTask(context, playerLocation, radius, zoneCount, damage, pullStrength, durationTicks, affectsPlayers);
            context.plugin().getTaskManager().runTaskTimer(voidZoneTask, 0L, 2L);
        }
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
     * A runnable that handles the void zone effects over time.
     * <p>
     * This task manages the creation, updating, and effects of multiple void zones.
     */
    private static class VoidZoneTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int zoneCount;
        private final double damage;
        private final double pullStrength;
        private final int durationTicks;
        private final boolean affectsPlayers;
        private final World world;
        private final Random random = new Random();
        private final List<VoidZoneInstance> voidZones = new ArrayList<>();
        private int ticks = 0;

        /**
         * Creates a new VoidZoneTask instance.
         *
         * @param context the spell context
         * @param center the center location for void zone creation
         * @param radius the radius of the void zone effect
         * @param zoneCount the number of void zones to create
         * @param damage the damage to apply to entities in void zones
         * @param pullStrength the strength of the pull effect toward void zone centers
         * @param durationTicks the duration of the effect in ticks
         * @param affectsPlayers whether the effect affects players
         */
        public VoidZoneTask(@NotNull SpellContext context, @NotNull Location center, double radius, int zoneCount,
                           double damage, double pullStrength, int durationTicks, boolean affectsPlayers) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.radius = radius;
            this.zoneCount = zoneCount;
            this.damage = damage;
            this.pullStrength = pullStrength;
            this.durationTicks = durationTicks;
            this.affectsPlayers = affectsPlayers;
            this.world = center.getWorld();
            
            // Create initial void zones
            for (int i = 0; i < zoneCount; i++) {
                createVoidZone();
            }
        }

        /**
         * Runs the void zone task, updating existing zones and creating new ones
         * periodically.
         */
        @Override
        public void run() {
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= durationTicks) {
                this.cancel();
                return;
            }

            // Update existing void zones
            Iterator<VoidZoneInstance> iterator = voidZones.iterator();
            while (iterator.hasNext()) {
                VoidZoneInstance zone = iterator.next();
                if (!zone.update()) {
                    iterator.remove();
                }
            }

            // Create new void zones periodically
            if (ticks % 30 == 0 && voidZones.size() < zoneCount * 2) {
                createVoidZone();
            }

            // Apply pull and damage effects
            if (ticks % 5 == 0) {
                applyVoidEffects();
            }

            ticks++;
        }

        /**
         * Creates a new void zone at a random location within the effect radius.
         * <p>
         * This method generates a random position and creates a new void zone instance
         * at that location.
         */
        private void createVoidZone() {
            // Random location within radius
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * (radius - 5);
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;
            Location zoneLocation = new Location(world, x, world.getHighestBlockYAt((int)x, (int)z), z);
            
            voidZones.add(new VoidZoneInstance(zoneLocation, 4.0, 60));
        }

        /**
         * Applies void effects to entities within all active void zones.
         * <p>
         * This method pulls entities toward void zone centers and applies damage
         * and wither effects to those who get too close.
         */
        private void applyVoidEffects() {
            if (world == null) {
                return;
            }
            
            for (VoidZoneInstance zone : voidZones) {
                for (LivingEntity entity : world.getNearbyLivingEntities(zone.location, zone.radius, 5, zone.radius)) {
                    if (entity.equals(context.caster())) continue;
                    if (entity instanceof Player && !affectsPlayers) continue;
                    if (entity.isDead() || !entity.isValid()) continue;

                    // Pull entity toward center of void zone
                    var entityLocation = entity.getLocation();
                    if (entityLocation != null) {
                        Vector pull = zone.location.toVector().subtract(entityLocation.toVector()).normalize().multiply(pullStrength);
                        entity.setVelocity(entity.getVelocity().add(pull));
                    }

                    // Damage entity if close to center
                    double distance = zone.location.distance(entityLocation);
                    if (distance < zone.radius * 0.3) {
                        entity.damage(damage, context.caster());
                        
                        // Apply wither effect
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 1, false, false));
                        
                        // Visual effect
                        if (entityLocation != null) {
                            world.spawnParticle(Particle.SQUID_INK, entityLocation.add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
                        }
                    }
                }
            }
        }

        /**
         * An instance of a void zone that applies effects to nearby entities.
         * <p>
         * This class represents a single void zone with its location, radius, duration,
         * and visual effects.
         */
        private class VoidZoneInstance {
            final Location location;
            final double radius;
            int duration;
            final Set<Block> affectedBlocks = new HashSet<>();
            final Map<Block, BlockData> originalBlocks = new HashMap<>();

            /**
             * Creates a new VoidZoneInstance.
             *
             * @param location the location of the void zone
             * @param radius the radius of the void zone
             * @param duration the duration of the void zone in ticks
             */
            VoidZoneInstance(@NotNull Location location, double radius, int duration) {
                this.location = Objects.requireNonNull(location, "Location cannot be null");
                this.radius = radius;
                this.duration = duration;
                createVisualEffect();
            }

            /**
             * Updates the void zone instance, applying pulsing and particle effects.
             *
             * @return true if the void zone should continue to exist, false if it should be removed
             */
            boolean update() {
                duration--;
                
                // Pulsing effect
                if (duration % 10 == 0) {
                    pulseEffect();
                }
                
                // Visual particles
                createParticles();
                
                return duration > 0;
            }

            /**
             * Creates the initial visual effect for the void zone.
             * <p>
             * This method creates particle effects around affected blocks and plays
             * a sound effect for the void zone creation.
             */
            private void createVisualEffect() {
                World world = location.getWorld();
                if (world == null) return;

                int blockRadius = (int) radius;
                for (int x = -blockRadius; x <= blockRadius; x++) {
                    for (int z = -blockRadius; z <= blockRadius; z++) {
                        double distance = Math.sqrt(x * x + z * z);
                        if (distance <= radius) {
                            Block block = world.getBlockAt(
                                location.getBlockX() + x,
                                location.getBlockY() - 1,
                                location.getBlockZ() + z
                            );
                            
                            if (!block.getType().isAir() && block.getType() != Material.BEDROCK) {
                                originalBlocks.put(block, block.getBlockData());
                                affectedBlocks.add(block);
                                
                                // Particle effect
                                world.spawnParticle(Particle.SQUID_INK, block.getLocation().add(0.5, 1, 0.5), 2, 0.1, 0.1, 0.1, 0);
                            }
                        }
                    }
                }
                
                // Sound effect
                world.playSound(location, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.5f);
            }

            /**
             * Creates a pulsing effect for the void zone.
             * <p>
             * This method generates particle pulses and plays sound effects to
             * visualize the void zone's activity.
             */
            private void pulseEffect() {
                World world = location.getWorld();
                if (world == null) return;

                // Particle pulse
                world.spawnParticle(Particle.SQUID_INK, location, 50, radius, 0.5, radius, 0.1);
                world.spawnParticle(Particle.SMOKE, location, 30, radius * 0.5, 1, radius * 0.5, 0.05);
                
                // Sound effect
                world.playSound(location, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.5f, 0.8f);
            }

            /**
             * Creates particle effects for the void zone.
             * <p>
             * This method generates ring particles and central void effects to
             * visualize the void zone.
             */
            private void createParticles() {
                World world = location.getWorld();
                if (world == null) return;

                // Ring of particles
                for (int i = 0; i < 20; i++) {
                    double angle = 2 * Math.PI * i / 20;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location particleLoc = location.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.SQUID_INK, particleLoc, 1, 0, 0, 0, 0);
                }

                // Central void effect
                world.spawnParticle(Particle.SMOKE, location, 5, 0.5, 0.5, 0.5, 0.01);
            }
        }
    }
}