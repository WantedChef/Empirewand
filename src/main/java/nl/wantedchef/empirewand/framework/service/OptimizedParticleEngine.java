package nl.wantedchef.empirewand.framework.service;

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
 * An enhanced particle engine featuring object pooling and optimized batching for high-performance
 * particle rendering. This engine is designed to handle a large volume of particle effects with
 * minimal impact on server performance.
 */
public class OptimizedParticleEngine {
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_POOL_SIZE = 1000;
    private static final long CLEANUP_INTERVAL_TICKS = 1200L; // 60 seconds

    @SuppressFBWarnings(value = "UUF_UNUSED_FIELD",
            justification = "Plugin is used for scheduling cleanup tasks")
    private final Plugin plugin;
    private static final Logger logger = Logger.getLogger(OptimizedParticleEngine.class.getName());
    private final ParticleBatchPool batchPool;
    private final ConcurrentLinkedQueue<ParticleBatch> activeBatches;
    private final AtomicInteger totalParticlesSpawned = new AtomicInteger(0);
    private BukkitRunnable cleanupTask;

    /**
     * A pooled object representing a batch of particles to be spawned. Using a pool of these
     * objects reduces garbage collection pressure.
     */
    private static class ParticleBatch {
        Location location;
        Particle particle;
        int count;
        double offsetX, offsetY, offsetZ, speed;
        Object data;
        boolean inUse;

        /**
         * Resets the batch to its default state, making it available for reuse.
         */
        void reset() {
            location = null;
            particle = null;
            count = 0;
            offsetX = offsetY = offsetZ = speed = 0.0;
            data = null;
            inUse = false;
        }

        /**
         * Sets the properties of the particle batch.
         *
         * @param location The location to spawn particles.
         * @param particle The type of particle.
         * @param count The number of particles.
         * @param offsetX The offset on the X axis.
         * @param offsetY The offset on the Y axis.
         * @param offsetZ The offset on the Z axis.
         * @param speed The speed of the particles.
         * @param data The data for the particle (e.g., DustOptions).
         */
        void set(Location location, Particle particle, int count, double offsetX, double offsetY,
                double offsetZ, double speed, Object data) {
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

        /**
         * Executes the particle spawning operation.
         */
        void execute() {
            if (!inUse || location == null || particle == null || count <= 0)
                return;

            World world = location.getWorld();
            if (world != null) {
                try {
                    if (data != null) {
                        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ,
                                speed, data);
                    } else {
                        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ,
                                speed);
                    }
                } catch (Exception e) {
                    // Silently handle particle spawn failures to avoid spam
                }
            }
        }
    }

    /**
     * An object pool for managing ParticleBatch instances.
     */
    private static class ParticleBatchPool {
        private final List<ParticleBatch> pool = new ArrayList<>();
        private final int maxSize;

        /**
         * Constructs a new ParticleBatchPool.
         *
         * @param maxSize The maximum number of objects to store in the pool.
         */
        ParticleBatchPool(int maxSize) {
            this.maxSize = maxSize;
        }

        /**
         * Borrows a ParticleBatch from the pool or creates a new one if the pool is empty.
         *
         * @return A ParticleBatch instance.
         */
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

        /**
         * Returns a ParticleBatch to the pool for reuse.
         *
         * @param batch The ParticleBatch to return.
         */
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

        /**
         * Gets the current size of the pool.
         *
         * @return The number of available objects in the pool.
         */
        int size() {
            synchronized (pool) {
                return pool.size();
            }
        }
    }

    /**
     * Constructs a new OptimizedParticleEngine.
     *
     * @param plugin The plugin instance.
     */
    public OptimizedParticleEngine(Plugin plugin) {
        this.plugin = plugin;
        this.batchPool = new ParticleBatchPool(MAX_POOL_SIZE);
        this.activeBatches = new ConcurrentLinkedQueue<>();
    }

    /**
     * Initializes the particle engine by starting its cleanup and flushing tasks. This should be
     * called when the plugin is enabled.
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

        logger.info(String.format("OptimizedParticleEngine initialized with batch size: %d",
                DEFAULT_BATCH_SIZE));
    }

    /**
     * Spawns particles using the optimized batching system.
     *
     * @param location The location to spawn particles.
     * @param particle The type of particle.
     * @param count The number of particles.
     * @param offsetX The offset on the X axis.
     * @param offsetY The offset on the Y axis.
     * @param offsetZ The offset on the Z axis.
     * @param speed The speed of the particles.
     */
    public void spawnParticle(@NotNull Location location, @NotNull Particle particle, int count,
            double offsetX, double offsetY, double offsetZ, double speed) {
        spawnParticle(location, particle, count, offsetX, offsetY, offsetZ, speed, null);
    }

    /**
     * Spawns particles with data using the optimized batching system.
     *
     * @param location The location to spawn particles.
     * @param particle The type of particle.
     * @param count The number of particles.
     * @param offsetX The offset on the X axis.
     * @param offsetY The offset on the Y axis.
     * @param offsetZ The offset on the Z axis.
     * @param speed The speed of the particles.
     * @param data The data for the particle (e.g., DustOptions).
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
     * Spawns a burst of particles immediately, bypassing the batching system.
     *
     * @param location The location to spawn the burst.
     * @param particle The type of particle.
     * @param count The number of particles.
     * @param spread The spread of the particles.
     */
    public void spawnParticleBurst(@NotNull Location location, @NotNull Particle particle,
            int count, double spread) {
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
     *
     * @param start The start location of the trail.
     * @param end The end location of the trail.
     * @param particle The particle to use for the trail.
     * @param particlesPerBlock The number of particles to spawn per block of distance.
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
     * Flushes all pending particle batches, spawning the particles in the world.
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
     * Gets performance metrics for monitoring the state of the particle engine.
     *
     * @return A snapshot of the current performance metrics.
     */
    public ParticleMetrics getMetrics() {
        return new ParticleMetrics(totalParticlesSpawned.get(), activeBatches.size(),
                batchPool.size());
    }

    /**
     * Periodically cleans up the engine by flushing any remaining batches.
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
     * Shuts down the particle engine, stopping the cleanup task and flushing any remaining
     * particles.
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        // Final flush
        flush();

        logger.info(String.format("OptimizedParticleEngine shut down. Final metrics: %s",
                getMetrics()));
    }

    /**
     * A data class holding a snapshot of particle engine metrics.
     *
     * @param totalParticlesSpawned The total number of particles spawned since startup.
     * @param activeBatches The number of batches currently waiting to be flushed.
     * @param poolSize The number of available objects in the batch pool.
     */
    public record ParticleMetrics(long totalParticlesSpawned, int activeBatches, int poolSize) {
    }
}


