package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * Mephidrain - A dark spell that drains health from nearby enemies and transfers it to the caster.
 * <p>
 * This spell represents Mephidantes' power by draining health from nearby enemies and
 * transferring a portion of that health to the caster. The spell provides visual and audio
 * feedback during the draining process.
 * <p>
 * <strong>Effects:</strong>
 * <ul>
 *   <li>Drains health from nearby entities within radius</li>
 *   <li>Transfers a percentage of drained health to the caster</li>
 *   <li>Applies visual smoke particles to affected entities</li>
 *   <li>Provides visual heart particles to the caster</li>
 *   <li>Audio feedback with wither hurt sound</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell mephidrain = new Mephidrain.Builder(api)
 *     .name("Mephidrain")
 *     .description("Drain health from nearby enemies and transfer it to yourself.")
 *     .cooldown(Duration.ofSeconds(12))
 *     .build();
 * }</pre>
 *
 * @author WantedChef
 * @since 1.0.0
 */
public class Mephidrain extends Spell<Void> {

    /**
     * Builder for creating Mephidrain spell instances.
     * <p>
     * Provides a fluent API for configuring the mephidrain spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new Mephidrain spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Mephidrain";
            this.description = "Drain health from nearby enemies and transfer it to yourself.";
            this.cooldown = Duration.ofSeconds(12);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds and returns a new Mephidrain spell instance.
         *
         * @return the constructed Mephidrain spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new Mephidrain(this);
        }
    }

    /**
     * Constructs a new Mephidrain spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    private Mephidrain(Builder builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "mephidrain"
     */
    @Override
    @NotNull
    public String key() {
        return "mephidrain";
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
     * Executes the mephidrain spell logic.
     * <p>
     * This method drains health from nearby entities and transfers a portion of that
     * health to the caster.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    @Nullable
    protected Void executeSpell(SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 6.0);
        double damage = spellConfig.getDouble("values.damage", 8.0);
        double healPercentage = spellConfig.getDouble("values.heal-percentage", 0.5);

        // Find nearby entities
        var world = player.getWorld();
        if (world == null) {
            return null;
        }
        
        var entities = world.getNearbyEntities(player.getLocation(), radius, radius, radius);
        for (var entity : entities) {
            if (entity instanceof org.bukkit.entity.LivingEntity living && !living.equals(player)
                    && !living.isDead() && living.isValid()) {

                // Damage the entity
                living.damage(damage, player);

                // Heal the player
                double healAmount = damage * healPercentage;
                AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
                player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));

                // Visual effects
                context.fx().spawnParticles(living.getLocation(), org.bukkit.Particle.SMOKE, 15,
                        0.3, 0.6, 0.3, 0.1);
                context.fx().spawnParticles(player.getLocation(), org.bukkit.Particle.HEART, 5, 0.3,
                        0.6, 0.3, 0.1);
            }
        }

        // Sound effect
        context.fx().playSound(player, org.bukkit.Sound.ENTITY_WITHER_HURT, 1.0f, 0.8f);
        return null;
    }

    /**
     * Handles the spell effect after execution.
     * <p>
     * This spell has instant effects that are applied during execution.
     *
     * @param context the spell context
     * @param result the result of the spell execution (always null for this spell)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
