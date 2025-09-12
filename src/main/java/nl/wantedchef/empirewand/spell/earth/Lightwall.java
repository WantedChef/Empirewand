package nl.wantedchef.empirewand.spell.earth;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

/**
 * An earth spell that creates a temporary wall of light that knocks back entities.
 * <p>
 * This spell creates a wall of invisible armor stands that act as a barrier,
 * knocking back any entities that come into contact with it. The wall also applies
 * blindness effects to entities that touch it. The spell has a Dutch display name
 * ("Lichtmuur") and includes visual particle effects.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Temporary light wall barrier</li>
 *   <li>Knockback effect on contact</li>
 *   <li>Blindness potion effect application</li>
 *   <li>Dutch display name ("Lichtmuur")</li>
 *   <li>Particle effects for visual feedback</li>
 *   <li>Configurable size, duration, and effects</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell lightwall = new Lightwall.Builder(api)
 *     .name("Lightwall")
 *     .description("Creates a temporary wall of light that knocks back entities.")
 *     .cooldown(Duration.ofSeconds(18))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class Lightwall extends Spell<Void> {

    /**
     * Builder for creating Lightwall spell instances.
     * <p>
     * Provides a fluent API for configuring the lightwall spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new Lightwall spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Lightwall";
            this.description = "Creates a temporary wall of light that knocks back entities.";
            this.cooldown = java.time.Duration.ofSeconds(18);
            this.spellType = SpellType.EARTH;
        }

        /**
         * Builds and returns a new Lightwall spell instance.
         *
         * @return the constructed Lightwall spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new Lightwall(this);
        }
    }

    /**
     * Constructs a new Lightwall spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private Lightwall(@NotNull Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "lightwall"
     */
    @Override
    @NotNull
    public String key() {
        return "lightwall";
    }

    /**
     * Returns the display name for this spell.
     * <p>
     * This spell uses a Dutch display name: "Lichtmuur".
     *
     * @return the Dutch display name component
     */
    @Override
    @NotNull
    public Component displayName() {
        return Component.text("Lichtmuur");
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
     * Executes the lightwall spell logic.
     * <p>
     * This method creates a wall of invisible armor stands at a location in front
     * of the caster, which knock back entities that come into contact with them.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();
        Location eyeLocation = player.getEyeLocation();
        Location center = eyeLocation.add(eyeLocation.getDirection().multiply(3));
        Vector right = eyeLocation.getDirection().crossProduct(new Vector(0, 1, 0)).normalize();

        double width = spellConfig.getDouble("values.width", 6.0);
        double height = spellConfig.getDouble("values.height", 3.0);
        int duration = spellConfig.getInt("values.duration-ticks", 100);
        double knockbackStrength = spellConfig.getDouble("values.knockback-strength", 0.5);
        int blindnessDuration = spellConfig.getInt("values.blindness-duration-ticks", 30);
        boolean hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);

        List<ArmorStand> wallStands = createWallStands(center, right, width, height);
        new WallTask(context, wallStands, knockbackStrength, blindnessDuration, hitPlayers, hitMobs, width, height)
                .runTaskTimer(context.plugin(), 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                wallStands.forEach(stand -> {
                    if (stand.isValid())
                        stand.remove();
                });
            }
        }.runTaskLater(context.plugin(), duration);

        spawnWallParticles(context, center, width, height, right);
        context.fx().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
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
     * Creates the armor stands that form the light wall.
     * <p>
     * This method spawns invisible armor stands in a wall formation at the specified
     * location with the given dimensions.
     *
     * @param center the center location of the wall
     * @param right the right vector for wall orientation
     * @param width the width of the wall
     * @param height the height of the wall
     * @return a list of armor stands forming the wall
     */
    private @NotNull List<ArmorStand> createWallStands(@NotNull Location center, @NotNull Vector right, double width, double height) {
        Objects.requireNonNull(center, "Center location cannot be null");
        Objects.requireNonNull(right, "Right vector cannot be null");
        
        List<ArmorStand> wallStands = new ArrayList<>();
        var world = center.getWorld();
        if (world == null) {
            return wallStands;
        }
        
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                Vector offset = right.clone().multiply(w - width / 2).add(new Vector(0, h, 0));
                Location standLocation = center.clone().add(offset);

                ArmorStand stand = world.spawn(standLocation, ArmorStand.class, s -> {
                    s.setInvisible(true);
                    s.setMarker(true);
                    s.setGravity(false);
                    s.setInvulnerable(true);
                });
                wallStands.add(stand);
            }
        }
        return wallStands;
    }

    /**
     * A runnable that handles the wall's collision detection and effects.
     * <p>
     * This task checks for entities near the wall and applies knockback and blindness
     * effects to them when they come into contact.
     */
    private class WallTask extends BukkitRunnable {
        private final SpellContext context;
        private final List<ArmorStand> wallStands;
        private final double knockbackStrength;
        private final int blindnessDuration;
        private final boolean hitPlayers;
        private final boolean hitMobs;
        private final double width;
        private final double height;

        /**
         * Creates a new WallTask instance.
         *
         * @param context the spell context
         * @param wallStands the armor stands forming the wall
         * @param knockbackStrength the strength of the knockback effect
         * @param blindnessDuration the duration of the blindness effect in ticks
         * @param hitPlayers whether to affect players
         * @param hitMobs whether to affect mobs
         * @param width the width of the wall
         * @param height the height of the wall
         */
        public WallTask(@NotNull SpellContext context, @NotNull List<ArmorStand> wallStands, double knockbackStrength,
                int blindnessDuration, boolean hitPlayers, boolean hitMobs, double width, double height) {
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.wallStands = Objects.requireNonNull(wallStands, "Wall stands cannot be null");
            this.knockbackStrength = knockbackStrength;
            this.blindnessDuration = blindnessDuration;
            this.hitPlayers = hitPlayers;
            this.hitMobs = hitMobs;
            this.width = width;
            this.height = height;
        }

        /**
         * Runs the wall task, checking for entities near the wall and applying effects.
         */
        @Override
        public void run() {
            if (wallStands.isEmpty() || !wallStands.get(0).isValid()) {
                this.cancel();
                return;
            }

            wallStands.forEach(stand -> {
                if (!stand.isValid())
                    return;

                var world = stand.getWorld();
                if (world == null)
                    return;

                world.getNearbyEntities(stand.getLocation(), 1.5, 1.5, 1.5).forEach(entity -> {
                    if (entity instanceof LivingEntity living) {
                        if ((entity instanceof Player && !hitPlayers) || (!(entity instanceof Player) && !hitMobs))
                            return;

                        Vector delta = living.getLocation().toVector().subtract(stand.getLocation().toVector());
                        if (delta.lengthSquared() < 1.0E-6) {
                            return; // Avoid normalizing a zero-length vector
                        }
                        Vector knockback = delta.normalize();
                        Vector result = entity.getVelocity().add(knockback.multiply(knockbackStrength)).setY(0.2);
                        if (Double.isFinite(result.getX()) && Double.isFinite(result.getY()) && Double.isFinite(result.getZ())) {
                            living.setVelocity(result);
                            living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDuration, 0));
                        }
                        // else skip applying non-finite velocity
                    }
                });
            });

            if (System.currentTimeMillis() % 1000 < 50) { // Roughly every second
                ArmorStand firstStand = wallStands.get(0);
                Location center = firstStand.getLocation(); // Approximate center
                Vector direction = firstStand.getLocation().getDirection();
                if (direction != null) {
                    Vector right = direction.crossProduct(new Vector(0, 1, 0)).normalize();
                    spawnWallParticles(context, center, width, height, right);
                }
            }
        }
    }

    /**
     * Spawns wall particles for visual feedback.
     * <p>
     * This method creates particle effects throughout the wall to make it visible
     * to players.
     *
     * @param context the spell context
     * @param center the center location of the wall
     * @param width the width of the wall
     * @param height the height of the wall
     * @param right the right vector for wall orientation
     */
    private void spawnWallParticles(@NotNull SpellContext context, @NotNull Location center, double width, double height, @NotNull Vector right) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(center, "Center location cannot be null");
        Objects.requireNonNull(right, "Right vector cannot be null");
        
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                Vector offset = right.clone().multiply(w - width / 2).add(new Vector(0, h, 0));
                Location particleLoc = center.clone().add(offset);
                context.fx().spawnParticles(particleLoc, Particle.WHITE_ASH, 2, 0.1, 0.1, 0.1, 0);
                context.fx().spawnParticles(particleLoc, Particle.GLOW, 1, 0, 0, 0, 0);
            }
        }
    }
}
