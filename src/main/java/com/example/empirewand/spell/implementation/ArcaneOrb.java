package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class ArcaneOrb implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double speed = spells.getDouble("arcane-orb.values.speed", 0.6);
        double radius = spells.getDouble("arcane-orb.values.radius", 3.5);
        double damage = spells.getDouble("arcane-orb.values.damage", 8.0);
        double knockback = spells.getDouble("arcane-orb.values.knockback", 0.6);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Visual-only enhancement parameters
        int trailLength = spells.getInt("arcane-orb.values.trail_length", 4);
        int particleCount = spells.getInt("arcane-orb.values.particle_count", 3);
        int blockLifetime = spells.getInt("arcane-orb.values.block_lifetime_ticks", 30);
        int haloParticles = spells.getInt("arcane-orb.values.halo_particles", 8);
        double haloSpeedDeg = spells.getDouble("arcane-orb.values.halo_rotation_speed", 12.0);

        Location spawnLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5));
        Snowball orb = player.getWorld().spawn(spawnLoc, Snowball.class);
        orb.setVelocity(player.getEyeLocation().getDirection().multiply(speed));
        orb.setShooter(player);

        // Tag projectile
        orb.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL,
                org.bukkit.persistence.PersistentDataType.STRING, "arcane-orb");

        // Register hit listener for this cast
        HitListener listener = new HitListener(context, radius, damage, knockback, friendlyFire);
        context.plugin().getServer().getPluginManager().registerEvents(listener, context.plugin());

        // Start orb visuals (halo + trailing temp blocks)
        new OrbVisuals(context, orb, trailLength, particleCount, blockLifetime, haloParticles, haloSpeedDeg)
                .runTaskTimer(context.plugin(), 0L, 1L);

        // Launch FX
        context.fx().playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.2f);
        context.fx().spawnParticles(player.getEyeLocation(), Particle.ENCHANT, 20, 0.3, 0.3, 0.3, 0.0);
    }

    /**
     * Visual runnable providing rotating halo and trailing temporary SEA_LANTERN
     * blocks that revert.
     * Purely cosmetic; does not change gameplay.
     */
    private static final class OrbVisuals extends BukkitRunnable {
        private final Snowball orb;
        private final org.bukkit.World world;
        private final int trailLength;
        private final int particleCount;
        private final int blockLifetime;
        private final int haloParticles;
        private final double haloSpeedRad;

        private double angle = 0.0; // radians
        private int tick = 0;

        private final java.util.Deque<TempBlock> queue = new java.util.ArrayDeque<>();
        private final java.util.Set<Block> ours = new java.util.HashSet<>();

        OrbVisuals(SpellContext ctx, Snowball orb, int trailLength, int particleCount, int blockLifetime,
                int haloParticles, double haloSpeedDeg) {
            this.orb = orb;
            this.world = orb.getWorld();
            this.trailLength = Math.max(1, trailLength);
            this.particleCount = Math.max(1, particleCount);
            this.blockLifetime = Math.max(5, blockLifetime);
            this.haloParticles = Math.max(3, haloParticles);
            this.haloSpeedRad = Math.toRadians(Math.max(1.0, haloSpeedDeg));
        }

        @Override
        public void run() {
            if (!orb.isValid() || orb.isDead()) {
                cleanup();
                cancel();
                return;
            }

            // Halo rotation
            Location center = orb.getLocation().clone();
            double radius = 0.6; // fixed visual radius
            for (int i = 0; i < haloParticles; i++) {
                double theta = angle + (Math.PI * 2 * i / haloParticles);
                double x = center.getX() + radius * Math.cos(theta);
                double z = center.getZ() + radius * Math.sin(theta);
                Location p = new Location(center.getWorld(), x, center.getY(), z);
                world.spawnParticle(Particle.ENCHANT, p, 1, 0, 0, 0, 0);
                if (i % 2 == 0) {
                    world.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0);
                }
            }
            angle += haloSpeedRad;

            // Trail blocks & particles
            Vector dir = orb.getVelocity().clone();
            if (dir.lengthSquared() < 0.0001)
                dir = orb.getLocation().getDirection();
            dir.normalize();
            Location base = orb.getLocation().clone().add(0, -0.1, 0);
            for (int i = 0; i < trailLength; i++) {
                Location l = base.clone().add(dir.clone().multiply(-i));
                Block b = l.getBlock();
                if (!ours.contains(b) && isReplaceable(b.getType())) {
                    BlockData prev = b.getBlockData().clone();
                    queue.addLast(new TempBlock(b, prev, tick + blockLifetime));
                    b.setType(Material.SEA_LANTERN, false);
                    ours.add(b);
                    world.spawnParticle(Particle.BLOCK, l.clone().add(0.5, 0.5, 0.5), particleCount, 0.05, 0.05, 0.05,
                            0,
                            Material.SEA_LANTERN.createBlockData());
                    world.spawnParticle(Particle.END_ROD, l, particleCount, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Expire old blocks
            while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.SEA_LANTERN && ours.contains(tb.block)) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
            }

            tick++;
            if (tick > 20 * 12) { // safety stop at 12s
                cleanup();
                cancel();
            }
        }

        private void cleanup() {
            while (!queue.isEmpty()) {
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.SEA_LANTERN && ours.contains(tb.block)) {
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

    private static class HitListener implements Listener {
        private final SpellContext context;
        private final double radius;
        private final double damage;
        private final double knockback;
        private final boolean friendlyFire;

        HitListener(SpellContext context, double radius, double damage, double knockback, boolean friendlyFire) {
            this.context = context;
            this.radius = radius;
            this.damage = damage;
            this.knockback = knockback;
            this.friendlyFire = friendlyFire;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof Snowball))
                return;
            String key = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_SPELL,
                    org.bukkit.persistence.PersistentDataType.STRING);
            if (!"arcane-orb".equals(key))
                return;

            Location hit = projectile.getLocation();

            // Impact FX
            context.fx().impact(hit, Particle.EXPLOSION, 30, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);

            // AoE damage/knockback
            for (var e : hit.getWorld().getNearbyEntities(hit, radius, radius, radius)) {
                if (!(e instanceof LivingEntity living))
                    continue;
                if (!friendlyFire && living.equals(projectile.getShooter()))
                    continue;
                if (living.isDead() || !living.isValid())
                    continue;

                living.damage(damage, context.caster());
                Vector push = living.getLocation().toVector().subtract(hit.toVector()).normalize().multiply(knockback)
                        .setY(0.2);
                living.setVelocity(living.getVelocity().add(push));
            }

            projectile.remove();
        }
    }

    @Override
    public String getName() {
        return "arcane-orb";
    }

    @Override
    public String key() {
        return "arcane-orb";
    }

    @Override
    public Component displayName() {
        return Component.text("Arcane Orb");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
