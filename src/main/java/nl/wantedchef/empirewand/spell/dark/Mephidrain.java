package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Mephidrain - A dark spell that drains health from nearby enemies and transfers it to the caster,
 * representing Mephidantes' power.
 */
public class Mephidrain extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Mephidrain";
            this.description = "Drain health from nearby enemies and transfer it to yourself.";
            this.cooldown = Duration.ofSeconds(12);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Mephidrain(this);
        }
    }

    private Mephidrain(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "mephidrain";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 6.0);
        double damage = spellConfig.getDouble("values.damage", 8.0);
        double healPercentage = spellConfig.getDouble("values.heal-percentage", 0.5);

        // Find nearby entities
        for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius,
                radius)) {
            if (entity instanceof org.bukkit.entity.LivingEntity living && !living.equals(player)
                    && !living.isDead() && living.isValid()) {

                // Damage the entity
                living.damage(damage, player);

                // Heal the player
                double healAmount = damage * healPercentage;
                double maxHealth = living
                        .getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
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

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
