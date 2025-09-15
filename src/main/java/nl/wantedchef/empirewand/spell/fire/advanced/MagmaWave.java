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
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * MagmaWave - Sends a devastating wave of magma forward, creating temporary magma blocks
 * and damaging entities in its path. Features proper resource cleanup and optimized effects.
 */
public class MagmaWave extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Magma Wave";
            this.description = "Send a devastating wave of magma forward";
            this.cooldown = Duration.ofSeconds(20);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new MagmaWave(this);
        }
    }

    // Configuration defaults
    private static final double DEFAULT_DAMAGE = 12.0;
    private static final double DEFAULT_RANGE = 15.0;
    private static final double DEFAULT_WIDTH = 5.0;

    // Effect constants
    private static final int WAVE_SPEED_TICKS = 2;
    private static final int PARTICLE_INTERVAL = 2;
    private static final int BLOCK_INTERVAL = 3;
    private static final int DAMAGE_INTERVAL = 2;
    private static final int FLAME_PARTICLE_COUNT = 2;
    private static final int LAVA_PARTICLE_COUNT = 1;
    private static final int CLEANUP_DELAY_TICKS = 400; // 20 seconds
    private static final int MAX_MAGMA_BLOCKS = 100; // Prevent excessive block creation

    private MagmaWave(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "magmawave";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        // Load configuration
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        double width = spellConfig.getDouble("values.width", DEFAULT_WIDTH);

        Location start = player.getLocation();
        World world = start.getWorld();
        Vector direction = start.getDirection().normalize();
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

        // Initial sound effect
        context.fx().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);

        // Track magma blocks for cleanup
        List<Location> magmaBlocks = new ArrayList<>();

        // Create wave animation task
        BukkitTask waveTask = new BukkitRunnable() {
            double distance = 3; // Start 3 blocks away from caster
            int tickCount = 0;

            @Override
            public void run() {
                if (distance >= range) {
                    cancel();
                    return;
                }

                Location waveLoc = start.clone().add(direction.clone().multiply(distance));

                // Spawn particles (optimized)
                if (tickCount % PARTICLE_INTERVAL == 0) {
                    for (double w = -width/2; w <= width/2; w += 1.0) {
                        Location particleLoc = waveLoc.clone().add(perpendicular.clone().multiply(w));

                        world.spawnParticle(Particle.LAVA, particleLoc, LAVA_PARTICLE_COUNT,
                            0.1, 0.3, 0.1, 0);
                        world.spawnParticle(Particle.FLAME, particleLoc, FLAME_PARTICLE_COUNT,
                            0.15, 0.2, 0.15, 0.01);
                    }
                }

                // Place magma blocks (less frequently)
                if (tickCount % BLOCK_INTERVAL == 0 && magmaBlocks.size() < MAX_MAGMA_BLOCKS) {
                    for (double w = -width/2; w <= width/2; w += 1.0) {
                        Location blockLoc = waveLoc.clone().add(perpendicular.clone().multiply(w));

                        if (blockLoc.getBlock().getType() == Material.AIR &&
                            blockLoc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                            blockLoc.getBlock().setType(Material.MAGMA_BLOCK);
                            magmaBlocks.add(blockLoc.clone());
                        }
                    }
                }

                // Damage entities (optimized radius)
                if (tickCount % DAMAGE_INTERVAL == 0) {
                    for (var entity : world.getNearbyEntities(waveLoc, width/3, 1.5, width/3)) {
                        if (entity instanceof LivingEntity living && !living.equals(player) && !living.isDead()) {
                            living.damage(damage, player);
                            living.setFireTicks(80);
                            living.setVelocity(living.getVelocity().add(
                                direction.clone().multiply(0.5).setY(0.3)));

                            // Damage particles
                            world.spawnParticle(Particle.FLAME, living.getLocation(), 3,
                                0.2, 0.2, 0.2, 0.02);
                        }
                    }
                }

                distance += 1;
                tickCount++;
            }
        }.runTaskTimer(context.plugin(), 0L, WAVE_SPEED_TICKS);

        // Schedule cleanup of magma blocks
        BukkitTask cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Location loc : magmaBlocks) {
                    if (loc.getBlock().getType() == Material.MAGMA_BLOCK) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
                magmaBlocks.clear();
            }
        }.runTaskLater(context.plugin(), CLEANUP_DELAY_TICKS);

        // Register tasks for cleanup
        if (context.plugin() instanceof nl.wantedchef.empirewand.EmpireWandPlugin plugin) {
            plugin.getTaskManager().registerTask(waveTask);
            plugin.getTaskManager().registerTask(cleanupTask);
        }
    }
}