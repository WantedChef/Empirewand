package nl.wantedchef.empirewand.spell.fire.advanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Inferno - Advanced fire spell that unleashes a devastating area-of-effect inferno
 *
 * This spell creates a large-radius fire damage zone around the caster for a sustained duration.
 * It deals periodic fire damage to all nearby living entities while displaying impressive
 * visual effects. The spell has a long cooldown and requires high player level to cast.
 *
 * <p>Configuration options:</p>
 * <ul>
 *   <li>radius - The radius of the inferno effect (default: 8.0 blocks)</li>
 *   <li>damage - Base damage per tick (default: 15.0, divided by 3 for actual damage)</li>
 *   <li>duration_ticks - How long the effect lasts in ticks (default: 60 ticks)</li>
 * </ul>
 *
 * @author EmpireWand Development Team
 * @version 1.0
 * @since 1.0
 */
public class Inferno extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Inferno";
            this.description = "Unleash a devastating inferno around you";
            this.cooldown = Duration.ofSeconds(45);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Inferno(this);
        }
    }

    private static final double DEFAULT_RADIUS = 8.0;
    private static final double DEFAULT_DAMAGE = 15.0;
    private static final int DEFAULT_DURATION_TICKS = 60;

    // Effect constants
    private static final double ANGLE_INCREMENT = Math.PI / 16;
    private static final int FLAME_PARTICLE_COUNT = 5;
    private static final int LAVA_PARTICLE_COUNT = 1;
    private static final double FLAME_PARTICLE_SPREAD_X = 0.2;
    private static final double FLAME_PARTICLE_SPREAD_Y = 1.0;
    private static final double FLAME_PARTICLE_SPREAD_Z = 0.2;
    private static final double FLAME_PARTICLE_SPEED = 0.05;
    private static final double LAVA_PARTICLE_SPREAD_X = 0.2;
    private static final double LAVA_PARTICLE_SPREAD_Y = 0.5;
    private static final double LAVA_PARTICLE_SPREAD_Z = 0.2;
    private static final double LAVA_PARTICLE_SPEED = 0.0;
    private static final int DAMAGE_PARTICLE_COUNT = 10;
    private static final double DAMAGE_PARTICLE_SPREAD_X = 0.3;
    private static final double DAMAGE_PARTICLE_SPREAD_Y = 0.5;
    private static final double DAMAGE_PARTICLE_SPREAD_Z = 0.3;
    private static final double DAMAGE_PARTICLE_SPEED = 0.02;
    private static final int FIRE_TICKS_DURATION = 40;
    private static final int DAMAGE_INTERVAL_TICKS = 10;
    private static final long TASK_TIMER_DELAY = 2L;
    // Configuration validation constants
    private static final double MIN_RADIUS = 1.0;
    private static final double MAX_RADIUS = 50.0;
    private static final double MIN_DAMAGE = 0.0;
    private static final double MAX_DAMAGE = 100.0;
    private static final int MIN_DURATION_TICKS = 1;
    // Optimization constants
    private static final int PARTICLE_SPAWN_INTERVAL = 4; // Only spawn particles every 4 ticks instead of every 2
    private static final double PARTICLE_REDUCTION_FACTOR = 0.75; // Reduce particle count by 25%

    /**
     * Validates spell configuration parameters and returns sanitized values.
     *
     * @param configRadius The configured radius value
     * @param configDamage The configured damage value
     * @param configDuration The configured duration value
     * @return A ValidatedConfig containing sanitized values
     */
    private ValidatedConfig validateAndSanitizeConfig(double configRadius, double configDamage, int configDuration) {
        double validRadius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, configRadius));
        double validDamage = Math.max(MIN_DAMAGE, Math.min(MAX_DAMAGE, configDamage));
        int validDuration = Math.max(MIN_DURATION_TICKS, Math.min(MAX_DURATION_TICKS, configDuration));

        return new ValidatedConfig(validRadius, validDamage, validDuration);
    }

    /**
     * Simple data class to hold validated configuration values
     */
    private static class ValidatedConfig {
        final double radius;
        final double damage;
        final int duration;

        ValidatedConfig(double radius, double damage, int duration) {
            this.radius = radius;
            this.damage = damage;
            this.duration = duration;
        }
    }

    private Inferno(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "inferno";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(30);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        if (player == null) {
            return null;
        }

        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        
        // Validate and sanitize configuration values
        ValidatedConfig config = validateAndSanitizeConfig(radius, damage, duration);
        
        Location center = player.getLocation();
        if (center == null || center.getWorld() == null) {
            return null; // Cannot cast spell if location is invalid
        }
        
        // Create inferno effect
        try {
            context.fx().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
            player.sendMessage("§c§lINFERNO UNLEASHED!");
        } catch (Exception e) {
            // Log error but continue with spell execution
            context.plugin().getLogger().warning("Failed to play sound effect for Inferno spell: " + e.getMessage());
        }
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= config.duration) {
                    cancel();
                    return;
                }
                
                // Fire ring effect - optimized to reduce server load
                if (ticks % PARTICLE_SPAWN_INTERVAL == 0) {
                    try {
                        // Calculate reduced particle count for performance
                        int effectiveFlameCount = (int) Math.max(1, FLAME_PARTICLE_COUNT * PARTICLE_REDUCTION_FACTOR);
                        int effectiveLavaCount = (int) Math.max(1, LAVA_PARTICLE_COUNT * PARTICLE_REDUCTION_FACTOR);
                        
                        // Spawn particles in a more spread out pattern
                        for (double angle = 0; angle < Math.PI * 2; angle += ANGLE_INCREMENT * 1.5) { // Reduced density
                            double x = Math.cos(angle) * config.radius;
                            double z = Math.sin(angle) * config.radius;
                            Location loc = center.clone().add(x, 0, z);
                            
                            loc.getWorld().spawnParticle(Particle.FLAME, loc, effectiveFlameCount, FLAME_PARTICLE_SPREAD_X, FLAME_PARTICLE_SPREAD_Y, FLAME_PARTICLE_SPREAD_Z, FLAME_PARTICLE_SPEED);
                            loc.getWorld().spawnParticle(Particle.LAVA, loc, effectiveLavaCount, LAVA_PARTICLE_SPREAD_X, LAVA_PARTICLE_SPREAD_Y, LAVA_PARTICLE_SPREAD_Z, LAVA_PARTICLE_SPEED);
                        }
                    } catch (Exception e) {
                        context.plugin().getLogger().warning("Failed to spawn particles for Inferno spell: " + e.getMessage());
                    }
                }
                
                // Damage entities
                if (ticks % DAMAGE_INTERVAL_TICKS == 0) {
                    try {
                        for (var entity : center.getWorld().getNearbyEntities(center, config.radius, config.radius, config.radius)) {
                            if (entity instanceof LivingEntity living && !living.equals(player) && !living.isDead()) {
                                living.damage(config.damage / 3, player);
                                living.setFireTicks(FIRE_TICKS_DURATION);
                                living.getWorld().spawnParticle(Particle.FLAME, living.getLocation(), DAMAGE_PARTICLE_COUNT, DAMAGE_PARTICLE_SPREAD_X, DAMAGE_PARTICLE_SPREAD_Y, DAMAGE_PARTICLE_SPREAD_Z, DAMAGE_PARTICLE_SPEED);
                            }
                        }
                    } catch (Exception e) {
                        context.plugin().getLogger().warning("Failed to damage entities for Inferno spell: " + e.getMessage());
                    }
                }
                
                ticks += 2;
            }
        }.runTaskTimer(context.plugin(), TASK_INITIAL_DELAY, TASK_TIMER_DELAY);
        
        return player;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        // Effects are applied in executeSpell for instant spells
    }
}
