package nl.wantedchef.empirewand.spell.weather;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Performs a mystical rain dance ceremony that summons powerful storms with spectacular visual effects.
 * Features ceremonial particle patterns, atmospheric transitions, and immersive rain summoning ritual.
 *
 * @author WantedChef
 */
public class RainDance extends Spell<Void> {

    /**
     * Builder for {@link RainDance}.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a builder for the spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Rain Dance";
            this.description = "Performs a mystical ceremony to summon rain with spectacular effects.";
            this.cooldown = Duration.ofMillis(8000);
            this.spellType = SpellType.WEATHER;
        }

        /**
         * Builds the spell instance.
         *
         * @return a new {@link RainDance}
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new RainDance(this);
        }
    }

    private RainDance(Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String key() {
        return "raindance";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Performs the mystical rain dance ceremony with spectacular visual and audio effects.
     * Creates a ceremonial ritual that builds up to summoning rain with smooth transitions.
     *
     * @param context the spell context containing the caster and resources
     * @return always {@code null}
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        World world = player.getWorld();
        Location castLocation = player.getLocation().clone();
        Plugin plugin = context.plugin();

        // Configuration values with enhanced defaults
        int stormDuration = spellConfig.getInt("values.storm-duration-ticks", 1200); // 60 seconds
        int ritualDuration = spellConfig.getInt("values.ritual-duration-ticks", 60); // 3 seconds
        double ritualRadius = spellConfig.getDouble("values.ritual-radius", 4.0);
        int particleDensity = spellConfig.getInt("values.particle-density", 3);
        boolean smoothTransition = spellConfig.getBoolean("values.smooth-transition", true);
        boolean ceremonialEffects = spellConfig.getBoolean("values.ceremonial-effects", true);

        // Start the rain dance ceremony
        performRainDanceCeremony(context, plugin, castLocation, ritualDuration, ritualRadius, particleDensity, ceremonialEffects);

        // Schedule the actual rain summoning after the ceremony
        new BukkitRunnable() {
            @Override
            public void run() {
                summonRainWithTransition(context, world, stormDuration, smoothTransition, castLocation);
            }
        }.runTaskLater(plugin, ritualDuration);

        return null;
    }

    /**
     * Performs the mystical rain dance ceremony with elaborate visual effects.
     */
    private void performRainDanceCeremony(@NotNull SpellContext context, @NotNull Plugin plugin,
            @NotNull Location castLocation, int duration, double radius, int density, boolean ceremonial) {
        Player player = context.caster();

        // Play ceremonial beginning sound
        context.fx().playSound(castLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);
        context.fx().playSound(castLocation, Sound.AMBIENT_UNDERWATER_ENTER, 0.6f, 1.2f);

        // Ceremonial particle ritual animation
        new BukkitRunnable() {
            int ticks = 0;
            final double baseRadius = radius;
            final Location center = castLocation.clone();

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Progress through the ceremony (0.0 to 1.0)
                double progress = (double) ticks / duration;

                // Create mystical dance patterns
                createMysticalDancePatterns(context, center, progress, baseRadius, density, ceremonial);

                // Intensifying atmospheric effects
                if (ticks % 10 == 0) {
                    createAtmosphericBuildup(context, center, progress);
                }

                // Ceremonial sound progression
                if (ticks % 15 == 0) {
                    playProgressiveCeremonialSounds(context, center, progress);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Creates mystical dance-like particle patterns around the caster.
     */
    private void createMysticalDancePatterns(@NotNull SpellContext context, @NotNull Location center,
            double progress, double radius, int density, boolean ceremonial) {

        double time = progress * Math.PI * 8; // Multiple rotations during ceremony
        double intensityMultiplier = Math.sin(progress * Math.PI) + 0.5; // Builds up then maintains

        // Primary mystical circle with flowing water particles
        int circleParticles = (int)(16 * intensityMultiplier);
        for (int i = 0; i < circleParticles; i++) {
            double angle = (2 * Math.PI * i / circleParticles) + time;
            double currentRadius = radius * (0.8 + 0.2 * Math.sin(time * 2));

            double x = center.getX() + Math.cos(angle) * currentRadius;
            double z = center.getZ() + Math.sin(angle) * currentRadius;
            double y = center.getY() + 0.2 + Math.sin(angle * 3 + time) * 0.3;

            Location particleLocation = new Location(center.getWorld(), x, y, z);

            // Flowing water particles
            context.fx().spawnParticles(particleLocation, Particle.DRIPPING_WATER, 1, 0.1, 0.1, 0.1, 0.0);

            // Add mystical sparkles
            if (ceremonial && Math.random() < 0.3) {
                context.fx().spawnParticles(particleLocation, Particle.ENCHANT, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }

        // Secondary inner spiral of cloud particles
        if (ceremonial) {
            int spiralPoints = (int)(8 * intensityMultiplier);
            for (int i = 0; i < spiralPoints; i++) {
                double spiralAngle = time * 2 + (i * Math.PI / 4);
                double spiralRadius = (radius * 0.4) * (1 - (double)i / spiralPoints);

                double x = center.getX() + Math.cos(spiralAngle) * spiralRadius;
                double z = center.getZ() + Math.sin(spiralAngle) * spiralRadius;
                double y = center.getY() + 0.5 + i * 0.1;

                Location spiralLocation = new Location(center.getWorld(), x, y, z);
                context.fx().spawnParticles(spiralLocation, Particle.CLOUD, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Upward flowing energy streams
        if (progress > 0.3) {
            int streamCount = (int)(4 * Math.min(1.0, (progress - 0.3) / 0.4));
            for (int i = 0; i < streamCount; i++) {
                double streamAngle = (Math.PI * 2 * i / 4) + time * 0.5;
                double streamRadius = radius * 0.6;

                for (int j = 0; j < 6; j++) {
                    double x = center.getX() + Math.cos(streamAngle) * streamRadius;
                    double z = center.getZ() + Math.sin(streamAngle) * streamRadius;
                    double y = center.getY() + j * 0.4 + Math.sin(time * 3) * 0.1;

                    Location streamLocation = new Location(center.getWorld(), x, y, z);
                    context.fx().spawnParticles(streamLocation, Particle.SPLASH, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }

        // Central pillar of mystical energy (final phase)
        if (progress > 0.7) {
            double pillarHeight = (progress - 0.7) / 0.3 * 5.0; // Up to 5 blocks high
            for (int i = 0; i < (int)(pillarHeight * 4); i++) {
                double y = center.getY() + i * 0.25;
                double pillarRadius = 0.3 * (1 - i / (pillarHeight * 4));

                // Multiple particles around the pillar
                for (int j = 0; j < 3; j++) {
                    double pillarAngle = time * 4 + j * Math.PI * 2 / 3;
                    double x = center.getX() + Math.cos(pillarAngle) * pillarRadius;
                    double z = center.getZ() + Math.sin(pillarAngle) * pillarRadius;

                    Location pillarLocation = new Location(center.getWorld(), x, y, z);
                    context.fx().spawnParticles(pillarLocation, Particle.NAUTILUS, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }
    }

    /**
     * Creates atmospheric buildup effects as the ceremony progresses.
     */
    private void createAtmosphericBuildup(@NotNull SpellContext context, @NotNull Location center, double progress) {
        // Expanding atmospheric rings
        double ringRadius = 8.0 * progress;
        int ringParticles = (int)(32 * progress);

        for (int i = 0; i < ringParticles; i++) {
            double angle = 2 * Math.PI * i / ringParticles;
            double x = center.getX() + Math.cos(angle) * ringRadius;
            double z = center.getZ() + Math.sin(angle) * ringRadius;
            double y = center.getY() + 0.1;

            Location ringLocation = new Location(center.getWorld(), x, y, z);
            context.fx().spawnParticles(ringLocation, Particle.FALLING_WATER, 1, 0.1, 0.0, 0.1, 0.0);
        }

        // High altitude cloud formation
        if (progress > 0.4) {
            int cloudParticles = (int)(20 * (progress - 0.4));
            for (int i = 0; i < cloudParticles; i++) {
                double x = center.getX() + (Math.random() - 0.5) * 16;
                double z = center.getZ() + (Math.random() - 0.5) * 16;
                double y = center.getY() + 8 + Math.random() * 4;

                Location cloudLocation = new Location(center.getWorld(), x, y, z);
                context.fx().spawnParticles(cloudLocation, Particle.CLOUD, 2, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }

    /**
     * Plays progressive ceremonial sounds that build in intensity.
     */
    private void playProgressiveCeremonialSounds(@NotNull SpellContext context, @NotNull Location center, double progress) {
        // Base mystical hum
        float volume = (float)(0.3 + progress * 0.5);
        float pitch = (float)(0.8 + progress * 0.6);
        context.fx().playSound(center, Sound.BLOCK_BEACON_AMBIENT, volume, pitch);

        // Wind effects intensify
        if (progress > 0.3) {
            float windVolume = (float)((progress - 0.3) * 0.6);
            context.fx().playSound(center, Sound.ITEM_ELYTRA_FLYING, windVolume, 0.7f);
        }

        // Thunder rumbles as climax approaches
        if (progress > 0.6) {
            float thunderVolume = (float)((progress - 0.6) * 0.8);
            context.fx().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, thunderVolume, 0.5f);
        }

        // Water droplet sounds
        if (Math.random() < progress) {
            context.fx().playSound(center, Sound.WEATHER_RAIN_ABOVE, 0.4f, 1.2f + (float)(Math.random() * 0.6));
        }
    }

    /**
     * Summons rain with smooth atmospheric transition effects.
     */
    private void summonRainWithTransition(@NotNull SpellContext context, @NotNull World world,
            int duration, boolean smooth, @NotNull Location center) {

        // Climactic ceremony completion
        context.fx().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 0.8f);
        context.fx().playSound(center, Sound.WEATHER_RAIN, 0.8f, 1.0f);

        // Explosive climax particle effect
        for (int i = 0; i < 60; i++) {
            Vector randomOffset = new Vector(
                (Math.random() - 0.5) * 8,
                Math.random() * 6,
                (Math.random() - 0.5) * 8
            );
            Location burstLocation = center.clone().add(randomOffset);
            context.fx().spawnParticles(burstLocation, Particle.SPLASH, 8, 0.3, 0.3, 0.3, 0.1);
            context.fx().spawnParticles(burstLocation, Particle.CLOUD, 4, 0.2, 0.2, 0.2, 0.05);
        }

        // Set the weather with enhanced duration
        world.setStorm(true);
        world.setWeatherDuration(duration);

        // If smooth transitions enabled, create gradual rain intensity buildup
        if (smooth) {
            createSmoothRainTransition(context, center, duration);
        }

        // Success message
        context.fx().actionBar(context.caster(), "§b§l✦ §7Rain dance completed! Storm summoned! §b§l✦");
    }

    /**
     * Creates smooth rain transition with gradually increasing intensity.
     */
    private void createSmoothRainTransition(@NotNull SpellContext context, @NotNull Location center, int totalDuration) {
        Plugin plugin = context.plugin();

        new BukkitRunnable() {
            int ticks = 0;
            final int transitionDuration = 100; // 5 seconds

            @Override
            public void run() {
                if (ticks >= transitionDuration || !context.caster().isOnline()) {
                    cancel();
                    return;
                }

                double intensity = (double) ticks / transitionDuration;

                // Gradually increasing rain particle effects around the area
                int rainParticles = (int)(30 * intensity);
                for (int i = 0; i < rainParticles; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * 20;
                    double z = center.getZ() + (Math.random() - 0.5) * 20;
                    double y = center.getY() + 4 + Math.random() * 8;

                    Location rainLocation = new Location(center.getWorld(), x, y, z);
                    context.fx().spawnParticles(rainLocation, Particle.FALLING_WATER, 1, 0.1, 0.1, 0.1, 0.0);
                }

                // Atmospheric sound building
                if (ticks % 20 == 0) {
                    float volume = (float)(intensity * 0.3);
                    context.fx().playSound(center, Sound.WEATHER_RAIN_ABOVE, volume, 1.0f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effect is handled entirely in executeSpell with async animations
    }
}

