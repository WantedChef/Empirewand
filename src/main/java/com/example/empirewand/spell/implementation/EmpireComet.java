package com.example.empirewand.spell.implementation;

import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class EmpireComet implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double explosionYield = spells.getDouble("empire-comet.values.yield", 3.5);
        double speed = spells.getDouble("empire-comet.values.speed", 0.8);
        // Visual parameters (to be added to config later if absent)
        int trailLength = spells.getInt("empire-comet.values.trail_length", 7);
        int particleCount = spells.getInt("empire-comet.values.particle_count", 4);
        int blockLifetime = spells.getInt("empire-comet.values.block_lifetime_ticks", 40);
        int burstInterval = spells.getInt("empire-comet.values.burst_interval_ticks", 6);

        // Launch large fireball (comet)
        LargeFireball comet = caster.launchProjectile(LargeFireball.class);
        comet.setYield((float) explosionYield);
        comet.setIsIncendiary(false); // No fire spread

        // Set velocity
        Vector direction = caster.getEyeLocation().getDirection();
        comet.setVelocity(direction.multiply(speed));

        // Initial launch burst
        context.fx().spawnParticles(caster.getEyeLocation(), Particle.FLAME, 25, 0.4, 0.4, 0.4, 0.12);
        context.fx().spawnParticles(caster.getEyeLocation(), Particle.SMOKE, 20, 0.3, 0.3, 0.3, 0.06);
        context.fx().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);

        // Tail runnable
        new EmpireCometTail(comet, trailLength, particleCount, blockLifetime, burstInterval)
                .runTaskTimer(context.plugin(), 0L, 1L);
    }

    @Override
    public String getName() {
        return "empire-comet";
    }

    @Override
    public String key() {
        return "empire-comet";
    }

    @Override
    public Component displayName() {
        return Component.text("Empire Comet");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}

/**
 * Denser flaming tail for Empire Comet with periodic burst and temporary
 * CRIMSON_NYLIUM blocks.
 */
final class EmpireCometTail extends BukkitRunnable {
    private final LargeFireball comet;
    private final org.bukkit.World world;
    private final int trailLength;
    private final int particleCount;
    private final int blockLifetime;
    private final int burstInterval;

    private int tick = 0;
    private final java.util.Deque<TempBlock> queue = new java.util.ArrayDeque<>();
    private final java.util.Set<Block> ours = new java.util.HashSet<>();

    EmpireCometTail(LargeFireball comet, int trailLength, int particleCount, int blockLifetime, int burstInterval) {
        this.comet = comet;
        this.world = comet.getWorld();
        this.trailLength = Math.max(1, trailLength);
        this.particleCount = Math.max(1, particleCount);
        this.blockLifetime = Math.max(5, blockLifetime);
        this.burstInterval = Math.max(2, burstInterval);
    }

    @Override
    public void run() {
        if (!comet.isValid() || comet.isDead()) {
            cleanup();
            cancel();
            return;
        }

        var dir = comet.getVelocity().clone();
        if (dir.lengthSquared() < 0.0001)
            dir = comet.getLocation().getDirection();
        dir.normalize();
        var base = comet.getLocation().clone().add(0, -0.30, 0);

        for (int i = 0; i < trailLength; i++) {
            var l = base.clone().add(dir.clone().multiply(-i));
            Block b = l.getBlock();
            if (!ours.contains(b) && isReplaceable(b.getType())) {
                BlockData prev = b.getBlockData().clone();
                queue.addLast(new TempBlock(b, prev, tick + blockLifetime));
                b.setType(Material.CRIMSON_NYLIUM, false);
                ours.add(b);
                world.spawnParticle(Particle.BLOCK, l.clone().add(0.5, 0.5, 0.5), particleCount, 0.07, 0.07, 0.07, 0,
                        Material.CRIMSON_NYLIUM.createBlockData());
                world.spawnParticle(Particle.FLAME, l, particleCount * 3, 0.18, 0.18, 0.18, 0.02);
                world.spawnParticle(Particle.SMOKE, l, particleCount, 0.15, 0.15, 0.15, 0.03);
                world.spawnParticle(Particle.LAVA, l, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        if (tick % burstInterval == 0) {
            var cLoc = comet.getLocation();
            world.spawnParticle(Particle.EXPLOSION, cLoc, 4, 0.2, 0.2, 0.2, 0.05);
            world.spawnParticle(Particle.FLAME, cLoc, 12, 0.25, 0.25, 0.25, 0.04);
            world.spawnParticle(Particle.LAVA, cLoc, 4, 0.12, 0.12, 0.12, 0.02);
            world.spawnParticle(Particle.CRIT, cLoc, 3, 0.05, 0.05, 0.05, 0);
        }

        while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
            TempBlock tb = queue.pollFirst();
            if (tb.block.getType() == Material.CRIMSON_NYLIUM && ours.contains(tb.block)) {
                tb.block.setBlockData(tb.previous, false);
            }
            ours.remove(tb.block);
        }

        tick++;
        if (tick > 20 * 15) { // safety
            cleanup();
            cancel();
        }
    }

    private void cleanup() {
        while (!queue.isEmpty()) {
            TempBlock tb = queue.pollFirst();
            if (tb.block.getType() == Material.CRIMSON_NYLIUM && ours.contains(tb.block)) {
                tb.block.setBlockData(tb.previous, false);
            }
            ours.remove(tb.block);
        }
    }

    private boolean isReplaceable(Material m) {
        return m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR ||
                m == Material.SHORT_GRASS || m == Material.TALL_GRASS || m == Material.SNOW || m == Material.FIRE;
    }

    private static final class TempBlock {
        final Block block;
        final BlockData previous;
        final int expireTick;

        TempBlock(Block block, BlockData previous, int expireTick) {
            this.block = block;
            this.previous = previous;
            this.expireTick = expireTick;
        }
    }
}