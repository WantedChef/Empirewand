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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * A dark spell that curses a target with the wither effect.
 *
 * @author WantedChef
 */
public class Wither extends Spell<LivingEntity> {

    /**
     * Builder for creating {@link Wither} instances.
     */
    public static class Builder extends Spell.Builder<LivingEntity> {

        /**
         * Creates a new builder for the Wither spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Wither";
            this.description = "Inflict wither curse upon your target";
            this.cooldown = Duration.ofSeconds(8);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds the Wither spell.
         *
         * @return the constructed spell
         */
        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Wither(this);
        }
    }

    private static final double DEFAULT_RANGE = 25.0;
    private static final int DEFAULT_DURATION = 200;
    private static final int DEFAULT_AMPLIFIER = 2;

    /**
     * Constructs the spell from its builder.
     *
     * @param builder the spell builder
     */
    private Wither(Builder builder) {
        super(builder);
    }

    /**
     * Gets the configuration key for this spell.
     *
     * @return "wither"
     */
    @Override
    public String key() {
        return "wither";
    }

    /**
     * Gets the prerequisites for casting.
     *
     * @return level prerequisite requiring level 20
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(20);
    }

    /**
     * Searches for a target within range to curse.
     *
     * @param context the spell context
     * @return the targeted entity or {@code null} if none found
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
     * Applies the wither effect and plays visual and sound cues.
     *
     * @param context the spell context
     * @param target  the entity to curse
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull LivingEntity target) {
        if (target == null) {
            return;
        }

        Player player = context.caster();
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        int amplifier = spellConfig.getInt("values.amplifier", DEFAULT_AMPLIFIER);

        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, amplifier));

        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2,
                0.01);

        context.fx().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 0.8f, 1.5f);

        player.sendMessage("§5§lWither §dcurse applied!");
    }
}
