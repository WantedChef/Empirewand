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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Bombards nearby foes with icy hail that bruises and drives them downward.
 *
 * @author WantedChef
 */
public class HailStorm extends Spell<Void> {

    /**
     * Builder for {@link HailStorm}.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a builder for the spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Hail Storm";
            this.description = "Pelts enemies with chunks of ice.";
            this.cooldown = Duration.ofMillis(6000);
            this.spellType = SpellType.WEATHER;
        }

        /**
         * Builds the spell instance.
         *
         * @return a new {@link HailStorm}
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new HailStorm(this);
        }
    }

    private HailStorm(Builder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String key() {
        return "hailstorm";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Damages and knocks down nearby entities with icy particles.
     *
     * @param context the spell context
     * @return always {@code null}
     */
    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        double radius = spellConfig.getDouble("values.radius", 5.0);
        double damage = spellConfig.getDouble("values.damage", 2.0);
        double push = spellConfig.getDouble("values.push", 0.4);
        for (LivingEntity e : player.getWorld().getNearbyLivingEntities(player.getLocation(), radius)) {
            if (e.equals(player)) {
                continue;
            }
            e.damage(damage, player);
            e.setVelocity(new Vector(0, -push, 0));
            context.fx().spawnParticles(e.getLocation(), Particle.ITEM_SNOWBALL, 10, 0.3, 0.8, 0.3, 0.05);
        }
        context.fx().playSound(player, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
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

