package nl.wantedchef.empirewand.common.visual;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A generic, configurable projectile trail system that creates visual block and particle trails
 * behind moving projectiles with automatic cleanup.
 * 
 * <p>This class centralizes the common logic used by various fire spells for creating
 * trailing effects behind projectiles like fireballs and comets.
 * 
 * <p>Features:
 * <ul>
 *   <li>Configurable trail blocks, particles, and lifetime</li>
 *   <li>Automatic block placement and cleanup with queues</li>
 *   <li>Flexible block replacement conditions</li>
 *   <li>Built-in safety limits and resource management</li>
 * </ul>
 */
public class ProjectileTrail extends BukkitRunnable {
    
    private static final double DEFAULT_Y_OFFSET = -0.25;
    private static final double DEFAULT_PARTICLE_OFFSET = 0.1;
    private static final double DEFAULT_PARTICLE_SPEED = 0.01;
    private static final int DEFAULT_PARTICLE_MULTIPLIER = 1;
    private static final int DEFAULT_MAX_LIFETIME_TICKS = 300; // 15 seconds safety limit
    
    private final Projectile projectile;
    private final World world;
    private final TrailConfig config;
    private int tick = 0;
    private final Deque<TempBlock> queue = new ArrayDeque<>();
    private final Set<Block> ours = new HashSet<>();
    
    /**
     * Creates a new projectile trail with the specified configuration.
     *
     * @param projectile The projectile to follow
     * @param config The trail configuration
     */
    public ProjectileTrail(@NotNull Projectile projectile, @NotNull TrailConfig config) {
        this.projectile = projectile;
        this.world = projectile.getWorld();
        this.config = config;
    }
    
    @Override
    public void run() {
        if (!projectile.isValid() || projectile.isDead()) {
            cleanup();
            cancel();
            return;
        }
        
        createTrailSegment();
        cleanupExpiredBlocks();
        
        tick++;
        if (tick > config.maxLifetimeTicks) {
            cleanup();
            cancel();
        }
    }
    
    /**
     * Creates a new trail segment behind the projectile.
     */
    private void createTrailSegment() {
        Vector dir = projectile.getVelocity().clone().normalize();
        Location base = projectile.getLocation().clone().add(0, config.yOffset, 0);
        
        for (int i = 0; i < config.trailLength; i++) {
            Location trailLocation = base.clone().add(dir.clone().multiply(-i));
            Block block = trailLocation.getBlock();
            
            if (!ours.contains(block) && config.blockReplacementCondition.test(block)) {
                // Create temporary block
                queue.addLast(new TempBlock(block, block.getBlockData(), tick + config.blockLifetimeTicks));
                block.setType(config.trailMaterial, false);
                ours.add(block);
                
                // Spawn particles
                int particleCount = config.particleCount * config.particleMultiplier;
                world.spawnParticle(config.particle, trailLocation, particleCount, 
                        config.particleOffset, config.particleOffset, config.particleOffset, 
                        config.particleSpeed);
            }
        }
    }
    
    /**
     * Cleans up blocks that have expired.
     */
    private void cleanupExpiredBlocks() {
        while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
            TempBlock tempBlock = queue.pollFirst();
            tempBlock.revert(config.trailMaterial);
            ours.remove(tempBlock.block);
        }
    }
    
    /**
     * Cleans up all remaining temporary blocks.
     */
    private void cleanup() {
        while (!queue.isEmpty()) {
            TempBlock tempBlock = queue.pollFirst();
            tempBlock.revert(config.trailMaterial);
            ours.remove(tempBlock.block);
        }
    }
    
    /**
     * Configuration class for projectile trails using the Builder pattern.
     */
    public static class TrailConfig {
        private final int trailLength;
        private final int particleCount;
        private final int blockLifetimeTicks;
        private final Material trailMaterial;
        private final Particle particle;
        private final double yOffset;
        private final double particleOffset;
        private final double particleSpeed;
        private final int particleMultiplier;
        private final int maxLifetimeTicks;
        private final Predicate<Block> blockReplacementCondition;
        
        private TrailConfig(Builder builder) {
            this.trailLength = builder.trailLength;
            this.particleCount = builder.particleCount;
            this.blockLifetimeTicks = builder.blockLifetimeTicks;
            this.trailMaterial = builder.trailMaterial;
            this.particle = builder.particle;
            this.yOffset = builder.yOffset;
            this.particleOffset = builder.particleOffset;
            this.particleSpeed = builder.particleSpeed;
            this.particleMultiplier = builder.particleMultiplier;
            this.maxLifetimeTicks = builder.maxLifetimeTicks;
            this.blockReplacementCondition = builder.blockReplacementCondition;
        }
        
        /**
         * Creates a new builder for trail configuration.
         *
         * @return A new TrailConfig builder
         */
        public static Builder builder() {
            return new Builder();
        }
        
        /**
         * Builder for TrailConfig following the project's Builder pattern guidelines.
         */
        public static class Builder {
            private int trailLength = 4;
            private int particleCount = 2;
            private int blockLifetimeTicks = 40;
            private Material trailMaterial = Material.MAGMA_BLOCK;
            private Particle particle = Particle.FLAME;
            private double yOffset = DEFAULT_Y_OFFSET;
            private double particleOffset = DEFAULT_PARTICLE_OFFSET;
            private double particleSpeed = DEFAULT_PARTICLE_SPEED;
            private int particleMultiplier = DEFAULT_PARTICLE_MULTIPLIER;
            private int maxLifetimeTicks = DEFAULT_MAX_LIFETIME_TICKS;
            private Predicate<Block> blockReplacementCondition = Block::isEmpty;
            
            public Builder trailLength(int trailLength) {
                this.trailLength = trailLength;
                return this;
            }
            
            public Builder particleCount(int particleCount) {
                this.particleCount = particleCount;
                return this;
            }
            
            public Builder blockLifetimeTicks(int blockLifetimeTicks) {
                this.blockLifetimeTicks = blockLifetimeTicks;
                return this;
            }
            
            public Builder trailMaterial(Material trailMaterial) {
                this.trailMaterial = trailMaterial;
                return this;
            }
            
            public Builder particle(Particle particle) {
                this.particle = particle;
                return this;
            }
            
            public Builder yOffset(double yOffset) {
                this.yOffset = yOffset;
                return this;
            }
            
            public Builder particleOffset(double particleOffset) {
                this.particleOffset = particleOffset;
                return this;
            }
            
            public Builder particleSpeed(double particleSpeed) {
                this.particleSpeed = particleSpeed;
                return this;
            }
            
            public Builder particleMultiplier(int particleMultiplier) {
                this.particleMultiplier = particleMultiplier;
                return this;
            }
            
            public Builder maxLifetimeTicks(int maxLifetimeTicks) {
                this.maxLifetimeTicks = maxLifetimeTicks;
                return this;
            }
            
            public Builder blockReplacementCondition(Predicate<Block> condition) {
                this.blockReplacementCondition = condition;
                return this;
            }
            
            public TrailConfig build() {
                return new TrailConfig(this);
            }
        }
    }
    
    /**
     * A record representing a temporary block that will be reverted after a certain time.
     *
     * @param block      The block that was changed
     * @param previous   The previous block data
     * @param expireTick The tick at which the block should revert
     */
    private record TempBlock(Block block, BlockData previous, int expireTick) {
        void revert(Material expectedMaterial) {
            if (block.getType() == expectedMaterial) {
                block.setBlockData(previous, false);
            }
        }
    }
}