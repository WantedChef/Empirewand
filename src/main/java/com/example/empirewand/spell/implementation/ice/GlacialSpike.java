package com.example.empirewand.spell.implementation.ice;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.core.storage.Keys;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.ProjectileSpell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class GlacialSpike extends ProjectileSpell<Arrow> {

    public static class Builder extends ProjectileSpell.Builder<Arrow> {
        public Builder(EmpireWandAPI api) {
            super(api, Arrow.class);
            this.name = "Glacial Spike";
            this.description = "Fires a spike of ice.";
            this.manaCost = 6; // Example
            this.cooldown = java.time.Duration.ofSeconds(4);
            this.spellType = SpellType.ICE;
            this.trailParticle = null; // Custom trail
        }

        @Override
        @NotNull
        public ProjectileSpell<Arrow> build() {
            return new GlacialSpike(this);
        }
    }

    private GlacialSpike(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "glacial-spike";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player caster = context.caster();
        double damage = spellConfig.getDouble("values.damage", 8.0);
        int spikeLen = spellConfig.getInt("values.spike_length", 4);
        int lifeTicks = spellConfig.getInt("values.block_lifetime_ticks", 40);

        caster.launchProjectile(Arrow.class, caster.getEyeLocation().getDirection(), arrow -> {
            arrow.setDamage(damage);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setCritical(true);
            arrow.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            arrow.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING, caster.getUniqueId().toString());
            new IceSpikeTrail(arrow, spikeLen, lifeTicks).runTaskTimer(context.plugin(), 0L, 1L);
        });
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile, @NotNull ProjectileHitEvent event) {
        // Vanilla arrow damage is handled by the projectile itself.
        // Additional effects on hit could be added here.
    }

    private static final class IceSpikeTrail extends BukkitRunnable {
        private final Arrow arrow;
        private final World world;
        private final int spikeLen;
        private final int lifeTicks;
        private int tick = 0;
        private final Deque<TempBlock> queue = new ArrayDeque<>();
        private final Set<Block> ours = new HashSet<>();

        IceSpikeTrail(Arrow arrow, int spikeLen, int lifeTicks) {
            this.arrow = arrow;
            this.world = arrow.getWorld();
            this.spikeLen = spikeLen;
            this.lifeTicks = lifeTicks;
        }

        @Override
        public void run() {
            if (!arrow.isValid() || arrow.isDead() || arrow.isInBlock()) {
                cleanup();
                cancel();
                return;
            }

            Vector dir = arrow.getVelocity().clone().normalize();
            Location base = arrow.getLocation().clone().add(0, -0.25, 0);

            for (int i = 0; i < spikeLen; i++) {
                Location l = base.clone().add(dir.clone().multiply(-i));
                Block b = l.getBlock();
                if (!ours.contains(b) && b.getType().isAir()) {
                    queue.addLast(new TempBlock(b, b.getBlockData(), tick + lifeTicks));
                    b.setType(Material.BLUE_ICE, false);
                    ours.add(b);
                    world.spawnParticle(Particle.BLOCK, l.add(0.5, 0.5, 0.5), 2, 0.05, 0.05, 0.05, 0, Material.BLUE_ICE.createBlockData());
                }
            }

            while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
                queue.pollFirst().revert();
            }

            tick++;
            if (tick > 20 * 15) {
                cleanup();
                cancel();
            }
        }

        private void cleanup() {
            while (!queue.isEmpty()) {
                queue.pollFirst().revert();
            }
        }

        private record TempBlock(Block block, BlockData previous, int expireTick) {
            void revert() {
                if (block.getType() == Material.BLUE_ICE) {
                    block.setBlockData(previous, false);
                }
            }
        }
    }
}