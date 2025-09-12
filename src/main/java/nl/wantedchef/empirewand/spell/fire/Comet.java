package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.common.visual.ProjectileTrail;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * A spell that launches a fiery projectile that explodes on impact.
 */
public class Comet extends ProjectileSpell<Fireball> {

    /**
     * The builder for the Comet spell.
     */
    public static class Builder extends ProjectileSpell.Builder<Fireball> {
        /**
         * Creates a new builder for the Comet spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api, Fireball.class);
            this.name = "Comet";
            this.description = "Launches a fiery projectile that explodes on impact.";
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

    private static final long TASK_TIMER_DELAY = 0L;
    private static final long TASK_TIMER_PERIOD = 1L;
    private static final float LAUNCH_SOUND_VOLUME = 1.0f;
    private static final float LAUNCH_SOUND_PITCH = 1.0f;

    /**
     * Configuration for the Comet spell.
     *
     * @param yield The explosion yield/power
     * @param trailLength Length of the fire trail
     * @param particleCount Number of particles in the trail
     * @param blockLifetimeTicks How long trail blocks remain
     * @param damage Direct damage to target entity
     * @param friendlyFire Whether to damage friendly players
     */
    private record Config(float yield, int trailLength, int particleCount,
                         int blockLifetimeTicks, double damage, boolean friendlyFire) {
    }

    private Config config;

    private Comet(Builder builder) {
        super(builder);
        this.config = new Config(2.5f, 5, 3, 35, 7.0, false);
    }

    @Override
    public String key() {
        return "comet";
    }

    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        super.loadConfig(spellConfig);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);
        this.config = new Config(
                (float) spellConfig.getDouble("values.yield", config.yield),
                spellConfig.getInt("values.trail_length", config.trailLength),
                spellConfig.getInt("values.particle_count", config.particleCount),
                spellConfig.getInt("values.block_lifetime_ticks", config.blockLifetimeTicks),
                spellConfig.getDouble("values.damage", config.damage),
                friendlyFire
        );
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

        Fireball fireball = launchLocation.getWorld().spawn(launchLocation, Fireball.class, fb -> {
            fb.setShooter(caster);
            fb.setYield(config.yield);
            fb.setIsIncendiary(false);
            fb.setDirection(direction);
            fb.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            fb.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                    caster.getUniqueId().toString());
        });

        // Create fiery trail with netherrack blocks and enhanced particles
        ProjectileTrail.TrailConfig trailConfig = ProjectileTrail.TrailConfig.builder()
                .trailLength(config.trailLength)
                .particleCount(config.particleCount)
                .blockLifetimeTicks(config.blockLifetimeTicks)
                .trailMaterial(Material.NETHERRACK)
                .particle(Particle.FLAME)
                .particleMultiplier(2) // Enhanced particle count for fiery effect
                .particleOffset(0.15) // Enhanced particle spread
                .blockReplacementCondition(block -> block.getType().isAir() || block.getType() == Material.SNOW)
                .build();
        context.plugin().getTaskManager().runTaskTimer(
            new ProjectileTrail(fireball, trailConfig),
            TASK_TIMER_DELAY,
            TASK_TIMER_PERIOD
        );
        context.fx().playSound(caster, Sound.ITEM_FIRECHARGE_USE, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        context.fx().impact(projectile.getLocation());

        if (event.getHitEntity() instanceof LivingEntity target) {
            String ownerUUID = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER,
                    PersistentDataType.STRING);

            if (ownerUUID != null) {
                Player caster = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                if (caster != null && !(target.equals(caster) && !config.friendlyFire)) {
                    target.damage(config.damage, caster);
                }
            }
        }

        // Always cleanup the projectile after impact to avoid lingering entities
        if (projectile.isValid() && !projectile.isDead()) {
            projectile.remove();
        }
    }

}
