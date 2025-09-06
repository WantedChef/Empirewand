package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class LightningArrow implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double damage = spells.getDouble("lightning-arrow.values.damage", 8.0); // 4 hearts
        boolean blockDamage = spells.getBoolean("lightning-arrow.flags.block-damage", false);
        boolean glowing = spells.getBoolean("lightning-arrow.flags.glowing", true);
        int glowingDuration = spells.getInt("lightning-arrow.values.glowing-duration-ticks", 60);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Visual trail config (purely cosmetic)
        int trailLength = spells.getInt("lightning-arrow.values.trail_length", 5);
        int particleCount = spells.getInt("lightning-arrow.values.particle_count", 3);
        int lifeTicks = spells.getInt("lightning-arrow.values.block_lifetime_ticks", 30);
        int sparkInterval = spells.getInt("lightning-arrow.values.spark_interval_ticks", 4);

        // Launch arrow
        Arrow arrow = player.getWorld().spawn(player.getEyeLocation(), Arrow.class);
        arrow.setVelocity(player.getEyeLocation().getDirection().multiply(2.0));
        arrow.setShooter(player);

        // Tag projectile
        arrow.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL,
                Keys.STRING_TYPE.getType(), "lightning-arrow");

        // Register listener for hit detection & cleanup
        ArrowListener listener = new ArrowListener(context, damage, blockDamage, glowing, glowingDuration,
                friendlyFire);
        context.plugin().getServer().getPluginManager().registerEvents(listener, context.plugin());

        // Electric trail effect runnable (independent of hit listener)
        new ElectricTrail(context, arrow, trailLength, particleCount, lifeTicks, sparkInterval, listener)
                .runTaskTimer(context.plugin(), 0L, 1L);

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
    }

    private static class ArrowListener implements Listener {
        private final SpellContext context;
        private final double damage;
        private final boolean blockDamage;
        private final boolean glowing;
        private final int glowingDuration;
        private final boolean friendlyFire;
        private boolean done = false;

        public ArrowListener(SpellContext context, double damage, boolean blockDamage,
                boolean glowing, int glowingDuration, boolean friendlyFire) {
            this.context = context;
            this.damage = damage;
            this.blockDamage = blockDamage;
            this.glowing = glowing;
            this.glowingDuration = glowingDuration;
            this.friendlyFire = friendlyFire;
        }

        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof Arrow))
                return;

            var pdc = projectile.getPersistentDataContainer();
            String spellType = pdc.get(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType());
            if (!"lightning-arrow".equals(spellType))
                return;

            if (done)
                return; // prevent double processing
            done = true;

            Location hitLoc = projectile.getLocation();

            // Strike lightning
            if (blockDamage) {
                projectile.getWorld().strikeLightning(hitLoc);
            } else {
                projectile.getWorld().strikeLightningEffect(hitLoc);
            }

            // Damage nearby entities manually if no block damage
            if (!blockDamage) {
                for (var entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 3.0, 3.0, 3.0)) {
                    if (!(entity instanceof LivingEntity living))
                        continue;
                    if (living.equals(projectile.getShooter()) && !friendlyFire)
                        continue;
                    if (living.isDead() || !living.isValid())
                        continue;

                    living.damage(damage, context.caster());

                    // Apply glowing effect
                    if (glowing) {
                        living.addPotionEffect(
                                new PotionEffect(PotionEffectType.GLOWING, glowingDuration, 0, false, true));
                    }
                }
            }

            // Effects
            context.fx().spawnParticles(hitLoc, Particle.ELECTRIC_SPARK, 20, 0.5, 0.5, 0.5, 0.1);

            // Remove projectile
            projectile.remove();

            // Unregister to avoid leaks
            HandlerList.unregisterAll(this);
        }
    }

    /**
     * Trails the arrow with ELECTRIC sparks and temporary REDSTONE_BLOCK blocks
     * that revert after lifeTicks.
     * Purely visual; blocks are only placed in replaceable air/foliage and
     * reverted.
     */
    private static final class ElectricTrail extends BukkitRunnable {
        private final Arrow arrow;
        private final org.bukkit.World world;
        private final int trailLength;
        private final int particleCount;
        private final int lifeTicks;
        private final int sparkInterval;
        private final ArrowListener listener;

        private int tick = 0;
        private final java.util.Deque<TempBlock> queue = new java.util.ArrayDeque<>();
        private final java.util.Set<Block> ours = new java.util.HashSet<>();

        ElectricTrail(SpellContext ctx, Arrow arrow, int trailLength, int particleCount, int lifeTicks,
                int sparkInterval, ArrowListener listener) {
            this.arrow = arrow;
            this.world = arrow.getWorld();
            this.trailLength = Math.max(1, trailLength);
            this.particleCount = Math.max(1, particleCount);
            this.lifeTicks = Math.max(5, lifeTicks);
            this.sparkInterval = Math.max(1, sparkInterval);
            this.listener = listener;
        }

        @Override
        public void run() {
            if (!arrow.isValid() || arrow.isDead() || listener.done) {
                cleanup();
                cancel();
                return;
            }

            Vector dir = arrow.getVelocity().clone();
            if (dir.lengthSquared() < 0.0001) {
                dir = arrow.getLocation().getDirection();
            }
            dir.normalize();

            Location base = arrow.getLocation().clone().add(0, -0.1, 0);

            // Place trail blocks and particles behind arrow
            for (int i = 0; i < trailLength; i++) {
                Location l = base.clone().add(dir.clone().multiply(-i));
                Block b = l.getBlock();
                if (!ours.contains(b) && isReplaceable(b.getType())) {
                    BlockData prev = b.getBlockData().clone();
                    queue.addLast(new TempBlock(b, prev, tick + lifeTicks));
                    b.setType(Material.REDSTONE_BLOCK, false);
                    ours.add(b);

                    world.spawnParticle(Particle.BLOCK, l.clone().add(0.5, 0.5, 0.5), particleCount, 0.05, 0.05, 0.05,
                            0, Material.REDSTONE_BLOCK.createBlockData());
                    world.spawnParticle(Particle.ELECTRIC_SPARK, l, particleCount, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Occasional additional sparks along forward vector
            if (tick % sparkInterval == 0) {
                Location sparkLoc = arrow.getLocation().clone();
                world.spawnParticle(Particle.CRIT, sparkLoc, 2, 0.05, 0.05, 0.05, 0.0);
                world.spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc, 6, 0.2, 0.2, 0.2, 0.02);
            }

            // Expire old blocks
            while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.REDSTONE_BLOCK && ours.contains(tb.block)) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
            }

            tick++;
            if (tick > 20 * 10) { // 10s safety limit
                cleanup();
                cancel();
            }
        }

        private void cleanup() {
            while (!queue.isEmpty()) {
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.REDSTONE_BLOCK && ours.contains(tb.block)) {
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
        return "lightning-arrow";
    }

    @Override
    public String key() {
        return "lightning-arrow";
    }

    @Override
    public Component displayName() {
        return Component.text("Lightning Arrow");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}