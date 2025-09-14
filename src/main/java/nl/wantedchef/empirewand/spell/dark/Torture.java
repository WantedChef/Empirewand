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
 * A dark spell that inflicts crippling pain and multiple debuffs on a target.
 *
 * <p>The victim is afflicted with numerous negative potion effects and
 * suffers additional damage.</p>
 *
 * @author WantedChef
 */
public class Torture extends Spell<LivingEntity> {

    /**
     * Builder for creating {@link Torture} instances.
     */
    public static class Builder extends Spell.Builder<LivingEntity> {

        /**
         * Creates a new builder for the Torture spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Torture";
            this.description = "Inflict unbearable torture upon your enemy";
            this.cooldown = Duration.ofSeconds(20);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds the Torture spell.
         *
         * @return the constructed spell
         */
        @Override
        @NotNull
        public Spell<LivingEntity> build() {
            return new Torture(this);
        }
    }

    private static final double DEFAULT_RANGE = 15.0;
    private static final double DEFAULT_DAMAGE = 15.0;
    private static final int DEFAULT_DURATION = 100;

    /**
     * Constructs the spell from its builder.
     *
     * @param builder the spell builder
     */
    private Torture(Builder builder) {
        super(builder);
    }

    /**
     * Gets the configuration key for this spell.
     *
     * @return "torture"
     */
    @Override
    public String key() {
        return "torture";
    }

    /**
     * Gets the prerequisites for casting the spell.
     *
     * @return level prerequisite requiring level 30
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(30);
    }

    /**
     * Searches for a target within range.
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
     * Applies damage and negative effects to the target.
     *
     * @param context the spell context
     * @param target  the entity to torture
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull LivingEntity target) {
        if (target == null) {
            return;
        }

        Player player = context.caster();
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);

        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 2));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 2));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 3));
        target.damage(damage, player);

        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, target.getLocation(), 30, 0.5, 1, 0.5, 0.05);

        context.fx().playSound(target.getLocation(), Sound.ENTITY_VILLAGER_HURT, 2.0f, 0.5f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.5f);

        player.sendMessage("§4§lTorture §cinflicted!");
    }
}
