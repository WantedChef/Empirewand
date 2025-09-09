package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * A spell that launches a powerful comet with a lingering, fiery tail.
 */
public class EmpireComet extends Spell<Void> {

    /**
     * The builder for the EmpireComet spell.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new builder for the EmpireComet spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empire Comet";
            this.description = "Launches a blazing comet with a lingering tail.";
            this.cooldown = java.time.Duration.ofSeconds(18);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EmpireComet(this);
        }
    }

    private static final double DEFAULT_YIELD = 3.5;
    private static final double DEFAULT_SPEED = 0.8;
    private static final int DEFAULT_TRAIL_LENGTH = 7;
    private static final int DEFAULT_PARTICLE_COUNT = 4;
    private static final int DEFAULT_BLOCK_LIFETIME_TICKS = 40;
    private static final int DEFAULT_BURST_INTERVAL_TICKS = 6;
    private static final int LAUNCH_PARTICLE_COUNT = 25;
    private static final double LAUNCH_PARTICLE_OFFSET = 0.4;
    private static final double LAUNCH_PARTICLE_SPEED = 0.12;
    private static final float LAUNCH_SOUND_VOLUME = 1.0f;
    private static final float LAUNCH_SOUND_PITCH = 0.5f;
    private static final long TASK_TIMER_DELAY = 0L;
    private static final long TASK_TIMER_PERIOD = 1L;

    private EmpireComet(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "empire-comet";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();
        if (caster == null) {
            return null;
        }

        var config = spellConfig;
        var explosionYield = config.getDouble("values.yield", DEFAULT_YIELD);
        var speed = config.getDouble("values.speed", DEFAULT_SPEED);
        var trailLength = config.getInt("values.trail_length", DEFAULT_TRAIL_LENGTH);
        var particleCount = config.getInt("values.particle_count", DEFAULT_PARTICLE_COUNT);
        var blockLifetime = config.getInt("values.block_lifetime_ticks", DEFAULT_BLOCK_LIFETIME_TICKS);
        var burstInterval = config.getInt("values.burst_interval_ticks", DEFAULT_BURST_INTERVAL_TICKS);

        var comet = caster.launchProjectile(LargeFireball.class);
        comet.setYield((float) explosionYield);
        comet.setIsIncendiary(false);
        comet.setDirection(caster.getEyeLocation().getDirection().multiply(speed));

        context.fx().spawnParticles(caster.getEyeLocation(), Particle.FLAME, LAUNCH_PARTICLE_COUNT, LAUNCH_PARTICLE_OFFSET, LAUNCH_PARTICLE_OFFSET, LAUNCH_PARTICLE_OFFSET, LAUNCH_PARTICLE_SPEED);
        context.fx().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);

        new EmpireCometTail(context, comet, trailLength, particleCount, blockLifetime, burstInterval)
                .runTaskTimer(context.plugin(), TASK_TIMER_DELAY, TASK_TIMER_PERIOD);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    /**
     * A runnable that creates a fiery tail effect for the Empire Comet.
     */
    private static final class EmpireCometTail extends BukkitRunnable {
        private static final double Y_OFFSET = -0.30;
        private static final int PARTICLE_MULTIPLIER = 3;
        private static final double PARTICLE_OFFSET = 0.18;
        private static final double PARTICLE_SPEED = 0.02;
        private static final int BURST_PARTICLE_COUNT = 4;
        private static final double BURST_PARTICLE_OFFSET = 0.2;
        private static final double BURST_PARTICLE_SPEED = 0.05;
        private static final int MAX_LIFETIME_TICKS = 300; // 15 seconds

        private final SpellContext context;
        private final LargeFireball comet;
        private final int trailLength;
        private final int particleCount;
        private final int blockLifetime;
        private final int burstInterval;
        private int tick = 0;
        private final java.util.Deque<TempBlock> queue = new java.util.ArrayDeque<>();
        private final java.util.Set<Block> ours = new java.util.HashSet<>();

        EmpireCometTail(SpellContext context, LargeFireball comet, int trailLength, int particleCount,
                int blockLifetime, int burstInterval) {
            this.context = context;
            this.comet = comet;
            this.trailLength = trailLength;
            this.particleCount = particleCount;
            this.blockLifetime = blockLifetime;
            this.burstInterval = burstInterval;
        }

        @Override
        public void run() {
            if (!comet.isValid() || comet.isDead()) {
                cleanup();
                cancel();
                return;
            }

            var dir = comet.getVelocity().clone().normalize();
            var base = comet.getLocation().clone().add(0, Y_OFFSET, 0);

            for (int i = 0; i < trailLength; i++) {
                var l = base.clone().add(dir.clone().multiply(-i));
                var b = l.getBlock();
                if (!ours.contains(b) && b.getType().isAir()) {
                    queue.addLast(new TempBlock(b, b.getBlockData(), tick + blockLifetime));
                    b.setType(Material.CRIMSON_NYLIUM, false);
                    ours.add(b);
                    context.fx().spawnParticles(l, Particle.FLAME, particleCount * PARTICLE_MULTIPLIER, PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_SPEED);
                }
            }

            if (tick % burstInterval == 0) {
                context.fx().spawnParticles(comet.getLocation(), Particle.EXPLOSION, BURST_PARTICLE_COUNT, BURST_PARTICLE_OFFSET, BURST_PARTICLE_OFFSET, BURST_PARTICLE_OFFSET, BURST_PARTICLE_SPEED);
            }

            while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
                var tb = queue.pollFirst();
                if (tb.block.getType() == Material.CRIMSON_NYLIUM) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
            }

            tick++;
            if (tick > MAX_LIFETIME_TICKS) {
                cleanup();
                cancel();
            }
        }

        /**
         * Cleans up any temporary blocks created by the tail.
         */
        private void cleanup() {
            while (!queue.isEmpty()) {
                var tb = queue.pollFirst();
                if (tb.block.getType() == Material.CRIMSON_NYLIUM) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
            }
        }

        /**
         * A record representing a temporary block.
         */
        private static class TempBlock {
            Block block;
            BlockData previous;
            int expireTick;

            public TempBlock(Block block, BlockData previous, int expireTick) {
                this.block = block;
                this.previous = previous;
                this.expireTick = expireTick;
            }
        }
    }
}
