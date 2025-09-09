package com.example.empirewand.spell.implementation.toggle.control;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import com.example.empirewand.api.spell.toggle.ToggleableSpell;
import com.example.empirewand.EmpireWandPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced StasisField - A powerful toggleable area control spell that creates
 * a persistent stasis field, freezing entities within its radius and providing
 * various field effects. Features advanced particle systems and dynamic field
 * mechanics.
 */
public class StasisField extends Spell<Void> implements ToggleableSpell {

    // Core spell properties (kept minimal; names moved to config if needed)

    // Configuration record for type-safe config loading
    public record Config(
            double baseRadius,
            double maxRadius,
            int freezeDuration,
            int tickInterval,
            int particleInterval,
            int maxDurationTicks,
            double pulseIntensity,
            double freezeIntensityThreshold) {
    }

    // Configuration constants
    private static final double BASE_RADIUS = 6.0;
    private static final double MAX_RADIUS = 10.0;
    private static final int FREEZE_DURATION = 60; // 3 seconds in ticks
    private static final int TICK_INTERVAL = 10; // 0.5 second intervals
    private static final int PARTICLE_INTERVAL = 5; // 0.25 second intervals
    private static final int MAX_DURATION_TICKS = 2400; // 120 seconds

    // Particle effect configurations
    private static final Particle.DustOptions FIELD_CORE = new Particle.DustOptions(Color.AQUA, 2.0f);
    private static final Particle.DustOptions FIELD_EDGE = new Particle.DustOptions(Color.BLUE, 1.5f);
    private static final Particle.DustOptions FREEZE_EFFECT = new Particle.DustOptions(Color.WHITE, 1.0f);

    // Active field tracking
    private final Map<UUID, StasisFieldInstance> activeFields = new ConcurrentHashMap<>();
    private EmpireWandPlugin plugin;

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Stasis Field";
            this.description = "Creates a powerful area-control field that freezes time itself.";
            this.cooldown = java.time.Duration.ofSeconds(10);
            this.spellType = SpellType.CONTROL;
        }

        @Override
        @org.jetbrains.annotations.NotNull
        public Spell<Void> build() {
            return new StasisField(this);
        }
    }

    private StasisField(Builder builder) {
        super(builder);
        this.plugin = null; // Will be set when spell is used
    }

    @Override
    public String key() {
        return "stasis-field";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Initialize plugin if not set
        if (this.plugin == null) {
            this.plugin = context.plugin();
        }

        // Toggle the field
        toggle(player, context);
        return null;
    }

    @Override
    protected void handleEffect(@org.jetbrains.annotations.NotNull SpellContext context,
            @org.jetbrains.annotations.NotNull Void result) {
        // Effect handling is done in toggle methods
    }

    private Config loadConfig() {
        try {
            if (spellConfig == null) {
                // Return default configuration if spellConfig is null
                return new Config(BASE_RADIUS, MAX_RADIUS, FREEZE_DURATION, TICK_INTERVAL,
                        PARTICLE_INTERVAL, MAX_DURATION_TICKS, 0.5, 0.8);
            }

            double baseRadius = Math.max(1.0, spellConfig.getDouble("values.base-radius", BASE_RADIUS));
            double maxRadius = Math.max(baseRadius + 1.0, spellConfig.getDouble("values.max-radius", MAX_RADIUS));
            int freezeDuration = Math.max(20, spellConfig.getInt("values.freeze-duration", FREEZE_DURATION));
            int tickInterval = Math.max(5, spellConfig.getInt("values.tick-interval", TICK_INTERVAL));
            int particleInterval = Math.max(1, spellConfig.getInt("values.particle-interval", PARTICLE_INTERVAL));
            int maxDurationTicks = Math.max(100, spellConfig.getInt("values.max-duration-ticks", MAX_DURATION_TICKS));
            double pulseIntensity = Math.max(0.1, spellConfig.getDouble("values.pulse-intensity", 0.5));
            double freezeIntensityThreshold = Math.max(0.1,
                    spellConfig.getDouble("values.freeze-intensity-threshold", 0.8));

            return new Config(baseRadius, maxRadius, freezeDuration, tickInterval,
                    particleInterval, maxDurationTicks, pulseIntensity, freezeIntensityThreshold);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Error loading Stasis Field config, using defaults: " + e.getMessage());
            }
            // Return safe default configuration
            return new Config(BASE_RADIUS, MAX_RADIUS, FREEZE_DURATION, TICK_INTERVAL,
                    PARTICLE_INTERVAL, MAX_DURATION_TICKS, 0.5, 0.8);
        }
    }

    @Override
    public void activate(Player player, SpellContext context) {
        UUID playerId = player.getUniqueId();

        // Initialize plugin if not set
        if (this.plugin == null) {
            this.plugin = context.plugin();
        }

        // Check if already active
        if (isActive(player)) {
            player.sendMessage("§bStasisField is already active!");
            return;
        }

        // Validate location
        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) {
            player.sendMessage("§cCannot activate StasisField in invalid world!");
            return;
        }

        try {
            // Load configuration
            Config config = loadConfig();

            // Create field instance
            StasisFieldInstance field = new StasisFieldInstance(player, location.clone(), config);
            activeFields.put(playerId, field);

            // Start field effects
            field.start();

            // Initial effects
            player.sendMessage("§b❄ StasisField activated! Time itself bends to your will!");
            player.playSound(location, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

            // Initial particle burst
            spawnActivationParticles(location);

        } catch (Exception e) {
            activeFields.remove(playerId);
            player.sendMessage("§cFailed to activate StasisField: " + e.getMessage());
        }
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        UUID playerId = player.getUniqueId();

        StasisFieldInstance field = activeFields.remove(playerId);
        if (field == null) {
            player.sendMessage("StasisField is not active!");
            return;
        }

        try {
            // Stop field effects
            field.stop();

            // Deactivation effects
            player.sendMessage("§b❄ StasisField deactivated. Time flows normally again.");
            Location loc = player.getLocation();
            if (loc != null && loc.getWorld() != null) {
                player.playSound(loc, Sound.BLOCK_GLASS_PLACE, 0.8f, 1.2f);
                // Deactivation particle effect
                spawnDeactivationParticles(loc);
            }

        } catch (Exception e) {
            player.sendMessage("Error deactivating StasisField: " + e.getMessage());
        }
    }

    @Override
    public boolean isActive(Player player) {
        return activeFields.containsKey(player.getUniqueId());
    }

    @Override
    public void forceDeactivate(Player player) {
        UUID playerId = player.getUniqueId();
        StasisFieldInstance field = activeFields.remove(playerId);
        if (field != null) {
            field.stop();

            // Notify player if online
            if (player.isOnline()) {
                player.sendMessage("§b❄ StasisField was forcefully deactivated!");
            }
        }
    }

    @Override
    public int getMaxDuration() {
        return MAX_DURATION_TICKS;
    }

    // Particle effect methods
    private void spawnActivationParticles(Location center) {
        World world = center.getWorld();
        if (world == null)
            return;

        // Central ice explosion
        world.spawnParticle(Particle.CLOUD, center, 30, 2.0, 1.0, 2.0, 0.1);
        world.spawnParticle(Particle.DUST, center, 50, 2.0, 1.0, 2.0, 0.0, FIELD_CORE);
        world.spawnParticle(Particle.FIREWORK, center, 15, 1.5, 1.0, 1.5, 0.1);
    }

    private void spawnDeactivationParticles(Location center) {
        World world = center.getWorld();
        if (world == null)
            return;

        // Dissipation effect
        world.spawnParticle(Particle.CLOUD, center, 25, 1.5, 1.0, 1.5, 0.05);
        world.spawnParticle(Particle.DUST, center, 20, 1.0, 1.0, 1.0, 0.0, FIELD_EDGE);
    }

    /**
     * Individual stasis field instance for each player
     */
    private class StasisFieldInstance {
        private final Player player;
        private final Location centerLocation;
        private final Config config;
        private BukkitTask fieldTask;
        private BukkitTask particleTask;
        private final long startTime;
        private double currentRadius;
        private int tickCount = 0;
        private final Set<UUID> frozenEntities = ConcurrentHashMap.newKeySet();
        private final Random random = new Random();

        public StasisFieldInstance(Player player, Location center, Config config) {
            this.player = player;
            this.centerLocation = center;
            this.config = config;
            this.startTime = System.currentTimeMillis();
            this.currentRadius = config.baseRadius();
        }

        public void start() {
            // Main field effect task - applies stasis effects
            fieldTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!StasisField.StasisFieldInstance.this.player.isOnline()
                            || !StasisField.this.isActive(StasisField.StasisFieldInstance.this.player)) {
                        cancel();
                        return;
                    }

                    // Check max duration
                    if (System.currentTimeMillis() - startTime > config.maxDurationTicks() * 50) { // Convert ticks to
                                                                                                   // ms
                        StasisField.this.forceDeactivate(StasisField.StasisFieldInstance.this.player);
                        cancel();
                        return;
                    }

                    updateFieldEffects();
                }
            }.runTaskTimer(plugin, 0L, config.tickInterval());

            // Particle effect task - runs more frequently for smooth visuals
            particleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!StasisField.StasisFieldInstance.this.player.isOnline()
                            || !StasisField.this.isActive(StasisField.StasisFieldInstance.this.player)) {
                        cancel();
                        return;
                    }

                    updateParticleEffects();
                }
            }.runTaskTimer(plugin, 0L, config.particleInterval());
        }

        public void stop() {
            if (fieldTask != null) {
                fieldTask.cancel();
                fieldTask = null;
            }
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }

            // Release all frozen entities
            unfreezeAllEntities();
        }

        private void updateFieldEffects() {
            if (this.player.getWorld() != centerLocation.getWorld()) {
                StasisField.this.forceDeactivate(this.player);
                return;
            }

            // Update radius dynamically (pulsing effect)
            tickCount++;
            double pulse = Math.sin(tickCount * 0.2) * config.pulseIntensity() + 0.5; // Configurable pulse
            currentRadius = config.baseRadius() + (config.maxRadius() - config.baseRadius()) * pulse;

            // Update center location to follow player
            centerLocation.setX(this.player.getLocation().getX());
            centerLocation.setY(this.player.getLocation().getY());
            centerLocation.setZ(this.player.getLocation().getZ());

            // Find and affect nearby entities
            Collection<Entity> nearbyEntities = centerLocation.getWorld()
                    .getNearbyEntities(centerLocation, currentRadius, currentRadius, currentRadius);

            Set<UUID> entitiesInRange = new HashSet<>();

            for (Entity entity : nearbyEntities) {
                if (!(entity instanceof LivingEntity))
                    continue;
                if (entity.equals(this.player))
                    continue; // Skip caster

                LivingEntity living = (LivingEntity) entity;
                double distance = living.getLocation().distance(centerLocation);

                if (distance <= currentRadius) {
                    entitiesInRange.add(entity.getUniqueId());
                    applyStasisEffect(living, distance);
                }
            }

            // Unfreeze entities that left the field
            frozenEntities.stream()
                    .filter(uuid -> !entitiesInRange.contains(uuid))
                    .collect(Collectors.toList())
                    .forEach(this::unfreezeEntity);

            // Update tracking set
            frozenEntities.retainAll(entitiesInRange);
            frozenEntities.addAll(entitiesInRange);
        }

        private void updateParticleEffects() {
            World world = centerLocation.getWorld();
            if (world == null)
                return;

            // Central field core
            world.spawnParticle(Particle.DUST, centerLocation, 3, 0.5, 0.5, 0.5, 0.0, FIELD_CORE);

            // Field boundary ring
            spawnFieldBoundary(world, centerLocation, currentRadius);

            // Floating crystals effect
            spawnFloatingCrystals(world, centerLocation, currentRadius);

            // Ground frost patterns
            if (tickCount % 4 == 0) { // Less frequent for performance
                spawnGroundFrost(world, centerLocation, currentRadius);
            }
        }

        private void spawnFieldBoundary(World world, Location center, double radius) {
            int points = 32;
            double angleStep = 2 * Math.PI / points;

            for (int i = 0; i < points; i++) {
                double angle = i * angleStep;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                double y = center.getY() + 1.0;

                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.2, 0.0, 0.0, FIELD_EDGE);
            }
        }

        private void spawnFloatingCrystals(World world, Location center, double radius) {
            for (int i = 0; i < 8; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius * 0.8;
                double height = random.nextDouble() * 3.0 + 1.0;

                double x = center.getX() + distance * Math.cos(angle);
                double z = center.getZ() + distance * Math.sin(angle);
                double y = center.getY() + height;

                Location crystalLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.ENCHANTED_HIT, crystalLoc, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }

        private void spawnGroundFrost(World world, Location center, double radius) {
            for (int i = 0; i < 12; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius;

                double x = center.getX() + distance * Math.cos(angle);
                double z = center.getZ() + distance * Math.sin(angle);
                double y = center.getY() + 0.1;

                Location frostLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.DUST, frostLoc, 1, 0.2, 0.0, 0.2, 0.0, FREEZE_EFFECT);
            }
        }

        private void applyStasisEffect(LivingEntity entity, double distance) {
            // Intensity based on distance from center
            double intensity = 1.0 - (distance / currentRadius);

            // Stronger effects closer to center
            if (intensity > config.freezeIntensityThreshold()) {
                // Complete stasis - freeze completely
                entity.addPotionEffect(
                        new PotionEffect(PotionEffectType.SLOWNESS, config.freezeDuration(), 9, false, false));
                entity.addPotionEffect(
                        new PotionEffect(PotionEffectType.MINING_FATIGUE, config.freezeDuration(), 9, false, false));

                // Visual freeze effect
                if (Math.random() < 0.3) {
                    entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 1, 0),
                            5, 0.3, 0.5, 0.3, 0.0, FREEZE_EFFECT);
                }
            } else if (intensity > 0.5) {
                // Partial stasis - significant slowdown
                entity.addPotionEffect(
                        new PotionEffect(PotionEffectType.SLOWNESS, config.freezeDuration(), 4, false, false));
                entity.addPotionEffect(
                        new PotionEffect(PotionEffectType.WEAKNESS, config.freezeDuration(), 2, false, false));
                entity.addPotionEffect(
                        new PotionEffect(PotionEffectType.MINING_FATIGUE, config.freezeDuration(), 2, false, false));
            } else {
                // Minor stasis - light effects
                entity.addPotionEffect(
                        new PotionEffect(PotionEffectType.SLOWNESS, config.freezeDuration(), 1, false, false));
                entity.addPotionEffect(
                        new PotionEffect(PotionEffectType.WEAKNESS, config.freezeDuration(), 0, false, false));
            }

            // Special effects for players
            if (entity instanceof Player) {
                Player target = (Player) entity;
                if (intensity > 0.7) {
                    target.sendMessage("❄ You are caught in a stasis field!");
                    target.playSound(target.getLocation(), Sound.BLOCK_GLASS_HIT, 0.5f, 1.5f);
                }
            }
        }

        private void unfreezeEntity(UUID entityId) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;

                // Remove stasis effects
                living.removePotionEffect(PotionEffectType.SLOWNESS);
                living.removePotionEffect(PotionEffectType.WEAKNESS);
                living.removePotionEffect(PotionEffectType.MINING_FATIGUE);

                // Liberation effect
                if (entity instanceof Player) {
                    ((Player) entity).sendMessage("❄ You break free from the stasis field!");
                }

                // Visual effect
                living.getWorld().spawnParticle(Particle.CLOUD, living.getLocation().add(0, 1, 0),
                        8, 0.5, 0.5, 0.5, 0.1);
            }

            frozenEntities.remove(entityId);
        }

        private void unfreezeAllEntities() {
            // Create a copy to avoid concurrent modification
            Set<UUID> toUnfreeze = new HashSet<>(frozenEntities);
            toUnfreeze.forEach(this::unfreezeEntity);
            frozenEntities.clear();
        }
    }
}