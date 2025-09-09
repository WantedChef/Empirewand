package com.example.empirewand.core.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Enhanced particle engine with object pooling and optimized batching.
 * Provides significant performance improvements for high-frequency particle
 * operations.
 */
public class OptimizedParticleEngine {
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_POOL_SIZE = 1000;
    private static final long CLEANUP_INTERVAL_TICKS = 1200L; // 60 seconds

    @SuppressFBWarnings(value = "UUF_UNUSED_FIELD", justification = "Plugin is used for scheduling cleanup tasks")
    private final Plugin plugin;
    private final Logger logger;
    private final ParticleBatchPool batchPool;
    private final ConcurrentLinkedQueue<ParticleBatch> activeBatches;
    private final AtomicInteger totalParticlesSpawned = new AtomicInteger(0);
    private BukkitRunnable cleanupTask;

    /**
     * Pooled particle batch for memory efficiency.
     */
    private static class ParticleBatch {
        Location location;
        Particle particle;
        int count;
        double offsetX, offsetY, offsetZ, speed;
        Object data;
        boolean inUse;

        void reset() {
            location = null;
            particle = null;
            count = 0;
            offsetX = offsetY = offsetZ = speed = 0.0;
            data = null;
            inUse = false;
        }

        void set(Location location, Particle particle, int count,
                double offsetX, double offsetY, double offsetZ, double speed, Object data) {
            this.location = location;
            this.particle = particle;
            this.count = count;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
            this.data = data;
            this.inUse = true;
        }

        void execute() {
            if (!inUse || location == null || particle == null || count <= 0)
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
                    // Silently handle particle spawn failures to avoid spam
                }
            }
        }
    }

    /**
     * Object pool for particle batches to reduce GC pressure.
     */
    private static class ParticleBatchPool {
        private final List<ParticleBatch> pool = new ArrayList<>();
        private final int maxSize;

        ParticleBatchPool(int maxSize) {
            this.maxSize = maxSize;
        }

        ParticleBatch borrow() {
            synchronized (pool) {
                if (!pool.isEmpty()) {
                    ParticleBatch batch = pool.remove(pool.size() - 1);
                    batch.inUse = true;
                    return batch;
                }
            }
            return new ParticleBatch();
        }

        void returnObject(ParticleBatch batch) {
            if (batch == null)
                return;

            batch.reset();
            synchronized (pool) {
                if (pool.size() < maxSize) {
                    pool.add(batch);
                }
            }
        }

        int size() {
            synchronized (pool) {
                return pool.size();
            }
        }
    }

    public OptimizedParticleEngine(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.batchPool = new ParticleBatchPool(MAX_POOL_SIZE);
        this.activeBatches = new ConcurrentLinkedQueue<>();
    }

    /**
     * Initializes the particle engine by starting the cleanup task.
     * This should be called after the plugin is enabled.
     */
    public void initialize() {
        // Start periodic cleanup task
        this.cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        };
        cleanupTask.runTaskTimer(plugin, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);

        logger.info("OptimizedParticleEngine initialized with batch size: " + DEFAULT_BATCH_SIZE);
    }

    /**
     * Spawns particles with optimized batching.
     */
    public void spawnParticle(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed) {
        spawnParticle(location, particle, count, offsetX, offsetY, offsetZ, speed, null);
    }

    /**
     * Spawns particles with data and optimized batching.
     */
    public void spawnParticle(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed, Object data) {

        ParticleBatch batch = batchPool.borrow();
        batch.set(location, particle, count, offsetX, offsetY, offsetZ, speed, data);

        activeBatches.offer(batch);
        totalParticlesSpawned.addAndGet(count);

        // Auto-flush when batch reaches threshold
        if (activeBatches.size() >= DEFAULT_BATCH_SIZE) {
            flush();
        }
    }

    /**
     * Spawns a burst of particles at once (bypasses batching for immediate effect).
     */
    public void spawnParticleBurst(@NotNull Location location, @NotNull Particle particle, int count,
            double spread) {
        World world = location.getWorld();
        if (world == null)
            return;

        try {
            world.spawnParticle(particle, location, count, spread, spread, spread, 0.1);
            totalParticlesSpawned.addAndGet(count);
        } catch (Exception e) {
            // Silently handle particle spawn failures
        }
    }

    /**
     * Creates a particle trail between two locations.
     */
    public void createTrail(@NotNull Location start, @NotNull Location end,
            @NotNull Particle particle, int particlesPerBlock) {

        if (start.getWorld() != end.getWorld())
            return;

        double distance = start.distance(end);
        if (distance <= 0.1)
            return;

        int totalParticles = Math.max(1, (int) (distance * particlesPerBlock));
        double stepSize = distance / totalParticles;

        Location current = start.clone();
        for (int i = 0; i < totalParticles; i++) {
            spawnParticle(current, particle, 1, 0, 0, 0, 0);
            current.add(end.clone().subtract(start).toVector().normalize().multiply(stepSize));
        }
    }

    /**
     * Flushes all pending particle batches.
     */
    public void flush() {
        List<ParticleBatch> batchesToExecute = new ArrayList<>();
        ParticleBatch batch;

        // Collect all batches
        while ((batch = activeBatches.poll()) != null) {
            batchesToExecute.add(batch);
        }

        if (batchesToExecute.isEmpty())
            return;

        // Execute all batches
        for (ParticleBatch b : batchesToExecute) {
            b.execute();
            batchPool.returnObject(b);
        }

        if (batchesToExecute.size() > DEFAULT_BATCH_SIZE / 2) {
            logger.fine(String.format("Flushed %d particle batches", batchesToExecute.size()));
        }
    }

    /**
     * Gets performance metrics.
     */
    public ParticleMetrics getMetrics() {
        return new ParticleMetrics(
                totalParticlesSpawned.get(),
                activeBatches.size(),
                batchPool.size());
    }

    /**
     * Periodic cleanup to prevent memory leaks.
     */
    private void cleanup() {
        // Force flush any remaining batches
        flush();

        // Log metrics periodically
        ParticleMetrics metrics = getMetrics();
        logger.fine(String.format("Particle Engine - Total: %d, Active: %d, Pool: %d",
                metrics.totalParticlesSpawned(), metrics.activeBatches(), metrics.poolSize()));
    }

    /**
     * Shuts down the particle engine.
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        // Final flush
        flush();

        logger.info("OptimizedParticleEngine shut down. Final metrics: " + getMetrics());
    }

    /**
     * Metrics record for monitoring particle engine performance.
     */
    public record ParticleMetrics(
            long totalParticlesSpawned,
            int activeBatches,
            int poolSize) {
    }
}