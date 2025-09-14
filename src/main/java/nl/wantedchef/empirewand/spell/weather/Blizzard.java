package nl.wantedchef.empirewand.spell.weather;

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
 * Engulfs nearby creatures in a chilling blizzard that slows and harms them.
 *
 * @author WantedChef
 */
public class Blizzard extends Spell<Void> {

    /**
     * Builder for {@link Blizzard}.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a builder for the spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blizzard";
            this.description = "Freezes nearby enemies in a snowstorm.";
            this.cooldown = Duration.ofMillis(7000);
            this.spellType = SpellType.WEATHER;
        }

        /**
         * Builds the spell instance.
         *
         * @return a new {@link Blizzard}
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new Blizzard(this);
        }
    }

    private Blizzard(Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String key() {
        return "blizzard";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Slows and damages enemies within range with snow particles.
     *
     * @param context the spell context
     * @return always {@code null}
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        double radius = spellConfig.getDouble("values.radius", 6.0);
        double damage = spellConfig.getDouble("values.damage", 1.0);
        int slowDur = spellConfig.getInt("values.slow-duration-ticks", 100);
        int slowAmp = spellConfig.getInt("values.slow-amplifier", 1);
        for (LivingEntity e : player.getWorld().getNearbyLivingEntities(player.getLocation(), radius)) {
            if (e.equals(player)) {
                continue;
            }
            e.damage(damage, player);
            e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDur, slowAmp, false, true));
            context.fx().spawnParticles(e.getLocation(), Particle.SNOWFLAKE, 20, 0.5, 1.0, 0.5, 0.02);
        }
        context.fx().playSound(player, Sound.BLOCK_SNOW_BREAK, 1.0f, 0.8f);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}

