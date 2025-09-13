package nl.wantedchef.empirewand.spell.projectile;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
        int haloParticles = spellConfig.getInt("values.halo_particles", 8);
        double haloSpeedDeg = spellConfig.getDouble("values.halo_rotation_speed", 12.0);

        context.caster().launchProjectile(Snowball.class,
                context.caster().getEyeLocation().getDirection().multiply(speed), projectile -> {
                    projectile.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING,
                            key());
                    projectile.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                            context.caster().getUniqueId().toString());
                    new OrbVisuals(projectile, haloParticles, haloSpeedDeg)
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
        private final int haloParticles;
        private final double haloSpeedRad;
        private double angle = 0.0;
        private int tick = 0;

        OrbVisuals(Projectile orb, int haloParticles, double haloSpeedDeg) {
            this.orb = orb;
            this.haloParticles = haloParticles;
            this.haloSpeedRad = Math.toRadians(haloSpeedDeg);
        }

        @Override
        public void run() {
            if (!orb.isValid() || orb.isDead()) {
                cancel();
                return;
            }

            Location center = orb.getLocation();
            for (int i = 0; i < haloParticles; i++) {
                double theta = angle + (Math.PI * 2 * i / haloParticles);
                center.getWorld().spawnParticle(Particle.ENCHANT,
                        center.clone().add(Math.cos(theta) * 0.6, 0, Math.sin(theta) * 0.6), 1, 0, 0, 0, 0);
            }
            // simple forward sparkle
            Vector v = orb.getVelocity().clone().normalize();
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(v.multiply(0.4)), 1, 0.02, 0.02, 0.02, 0.003);
            angle += haloSpeedRad;

            if (tick++ > 20 * 12) {
                cancel();
            }
        }
    }
}
