package com.example.empirewand.spell.implementation.fire;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.EffectService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ExplosionTrail extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Explosion Trail";
            this.description = "You become a walking explosion, damaging nearby entities.";
            this.manaCost = 15; // Example value
            this.cooldown = java.time.Duration.ofSeconds(25);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ExplosionTrail(this);
        }
    }

    private final Config config;

    private ExplosionTrail(Builder builder) {
        super(builder);
        this.config = new Config(spellConfig);
    }

    @Override
    public String key() {
        return "explosion-trail";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        startTrailScheduler(player, context);
        context.fx().playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private void startTrailScheduler(Player caster, SpellContext context) {
        new TrailScheduler(caster, context, config).runTaskTimer(context.plugin(), 0L, config.tickInterval());
    }

    private static class TrailScheduler extends BukkitRunnable {

        private final Player caster;
        private final SpellContext context;
        private final Config config;
        private final Deque<TempBlock> tempBlocks;
        private int ticks;

        public TrailScheduler(Player caster, SpellContext context, Config config) {
            this.caster = caster;
            this.context = context;
            this.config = config;
            this.tempBlocks = new ConcurrentLinkedDeque<>();
        }

        @Override
        public void run() {
            if (ticks >= config.duration() || !caster.isValid() || caster.isDead()) {
                cleanup();
                this.cancel();
                return;
            }

            Location playerLoc = caster.getLocation();
            var world = playerLoc.getWorld();
            if (world == null) {
                cleanup();
                this.cancel();
                return;
            }

            damageNearbyEntities(playerLoc, world);
            spawnParticles(playerLoc);
            playSound(playerLoc);

            if (config.placeBlocks()) {
                placeBlock(playerLoc, world);
            }

            removeExpiredBlocks(world);
            ticks += config.tickInterval();
        }

        private void damageNearbyEntities(Location playerLoc, org.bukkit.World world) {
            for (var entity : world.getNearbyEntities(playerLoc, 3.0, 3.0, 3.0)) {
                if (entity instanceof LivingEntity living && !living.equals(caster) && !living.isDead()
                        && living.isValid()) {
                    living.damage(config.damage(), caster);
                }
            }
        }

        private void spawnParticles(Location playerLoc) {
            context.fx().spawnParticles(playerLoc, Particle.EXPLOSION, config.particleCount(), 0.2, 0.2, 0.2, 0.05);
        }

        private void playSound(Location playerLoc) {
            context.fx().playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.1f);
        }

        private void placeBlock(Location playerLoc, org.bukkit.World world) {
            Block below = playerLoc.clone().subtract(0, 1, 0).getBlock();
            if (below.getType().isAir()) {
                tempBlocks.addLast(new TempBlock(below, below.getType(), world.getFullTime() + config.blockLifetime()));
                below.setType(Material.NETHERRACK, false);
                while (tempBlocks.size() > config.trailLength()) {
                    tempBlocks.removeFirst().revert();
                }
            }
        }

        private void removeExpiredBlocks(org.bukkit.World world) {
            long now = world.getFullTime();
            tempBlocks.removeIf(tb -> {
                if (tb.revertTick <= now) {
                    tb.revert();
                    return true;
                }
                return false;
            });
        }

        private void cleanup() {
            while (!tempBlocks.isEmpty()) {
                tempBlocks.removeFirst().revert();
            }
        }
    }

    private static class Config {

        private final int duration;
        private final double damage;
        private final int tickInterval;
        private final int trailLength;
        private final int particleCount;
        private final int blockLifetime;
        private final boolean placeBlocks;

        public Config(org.bukkit.configuration.ConfigurationSection config) {
            this.duration = config.getInt("values.duration-ticks", 100);
            this.damage = config.getDouble("values.damage", 8.0);
            this.tickInterval = config.getInt("values.tick-interval", 10);
            this.trailLength = config.getInt("values.trail-length", 12);
            this.particleCount = config.getInt("values.particle-count", 10);
            this.blockLifetime = config.getInt("values.block-lifetime-ticks", 40);
            this.placeBlocks = config.getBoolean("values.place-temp-blocks", true);
        }

        public int duration() {
            return duration;
        }

        public double damage() {
            return damage;
        }

        public int tickInterval() {
            return tickInterval;
        }

        public int trailLength() {
            return trailLength;
        }

        public int particleCount() {
            return particleCount;
        }

        public int blockLifetime() {
            return blockLifetime;
        }

        public boolean placeBlocks() {
            return placeBlocks;
        }
    }

    private static class TempBlock {

        private final Block block;
        private final Material original;
        private final long revertTick;

        public TempBlock(Block block, Material original, long revertTick) {
            this.block = block;
            this.original = original;
            this.revertTick = revertTick;
        }

        public void revert() {
            if (block.getType() == Material.NETHERRACK) {
                block.setType(original, false);
            }
        }
    }
}