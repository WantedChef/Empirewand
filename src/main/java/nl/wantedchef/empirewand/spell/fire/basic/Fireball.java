package nl.wantedchef.empirewand.spell.fire.basic;

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
import org.bukkit.Color;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A consolidated fireball spell that supports both standard and enhanced modes.
 * This spell launches a fireball projectile with configurable enhancement levels.
 */
public class Fireball extends ProjectileSpell<org.bukkit.entity.Fireball> {

    /**
     * Enhancement level enumeration for fireball variants.
     */
    public enum EnhancementLevel {
        STANDARD(1.0, "Standard fireball"),
        ENHANCED(1.5, "Enhanced fireball with increased effects");

        private final double multiplier;
        private final String description;

        EnhancementLevel(double multiplier, String description) {
            this.multiplier = multiplier;
            this.description = description;
        }

        public double getMultiplier() { return multiplier; }
        public String getDescription() { return description; }
    }

    /**
     * The builder for the consolidated Fireball spell.
     */
    public static class Builder extends ProjectileSpell.Builder<org.bukkit.entity.Fireball> {
        private EnhancementLevel enhancementLevel = EnhancementLevel.STANDARD;

        /**
         * Creates a new builder for the Fireball spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api, org.bukkit.entity.Fireball.class);
            this.name = "Fireball";
            this.description = "Launches a fireball projectile.";
            this.cooldown = java.time.Duration.ofSeconds(5);
            this.spellType = SpellType.FIRE;
            this.trailParticle = null; // Custom trail
            this.hitSound = null; // Vanilla explosion sound
        }

        /**
         * Sets the enhancement level for this fireball.
         */
        public Builder enhancementLevel(EnhancementLevel level) {
            this.enhancementLevel = level;
            updateNameAndDescription();
            return this;
        }

        private void updateNameAndDescription() {
            switch (enhancementLevel) {
                case ENHANCED:
                    this.name = "Enhanced Fireball";
                    this.description = "Launches a powerful fireball with enhanced area damage.";
                    break;
                default:
                    this.name = "Fireball";
                    this.description = "Launches a standard fireball.";
                    break;
            }
        }

        @Override
        @NotNull
        public Fireball build() { // changed return type to Fireball
            return new Fireball(this);
        }
    }

    private static final long TASK_TIMER_DELAY = 0L;
    private static final long TASK_TIMER_PERIOD = 1L;
    private static final float LAUNCH_SOUND_VOLUME = 1.0f;
    private static final float LAUNCH_SOUND_PITCH = 1.0f;

    /**
     * Configuration for the Fireball spell.
     */
    private record Config(double yield, boolean incendiary, int trailLength, int particleCount,
                         int blockLifetimeTicks, boolean blockDamage, double damageRadius,
                         double maxDamage, double minDamage) {}

    private final EnhancementLevel enhancementLevel;
    private Config config;

    private Fireball(Builder builder) {
        super(builder);
        this.enhancementLevel = builder.enhancementLevel;
        // Set base config values based on enhancement level
        double multiplier = enhancementLevel.getMultiplier();
        this.config = new Config(
                3.0 * multiplier, true,
                (int)(4 * multiplier), (int)(2 * multiplier),
                40, true,
                4.0 * multiplier, 20.0 * multiplier, 1.0);
    }

    @Override
    public String key() {
        return enhancementLevel == EnhancementLevel.ENHANCED ? "fireball-enhanced" : "fireball";
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
                spellConfig.getBoolean("flags.block-damage", config.blockDamage),
                spellConfig.getDouble("values.damage_radius", config.damageRadius),
                spellConfig.getDouble("values.max_damage", config.maxDamage),
                spellConfig.getDouble("values.min_damage", config.minDamage)
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

                    // Create spectacular fire trail with enhancement-based effects
                    if (enhancementLevel == EnhancementLevel.ENHANCED) {
                        // Enhanced fireball gets spectacular trail
                        new SpectacularFireballTrail(context, fireball).runTaskTimer(context.plugin(), 0L, 1L);
                    } else {
                        // Standard fireball uses enhanced ProjectileTrail
                        ProjectileTrail.TrailConfig trailConfig = ProjectileTrail.TrailConfig.builder()
                                .trailLength(config.trailLength)
                                .particleCount(config.particleCount)
                                .blockLifetimeTicks(config.blockLifetimeTicks)
                                .trailMaterial(Material.MAGMA_BLOCK)
                                .particle(Particle.FLAME)
                                .particleOffset(0.1)
                                .build();
                        context.plugin().getTaskManager().runTaskTimer(
                            new ProjectileTrail(fireball, trailConfig),
                            TASK_TIMER_DELAY,
                            TASK_TIMER_PERIOD
                        );
                    }
                });

        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        Location hitLoc = projectile.getLocation();

        // Always cancel the vanilla explosion to prevent caster damage
        event.setCancelled(true);

        // Create spectacular explosion effects based on enhancement level
        if (enhancementLevel == EnhancementLevel.ENHANCED) {
            // Spectacular enhanced effects
            createSpectacularFireballExplosion(context, hitLoc);
            createFireballShockwave(context, hitLoc);

            // Apply enhanced area damage
            applyAreaDamage(context, hitLoc);
            applyAreaEffects(context, hitLoc);

        } else {
            // Enhanced standard effects
            createStandardFireballExplosion(context, hitLoc);

            // Apply standard damage
            applyAreaDamage(context, hitLoc);
        }

        // Create block damage if enabled
        if (config.blockDamage) {
            hitLoc.getWorld().createExplosion(hitLoc, (float) config.yield, config.incendiary, false);
        }

        // Enhanced effects at hit location (no self-damage)
    }

    /**
     * Applies area damage to entities around the explosion point.
     */
    private void applyAreaDamage(SpellContext context, Location center) {
        for (var entity : center.getWorld().getNearbyEntities(center,
                config.damageRadius, config.damageRadius, config.damageRadius)) {
            if (entity instanceof LivingEntity living && !living.equals(context.caster())) {
                double distance = living.getLocation().distance(center);
                double damage = config.maxDamage * (1.0 - distance / config.damageRadius);
                if (damage > 0) {
                    living.damage(Math.max(damage, config.minDamage), context.caster());
                    // Enhanced version applies longer fire
                    int fireTicks = enhancementLevel == EnhancementLevel.ENHANCED ? 100 : 60;
                    living.setFireTicks(fireTicks);
                }
            }
        }
    }

    /**
     * Applies additional area effects like knockback and fire (Enhanced version only).
     */
    private void applyAreaEffects(SpellContext context, Location center) {
        if (enhancementLevel != EnhancementLevel.ENHANCED) return;

        for (var entity : center.getWorld().getNearbyEntities(center,
                config.damageRadius, config.damageRadius, config.damageRadius)) {
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
    
    private void createSpectacularFireballExplosion(SpellContext context, Location center) {
        // Massive fireball explosion with multiple layers
        context.fx().spawnParticles(center, Particle.EXPLOSION_EMITTER, 4, 1, 1, 1, 0);
        context.fx().spawnParticles(center, Particle.FLAME, 100, 1.5, 1.5, 1.5, 0.3);
        context.fx().spawnParticles(center, Particle.LAVA, 60, 1.2, 1.2, 1.2, 0.15);
        context.fx().spawnParticles(center, Particle.FIREWORK, 40, 2.0, 2.0, 2.0, 0.4);
        
        // Fiery dust clouds
        context.fx().spawnParticles(center, Particle.DUST, 50, 1.8, 1.8, 1.8, 0,
            new Particle.DustOptions(Color.fromRGB(255, 69, 0), 1.8f)); // Orange Red
        context.fx().spawnParticles(center, Particle.DUST, 30, 1.3, 1.3, 1.3, 0,
            new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.5f)); // Red
        
        // Enhanced explosion sounds
        context.fx().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        context.fx().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 1.0f);
    }
    
    private void createStandardFireballExplosion(SpellContext context, Location center) {
        // Enhanced standard fireball explosion
        context.fx().spawnParticles(center, Particle.EXPLOSION, 40, 0.8, 0.8, 0.8, 0.2);
        context.fx().spawnParticles(center, Particle.FLAME, 60, 1.0, 1.0, 1.0, 0.2);
        context.fx().spawnParticles(center, Particle.LAVA, 25, 0.8, 0.8, 0.8, 0.1);
        
        // Enhanced dust effect
        context.fx().spawnParticles(center, Particle.DUST, 30, 1.2, 1.2, 1.2, 0,
            new Particle.DustOptions(Color.fromRGB(255, 140, 0), 1.3f)); // Dark Orange
        
        // Enhanced sound
        context.fx().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 1.0f);
    }
    
    private void createFireballShockwave(SpellContext context, Location center) {
        // Enhanced fireball shockwave for enhanced version
        new BukkitRunnable() {
            int radius = 1;
            final int maxRadius = 8;

            @Override
            public void run() {
                if (radius > maxRadius || center.getWorld() == null) {
                    this.cancel();
                    return;
                }

                // Create expanding fire ring
                for (int i = 0; i < 32; i++) {
                    double angle = 2 * Math.PI * i / 32;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location ringLoc = center.clone().add(x, 0.1, z);
                    
                    context.fx().spawnParticles(ringLoc, Particle.FLAME, 3, 0.1, 0.1, 0.1, 0.02);
                    
                    // Dust ring with intensity based on distance
                    float intensity = 1.0f - (radius / (float)maxRadius);
                    context.fx().spawnParticles(ringLoc, Particle.DUST, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB((int)(255 * intensity), (int)(140 * intensity), 0), 1.2f));
                    
                    // Occasional lava bursts
                    if (i % 4 == 0) {
                        context.fx().spawnParticles(ringLoc, Particle.LAVA, 1, 0.2, 0.2, 0.2, 0.03);
                    }
                }

                // Shockwave sound
                context.fx().playSound(center, Sound.BLOCK_FIRE_AMBIENT,
                    0.4f + (radius / (float)maxRadius), 0.8f + (radius / (float)maxRadius));

                radius++;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    /**
     * Spectacular trail effect for enhanced fireball spells.
     */
    private static class SpectacularFireballTrail extends BukkitRunnable {
        private final SpellContext context;
        private final org.bukkit.entity.Fireball fireball;
        private int ticks = 0;

        SpectacularFireballTrail(SpellContext context, org.bukkit.entity.Fireball fireball) {
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

            // Spectacular fireball trail effects
            createFireballCore(location);
            createFireballAura(location);
            createFireballFragments(location);

            // Flight sounds
            if (ticks % 12 == 0) {
                context.fx().playSound(location, Sound.BLOCK_FIRE_AMBIENT, 0.3f, 1.4f);
            }

            ticks++;
        }
        
        private void createFireballCore(Location location) {
            // Main fireball trail
            context.fx().spawnParticles(location, Particle.FLAME, 6, 0.2, 0.2, 0.2, 0.05);
            context.fx().spawnParticles(location, Particle.LAVA, 3, 0.15, 0.15, 0.15, 0.02);
            
            // Fiery core
            context.fx().spawnParticles(location, Particle.DUST, 4, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(255, 69, 0), 1.3f));
        }
        
        private void createFireballAura(Location location) {
            // Rotating fireball aura
            double angle = ticks * 0.4;
            for (int i = 0; i < 3; i++) {
                double auraAngle = angle + (i * 2 * Math.PI / 3);
                double radius = 0.8 + Math.sin(ticks * 0.3) * 0.2;
                double x = Math.cos(auraAngle) * radius;
                double z = Math.sin(auraAngle) * radius;
                
                Location auraLoc = location.clone().add(x, 0, z);
                context.fx().spawnParticles(auraLoc, Particle.FLAME, 1, 0.05, 0.05, 0.05, 0.01);
                context.fx().spawnParticles(auraLoc, Particle.DUST, 1, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 140, 0), 0.8f));
            }
        }
        
        private void createFireballFragments(Location location) {
            // Fireball sparks and fragments
            if (ticks % 4 == 0) {
                context.fx().spawnParticles(location, Particle.FIREWORK, 3, 0.4, 0.4, 0.4, 0.1);
            }
            
            // Smoke trail
            if (ticks % 2 == 0) {
                context.fx().spawnParticles(location, Particle.SMOKE, 2, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }
}