package nl.wantedchef.empirewand.spell.projectile;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class ArcaneOrb extends ProjectileSpell<Snowball> {

    public static class Builder extends ProjectileSpell.Builder<Snowball> {
        public Builder(EmpireWandAPI api) {
            super(api, Snowball.class);
            this.name = "Arcane Orb";
            this.description = "Launches an orb of arcane energy.";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.PROJECTILE;
            this.trailParticle = null; // Custom trail
            this.hitSound = Sound.ENTITY_GENERIC_EXPLODE;
        }

        @Override
        @NotNull
        public ProjectileSpell<Snowball> build() {
            return new ArcaneOrb(this);
        }
    }

    private ArcaneOrb(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "arcane-orb";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        int trailLength = spellConfig.getInt("values.trail_length", 4);
        int blockLifetime = spellConfig.getInt("values.block_lifetime_ticks", 30);
        int haloParticles = spellConfig.getInt("values.halo_particles", 8);
        double haloSpeedDeg = spellConfig.getDouble("values.halo_rotation_speed", 12.0);

        context.caster().launchProjectile(Snowball.class,
                context.caster().getEyeLocation().getDirection().multiply(speed), projectile -> {
                    projectile.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING,
                            key());
                    projectile.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                            context.caster().getUniqueId().toString());
                    new OrbVisuals(projectile, trailLength, blockLifetime, haloParticles, haloSpeedDeg)
                            .runTaskTimer(context.plugin(), 0L, 1L);
                });

        context.fx().playSound(context.caster(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.2f);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        double radius = spellConfig.getDouble("values.radius", 3.5);
        double damage = spellConfig.getDouble("values.damage", 8.0);
        double knockback = spellConfig.getDouble("values.knockback", 0.6);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        context.fx().impact(projectile.getLocation(), Particle.EXPLOSION, 30, Sound.ENTITY_GENERIC_EXPLODE, 0.8f,
                1.0f);

        for (var e : projectile.getWorld().getNearbyLivingEntities(projectile.getLocation(), radius)) {
            if (e.equals(context.caster()) && !friendlyFire)
                continue;
            e.damage(damage, context.caster());
            Vector push = e.getLocation().toVector().subtract(projectile.getLocation().toVector()).normalize()
                    .multiply(knockback).setY(0.2);
            e.setVelocity(e.getVelocity().add(push));
        }
    }

    private class OrbVisuals extends BukkitRunnable {
        private final Projectile orb;
        private final int trailLength, blockLifetime, haloParticles;
        private final double haloSpeedRad;
        private double angle = 0.0;
        private int tick = 0;
        private final java.util.Deque<TempBlock> queue = new java.util.ArrayDeque<>();
        private final java.util.Set<Block> ours = new java.util.HashSet<>();

        OrbVisuals(Projectile orb, int trailLength, int blockLifetime, int haloParticles,
                double haloSpeedDeg) {
            this.orb = orb;
            this.trailLength = trailLength;
            this.blockLifetime = blockLifetime;
            this.haloParticles = haloParticles;
            this.haloSpeedRad = Math.toRadians(haloSpeedDeg);
        }

        @Override
        public void run() {
            if (!orb.isValid() || orb.isDead()) {
                cleanup();
                cancel();
                return;
            }

            Location center = orb.getLocation();
            for (int i = 0; i < haloParticles; i++) {
                double theta = angle + (Math.PI * 2 * i / haloParticles);
                center.getWorld().spawnParticle(Particle.ENCHANT,
                        center.clone().add(Math.cos(theta) * 0.6, 0, Math.sin(theta) * 0.6), 1, 0, 0, 0, 0);
            }
            angle += haloSpeedRad;

            Vector dir = orb.getVelocity().clone().normalize();
            for (int i = 0; i < trailLength; i++) {
                Block b = center.clone().add(dir.clone().multiply(-i)).getBlock();
                if (!ours.contains(b) && b.getType().isAir()) {
                    queue.addLast(new TempBlock(b, b.getBlockData(), tick + blockLifetime));
                    b.setType(Material.SEA_LANTERN, false);
                    ours.add(b);
                }
            }

            queue.removeIf(tb -> {
                if (tb.expireTick <= tick) {
                    tb.revert();
                    ours.remove(tb.block());
                    return true;
                }
                return false;
            });

            if (tick++ > 20 * 12) {
                cleanup();
                cancel();
            }
        }

        private void cleanup() {
            queue.forEach(TempBlock::revert);
            queue.clear();
            ours.clear();
        }

        private record TempBlock(Block block, BlockData previous, int expireTick) {
            void revert() {
                if (block.getType() == Material.SEA_LANTERN)
                    block.setBlockData(previous, false);
            }
        }
    }
}
