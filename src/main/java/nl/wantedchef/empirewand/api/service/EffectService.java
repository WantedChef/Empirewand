package nl.wantedchef.empirewand.api.service;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface EffectService {
        void actionBar(@NotNull Player player, @NotNull String message);

        void title(@NotNull Player player, @NotNull String title, @NotNull String subtitle);

        void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
                        double offsetY, double offsetZ, double extra);

        void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch);

        void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch);

        void batchParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX,
                        double offsetY, double offsetZ, double extra);

        void flushParticleBatch();
}
