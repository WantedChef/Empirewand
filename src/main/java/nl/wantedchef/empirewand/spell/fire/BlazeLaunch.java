package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
 import java.util.Objects;

/**
 * A mobility-focused fire spell that launches the caster forward and leaves a
 * short-lived flame trail for dramatic feedback.
 * <p>
 * The launch power and trail duration are configurable via the spell config. A
 * lightweight scheduler renders flame particles behind the player for a brief
 * period after launch, together with periodic ambient sounds.
 * <p>
 * Features:
 * <ul>
 *   <li>Forward dash based on the caster's eye direction.</li>
 *   <li>Configurable launch power and trail duration.</li>
 *   <li>Visual flame trail and ambient blaze sounds.</li>
 * </ul>
 *
 * Usage example:
 * <pre>{@code
 * Spell<Void> blaze = new BlazeLaunch.Builder(api)
 *     .name("Blaze Launch")
 *     .description("Launches you forward, leaving a trail of fire.")
 *     .cooldown(java.time.Duration.ofSeconds(10))
 *     .build();
 * }</pre>
 */
public class BlazeLaunch extends Spell<Void> {

    /**
     * The builder for the BlazeLaunch spell.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new builder for the BlazeLaunch spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blaze Launch";
            this.description = "Launches you forward, leaving a trail of fire.";
            this.cooldown = java.time.Duration.ofSeconds(10);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new BlazeLaunch(this);
        }
    }

    private static final double DEFAULT_POWER = 1.8;
    private static final int DEFAULT_TRAIL_DURATION_TICKS = 40;
    private static final float BLAZE_SHOOT_VOLUME = 1.0f;
    private static final float BLAZE_SHOOT_PITCH = 1.0f;
    private static final int PARTICLE_COUNT = 10;
    private static final double PARTICLE_OFFSET = 0.2;
    private static final double PARTICLE_SPEED = 0.05;
    private static final int AMBIENT_SOUND_INTERVAL_TICKS = 20;
    private static final float AMBIENT_SOUND_VOLUME = 0.5f;
    private static final float AMBIENT_SOUND_PITCH = 1.0f;
    private static final long TASK_TIMER_DELAY = 0L;
    private static final long TASK_TIMER_PERIOD = 1L;

    private BlazeLaunch(Builder builder) {
        super(builder);
    }

    @Override
    @NotNull
    public String key() {
        return "blaze-launch";
    }

    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");

        Player player = context.caster();

        double power = spellConfig.getDouble("values.power", DEFAULT_POWER);
        int trailDuration = spellConfig.getInt("values.trail-duration-ticks", DEFAULT_TRAIL_DURATION_TICKS);

        Location eye = player.getEyeLocation();
        if (eye == null) {
            context.fx().fizzle(player);
            return null;
        }

        Vector direction = eye.getDirection().normalize();
        player.setVelocity(direction.multiply(power));

        startFireTrail(player, trailDuration, context);

        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, BLAZE_SHOOT_VOLUME, BLAZE_SHOOT_PITCH);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    /**
     * Starts the fire trail effect.
     *
     * @param player   The player to create the trail for.
     * @param duration The duration of the trail in ticks.
     * @param context  The spell context.
     */
    private void startFireTrail(Player player, int duration, SpellContext context) {
        BukkitRunnable task = new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isValid() || player.isDead()) {
                    this.cancel();
                    return;
                }

                Location playerLoc = player.getLocation();
                if (playerLoc != null) {
                    context.fx().spawnParticles(playerLoc, Particle.FLAME, PARTICLE_COUNT, PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_SPEED);
                }

                if (ticks % AMBIENT_SOUND_INTERVAL_TICKS == 0) {
                    if (playerLoc != null) {
                        context.fx().playSound(playerLoc, Sound.ENTITY_BLAZE_AMBIENT, AMBIENT_SOUND_VOLUME, AMBIENT_SOUND_PITCH);
                    }
                }
                ticks++;
            }
        };
        context.plugin().getTaskManager().runTaskTimer(task, TASK_TIMER_DELAY, TASK_TIMER_PERIOD);
    }
}
