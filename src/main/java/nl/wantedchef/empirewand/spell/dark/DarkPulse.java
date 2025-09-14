package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;

import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * A dark spell that launches a pulsing void skull projectile.
 * <p>
 * This spell creates a charged wither skull that pulses with dark energy as it travels
 * toward a target entity. Upon impact, it damages and applies wither and blindness effects
 * to entities within its explosion radius.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Charged wither skull projectile with homing capabilities</li>
 *   <li>Visual pulsing ring effect that grows as the projectile travels</li>
 *   <li>Area of effect damage on impact</li>
 *   <li>Wither and blindness effects on affected entities</li>
 *   <li>Knockback effect pushing entities away from impact point</li>
 *   <li>Sound and particle feedback for casting and impact</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell darkPulse = new DarkPulse.Builder(api)
 *     .name("Dark Pulse")
 *     .description("Launches a pulsing void skull.")
 *     .cooldown(Duration.ofSeconds(8))
 *     .build();
 * }</pre>
 *
 * @author WantedChef
 * @since 1.0.0
 */
public class DarkPulse extends Spell<Void> {

    /**
     * Configuration record for the DarkPulse spell.
     * <p>
     * This record holds all configurable values for the spell, providing a clean
     * and immutable way to manage spell parameters.
     */
    public record Config(
            double range,
            int witherDuration,
            int witherAmplifier,
            int blindDuration,
            double speed,
            double radius,
            double damage,
            double knockback,
            boolean friendlyFire,
            int ringParticleCount,
            double ringRadiusStep,
            int ringEveryTicks) {
    }

    /**
     * Builder for creating DarkPulse spell instances.
     * <p>
     * Provides a fluent API for configuring the dark pulse spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new DarkPulse spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Dark Pulse";
            this.description = "Launches a pulsing void skull.";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds and returns a new DarkPulse spell instance.
         *
         * @return the constructed DarkPulse spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new DarkPulse(this);
        }
    }

    private Config config;

    /**
     * Constructs a new DarkPulse spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private DarkPulse(Builder builder) {
        super(builder);
        // Config will be initialized lazily when first accessed
    }

    /**
     * Gets the spell configuration, initializing it if necessary.
     * <p>
     * This method lazily initializes the configuration from the spell config file
     * with sensible defaults if values are missing.
     *
     * @return the spell configuration
     */
    private Config getConfig() {
        if (config == null) {
            // This will be called after loadConfig has been called
            config = new Config(
                    spellConfig.getDouble("values.range", 24.0),
                    spellConfig.getInt("values.wither-duration-ticks", 120),
                    spellConfig.getInt("values.wither-amplifier", 1),
                    spellConfig.getInt("values.blind-duration-ticks", 60),
                    spellConfig.getDouble("values.speed", 1.8),
                    spellConfig.getDouble("values.explosion-radius", 4.0),
                    spellConfig.getDouble("values.damage", 6.0),
                    spellConfig.getDouble("values.knockback", 0.6),
                    EmpireWandAPI.getService(ConfigService.class).getMainConfig().getBoolean("features.friendly-fire",
                            false),
                    spellConfig.getInt("values.ring-particle-count", 14),
                    spellConfig.getDouble("values.ring-radius-step", 0.25),
                    spellConfig.getInt("values.ring-interval-ticks", 2));
        }
        return config;
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "dark-pulse"
     */
    @Override
    @NotNull
    public String key() {
        return "dark-pulse";
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
     * Executes the dark pulse spell logic.
     * <p>
     * This method launches a charged wither skull toward the target entity and
     * creates a visual pulsing ring effect that grows as the projectile travels.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        var targetEntity = player.getTargetEntity((int) getConfig().range);
        if (!(targetEntity instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return null;
        }

        Vector direction = target.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
        Location spawnLoc = player.getEyeLocation().add(direction.clone().multiply(0.5));

        WitherSkull skull = player.getWorld().spawn(spawnLoc, WitherSkull.class);
        skull.setCharged(true);
        skull.setVelocity(direction.multiply(getConfig().speed));
        skull.setShooter(player);
        skull.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType(), key());

        context.plugin().getTaskManager().runTaskTimer(
            new RingPulse(context, skull, getConfig().ringParticleCount, getConfig().ringRadiusStep,
                    getConfig().ringEveryTicks),
            0L, 1L
        );
        context.fx().playSound(player, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);

        context.plugin().getServer().getPluginManager().registerEvents(
                new PulseListener(context, getConfig().witherDuration, getConfig().witherAmplifier,
                        getConfig().blindDuration,
                        getConfig().radius, getConfig().damage, getConfig().knockback, getConfig().friendlyFire),
                context.plugin());
        return null;
    }

    /**
     * Handles the spell effect after execution.
     * <p>
     * This spell's effects are handled by the registered listener.
     *
     * @param context the spell context
     * @param result the result of the spell execution (always null for this spell)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled by the listener.
    }

    /**
     * A runnable that creates the pulsing ring effect around the projectile.
     * <p>
     * This class creates a visual effect that pulses around the wither skull as it travels,
     * with the ring growing larger over time.
     */
    private static class RingPulse extends BukkitRunnable {
        private final SpellContext context;
        private final WitherSkull projectile;
        private final int ringParticleCount;
        private final double radiusStep;
        private final int interval;
        private int ticks = 0;
        private double accumRadius = 0;

        /**
         * Creates a new RingPulse instance.
         *
         * @param context the spell context
         * @param projectile the wither skull projectile
         * @param ringParticleCount the number of particles in each ring
         * @param radiusStep the amount to increase the ring radius each interval
         * @param interval the tick interval between ring updates
         */
        RingPulse(SpellContext context, WitherSkull projectile, int ringParticleCount, double radiusStep,
                int interval) {
            this.context = context;
            this.projectile = projectile;
            this.ringParticleCount = Math.max(4, ringParticleCount);
            this.radiusStep = Math.max(0.05, radiusStep);
            this.interval = Math.max(1, interval);
        }

        /**
         * Runs the ring pulse effect, creating particles around the projectile.
         */
        @Override
        public void run() {
            if (!projectile.isValid() || projectile.isDead()) {
                cancel();
                return;
            }
            if (ticks++ > 20 * 6) {
                cancel();
                return;
            }

            if (ticks % interval != 0)
                return;

            accumRadius += radiusStep;
            Location center = projectile.getLocation();

            for (int i = 0; i < ringParticleCount; i++) {
                double angle = (Math.PI * 2 * i) / ringParticleCount;
                double x = center.getX() + accumRadius * Math.cos(angle);
                double z = center.getZ() + accumRadius * Math.sin(angle);
                Location p = new Location(center.getWorld(), x, center.getY() + 0.15, z);
                context.fx().spawnParticles(p, Particle.SMOKE, 1, 0, 0, 0, 0);
                if (i % 3 == 0) {
                    context.fx().spawnParticles(p, Particle.SOUL_FIRE_FLAME, 1, 0, 0, 0, 0.0);
                }
            }
            context.fx().spawnParticles(center, Particle.REVERSE_PORTAL, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    /**
     * Listener for handling the projectile hit event and applying effects.
     * <p>
     * This listener handles the impact of the wither skull projectile, applying damage
     * and effects to entities within the explosion radius.
     */
    private static class PulseListener implements Listener {
        private final SpellContext context;
        private final int witherDuration;
        private final int witherAmplifier;
        private final int blindDuration;
        private final double radius;
        private final double damage;
        private final double knockback;
        private final boolean friendlyFire;

        /**
         * Creates a new PulseListener instance.
         *
         * @param context the spell context
         * @param witherDuration the duration of the wither effect in ticks
         * @param witherAmplifier the amplifier level for the wither effect
         * @param blindDuration the duration of the blindness effect in ticks
         * @param radius the explosion radius
         * @param damage the damage to apply
         * @param knockback the knockback strength
         * @param friendlyFire whether friendly fire is enabled
         */
        public PulseListener(SpellContext context, int witherDuration, int witherAmplifier, int blindDuration,
                double radius, double damage, double knockback, boolean friendlyFire) {
            this.context = context;
            this.witherDuration = witherDuration;
            this.witherAmplifier = witherAmplifier;
            this.blindDuration = blindDuration;
            this.radius = radius;
            this.damage = damage;
            this.knockback = knockback;
            this.friendlyFire = friendlyFire;
        }

        /**
         * Handles the projectile hit event.
         * <p>
         * This method applies damage and effects to entities within the explosion radius
         * and creates visual and audio feedback for the impact.
         *
         * @param event the projectile hit event
         */
        @EventHandler
        public void onProjectileHit(ProjectileHitEvent event) {
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof WitherSkull))
                return;

            String spellType = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_SPELL,
                    Keys.STRING_TYPE.getType());
            if (!"dark-pulse".equals(spellType))
                return;

            Location hitLoc = projectile.getLocation();
            context.fx().impact(hitLoc, Particle.SOUL_FIRE_FLAME, 50, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.7f);

            for (var entity : hitLoc.getWorld().getNearbyEntities(hitLoc, radius, radius, radius)) {
                if (entity instanceof LivingEntity living) {
                    if (living.equals(projectile.getShooter()) && !friendlyFire)
                        continue;
                    if (living.isDead() || !living.isValid())
                        continue;

                    if (damage > 0)
                        living.damage(damage, (projectile.getShooter() instanceof Player p) ? p : null);
                    living.addPotionEffect(
                            new PotionEffect(PotionEffectType.WITHER, witherDuration, witherAmplifier, false, true));
                    if (blindDuration > 0)
                        living.addPotionEffect(
                                new PotionEffect(PotionEffectType.BLINDNESS, blindDuration, 0, false, true));
                    if (knockback > 0) {
                        Vector kb = living.getLocation().toVector().subtract(hitLoc.toVector()).normalize()
                                .multiply(knockback).setY(0.2);
                        living.setVelocity(kb);
                    }
                }
            }

            projectile.remove();
            HandlerList.unregisterAll(this);
        }
    }
}
