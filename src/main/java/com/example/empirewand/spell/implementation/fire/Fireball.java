package com.example.empirewand.spell.implementation.fire;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.core.storage.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class Fireball implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double explosionYield = spells.getDouble("fireball.values.yield", 3.0);
        double projectileSpeed = spells.getDouble("fireball.values.speed", 1.0);
        boolean incendiary = spells.getBoolean("fireball.flags.incendiary", true);
        boolean blockDamage = spells.getBoolean("fireball.flags.block-damage", true);
        int trailLength = spells.getInt("fireball.values.trail_length", 4);
        int particleCount = spells.getInt("fireball.values.particle_count", 2);
        int lifeTicks = spells.getInt("fireball.values.block_lifetime_ticks", 40);

        // Launch fireball
        Location spawnLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5));
        org.bukkit.entity.Fireball fireball = player.getWorld().spawn(spawnLoc, org.bukkit.entity.Fireball.class);
        fireball.setVelocity(player.getEyeLocation().getDirection().multiply(projectileSpeed));
        fireball.setYield((float) explosionYield);
        fireball.setIsIncendiary(incendiary);
        fireball.setShooter(player);

        // Tag projectile
        fireball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL,
                Keys.STRING_TYPE.getType(), "fireball");

        // Fire trail effect
        new FireTrail(context, fireball, trailLength, particleCount, lifeTicks).runTaskTimer(context.plugin(), 0L, 1L);

        // Register listener for custom explosion handling
        context.plugin().getServer().getPluginManager().registerEvents(
                new FireballListener(context, blockDamage), context.plugin());

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }

    private static class FireballListener implements Listener {
        private final SpellContext context;
        private final boolean blockDamage;

        public FireballListener(SpellContext context, boolean blockDamage) {
            this.context = context;
            this.blockDamage = blockDamage;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof org.bukkit.entity.Fireball))
                return;

            var pdc = projectile.getPersistentDataContainer();
            String spellType = pdc.get(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType());
            if (!"fireball".equals(spellType))
                return;

            Location hitLoc = projectile.getLocation();

            // Create custom explosion if block damage is disabled
            if (!blockDamage) {
                // Damage nearby entities manually
                for (var entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 4.0, 4.0, 4.0)) {
                    if (!(entity instanceof LivingEntity living))
                        continue;
                    if (living.equals(projectile.getShooter()))
                        continue; // Self-immunity
                    if (living.isDead() || !living.isValid())
                        continue;

                    // Calculate damage based on distance
                    double distance = living.getLocation().distance(hitLoc);
                    double damage = 20.0 * (1.0 - distance / 4.0); // Max 10 hearts at center
                    if (damage > 0) {
                        living.damage(Math.max(damage, 1.0), context.caster());
                    }
                }

                // Explosion particles without block damage
                context.fx().spawnParticles(hitLoc, Particle.EXPLOSION, 30, 0.5, 0.5, 0.5, 0.1);
                context.fx().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            }

            // Remove projectile
            projectile.remove();

            // Unregister listener after handling to avoid leaks
            HandlerList.unregisterAll(this);
        }
    }

    /**
     * Trails the fireball with FLAME particles and temporary ASH blocks, cleans up
     * after lifeTicks.
     */
    private static final class FireTrail extends BukkitRunnable {
        private final org.bukkit.entity.Fireball fireball;
        private final org.bukkit.World world;
        private final int trailLength;
        private final int particleCount;
        private final int lifeTicks;

        private int tick = 0;
        private final Deque<TempBlock> queue = new ArrayDeque<>();
        private final Set<Block> ours = new HashSet<>();

        FireTrail(SpellContext ctx, org.bukkit.entity.Fireball fireball, int trailLength, int particleCount,
                int lifeTicks) {
            this.fireball = fireball;
            this.world = fireball.getWorld();
            this.trailLength = Math.max(1, trailLength);
            this.particleCount = Math.max(1, particleCount);
            this.lifeTicks = Math.max(5, lifeTicks);
        }

        @Override
        public void run() {
            if (!fireball.isValid() || fireball.isDead()) {
                cleanup();
                cancel();
                return;
            }

            Vector dir = fireball.getVelocity().clone();
            if (dir.lengthSquared() < 0.0001) {
                dir = fireball.getLocation().getDirection();
            }
            dir.normalize();

            Location base = fireball.getLocation().clone().add(0, -0.25, 0);

            for (int i = 0; i < trailLength; i++) {
                Location l = base.clone().add(dir.clone().multiply(-i));
                Block b = l.getBlock();

                if (!ours.contains(b) && isReplaceable(b.getType())) {
                    BlockData prev = b.getBlockData().clone();
                    queue.addLast(new TempBlock(b, prev, tick + lifeTicks));
                    b.setType(Material.MAGMA_BLOCK, false);
                    ours.add(b);

                    world.spawnParticle(Particle.BLOCK, l.add(0.5, 0.5, 0.5), particleCount, 0.05, 0.05, 0.05, 0,
                            Material.MAGMA_BLOCK.createBlockData());
                    world.spawnParticle(Particle.FLAME, l, particleCount, 0.1, 0.1, 0.1, 0.01);
                }
            }

            while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.MAGMA_BLOCK && ours.contains(tb.block)) {
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
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.MAGMA_BLOCK && ours.contains(tb.block)) {
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

    @Override
    public String getName() {
        return "fireball";
    }

    @Override
    public String key() {
        return "fireball";
    }

    @Override
    public Component displayName() {
        return Component.text("Fireball");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
