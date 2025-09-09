package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A spell that turns the caster into a walking explosion, damaging nearby entities.
 */
public class ExplosionTrail extends Spell<Void> {

    /**
     * The builder for the ExplosionTrail spell.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new builder for the ExplosionTrail spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Explosion Trail";
            this.description = "You become a walking explosion, damaging nearby entities.";
            this.cooldown = java.time.Duration.ofSeconds(25);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ExplosionTrail(this);
        }
    }

    private static final float LAUNCH_SOUND_VOLUME = 1.0f;
    private static final float LAUNCH_SOUND_PITCH = 0.5f;

    private Config config;

    private ExplosionTrail(Builder builder) {
        super(builder);
        // Config will be initialized lazily when first accessed
    }

    private Config getConfig() {
        if (config == null) {
            // This will be called after loadConfig has been called
            config = new Config(spellConfig);
        }
        return config;
    }

    @Override
    public String key() {
        return "explosion-trail";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        startTrailScheduler(player, context);
        context.fx().playSound(player, Sound.ENTITY_GENERIC_EXPLODE, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    /**
     * Starts the trail scheduler.
     *
     * @param caster  The player who cast the spell.
     * @param context The spell context.
     */
    private void startTrailScheduler(Player caster, SpellContext context) {
        new TrailScheduler(caster, context, getConfig()).runTaskTimer(context.plugin(), 0L, getConfig().tickInterval());
    }

    /**
     * A runnable that creates the explosion trail effect.
     */
    private static class TrailScheduler extends BukkitRunnable {

        private static final double DAMAGE_RADIUS = 3.0;
        private static final double PARTICLE_OFFSET = 0.2;
        private static final double PARTICLE_SPEED = 0.05;
        private static final float TRAIL_SOUND_VOLUME = 0.6f;
        private static final float TRAIL_SOUND_PITCH = 1.1f;
        private static final Material TRAIL_BLOCK_MATERIAL = Material.NETHERRACK;

        private final Player caster;
        private final SpellContext context;
        private final Config config;
        private final Deque<TempBlock> tempBlocks;
        private int ticks;

        public TrailScheduler(Player caster, SpellContext context, Config config) {
            this.caster = caster;
            this.context = context;
            this.config = config;
            this.tempBlocks = new ConcurrentLinkedDeque<>();
        }

        @Override
        public void run() {
            if (ticks >= config.duration() || !caster.isValid() || caster.isDead()) {
                cleanup();
                this.cancel();
                return;
            }

            Location playerLoc = caster.getLocation();
            var world = playerLoc.getWorld();
            if (world == null) {
                cleanup();
                this.cancel();
                return;
            }

            damageNearbyEntities(playerLoc, world);
            spawnParticles(playerLoc);
            playSound(playerLoc);

            if (config.placeBlocks()) {
                placeBlock(playerLoc, world);
            }

            removeExpiredBlocks(world);
            ticks += config.tickInterval();
        }

        private void damageNearbyEntities(Location playerLoc, org.bukkit.World world) {
            for (var entity : world.getNearbyEntities(playerLoc, DAMAGE_RADIUS, DAMAGE_RADIUS, DAMAGE_RADIUS)) {
                if (entity instanceof LivingEntity living && !living.equals(caster) && !living.isDead()
                        && living.isValid()) {
                    living.damage(config.damage(), caster);
                }
            }
        }

        private void spawnParticles(Location playerLoc) {
            context.fx().spawnParticles(playerLoc, Particle.EXPLOSION, config.particleCount(), PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_SPEED);
        }

        private void playSound(Location playerLoc) {
            context.fx().playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, TRAIL_SOUND_VOLUME, TRAIL_SOUND_PITCH);
        }

        private void placeBlock(Location playerLoc, org.bukkit.World world) {
            Block below = playerLoc.clone().subtract(0, 1, 0).getBlock();
            if (below.getType().isAir()) {
                tempBlocks.addLast(new TempBlock(below, below.getType(), world.getFullTime() + config.blockLifetime()));
                below.setType(TRAIL_BLOCK_MATERIAL, false);
                while (tempBlocks.size() > config.trailLength()) {
                    tempBlocks.removeFirst().revert();
                }
            }
        }

        private void removeExpiredBlocks(org.bukkit.World world) {
            long now = world.getFullTime();
            tempBlocks.removeIf(tb -> {
                if (tb.revertTick <= now) {
                    tb.revert();
                    return true;
                }
                return false;
            });
        }

        private void cleanup() {
            while (!tempBlocks.isEmpty()) {
                tempBlocks.removeFirst().revert();
            }
        }
    }

    /**
     * The configuration for the ExplosionTrail spell.
     */
    private static class Config {

        private static final int DEFAULT_DURATION_TICKS = 100;
        private static final double DEFAULT_DAMAGE = 8.0;
        private static final int DEFAULT_TICK_INTERVAL = 10;
        private static final int DEFAULT_TRAIL_LENGTH = 12;
        private static final int DEFAULT_PARTICLE_COUNT = 10;
        private static final int DEFAULT_BLOCK_LIFETIME_TICKS = 40;
        private static final boolean DEFAULT_PLACE_BLOCKS = true;

        private final int duration;
        private final double damage;
        private final int tickInterval;
        private final int trailLength;
        private final int particleCount;
        private final int blockLifetime;
        private final boolean placeBlocks;

        public Config(ReadableConfig config) {
            this.duration = config.getInt("values.duration-ticks", DEFAULT_DURATION_TICKS);
            this.damage = config.getDouble("values.damage", DEFAULT_DAMAGE);
            this.tickInterval = config.getInt("values.tick-interval", DEFAULT_TICK_INTERVAL);
            this.trailLength = config.getInt("values.trail-length", DEFAULT_TRAIL_LENGTH);
            this.particleCount = config.getInt("values.particle-count", DEFAULT_PARTICLE_COUNT);
            this.blockLifetime = config.getInt("values.block-lifetime-ticks", DEFAULT_BLOCK_LIFETIME_TICKS);
            this.placeBlocks = config.getBoolean("values.place-temp-blocks", DEFAULT_PLACE_BLOCKS);
        }

        public int duration() {
            return duration;
        }

        public double damage() {
            return damage;
        }

        public int tickInterval() {
            return tickInterval;
        }

        public int trailLength() {
            return trailLength;
        }

        public int particleCount() {
            return particleCount;
        }

        public int blockLifetime() {
            return blockLifetime;
        }

        public boolean placeBlocks() {
            return placeBlocks;
        }
    }

    /**
     * A record representing a temporary block.
     */
    private static class TempBlock {

        private final Block block;
        private final Material original;
        private final long revertTick;

        public TempBlock(Block block, Material original, long revertTick) {
            this.block = block;
            this.original = original;
            this.revertTick = revertTick;
        }

        public void revert() {
            if (block.getType() == TrailScheduler.TRAIL_BLOCK_MATERIAL) {
                block.setType(original, false);
            }
        }
    }
}
