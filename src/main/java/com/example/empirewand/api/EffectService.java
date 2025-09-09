package com.example.empirewand.api;

import com.example.empirewand.api.common.MainThread;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

/**
 * Service for managing visual and audio effects.
 * Provides particle effects, sound effects, and visual effect management.
 * 
 * @since 2.0.0
 */
public interface EffectService extends EmpireWandService {

        /**
         * Sends an action bar message to a player.
         * 
         * @param player  the player to send the message to
         * @param message the component message
         */
        @MainThread
        void actionBar(@NotNull Player player, @NotNull Component message);

        /**
         * Sends a plain text action bar message to a player.
         * 
         * @param player    the player to send the message to
         * @param plainText the plain text message
         */
        @MainThread
        void actionBar(@NotNull Player player, @NotNull String plainText);

        /**
         * Sends an action bar message from a key.
         * 
         * @param player     the player to send the message to
         * @param messageKey the message key
         */
        @MainThread
        void actionBarKey(@NotNull Player player, @NotNull String messageKey);

        /**
         * Sends an action bar message from a key with placeholders.
         * 
         * @param player       the player to send the message to
         * @param messageKey   the message key
         * @param placeholders the placeholders map
         */
        @MainThread
        void actionBarKey(@NotNull Player player, @NotNull String messageKey,
                        @NotNull java.util.Map<String, String> placeholders);

        /**
         * Shows a title and subtitle to a player.
         * 
         * @param player   the player to show the title to
         * @param title    the title component
         * @param subtitle the subtitle component
         */
        @MainThread
        void title(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle);

        /**
         * Shows a title and subtitle to a player with fade times.
         * 
         * @param player   the player to show the title to
         * @param title    the title component
         * @param subtitle the subtitle component
         * @param fadeIn   fade in ticks
         * @param stay     stay ticks
         * @param fadeOut  fade out ticks
         */
        @MainThread
        void title(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle, int fadeIn, int stay,
                        int fadeOut);

        /**
         * Plays a sound to a player.
         * 
         * @param player the player to play the sound to
         * @param sound  the sound
         * @param volume the volume
         * @param pitch  the pitch
         */
        @MainThread
        void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch);

        /**
         * Plays a sound at a location.
         * 
         * @param location the location to play the sound at
         * @param sound    the sound
         * @param volume   the volume
         * @param pitch    the pitch
         */
        @MainThread
        void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch);

        /**
         * Spawns particles at a location.
         * 
         * @param location the location
         * @param particle the particle
         * @param count    the count
         * @param offsetX  x offset
         * @param offsetY  y offset
         * @param offsetZ  z offset
         * @param speed    the speed
         */
        @MainThread
        void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
                        double offsetY, double offsetZ, double speed);

        /**
         * Spawns particles at a location with data.
         * 
         * @param location the location
         * @param particle the particle
         * @param count    the count
         * @param offsetX  x offset
         * @param offsetY  y offset
         * @param offsetZ  z offset
         * @param speed    the speed
         * @param data     the data
         */
        @MainThread
        void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
                        double offsetY, double offsetZ, double speed, Object data);

        /**
         * Batches particles for performance.
         * 
         * @param location the location
         * @param particle the particle
         * @param count    the count
         * @param offsetX  x offset
         * @param offsetY  y offset
         * @param offsetZ  z offset
         * @param speed    the speed
         */
        @MainThread
        void batchParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
                        double offsetY, double offsetZ, double speed);

        /**
         * Batches particles with data for performance.
         * 
         * @param location the location
         * @param particle the particle
         * @param count    the count
         * @param offsetX  x offset
         * @param offsetY  y offset
         * @param offsetZ  z offset
         * @param speed    the speed
         * @param data     the data
         */
        @MainThread
        void batchParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
                        double offsetY, double offsetZ, double speed, Object data);

        /**
         * Flushes batched particles.
         */
        @MainThread
        void flushParticleBatch();

        /**
         * Creates a trail between two locations.
         * 
         * @param start    the start location
         * @param end      the end location
         * @param particle the particle
         * @param perStep  particles per step
         */
        @MainThread
        void trail(@NotNull Location start, @NotNull Location end, @NotNull Particle particle, int perStep);

        /**
         * Creates an impact effect.
         * 
         * @param location the location
         * @param particle the particle
         * @param count    the count
         * @param sound    the sound
         * @param volume   the volume
         * @param pitch    the pitch
         */
        @MainThread
        void impact(@NotNull Location location, @NotNull Particle particle, int count, @NotNull Sound sound,
                        float volume,
                        float pitch);

        /**
         * Creates an impact effect.
         * 
         * @param location the location
         */
        @MainThread
        void impact(@NotNull Location location);

        /**
         * Creates an impact effect.
         * 
         * @param location the location
         * @param particle the particle
         * @param count    the count
         * @param spread   the spread
         * @param sound    the sound
         * @param volume   the volume
         * @param pitch    the pitch
         */
        @MainThread
        void impact(@NotNull Location location, @NotNull Particle particle, int count, double spread,
                        @NotNull Sound sound, float volume, float pitch);

        /**
         * Creates a trail effect.
         * 
         * @param location the location
         */
        @MainThread
        void trail(@NotNull Location location);

        /**
         * Creates a fizzle effect.
         * 
         * @param location the location
         */
        @MainThread
        void fizzle(@NotNull Location location);

        /**
         * Creates a fizzle effect at a player's location.
         * 
         * @param player the player
         */
        @MainThread
        void fizzle(@NotNull Player player);

        /**
         * Follows an entity with particles.
         * 
         * @param plugin      the plugin
         * @param entity      the entity
         * @param particle    the particle
         * @param count       the count
         * @param offsetX     x offset
         * @param offsetY     y offset
         * @param offsetZ     z offset
         * @param speed       the speed
         * @param data        the data
         * @param periodTicks the period
         */
        @MainThread
        void followParticles(@NotNull Plugin plugin, @NotNull org.bukkit.entity.Entity entity,
                        @NotNull Particle particle,
                        int count, double offsetX, double offsetY, double offsetZ, double speed, Object data,
                        long periodTicks);

        /**
         * Follows an entity with a simple trail effect.
         *
         * @param plugin      the plugin instance scheduling the task
         * @param entity      the entity to follow
         * @param periodTicks the period between trail updates in ticks
         */
        @MainThread
        void followTrail(@NotNull Plugin plugin, @NotNull org.bukkit.entity.Entity entity, long periodTicks);

        /**
         * Shows an error message to a player using the configured error message system.
         *
         * @param player    the player to show the error to
         * @param errorType the error type key (e.g., "no-target")
         */
        @MainThread
        void showError(@NotNull Player player, @NotNull String errorType);

        /**
         * Shows a success message to a player using the configured success message
         * system.
         *
         * @param player      the player to show the success to
         * @param successType the success type key (e.g., "spell-cast")
         */
        @MainThread
        void showSuccess(@NotNull Player player, @NotNull String successType);

        /**
         * Shows an informational message to a player using the configured info message
         * system.
         *
         * @param player   the player to show the info to
         * @param infoType the info type key (e.g., "spell-ready")
         */
        @MainThread
        void showInfo(@NotNull Player player, @NotNull String infoType);
}