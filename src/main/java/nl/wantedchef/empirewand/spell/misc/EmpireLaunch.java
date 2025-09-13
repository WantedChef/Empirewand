package nl.wantedchef.empirewand.spell.misc;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
// Removed slow falling imports; spell now launches a target entity without applying effects
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class EmpireLaunch extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empire Launch";
            this.description = "Launches the caster into the air with enhanced mobility and fall protection.";
            this.cooldown = java.time.Duration.ofSeconds(10);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EmpireLaunch(this);
        }
    }

    private EmpireLaunch(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "empire-launch";
    }

    @Override
    public Component displayName() {
        return Component.text("Empire Launch").color(TextColor.color(255, 215, 0));
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();

        double powerUp = spellConfig.getDouble("values.power", 1.8);
        double forwardMult = spellConfig.getDouble("values.forward-multiplier", 0.6);
        int range = spellConfig.getInt("values.range", 25);

        var target = caster.getTargetEntity(range);
        if (target == null || target.equals(caster)) {
            context.fx().fizzle(caster);
            return null;
        }

        Vector dir = caster.getEyeLocation().getDirection().normalize();
        Vector launchVector = dir.multiply(forwardMult).setY(powerUp);
        target.setVelocity(launchVector);

        Location launchLocation = target.getLocation().add(0, 0.5, 0);
        context.fx().spawnParticles(launchLocation, Particle.CLOUD, 18, 0.6, 0.4, 0.6, 0.02);
        context.fx().playSound(launchLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
