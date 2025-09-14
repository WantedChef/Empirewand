package nl.wantedchef.empirewand.spell.lightning;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Spark extends ProjectileSpell<Snowball> {

    public static class Builder extends ProjectileSpell.Builder<Snowball> {
        public Builder(EmpireWandAPI api) {
            super(api, Snowball.class);
            this.name = "Spark";
            this.description = "Fires a fast spark of lightning energy like a rod.";
            this.cooldown = java.time.Duration.ofSeconds(2);
            this.spellType = SpellType.LIGHTNING;
            this.trailParticle = null; // Custom trail
            this.hitSound = Sound.ENTITY_LIGHTNING_BOLT_IMPACT;
        }

        @Override
        @NotNull
        public ProjectileSpell<Snowball> build() {
            return new Spark(this);
        }
    }

    private Spark(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "spark";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        int trailSteps = spellConfig.getInt("values.trail-steps", 8);
        int particleCount = spellConfig.getInt("values.particle-count", 6);
        int burstInterval = spellConfig.getInt("values.burst-interval-ticks", 10);

        context.caster().launchProjectile(Snowball.class,
                context.caster().getEyeLocation().getDirection().multiply(speed), projectile -> {
                    projectile.getPersistentDataContainer().set(
                            nl.wantedchef.empirewand.core.storage.Keys.PROJECTILE_SPELL,
                            org.bukkit.persistence.PersistentDataType.STRING, key());
                    projectile.getPersistentDataContainer().set(
                            nl.wantedchef.empirewand.core.storage.Keys.PROJECTILE_OWNER,
                            org.bukkit.persistence.PersistentDataType.STRING,
                            context.caster().getUniqueId().toString());
                    new ParticleTrail(context, projectile, particleCount, trailSteps, burstInterval)
                            .runTaskTimer(context.plugin(), 0L, 1L);
                });

        context.fx().playSound(context.caster(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.8f);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof LivingEntity living))
            return;

        // Always prevent self-damage for Spark (it's not meant to be a fireball)
        if (living.equals(context.caster()))
            return;

        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);
        if (!friendlyFire && living instanceof org.bukkit.entity.Player)
            return;

        double damage = spellConfig.getDouble("values.damage", 6.0);
        double knockbackStrength = spellConfig.getDouble("values.knockback-strength", 0.45);

        living.damage(damage, context.caster());
        Vector direction = living.getLocation().toVector().subtract(projectile.getLocation().toVector()).normalize();
        living.setVelocity(direction.multiply(knockbackStrength));

        // Lightning rod-like effects
        context.fx().spawnParticles(living.getLocation(), Particle.ELECTRIC_SPARK, 25, 0.5, 1.0, 0.5, 0.2);
        context.fx().spawnParticles(living.getLocation(), Particle.CRIT, 10, 0.3, 0.8, 0.3, 0.1);
        context.fx().playSound(living.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.2f);
    }

    private class ParticleTrail extends BukkitRunnable {
        private final SpellContext context;
        private final Projectile projectile;
        private final int particleCount;
        private final int maxSteps;
        private final int burstInterval;
        private final java.util.Deque<Location> history = new java.util.ArrayDeque<>();
        private int ticks = 0;

        ParticleTrail(SpellContext context, Projectile projectile, int particleCount, int maxSteps, int burstInterval) {
            this.context = context;
            this.projectile = projectile;
            this.particleCount = particleCount;
            this.maxSteps = maxSteps;
            this.burstInterval = burstInterval;
        }

        @Override
        public void run() {
            if (!projectile.isValid() || projectile.isDead()) {
                cancel();
                return;
            }
            if (ticks++ > 20 * 6) {
                cancel();
                return;
            }

            history.addFirst(projectile.getLocation().clone());
            if (history.size() > maxSteps)
                history.removeLast();

            // Lightning rod trail - electric sparks and energy
            for (Location loc : history) {
                context.fx().spawnParticles(loc, Particle.ELECTRIC_SPARK, particleCount, 0.06, 0.06, 0.06, 0.015);
                context.fx().spawnParticles(loc, Particle.ENCHANT, 2, 0.03, 0.03, 0.03, 0.005);
            }

            if (burstInterval > 0 && ticks % burstInterval == 0) {
                context.fx().spawnParticles(projectile.getLocation(), Particle.FLASH, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }
}
