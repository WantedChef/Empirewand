package nl.wantedchef.empirewand.spell.fire.advanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Flamewalk - Creates a trail of fire as the player walks and damages nearby enemies.
 * Features proper resource cleanup and optimized particle effects.
 */
public class Flamewalk extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Flamewalk";
            this.description = "Leave a trail of fire that burns enemies as you walk";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Flamewalk(this);
        }
    }

    // Configuration defaults
    private static final int DEFAULT_DURATION_TICKS = 200; // 10 seconds
    private static final double DEFAULT_RADIUS = 3.0;
    private static final double DEFAULT_DAMAGE = 2.0;
    private static final int DEFAULT_FIRE_TICKS = 60;

    // Effect constants
    private static final int CHECK_INTERVAL_TICKS = 5;
    private static final int FLAME_PARTICLE_COUNT = 10;
    private static final int LAVA_PARTICLE_COUNT = 2;
    private static final double PARTICLE_SPREAD = 0.5;
    private static final double PARTICLE_SPEED = 0.02;

    private Flamewalk(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "flamewalk";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(15);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        // Load configuration
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int fireTicks = spellConfig.getInt("values.fire_ticks", DEFAULT_FIRE_TICKS);

        // Initial effects
        context.fx().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);

        // Create flamewalk task
        BukkitTask task = new BukkitRunnable() {
            private int ticks = 0;
            private final List<Block> fireBlocks = new ArrayList<>();

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cleanup();
                    cancel();
                    return;
                }

                Location loc = player.getLocation();

                // Create fire particles around player
                context.fx().spawnParticles(loc, Particle.FLAME, FLAME_PARTICLE_COUNT,
                    PARTICLE_SPREAD, 0.1, PARTICLE_SPREAD, PARTICLE_SPEED);
                context.fx().spawnParticles(loc, Particle.LAVA, LAVA_PARTICLE_COUNT,
                    PARTICLE_SPREAD, 0.1, PARTICLE_SPREAD, 0);

                // Set fire on ground where player walks
                Block ground = loc.clone().subtract(0, 1, 0).getBlock();
                Block airBlock = loc.getBlock();

                if (ground.getType().isSolid() && airBlock.getType() == Material.AIR) {
                    airBlock.setType(Material.FIRE);
                    fireBlocks.add(airBlock);

                    // Limit fire blocks to prevent excessive buildup
                    if (fireBlocks.size() > 50) {
                        Block oldFire = fireBlocks.remove(0);
                        if (oldFire.getType() == Material.FIRE) {
                            oldFire.setType(Material.AIR);
                        }
                    }
                }

                // Damage and ignite nearby enemies
                for (var entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                    if (entity instanceof LivingEntity living && !living.equals(player) && !living.isDead()) {
                        living.damage(damage, player);
                        living.setFireTicks(fireTicks);

                        // Damage particles
                        context.fx().spawnParticles(living.getLocation(), Particle.FLAME, 5,
                            0.3, 0.3, 0.3, 0.02);
                    }
                }

                ticks += CHECK_INTERVAL_TICKS;
            }

            private void cleanup() {
                // Clean up all fire blocks
                for (Block block : fireBlocks) {
                    if (block.getType() == Material.FIRE) {
                        block.setType(Material.AIR);
                    }
                }
                fireBlocks.clear();
            }
        }.runTaskTimer(context.plugin(), 0L, CHECK_INTERVAL_TICKS);

        // Register task for cleanup
        if (context.plugin() instanceof nl.wantedchef.empirewand.EmpireWandPlugin plugin) {
            plugin.getTaskManager().registerTask(task);
        }
    }
}