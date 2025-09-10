package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.entity.Fireball;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A powerful spell that fires multiple homing rockets that seek out targets
 * and explode on impact, dealing area damage.
 */
public class HomingRockets extends ProjectileSpell<Fireball> {

    public static class Builder extends ProjectileSpell.Builder<Fireball> {
        public Builder(EmpireWandAPI api) {
            super(api, Fireball.class);
            this.name = "Homing Rockets";
            this.description = "Fires multiple homing rockets that seek out targets and explode on impact.";
            this.cooldown = java.time.Duration.ofSeconds(25);
            this.spellType = SpellType.FIRE;
            this.trailParticle = Particle.FLAME;
            this.hitSound = Sound.ENTITY_GENERIC_EXPLODE;
        }

        @Override
        @NotNull
        public ProjectileSpell<Fireball> build() {
            return new HomingRockets(this);
        }
    }

    private HomingRockets(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "homing-rockets";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player player = context.caster();

        // Configuration
        int rocketCount = spellConfig.getInt("values.rocket-count", 5);
        double spread = spellConfig.getDouble("values.spread", 0.1);
        double speed = spellConfig.getDouble("values.speed", 1.5);
        double homingStrength = spellConfig.getDouble("values.homing-strength", 0.2);

        // Play launch sound
        context.fx().playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);

        // Launch multiple rockets with spread
        for (int i = 0; i < rocketCount; i++) {
            // Calculate spread direction
            Vector direction = player.getEyeLocation().getDirection().clone();
            if (i > 0) {
                // Apply spread to rockets after the first
                direction.rotateAroundX((Math.random() - 0.5) * spread);
                direction.rotateAroundY((Math.random() - 0.5) * spread);
            }
            
            direction.multiply(speed);

            Fireball fireball = player.launchProjectile(Fireball.class, direction);
            fireball.setYield(2.0f);
            fireball.setIsIncendiary(true);

            // Start homing behavior
            new HomingTask(context, fireball, homingStrength).runTaskTimer(context.plugin(), 5L, 2L);
            
            // Visual trail
            context.fx().followParticles(context.plugin(), fireball, Particle.FLAME, 5, 0.1, 0.1, 0.1, 0.01, null, 1L);
        }
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile, @NotNull ProjectileHitEvent event) {
        // The default fireball explosion handles most of the effects
        // We'll add some additional particle effects
        
        Location hitLocation = projectile.getLocation();
        World world = hitLocation.getWorld();
        if (world != null) {
            // Additional explosion effects
            world.spawnParticle(Particle.EXPLOSION_EMITTER, hitLocation, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, hitLocation, 50, 1, 1, 1, 0.1);
            
            // Sound effect
            world.playSound(hitLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        }
    }

    private static class HomingTask extends BukkitRunnable {
        private final SpellContext context;
        private final Fireball fireball;
        private final double homingStrength;
        private int ticks = 0;
        private static final int MAX_LIFETIME = 100; // 5 seconds

        public HomingTask(SpellContext context, Fireball fireball, double homingStrength) {
            this.context = context;
            this.fireball = fireball;
            this.homingStrength = homingStrength;
        }

        @Override
        public void run() {
            if (!fireball.isValid() || fireball.isDead() || ticks > MAX_LIFETIME) {
                this.cancel();
                return;
            }

            // Find nearest target
            Entity target = findNearestTarget();
            if (target != null) {
                // Calculate homing direction
                Vector toTarget = target.getLocation().toVector().subtract(fireball.getLocation().toVector()).normalize();
                Vector currentVelocity = fireball.getVelocity().normalize();
                Vector newVelocity = currentVelocity.clone().add(toTarget.clone().multiply(homingStrength)).normalize();
                
                // Apply velocity change
                fireball.setVelocity(newVelocity.multiply(currentVelocity.length()));
                
                // Visual effect showing homing
                fireball.getWorld().spawnParticle(Particle.CRIT, fireball.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
            }

            ticks++;
        }

        private Entity findNearestTarget() {
            Location fireballLocation = fireball.getLocation();
            double closestDistance = 20.0; // Max homing range
            Entity closestEntity = null;

            for (Entity entity : fireball.getNearbyEntities(15, 15, 15)) {
                // Skip non-living entities, the caster, and invalid entities
                if (!(entity instanceof LivingEntity) || entity.equals(context.caster()) || 
                    entity.isDead() || !entity.isValid()) {
                    continue;
                }

                double distance = entity.getLocation().distance(fireballLocation);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }

            return closestEntity;
        }
    }
}