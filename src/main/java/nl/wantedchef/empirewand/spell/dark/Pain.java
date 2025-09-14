package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * A dark spell that instantly damages the targeted entity.
 *
 * <p>The caster focuses dark energy on their victim, inflicting immediate
 * pain and playing accompanying particle and sound effects.</p>
 *
 * @author WantedChef
 */
public class Pain extends Spell<LivingEntity> {

    /**
     * Builder for creating {@link Pain} instances.
     */
    public static class Builder extends Spell.Builder<LivingEntity> {

        /**
         * Creates a new builder for the Pain spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Pain";
            this.description = "Inflict instant pain on your target";
            this.cooldown = Duration.ofSeconds(5);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds the Pain spell.
         *
         * @return the constructed spell
         */
        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Pain(this);
        }
    }

    private static final double DEFAULT_RANGE = 30.0;
    private static final double DEFAULT_DAMAGE = 10.0;

    /**
     * Constructs the spell from its builder.
     *
     * @param builder the spell builder
     */
    private Pain(Builder builder) {
        super(builder);
    }

    /**
     * Gets the configuration key for this spell.
     *
     * @return "pain"
     */
    @Override
    public String key() {
        return "pain";
    }

    /**
     * Gets the prerequisites for casting the spell.
     *
     * @return level prerequisite requiring level 10
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(10);
    }

    /**
     * Searches for the target entity to hurt.
     *
     * @param context the spell context
     * @return the targeted living entity or {@code null} if none is found
     */
    @Override
    protected LivingEntity executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);

        var target = player.getTargetEntity((int) range);
        if (target instanceof LivingEntity living) {
            return living;
        }

        player.sendMessage("§cNo valid target found!");
        return null;
    }

    /**
     * Applies damage and effects to the target.
     *
     * @param context the spell context
     * @param target  the affected entity
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull LivingEntity target) {
        if (target == null) {
            return;
        }

        Player player = context.caster();
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);

        target.damage(damage, player);

        // Visual effects
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);

        // Sound effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.5f, 0.5f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_BITE, 1.0f, 1.5f);

        player.sendMessage("§4§lPain §cinflicted!");
    }
}
