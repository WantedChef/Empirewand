package nl.wantedchef.empirewand.spell.fire.advanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Inferno - Creates a devastating area-of-effect fire damage zone around the caster.
 * Features optimized particle effects and proper resource management.
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

    // Configuration defaults
    private static final double DEFAULT_RADIUS = 8.0;
    private static final double DEFAULT_DAMAGE = 15.0;
    private static final int DEFAULT_DURATION_TICKS = 60;

    // Effect constants
    private static final double ANGLE_INCREMENT = Math.PI / 8; // Reduced for performance
    private static final int FLAME_PARTICLE_COUNT = 3; // Reduced
    private static final int LAVA_PARTICLE_COUNT = 1;
    private static final int DAMAGE_INTERVAL_TICKS = 10;
    private static final int PARTICLE_INTERVAL_TICKS = 4; // Spawn particles less frequently
    private static final int FIRE_TICKS_DURATION = 40;
    private static final int DAMAGE_PARTICLE_COUNT = 5; // Reduced

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
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        // Load configuration with validation
        double radius = Math.max(1.0, Math.min(50.0, spellConfig.getDouble("values.radius", DEFAULT_RADIUS)));
        double damage = Math.max(0.0, Math.min(100.0, spellConfig.getDouble("values.damage", DEFAULT_DAMAGE)));
        int duration = Math.max(1, Math.min(200, spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS)));

        Location center = player.getLocation();
        World world = center.getWorld();

        // Initial sound effect
        context.fx().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);

        // Create inferno effect task
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Spawn fire ring particles (optimized)
                if (ticks % PARTICLE_INTERVAL_TICKS == 0) {
                    for (double angle = 0; angle < Math.PI * 2; angle += ANGLE_INCREMENT) {
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        Location loc = center.clone().add(x, 0, z);

                        // Batch spawn particles
                        world.spawnParticle(Particle.FLAME, loc, FLAME_PARTICLE_COUNT,
                            0.2, 1.0, 0.2, 0.05);
                        world.spawnParticle(Particle.LAVA, loc, LAVA_PARTICLE_COUNT,
                            0.2, 0.5, 0.2, 0.0);
                    }
                }

                // Damage entities periodically
                if (ticks % DAMAGE_INTERVAL_TICKS == 0) {
                    for (var entity : world.getNearbyEntities(center, radius, radius, radius)) {
                        if (entity instanceof LivingEntity living && !living.equals(player) && !living.isDead()) {
                            living.damage(damage / 3, player);
                            living.setFireTicks(FIRE_TICKS_DURATION);

                            // Damage particles
                            world.spawnParticle(Particle.FLAME, living.getLocation(), DAMAGE_PARTICLE_COUNT,
                                0.3, 0.5, 0.3, 0.02);
                        }
                    }
                }

                ticks += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);

        // Register task for cleanup
        if (context.plugin() instanceof nl.wantedchef.empirewand.EmpireWandPlugin plugin) {
            plugin.getTaskManager().registerTask(task);
        }
    }
}