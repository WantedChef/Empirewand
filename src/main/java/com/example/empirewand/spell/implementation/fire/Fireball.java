package com.example.empirewand.spell.implementation.fire;

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
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Fireball extends ProjectileSpell<org.bukkit.entity.Fireball> {

    public static class Builder extends ProjectileSpell.Builder<org.bukkit.entity.Fireball> {
        public Builder(EmpireWandAPI api) {
            super(api, org.bukkit.entity.Fireball.class);
            this.name = "Fireball";
            this.description = "Launches a standard fireball.";
            this.manaCost = 8; // Example
            this.cooldown = java.time.Duration.ofSeconds(5);
            this.spellType = SpellType.FIRE;
            this.trailParticle = null; // Custom trail
            this.hitSound = null; // Vanilla explosion sound
        }

        @Override
        @NotNull
        public ProjectileSpell<org.bukkit.entity.Fireball> build() {
            return new Fireball(this);
        }
    }

    private Fireball(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "fireball";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player player = context.caster();

        double explosionYield = spellConfig.getDouble("values.yield", 3.0);
        boolean incendiary = spellConfig.getBoolean("flags.incendiary", true);
        int trailLength = spellConfig.getInt("values.trail_length", 4);
        int particleCount = spellConfig.getInt("values.particle_count", 2);
        int lifeTicks = spellConfig.getInt("values.block_lifetime_ticks", 40);

        player.launchProjectile(org.bukkit.entity.Fireball.class, player.getEyeLocation().getDirection().multiply(speed), fireball -> {
            fireball.setYield((float) explosionYield);
            fireball.setIsIncendiary(incendiary);
            fireball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            fireball.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());
            new FireTrail(fireball, trailLength, particleCount, lifeTicks).runTaskTimer(context.plugin(), 0L, 1L);
        });

        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile, @NotNull ProjectileHitEvent event) {
        boolean blockDamage = spellConfig.getBoolean("flags.block-damage", true);
        if (!blockDamage) {
            // If block damage is disabled, create a visual-only explosion
            // and manually damage entities, since the projectile's explosion is cancelled.
            Location hitLoc = projectile.getLocation();
            hitLoc.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, 30, 0.5, 0.5, 0.5, 0.1);
            hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

            for (var entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 4.0, 4.0, 4.0)) {
                if (entity instanceof LivingEntity living && !living.equals(context.caster())) {
                    double distance = living.getLocation().distance(hitLoc);
                    double damage = 20.0 * (1.0 - distance / 4.0); // Max 10 hearts
                    if (damage > 0) {
                        living.damage(Math.max(damage, 1.0), context.caster());
                    }
                }
            }
            event.setCancelled(true); // Cancel the vanilla explosion
        }
    }

    private static final class FireTrail extends BukkitRunnable {
        private final org.bukkit.entity.Fireball fireball;
        private final int trailLength;
        private final int particleCount;
        private final int lifeTicks;
        private int tick = 0;
        private final Deque<TempBlock> queue = new ArrayDeque<>();
        private final Set<Block> ours = new HashSet<>();

        FireTrail(org.bukkit.entity.Fireball fireball, int trailLength, int particleCount, int lifeTicks) {
            this.fireball = fireball;
            this.trailLength = trailLength;
            this.particleCount = particleCount;
            this.lifeTicks = lifeTicks;
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
                if (!ours.contains(b) && b.getType().isAir()) {
                    queue.addLast(new TempBlock(b, b.getBlockData(), tick + lifeTicks));
                    b.setType(Material.MAGMA_BLOCK, false);
                    ours.add(b);
                    fireball.getWorld().spawnParticle(Particle.FLAME, l, particleCount, 0.1, 0.1, 0.1, 0.01);
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
                if (block.getType() == Material.MAGMA_BLOCK) {
                    block.setBlockData(previous, false);
                }
            }
        }
    }
}