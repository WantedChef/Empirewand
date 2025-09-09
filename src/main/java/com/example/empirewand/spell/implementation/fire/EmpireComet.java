package com.example.empirewand.spell.implementation.fire;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class EmpireComet extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
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

        var config = spellConfig;
        var explosionYield = config.getDouble("values.yield", 3.5);
        var speed = config.getDouble("values.speed", 0.8);
        var trailLength = config.getInt("values.trail_length", 7);
        var particleCount = config.getInt("values.particle_count", 4);
        var blockLifetime = config.getInt("values.block_lifetime_ticks", 40);
        var burstInterval = config.getInt("values.burst_interval_ticks", 6);

        var comet = caster.launchProjectile(LargeFireball.class);
        comet.setYield((float) explosionYield);
        comet.setIsIncendiary(false);
        comet.setDirection(caster.getEyeLocation().getDirection().multiply(speed));

        context.fx().spawnParticles(caster.getEyeLocation(), Particle.FLAME, 25, 0.4, 0.4, 0.4, 0.12);
        context.fx().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);

        new EmpireCometTail(context, comet, trailLength, particleCount, blockLifetime, burstInterval)
                .runTaskTimer(context.plugin(), 0L, 1L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private static final class EmpireCometTail extends BukkitRunnable {
        private final SpellContext context;
        private final LargeFireball comet;
        private final org.bukkit.World world;
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
            this.world = comet.getWorld();
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
            var base = comet.getLocation().clone().add(0, -0.30, 0);

            for (int i = 0; i < trailLength; i++) {
                var l = base.clone().add(dir.clone().multiply(-i));
                var b = l.getBlock();
                if (!ours.contains(b) && b.getType().isAir()) {
                    queue.addLast(new TempBlock(b, b.getBlockData(), tick + blockLifetime));
                    b.setType(Material.CRIMSON_NYLIUM, false);
                    ours.add(b);
                    context.fx().spawnParticles(l, Particle.FLAME, particleCount * 3, 0.18, 0.18, 0.18, 0.02);
                }
            }

            if (tick % burstInterval == 0) {
                context.fx().spawnParticles(comet.getLocation(), Particle.EXPLOSION, 4, 0.2, 0.2, 0.2, 0.05);
            }

            while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
                var tb = queue.pollFirst();
                if (tb.block.getType() == Material.CRIMSON_NYLIUM) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
            }

            tick++;
            if (tick > 20 * 15) {
                cleanup();
                cancel();
            }
        }

        private void cleanup() {
            while (!queue.isEmpty()) {
                var tb = queue.pollFirst();
                if (tb.block.getType() == Material.CRIMSON_NYLIUM) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
            }
        }

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