package nl.wantedchef.empirewand.spell.movement.flight;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Leap extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Leap";
            this.description = "Launch yourself forward and upward with jetpack-like power.";
            this.cooldown = java.time.Duration.ZERO; // No cooldown
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Leap(this);
        }
    }

    private Leap(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "leap";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Get configuration values
        double forwardMultiplier = spellConfig.getDouble("leap.forward-multiplier", 2.0);
        double verticalBoost = spellConfig.getDouble("leap.vertical-boost", 1.2);
        double maxVerticalVelocity = spellConfig.getDouble("leap.max-vertical-velocity", 3.0);

        // Get player's current direction and velocity
        org.bukkit.util.Vector direction = player.getLocation().getDirection().normalize();
        org.bukkit.util.Vector currentVelocity = player.getVelocity();

        // Create jetpack-style launch vector
        org.bukkit.util.Vector launchVector = direction.multiply(forwardMultiplier);

        // Add vertical boost - more if player is already in the air (jetpack effect)
        double verticalComponent = verticalBoost;
        boolean isOnGround = player.getLocation().subtract(0, 0.1, 0).getBlock().getType().isSolid();
        if (!isOnGround && currentVelocity.getY() < maxVerticalVelocity) {
            // Jetpack mode - add extra vertical boost when already airborne
            verticalComponent += spellConfig.getDouble("leap.jetpack-boost", 0.8);
        }

        launchVector.setY(Math.min(verticalComponent, maxVerticalVelocity));

        // Apply the launch vector
        player.setVelocity(launchVector);

        // Enhanced visual and audio effects
        context.fx().spawnParticles(player.getLocation(), Particle.CLOUD, 20, 0.5, 0.2, 0.5, 0.05);
        context.fx().spawnParticles(player.getLocation(), Particle.CRIT, 8, 0.3, 0.1, 0.3, 0.1);

        // Jetpack-style sounds
        context.fx().playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.6f, 1.4f);
        context.fx().playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.3f, 1.8f);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
