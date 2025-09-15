package nl.wantedchef.empirewand.spell.weather;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Clears rain and thunder with a spectacular divine light show, bringing back warm sunlight.
 * Features beautiful sun rays, golden particle waves, and peaceful ambient sounds that create
 * a truly magical weather clearing experience.
 *
 * Visual Effects:
 * - Initial divine light burst with golden sparkles
 * - Rotating sun rays emanating from the sky
 * - Expanding waves of golden clearing particles
 * - Ascending firework particles for divine ambiance
 * - Light blue accent particles for freshness
 *
 * Audio Effects:
 * - Beacon activation sound for initial cast
 * - Conduit ambient for mystical atmosphere
 * - Bell chime for peaceful transition
 * - Subtle cave ambient for depth
 *
 * @author WantedChef
 */
public class ClearSkies extends Spell<Void> {

    /**
     * Builder for {@link ClearSkies}.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a builder for the spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Clear Skies";
            this.description = "Calls upon divine light to banish storms and bring peaceful sunshine.";
            this.cooldown = Duration.ofMillis(8000);
            this.spellType = SpellType.WEATHER;
        }

        /**
         * Builds the spell instance.
         *
         * @return a new {@link ClearSkies}
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new ClearSkies(this);
        }
    }

    private ClearSkies(Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String key() {
        return "clearskies";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Stops weather in the caster's world and creates a magical clearing effect.
     * Features divine light rays, golden particles, and smooth weather transition.
     *
     * @param context the spell context
     * @return always {@code null}
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Create initial clearing sound and effect
        context.fx().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        context.fx().playSound(player, Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 1.2f);

        // Phase 1: Initial burst of divine light
        createDivineBurst(context, playerLoc);

        // Clear weather immediately for visual consistency
        world.setStorm(false);
        world.setThundering(false);

        // Phase 2: Expanding clearing effect with light rays (async)
        new BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 100; // 5 seconds

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                // Create sun rays effect
                if (ticks % 5 == 0) {
                    createSunRays(context, playerLoc, ticks);
                }

                // Create expanding golden particles
                if (ticks % 3 == 0) {
                    createExpandingGoldenWave(context, playerLoc, ticks);
                }

                // Ambient peaceful sounds
                if (ticks == 20) {
                    context.fx().playSound(player, Sound.BLOCK_BELL_USE, 0.6f, 1.5f);
                } else if (ticks == 60) {
                    context.fx().playSound(player, Sound.AMBIENT_CAVE, 0.3f, 2.0f);
                }

                ticks++;
            }
        }.runTaskTimer(context.plugin(), 10L, 1L);

        return null;
    }

    /**
     * Creates an initial burst of divine light particles around the caster.
     */
    private void createDivineBurst(@NotNull SpellContext context, @NotNull Location center) {
        // Central divine burst
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.ENCHANT, 30, 0.5, 0.5, 0.5, 2.0);

        // Golden sparkles
        Particle.DustOptions goldDust = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f);
        context.fx().spawnParticles(center.clone().add(0, 2, 0), Particle.DUST, 25, 1.0, 1.0, 1.0, 0.0, goldDust);

        // White light burst
        Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 2.0f);
        context.fx().spawnParticles(center.clone().add(0, 1.5, 0), Particle.DUST, 20, 0.8, 0.8, 0.8, 0.0, whiteDust);

        // Cloud clearing effect
        context.fx().spawnParticles(center.clone().add(0, 5, 0), Particle.CLOUD, 15, 2.0, 1.0, 2.0, 0.1);
    }

    /**
     * Creates sun rays effect emanating from above the player.
     */
    private void createSunRays(@NotNull SpellContext context, @NotNull Location center, int tick) {
        Location skyPoint = center.clone().add(0, 25, 0);
        int numRays = 8;
        double radius = 3.0 + (tick * 0.05); // Expanding rays

        for (int i = 0; i < numRays; i++) {
            double angle = (2 * Math.PI * i / numRays) + (tick * 0.1); // Rotating rays

            // Create ray from sky to ground
            for (int j = 0; j < 15; j++) {
                double x = skyPoint.getX() + Math.cos(angle) * radius * (j / 15.0);
                double y = skyPoint.getY() - (j * 1.5);
                double z = skyPoint.getZ() + Math.sin(angle) * radius * (j / 15.0);

                Location rayPoint = new Location(center.getWorld(), x, y, z);

                // Golden ray particles
                if (j % 2 == 0) {
                    Particle.DustOptions rayDust = new Particle.DustOptions(Color.YELLOW, 1.0f);
                    context.fx().spawnParticles(rayPoint, Particle.DUST, 1, 0.1, 0.1, 0.1, 0.0, rayDust);
                }

                // Bright white core
                if (j % 3 == 0) {
                    Particle.DustOptions coreDust = new Particle.DustOptions(Color.WHITE, 0.8f);
                    context.fx().spawnParticles(rayPoint, Particle.DUST, 1, 0.05, 0.05, 0.05, 0.0, coreDust);
                }
            }
        }
    }

    /**
     * Creates expanding waves of golden particles representing the clearing effect.
     */
    private void createExpandingGoldenWave(@NotNull SpellContext context, @NotNull Location center, int tick) {
        double waveRadius = tick * 0.3; // Expanding wave
        int particlesPerWave = Math.min(50, tick * 2);

        for (int i = 0; i < particlesPerWave; i++) {
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double currentRadius = waveRadius + ThreadLocalRandom.current().nextGaussian() * 0.5;

            double x = center.getX() + Math.cos(angle) * currentRadius;
            double y = center.getY() + 1 + ThreadLocalRandom.current().nextGaussian() * 0.3;
            double z = center.getZ() + Math.sin(angle) * currentRadius;

            Location particlePoint = new Location(center.getWorld(), x, y, z);

            // Golden clearing particles
            Particle.DustOptions goldDust = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f);
            context.fx().spawnParticles(particlePoint, Particle.DUST, 1, 0.1, 0.1, 0.1, 0.0, goldDust);

            // Occasional sparkles
            if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                context.fx().spawnParticles(particlePoint, Particle.ENCHANT, 1, 0.1, 0.1, 0.1, 0.5);
            }

            // Light blue accent particles for freshness
            if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                Particle.DustOptions blueDust = new Particle.DustOptions(Color.fromRGB(173, 216, 230), 0.8f);
                context.fx().spawnParticles(particlePoint, Particle.DUST, 1, 0.1, 0.1, 0.1, 0.0, blueDust);
            }
        }

        // Create ascending particles for divine feel
        if (tick % 4 == 0) {
            for (int i = 0; i < 5; i++) {
                double x = center.getX() + ThreadLocalRandom.current().nextGaussian() * 2;
                double y = center.getY() + ThreadLocalRandom.current().nextDouble() * 3;
                double z = center.getZ() + ThreadLocalRandom.current().nextGaussian() * 2;

                Location ascendPoint = new Location(center.getWorld(), x, y, z);


                context.fx().spawnParticles(ascendPoint, Particle.FIREWORK, 1, 0, 0, 0, 0.1);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}

