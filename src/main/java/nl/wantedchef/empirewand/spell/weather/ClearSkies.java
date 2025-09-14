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
 * Clears rain and thunder, bringing back warm sunlight around the caster.
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
            this.description = "Dispels rain and thunder.";
            this.cooldown = Duration.ofMillis(5000);
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
     * Stops weather in the caster's world and plays a sunny effect.
     *
     * @param context the spell context
     * @return always {@code null}
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        World world = player.getWorld();
        world.setStorm(false);
        world.setThundering(false);
        context.fx().spawnParticles(player.getLocation(), Particle.CLOUD, 20, 1.0, 1.0, 1.0, 0.0);
        context.fx().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
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

