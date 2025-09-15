package nl.wantedchef.empirewand.api.impl;

import nl.wantedchef.empirewand.api.service.EffectService;
import nl.wantedchef.empirewand.api.ServiceHealth;
import nl.wantedchef.empirewand.api.Version;
import nl.wantedchef.empirewand.api.common.MainThread;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.framework.service.ThreadingGuard;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

/**
 * Optimized adapter for EffectService that wraps FxService core implementation.
 * Delegates all effect operations to core while providing API contract.
 * Features performance optimizations for high-frequency particle operations.
 *
 * Performance improvements:
 * - Lightweight thread checking for hot paths
 * - Reduced method call overhead
 * - Optimized delegation pattern
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

    // EffectService implementations - optimized with lightweight thread checking

    @Override
    @MainThread
    public void actionBar(@NotNull Player player, @NotNull Component message) {
        ThreadingGuard.ensureMainLightweight();
        core.actionBar(player, message);
    }

    @Override
    @MainThread
    public void actionBar(@NotNull Player player, @NotNull String plainText) {
        ThreadingGuard.ensureMainLightweight();
        core.actionBar(player, plainText);
    }

    @Override
    @MainThread
    public void actionBarKey(@NotNull Player player, @NotNull String messageKey) {
        ThreadingGuard.ensureMainLightweight();
        core.actionBarKey(player, messageKey);
    }

    @Override
    @MainThread
    public void actionBarKey(@NotNull Player player, @NotNull String messageKey,
            @NotNull java.util.Map<String, String> placeholders) {
        ThreadingGuard.ensureMainLightweight();
        core.actionBarKey(player, messageKey, placeholders);
    }

    @Override
    @MainThread
    public void title(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle) {
        ThreadingGuard.ensureMain(); // Title operations are less frequent, use full check
        core.title(player, title, subtitle);
    }

    @Override
    @MainThread
    public void title(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle, int fadeIn,
            int stay, int fadeOut) {
        ThreadingGuard.ensureMain(); // Title operations are less frequent, use full check
        core.title(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    @MainThread
    public void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch) {
        ThreadingGuard.ensureMainLightweight();
        core.playSound(player, sound, volume, pitch);
    }

    @Override
    @MainThread
    public void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch) {
        ThreadingGuard.ensureMainLightweight();
        core.playSound(location, sound, volume, pitch);
    }

    // Particle methods - most performance critical, use lightweight checking
    @Override
    @MainThread
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed) {
        ThreadingGuard.ensureMainLightweight();
        core.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    @MainThread
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed, Object data) {
        ThreadingGuard.ensureMainLightweight();
        core.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, data);
    }

    @Override
    @MainThread
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed) {
        ThreadingGuard.ensureMainLightweight();
        core.batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    @MainThread
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed, Object data) {
        ThreadingGuard.ensureMainLightweight();
        core.batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, data);
    }

    @Override
    @MainThread
    public void flushParticleBatch() {
        ThreadingGuard.ensureMainLightweight();
        core.flushParticleBatch();
    }

    @Override
    @MainThread
    public void trail(@NotNull Location start, @NotNull Location end, @NotNull Particle particle, int perStep) {
        ThreadingGuard.ensureMainLightweight();
        core.trail(start, end, particle, perStep);
    }

    @Override
    @MainThread
    public void impact(@NotNull Location location, @NotNull Particle particle, int count, @NotNull Sound sound,
            float volume, float pitch) {
        ThreadingGuard.ensureMainLightweight();
        core.impact(location, particle, count, sound, volume, pitch);
    }

    @Override
    @MainThread
    public void impact(@NotNull Location location) {
        ThreadingGuard.ensureMainLightweight();
        core.impact(location);
    }

    @Override
    @MainThread
    public void impact(@NotNull Location location, @NotNull Particle particle, int count, double spread,
            @NotNull Sound sound, float volume, float pitch) {
        ThreadingGuard.ensureMainLightweight();
        core.impact(location, particle, count, spread, sound, volume, pitch);
    }

    @Override
    @MainThread
    public void trail(@NotNull Location location) {
        ThreadingGuard.ensureMainLightweight();
        core.trail(location);
    }

    @Override
    @MainThread
    public void followTrail(@NotNull Plugin plugin, @NotNull org.bukkit.entity.Entity entity, long periodTicks) {
        ThreadingGuard.ensureMain(); // Follow operations are less frequent
        core.followTrail(plugin, entity, periodTicks);
    }

    @Override
    @MainThread
    public void fizzle(@NotNull Location location) {
        ThreadingGuard.ensureMainLightweight();
        core.fizzle(location);
    }

    @Override
    @MainThread
    public void fizzle(@NotNull Player player) {
        ThreadingGuard.ensureMainLightweight();
        core.fizzle(player);
    }

    @Override
    @MainThread
    public void followParticles(@NotNull Plugin plugin, @NotNull org.bukkit.entity.Entity entity,
            @NotNull Particle particle, int count, double offsetX, double offsetY, double offsetZ, double speed,
            Object data, long periodTicks) {
        ThreadingGuard.ensureMain(); // Follow operations are less frequent
        core.followParticles(plugin, entity, particle, count, offsetX, offsetY, offsetZ, speed, data, periodTicks);
    }

    @Override
    @MainThread
    public void showError(@NotNull Player player, @NotNull String errorType) {
        ThreadingGuard.ensureMainLightweight();
        core.showError(player, errorType);
    }

    @Override
    @MainThread
    public void showSuccess(@NotNull Player player, @NotNull String successType) {
        ThreadingGuard.ensureMainLightweight();
        core.showSuccess(player, successType);
    }

    @Override
    @MainThread
    public void showInfo(@NotNull Player player, @NotNull String infoType) {
        ThreadingGuard.ensureMainLightweight();
        core.showInfo(player, infoType);
    }
}