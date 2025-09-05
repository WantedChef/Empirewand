package com.example.empirewand.core;

import com.example.empirewand.core.text.TextService;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Centralized FX helper for sounds, particles, and action bar messages.
 *
 * Keep this lightweight and safe to call from event handlers.
 * Includes performance optimizations and batching for high-frequency
 * operations.
 */
public class FxService {
    private final ConfigService config;
    private final TextService textService;

    // Performance optimization: batch particle operations
    private static final int MAX_BATCH_SIZE = 50;
    private final List<ParticleBatch> particleBatch = new ArrayList<>();

    /**
     * Internal class for batching particle operations.
     */
    private static class ParticleBatch {
        final Location location;
        final Particle particle;
        final int count;
        final double offsetX, offsetY, offsetZ, speed;
        final Object data;

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

        void execute() {
            if (location == null || particle == null || count <= 0)
                return;
            World world = location.getWorld();
            if (world != null) {
                if (data != null) {
                    world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed, data);
                } else {
                    world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
                }
            }
        }
    }

    public FxService(ConfigService config, TextService textService) {
        this.config = config;
        this.textService = textService;
    }

    // ---- Action bar helpers ----

    public void actionBar(Player player, Component message) {
        if (player != null && message != null) {
            player.sendActionBar(message);
        }
    }

    public void actionBar(Player player, String plainText) {
        if (player != null && plainText != null && !plainText.isEmpty()) {
            player.sendActionBar(Component.text(textService.stripMiniTags(plainText)));
        }
    }

    public void actionBarKey(Player player, String messageKey) {
        String raw = textService.getMessage(messageKey);
        actionBar(player, raw);
    }

    public void actionBarKey(Player player, String messageKey, Map<String, String> placeholders) {
        String raw = textService.getMessage(messageKey, placeholders);
        actionBar(player, raw);
    }

    public void actionBarSound(Player player, Component message, Sound sound, float volume, float pitch) {
        actionBar(player, message);
        playSound(player, sound, volume, pitch);
    }

    public void actionBarSound(Player player, String messageKey, Sound sound, float volume, float pitch) {
        String raw = textService.getMessage(messageKey);
        actionBarSound(player, Component.text(textService.stripMiniTags(raw)), sound, volume, pitch);
    }

    public void actionBarSound(Player player, String messageKey, Map<String, String> placeholders,
            Sound sound, float volume, float pitch) {
        String raw = textService.getMessage(messageKey, placeholders);
        actionBarSound(player, Component.text(textService.stripMiniTags(raw)), sound, volume, pitch);
    }

    public void selectedSpell(Player player, String displayName) {
        showInfo(player, "spell-selected", Map.of("spell", textService.stripMiniTags(displayName)));
    }

    public void onCooldown(Player player, String displayName, long msRemaining) {
        showError(player, "on-cooldown", Map.of(
                "spell", textService.stripMiniTags(displayName),
                "time", Long.toString(Math.max(0, msRemaining))));
    }

    public void noSpells(Player player) {
        showError(player, "no-spells-bound");
    }

    public void noPermission(Player player) {
        showError(player, "no-permission");
    }

    public void fizzle(Player player) {
        actionBarKey(player, "fizzle");
        if (player != null) {
            fizzle(player.getLocation());
        }
    }

    // ---- Title/Subtitle helpers ----

    public void title(Player player, Component title, Component subtitle) {
        if (player != null) {
            player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle));
        }
    }

    public void title(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (player != null) {
            var times = net.kyori.adventure.title.Title.Times.times(
                    java.time.Duration.ofMillis(fadeIn * 50),
                    java.time.Duration.ofMillis(stay * 50),
                    java.time.Duration.ofMillis(fadeOut * 50));
            player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle, times));
        }
    }

    public void title(Player player, String titleKey, String subtitleKey) {
        Component titleComp = textService.parseMiniMessage(textService.getMessage(titleKey));
        Component subtitleComp = textService.parseMiniMessage(textService.getMessage(subtitleKey));
        title(player, titleComp, subtitleComp);
    }

    // ---- Sound profiles ----

    public void playUISound(Player player, String profile) {
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

    // ---- Standardized error messages ----

    public void showError(Player player, String errorType) {
        showError(player, errorType, Map.of());
    }

    public void showError(Player player, String errorType, Map<String, String> placeholders) {
        String messageKey = "error." + errorType;
        String soundProfile = switch (errorType) {
            case "no-permission" -> "error";
            case "on-cooldown" -> "cooldown";
            case "invalid-target" -> "warning";
            case "out-of-range" -> "warning";
            case "no-mana" -> "error";
            case "spell-disabled" -> "error";
            default -> "error";
        };

        actionBarKey(player, messageKey, placeholders);
        playUISound(player, soundProfile);
    }

    public void showSuccess(Player player, String successType) {
        showSuccess(player, successType, Map.of());
    }

    public void showSuccess(Player player, String successType, Map<String, String> placeholders) {
        String messageKey = "success." + successType;
        actionBarKey(player, messageKey, placeholders);
        playUISound(player, "success");
    }

    public void showInfo(Player player, String infoType) {
        showInfo(player, infoType, Map.of());
    }

    public void showInfo(Player player, String infoType, Map<String, String> placeholders) {
        String messageKey = "info." + infoType;
        actionBarKey(player, messageKey, placeholders);
        playUISound(player, "info");
    }

    // ---- Sound helpers ----

    public void playSound(Player player, Sound sound, float volume, float pitch) {
        PerformanceMonitor.TimingContext timing = PerformanceMonitor.startTiming("playSoundPlayer");

        if (player != null && sound != null && player.getLocation() != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }

        timing.complete(2); // Log if sound playing takes > 2ms
    }

    public void playSound(Location location, Sound sound, float volume, float pitch) {
        PerformanceMonitor.TimingContext timing = PerformanceMonitor.startTiming("playSoundLocation");

        if (location == null || sound == null) {
            timing.complete(1);
            return;
        }

        World world = location.getWorld();
        if (world != null) {
            world.playSound(location, sound, volume, pitch);
        }

        timing.complete(2);
    }

    // ---- Particle helpers ----

    public void spawnParticles(Location location, Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed) {
        PerformanceMonitor.TimingContext timing = PerformanceMonitor.startTiming("spawnParticles");

        if (location == null || particle == null || count <= 0) {
            timing.complete(1); // Very short threshold for validation failures
            return;
        }

        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        }

        timing.complete(5); // Log if particle spawning takes > 5ms
    }

    public void spawnParticles(Location location, Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        PerformanceMonitor.TimingContext timing = PerformanceMonitor.startTiming("spawnParticlesWithData");

        if (location == null || particle == null || count <= 0) {
            timing.complete(1);
            return;
        }

        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed, data);
        }

        timing.complete(5);
    }

    /**
     * Batches multiple particle operations for improved performance.
     * Automatically flushes when batch size reaches MAX_BATCH_SIZE.
     */
    public void batchParticles(Location location, Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        particleBatch.add(new ParticleBatch(location, particle, count, offsetX, offsetY, offsetZ, speed, data));

        if (particleBatch.size() >= MAX_BATCH_SIZE) {
            flushParticleBatch();
        }
    }

    /**
     * Batches multiple particle operations for improved performance.
     * Automatically flushes when batch size reaches MAX_BATCH_SIZE.
     */
    public void batchParticles(Location location, Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed) {
        batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, null);
    }

    /**
     * Flushes all batched particle operations.
     * Should be called at the end of high-frequency operations.
     */
    public void flushParticleBatch() {
        if (particleBatch.isEmpty())
            return;

        PerformanceMonitor.TimingContext timing = PerformanceMonitor.startTiming("flushParticleBatch");

        for (ParticleBatch batch : particleBatch) {
            batch.execute();
        }

        particleBatch.clear();

        timing.complete(10); // Log if batch flush takes > 10ms
    }

    public void trail(Location start, Location end, Particle particle, int perStep) {
        PerformanceMonitor.TimingContext timing = PerformanceMonitor.startTiming("trail");

        if (start == null || end == null || particle == null || perStep <= 0) {
            timing.complete(1);
            return;
        }

        Vector dir = end.toVector().subtract(start.toVector());
        double length = dir.length();
        if (length <= 0.001) {
            timing.complete(1);
            return;
        }

        int steps = Math.max(1, (int) (length * 4));
        Vector step = dir.normalize().multiply(length / steps);
        Location point = start.clone();

        for (int i = 0; i < steps; i++) {
            spawnParticles(point, particle, perStep, 0, 0, 0, 0);
            point.add(step);
        }

        timing.complete(15); // Trail operations can be more expensive
    }

    public void impact(Location location, Particle particle, int count, Sound sound, float volume, float pitch) {
        impact(location, particle, count, 0.2, sound, volume, pitch);
    }

    public void impact(Location location, Particle particle, int count, double spread, Sound sound, float volume,
            float pitch) {
        PerformanceMonitor.TimingContext timing = PerformanceMonitor.startTiming("impact");

        if (location == null) {
            timing.complete(1);
            return;
        }

        spawnParticles(location, particle, count, spread, spread, spread, 0);
        playSound(location, sound, volume, pitch);

        timing.complete(5);
    }

    /**
     * Subtle failure effect: short extinguish + smoke.
     */
    public void fizzle(Location location) {
        playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
        spawnParticles(location, Particle.SMOKE, 10, 0.1, 0.1, 0.1, 0.05);
    }

    /**
     * Generic projectile trail effect.
     */
    public void trail(Location location) {
        spawnParticles(location, Particle.SOUL_FIRE_FLAME, 10, 0.1, 0.1, 0.1, 0.05);
    }

    /**
     * Generic projectile impact effect.
     */
    public void impact(Location location) {
        spawnParticles(location, Particle.EXPLOSION, 30, 0.5, 0.5, 0.5, 0.1);
        playSound(location, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }
}
