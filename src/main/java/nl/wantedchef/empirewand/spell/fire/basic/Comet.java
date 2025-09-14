package nl.wantedchef.empirewand.spell.fire.basic;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * A consolidated comet spell that supports both standard and enhanced modes.
 * This spell launches a fiery projectile that explodes on impact with configurable enhancement levels.
 */
public class Comet extends ProjectileSpell<Fireball> {

    /**
     * Enhancement level enumeration for comet variants.
     */
    public enum EnhancementLevel {
        STANDARD(1.0, "Standard comet projectile", 20, Sound.ITEM_FIRECHARGE_USE),
        ENHANCED(1.5, "Enhanced comet with improved effects", 8, Sound.ENTITY_BLAZE_SHOOT);

        private final double multiplier;
        private final String description;
        private final int cooldownSeconds;
        private final Sound launchSound;

        EnhancementLevel(double multiplier, String description, int cooldownSeconds, Sound launchSound) {
            this.multiplier = multiplier;
            this.description = description;
            this.cooldownSeconds = cooldownSeconds;
            this.launchSound = launchSound;
        }

        public double getMultiplier() { return multiplier; }
        public String getDescription() { return description; }
        public int getCooldownSeconds() { return cooldownSeconds; }
        public Sound getLaunchSound() { return launchSound; }
    }

    /**
     * The builder for the consolidated Comet spell.
     */
    public static class Builder extends ProjectileSpell.Builder<Fireball> {
        private EnhancementLevel enhancementLevel = EnhancementLevel.STANDARD;

        public Builder(EmpireWandAPI api) {
            super(api, Fireball.class);
            this.name = "Comet";
            this.description = "Launches a fiery projectile that explodes on impact";
            this.cooldown = java.time.Duration.ofSeconds(20);
            this.spellType = SpellType.FIRE;
            this.hitSound = null; // Explosion handles sound
            this.trailParticle = null; // Custom trail
        }

        public Builder enhancementLevel(EnhancementLevel level) {
            this.enhancementLevel = level;
            this.name = level == EnhancementLevel.ENHANCED ? "Enhanced Comet" : "Comet";
            this.description = level.getDescription();
            this.cooldown = java.time.Duration.ofSeconds(level.getCooldownSeconds());
            return this;
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
     */
    private record Config(float yield, int trailLength, int particleCount,
                         int blockLifetimeTicks, double damage, boolean friendlyFire,
                         EnhancementLevel enhancementLevel) {
    }

    private Config config;
    private final EnhancementLevel enhancementLevel;

    private Comet(Builder builder) {
        super(builder);
        this.enhancementLevel = builder.enhancementLevel;
        this.config = new Config(
            (float) (2.5f * enhancementLevel.getMultiplier()),
            enhancementLevel == EnhancementLevel.ENHANCED ? 8 : 5,
            enhancementLevel == EnhancementLevel.ENHANCED ? 5 : 3,
            35,
            7.0 * enhancementLevel.getMultiplier(),
            false,
            enhancementLevel
        );
    }

    @Override
    public String key() {
        return enhancementLevel == EnhancementLevel.ENHANCED ? "comet-enhanced" : "comet";
    }

    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        super.loadConfig(spellConfig);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        double multiplier = enhancementLevel.getMultiplier();
        this.config = new Config(
                (float) (spellConfig.getDouble("values.yield", 2.5f) * multiplier),
                spellConfig.getInt("values.trail_length", enhancementLevel == EnhancementLevel.ENHANCED ? 8 : 5),
                spellConfig.getInt("values.particle_count", enhancementLevel == EnhancementLevel.ENHANCED ? 5 : 3),
                spellConfig.getInt("values.block_lifetime_ticks", 35),
                spellConfig.getDouble("values.damage", 7.0) * multiplier,
                friendlyFire,
                enhancementLevel
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
            fb.setIsIncendiary(enhancementLevel == EnhancementLevel.ENHANCED);
            fb.setDirection(direction);
            fb.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            fb.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                    caster.getUniqueId().toString());
        });

        if (enhancementLevel == EnhancementLevel.ENHANCED) {
            // Enhanced version uses custom trail effect
            new CometTrailEffect(context, fireball).runTaskTimer(context.plugin(), 0L, 1L);
            // Enhanced launch sounds
            context.fx().playSound(caster, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.6f);
            context.fx().playSound(caster, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.96f, 0.72f);
        } else {
            // Standard version uses ProjectileTrail
            ProjectileTrail.TrailConfig trailConfig = ProjectileTrail.TrailConfig.builder()
                    .trailLength(config.trailLength)
                    .particleCount(config.particleCount)
                    .blockLifetimeTicks(config.blockLifetimeTicks)
                    .trailMaterial(Material.NETHERRACK)
                    .particle(Particle.FLAME)
                    .particleMultiplier(2)
                    .particleOffset(0.15)
                    .blockReplacementCondition(block -> block.getType().isAir() || block.getType() == Material.SNOW)
                    .build();
            context.plugin().getTaskManager().runTaskTimer(
                new ProjectileTrail(fireball, trailConfig),
                TASK_TIMER_DELAY,
                TASK_TIMER_PERIOD
            );
            context.fx().playSound(caster, Sound.ITEM_FIRECHARGE_USE, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);
        }
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {

        if (enhancementLevel == EnhancementLevel.ENHANCED) {
            handleEnhancedHit(context, projectile, event);
        } else {
            handleStandardHit(context, projectile, event);
        }

        // Always cleanup the projectile after impact
        if (projectile.isValid() && !projectile.isDead()) {
            projectile.remove();
        }
    }

    private void handleStandardHit(@NotNull SpellContext context, @NotNull Projectile projectile,
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
    }

    private void handleEnhancedHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        Location hitLoc = projectile.getLocation();

        // Enhanced hit sounds
        context.fx().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, hitVolume, hitPitch);
        context.fx().playSound(hitLoc, Sound.BLOCK_FIRE_EXTINGUISH, hitVolume * 0.8f, hitPitch * 1.3f);

        // Enhanced particle effects
        hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 2, 0, 0, 0, 0);
        hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, 60, 1.0, 1.0, 1.0, 0.1);
        hitLoc.getWorld().spawnParticle(Particle.LAVA, hitLoc, 30, 0.8, 0.8, 0.8, 0.05);

        // Create shockwave effect
        createShockwave(context, hitLoc);

        // Handle entity damage
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

                context.fx().playSound(center, Sound.BLOCK_LAVA_POP,
                    0.3f + (radius / (float)maxRadius), 0.8f + (radius / (float)maxRadius));

                radius++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * Enhanced trail effect for comet spells.
     */
    private static class CometTrailEffect extends BukkitRunnable {
        private final SpellContext context;
        private final Fireball fireball;
        private int ticks = 0;

        private static final int TRAIL_PARTICLE_COUNT = 5;
        private static final double TRAIL_PARTICLE_OFFSET = 0.1;
        private static final double TRAIL_PARTICLE_SPEED = 0.01;

        CometTrailEffect(SpellContext context, Fireball fireball) {
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