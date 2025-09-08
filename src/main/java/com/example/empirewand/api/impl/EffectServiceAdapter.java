package com.example.empirewand.api.impl;

import com.example.empirewand.api.EffectService;
import com.example.empirewand.api.ServiceHealth;
import com.example.empirewand.api.Version;
import com.example.empirewand.api.common.MainThread;
import com.example.empirewand.core.services.FxService;
import com.example.empirewand.core.services.ThreadingGuard;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import java.util.Map;

/**
 * Adapter for EffectService that wraps FxService core implementation.
 * Delegates all effect operations to core while providing API contract.
 * Implements EmpireWandService base methods with defaults.
 *
 * <h2>Usage Example:</h2>
 * 
 * <pre>{@code
 * // Get the effect service from the API
 * EffectService effects = EmpireWandAPI.getProvider().getEffectService();
 *
 * // Display messages to players
 * effects.actionBar(player, "Spell ready!");
 * effects.actionBarKey(player, "spell-cast", Map.of("spell", "Fireball"));
 *
 * // Show titles
 * effects.title(player,
 *         Component.text("Spell Cast!"),
 *         Component.text("Fireball"),
 *         10, 40, 10);
 *
 * // Play sounds
 * effects.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
 *
 * // Create particle effects
 * effects.spawnParticles(location, Particle.FLAME, 20, 0.5, 0.5, 0.5, 0.1);
 *
 * // Batch particles for performance
 * effects.batchParticles(location, Particle.SMOKE, 10, 0.1, 0.1, 0.1, 0.05);
 * effects.flushParticleBatch();
 *
 * // Create trails and impacts
 * effects.trail(startLocation, endLocation, Particle.SOUL_FIRE_FLAME, 5);
 * effects.impact(location, Particle.EXPLOSION, 30, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
 *
 * // Follow entities with effects
 * effects.followParticles(plugin, entity, Particle.FLAME, 5, 0, 0, 0, 0.1, null, 20L);
 * }</pre>
 *
 * @since 2.0.0
 */
public class EffectServiceAdapter implements EffectService {

    private final FxService core;

    /**
     * Constructor.
     *
     * @param core the core FxService to wrap
     */
    public EffectServiceAdapter(FxService core) {
        if (core == null) {
            throw new IllegalArgumentException("core FxService cannot be null");
        }
        this.core = core;
    }

    // EmpireWandService implementations

    @Override
    public @NotNull String getServiceName() {
        return "EffectService";
    }

    @Override
    public @NotNull Version getServiceVersion() {
        return Version.of(2, 0, 0);
    }

    @Override
    public boolean isEnabled() {
        return true; // Assume enabled if core is injected
    }

    @Override
    public @NotNull ServiceHealth getHealth() {
        try {
            // Check if core service is available
            if (core == null) {
                return ServiceHealth.UNHEALTHY;
            }
            
            // Additional health checks can be added here
            // For example, check if the service can perform basic operations
            return ServiceHealth.HEALTHY;
        } catch (Exception e) {
            return ServiceHealth.UNHEALTHY;
        }
    }

    @Override
    public void reload() {
        try {
            // Flush any pending particle batches on reload
            if (core != null) {
                core.flushParticleBatch();
            }
        } catch (Exception e) {
            // Log error but don't propagate - graceful degradation
            System.err.println("Failed to reload effect service: " + e.getMessage());
        }
    }

    // EffectService implementations

    @Override
    @MainThread
    public void actionBar(@NotNull Player player, @NotNull Component message) {
        ThreadingGuard.ensureMain();
        core.actionBar(player, message);
    }

    @Override
    @MainThread
    public void actionBar(@NotNull Player player, @NotNull String plainText) {
        ThreadingGuard.ensureMain();
        core.actionBar(player, plainText);
    }

    @Override
    @MainThread
    public void actionBarKey(@NotNull Player player, @NotNull String messageKey) {
        ThreadingGuard.ensureMain();
        core.actionBarKey(player, messageKey);
    }

    @Override
    @MainThread
    public void actionBarKey(@NotNull Player player, @NotNull String messageKey,
            @NotNull java.util.Map<String, String> placeholders) {
        ThreadingGuard.ensureMain();
        core.actionBarKey(player, messageKey, placeholders);
    }

    @Override
    @MainThread
    public void title(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle) {
        ThreadingGuard.ensureMain();
        core.title(player, title, subtitle);
    }

    @Override
    @MainThread
    public void title(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle, int fadeIn,
            int stay, int fadeOut) {
        ThreadingGuard.ensureMain();
        core.title(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    @MainThread
    public void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch) {
        ThreadingGuard.ensureMain();
        core.playSound(player, sound, volume, pitch);
    }

    @Override
    @MainThread
    public void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch) {
        ThreadingGuard.ensureMain();
        core.playSound(location, sound, volume, pitch);
    }

    @Override
    @MainThread
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed) {
        ThreadingGuard.ensureMain();
        core.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    @MainThread
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed, Object data) {
        ThreadingGuard.ensureMain();
        core.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, data);
    }

    @Override
    @MainThread
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed) {
        ThreadingGuard.ensureMain();
        core.batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    @MainThread
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed, Object data) {
        ThreadingGuard.ensureMain();
        core.batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, data);
    }

    @Override
    @MainThread
    public void flushParticleBatch() {
        ThreadingGuard.ensureMain();
        core.flushParticleBatch();
    }

    @Override
    @MainThread
    public void trail(@NotNull Location start, @NotNull Location end, @NotNull Particle particle, int perStep) {
        ThreadingGuard.ensureMain();
        core.trail(start, end, particle, perStep);
    }

    @Override
    @MainThread
    public void impact(@NotNull Location location, @NotNull Particle particle, int count, @NotNull Sound sound,
            float volume, float pitch) {
        ThreadingGuard.ensureMain();
        core.impact(location, particle, count, sound, volume, pitch);
    }

    @Override
    @MainThread
    public void impact(@NotNull Location location) {
        ThreadingGuard.ensureMain();
        core.impact(location);
    }

    @Override
    @MainThread
    public void impact(@NotNull Location location, @NotNull Particle particle, int count, double spread,
            @NotNull Sound sound, float volume, float pitch) {
        ThreadingGuard.ensureMain();
        core.impact(location, particle, count, spread, sound, volume, pitch);
    }

    @Override
    @MainThread
    public void trail(@NotNull Location location) {
        ThreadingGuard.ensureMain();
        core.trail(location);
    }

    @Override
    @MainThread
    public void followTrail(@NotNull Plugin plugin, @NotNull org.bukkit.entity.Entity entity, long periodTicks) {
        ThreadingGuard.ensureMain();
        core.followTrail(plugin, entity, periodTicks);
    }

    @Override
    @MainThread
    public void fizzle(@NotNull Location location) {
        ThreadingGuard.ensureMain();
        core.fizzle(location);
    }

    @Override
    @MainThread
    public void fizzle(@NotNull Player player) {
        ThreadingGuard.ensureMain();
        core.fizzle(player);
    }

    @Override
    @MainThread
    public void followParticles(@NotNull Plugin plugin, @NotNull org.bukkit.entity.Entity entity,
            @NotNull Particle particle, int count, double offsetX, double offsetY, double offsetZ, double speed,
            Object data, long periodTicks) {
        ThreadingGuard.ensureMain();
        core.followParticles(plugin, entity, particle, count, offsetX, offsetY, offsetZ, speed, data, periodTicks);
    }

    @Override
    @MainThread
    public void showError(@NotNull Player player, @NotNull String errorType) {
        ThreadingGuard.ensureMain();
        core.showError(player, errorType);
    }

    @Override
    @MainThread
    public void showSuccess(@NotNull Player player, @NotNull String successType) {
        ThreadingGuard.ensureMain();
        core.showSuccess(player, successType);
    }

    @Override
    @MainThread
    public void showInfo(@NotNull Player player, @NotNull String infoType) {
        ThreadingGuard.ensureMain();
        core.showInfo(player, infoType);
    }

    // Additional API-specific methods can be added here if needed beyond core

}