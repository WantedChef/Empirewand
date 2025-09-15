package nl.wantedchef.empirewand.spell.fire.advanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.util.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A spell that rains down fiery comets on a target area.
 * Features intelligent targeting, configurable spread, and proper resource cleanup.
 */
public class CometShower extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Comet Shower";
            this.description = "Rains down fiery comets on a target area";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new CometShower(this);
        }
    }

    // Configuration defaults
    private static final int DEFAULT_COMET_COUNT = 5;
    private static final double DEFAULT_RADIUS = 8.0;
    private static final double DEFAULT_YIELD = 2.6;
    private static final int DEFAULT_DELAY_TICKS = 6;
    private static final double DEFAULT_RAY_TRACE_DISTANCE = 100.0;
    private static final int DEFAULT_FALLBACK_DISTANCE = 50;

    // Effect constants
    private static final int SPAWN_HEIGHT_OFFSET = 12;
    private static final Vector COMET_DIRECTION = new Vector(0, -1, 0);
    private static final int FLAME_PARTICLE_COUNT = 10;
    private static final int LAVA_PARTICLE_COUNT = 5;
    private static final double PARTICLE_OFFSET = 0.2;
    private static final double PARTICLE_SPEED = 0.1;

    private CometShower(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "comet-shower";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(@NotNull SpellContext context) {
        Player caster = context.caster();
        Location targetLocation = findTargetLocation(caster);

        // Load configuration using SpellUtils
        int cometCount = SpellUtils.getConfigInt(spellConfig, "values.comet-count", DEFAULT_COMET_COUNT);
        double radius = SpellUtils.getConfigDouble(spellConfig, "values.radius", DEFAULT_RADIUS);
        double explosionYield = SpellUtils.getConfigDouble(spellConfig, "values.yield", DEFAULT_YIELD);
        int delayTicks = SpellUtils.getConfigInt(spellConfig, "values.delay-ticks", DEFAULT_DELAY_TICKS);

        // Play initial sound effect using SpellUtils
        SpellUtils.playSoundAtCaster(context, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);

        // Create and schedule the comet shower task
        BukkitTask task = new BukkitRunnable() {
            private int launched = 0;

            @Override
            public void run() {
                if (launched >= cometCount || !caster.isOnline()) {
                    cancel();
                    return;
                }

                launchComet(caster, targetLocation, radius, explosionYield, context);
                launched++;
            }
        }.runTaskTimer(context.plugin(), 0L, delayTicks);

        // Register task for proper cleanup
        if (context.plugin() instanceof nl.wantedchef.empirewand.EmpireWandPlugin plugin) {
            plugin.getTaskManager().registerTask(task);
        }

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in executeSpell
    }

    /**
     * Finds the target location for the comet shower using ray tracing.
     */
    private Location findTargetLocation(Player caster) {
        double rayDist = SpellUtils.getConfigDouble(spellConfig, "values.ray-trace-distance", DEFAULT_RAY_TRACE_DISTANCE);
        int fallbackDistance = SpellUtils.getConfigInt(spellConfig, "values.fallback-distance", DEFAULT_FALLBACK_DISTANCE);

        RayTraceResult rayTrace = caster.getWorld().rayTraceBlocks(
            caster.getEyeLocation(),
            caster.getEyeLocation().getDirection(),
            rayDist
        );

        if (rayTrace != null && rayTrace.getHitPosition() != null) {
            return rayTrace.getHitPosition().toLocation(caster.getWorld());
        }

        // Fallback to a point in front of the caster
        return caster.getEyeLocation().add(
            caster.getEyeLocation().getDirection().multiply(fallbackDistance)
        );
    }

    /**
     * Launches a single comet at a random location within the target area.
     */
    private void launchComet(Player caster, Location center, double radius, double explosionYield, SpellContext context) {
        // Generate random position within the radius using SpellUtils
        Location spawnLoc = SpellUtils.getRandomLocationInRadius(center, radius, SPAWN_HEIGHT_OFFSET);

        // Spawn the comet fireball
        LargeFireball comet = spawnLoc.getWorld().spawn(spawnLoc, LargeFireball.class, fireball -> {
            fireball.setYield((float) explosionYield);
            fireball.setIsIncendiary(false);
            fireball.setDirection(COMET_DIRECTION);
            fireball.setShooter(caster);
        });

        // Create visual effects using SpellUtils
        SpellUtils.spawnParticles(context, spawnLoc, Particle.FLAME, FLAME_PARTICLE_COUNT, PARTICLE_OFFSET, PARTICLE_SPEED);
        SpellUtils.spawnParticles(context, spawnLoc, Particle.LAVA, LAVA_PARTICLE_COUNT, PARTICLE_OFFSET * 0.5, PARTICLE_SPEED * 0.5);

        // Sound effect for each comet using SpellUtils
        SpellUtils.playSoundAtLocation(context, spawnLoc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);
    }
}