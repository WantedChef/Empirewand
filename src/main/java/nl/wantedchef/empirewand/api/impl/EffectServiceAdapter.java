package nl.wantedchef.empirewand.api.impl;

import nl.wantedchef.empirewand.api.service.EffectService;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EffectServiceAdapter implements EffectService {
    private final nl.wantedchef.empirewand.core.services.FxService delegate;
    
    public EffectServiceAdapter(nl.wantedchef.empirewand.core.services.FxService delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void actionBar(@NotNull Player player, @NotNull String message) {
        delegate.actionBar(player, message);
    }
    
    @Override
    public void title(@NotNull Player player, @NotNull String title, @NotNull String subtitle) {
        delegate.title(player, title, subtitle);
    }
    
    @Override
    public void spawnParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        delegate.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, extra);
    }
    
    @Override
    public void playSound(@NotNull Player player, @NotNull Sound sound, float volume, float pitch) {
        delegate.playSound(player, sound, volume, pitch);
    }
    
    @Override
    public void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch) {
        delegate.playSound(location, sound, volume, pitch);
    }
    
    @Override
    public void batchParticles(@NotNull Location location, @NotNull Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        delegate.batchParticles(location, particle, count, offsetX, offsetY, offsetZ, extra);
    }
    
    @Override
    public void flushParticleBatch() {
        delegate.flushParticleBatch();
    }
}
