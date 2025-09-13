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
import org.jetbrains.annotations.NotNull;

/**
 * A spell that launches a standard fireball projectile.
 */
public class Fireball extends ProjectileSpell<org.bukkit.entity.Fireball> {

    /**
     * The builder for the Fireball spell.
     */
    public static class Builder extends ProjectileSpell.Builder<org.bukkit.entity.Fireball> {
        /**
         * Creates a new builder for the Fireball spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api, org.bukkit.entity.Fireball.class);
            this.name = "Fireball";
            this.description = "Launches a standard fireball.";
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

    private static final long TASK_TIMER_DELAY = 0L;
    private static final long TASK_TIMER_PERIOD = 1L;
    private static final float LAUNCH_SOUND_VOLUME = 1.0f;
    private static final float LAUNCH_SOUND_PITCH = 1.0f;
    private static final int HIT_PARTICLE_COUNT = 30;
    private static final double HIT_PARTICLE_OFFSET = 0.5;
    private static final double HIT_PARTICLE_SPEED = 0.1;
    private static final float HIT_SOUND_VOLUME = 1.0f;
    private static final float HIT_SOUND_PITCH = 1.0f;
    private static final double DAMAGE_RADIUS = 4.0;
    private static final double MAX_DAMAGE = 20.0;
    private static final double MIN_DAMAGE = 1.0;

    /**
     * Configuration for the Fireball spell.
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

    private Fireball(Builder builder) {
        super(builder);
        this.config = new Config(3.0, true, 4, 2, 40, true);
    }

    @Override
    public String key() {
        return "fireball";
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
                    // Create standard fire trail with magma blocks
                    ProjectileTrail.TrailConfig trailConfig = ProjectileTrail.TrailConfig.builder()
                            .trailLength(config.trailLength)
                            .particleCount(config.particleCount)
                            .blockLifetimeTicks(config.blockLifetimeTicks)
                            .trailMaterial(Material.MAGMA_BLOCK)
                            .particle(Particle.FLAME)
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
            hitLoc.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, HIT_PARTICLE_COUNT, HIT_PARTICLE_OFFSET, HIT_PARTICLE_OFFSET, HIT_PARTICLE_OFFSET, HIT_PARTICLE_SPEED);
            hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, HIT_SOUND_VOLUME, HIT_SOUND_PITCH);

            for (var entity : hitLoc.getWorld().getNearbyEntities(hitLoc, DAMAGE_RADIUS, DAMAGE_RADIUS, DAMAGE_RADIUS)) {
                if (entity instanceof LivingEntity living && !living.equals(context.caster())) {
                    double distance = living.getLocation().distance(hitLoc);
                    double damage = MAX_DAMAGE * (1.0 - distance / DAMAGE_RADIUS); // Max 10 hearts
                    if (damage > 0) {
                        living.damage(Math.max(damage, MIN_DAMAGE), context.caster());
                    }
                }
            }
            event.setCancelled(true); // Cancel the vanilla explosion
        }
        
        // Self-explosion effect at caster location
        Player caster = context.caster();
        Location casterLoc = caster.getLocation();
        casterLoc.getWorld().createExplosion(casterLoc, 2.0f, false, false); // Small explosion at caster
        casterLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, casterLoc, 1, 0, 0, 0, 0);
        casterLoc.getWorld().spawnParticle(Particle.FLAME, casterLoc, 30, 1.0, 1.0, 1.0, 0.1);
        casterLoc.getWorld().playSound(casterLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
    }

}