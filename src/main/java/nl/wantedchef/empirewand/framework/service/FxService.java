package nl.wantedchef.empirewand.framework.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import nl.wantedchef.empirewand.api.service.EffectService;
import nl.wantedchef.empirewand.core.text.TextService;
import nl.wantedchef.empirewand.core.util.PerformanceMonitor;
import nl.wantedchef.empirewand.core.logging.StructuredLogger;

import net.kyori.adventure.text.Component;

/**
 * Centralized service for handling visual and audio effects (FX).
 * <p>
 * This class provides a robust and performant way to spawn particles, play
 * sounds, and send messages to players. It includes features like particle
 * batching to prevent server overload from high-frequency effects.
 */
public class FxService implements EffectService {

    private static final Logger LOGGER = Logger.getLogger(FxService.class.getName());

    private final TextService textService;
    private final PerformanceMonitor performanceMonitor;
    private final StructuredLogger structuredLogger;

    private static final int AUTO_FLUSH_THRESHOLD = 75; // Auto-flush at 75% capacity
    private final List<ParticleBatch> particleBatch = new ArrayList<>();
    private final Object particleBatchLock = new Object(); // thread-safety

    /**
     * Represents a single, self-contained particle effect to be executed in a
     * batch.
     */
    private static class ParticleBatch {

        final Location location;
        final Particle particle;
        final int count;
        final double offsetX, offsetY, offsetZ, speed;
        final Object data;

        /**
         * Constructs a new ParticleBatch.
         *
         * @param location The location to spawn the particles.
         * @param particle The particle type to spawn.
         * @param count The number of particles.
         * @param offsetX The random offset on the X axis.
         * @param offsetY The random offset on the Y axis.
         * @param offsetZ The random offset on the Z axis.
         * @param speed The speed of the particles.
         * @param data The data for the particle (e.g., DustOptions).
         */
        ParticleBatch(Location location, Particle particle, int count,
                double offsetX, double offsetY, double offsetZ, double speed, Object data) {
            this.location = location;
            this.particle = particle;
            this.count = count;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
            this.data = data;
        }

        /**
         * Executes the particle spawning operation in the world.
         */
        void execute() {
            if (location == null || particle == null || count <= 0) {
                return;
            }
            World world = location.getWorld();
            if (world != null) {
                try {
                    if (data != null) {
                        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed, data);
                    } else {
                        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to execute particle batch", e);
                }
            }
        }
    }

    /**
     * Constructs a new FxService.
     *
     * @param textService The text service for message formatting.
     * @param performanceMonitor The performance monitor for tracking
     * effect-related timings.
     * @param structuredLogger The structured logger for enhanced logging.
     */
    public FxService(TextService textService, PerformanceMonitor performanceMonitor, StructuredLogger structuredLogger) {
        if (textService == null) {
            throw new IllegalArgumentException("TextService cannot be null");
        }
        if (performanceMonitor == null) {
            throw new IllegalArgumentException("PerformanceMonitor cannot be null");
        }
        if (structuredLogger == null) {
            throw new IllegalArgumentException("StructuredLogger cannot be null");
        }
        this.textService = textService;
        this.performanceMonitor = performanceMonitor;
        this.structuredLogger = structuredLogger;
    }

    // ---- Action bar helpers ----
    @Override
    public void actionBar(@NotNull Player player, @NotNull Component message) {
        try {
            player.sendActionBar(message);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send action bar message", e);
        }
    }

    @Override
    public void actionBar(@NotNull Player player, @NotNull String plainText) {
        if (plainText.trim().isEmpty()) {
            return;
        }
        try {
            player.sendActionBar(Component.text(plainText));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send action bar message", e);
        }
    }

    @Override
    public void actionBarKey(@NotNull Player player, @NotNull String messageKey) {
        String raw = textService.getMessage(messageKey);
        actionBar(player, raw);
    }

    @Override
    public void actionBarKey(@NotNull Player player, @NotNull String messageKey,
            @NotNull Map<String, String> placeholders) {
        String raw = textService.getMessage(messageKey, placeholders);
        actionBar(player, raw);
    }

    /**
     * Displays an action bar message and plays a sound to the player.
     *
     * @param player The player to receive the effect.
     * @param message The message to display.
     * @param sound The sound to play.
     * @param volume The volume of the sound.
     * @param pitch The pitch of the sound.
     */
    public void actionBarSound(Player player, Component message, Sound sound, float volume, float pitch) {
        actionBar(player, message);
        playSound(player, sound, volume, pitch);
    }

    /**
     * Displays an action bar message from a message key and plays a sound to
     * the player.
     *
     * @param player The player to receive the effect.
     * @param messageKey The key of the message to display.
     * @param sound The sound to play.
     * @param volume The volume of the sound.
     * @param pitch The pitch of the sound.
     */
    public void actionBarSound(Player player, String messageKey, Sound sound, float volume, float pitch) {
        String raw = textService.getMessage(messageKey);
        actionBarSound(player, Component.text(raw), sound, volume, pitch);
    }

    /**
     * Displays a formatted action bar message and plays a sound to the player.
     *
     * @param player The player to receive the effect.
     * @param messageKey The key of the message to display.
     * @param placeholders The placeholders to insert into the message.
     * @param sound The sound to play.
     * @param volume The volume of the sound.
     * @param pitch The pitch of the sound.
     */
    public void actionBarSound(Player player, String messageKey, Map<String, String> placeholders,
            Sound sound, float volume, float pitch) {
        String raw = (placeholders == null || placeholders.isEmpty())
                ? textService.getMessage(messageKey)
                : textService.getMessage(messageKey, placeholders);
        actionBarSound(player, Component.text(raw), sound, volume, pitch);
    }

    /**
     * Shows the player which spell they have selected.
     *
     * @param player The player.
     * @param displayName The display name of the selected spell.
     */
    public void selectedSpell(@NotNull Player player, @NotNull String displayName) {
        actionBarKey(player, "spell-selected", Map.of("spell", textService.stripMiniTags(displayName)));
    }

    /**
     * Informs the player that a spell is on cooldown.
     *
     * @param player The player.
     * @param displayName The display name of the spell on cooldown.
     * @param msRemaining The remaining cooldown time in milliseconds.
     */
    public void onCooldown(@NotNull Player player, @NotNull String displayName, long msRemaining) {
        double secondsRemaining = Math.max(0.1, msRemaining / 1000.0);
        String formattedTime = String.format(java.util.Locale.US, "%.1f", secondsRemaining);

        actionBarKey(player, "cooldown-active", Map.of(
                "spell", textService.stripMiniTags(displayName),
                "remaining", formattedTime));
        playUISound(player, "cooldown");
    }

    /**
     * Informs the player that their wand has no spells bound.
     *
     * @param player The player.
     */
    public void noSpells(@NotNull Player player) {
        actionBarKey(player, "no-spells-bound");
    }

    /**
     * Informs the player that they do not have permission for an action.
     *
     * @param player The player.
     */
    public void noPermission(@NotNull Player player) {
        actionBarKey(player, "no-permission");
    }

    @Override
    @SuppressWarnings({"ConstantConditions", "DataFlowIssue"})
    public void fizzle(@NotNull Player player) {
        actionBarKey(player, "fizzle");
        fizzle(java.util.Objects.requireNonNull(player.getLocation(), "Player location was null"));
    }

    // ---- Title/Subtitle helpers ----
    @Override
    public void title(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle) {
        try {
            player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to show title", e);
        }
    }

    @Override
    public void title(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle,
            int fadeIn, int stay, int fadeOut) {
        try {
            var times = net.kyori.adventure.title.Title.Times.times(
                    java.time.Duration.ofMillis((long) fadeIn * 50),
                    java.time.Duration.ofMillis((long) stay * 50),
                    java.time.Duration.ofMillis((long) fadeOut * 50));
            player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle, times));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to show title with timing", e);
        }
    }

    // ---- Sound profiles ----
    /**
     * Plays a standardized UI sound to the player.
     *
     * @param player The player.
     * @param profile The name of the sound profile (e.g., "success", "error").
     */
    public void playUISound(@NotNull Player player, @NotNull String profile) {
        switch (profile.toLowerCase()) {
            case "success" ->
                playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
            case "error" ->
                playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
            case "warning" ->
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 0.8f);
            case "info" ->
                playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
            case "cast" ->
                playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
            case "select" ->
                playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.8f);
            case "cooldown" ->
                playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
            default ->
                playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        }
    }

    // ---- Standardized messages ----
    @Override
    public void showError(@NotNull Player player, @NotNull String errorType) {
        showError(player, errorType, Map.of());
    }

    /**
     * Shows a standardized error message to the player, with an associated
     * sound.
     *
     * @param player The player.
     * @param errorType The type of error, used to determine the message key and
     * sound.
     * @param placeholders Placeholders for the message.
     */
    public void showError(@NotNull Player player, @NotNull String errorType,
            @NotNull Map<String, String> placeholders) {
        String messageKey = "error." + errorType;
        String soundProfile = switch (errorType) {
            case "no-permission" ->
                "error";
            case "on-cooldown" ->
                "cooldown";
            case "invalid-target" ->
                "warning";
            case "out-of-range" ->
                "warning";
            case "spell-disabled" ->
                "error";
            default ->
                "error";
        };

        actionBarKey(player, messageKey, placeholders);
        playUISound(player, soundProfile);
    }

    @Override
    public void showSuccess(@NotNull Player player, @NotNull String successType) {
        showSuccess(player, successType, Map.of());
    }

    /**
     * Shows a standardized success message to the player, with an associated
     * sound.
     *
     * @param player The player.
     * @param successType The type of success, used to determine the message
     * key.
     * @param placeholders Placeholders for the message.
     */
    public void showSuccess(@NotNull Player player, @NotNull String successType,
            @NotNull Map<String, String> placeholders) {
        String messageKey = "success." + successType;
        actionBarKey(player, messageKey, placeholders);
        playUISound(player, "success");
    }

    @Override
    public void showInfo(@NotNull Player player, @NotNull String infoType) {
        showInfo(player, infoType, Map.of());
    }

    /**
     * Shows a standardized informational message to the player, with an
     * associated sound.
     *
     * @param player The player.
     * @param infoType The type of info, used to determine the message key.
     * @param placeholders Placeholders for the message.
     */
    public void showInfo(@NotNull Player player, @NotNull String infoType, @NotNull Map<String, String> placeholders) {
        String messageKey = "info." + infoType;
        actionBarKey(player, messageKey, placeholders);
        playUISound(player, "info");
    }

    // ---- Sound helpers ----
    @Override
    public void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch) {
        try (var timing = performanceMonitor.startTiming("playSoundPlayer", 2)) {
            timing.observe();
            assert timing != null; // ensure variable considered read
            Location location = player.getLocation();
            if (location != null) {
                player.playSound(location, sound, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to play sound for player", e);
        }
    }

    @Override
    public void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch) {
        try (var timing = performanceMonitor.startTiming("playSoundLocation", 2)) {
            timing.observe();
            assert timing != null;
            World world = location.getWorld();
            if (world != null) {
                world.playSound(location, sound, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to play sound at location", e);
        }
    }

    // ---- Particle helpers ----
    @Override
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed) {
        if (count <= 0) {
            return;
        }
        try (var timing = performanceMonitor.startTiming("spawnParticles", 5)) {
            timing.observe();
            assert timing != null;
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
                
                // Log particle spawning with structured logging
                structuredLogger.logPerformance("particle_spawned", 0L, 
                    Map.of("particle", particle.name(),
                           "count", count,
                           "world", world.getName(),
                           "x", location.getX(),
                           "y", location.getY(),
                           "z", location.getZ()));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to spawn particles", e);
            structuredLogger.logError("particle_spawn_failed", "Failed to spawn particles", 
                Map.of("particle", particle != null ? particle.name() : "null",
                       "error", e.getMessage()));
        }
    }

    @Override
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        if (count <= 0) {
            return;
        }
        try (var timing = performanceMonitor.startTiming("spawnParticlesWithData", 5)) {
            timing.observe();
            assert timing != null;
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed, data);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to spawn particles with data", e);
        }
    }

    /**
     * Adds a particle effect to a batch for later execution. This is more
     * performant than spawning particles directly when dealing with a high
     * volume of effects.
     *
     * @param location The location to spawn the particles.
     * @param particle The particle type to spawn.
     * @param count The number of particles.
     * @param offsetX The random offset on the X axis.
     * @param offsetY The random offset on the Y axis.
     * @param offsetZ The random offset on the Z axis.
     * @param speed The speed of the particles.
     * @param data The data for the particle (e.g., DustOptions).
     */
    @Override
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        synchronized (particleBatchLock) {
            particleBatch.add(new ParticleBatch(location, particle, count, offsetX, offsetY, offsetZ, speed, data));
            // Auto-flush when reaching threshold to prevent memory buildup
            if (particleBatch.size() >= AUTO_FLUSH_THRESHOLD) {
                flushParticleBatch();
            }
        }
    }

    /**
     * Adds a particle effect to a batch for later execution.
     *
     * @param location The location to spawn the particles.
     * @param particle The particle type to spawn.
     * @param count The number of particles.
     * @param offsetX The random offset on the X axis.
     * @param offsetY The random offset on the Y axis.
     * @param offsetZ The random offset on the Z axis.
     * @param speed The speed of the particles.
     */
    @Override
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed) {
        batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, null);
    }

    /**
     * Executes all pending particle effects in the batch. This should be called
     * at the end of a tick or a high-frequency operation.
     */
    @Override
    public void flushParticleBatch() {
        synchronized (particleBatchLock) {
            if (particleBatch.isEmpty()) {
                return;
            }

            try (var timing = performanceMonitor.startTiming("flushParticleBatch", 10)) {
                timing.observe();
                assert timing != null;
                // Copy to avoid concurrent modification
                List<ParticleBatch> batchCopy = new ArrayList<>(particleBatch);
                particleBatch.clear();

                for (ParticleBatch batch : batchCopy) {
                    batch.execute();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to flush particle batch", e);
            }
        }
    }

    /**
     * Creates a particle trail between two locations.
     *
     * @param start The start location.
     * @param end The end location.
     * @param particle The particle to use for the trail.
     * @param perStep The number of particles to spawn at each step.
     */
    @Override
    public void trail(@NotNull Location start, @NotNull Location end, @NotNull Particle particle, int perStep) {
        if (perStep <= 0) {
            return;
        }
        try (var timing = performanceMonitor.startTiming("trail", 15)) {
            timing.observe();
            assert timing != null;
            Vector dir = end.toVector().subtract(start.toVector());
            double length = dir.length();
            if (length <= 0.001) {
                return;
            }

            int steps = (int) Math.max(1, Math.round(length));
            Vector step = dir.normalize().multiply(length / steps);
            Location point = start.clone();

            for (int i = 0; i < steps; i++) {
                spawnParticles(point, particle, perStep, 0, 0, 0, 0);
                point.add(step);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create trail", e);
        }
    }

    /**
     * Creates a default particle trail at a location.
     *
     * @param location The location to create the trail at.
     */
    @Override
    public void trail(@NotNull Location location) {
        spawnParticles(location, Particle.SOUL_FIRE_FLAME, 10, 0.1, 0.1, 0.1, 0.05);
    }

    @Override
    public void impact(@NotNull Location location, @NotNull Particle particle, int count,
            @NotNull Sound sound, float volume, float pitch) {
        impact(location, particle, count, 0.2, sound, volume, pitch);
    }

    @Override
    public void impact(@NotNull Location location, @NotNull Particle particle, int count, double spread,
            @NotNull Sound sound, float volume, float pitch) {
        try (var timing = performanceMonitor.startTiming("impact", 5)) {
            timing.observe();
            assert timing != null;
            spawnParticles(location, particle, count, spread, spread, spread, 0);
            playSound(location, sound, volume, pitch);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create impact effect", e);
        }
    }

    /**
     * Creates a default impact effect at a location.
     *
     * @param location The location to create the impact effect at.
     */
    @Override
    public void impact(@NotNull Location location) {
        spawnParticles(location, Particle.EXPLOSION, 30, 0.5, 0.5, 0.5, 0.1);
        playSound(location, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }

    @Override
    public void fizzle(@NotNull Location location) {
        playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
        spawnParticles(location, Particle.SMOKE, 10, 0.1, 0.1, 0.1, 0.05);
    }

    /**
     * Creates a particle effect that follows an entity.
     *
     * @param plugin The plugin instance.
     * @param entity The entity to follow.
     * @param particle The particle to spawn.
     * @param count The number of particles.
     * @param offsetX The random offset on the X axis.
     * @param offsetY The random offset on the Y axis.
     * @param offsetZ The random offset on the Z axis.
     * @param speed The speed of the particles.
     * @param data The data for the particle (e.g., DustOptions).
     * @param periodTicks The period in ticks between particle spawns.
     */
    @Override
    public void followParticles(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed, Object data,
            long periodTicks) {
        if (periodTicks <= 0) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead()) {
                    cancel();
                    return;
                }
                if (entity instanceof Player player && !player.isOnline()) {
                    cancel();
                    return;
                }

                if (data != null) {
                    spawnParticles(entity.getLocation(), particle, count, offsetX, offsetY, offsetZ, speed, data);
                } else {
                    spawnParticles(entity.getLocation(), particle, count, offsetX, offsetY, offsetZ, speed);
                }
            }
        }.runTaskTimer(plugin, 0L, Math.max(1L, periodTicks));
    }

    /**
     * Creates a particle trail that follows an entity.
     *
     * @param plugin The plugin instance.
     * @param entity The entity to follow.
     * @param periodTicks The period in ticks between trail updates.
     */
    @Override
    public void followTrail(@NotNull Plugin plugin, @NotNull Entity entity, long periodTicks) {
        if (periodTicks <= 0) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead()) {
                    cancel();
                    return;
                }
                if (entity instanceof Player player && !player.isOnline()) {
                    cancel();
                    return;
                }
                trail(entity.getLocation());
            }
        }.runTaskTimer(plugin, 0L, Math.max(1L, periodTicks));
    }

    // ---- Lifecycle ----
    /**
     * Shuts down the FxService and flushes any pending particle batches. This
     * method should be called during plugin shutdown to prevent memory leaks.
     */
    public void shutdown() {
        synchronized (particleBatchLock) {
            flushParticleBatch();
            particleBatch.clear();
        }
    }

    // ===== EmpireWandService Implementation =====
    @Override
    public String getServiceName() {
        return "FxService";
    }

    @Override
    public nl.wantedchef.empirewand.api.Version getServiceVersion() {
        return nl.wantedchef.empirewand.api.Version.of(2, 0, 0);
    }

    @Override
    public boolean isEnabled() {
        return true; // FxService is always enabled
    }

    @Override
    public nl.wantedchef.empirewand.api.ServiceHealth getHealth() {
        return nl.wantedchef.empirewand.api.ServiceHealth.HEALTHY;
    }

    @Override
    public void reload() {
        // FxService doesn't have configuration to reload
        // Flush any pending particle batches
        flushParticleBatch();
    }

    // ---- String-based particle methods for backward compatibility ----
    
    /**
     * Spawns particles using string particle names for backward compatibility.
     * 
     * @param particleName The name of the particle (e.g., "CLOUD", "SNOWFLAKE")
     * @param location The location to spawn particles at
     * @param count The number of particles
     * @param offsetX The X offset
     * @param offsetY The Y offset 
     * @param offsetZ The Z offset
     * @param speed The particle speed
     */
    public void spawnParticle(String particleName, Location location, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Unknown particle type: " + particleName);
            // Fallback to a default particle
            spawnParticles(location, Particle.FLAME, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    /**
     * Spawns particles using string particle names with additional data.
     * 
     * @param particleName The name of the particle
     * @param location The location to spawn particles at
     * @param count The number of particles
     * @param offsetX The X offset
     * @param offsetY The Y offset
     * @param offsetZ The Z offset
     * @param speed The particle speed
     * @param data The particle data
     */
    public void spawnParticle(String particleName, Location location, int count, double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, data);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Unknown particle type: " + particleName);
            // Fallback to a default particle
            spawnParticles(location, Particle.FLAME, count, offsetX, offsetY, offsetZ, speed, data);
        }
    }

    // ---- String-based sound methods for backward compatibility ----

    /**
     * Plays a sound using string sound names for backward compatibility.
     * 
     * @param location The location to play the sound at
     * @param soundName The name of the sound (e.g., "BLOCK_GLASS_BREAK")
     * @param volume The volume of the sound
     * @param pitch The pitch of the sound
     */
    public void playSound(Location location, String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            playSound(location, sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Unknown sound type: " + soundName);
            // Fallback to a default sound
            playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume, pitch);
        }
    }

    /**
     * Plays a sound to a player using string sound names for backward compatibility.
     * 
     * @param player The player to play the sound to
     * @param soundName The name of the sound
     * @param volume The volume of the sound
     * @param pitch The pitch of the sound
     */
    public void playSound(Player player, String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            playSound(player, sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Unknown sound type: " + soundName);
            // Fallback to a default sound
            playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume, pitch);
        }
    }

    // ---- Additional utility methods ----

    /**
     * Sends an action bar message to a player using string text.
     * 
     * @param player The player to send the message to
     * @param message The message text (supports color codes)
     */
    public void sendActionBar(Player player, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        // Convert color codes to components
        String processedMessage = message.replace("&", "§");
        actionBar(player, Component.text(processedMessage));
    }
}
