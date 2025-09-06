package com.example.empirewand.spell.implementation;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Fireball;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Prereq;
import com.example.empirewand.spell.ProjectileSpell;
import com.example.empirewand.spell.SpellContext;

import net.kyori.adventure.text.Component;

public class Comet implements ProjectileSpell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        var spells = context.config().getSpellsConfig();
        float yield = (float) spells.getDouble("comet.values.yield", 2.5);
        // Visual-only params (defaults applied if absent; will be added to config
        // later)
        int trailLength = spells.getInt("comet.values.trail_length", 5);
        int particleCount = spells.getInt("comet.values.particle_count", 3);
        int blockLifetime = spells.getInt("comet.values.block_lifetime_ticks", 35);

        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(Math.max(0.0f, yield));
        fireball.setIsIncendiary(false);
        fireball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, getName());
        fireball.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                player.getUniqueId().toString());

        // Custom fiery tail runnable
        new FieryTail(fireball, trailLength, particleCount, blockLifetime).runTaskTimer(context.plugin(), 0L, 1L);

        // Launch FX sound (extra punch)
        context.fx().playSound(player, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
    }

    @Override
    public void onProjectileHit(SpellContext context, Projectile projectile, ProjectileHitEvent event) {
        context.fx().impact(projectile.getLocation());

        if (event.getHitEntity() instanceof LivingEntity target) {
            String ownerUUID = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER,
                    PersistentDataType.STRING);
            double damage = context.config().getSpellsConfig().getDouble("comet.values.damage", 7.0);
            boolean hitPlayers = context.config().getSpellsConfig().getBoolean("comet.flags.hit-players", true);
            boolean hitMobs = context.config().getSpellsConfig().getBoolean("comet.flags.hit-mobs", true);
            boolean isPlayer = target instanceof Player;
            if (((isPlayer && hitPlayers) || (!isPlayer && hitMobs)) && ownerUUID != null) {
                Player caster = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                if (caster != null) {
                    boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);
                    boolean hittingSelfWhenNoFF = (target instanceof Player tgtPlayer) && !friendlyFire
                            && tgtPlayer.getUniqueId().equals(caster.getUniqueId());
                    if (!hittingSelfWhenNoFF) {
                        target.damage(damage, caster);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "comet";
    }

    @Override
    public String key() {
        return "comet";
    }

    @Override
    public Component displayName() {
        return Component.text("Comet");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}

/**
 * Trails a comet fireball with flame/smoke particles and temporary NETHERRACK
 * blocks
 * that revert after a short lifetime. Purely cosmetic.
 */
final class FieryTail extends BukkitRunnable {
    private final Fireball fireball;
    private final org.bukkit.World world;
    private final int trailLength;
    private final int particleCount;
    private final int blockLifetime;

    private int tick = 0;
    private final java.util.Deque<TempBlock> queue = new java.util.ArrayDeque<>();
    private final java.util.Set<Block> ours = new java.util.HashSet<>();

    FieryTail(Fireball fb, int trailLength, int particleCount, int blockLifetime) {
        this.fireball = fb;
        this.world = fb.getWorld();
        this.trailLength = Math.max(1, trailLength);
        this.particleCount = Math.max(1, particleCount);
        this.blockLifetime = Math.max(5, blockLifetime);
    }

    @Override
    public void run() {
        if (!fireball.isValid() || fireball.isDead()) {
            cleanup();
            cancel();
            return;
        }

        Vector dir = fireball.getVelocity().clone();
        if (dir.lengthSquared() < 0.0001)
            dir = fireball.getLocation().getDirection();
        dir.normalize();

        var base = fireball.getLocation().clone().add(0, -0.25, 0);
        for (int i = 0; i < trailLength; i++) {
            var l = base.clone().add(dir.clone().multiply(-i));
            Block b = l.getBlock();
            if (!ours.contains(b) && isReplaceable(b.getType())) {
                BlockData prev = b.getBlockData().clone();
                queue.addLast(new TempBlock(b, prev, tick + blockLifetime));
                b.setType(Material.NETHERRACK, false);
                ours.add(b);

                world.spawnParticle(Particle.BLOCK, l.clone().add(0.5, 0.5, 0.5), particleCount, 0.06, 0.06, 0.06, 0,
                        Material.NETHERRACK.createBlockData());
                world.spawnParticle(Particle.FLAME, l, particleCount * 2, 0.15, 0.15, 0.15, 0.01);
                world.spawnParticle(Particle.SMOKE, l, particleCount, 0.12, 0.12, 0.12, 0.02);
            }
        }

        // Expire old blocks
        while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
            TempBlock tb = queue.pollFirst();
            if (tb.block.getType() == Material.NETHERRACK && ours.contains(tb.block)) {
                tb.block.setBlockData(tb.previous, false);
            }
            ours.remove(tb.block);
        }

        tick++;
        if (tick > 20 * 15) { // safety stop at 15s
            cleanup();
            cancel();
        }
    }

    private void cleanup() {
        while (!queue.isEmpty()) {
            TempBlock tb = queue.pollFirst();
            if (tb.block.getType() == Material.NETHERRACK && ours.contains(tb.block)) {
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
