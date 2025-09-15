package nl.wantedchef.empirewand.spell.fire.advanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * A spell that launches a powerful comet with a lingering, fiery tail.
 * Features proper resource management and optimized particle effects.
 */
public class EmpireComet extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empire Comet";
            this.description = "Launches a blazing comet with a lingering trail";
            this.cooldown = Duration.ofSeconds(18);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EmpireComet(this);
        }
    }

    // Configuration defaults
    private static final double DEFAULT_YIELD = 3.5;
    private static final double DEFAULT_SPEED = 0.8;
    private static final int DEFAULT_TRAIL_LENGTH = 7;
    private static final int DEFAULT_PARTICLE_COUNT = 4;
    private static final int DEFAULT_BLOCK_LIFETIME_TICKS = 40;
    private static final int DEFAULT_BURST_INTERVAL_TICKS = 6;

    // Effect constants
    private static final int LAUNCH_PARTICLE_COUNT = 25;
    private static final double LAUNCH_PARTICLE_OFFSET = 0.4;
    private static final double LAUNCH_PARTICLE_SPEED = 0.12;
    private static final int MAX_LIFETIME_TICKS = 300; // 15 seconds max

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
    protected Void executeSpell(@NotNull SpellContext context) {
        Player caster = context.caster();

        // Load configuration
        double explosionYield = spellConfig.getDouble("values.yield", DEFAULT_YIELD);
        double speed = spellConfig.getDouble("values.speed", DEFAULT_SPEED);
        int trailLength = spellConfig.getInt("values.trail_length", DEFAULT_TRAIL_LENGTH);
        int particleCount = spellConfig.getInt("values.particle_count", DEFAULT_PARTICLE_COUNT);
        int blockLifetime = spellConfig.getInt("values.block_lifetime_ticks", DEFAULT_BLOCK_LIFETIME_TICKS);
        int burstInterval = spellConfig.getInt("values.burst_interval_ticks", DEFAULT_BURST_INTERVAL_TICKS);

        // Spawn comet above caster
        Location spawnLocation = caster.getLocation().clone().add(0, 8, 0);
        LargeFireball comet = spawnLocation.getWorld().spawn(spawnLocation, LargeFireball.class);
        comet.setYield((float) explosionYield);
        comet.setIsIncendiary(false);
        comet.setDirection(new Vector(0, -speed, 0));
        comet.setShooter(caster);

        // Launch effects
        context.fx().spawnParticles(caster.getEyeLocation(), Particle.FLAME, LAUNCH_PARTICLE_COUNT,
            LAUNCH_PARTICLE_OFFSET, LAUNCH_PARTICLE_OFFSET, LAUNCH_PARTICLE_OFFSET, LAUNCH_PARTICLE_SPEED);
        context.fx().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);

        // Create trail effect task
        BukkitTask task = new EmpireCometTail(context, comet, trailLength, particleCount, blockLifetime, burstInterval)
            .runTaskTimer(context.plugin(), 0L, 1L);

        // Register task for cleanup
        if (context.plugin() instanceof nl.wantedchef.empirewand.EmpireWandPlugin plugin) {
            plugin.getTaskManager().registerTask(task);
        }

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in executeSpell
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

        private final SpellContext context;
        private final LargeFireball comet;
        private final int trailLength;
        private final int particleCount;
        private final int blockLifetime;
        private final int burstInterval;
        private int tick = 0;
        private final Deque<TempBlock> queue = new ArrayDeque<>();
        private final Set<Block> ours = new HashSet<>();

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
            if (!comet.isValid() || comet.isDead() || tick > MAX_LIFETIME_TICKS) {
                cleanup();
                cancel();
                return;
            }

            Vector dir = comet.getVelocity().clone().normalize();
            Location base = comet.getLocation().clone().add(0, Y_OFFSET, 0);

            // Create trail
            for (int i = 0; i < trailLength; i++) {
                Location trailLoc = base.clone().add(dir.clone().multiply(-i));
                Block block = trailLoc.getBlock();

                if (!ours.contains(block) && block.getType().isAir()) {
                    queue.addLast(new TempBlock(block, block.getBlockData(), tick + blockLifetime));
                    block.setType(Material.CRIMSON_NYLIUM, false);
                    ours.add(block);

                    // Spawn particles
                    context.fx().spawnParticles(trailLoc, Particle.FLAME, particleCount * PARTICLE_MULTIPLIER,
                        PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_SPEED);
                }
            }

            // Create burst effects periodically
            if (tick % burstInterval == 0) {
                context.fx().spawnParticles(comet.getLocation(), Particle.EXPLOSION, BURST_PARTICLE_COUNT,
                    BURST_PARTICLE_OFFSET, BURST_PARTICLE_OFFSET, BURST_PARTICLE_OFFSET, BURST_PARTICLE_SPEED);
            }

            // Clean up expired blocks
            while (!queue.isEmpty() && queue.peekFirst().expireTick() <= tick) {
                TempBlock tb = queue.pollFirst();
                if (tb.block().getType() == Material.CRIMSON_NYLIUM) {
                    tb.block().setBlockData(tb.previous(), false);
                }
                ours.remove(tb.block());
            }

            tick++;
        }

        /**
         * Cleans up all temporary blocks created by the trail.
         */
        private void cleanup() {
            while (!queue.isEmpty()) {
                TempBlock tb = queue.pollFirst();
                if (tb.block().getType() == Material.CRIMSON_NYLIUM) {
                    tb.block().setBlockData(tb.previous(), false);
                }
                ours.remove(tb.block());
            }
            ours.clear();
        }

        /**
         * A record representing a temporary block.
         */
        private record TempBlock(Block block, BlockData previous, int expireTick) {
        }
    }
}