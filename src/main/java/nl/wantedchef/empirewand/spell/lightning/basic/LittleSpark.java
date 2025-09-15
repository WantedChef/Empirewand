package nl.wantedchef.empirewand.spell.lightning.basic;

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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class LittleSpark extends ProjectileSpell<Snowball> {

    public static class Builder extends ProjectileSpell.Builder<Snowball> {
        public Builder(EmpireWandAPI api) {
            super(api, Snowball.class);
            this.name = "Little Spark";
            this.description = "Fires a small, fast spark.";
            this.cooldown = java.time.Duration.ofMillis(500);
            this.spellType = SpellType.LIGHTNING;
            this.speed = 1.8; // Set speed to 1.8 as requested
            this.trailParticle = null; // Custom trail
            this.hitSound = Sound.ENTITY_BLAZE_HURT;
        }

        @Override
        @NotNull
        public ProjectileSpell<Snowball> build() {
            return new LittleSpark(this);
        }
    }

    private LittleSpark(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "little-spark";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        super.launchProjectile(context);
        context.fx().playSound(context.caster(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.5f);
        
        // Add launch effect to make the snowball more noticeable
        Location launchLoc = context.caster().getEyeLocation();
        launchLoc.getWorld().spawnParticle(Particle.FLAME, launchLoc, 10, 0.2, 0.2, 0.2, 0.1);
        launchLoc.getWorld().spawnParticle(Particle.CRIT, launchLoc, 5, 0.1, 0.1, 0.1, 0.05);
    }
    
    @Override
    protected void startTrailEffect(@NotNull Snowball projectile, @NotNull SpellContext context) {
        // Custom firework spark trail
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    this.cancel();
                    return;
                }
                
                try {
                    Location loc = projectile.getLocation();
                    // Create firework spark-like trail
                    projectile.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.02);
                    projectile.getWorld().spawnParticle(Particle.CRIT, loc, 2, 0.05, 0.05, 0.05, 0.01);
                } catch (Exception e) {
                    this.cancel();
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof LivingEntity living))
            return;

        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);
        if (living.equals(context.caster()) && !friendlyFire)
            return;

        double damage = spellConfig.getDouble("values.damage", 2.0);
        double knockbackStrength = spellConfig.getDouble("values.knockback-strength", 0.3);

        living.damage(damage, context.caster());
        Vector direction = living.getLocation().toVector().subtract(projectile.getLocation().toVector()).normalize();
        living.setVelocity(direction.multiply(knockbackStrength));

        context.fx().spawnParticles(living.getLocation(), Particle.ELECTRIC_SPARK, 10, 0.2, 0.2, 0.2, 0.1);
        context.fx().playSound(living.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.6f, 1.2f);
    }
}
