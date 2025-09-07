package com.example.empirewand.spell.implementation.ice;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.core.storage.Keys;
import com.example.empirewand.spell.Prereq;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;

import net.kyori.adventure.text.Component;

public class GlacialSpike implements Spell {

    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Arrow arrow = player.launchProjectile(Arrow.class);

        double damage = context.config().getSpellsConfig()
                .getDouble("glacial-spike.values.damage", 8.0);
        int spikeLen = context.config().getSpellsConfig()
                .getInt("glacial-spike.values.spike_length", 4);
        int lifeTicks = context.config().getSpellsConfig()
                .getInt("glacial-spike.values.block_lifetime_ticks", 40);

        arrow.setDamage(damage);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setCritical(true);
        arrow.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType(), getName());
        arrow.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, Keys.STRING_TYPE.getType(),
                player.getUniqueId().toString());

        // IJsspike-effect die de pijl volgt en blokken later terugzet
        new IceSpikeTrail(context, arrow, spikeLen, lifeTicks).runTaskTimer(context.plugin(), 0L, 1L);
    }

    @Override
    public String getName() {
        return "glacial-spike";
    }

    @Override
    public String key() {
        return "glacial-spike";
    }

    @Override
    public Component displayName() {
        return Component.text("Glacial Spike");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }

    /**
     * Volgt de pijl en plaatst een korte keten van BLUE_ICE blokken achter de pijl,
     * die automatisch teruggezet worden naar de originele blockdata na lifeTicks.
     */
    private static final class IceSpikeTrail extends BukkitRunnable {
        private final Arrow arrow;
        private final World world;
        private final int spikeLen;
        private final int lifeTicks;

        // Houd bij welke blocks we veranderd hebben en wanneer ze terug moeten
        private int tick = 0;
        private final Deque<TempBlock> queue = new ArrayDeque<>();
        private final Set<Block> ours = new HashSet<>();

        IceSpikeTrail(SpellContext ctx, Arrow arrow, int spikeLen, int lifeTicks) {
            this.arrow = arrow;
            this.world = arrow.getWorld();
            this.spikeLen = Math.max(1, spikeLen);
            this.lifeTicks = Math.max(5, lifeTicks);
        }

        @Override
        public void run() {
            if (!arrow.isValid() || arrow.isDead() || arrow.isInBlock()) {
                cleanup();
                cancel();
                return;
            }

            // Richting van de pijl (val terug op kijkrichting als hij bijna stilstaat)
            Vector dir = arrow.getVelocity().clone();
            if (dir.lengthSquared() < 0.0001) {
                dir = arrow.getLocation().getDirection();
            }
            dir.normalize();

            // Basislocatie net achter de pijl (ietsje omlaag voor “ijsspegel”-look)
            Location base = arrow.getLocation().clone().add(0, -0.25, 0);

            // Plaats een korte “spike”-ketting achter de pijl
            for (int i = 0; i < spikeLen; i++) {
                Location l = base.clone().add(dir.clone().multiply(-i));
                Block b = l.getBlock();

                if (!ours.contains(b) && isReplaceable(b.getType())) {
                    BlockData prev = b.getBlockData().clone();
                    queue.addLast(new TempBlock(b, prev, tick + lifeTicks));
                    // Gebruik BLUE_ICE voor harde “spike” look (kan je aanpassen)
                    b.setType(Material.BLUE_ICE, false);
                    ours.add(b);

                    // Klein beetje visuele flair zonder kosten
                    world.spawnParticle(Particle.BLOCK, l.add(0.5, 0.5, 0.5), 2, 0.05, 0.05, 0.05, 0,
                            Material.BLUE_ICE.createBlockData());
                }
            }

            // Zet verlopen blokken terug
            while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.BLUE_ICE && ours.contains(tb.block)) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
            }

            // Veiligheidsstop (max 15s)
            tick++;
            if (tick > 20 * 15) {
                cleanup();
                cancel();
            }
        }

        private void cleanup() {
            // Alles terugzetten wat nog van ons is
            while (!queue.isEmpty()) {
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.BLUE_ICE && ours.contains(tb.block)) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
            }
        }

        private boolean isReplaceable(Material m) {
            // Voorkom grief: alleen “zachte”/vervangbare blokken.
            // Pas dit lijstje gerust aan op je server.
            return m == Material.AIR
                    || m == Material.CAVE_AIR
                    || m == Material.VOID_AIR
                    || m == Material.SHORT_GRASS
                    || m == Material.TALL_GRASS
                    || m == Material.FERN
                    || m == Material.LARGE_FERN
                    || m == Material.SNOW
                    || m == Material.FIRE;
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
}
