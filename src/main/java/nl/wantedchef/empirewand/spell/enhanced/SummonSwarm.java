package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.entity.Vex;
import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.Objects;

/**
 * A powerful necromancy spell that summons a swarm of minions to fight for you.
 * <p>
 * This spell summons a swarm of vex minions that follow the caster and attack
 * nearby enemies. The minions have configurable health and damage values, and
 * the spell includes visual effects with smoke and enchant particles to enhance
 * the summoning experience. The minions are automatically dismissed when the
 * spell duration expires.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Summoning of multiple vex minions</li>
 *   <li>Minions follow the caster and attack enemies</li>
 *   <li>Configurable minion count, health, and damage</li>
 *   <li>Animated particle visual effects</li>
 *   <li>Audio feedback for summoning and dismissal</li>
 *   <li>Automatic cleanup of summoned minions</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell summonSwarm = new SummonSwarm.Builder(api)
 *     .name("Summon Swarm")
 *     .description("Summons a swarm of minions to fight for you.")
 *     .cooldown(Duration.ofSeconds(70))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class SummonSwarm extends Spell<Void> {

    /**
     * Builder for creating SummonSwarm spell instances.
     * <p>
     * Provides a fluent API for configuring the summon swarm spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new SummonSwarm spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@NotNull EmpireWandAPI api) {
            super(api);
            this.name = "Summon Swarm";
            this.description = "Summons a swarm of minions to fight for you.";
            this.cooldown = Duration.ofSeconds(70);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds and returns a new SummonSwarm spell instance.
         *
         * @return the constructed SummonSwarm spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new SummonSwarm(this);
        }
    }

    /**
     * Constructs a new SummonSwarm spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private SummonSwarm(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "summon-swarm"
     */
    @Override
    @NotNull
    public String key() {
        return "summon-swarm";
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
     * Executes the summon swarm spell logic.
     * <p>
     * This method creates a swarm of vex minions around the caster that follow
     * the caster and attack nearby enemies.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        // Configuration
        int minionCount = spellConfig.getInt("values.minion-count", 8);
        double radius = spellConfig.getDouble("values.radius", 8.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 300);
        double minionHealth = spellConfig.getDouble("values.minion-health", 10.0);
        double minionDamage = spellConfig.getDouble("values.minion-damage", 4.0);

        // Play summoning sound
        var world = player.getWorld();
        if (world != null) {
            world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 2.0f, 0.8f);
        }

        // Start summoning effect
        BukkitRunnable summonTask = new SummonSwarmTask(context, player.getLocation(), minionCount, radius, durationTicks, 
                           minionHealth, minionDamage);
        context.plugin().getTaskManager().runTaskTimer(summonTask, 0L, 5L);
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
     * A runnable that handles the summon swarm's effects over time.
     * <p>
     * This task manages the summoning of minions, their behavior updates, visual
     * effects, and cleanup when the spell duration expires.
     */
    private static class SummonSwarmTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final int minionCount;
        private final double radius;
        private final int durationTicks;
        private final double minionHealth;
        private final double minionDamage;
        private final World world;
        private int ticks = 0;
        private final int maxTicks;
        private final List<Vex> summonedMinions = new ArrayList<>();
        private final Random random = new Random();

        /**
         * Creates a new SummonSwarmTask instance.
         *
         * @param context the spell context
         * @param center the center location for minion summoning
         * @param minionCount the number of minions to summon
         * @param radius the radius around which to summon minions
         * @param durationTicks the duration of the spell in ticks
         * @param minionHealth the health value for each summoned minion
         * @param minionDamage the damage value for each summoned minion
         */
        public SummonSwarmTask(@NotNull SpellContext context, @NotNull Location center, int minionCount, double radius, 
                              int durationTicks, double minionHealth, double minionDamage) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.minionCount = minionCount;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.minionHealth = minionHealth;
            this.minionDamage = minionDamage;
            this.world = center.getWorld();
            this.maxTicks = durationTicks / 5; // Convert to our tick interval
        }

        /**
         * Runs the summon swarm task, summoning minions, updating their behavior,
         * and creating visual effects.
         */
        @Override
        public void run() {
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= maxTicks) {
                this.cancel();
                dismissMinions();
                return;
            }

            // Summon minions at the beginning
            if (ticks == 0) {
                summonMinions();
            }

            // Update minion behavior
            updateMinionBehavior();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        /**
         * Summons the vex minions in a circular pattern around the caster.
         * <p>
         * This method spawns vex minions at calculated positions and applies
         * initial configuration to them.
         */
        private void summonMinions() {
            Player player = context.caster();
            
            for (int i = 0; i < minionCount; i++) {
                // Calculate position in a circle around the player
                double angle = 2 * Math.PI * i / minionCount;
                double x = center.getX() + Math.cos(angle) * radius;
                double z = center.getZ() + Math.sin(angle) * radius;
                Location spawnLoc = new Location(world, x, world.getHighestBlockYAt((int)x, (int)z) + 1, z);
                
                // Spawn vex minion
                Vex minion = world.spawn(spawnLoc, Vex.class, vex -> {
                    vex.setTarget(player); // Make it follow the player
                    vex.setHealth(minionHealth);
                });
                
                summonedMinions.add(minion);
                
                // Visual effect for summoning
                world.spawnParticle(Particle.SMOKE, spawnLoc, 20, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticle(Particle.SQUID_INK, spawnLoc, 10, 0.3, 0.3, 0.3, 0.01);
            }
            
            // Sound effect
            world.playSound(center, Sound.ENTITY_VEX_CHARGE, 1.5f, 1.2f);
        }

        /**
         * Updates the behavior of summoned minions.
         * <p>
         * This method makes minions follow the caster and attack nearby enemies,
         * and creates periodic visual effects.
         */
        private void updateMinionBehavior() {
            Player player = context.caster();
            Location playerLoc = player.getLocation();
            
            if (playerLoc == null) {
                return;
            }
            
            for (Vex minion : summonedMinions) {
                if (minion.isValid() && !minion.isDead()) {
                    // Make minions follow the player
                    minion.setTarget(player);
                    
                    // Find nearby enemies to attack
                    LivingEntity target = findNearestEnemy(minion);
                    if (target != null && target.isValid() && !target.isDead()) {
                        minion.setTarget(target);
                    }
                    
                    var minionLoc = minion.getLocation();
                    if (minionLoc != null) {
                        Vector direction = playerLoc.toVector().subtract(minionLoc.toVector()).normalize();
                        minion.setVelocity(direction.multiply(0.5));
                    }
                    
                    // Visual effect periodically
                    if (ticks % 20 == 0) {
                        var smokeLoc = minion.getLocation();
                        if (smokeLoc != null && world != null) {
                            world.spawnParticle(Particle.SMOKE, smokeLoc.add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.01);
                        }
                    }
                }
            }
        }

        /**
         * Finds the nearest enemy to a minion within search distance.
         * <p>
         * This method searches for valid living entities near a minion and returns
         * the closest one that is not the caster or another minion.
         *
         * @param minion the vex minion to find enemies for
         * @return the nearest enemy entity, or null if none found
         */
        private @Nullable LivingEntity findNearestEnemy(@NotNull Vex minion) {
            Objects.requireNonNull(minion, "Minion cannot be null");
            
            LivingEntity nearest = null;
            double nearestDistance = 16.0; // Max search distance
            
            for (Entity entity : minion.getNearbyEntities(16, 16, 16)) {
                if (entity instanceof LivingEntity && !(entity instanceof Vex) && 
                    !entity.equals(context.caster()) && entity.isValid() && !entity.isDead()) {
                    
                    var entityLocation = entity.getLocation();
                    var minionLocation = minion.getLocation();
                    
                    if (entityLocation != null && minionLocation != null) {
                        double distance = entityLocation.distance(minionLocation);
                        if (distance < nearestDistance) {
                            nearest = (LivingEntity) entity;
                            nearestDistance = distance;
                        }
                    }
                }
            }
            
            return nearest;
        }

        /**
         * Creates visual effects for the summon swarm spell.
         * <p>
         * This method generates animated enchant and smoke particles around the
         * summoning circle and minions.
         */
        private void createVisualEffects() {
            if (world == null) {
                return;
            }
            
            // Create magical circle around the center
            double currentRadius = radius + Math.sin(ticks * 0.2) * 1.5;
            
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i / 24) + (ticks * 0.1);
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Create particles around minions
            for (Vex minion : summonedMinions) {
                if (minion.isValid() && !minion.isDead()) {
                    var minionLocation = minion.getLocation();
                    if (minionLocation != null && world != null) {
                        if (ticks % 10 == 0) {
                            world.spawnParticle(Particle.SMOKE, minionLocation.add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
                        }
                    }
                }
            }
            
            // Create central vortex
            if (ticks % 8 == 0) {
                for (int i = 0; i < 10; i++) {
                    double height = i * 0.3;
                    Location vortexLoc = center.clone().add(0, height, 0);
                    world.spawnParticle(Particle.WITCH, vortexLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }

        /**
         * Dismisses all summoned minions with visual and audio effects.
         * <p>
         * This method removes all valid minions from the world and creates particle
         * effects to visualize their dismissal.
         */
        private void dismissMinions() {
            if (world == null) {
                return;
            }
            
            // Dismiss all minions with visual effects
            for (Vex minion : summonedMinions) {
                if (minion.isValid() && !minion.isDead()) {
                    // Visual effect for dismissal
                    var minionLocation = minion.getLocation();
                    if (minionLocation != null) {
                        world.spawnParticle(Particle.SMOKE, minionLocation, 30, 0.5, 0.5, 0.5, 0.1);
                        world.spawnParticle(Particle.EXPLOSION, minionLocation, 1, 0, 0, 0, 0);
                    }
                    minion.remove();
                }
            }
            
            // Sound effect
            world.playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 1.2f);
            
            summonedMinions.clear();
        }
    }
}