package nl.wantedchef.empirewand.core.services;

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

import net.kyori.adventure.text.Component;

/**
 * Centralized service for handling visual and audio effects (FX).
 * <p>
 * This class provides a robust and performant way to spawn particles, play
 * sounds, and send messages to players.
 * It includes features like particle batching to prevent server overload from
 * high-frequency effects.
 */
public class FxService implements EffectService {
    private static final Logger LOGGER = Logger.getLogger(FxService.class.getName());

    private final TextService textService;
    private final PerformanceMonitor performanceMonitor;

    // Performance optimization: batch particle operations
    private static final int MAX_BATCH_SIZE = 50;
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
         * @param count    The number of particles.
         * @param offsetX  The random offset on the X axis.
         * @param offsetY  The random offset on the Y axis.
         * @param offsetZ  The random offset on the Z axis.
         * @param speed    The speed of the particles.
         * @param data     The data for the particle (e.g., DustOptions).
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
            if (location == null || particle == null || count <= 0)
                return;
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
     * @param textService        The text service for message formatting.
     * @param performanceMonitor The performance monitor for tracking effect-related
     *                           timings.
     */
    public FxService(TextService textService, PerformanceMonitor performanceMonitor) {
        if (textService == null) {
            throw new IllegalArgumentException("TextService cannot be null");
        }
        if (performanceMonitor == null) {
            throw new IllegalArgumentException("PerformanceMonitor cannot be null");
        }
        this.textService = textService;
        this.performanceMonitor = performanceMonitor;
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
     * @param player  The player to receive the effect.
     * @param message The message to display.
     * @param sound   The sound to play.
     * @param volume  The volume of the sound.
     * @param pitch   The pitch of the sound.
     */
    public void actionBarSound(Player player, Component message, Sound sound, float volume, float pitch) {
        actionBar(player, message);
        playSound(player, sound, volume, pitch);
    }

    /**
     * Displays an action bar message from a message key and plays a sound to the
     * player.
     *
     * @param player     The player to receive the effect.
     * @param messageKey The key of the message to display.
     * @param sound      The sound to play.
     * @param volume     The volume of the sound.
     * @param pitch      The pitch of the sound.
     */
    public void actionBarSound(Player player, String messageKey, Sound sound, float volume, float pitch) {
        String raw = textService.getMessage(messageKey);
        actionBarSound(player, Component.text(raw), sound, volume, pitch);
    }

    /**
     * Displays a formatted action bar message and plays a sound to the player.
     *
     * @param player       The player to receive the effect.
     * @param messageKey   The key of the message to display.
     * @param placeholders The placeholders to insert into the message.
     * @param sound        The sound to play.
     * @param volume       The volume of the sound.
     * @param pitch        The pitch of the sound.
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
     * @param player      The player.
     * @param displayName The display name of the selected spell.
     */
    public void selectedSpell(@NotNull Player player, @NotNull String displayName) {
        actionBarKey(player, "spell-selected", Map.of("spell", textService.stripMiniTags(displayName)));
    }

    /**
     * Informs the player that a spell is on cooldown.
     *
     * @param player      The player.
     * @param displayName The display name of the spell on cooldown.
     * @param msRemaining The remaining cooldown time in milliseconds.
     */
    public void onCooldown(@NotNull Player player, @NotNull String displayName, long msRemaining) {
        double secondsRemaining = Math.max(0.1, msRemaining / 1000.0);
        String formattedTime = String.format(java.util.Locale.US, "%.1f", secondsRemaining);

        actionBarKey(player, "on-cooldown", Map.of(
                "spell", textService.stripMiniTags(displayName),
                "remaining", formattedTime));
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
    @SuppressWarnings({ "ConstantConditions", "DataFlowIssue" })
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
     * @param player  The player.
     * @param profile The name of the sound profile (e.g., "success", "error").
     */
    public void playUISound(@NotNull Player player, @NotNull String profile) {
        switch (profile.toLowerCase()) {
            case "success" -> playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
            case "error" -> playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
            case "warning" -> playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 0.8f);
            case "info" -> playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
            case "cast" -> playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
            case "select" -> playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.8f);
            case "cooldown" -> playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
            default -> playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        }
    }

    // ---- Standardized messages ----

    @Override
    public void showError(@NotNull Player player, @NotNull String errorType) {
        showError(player, errorType, Map.of());
    }

    /**
     * Shows a standardized error message to the player, with an associated sound.
     *
     * @param player       The player.
     * @param errorType    The type of error, used to determine the message key and
     *                     sound.
     * @param placeholders Placeholders for the message.
     */
    public void showError(@NotNull Player player, @NotNull String errorType,
            @NotNull Map<String, String> placeholders) {
        String messageKey = "error." + errorType;
        String soundProfile = switch (errorType) {
            case "no-permission" -> "error";
            case "on-cooldown" -> "cooldown";
            case "invalid-target" -> "warning";
            case "out-of-range" -> "warning";
            case "spell-disabled" -> "error";
            default -> "error";
        };

        actionBarKey(player, messageKey, placeholders);
        playUISound(player, soundProfile);
    }

    @Override
    public void showSuccess(@NotNull Player player, @NotNull String successType) {
        showSuccess(player, successType, Map.of());
    }

    /**
     * Shows a standardized success message to the player, with an associated sound.
     *
     * @param player       The player.
     * @param successType  The type of success, used to determine the message key.
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
     * Shows a standardized informational message to the player, with an associated
     * sound.
     *
     * @param player       The player.
     * @param infoType     The type of info, used to determine the message key.
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
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("playSoundPlayer");
        try {
            Location location = player.getLocation();
            if (location != null) {
                player.playSound(location, sound, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to play sound for player", e);
        } finally {
            timing.complete(2); // Log if sound playing takes > 2ms
        }
    }

    @Override
    public void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch) {
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("playSoundLocation");
        try {
            World world = location.getWorld();
            if (world != null) {
                world.playSound(location, sound, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to play sound at location", e);
        } finally {
            timing.complete(2);
        }
    }

    // ---- Particle helpers ----

    @Override
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed) {
        if (count <= 0) {
            return;
        }
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("spawnParticles");
        try {
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to spawn particles", e);
        } finally {
            timing.complete(5);
        }
    }

    @Override
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        if (count <= 0) {
            return;
        }
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("spawnParticlesWithData");
        try {
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed, data);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to spawn particles with data", e);
        } finally {
            timing.complete(5);
        }
    }

    /**
     * Adds a particle effect to a batch for later execution. This is more
     * performant than spawning particles directly
     * when dealing with a high volume of effects.
     *
     * @param location The location to spawn the particles.
     * @param particle The particle type to spawn.
     * @param count    The number of particles.
     * @param offsetX  The random offset on the X axis.
     * @param offsetY  The random offset on the Y axis.
     * @param offsetZ  The random offset on the Z axis.
     * @param speed    The speed of the particles.
     * @param data     The data for the particle (e.g., DustOptions).
     */
    @Override
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        synchronized (particleBatchLock) {
            particleBatch.add(new ParticleBatch(location, particle, count, offsetX, offsetY, offsetZ, speed, data));
            if (particleBatch.size() >= MAX_BATCH_SIZE) {
                flushParticleBatch();
            }
        }
    }

    /**
     * Adds a particle effect to a batch for later execution.
     *
     * @param location The location to spawn the particles.
     * @param particle The particle type to spawn.
     * @param count    The number of particles.
     * @param offsetX  The random offset on the X axis.
     * @param offsetY  The random offset on the Y axis.
     * @param offsetZ  The random offset on the Z axis.
     * @param speed    The speed of the particles.
     */
    @Override
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed) {
        batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, null);
    }

    /**
     * Executes all pending particle effects in the batch.
     * This should be called at the end of a tick or a high-frequency operation.
     */
    @Override
    public void flushParticleBatch() {
        synchronized (particleBatchLock) {
            if (particleBatch.isEmpty())
                return;

            PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("flushParticleBatch");

            try {
                // Copy to avoid concurrent modification
                List<ParticleBatch> batchCopy = new ArrayList<>(particleBatch);
                particleBatch.clear();

                for (ParticleBatch batch : batchCopy) {
                    batch.execute();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to flush particle batch", e);
            } finally {
                timing.complete(10); // Log if batch flush takes > 10ms
            }
        }
    }

    /**
     * Creates a particle trail between two locations.
     *
     * @param start    The start location.
     * @param end      The end location.
     * @param particle The particle to use for the trail.
     * @param perStep  The number of particles to spawn at each step.
     */
    @Override
    public void trail(@NotNull Location start, @NotNull Location end, @NotNull Particle particle, int perStep) {
        if (perStep <= 0) {
            return;
        }
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("trail");
        try {
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
        } finally {
            timing.complete(15); // Trail operations can be more expensive
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
        PerformanceMonitor.TimingContext timing = performanceMonitor.startTiming("impact");
        try {
            spawnParticles(location, particle, count, spread, spread, spread, 0);
            playSound(location, sound, volume, pitch);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create impact effect", e);
        } finally {
            timing.complete(5);
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
     * @param plugin      The plugin instance.
     * @param entity      The entity to follow.
     * @param particle    The particle to spawn.
     * @param count       The number of particles.
     * @param offsetX     The random offset on the X axis.
     * @param offsetY     The random offset on the Y axis.
     * @param offsetZ     The random offset on the Z axis.
     * @param speed       The speed of the particles.
     * @param data        The data for the particle (e.g., DustOptions).
     * @param periodTicks The period in ticks between particle spawns.
     */
    @Override
    public void followParticles(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed, Object data,
            long periodTicks) {
        if (periodTicks <= 0)
            return;
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
     * @param plugin      The plugin instance.
     * @param entity      The entity to follow.
     * @param periodTicks The period in ticks between trail updates.
     */
    @Override
    public void followTrail(@NotNull Plugin plugin, @NotNull Entity entity, long periodTicks) {
        if (periodTicks <= 0)
            return;
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
     * Shuts down the FxService and flushes any pending particle batches.
     * This method should be called during plugin shutdown to prevent memory leaks.
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
}
