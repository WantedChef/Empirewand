package com.example.empirewand.core.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptimizedParticleEngineTest {

    @Mock
    private Plugin plugin;

    @Mock
    private World world;

    @Mock
    private Location location;

    private OptimizedParticleEngine engine;

    @BeforeEach
    void setUp() {
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("TestLogger"));
        when(location.getWorld()).thenReturn(world);
        engine = new OptimizedParticleEngine(plugin);
        engine.initialize();
    }

    @Test
    void testSpawnParticle() {
        // Act
        engine.spawnParticle(location, Particle.FLAME, 10, 0.1, 0.1, 0.1, 0.05);

        // Assert - Should not throw any exceptions
        assertDoesNotThrow(() -> engine.flush());
    }

    @Test
    void testSpawnParticleWithData() {
        // Act
        engine.spawnParticle(location, Particle.DUST, 5, 0.0, 0.0, 0.0, 0.0,
                new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));

        // Assert - Should not throw any exceptions
        assertDoesNotThrow(() -> engine.flush());
    }

    @Test
    void testSpawnParticleBurst() {
        // Act
        engine.spawnParticleBurst(location, Particle.EXPLOSION, 20, 0.5);

        // Assert - Should not throw any exceptions
        assertDoesNotThrow(() -> engine.flush());
    }

    @Test
    void testCreateTrail() {
        // Arrange
        Location endLocation = mock(Location.class);
        when(endLocation.getWorld()).thenReturn(world);
        when(location.distance(endLocation)).thenReturn(5.0);

        // Act
        engine.createTrail(location, endLocation, Particle.SMOKE, 2);

        // Assert - Should not throw any exceptions
        assertDoesNotThrow(() -> engine.flush());
    }

    @Test
    void testCreateTrailSameWorld() {
        // Arrange
        Location endLocation = mock(Location.class);
        when(endLocation.getWorld()).thenReturn(world);
        when(location.distance(endLocation)).thenReturn(0.0); // Same location

        // Act
        engine.createTrail(location, endLocation, Particle.SMOKE, 2);

        // Assert - Should not throw any exceptions
        assertDoesNotThrow(() -> engine.flush());
    }

    @Test
    void testCreateTrailDifferentWorld() {
        // Arrange
        World otherWorld = mock(World.class);
        Location endLocation = mock(Location.class);
        when(endLocation.getWorld()).thenReturn(otherWorld); // Different world

        // Act
        engine.createTrail(location, endLocation, Particle.SMOKE, 2);

        // Assert - Should not throw any exceptions (should return early)
        assertDoesNotThrow(() -> engine.flush());
    }

    @Test
    void testFlush() {
        // Arrange
        engine.spawnParticle(location, Particle.FLAME, 10, 0.1, 0.1, 0.1, 0.05);

        // Act
        engine.flush();

        // Assert - Should not throw any exceptions
        assertDoesNotThrow(() -> engine.getMetrics());
    }

    @Test
    void testGetMetrics() {
        // Act
        OptimizedParticleEngine.ParticleMetrics metrics = engine.getMetrics();

        // Assert
        assertNotNull(metrics);
        assertTrue(metrics.totalParticlesSpawned() >= 0);
        assertTrue(metrics.activeBatches() >= 0);
        assertTrue(metrics.poolSize() >= 0);
    }

    @Test
    void testShutdown() {
        // Arrange
        engine.spawnParticle(location, Particle.FLAME, 5, 0.0, 0.0, 0.0, 0.0);

        // Act
        engine.shutdown();

        // Assert - Should not throw any exceptions
        assertDoesNotThrow(() -> engine.getMetrics());
    }

    @Test
    void testNullLocationHandling() {
        // Act & Assert
        assertDoesNotThrow(() -> engine.spawnParticle(null, Particle.FLAME, 1, 0, 0, 0, 0));
    }

    @Test
    void testNullParticleHandling() {
        // Act & Assert
        assertDoesNotThrow(() -> engine.spawnParticle(location, null, 1, 0, 0, 0, 0));
    }

    @Test
    void testNegativeCountHandling() {
        // Act & Assert
        assertDoesNotThrow(() -> engine.spawnParticle(location, Particle.FLAME, -1, 0, 0, 0, 0));
    }

    @Test
    void testZeroCountHandling() {
        // Act & Assert
        assertDoesNotThrow(() -> engine.spawnParticle(location, Particle.FLAME, 0, 0, 0, 0, 0));
    }
}