package nl.wantedchef.empirewand.spell.weather;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Summons a localized rain storm around the caster, soaking the area and starting a brief downpour.
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
            this.description = "Summons rain around you.";
            this.cooldown = Duration.ofMillis(5000);
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
     * Starts rain in the caster's world and plays splash effects.
     *
     * @param context the spell context containing the caster and resources
     * @return always {@code null}
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        World world = player.getWorld();
        int duration = spellConfig.getInt("values.storm-duration-ticks", 600);
        world.setStorm(true);
        world.setWeatherDuration(duration);
        context.fx().spawnParticles(player.getLocation(), Particle.DRIPPING_WATER, 40, 1.0, 1.0, 1.0, 0.0);
        context.fx().playSound(player, Sound.WEATHER_RAIN, 1.0f, 1.0f);
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

