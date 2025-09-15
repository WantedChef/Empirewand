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
import org.bukkit.Color;
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
            // Enhanced version uses spectacular comet trail effect
            new SpectacularCometTrailEffect(context, fireball).runTaskTimer(context.plugin(), 0L, 1L);
            // Enhanced launch sounds with comet aura
            createLaunchAura(context, caster.getLocation());
            context.fx().playSound(caster, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.6f);
            context.fx().playSound(caster, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.96f, 0.72f);
            context.fx().playSound(caster, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.5f);
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
        float hitVolume = 2.0f;
        float hitPitch = 0.8f;
        context.fx().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, hitVolume, hitPitch);
        context.fx().playSound(hitLoc, Sound.BLOCK_FIRE_EXTINGUISH, hitVolume * 0.8f, hitPitch * 1.3f);

        // Spectacular enhanced particle effects
        createSpectacularExplosion(context, hitLoc);

        // Create enhanced shockwave effect
        createEnhancedShockwave(context, hitLoc);

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


    private void createLaunchAura(SpellContext context, Location center) {
        // Create spectacular launch aura for enhanced comet
        for (int i = 0; i < 8; i++) {
            final int step = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                // Expanding fire ring
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
                    double radius = (step + 1) * 0.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location auraLoc = center.clone().add(x, 0.3, z);
                    
                    context.fx().spawnParticles(auraLoc, Particle.FLAME, 3, 0.1, 0.1, 0.1, 0.05);
                    context.fx().spawnParticles(auraLoc, Particle.DUST, 2, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 69, 0), 1.2f)); // Orange Red
                }
                
                // Central comet formation
                context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.LAVA, 5, 0.3, 0.3, 0.3, 0.1);
                context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.FIREWORK, 8, 0.5, 0.5, 0.5, 0.2);
            }, step * 2L);
        }
    }
    
    private void createSpectacularExplosion(SpellContext context, Location center) {
        // Massive explosion with multiple layers
        context.fx().spawnParticles(center, Particle.EXPLOSION_EMITTER, 5, 1, 1, 1, 0);
        context.fx().spawnParticles(center, Particle.FLAME, 120, 2.0, 2.0, 2.0, 0.3);
        context.fx().spawnParticles(center, Particle.LAVA, 80, 1.5, 1.5, 1.5, 0.15);
        context.fx().spawnParticles(center, Particle.FIREWORK, 50, 2.5, 2.5, 2.5, 0.4);
        
        // Fiery dust clouds
        context.fx().spawnParticles(center, Particle.DUST, 60, 2.0, 2.0, 2.0, 0,
            new Particle.DustOptions(Color.fromRGB(255, 69, 0), 2.0f)); // Orange Red
        context.fx().spawnParticles(center, Particle.DUST, 40, 1.5, 1.5, 1.5, 0,
            new Particle.DustOptions(Color.fromRGB(255, 140, 0), 1.8f)); // Dark Orange
        
        // Comet fragments
        for (int i = 0; i < 12; i++) {
            final int fragment = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                double angle = (2 * Math.PI * fragment / 12);
                for (int r = 1; r <= 6; r++) {
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    double y = Math.sin(r * 0.3) * 0.5;
                    Location fragmentLoc = center.clone().add(x, y, z);
                    
                    context.fx().spawnParticles(fragmentLoc, Particle.FLAME, 3, 0.2, 0.2, 0.2, 0.05);
                    context.fx().spawnParticles(fragmentLoc, Particle.LAVA, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }, fragment);
        }
    }
    
    private void createEnhancedShockwave(SpellContext context, Location center) {
        new BukkitRunnable() {
            int radius = 1;
            final int maxRadius = 12;
            int wave = 0;

            @Override
            public void run() {
                if (radius > maxRadius || center.getWorld() == null) {
                    this.cancel();
                    return;
                }

                // Multiple shockwave rings
                for (int ringOffset = 0; ringOffset < 3; ringOffset++) {
                    int currentRadius = radius + ringOffset * 2;
                    if (currentRadius <= maxRadius) {
                        createShockwaveRing(context, center, currentRadius, wave);
                    }
                }

                // Enhanced shockwave sounds
                context.fx().playSound(center, Sound.BLOCK_LAVA_POP,
                    0.5f + (radius / (float)maxRadius), 0.6f + (radius / (float)maxRadius));
                context.fx().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                    0.3f, 1.8f - (radius / (float)maxRadius));

                radius += 2;
                wave++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }
    
    private void createShockwaveRing(SpellContext context, Location center, int radius, int wave) {
        // Create enhanced shockwave ring with multiple particle types
        for (int i = 0; i < 48; i++) {
            double angle = 2 * Math.PI * i / 48;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location ringLoc = center.clone().add(x, 0.2, z);
            
            // Flame ring
            context.fx().spawnParticles(ringLoc, Particle.FLAME, 2, 0.1, 0.1, 0.1, 0.02);
            
            // Dust ring with fading color intensity
            float intensity = 1.0f - (radius / 12.0f);
            context.fx().spawnParticles(ringLoc, Particle.DUST, 1, 0, 0, 0, 0,
                new Particle.DustOptions(Color.fromRGB((int)(255 * intensity), (int)(69 * intensity), 0), 1.5f));
            
            // Occasional lava bursts
            if (i % 6 == 0) {
                context.fx().spawnParticles(ringLoc, Particle.LAVA, 1, 0.2, 0.2, 0.2, 0.05);
            }
            
            // Firework sparks
            if (wave % 3 == 0 && i % 8 == 0) {
                context.fx().spawnParticles(ringLoc, Particle.FIREWORK, 2, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    /**
     * Spectacular enhanced trail effect for comet spells.
     */
    private static class SpectacularCometTrailEffect extends BukkitRunnable {
        private final SpellContext context;
        private final Fireball fireball;
        private int ticks = 0;

        private static final double TRAIL_PARTICLE_OFFSET = 0.1;
        private static final double TRAIL_PARTICLE_SPEED = 0.01;

        SpectacularCometTrailEffect(SpellContext context, Fireball fireball) {
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

            // Spectacular multi-layered comet trail
            createCometTail(location);
            createCometAura(location);
            createCometFragments(location);

            // Enhanced flight sounds
            if (ticks % 10 == 0) {
                context.fx().playSound(location, Sound.BLOCK_LAVA_AMBIENT, 0.3f, 1.2f);
                context.fx().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.2f, 0.8f);
            }

            ticks++;
        }
        
        private void createCometTail(Location location) {
            // Main spectacular flame trail
            context.fx().spawnParticles(location, Particle.FLAME, 8,
                    TRAIL_PARTICLE_OFFSET, TRAIL_PARTICLE_OFFSET, TRAIL_PARTICLE_OFFSET, TRAIL_PARTICLE_SPEED);
            
            // Lava core trail
            context.fx().spawnParticles(location, Particle.LAVA, 4,
                    TRAIL_PARTICLE_OFFSET * 0.5, TRAIL_PARTICLE_OFFSET * 0.5, TRAIL_PARTICLE_OFFSET * 0.5, TRAIL_PARTICLE_SPEED * 0.3);
            
            // Fiery dust trail
            context.fx().spawnParticles(location, Particle.DUST, 6, 
                    TRAIL_PARTICLE_OFFSET * 1.5, TRAIL_PARTICLE_OFFSET * 1.5, TRAIL_PARTICLE_OFFSET * 1.5, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 69, 0), 1.5f));
        }
        
        private void createCometAura(Location location) {
            // Rotating comet aura
            double angle = ticks * 0.3;
            for (int i = 0; i < 4; i++) {
                double auraAngle = angle + (i * Math.PI / 2);
                double radius = 1.2 + Math.sin(ticks * 0.2) * 0.3;
                double x = Math.cos(auraAngle) * radius;
                double z = Math.sin(auraAngle) * radius;
                
                Location auraLoc = location.clone().add(x, 0, z);
                context.fx().spawnParticles(auraLoc, Particle.FLAME, 2, 0.1, 0.1, 0.1, 0.02);
                context.fx().spawnParticles(auraLoc, Particle.DUST, 1, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 140, 0), 1.0f));
            }
        }
        
        private void createCometFragments(Location location) {
            // Occasional comet fragments and sparks
            if (ticks % 5 == 0) {
                context.fx().spawnParticles(location, Particle.FIREWORK, 5,
                        TRAIL_PARTICLE_OFFSET * 2, TRAIL_PARTICLE_OFFSET * 2, TRAIL_PARTICLE_OFFSET * 2, TRAIL_PARTICLE_SPEED * 1.5);
            }
            
            // Comet debris trail
            if (ticks % 3 == 0) {
                for (int i = 0; i < 3; i++) {
                    Location debrisLoc = location.clone().add(
                        (Math.random() - 0.5) * 0.8,
                        (Math.random() - 0.5) * 0.8,
                        (Math.random() - 0.5) * 0.8
                    );
                    
                    context.fx().spawnParticles(debrisLoc, Particle.LAVA, 1, 0.1, 0.1, 0.1, 0.02);
                    context.fx().spawnParticles(debrisLoc, Particle.SMOKE, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
    }
}