package nl.wantedchef.empirewand.spell.dark.control;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.base.PrereqInterface;
import nl.wantedchef.empirewand.spell.base.Spell;
import nl.wantedchef.empirewand.spell.base.SpellContext;
import nl.wantedchef.empirewand.spell.base.SpellType;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * MephiThrow - An extremely powerful throw that launches the target with high
 * force
 * in the caster's view direction, designed to feel like "really being thrown"
 * and to enforce terrain/fall damage.
 */
public class MephiThrow extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Mephi Throw";
            this.description = "Extremely powerful throw that launches target with high force. Designed to feel like 'really being thrown' with terrain/fall damage.";
            this.cooldown = Duration.ofSeconds(10);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new MephiThrow(this);
        }
    }

    private MephiThrow(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "mephi-throw";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();
        double range = spellConfig.getDouble("values.range", 15.0);
        double throwPower = spellConfig.getDouble("values.throw-power", 3.0);
        boolean applyFallDamage = spellConfig.getBoolean("values.apply-fall-damage", true);

        // Get target entity
        Entity targetEntity = caster.getTargetEntity((int) range);
        if (!(targetEntity instanceof LivingEntity)) {
            context.fx().showError(caster, "no-target");
            return null;
        }

        LivingEntity target = (LivingEntity) targetEntity;

        // Check if target is valid
        if (target.isDead() || !target.isValid()) {
            context.fx().showError(caster, "invalid-target");
            return null;
        }

        // Calculate throw direction (caster's view direction)
        Vector throwDirection = caster.getLocation().getDirection();

        // Apply throw velocity
        Vector throwVelocity = throwDirection.multiply(throwPower).setY(throwPower * 0.5); // Add upward component
        target.setVelocity(throwVelocity);

        // Apply fall damage vulnerability if enabled
        if (applyFallDamage) {
            // Mark target for fall damage (implementation would depend on how the plugin
            // handles this)
            target.setFallDistance(10.0f); // Set initial fall distance to make any fall damage more impactful
        }

        // Spawn throw effect particles
        spawnThrowEffects(context, target.getLocation(), caster.getLocation());

        // Play sound effects
        context.fx().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        context.fx().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);

        caster.sendMessage("§5⚡ §7Mephi Throw launched: " + target.getName());
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    private void spawnThrowEffects(SpellContext context, Location targetLoc, Location casterLoc) {
        World world = targetLoc.getWorld();

        // Create explosion particles at target location
        context.fx().spawnParticles(targetLoc, Particle.EXPLOSION, 15, 0.5, 0.5, 0.5, 0.1);

        // Create smoke particles
        context.fx().spawnParticles(targetLoc, Particle.SMOKE, 20, 1.0, 1.0, 1.0, 0.05);

        // Create soul fire particles
        context.fx().spawnParticles(targetLoc, Particle.SOUL_FIRE_FLAME, 10, 0.8, 0.8, 0.8, 0.03);

        // Create a particle trail from caster to target
        Vector direction = targetLoc.toVector().subtract(casterLoc.toVector()).normalize();
        Location currentLoc = casterLoc.clone();
        for (int i = 0; i < 10; i++) {
            currentLoc.add(direction.multiply(0.5));
            world.spawnParticle(Particle.CRIT, currentLoc, 2, 0.1, 0.1, 0.1, 0.02);
        }
    }
}