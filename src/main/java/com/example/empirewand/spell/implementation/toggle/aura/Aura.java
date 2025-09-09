package com.example.empirewand.spell.implementation.toggle.aura;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.spell.toggle.ToggleableSpell;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;

/**
 * Enhanced Aura - A powerful toggleable aura spell that provides continuous
 * damage and buffs to the caster while affecting nearby entities.
 * Features sophisticated particle effects and dynamic radius scaling.
 *
 * <p>
 * This spell creates a persistent aura around the caster that:
 * <ul>
 * <li>Damages nearby enemy entities over time</li>
 * <li>Applies beneficial effects to the caster</li>
 * <li>Generates dynamic particle effects</li>
 * <li>Can be toggled on/off by the caster</li>
 * </ul>
 *
 * <p>
 * The aura's radius pulses dynamically and follows the caster's movement.
 * Particle effects include rotating rings and energy streams for visual appeal.
 * </p>
 *
 * @author EmpireWand Team
 * @version 1.0
 * @since 1.0
 */
public class Aura extends Spell<Void> implements ToggleableSpell {

    // Configuration constants
    private static final double BASE_RADIUS = 6.0;
    private static final int EFFECT_DURATION = 80; // 4 seconds in ticks
    private static final int TICK_INTERVAL = 20; // 1 second intervals
    private static final int PARTICLE_INTERVAL = 4; // 0.2 seconds
    private static final int MAX_DURATION_TICKS = 1200; // 60 seconds

    // Particle effect configurations
    private static final Particle.DustOptions AURA_CORE = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 2.0f); // Gold
    private static final Particle.DustOptions AURA_EDGE = new Particle.DustOptions(Color.fromRGB(255, 165, 0), 1.5f); // Orange
    private static final Particle.DustOptions DAMAGE_PARTICLE = new Particle.DustOptions(Color.fromRGB(255, 0, 0),
            1.0f); // Red

    // Active aura tracking
    private final Map<UUID, AuraInstance> activeAuras = new ConcurrentHashMap<>();
    private EmpireWandPlugin plugin;

    /**
     * Configuration record for the Aura spell parameters.
     *
     * @param radius            The base radius of the aura effect
     * @param damage            The damage per tick applied to enemies
     * @param duration          The maximum duration in ticks
     * @param tickInterval      The interval between effect applications in ticks
     * @param ringParticleCount The number of particles in the rotating ring effect
     * @param ringMaxRadius     The maximum radius of the particle ring
     * @param ringExpandStep    The expansion step for the particle ring animation
     */
    public record Config(double radius, double damage, int duration, int tickInterval, int ringParticleCount,
            double ringMaxRadius, double ringExpandStep) {
    }

    /**
     * Builder class for constructing Aura spell instances.
     * Provides a fluent API for configuring the spell properties.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Constructs a new Aura Builder with the given API instance.
         *
         * @param api The EmpireWandAPI instance
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Aura";
            this.description = "Creates a toggleable damaging aura around the caster.";
            this.cooldown = java.time.Duration.ofSeconds(5);
            this.spellType = SpellType.AURA;
        }

        /**
         * Builds and returns a new Aura spell instance.
         *
         * @return A new Aura spell instance
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new Aura(this);
        }
    }

    /**
     * Constructs a new Aura spell instance.
     *
     * @param builder The builder containing the spell configuration
     */
    private Aura(Builder builder) {
        super(builder);
        // Plugin will be injected via SpellContext when needed
        this.plugin = null; // Will be set when spell is used
    }

    /**
     * Returns the unique key identifier for this spell.
     *
     * @return The spell key "aura"
     */
    @Override
    public String key() {
        return "aura";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Executes the aura spell, toggling it on or off.
     *
     * @param context The spell execution context
     * @return null as this spell doesn't produce a result object
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Initialize plugin if not set
        if (this.plugin == null) {
            this.plugin = context.plugin();
        }

        // Toggle the aura
        toggle(player, context);
        return null;
    }

    /**
     * Handles the effect of the aura spell execution.
     * For toggleable spells, effect handling is done in the toggle methods.
     *
     * @param context The spell execution context
     * @param result  The result object (null for this spell)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effect handling is done in toggle methods
    }

    // ToggleableSpell interface implementation

    /**
     * Checks if the aura is currently active for the specified player.
     *
     * @param player The player to check
     * @return true if the aura is active, false otherwise
     */
    @Override
    public boolean isActive(@NotNull Player player) {
        return activeAuras.containsKey(player.getUniqueId());
    }

    /**
     * Activates the aura for the specified player.
     * Creates a new aura instance and starts its effects.
     *
     * @param player  The player to activate the aura for
     * @param context The spell context
     */
    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        UUID playerId = player.getUniqueId();

        // Check if already active
        if (isActive(player)) {
            player.sendMessage("§6Aura is already active!");
            return;
        }

        // Validate location
        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) {
            player.sendMessage("§cCannot activate Aura in invalid world!");
            return;
        }

        try {
            // Create aura instance
            Config config = loadConfig();
            AuraInstance aura = new AuraInstance(player, location.clone(), config);
            activeAuras.put(playerId, aura);

            // Start aura effects
            aura.start();

            // Initial effects
            player.sendMessage("§6⚡ Aura activated! Power radiates from you!");
            Location soundLoc = player.getLocation();
            if (soundLoc != null) {
                player.playSound(soundLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
            }

            // Initial particle burst
            spawnActivationParticles(location);

        } catch (Exception e) {
            activeAuras.remove(playerId);
            player.sendMessage("§cFailed to activate Aura: " + e.getMessage());
        }
    }

    /**
     * Deactivates the aura for the specified player.
     * Stops the aura effects and cleans up resources.
     *
     * @param player  The player to deactivate the aura for
     * @param context The spell context
     */
    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        UUID playerId = player.getUniqueId();

        // Player is guaranteed non-null by signature

        AuraInstance aura = activeAuras.remove(playerId);
        if (aura == null) {
            if (player.isOnline()) {
                player.sendMessage("§6Aura is not active!");
            }
            return;
        }

        try {
            // Stop aura effects
            aura.stop();

            // Deactivation effects only if player is online
            if (player.isOnline()) {
                player.sendMessage("§6⚡ Aura deactivated.");
                Location location = player.getLocation();
                if (location != null && location.getWorld() != null) {
                    player.playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.8f);
                    spawnDeactivationParticles(location);
                }
            }

            // Deactivation particle effect
            Location deactLoc = player.getLocation();
            if (deactLoc != null) {
                spawnDeactivationParticles(deactLoc);
            }

        } catch (Exception e) {
            player.sendMessage("§cError deactivating Aura: " + e.getMessage());
        }
    }

    /**
     * Forcefully deactivates the aura for the specified player.
     * Used when the player logs out or the aura needs to be stopped immediately.
     *
     * @param player The player to force deactivate the aura for
     */
    @Override
    public void forceDeactivate(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        AuraInstance aura = activeAuras.remove(playerId);
        if (aura != null) {
            aura.stop();

            // Notify player if online
            if (player.isOnline()) {
                player.sendMessage("§6⚡ Aura was forcefully deactivated!");
            }
        }
    }

    /**
     * Returns the maximum duration for the aura in ticks.
     *
     * @return The maximum duration in ticks (60 seconds)
     */
    @Override
    public int getMaxDuration() {
        return MAX_DURATION_TICKS;
    }

    /**
     * Loads the configuration for the aura spell from the spell config.
     * Falls back to default values if configuration is missing or invalid.
     *
     * @return A Config object with the loaded or default values
     */
    private Config loadConfig() {
        try {
            if (spellConfig == null) {
                // Return default configuration if spellConfig is null
                return new Config(6.0, 2.0, 1200, 20, 32, 6.0, 0.6);
            }

            double radius = Math.max(1.0, spellConfig.getDouble("values.radius", 6.0));
            double damage = Math.max(0.1, spellConfig.getDouble("values.damage-per-tick", 2.0));
            int duration = Math.max(100, spellConfig.getInt("values.duration-ticks", 1200));
            int tickInterval = Math.max(5, spellConfig.getInt("values.tick-interval", 20));
            int ringParticleCount = Math.max(8, spellConfig.getInt("values.ring-particle-count", 32));
            double ringMaxRadius = Math.max(radius * 0.5, spellConfig.getDouble("values.ring-max-radius", radius));
            double ringExpandStep = Math.max(0.1, spellConfig.getDouble("values.ring-expand-step", 0.6));

            return new Config(radius, damage, duration, tickInterval, ringParticleCount, ringMaxRadius, ringExpandStep);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Error loading Aura config, using defaults", e);
            }
            // Return safe default configuration
            return new Config(6.0, 2.0, 1200, 20, 32, 6.0, 0.6);
        }
    }

    // Particle effect methods

    /**
     * Spawns activation particle effects when the aura is first activated.
     * Creates an explosion effect with dust particles and enchanted hits.
     *
     * @param center The center location for the particle effects
     */
    private void spawnActivationParticles(Location center) {
        if (center == null)
            return;
        World world = center.getWorld();
        if (world == null)
            return;

        try {
            // Central explosion
            world.spawnParticle(Particle.EXPLOSION, center, 3, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.DUST, center, 50, 2.0, 1.0, 2.0, 0.0, AURA_CORE);
            world.spawnParticle(Particle.ENCHANTED_HIT, center, 30, 1.5, 1.0, 1.5, 0.2);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Error spawning activation particles", e);
            }
        }
    }

    /**
     * Spawns deactivation particle effects when the aura is turned off.
     * Creates a smoke effect with dust particles to indicate deactivation.
     *
     * @param center The center location for the particle effects
     */
    private void spawnDeactivationParticles(Location center) {
        if (center == null)
            return;
        World world = center.getWorld();
        if (world == null)
            return;

        try {
            // Dissipation effect
            world.spawnParticle(Particle.SMOKE, center, 20, 1.0, 1.0, 1.0, 0.05);
            world.spawnParticle(Particle.DUST, center, 30, 1.5, 1.0, 1.5, 0.0, AURA_EDGE);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Error spawning deactivation particles", e);
            }
        }
    }

    /**
     * Individual aura instance for each player
     */
    private class AuraInstance {
        private final Player player;
        private final Location centerLocation;
        private final Config config;
        private BukkitTask effectTask;
        private BukkitTask particleTask;
        private final long startTime;
        private double currentRadius;
        private int tickCount = 0;

        public AuraInstance(Player player, Location center, Config config) {
            this.player = player;
            this.centerLocation = center;
            this.config = config;
            this.startTime = System.currentTimeMillis();
            this.currentRadius = BASE_RADIUS;
        }

        /**
         * Starts the aura effects by scheduling the effect and particle tasks.
         * Creates two separate tasks: one for applying effects and one for particle
         * visuals.
         */
        public void start() {
            // Main effect task - applies damage every second
            effectTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !isActive(player)) {
                        cancel();
                        return;
                    }

                    // Check max duration
                    if (System.currentTimeMillis() - startTime > getMaxDuration() * 50) { // Convert ticks to ms
                        forceDeactivate(player);
                        cancel();
                        return;
                    }

                    updateAuraEffects();
                }
            }.runTaskTimer(plugin, 0L, TICK_INTERVAL);

            // Particle effect task - runs more frequently for smooth visuals
            particleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !isActive(player)) {
                        cancel();
                        return;
                    }

                    updateParticleEffects();
                }
            }.runTaskTimer(plugin, 0L, PARTICLE_INTERVAL);
        }

        /**
         * Stops the aura effects by canceling both the effect and particle tasks.
         * Cleans up resources and sets task references to null.
         */
        public void stop() {
            if (effectTask != null) {
                effectTask.cancel();
                effectTask = null;
            }
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }
        }

        /**
         * Updates the aura effects each tick, including radius pulsing, position
         * tracking,
         * caster buffs, and damage application to nearby entities.
         */
        private void updateAuraEffects() {
            if (player.getWorld() != centerLocation.getWorld()) {
                forceDeactivate(player);
                return;
            }

            // Update radius dynamically (pulsing effect)
            tickCount++;
            double pulse = Math.sin(tickCount * 0.2) * 0.3 + 0.7; // 0.4 to 1.0
            currentRadius = config.radius() * pulse;

            // Update center location to follow player
            Location playerLoc = player.getLocation();
            if (playerLoc != null) {
                centerLocation.setX(playerLoc.getX());
                centerLocation.setY(playerLoc.getY());
                centerLocation.setZ(playerLoc.getZ());
            }

            // Apply self buffs to caster
            applyCasterBuffs();

            // Find and affect nearby entities
            Collection<Entity> nearbyEntities = centerLocation.getWorld()
                    .getNearbyEntities(centerLocation, currentRadius, currentRadius, currentRadius);

            for (Entity entity : nearbyEntities) {
                if (!(entity instanceof LivingEntity))
                    continue;
                if (entity.equals(player))
                    continue; // Skip self

                LivingEntity living = (LivingEntity) entity;
                double distance = living.getLocation().distance(centerLocation);

                if (distance <= currentRadius) {
                    applyAuraEffects(living, distance);
                }
            }
        }

        /**
         * Updates the particle effects for visual representation of the aura.
         * Creates core particles, rotating rings, and vertical energy streams.
         */
        private void updateParticleEffects() {
            World world = centerLocation.getWorld();
            if (world == null)
                return;

            // Core aura particles at center
            world.spawnParticle(Particle.DUST, centerLocation, 2, 0.3, 0.3, 0.3, 0.0, AURA_CORE);

            // Rotating particle rings
            double angle = (System.currentTimeMillis() / 50.0) % (2 * Math.PI);
            spawnParticleRing(world, centerLocation.clone().add(0, 0.5, 0),
                    currentRadius * 0.8, angle, AURA_EDGE, config.ringParticleCount());

            // Vertical energy streams
            if (tickCount % 10 == 0) { // Less frequent for performance
                for (int i = 0; i < 3; i++) {
                    Location streamLoc = centerLocation.clone().add(0, i * 0.8, 0);
                    world.spawnParticle(Particle.ENCHANTED_HIT, streamLoc, 3, 0.2, 0.1, 0.2, 0.05);
                }
            }
        }

        /**
         * Spawns a rotating particle ring around the specified center location.
         *
         * @param world          The world to spawn particles in
         * @param center         The center location of the ring
         * @param radius         The radius of the particle ring
         * @param rotationOffset The rotation offset for animation
         * @param color          The color of the particles
         * @param density        The number of particles in the ring
         */
        private void spawnParticleRing(World world, Location center, double radius,
                double rotationOffset, Particle.DustOptions color, int density) {
            for (int i = 0; i < density; i++) {
                double angle = (2 * Math.PI * i / density) + rotationOffset;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);

                Location particleLoc = new Location(world, x, center.getY(), z);
                world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, color);
            }
        }

        /**
         * Applies beneficial potion effects to the caster.
         * Grants strength, resistance, and regeneration effects.
         */
        private void applyCasterBuffs() {
            // Enhanced buffs for the caster
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, EFFECT_DURATION, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, EFFECT_DURATION, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, EFFECT_DURATION, 0, false, false));
        }

        /**
         * Applies aura effects to a target entity, including damage and debuffs.
         * Effects are scaled based on distance from the aura center.
         *
         * @param target   The entity to apply effects to
         * @param distance The distance from the aura center to the target
         */
        private void applyAuraEffects(LivingEntity target, double distance) {
            // Intensity based on distance from center
            double intensity = 1.0 - (distance / currentRadius);

            // Deal damage
            double damage = config.damage() * intensity;
            target.damage(damage, player);

            // Damage particles
            if (Math.random() < 0.5) {
                World targetWorld = target.getWorld();
                Location targetLoc = target.getLocation();
                if (targetWorld != null && targetLoc != null) {
                    targetWorld.spawnParticle(Particle.DUST, targetLoc.add(0, 1, 0),
                            3, 0.3, 0.5, 0.3, 0.0, DAMAGE_PARTICLE);
                }
            }

            // Apply negative effects to enemies
            if (isEnemy(target)) {
                int amplifier = intensity > 0.7 ? 1 : 0;
                target.addPotionEffect(
                        new PotionEffect(PotionEffectType.WEAKNESS, EFFECT_DURATION, amplifier, false, false));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, EFFECT_DURATION, 0, false, false));
            }
        }

        /**
         * Determines if a living entity should be considered an enemy for aura effects.
         * Currently excludes players and targets hostile mobs.
         *
         * @param entity The entity to check
         * @return true if the entity is considered an enemy, false otherwise
         */
        private boolean isEnemy(LivingEntity entity) {
            if (entity instanceof Player) {
                // TODO: Implement proper enemy/PvP checking
                // For now, don't affect other players
                return false;
            }

            // Hostile mobs
            String type = entity.getType().toString();
            return type.contains("ZOMBIE") || type.contains("SKELETON") ||
                    type.contains("CREEPER") || type.contains("SPIDER") ||
                    type.contains("WITCH") || type.contains("PILLAGER") ||
                    type.contains("VINDICATOR") || type.contains("EVOKER") ||
                    type.contains("WITHER") || type.contains("BLAZE") ||
                    type.contains("GHAST") || type.contains("ENDERMAN") ||
                    type.contains("GUARDIAN") || type.contains("PHANTOM");
        }
    }
}