package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A dark spell that channels a destructive ritual damaging and weakening nearby enemies.
 * <p>
 * This spell requires the caster to channel for a period of time, during which a circle
 * of soul particles appears around them. Upon completion, it damages and applies weakness
 * effects to all entities within the ritual's radius.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Channeling requirement with visual feedback</li>
 *   <li>Area of effect damage within specified radius</li>
 *   <li>Weakness potion effect application</li>
 *   <li>Dutch display name ("Ritueel van Ontering")</li>
 *   <li>Configurable damage, duration, and targeting options</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell ritual = new RitualOfUnmaking.Builder(api)
 *     .name("Ritual of Unmaking")
 *     .description("Channels a destructive ritual that damages and weakens nearby enemies.")
 *     .cooldown(Duration.ofSeconds(40))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class RitualOfUnmaking extends Spell<Void> {

    /**
     * Configuration class for the RitualOfUnmaking spell.
     * <p>
     * This class holds all configurable values for the spell, providing a clean
     * way to manage spell parameters.
     */
    private static class Config {
        private final int channelTicks;
        private final double radius;
        private final double damage;
        private final int weaknessDuration;
        private final int weaknessAmplifier;
        private final boolean hitPlayers;
        private final boolean hitMobs;

        /**
         * Creates a new Config instance from a readable config.
         *
         * @param config the readable configuration
         * @throws NullPointerException if config is null
         */
        public Config(@NotNull ReadableConfig config) {
            Objects.requireNonNull(config, "Config cannot be null");
            this.channelTicks = config.getInt("values.channel-ticks", 40);
            this.radius = config.getDouble("values.radius", 6.0);
            this.damage = config.getDouble("values.damage", 8.0);
            this.weaknessDuration = config.getInt("values.weakness-duration-ticks", 120);
            this.weaknessAmplifier = config.getInt("values.weakness-amplifier", 0);
            this.hitPlayers = config.getBoolean("flags.hit-players", true);
            this.hitMobs = config.getBoolean("flags.hit-mobs", true);
        }
    }

    /**
     * Builder for creating RitualOfUnmaking spell instances.
     * <p>
     * Provides a fluent API for configuring the ritual of unmaking spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new RitualOfUnmaking spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@NotNull EmpireWandAPI api) {
            super(api);
            this.name = "Ritual of Unmaking";
            this.description = "Channels a destructive ritual that damages and weakens nearby enemies.";
            this.cooldown = java.time.Duration.ofSeconds(40);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds and returns a new RitualOfUnmaking spell instance.
         *
         * @return the constructed RitualOfUnmaking spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new RitualOfUnmaking(this);
        }
    }

    private Config config;

    /**
     * Constructs a new RitualOfUnmaking spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private RitualOfUnmaking(@NotNull Builder builder) {
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
    private @NotNull Config getConfig() {
        if (config == null) {
            // This will be called after loadConfig has been called
            config = new Config(spellConfig);
        }
        return config;
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "ritual-of-unmaking"
     */
    @Override
    @NotNull
    public String key() {
        return "ritual-of-unmaking";
    }

    /**
     * Returns the display name for this spell.
     * <p>
     * This spell uses a Dutch display name: "Ritueel van Ontering".
     *
     * @return the Dutch display name component
     */
    @Override
    @NotNull
    public Component displayName() {
        return Component.text("Ritueel van Ontering");
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
     * Executes the ritual of unmaking spell logic.
     * <p>
     * This method starts a channeling task that creates visual effects around the caster
     * and applies damage and weakness effects to nearby entities upon completion.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        context.plugin().getTaskManager().runTaskTimer(
            new ChannelTask(context, player, getConfig().channelTicks, getConfig().radius, getConfig().damage,
                    getConfig().weaknessDuration,
                    getConfig().weaknessAmplifier, getConfig().hitPlayers, getConfig().hitMobs),
            0L, 1L
        );
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
        // Effects are handled in the scheduler.
    }

    /**
     * A runnable that handles the channeling process for the ritual.
     * <p>
     * This task creates visual effects around the caster during channeling and
     * applies the ritual's effects upon completion.
     */
    private class ChannelTask extends BukkitRunnable {
        private final SpellContext context;
        private final Player player;
        private final int channelTicks;
        private final double radius;
        private final double damage;
        private final int weaknessDuration;
        private final int weaknessAmplifier;
        private final boolean hitPlayers;
        private final boolean hitMobs;
        private int ticksPassed = 0;

        /**
         * Creates a new ChannelTask instance.
         *
         * @param context the spell context
         * @param player the player casting the spell
         * @param channelTicks the number of ticks to channel
         * @param radius the radius of effect
         * @param damage the damage to apply
         * @param weaknessDuration the duration of weakness effect in ticks
         * @param weaknessAmplifier the amplifier for weakness effect
         * @param hitPlayers whether to affect players
         * @param hitMobs whether to affect mobs
         */
        public ChannelTask(@NotNull SpellContext context, @NotNull Player player, int channelTicks, double radius, double damage,
                int weaknessDuration, int weaknessAmplifier, boolean hitPlayers, boolean hitMobs) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.player = Objects.requireNonNull(player, "Player cannot be null");
            this.channelTicks = channelTicks;
            this.radius = radius;
            this.damage = damage;
            this.weaknessDuration = weaknessDuration;
            this.weaknessAmplifier = weaknessAmplifier;
            this.hitPlayers = hitPlayers;
            this.hitMobs = hitMobs;
        }

        /**
         * Runs the channeling task, creating visual effects and checking for interruption.
         */
        @Override
        public void run() {
            if (!player.isValid() || player.isDead() || (player.getLastDamage() > 0 && ticksPassed > 0)) {
                this.cancel();
                context.fx().fizzle(player);
                return;
            }

            spawnCircleParticles(player, radius);
            ticksPassed++;

            if (ticksPassed >= channelTicks) {
                this.cancel();
                onFinish();
            }
        }

        /**
         * Applies the ritual's effects to nearby entities upon completion.
         */
        private void onFinish() {
            var world = player.getWorld();
            if (world == null) {
                return;
            }
            
            List<LivingEntity> targets = world.getLivingEntities().stream()
                    .filter(entity -> entity.getLocation() != null && entity.getLocation().distance(player.getLocation()) <= radius)
                    .filter(entity -> !entity.equals(player))
                    .filter(entity -> (entity instanceof Player && hitPlayers)
                            || (!(entity instanceof Player) && hitMobs))
                    .collect(Collectors.toList());

            for (LivingEntity target : targets) {
                if (target.isValid() && !target.isDead()) {
                    target.damage(damage, player);
                    target.addPotionEffect(
                            new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, weaknessAmplifier));
                }
            }

            context.fx().playSound(player, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
            spawnBurstParticles(player, radius);
        }

        /**
         * Spawns circle particles around the player during channeling.
         *
         * @param player the player casting the spell
         * @param radius the radius of the circle
         */
        private void spawnCircleParticles(@NotNull Player player, double radius) {
            Objects.requireNonNull(player, "Player cannot be null");
            var world = player.getWorld();
            if (world == null) {
                return;
            }
            
            for (int i = 0; i < 36; i++) {
                double angle = 2 * Math.PI * i / 36;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                world.spawnParticle(Particle.SOUL, player.getLocation().add(x, 0.1, z), 1, 0, 0, 0, 0);
            }
        }

        /**
         * Spawns burst particles around the player upon ritual completion.
         *
         * @param player the player casting the spell
         * @param radius the radius of the burst
         */
        private void spawnBurstParticles(@NotNull Player player, double radius) {
            Objects.requireNonNull(player, "Player cannot be null");
            var world = player.getWorld();
            if (world == null) {
                return;
            }
            
            for (int i = 0; i < 50; i++) {
                double x = (Math.random() - 0.5) * radius * 2;
                double z = (Math.random() - 0.5) * radius * 2;
                world.spawnParticle(Particle.SMOKE, player.getLocation().add(x, 1, z), 1, 0, 0, 0, 0.1);
            }
        }
    }
}
