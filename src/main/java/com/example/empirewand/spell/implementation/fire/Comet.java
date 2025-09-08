package com.example.empirewand.spell.implementation.fire;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.ConfigService;

import com.example.empirewand.core.storage.Keys;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.ProjectileSpell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Comet extends ProjectileSpell<Fireball> {

    public static class Builder extends ProjectileSpell.Builder<Fireball> {
        public Builder(EmpireWandAPI api) {
            super(api, Fireball.class);
            this.name = "Comet";
            this.description = "Launches a fiery projectile that explodes on impact.";
            this.manaCost = 15;
            this.cooldown = java.time.Duration.ofSeconds(20);
            this.spellType = SpellType.FIRE;
            this.hitSound = null; // Explosion handles sound
            this.trailParticle = null; // Custom trail
        }

        @Override
        @NotNull
        public ProjectileSpell<Fireball> build() {
            return new Comet(this);
        }
    }

    private Comet(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "comet";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player caster = context.caster();
        Location launchLocation = caster.getEyeLocation();
        Vector direction = launchLocation.getDirection().normalize().multiply(speed);

        float yield = (float) spellConfig.getDouble("values.yield", 2.5f);
        int trailLength = spellConfig.getInt("values.trail_length", 5);
        int particleCount = spellConfig.getInt("values.particle_count", 3);
        int blockLifetime = spellConfig.getInt("values.block_lifetime_ticks", 35);

        Fireball fireball = launchLocation.getWorld().spawn(launchLocation, Fireball.class, fb -> {
            fb.setShooter(caster);
            fb.setYield(yield);
            fb.setIsIncendiary(false);
            fb.setDirection(direction);
            fb.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            fb.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                    caster.getUniqueId().toString());
        });

        new FieryTail(fireball, trailLength, particleCount, blockLifetime).runTaskTimer(context.plugin(), 0L, 1L);
        context.fx().playSound(caster, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        context.fx().impact(projectile.getLocation());

        if (event.getHitEntity() instanceof LivingEntity target) {
            String ownerUUID = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER,
                    PersistentDataType.STRING);
            double damage = spellConfig.getDouble("values.damage", 7.0);
            boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig().getBoolean("features.friendly-fire", false);

            if (ownerUUID != null) {
                Player caster = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                if (caster != null && !(target.equals(caster) && !friendlyFire)) {
                    target.damage(damage, caster);
                }
            }
        }
    }

    private static final class FieryTail extends BukkitRunnable {
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
            this.trailLength = trailLength;
            this.particleCount = particleCount;
            this.blockLifetime = blockLifetime;
        }

        @Override
        public void run() {
            if (!fireball.isValid() || fireball.isDead()) {
                cleanup();
                cancel();
                return;
            }

            Vector dir = fireball.getVelocity().clone().normalize();
            Location base = fireball.getLocation().clone().add(0, -0.25, 0);

            for (int i = 0; i < trailLength; i++) {
                Location l = base.clone().add(dir.clone().multiply(-i));
                Block b = l.getBlock();
                if (!ours.contains(b) && (b.getType().isAir() || b.getType() == Material.SNOW)) {
                    queue.addLast(new TempBlock(b, b.getBlockData(), tick + blockLifetime));
                    b.setType(Material.NETHERRACK, false);
                    ours.add(b);
                    world.spawnParticle(Particle.FLAME, l, particleCount * 2, 0.15, 0.15, 0.15, 0.01);
                }
            }

            while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.NETHERRACK) {
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
                if (tb.block.getType() == Material.NETHERRACK) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
            }
        }

        private record TempBlock(Block block, BlockData previous, int expireTick) {
        }
    }
}