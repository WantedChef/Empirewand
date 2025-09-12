package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.common.visual.ProjectileTrail;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A spell that launches a fireball projectile with enhanced area-of-effect damage.
 */
public class FireballEnhanced extends ProjectileSpell<org.bukkit.entity.Fireball> {

    /**
     * The builder for the FireballEnhanced spell.
     */
    public static class Builder extends ProjectileSpell.Builder<org.bukkit.entity.Fireball> {
        /**
         * Creates a new builder for the FireballEnhanced spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api, org.bukkit.entity.Fireball.class);
            this.name = "Enhanced Fireball";
            this.description = "Launches a powerful fireball with enhanced area damage.";
            this.cooldown = java.time.Duration.ofSeconds(5);
            this.spellType = SpellType.FIRE;
            this.trailParticle = null; // Custom trail
            this.hitSound = null; // Vanilla explosion sound
        }

        @Override
        @NotNull
        public ProjectileSpell<org.bukkit.entity.Fireball> build() {
            return new FireballEnhanced(this);
        }
    }

    private static final long TASK_TIMER_DELAY = 0L;
    private static final long TASK_TIMER_PERIOD = 1L;
    private static final float LAUNCH_SOUND_VOLUME = 1.0f;
    private static final float LAUNCH_SOUND_PITCH = 1.0f;
    private static final int HIT_PARTICLE_COUNT = 50;
    private static final double HIT_PARTICLE_OFFSET = 0.7;
    private static final double HIT_PARTICLE_SPEED = 0.2;
    private static final float HIT_SOUND_VOLUME = 1.5f;
    private static final float HIT_SOUND_PITCH = 0.8f;
    private static final double DAMAGE_RADIUS = 6.0;
    private static final double MAX_DAMAGE = 15.0;
    private static final double MIN_DAMAGE = 3.0;

    /**
     * Configuration for the FireballEnhanced spell.
     *
     * @param yield The explosion yield/power
     * @param incendiary Whether the explosion causes fire
     * @param trailLength Length of the fire trail
     * @param particleCount Number of particles in the trail
     * @param blockLifetimeTicks How long trail blocks remain
     * @param blockDamage Whether the explosion damages blocks
     */
    private record Config(double yield, boolean incendiary, int trailLength, int particleCount, 
                         int blockLifetimeTicks, boolean blockDamage) {}

    private Config config;

    private FireballEnhanced(Builder builder) {
        super(builder);
        this.config = new Config(5.0, true, 6, 3, 50, true);
    }

    @Override
    public String key() {
        return "fireball-enhanced";
    }

    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        super.loadConfig(spellConfig);
        this.config = new Config(
                spellConfig.getDouble("values.yield", config.yield),
                spellConfig.getBoolean("flags.incendiary", config.incendiary),
                spellConfig.getInt("values.trail_length", config.trailLength),
                spellConfig.getInt("values.particle_count", config.particleCount),
                spellConfig.getInt("values.block_lifetime_ticks", config.blockLifetimeTicks),
                spellConfig.getBoolean("flags.block-damage", config.blockDamage)
        );
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
                    fireball.setYield((float) config.yield);
                    fireball.setIsIncendiary(config.incendiary);
                    fireball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
                    fireball.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                            player.getUniqueId().toString());
                    // Create enhanced fire trail with magma blocks and increased particle effects
                    ProjectileTrail.TrailConfig trailConfig = ProjectileTrail.TrailConfig.builder()
                            .trailLength(config.trailLength)
                            .particleCount(config.particleCount)
                            .blockLifetimeTicks(config.blockLifetimeTicks)
                            .trailMaterial(Material.MAGMA_BLOCK)
                            .particle(Particle.FLAME)
                            .particleOffset(0.15) // Enhanced particle spread
                            .build();
                    context.plugin().getTaskManager().runTaskTimer(
                        new ProjectileTrail(fireball, trailConfig), 
                        TASK_TIMER_DELAY, 
                        TASK_TIMER_PERIOD
                    );
                });

        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        if (!config.blockDamage) {
            // If block damage is disabled, create a visual-only explosion
            // and manually damage entities, since the projectile's explosion is cancelled.
            Location hitLoc = projectile.getLocation();
            hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 1, 0, 0, 0, 0);
            hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, HIT_PARTICLE_COUNT * 2, HIT_PARTICLE_OFFSET, HIT_PARTICLE_OFFSET, HIT_PARTICLE_OFFSET, HIT_PARTICLE_SPEED);
            hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, HIT_SOUND_VOLUME, HIT_SOUND_PITCH);

            // Enhanced area damage
            applyAreaDamage(context, hitLoc);

            event.setCancelled(true); // Cancel the vanilla explosion
        } else {
            // Enhanced effects for normal explosion
            Location hitLoc = projectile.getLocation();
            hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 2, 0, 0, 0, 0);
            hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, HIT_PARTICLE_COUNT, HIT_PARTICLE_OFFSET, HIT_PARTICLE_OFFSET, HIT_PARTICLE_OFFSET, HIT_PARTICLE_SPEED);
            
            // Apply additional area effects
            applyAreaEffects(context, hitLoc);
        }
    }

    /**
     * Applies area damage to entities around the explosion point.
     */
    private void applyAreaDamage(SpellContext context, Location center) {
        for (var entity : center.getWorld().getNearbyEntities(center, DAMAGE_RADIUS, DAMAGE_RADIUS, DAMAGE_RADIUS)) {
            if (entity instanceof LivingEntity living && !living.equals(context.caster())) {
                double distance = living.getLocation().distance(center);
                double damage = MAX_DAMAGE * (1.0 - distance / DAMAGE_RADIUS); // Max damage at center
                if (damage > 0) {
                    living.damage(Math.max(damage, MIN_DAMAGE), context.caster());
                    living.setFireTicks(100); // 5 seconds of fire
                }
            }
        }
    }

    /**
     * Applies additional area effects like knockback and fire.
     */
    private void applyAreaEffects(SpellContext context, Location center) {
        for (var entity : center.getWorld().getNearbyEntities(center, DAMAGE_RADIUS, DAMAGE_RADIUS, DAMAGE_RADIUS)) {
            if (entity instanceof LivingEntity living && !living.equals(context.caster())) {
                // Apply knockback
                Vector knockback = living.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.2);
                knockback.setY(0.5); // Add upward force
                living.setVelocity(living.getVelocity().add(knockback));
                
                // Set on fire
                living.setFireTicks(80);
                
                // Visual effect
                context.fx().spawnParticles(living.getLocation(), Particle.FLAME, 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

}
