package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import org.jetbrains.annotations.NotNull;

/**
 * A spell that launches a powerful comet projectile with custom sound effects.
 */
public class CometEnhanced extends ProjectileSpell<org.bukkit.entity.Fireball> {

    /**
     * The builder for the CometEnhanced spell.
     */
    public static class Builder extends ProjectileSpell.Builder<org.bukkit.entity.Fireball> {
        /**
         * Creates a new builder for the CometEnhanced spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api, org.bukkit.entity.Fireball.class);
            this.name = "Enhanced Comet";
            this.description = "Launches a powerful comet with custom sound effects.";
            this.cooldown = java.time.Duration.ofSeconds(8);
            this.spellType = SpellType.FIRE;
            this.trailParticle = Particle.FLAME;
            this.hitSound = Sound.ENTITY_DRAGON_FIREBALL_EXPLODE;
            this.hitSound = Sound.ENTITY_DRAGON_FIREBALL_EXPLODE;
            // hitVolume and hitPitch are handled by the parent class
        }

        @Override
        @NotNull
        public ProjectileSpell<org.bukkit.entity.Fireball> build() {
            return new CometEnhanced(this);
        }
    }

    private static final float LAUNCH_SOUND_VOLUME = 1.2f;
    private static final float LAUNCH_SOUND_PITCH = 0.6f;
    private static final int TRAIL_PARTICLE_COUNT = 5;
    private static final double TRAIL_PARTICLE_OFFSET = 0.1;
    private static final double TRAIL_PARTICLE_SPEED = 0.01;

    private CometEnhanced(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "comet-enhanced";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player player = context.caster();

        player.launchProjectile(org.bukkit.entity.Fireball.class,
                player.getEyeLocation().getDirection().multiply(speed), fireball -> {
                    fireball.setYield(4.0f);
                    fireball.setIsIncendiary(true);
                    new CometTrailEffect(context, fireball).runTaskTimer(context.plugin(), 0L, 1L);
                });

        // Custom launch sounds
        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);
        context.fx().playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, LAUNCH_SOUND_VOLUME * 0.8f, LAUNCH_SOUND_PITCH * 1.2f);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        // Play custom hit sounds
        Location hitLoc = projectile.getLocation();
        context.fx().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, hitVolume, hitPitch);
        context.fx().playSound(hitLoc, Sound.BLOCK_FIRE_EXTINGUISH, hitVolume * 0.8f, hitPitch * 1.3f);
        
        // Enhanced particle effects
        hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 2, 0, 0, 0, 0);
        hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, 60, 1.0, 1.0, 1.0, 0.1);
        hitLoc.getWorld().spawnParticle(Particle.LAVA, hitLoc, 30, 0.8, 0.8, 0.8, 0.05);
        
        // Create shockwave effect
        createShockwave(context, hitLoc);
    }

    private void createShockwave(SpellContext context, Location center) {
        new BukkitRunnable() {
            int radius = 1;
            final int maxRadius = 8;
            
            @Override
            public void run() {
                if (radius > maxRadius || center.getWorld() == null) {
                    this.cancel();
                    return;
                }
                
                // Create ring of fire particles
                for (int i = 0; i < 36; i++) {
                    double angle = 2 * Math.PI * i / 36;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0, z);
                    center.getWorld().spawnParticle(Particle.FLAME, particleLoc, 2, 0, 0, 0, 0);
                }
                
                // Sound effect that gets louder as it expands
                context.fx().playSound(center, Sound.BLOCK_LAVA_POP, 0.3f + (radius / (float)maxRadius), 0.8f + (radius / (float)maxRadius));
                
                radius++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * A runnable that creates a custom trail effect for the comet.
     */
    private static class CometTrailEffect extends BukkitRunnable {
        private final SpellContext context;
        private final org.bukkit.entity.Fireball fireball;
        private int ticks = 0;

        CometTrailEffect(SpellContext context, org.bukkit.entity.Fireball fireball) {
            this.context = context;
            this.fireball = fireball;
        }

        @Override
        public void run() {
            if (!fireball.isValid() || fireball.isDead()) {
                cancel();
                return;
            }

            Location location = fireball.getLocation();
            
            // Main flame trail
            context.fx().spawnParticles(location, Particle.FLAME, TRAIL_PARTICLE_COUNT,
                    TRAIL_PARTICLE_OFFSET, TRAIL_PARTICLE_OFFSET, TRAIL_PARTICLE_OFFSET, TRAIL_PARTICLE_SPEED);
            
            // Secondary particle trail
            if (ticks % 3 == 0) {
                context.fx().spawnParticles(location, Particle.LAVA, TRAIL_PARTICLE_COUNT / 2,
                        TRAIL_PARTICLE_OFFSET * 2, TRAIL_PARTICLE_OFFSET * 2, TRAIL_PARTICLE_OFFSET * 2, TRAIL_PARTICLE_SPEED * 0.5);
            }
            
            // Occasional spark effects
            if (ticks % 7 == 0) {
                context.fx().spawnParticles(location, Particle.FIREWORK, 3,
                        TRAIL_PARTICLE_OFFSET * 3, TRAIL_PARTICLE_OFFSET * 3, TRAIL_PARTICLE_OFFSET * 3, TRAIL_PARTICLE_SPEED * 2);
            }
            
            // Sound effects during flight
            if (ticks % 15 == 0) {
                context.fx().playSound(location, Sound.BLOCK_LAVA_AMBIENT, 0.2f, 1.2f);
            }
            
            ticks++;
        }
    }
}