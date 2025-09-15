package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.util.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A dark spell that creates a pulling void circle that launches enemies into the air.
 * <p>
 * This spell creates a dark energy circle around the caster that pulls nearby entities
 * toward the center, then launches them into the air with significant force. The spell
 * also applies wither effects to affected entities and includes visual and audio feedback.
 * <p>
 * <strong>Effects:</strong>
 * <ul>
 *   <li>Creates a void circle that pulls entities toward its center</li>
 *   <li>Launches pulled entities into the air after a delay</li>
 *   <li>Applies wither effect to affected entities</li>
 *   <li>Deals detonation damage when entities are launched</li>
 *   <li>Provides radial knockback effect</li>
 *   <li>Visual effects with smoke and soul fire particles</li>
 *   <li>Audio feedback with dragon sounds</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell darkCircle = new DarkCircle.Builder(api)
 *     .name("Dark Circle")
 *     .description("Creates a pulling void circle that launches enemies into the air.")
 *     .cooldown(Duration.ofSeconds(10))
 *     .build();
 * }</pre>
 *
 * @author WantedChef
 * @since 1.0.0
 */
public class DarkCircle extends Spell<Void> {

    /**
     * Configuration record containing all DarkCircle spell settings.
     * <p>
     * This record holds all configuration values loaded from the spell configuration.
     * Using a record provides immutable configuration access and better performance
     * compared to repeated map lookups.
     *
     * @param radius           the radius of the void circle effect
     * @param pullStrength     the strength of the pull effect
     * @param launchPower      the power of the launch effect
     * @param pullDuration     the duration of the pull effect in ticks
     * @param launchDelay      the delay between pull and launch in ticks
     * @param detonationDamage the damage dealt when entities are launched
     * @param radialKnockback  the radial knockback strength
     * @param witherDuration   the duration of the wither effect in ticks
     * @param friendlyFire     whether to affect the caster
     */
    private record Config(
        double radius,
        double pullStrength,
        double launchPower,
        int pullDuration,
        int launchDelay,
        double detonationDamage,
        double radialKnockback,
        int witherDuration,
        boolean friendlyFire
    ) {}

    /**
     * Default configuration values.
     */
    private static final double DEFAULT_RADIUS = 10.0;
    private static final double DEFAULT_PULL_STRENGTH = 0.4;
    private static final double DEFAULT_LAUNCH_POWER = 2.2;
    private static final int DEFAULT_PULL_DURATION = 30;
    private static final int DEFAULT_LAUNCH_DELAY = 10;
    private static final double DEFAULT_DETONATION_DAMAGE = 4.0;
    private static final double DEFAULT_RADIAL_KNOCKBACK = 1.0;
    private static final int DEFAULT_WITHER_DURATION = 60;
    private static final boolean DEFAULT_FRIENDLY_FIRE = false;

    /**
     * Current spell configuration.
     */
    private Config config = new Config(
        DEFAULT_RADIUS,
        DEFAULT_PULL_STRENGTH,
        DEFAULT_LAUNCH_POWER,
        DEFAULT_PULL_DURATION,
        DEFAULT_LAUNCH_DELAY,
        DEFAULT_DETONATION_DAMAGE,
        DEFAULT_RADIAL_KNOCKBACK,
        DEFAULT_WITHER_DURATION,
        DEFAULT_FRIENDLY_FIRE
    );

    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new DarkCircle spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Dark Circle";
            this.description = "Creates a pulling void circle that launches enemies into the air.";
            this.cooldown = Duration.ofSeconds(10);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds and returns a new DarkCircle spell instance.
         *
         * @return the constructed DarkCircle spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new DarkCircle(this);
        }
    }

    /**
     * Constructs a new DarkCircle spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private DarkCircle(Builder builder) {
        super(builder);
    }

    /**
     * Loads configuration values from the provided ReadableConfig.
     * <p>
     * This method is called during spell initialization to load spell-specific
     * configuration values from the configuration file. It creates an immutable
     * Config record for efficient access during spell execution.
     *
     * @param spellConfig the configuration section for this spell
     * @throws NullPointerException if spellConfig is null
     */
    @Override
    public void loadConfig(@NotNull ReadableConfig spellConfig) {
        Objects.requireNonNull(spellConfig, "SpellConfig cannot be null");
        super.loadConfig(spellConfig);
        
        this.config = new Config(
            SpellUtils.getConfigDouble(spellConfig, "values.radius", DEFAULT_RADIUS),
            SpellUtils.getConfigDouble(spellConfig, "values.pull-strength", DEFAULT_PULL_STRENGTH),
            SpellUtils.getConfigDouble(spellConfig, "values.launch-power", DEFAULT_LAUNCH_POWER),
            SpellUtils.getConfigInt(spellConfig, "values.pull-duration-ticks", DEFAULT_PULL_DURATION),
            SpellUtils.getConfigInt(spellConfig, "values.launch-delay-ticks", DEFAULT_LAUNCH_DELAY),
            SpellUtils.getConfigDouble(spellConfig, "values.detonation-damage", DEFAULT_DETONATION_DAMAGE),
            SpellUtils.getConfigDouble(spellConfig, "values.radial-knockback", DEFAULT_RADIAL_KNOCKBACK),
            SpellUtils.getConfigInt(spellConfig, "values.wither-duration-ticks", DEFAULT_WITHER_DURATION),
            SpellUtils.getConfigBoolean(spellConfig, "values.friendly-fire", DEFAULT_FRIENDLY_FIRE)
        );
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "dark-circle"
     */
    @Override
    @NotNull
    public String key() {
        return "dark-circle";
    }

    /**
     * Returns the prerequisites for casting this spell.
     * <p>
     * Currently, this spell has no prerequisites beyond standard casting requirements.
     *
     * @return a no-op prerequisite
     */
    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Executes the dark circle spell logic.
     * <p>
     * This method creates a void circle around the caster that pulls nearby entities
     * toward the center, then launches them into the air with significant force.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        Location center = player.getLocation();
        var world = center.getWorld();
        if (world == null) {
            return null;
        }

        // Use pre-loaded configuration for better performance
        var radius = config.radius;
        var pullStrength = config.pullStrength;
        var launchPower = config.launchPower;
        var pullDuration = config.pullDuration;
        var launchDelay = config.launchDelay;
        var detonationDamage = config.detonationDamage;
        var radialKnockback = config.radialKnockback;
        var witherDuration = config.witherDuration;
        var friendlyFire = config.friendlyFire;

        var entities = world.getNearbyEntities(center, radius, radius, radius);
        for (var entity : entities) {
            if (entity instanceof LivingEntity living) {
                if (living.equals(player) && !friendlyFire) {
                    continue;
                }
                if (living.isDead() || !living.isValid()) {
                    continue;
                }

                startPullEffect(living, center, pullStrength, pullDuration, launchPower, launchDelay,
                        detonationDamage, radialKnockback, witherDuration, player, context);
            }
        }

        createCircleEffect(center, radius, context);
        context.fx().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        return null;
    }

    /**
     * Handles the spell effect after execution.
     * <p>
     * This spell's effects are handled asynchronously through BukkitRunnables.
     *
     * @param context the spell context
     * @param result the result of the spell execution (always null for this spell)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler
    }

    /**
     * Starts the pull effect on a target entity.
     * <p>
     * This method creates a runnable that pulls the entity toward the center of the circle
     * for a specified duration, then launches it into the air with additional effects.
     *
     * @param target the entity to apply the effect to
     * @param center the center location of the void circle
     * @param pullStrength the strength of the pull effect
     * @param pullDuration the duration of the pull effect in ticks
     * @param launchPower the power of the launch effect
     * @param launchDelay the delay between pull and launch in ticks
     * @param detonationDamage the damage dealt when launched
     * @param radialKnockback the radial knockback strength
     * @param witherDuration the duration of the wither effect in ticks
     * @param caster the player who cast the spell
     * @param context the spell context
     */
    private void startPullEffect(LivingEntity target, Location center, double pullStrength, int pullDuration,
            double launchPower, int launchDelay, double detonationDamage, double radialKnockback,
            int witherDuration, Player caster, SpellContext context) {
        context.plugin().getTaskManager().runTaskTimer(
            new BukkitRunnable() {
                private int ticks = 0;
                private final int launchTick = pullDuration + Math.max(0, launchDelay);

                @Override
                public void run() {
                    if (ticks >= launchTick + 2 || target.isDead() || !target.isValid()) {
                        this.cancel();
                        return;
                    }

                    if (ticks < pullDuration) {
                        var direction = center.toVector().subtract(target.getLocation().toVector()).normalize();
                        target.setVelocity(direction.multiply(pullStrength));
                        context.fx().spawnParticles(target.getLocation(), Particle.SMOKE, 3, 0.1, 0.1, 0.1, 0.05);
                        context.fx().spawnParticles(target.getLocation(), Particle.SOUL_FIRE_FLAME, 2, 0.2, 0.2, 0.2,
                                0.02);
                    } else if (ticks == pullDuration - 5) {
                        context.fx().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.3f);
                        context.fx().spawnParticles(target.getLocation(), Particle.FLASH, 5, 0.5, 0.5, 0.5, 0.1);
                    } else if (ticks == launchTick) {
                        target.setVelocity(new Vector(0, launchPower, 0));
                        context.fx().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                        context.fx().spawnParticles(target.getLocation(), Particle.EXPLOSION, 15, 0.5, 0.5, 0.5, 0.1);

                        if (detonationDamage > 0) {
                            target.damage(detonationDamage, caster);
                        }
                        if (radialKnockback > 0) {
                            var away = target.getLocation().toVector().subtract(center.toVector()).normalize()
                                    .multiply(radialKnockback).setY(0.15);
                            target.setVelocity(target.getVelocity().add(away));
                        }
                        if (witherDuration > 0) {
                            target.addPotionEffect(
                                    new PotionEffect(PotionEffectType.WITHER, witherDuration, 0, false, true));
                        }
                    }
                    ticks++;
                }
            },
            0L, 1L
        );
    }

    /**
     * Creates the visual circle effect around the center location.
     * <p>
     * This method spawns particles in a circle pattern around the caster to
     * provide visual feedback for the spell's area of effect.
     *
     * @param center the center location of the circle
     * @param radius the radius of the circle
     * @param context the spell context
     */
    private void createCircleEffect(Location center, double radius, SpellContext context) {
        context.plugin().getTaskManager().runTaskTimer(
            new BukkitRunnable() {
                private int ticks = 0;

                @Override
                public void run() {
                    if (ticks >= 40) {
                        this.cancel();
                        return;
                    }
                    for (int i = 0; i < 360; i += 6) {
                        double angle = Math.toRadians(i);
                        double x = center.getX() + radius * Math.cos(angle);
                        double z = center.getZ() + radius * Math.sin(angle);
                        Location particleLoc = new Location(center.getWorld(), x, center.getY(), z);
                        context.fx().spawnParticles(particleLoc, Particle.SOUL_FIRE_FLAME, 1, 0, 0, 0, 0);
                        if (ticks % 4 == 0) {
                            context.fx().spawnParticles(particleLoc, Particle.SMOKE, 1, 0, 0, 0, 0.02);
                        }
                    }
                    ticks++;
                }
            },
            0L, 2L
        );
    }
}
