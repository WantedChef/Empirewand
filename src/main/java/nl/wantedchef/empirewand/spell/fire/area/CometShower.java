package nl.wantedchef.empirewand.spell.fire.area;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * A spell that rains down comets on a target area.
 */
public class CometShower extends Spell<Void> {

    /**
     * The configuration for the CometShower spell.
     *
     * @param cometCount     The number of comets to spawn.
     * @param radius         The radius of the area where comets will fall.
     * @param explosionYield The power of the explosion of each comet.
     * @param delayTicks     The delay in ticks between each comet spawn.
     */
    public record Config(int cometCount, double radius, double explosionYield, int delayTicks) {
    }

    /**
     * The builder for the CometShower spell.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new builder for the CometShower spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Comet Shower";
            this.description = "Rains down comets on a target area.";
            this.cooldown = java.time.Duration.ofSeconds(30);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new CometShower(this);
        }
    }

    private static final int DEFAULT_COMET_COUNT = 5;
    private static final double DEFAULT_RADIUS = 8.0;
    private static final double DEFAULT_YIELD = 2.6;
    private static final int DEFAULT_DELAY_TICKS = 6;
    private static final double RAY_TRACE_DISTANCE = 25.0;
    private static final int TARGET_DISTANCE_FALLBACK = 20;
    private static final int SPAWN_HEIGHT_OFFSET = 12;
    private static final Vector COMET_DIRECTION = new Vector(0, -1, 0);
    private static final int FLAME_PARTICLE_COUNT = 10;
    private static final double FLAME_PARTICLE_OFFSET = 0.2;
    private static final double FLAME_PARTICLE_SPEED = 0.1;
    private static final int LAVA_PARTICLE_COUNT = 5;
    private static final double LAVA_PARTICLE_OFFSET = 0.1;
    private static final double LAVA_PARTICLE_SPEED = 0.05;

    private Config config;

    private CometShower(Builder builder) {
        super(builder);
        this.config = new Config(DEFAULT_COMET_COUNT, DEFAULT_RADIUS, DEFAULT_YIELD, DEFAULT_DELAY_TICKS);
    }

    @Override
    public void loadConfig(@NotNull nl.wantedchef.empirewand.core.config.ReadableConfig spellConfig) {
        super.loadConfig(spellConfig);
        this.config = new Config(
                spellConfig.getInt("values.comet-count", DEFAULT_COMET_COUNT),
                spellConfig.getDouble("values.radius", DEFAULT_RADIUS),
                spellConfig.getDouble("values.yield", DEFAULT_YIELD),
                spellConfig.getInt("values.delay-ticks", DEFAULT_DELAY_TICKS));
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
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();
        Location targetLocation = findTargetLocation(caster);

        new BukkitRunnable() {
            private int launched = 0;

            @Override
            public void run() {
                if (launched >= config.cometCount) {
                    this.cancel();
                    return;
                }
                launchComet(caster, targetLocation, config.radius, config.explosionYield, context);
                launched++;
            }
        }.runTaskTimer(context.plugin(), 0L, config.delayTicks);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    /**
     * Finds the target location for the comet shower.
     *
     * @param caster The player casting the spell.
     * @return The target location.
     */
    private Location findTargetLocation(Player caster) {
        RayTraceResult rayTrace = caster.getWorld().rayTraceBlocks(caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(), RAY_TRACE_DISTANCE);
        if (rayTrace != null && rayTrace.getHitPosition() != null) {
            return rayTrace.getHitPosition().toLocation(caster.getWorld());
        }
        return caster.getEyeLocation().add(caster.getEyeLocation().getDirection().multiply(TARGET_DISTANCE_FALLBACK));
    }

    /**
     * Launches a single comet.
     *
     * @param caster         The player who cast the spell.
     * @param center         The center of the target area.
     * @param radius         The radius of the target area.
     * @param explosionYield The power of the explosion.
     * @param context        The spell context.
     */
    private void launchComet(Player caster, Location center, double radius, double explosionYield,
            SpellContext context) {
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        Location spawnLoc = new Location(center.getWorld(), x, center.getY() + SPAWN_HEIGHT_OFFSET, z);

        caster.getWorld().spawn(spawnLoc, LargeFireball.class, c -> {
            c.setYield((float) explosionYield);
            c.setIsIncendiary(false);
            c.setDirection(COMET_DIRECTION);
            c.setShooter(caster);
        });

        context.fx().spawnParticles(spawnLoc, Particle.FLAME, FLAME_PARTICLE_COUNT, FLAME_PARTICLE_OFFSET, FLAME_PARTICLE_OFFSET, FLAME_PARTICLE_OFFSET, FLAME_PARTICLE_SPEED);
        context.fx().spawnParticles(spawnLoc, Particle.LAVA, LAVA_PARTICLE_COUNT, LAVA_PARTICLE_OFFSET, LAVA_PARTICLE_OFFSET, LAVA_PARTICLE_OFFSET, LAVA_PARTICLE_SPEED);
    }
}
