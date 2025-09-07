package com.example.empirewand.api.impl;

import com.example.empirewand.api.EffectService;
import com.example.empirewand.api.EmpireWandService;
import com.example.empirewand.api.ServiceHealth;
import com.example.empirewand.api.Version;
import com.example.empirewand.core.services.FxService;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

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
        return ServiceHealth.HEALTHY; // Default healthy; can add core health check if implemented
    }

    @Override
    public void reload() {
        // No-op for effects; core FxService doesn't have reload, but can clear batches
        // if needed
        // core.flushParticleBatch(); // Optional
    }

    // EffectService implementations

    @Override
    public void actionBar(@NotNull Player player, @NotNull Component message) {
        core.actionBar(player, message);
    }

    @Override
    public void actionBar(@NotNull Player player, @NotNull String plainText) {
        core.actionBar(player, plainText);
    }

    @Override
    public void actionBarKey(@NotNull Player player, @NotNull String messageKey) {
        core.actionBarKey(player, messageKey);
    }

    @Override
    public void actionBarKey(@NotNull Player player, @NotNull String messageKey,
            @NotNull java.util.Map<String, String> placeholders) {
        core.actionBarKey(player, messageKey, placeholders);
    }

    @Override
    public void title(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle) {
        core.title(player, title, subtitle);
    }

    @Override
    public void title(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle, int fadeIn,
            int stay, int fadeOut) {
        core.title(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch) {
        core.playSound(player, sound, volume, pitch);
    }

    @Override
    public void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch) {
        core.playSound(location, sound, volume, pitch);
    }

    @Override
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed) {
        core.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed, Object data) {
        core.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, data);
    }

    @Override
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed) {
        core.batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
            double offsetY, double offsetZ, double speed, Object data) {
        core.batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, data);
    }

    @Override
    public void flushParticleBatch() {
        core.flushParticleBatch();
    }

    @Override
    public void trail(@NotNull Location start, @NotNull Location end, @NotNull Particle particle, int perStep) {
        core.trail(start, end, particle, perStep);
    }

    @Override
    public void impact(@NotNull Location location, @NotNull Particle particle, int count, @NotNull Sound sound,
            float volume, float pitch) {
        core.impact(location, particle, count, sound, volume, pitch);
    }

    @Override
    public void fizzle(@NotNull Location location) {
        core.fizzle(location);
    }

    @Override
    public void followParticles(@NotNull Plugin plugin, @NotNull org.bukkit.entity.Entity entity,
            @NotNull Particle particle, int count, double offsetX, double offsetY, double offsetZ, double speed,
            Object data, long periodTicks) {
        core.followParticles(plugin, entity, particle, count, offsetX, offsetY, offsetZ, speed, data, periodTicks);
    }

    // Additional API-specific methods can be added here if needed beyond core

}