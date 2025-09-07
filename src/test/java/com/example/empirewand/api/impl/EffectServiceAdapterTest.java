package com.example.empirewand.api.impl;

import com.example.empirewand.core.services.FxService;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EffectServiceAdapter.
 * Verifies that the adapter properly wraps FxService and delegates calls.
 */
@ExtendWith(MockitoExtension.class)
class EffectServiceAdapterTest {

    @Mock
    private FxService mockFxService;

    @Mock
    private Player mockPlayer;

    @Mock
    private Location mockLocation;

    @Mock
    private Plugin mockPlugin;

    private EffectServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EffectServiceAdapter(mockFxService);
    }

    @Test
    void testGetServiceName() {
        assertEquals("EffectService", adapter.getServiceName());
    }

    @Test
    void testGetServiceVersion() {
        assertEquals(2, adapter.getServiceVersion().getMajor());
        assertEquals(0, adapter.getServiceVersion().getMinor());
        assertEquals(0, adapter.getServiceVersion().getPatch());
    }

    @Test
    void testIsEnabled() {
        assertTrue(adapter.isEnabled());
    }

    @Test
    void testGetHealth() {
        assertNotNull(adapter.getHealth());
    }

    @Test
    void testActionBarWithComponent() {
        Component message = Component.text("Test message");
        adapter.actionBar(mockPlayer, message);

        verify(mockFxService).actionBar(mockPlayer, message);
    }

    @Test
    void testActionBarWithString() {
        String message = "Test message";
        adapter.actionBar(mockPlayer, message);

        verify(mockFxService).actionBar(mockPlayer, message);
    }

    @Test
    void testActionBarKey() {
        String messageKey = "test.key";
        adapter.actionBarKey(mockPlayer, messageKey);

        verify(mockFxService).actionBarKey(mockPlayer, messageKey);
    }

    @Test
    void testActionBarKeyWithPlaceholders() {
        String messageKey = "test.key";
        var placeholders = java.util.Map.of("name", "value");
        adapter.actionBarKey(mockPlayer, messageKey, placeholders);

        verify(mockFxService).actionBarKey(mockPlayer, messageKey, placeholders);
    }

    @Test
    void testTitle() {
        Component title = Component.text("Title");
        Component subtitle = Component.text("Subtitle");
        adapter.title(mockPlayer, title, subtitle);

        verify(mockFxService).title(mockPlayer, title, subtitle);
    }

    @Test
    void testTitleWithFadeTimes() {
        Component title = Component.text("Title");
        Component subtitle = Component.text("Subtitle");
        int fadeIn = 10, stay = 20, fadeOut = 10;
        adapter.title(mockPlayer, title, subtitle, fadeIn, stay, fadeOut);

        verify(mockFxService).title(mockPlayer, title, subtitle, fadeIn, stay, fadeOut);
    }

    @Test
    void testPlaySoundWithPlayer() {
        Sound sound = Sound.UI_BUTTON_CLICK;
        float volume = 1.0f, pitch = 1.0f;
        adapter.playSound(mockPlayer, sound, volume, pitch);

        verify(mockFxService).playSound(mockPlayer, sound, volume, pitch);
    }

    @Test
    void testPlaySoundWithLocation() {
        Sound sound = Sound.UI_BUTTON_CLICK;
        float volume = 1.0f, pitch = 1.0f;
        adapter.playSound(mockLocation, sound, volume, pitch);

        verify(mockFxService).playSound(mockLocation, sound, volume, pitch);
    }

    @Test
    void testSpawnParticles() {
        Particle particle = Particle.FLAME;
        int count = 10;
        double offsetX = 0.1, offsetY = 0.1, offsetZ = 0.1, speed = 0.05;
        adapter.spawnParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed);

        verify(mockFxService).spawnParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed);
    }

    @Test
    void testSpawnParticlesWithData() {
        Particle particle = Particle.FLAME;
        int count = 10;
        double offsetX = 0.1, offsetY = 0.1, offsetZ = 0.1, speed = 0.05;
        Object data = "test";
        adapter.spawnParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed, data);

        verify(mockFxService).spawnParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed, data);
    }

    @Test
    void testBatchParticles() {
        Particle particle = Particle.FLAME;
        int count = 10;
        double offsetX = 0.1, offsetY = 0.1, offsetZ = 0.1, speed = 0.05;
        adapter.batchParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed);

        verify(mockFxService).batchParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed);
    }

    @Test
    void testBatchParticlesWithData() {
        Particle particle = Particle.FLAME;
        int count = 10;
        double offsetX = 0.1, offsetY = 0.1, offsetZ = 0.1, speed = 0.05;
        Object data = "test";
        adapter.batchParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed, data);

        verify(mockFxService).batchParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed, data);
    }

    @Test
    void testFlushParticleBatch() {
        adapter.flushParticleBatch();

        verify(mockFxService).flushParticleBatch();
    }

    @Test
    void testTrail() {
        Location start = mockLocation;
        Location end = mock(Location.class);
        Particle particle = Particle.FLAME;
        int perStep = 5;
        adapter.trail(start, end, particle, perStep);

        verify(mockFxService).trail(start, end, particle, perStep);
    }

    @Test
    void testImpact() {
        Particle particle = Particle.EXPLOSION;
        int count = 20;
        Sound sound = Sound.ENTITY_GENERIC_EXPLODE;
        float volume = 1.0f, pitch = 1.0f;
        adapter.impact(mockLocation, particle, count, sound, volume, pitch);

        verify(mockFxService).impact(mockLocation, particle, count, sound, volume, pitch);
    }

    @Test
    void testFizzle() {
        adapter.fizzle(mockLocation);

        verify(mockFxService).fizzle(mockLocation);
    }

    @Test
    void testFollowParticles() {
        var entity = mock(org.bukkit.entity.Entity.class);
        Particle particle = Particle.FLAME;
        int count = 10;
        double offsetX = 0.1, offsetY = 0.1, offsetZ = 0.1, speed = 0.05;
        Object data = null;
        long periodTicks = 20L;
        adapter.followParticles(mockPlugin, entity, particle, count, offsetX, offsetY, offsetZ, speed, data,
                periodTicks);

        verify(mockFxService).followParticles(mockPlugin, entity, particle, count, offsetX, offsetY, offsetZ, speed,
                data, periodTicks);
    }
}