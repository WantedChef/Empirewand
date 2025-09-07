package com.example.empirewand.spell.implementation.movement;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.Color;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Enhanced SunburstStep spell with modern PaperMC Particle API and performance
 * optimizations.
 *
 * This spell creates a powerful teleportation dash with stunning visual
 * effects,
 * including dynamic particle trails, impact bursts, and aura effects.
 * Features extensive customization options and optimized performance.
 *
 * @author Enhanced by AI Assistant
 * @version 2.0.0
 * @since 1.21.7
 */
public class SunburstStep implements Spell {

    // Performance constants
    private static final double MAX_RENDER_DISTANCE = 64.0;
    private static final int BURST_PARTICLE_COUNT = 25;

    // Animation timing constants
    private static final int TRAIL_DURATION_TICKS = 40;
    private static final int AURA_DURATION_TICKS = 60;
    private static final int IMPACT_DURATION_TICKS = 20;

    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        if (player == null || !player.isValid()) {
            return;
        }

        // Load configuration with validation
        SpellConfig config = loadSpellConfig(context);

        // Validate teleportation target
        Location startLocation = player.getLocation();
        Vector direction = startLocation.getDirection().normalize();
        Location destination = findSafeDestination(startLocation, direction, config.maxDistance);

        if (destination == null) {
            handleTeleportFailure(context, player);
            return;
        }

        // Execute teleportation with enhanced effects
        performTeleportation(context, player, startLocation, destination, config);
    }

    /**
     * Loads and validates spell configuration from the config service.
     * Provides sensible defaults for all customizable parameters.
     */
    private SpellConfig loadSpellConfig(SpellContext context) {
        var spells = context.config().getSpellsConfig();
        SpellConfig config = new SpellConfig();

        // Core mechanics
        config.maxDistance = Math.max(1.0, spells.getDouble("sunburst-step.values.max-distance", 12.0));
        config.pulseRadius = Math.max(1.0, spells.getDouble("sunburst-step.values.pulse-radius", 4.0));
        config.allyHeal = Math.max(0.0, spells.getDouble("sunburst-step.values.ally-heal", 2.0));
        config.enemyDamage = Math.max(0.0, spells.getDouble("sunburst-step.values.enemy-damage", 3.0));

        // Targeting flags
        config.hitPlayers = spells.getBoolean("sunburst-step.flags.hit-players", true);
        config.hitMobs = spells.getBoolean("sunburst-step.flags.hit-mobs", true);

        // Visual customization
        config.trailColor = parseColor(spells.getString("sunburst-step.visual.trail-color", "#FFD700"));
        config.burstColor = parseColor(spells.getString("sunburst-step.visual.burst-color", "#FFA500"));
        config.auraColor = parseColor(spells.getString("sunburst-step.visual.aura-color", "#FFFF00"));

        config.trailParticle = parseParticle(spells.getString("sunburst-step.visual.trail-particle", "DUST"));
        config.burstParticle = parseParticle(spells.getString("sunburst-step.visual.burst-particle", "EXPLOSION"));
        config.auraParticle = parseParticle(spells.getString("sunburst-step.visual.aura-particle", "GLOW"));

        // Sound customization
        config.teleportSound = parseSound(
                spells.getString("sunburst-step.audio.teleport-sound", "ENTITY_ENDERMAN_TELEPORT"));
        config.impactSound = parseSound(spells.getString("sunburst-step.audio.impact-sound", "ENTITY_GENERIC_EXPLODE"));
        config.auraSound = parseSound(spells.getString("sunburst-step.audio.aura-sound", "BLOCK_AMETHYST_BLOCK_CHIME"));

        // Performance settings
        config.enableDistanceCulling = spells.getBoolean("sunburst-step.performance.distance-culling", true);

        return config;
    }

    /**
     * Parses a color string in hex format (#RRGGBB) to a Bukkit Color object.
     * Returns white as fallback for invalid formats.
     */
    private Color parseColor(String colorString) {
        try {
            if (colorString.startsWith("#") && colorString.length() == 7) {
                int r = Integer.parseInt(colorString.substring(1, 3), 16);
                int g = Integer.parseInt(colorString.substring(3, 5), 16);
                int b = Integer.parseInt(colorString.substring(5, 7), 16);
                return Color.fromRGB(r, g, b);
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            // Log warning in production
        }
        return Color.WHITE;
    }

    /**
     * Parses a particle name string to a Particle enum value.
     * Returns DUST as fallback for invalid names.
     */
    private Particle parseParticle(String particleName) {
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Particle.DUST;
        }
    }

    /**
     * Parses a sound name string to a Sound enum value.
     * Returns a default sound as fallback for invalid names.
     */
    private Sound parseSound(String soundName) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_ENDERMAN_TELEPORT;
        }
    }

    /**
     * Finds a safe destination for teleportation with enhanced validation.
     */
    private Location findSafeDestination(Location start, Vector direction, double maxDistance) {
        int steps = (int) Math.ceil((maxDistance - 1.0) / 0.5);
        for (int i = 0; i <= steps; i++) {
            double dist = maxDistance - (i * 0.5);
            if (dist < 1.0)
                break;

            Location testLoc = start.clone().add(direction.clone().multiply(dist));
            if (isSafeLocation(testLoc)) {
                return testLoc;
            }
        }
        return null;
    }

    /**
     * Enhanced safety check for teleportation destinations.
     */
    private boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().add(0, -1, 0).getBlock();

        return feet.getType() == Material.AIR &&
                head.getType() == Material.AIR &&
                ground.getType().isSolid() &&
                !ground.getType().name().contains("LAVA") &&
                !ground.getType().name().contains("FIRE");
    }

    /**
     * Handles teleportation failure with appropriate feedback.
     */
    private void handleTeleportFailure(SpellContext context, Player player) {
        context.fx().fizzle(player);
        context.fx().actionBarKey(player, "spell-no-target");
    }

    /**
     * Performs the complete teleportation sequence with all visual effects.
     */
    private void performTeleportation(SpellContext context, Player player,
            Location start, Location destination, SpellConfig config) {
        // Execute teleport
        if (!player.teleport(destination)) {
            handleTeleportFailure(context, player);
            return;
        }

        // Apply pulse effects to nearby entities
        applyPulseEffects(player, destination, config);

        // Create visual effects
        createDashTrail(context, start, destination, config);
        createImpactBurst(context, destination, config);
        createAuraEffect(context, player, config);

        // Play sounds
        playSpellSounds(context, player, destination, config);
    }

    /**
     * Applies healing/damage effects to entities within the pulse radius.
     */
    private void applyPulseEffects(Player player, Location center, SpellConfig config) {
        List<LivingEntity> affectedEntities = new ArrayList<>();

        // Collect entities within radius (with distance culling if enabled)
        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.getLocation().distance(center) <= config.pulseRadius && !entity.equals(player)) {
                if (!config.enableDistanceCulling ||
                        entity.getLocation().distance(player.getLocation()) <= MAX_RENDER_DISTANCE) {
                    affectedEntities.add(entity);
                }
            }
        }

        // Apply effects to collected entities
        for (LivingEntity entity : affectedEntities) {
            boolean isAlly = entity instanceof Player &&
                    ((Player) entity).getUniqueId().equals(player.getUniqueId());

            if (isAlly) {
                // Heal ally
                var attr = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                double maxHealth = attr != null ? attr.getValue() : entity.getHealth();
                double newHealth = Math.min(maxHealth, entity.getHealth() + config.allyHeal);
                entity.setHealth(newHealth);
            } else {
                // Damage enemy
                if ((entity instanceof Player && config.hitPlayers) ||
                        (!(entity instanceof Player) && config.hitMobs)) {
                    entity.damage(config.enemyDamage, player);
                }
            }
        }
    }

    /**
     * Creates an animated dash trail with modern particle effects.
     */
    private void createDashTrail(SpellContext context, Location start, Location end, SpellConfig config) {
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();

        if (distance == 0)
            return;

        direction.normalize();
        int steps = (int) Math.ceil(distance / 0.3);
        double stepSize = distance / steps;

        // Create trail animation task
        new BukkitRunnable() {
            private int tick = 0;
            private final int maxTicks = TRAIL_DURATION_TICKS;

            @Override
            public void run() {
                if (tick >= maxTicks) {
                    this.cancel();
                    return;
                }

                // Calculate trail opacity based on animation progress
                double progress = (double) tick / maxTicks;
                double opacity = 1.0 - (progress * 0.7); // Fade out over time

                // Spawn trail particles along the path
                for (int i = 0; i <= steps; i++) {
                    double d = i * stepSize;
                    Location particleLoc = start.clone().add(direction.clone().multiply(d));

                    // Add slight randomization for more natural effect
                    double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2;
                    double offsetY = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2;
                    double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2;

                    particleLoc.add(offsetX, offsetY, offsetZ);

                    // Spawn trail particle with fading effect
                    spawnParticle(context, particleLoc, config.trailParticle,
                            config.trailColor, opacity, 1, config);
                }

                tick++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * Creates an impact burst effect at the destination.
     */
    private void createImpactBurst(SpellContext context, Location location, SpellConfig config) {
        new BukkitRunnable() {
            private int tick = 0;
            private final int maxTicks = IMPACT_DURATION_TICKS;

            @Override
            public void run() {
                if (tick >= maxTicks) {
                    this.cancel();
                    return;
                }

                double progress = (double) tick / maxTicks;
                double radius = progress * config.pulseRadius;
                int particleCount = (int) (BURST_PARTICLE_COUNT * (1.0 - progress));

                // Create expanding burst ring
                for (int i = 0; i < particleCount; i++) {
                    double angle = (2 * Math.PI * i) / particleCount;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location particleLoc = location.clone().add(x, 0.5, z);

                    spawnParticle(context, particleLoc, config.burstParticle,
                            config.burstColor, 1.0, 2, config);
                }

                tick++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * Creates a rotating aura effect around the player.
     */
    private void createAuraEffect(SpellContext context, Player player, SpellConfig config) {
        new BukkitRunnable() {
            private int tick = 0;
            private final int maxTicks = AURA_DURATION_TICKS;

            @Override
            public void run() {
                if (tick >= maxTicks || !player.isValid()) {
                    this.cancel();
                    return;
                }

                Location center = player.getLocation().clone().add(0, 1, 0);
                double progress = (double) tick / maxTicks;
                double radius = 1.5 * (1.0 - progress * 0.5); // Shrinking aura
                double height = 2.0 * (1.0 - progress);

                // Create rotating aura particles
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI * i) / 12 + (tick * 0.3); // Rotation
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = Math.sin(tick * 0.2) * height * 0.5; // Vertical oscillation

                    Location particleLoc = center.clone().add(x, y, z);

                    spawnParticle(context, particleLoc, config.auraParticle,
                            config.auraColor, 0.8, 1, config);
                }

                tick++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * Optimized particle spawning with batching and distance culling.
     */
    private void spawnParticle(SpellContext context, Location location, Particle particle,
            Color color, double brightness, int count, SpellConfig config) {
        if (config.enableDistanceCulling &&
                location.distance(context.caster().getLocation()) > MAX_RENDER_DISTANCE) {
            return;
        }

        // Use modern Particle.DustOptions for colored particles
        if (particle == Particle.DUST || particle == Particle.DUST_COLOR_TRANSITION) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(
                    Color.fromRGB(
                            (int) (color.getRed() * brightness),
                            (int) (color.getGreen() * brightness),
                            (int) (color.getBlue() * brightness)),
                    1.0f);
            context.fx().spawnParticles(location, particle, count, 0, 0, 0, 0, dustOptions);
        } else {
            context.fx().spawnParticles(location, particle, count, 0, 0, 0, 0);
        }
    }

    /**
     * Plays all spell-related sounds with appropriate timing.
     */
    private void playSpellSounds(SpellContext context, Player player, Location destination, SpellConfig config) {
        // Teleport sound at start location
        context.fx().playSound(player.getLocation(), config.teleportSound, 1.0f, 1.2f);

        // Impact sound at destination
        new BukkitRunnable() {
            @Override
            public void run() {
                context.fx().playSound(destination, config.impactSound, 1.0f, 0.8f);
            }
        }.runTaskLater(context.plugin(), 2L);

        // Aura sound with delay
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isValid()) {
                    context.fx().playSound(player.getLocation(), config.auraSound, 0.7f, 1.5f);
                }
            }
        }.runTaskLater(context.plugin(), 5L);
    }

    @Override
    public String getName() {
        return "sunburst-step";
    }

    @Override
    public String key() {
        return "sunburst-step";
    }

    @Override
    public Component displayName() {
        return Component.text("Sunburst Step");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }

    /**
     * Configuration class for spell customization.
     * All values are loaded from config with validation and defaults.
     */
    private static class SpellConfig {
        // Core mechanics
        double maxDistance = 12.0;
        double pulseRadius = 4.0;
        double allyHeal = 2.0;
        double enemyDamage = 3.0;

        // Targeting flags
        boolean hitPlayers = true;
        boolean hitMobs = true;

        // Visual customization
        Color trailColor = Color.fromRGB(255, 215, 0); // Gold
        Color burstColor = Color.fromRGB(255, 165, 0); // Orange
        Color auraColor = Color.fromRGB(255, 255, 0); // Yellow

        Particle trailParticle = Particle.DUST;
        Particle burstParticle = Particle.EXPLOSION;
        Particle auraParticle = Particle.GLOW;

        // Audio customization
        Sound teleportSound = Sound.ENTITY_ENDERMAN_TELEPORT;
        Sound impactSound = Sound.ENTITY_GENERIC_EXPLODE;
        Sound auraSound = Sound.BLOCK_AMETHYST_BLOCK_CHIME;

        // Performance settings
        boolean enableDistanceCulling = true;
    }
}
