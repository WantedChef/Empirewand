package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A spell that unleashes a wave of fire in a cone in front of the caster.
 * Refactored for better performance and code quality.
 */
public class FlameWaveRefactored extends Spell<Void> {

    /**
     * The builder for the FlameWave spell.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new builder for the FlameWave spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Flame Wave";
            this.description = "Unleashes a wave of fire in a cone.";
            this.cooldown = java.time.Duration.ofSeconds(6);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new FlameWaveRefactored(this);
        }
    }

    private static final double DEFAULT_RANGE = 6.0;
    private static final double DEFAULT_CONE_ANGLE_DEGREES = 60.0;
    private static final double DEFAULT_DAMAGE = 4.0;
    private static final int DEFAULT_FIRE_TICKS = 80;
    private static final int FLAME_PARTICLE_COUNT = 10;
    private static final double FLAME_PARTICLE_OFFSET = 0.2;
    private static final double FLAME_PARTICLE_SPEED = 0.05;
    private static final int SMOKE_PARTICLE_COUNT = 5;
    private static final double SMOKE_PARTICLE_OFFSET = 0.2;
    private static final double SMOKE_PARTICLE_SPEED = 0.1;
    private static final float SOUND_VOLUME = 0.8f;
    private static final float SOUND_PITCH = 0.8f;

    private FlameWaveRefactored(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "flame-wave";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        double coneAngle = spellConfig.getDouble("values.cone-angle-degrees", DEFAULT_CONE_ANGLE_DEGREES);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int fireTicks = spellConfig.getInt("values.fire-ticks", DEFAULT_FIRE_TICKS);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        // Use streams for better performance and readability
        List<LivingEntity> targets = getEntitiesInCone(player, range, coneAngle).stream()
                .filter(target -> !target.equals(player) || friendlyFire)
                .filter(target -> !target.isDead() && target.isValid())
                .collect(Collectors.toList());

        // Apply damage and effects to all targets
        targets.forEach(target -> {
            target.damage(damage, player);
            target.setFireTicks(fireTicks);

            // Spawn particles at target location
            context.fx().spawnParticles(target.getLocation(), Particle.FLAME, FLAME_PARTICLE_COUNT,
                    FLAME_PARTICLE_OFFSET, FLAME_PARTICLE_OFFSET, FLAME_PARTICLE_OFFSET, FLAME_PARTICLE_SPEED);
            context.fx().spawnParticles(target.getLocation(), Particle.SMOKE, SMOKE_PARTICLE_COUNT,
                    SMOKE_PARTICLE_OFFSET, SMOKE_PARTICLE_OFFSET, SMOKE_PARTICLE_OFFSET, SMOKE_PARTICLE_SPEED);
        });

        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, SOUND_VOLUME, SOUND_PITCH);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    /**
     * Gets all living entities within a cone in front of the player.
     *
     * @param player    The player.
     * @param range     The range of the cone.
     * @param coneAngle The angle of the cone in degrees.
     * @return A list of living entities within the cone.
     */
    private List<LivingEntity> getEntitiesInCone(Player player, double range, double coneAngle) {
        Location playerLoc = player.getEyeLocation();
        Vector playerDir = playerLoc.getDirection().normalize();
        double coneAngleRad = Math.toRadians(coneAngle / 2.0);
        double rangeSquared = range * range;

        return player.getWorld().getNearbyEntities(playerLoc, range, range, range).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(living -> {
                    Vector toEntity = living.getEyeLocation().toVector().subtract(playerLoc.toVector());
                    return toEntity.lengthSquared() <= rangeSquared && 
                           playerDir.angle(toEntity.normalize()) < coneAngleRad;
                })
                .collect(Collectors.toList());
    }
}