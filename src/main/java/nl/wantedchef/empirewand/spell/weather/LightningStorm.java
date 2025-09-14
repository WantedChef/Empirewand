package nl.wantedchef.empirewand.spell.weather;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Calls down a barrage of lightning bolts around the caster's location.
 *
 * @author WantedChef
 */
public class LightningStorm extends Spell<Void> {

    /**
     * Builder for {@link LightningStorm}.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a builder for the spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lightning Storm";
            this.description = "Strikes lightning randomly around you.";
            this.cooldown = Duration.ofMillis(7000);
            this.spellType = SpellType.WEATHER;
        }

        /**
         * Builds the spell instance.
         *
         * @return a new {@link LightningStorm}
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new LightningStorm(this);
        }
    }

    private LightningStorm(Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String key() {
        return "lightningstorm";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Strikes multiple lightning bolts at random positions near the caster.
     *
     * @param context the spell context
     * @return always {@code null}
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        World world = player.getWorld();
        double radius = spellConfig.getDouble("values.radius", 8.0);
        int strikes = spellConfig.getInt("values.strike-count", 3);
        for (int i = 0; i < strikes; i++) {
            double dx = ThreadLocalRandom.current().nextDouble(-radius, radius);
            double dz = ThreadLocalRandom.current().nextDouble(-radius, radius);
            Location loc = player.getLocation().clone().add(dx, 0, dz);
            world.strikeLightning(loc);
        }
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
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

