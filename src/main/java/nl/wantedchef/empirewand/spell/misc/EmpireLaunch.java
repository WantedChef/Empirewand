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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

        double power = spellConfig.getDouble("values.power", 1.8);
        int slowFallingDuration = spellConfig.getInt("values.slow-falling-duration", 80);

        Vector launchVector = caster.getEyeLocation().getDirection().normalize().multiply(0.4).setY(power);
        caster.setVelocity(launchVector);
        caster.addPotionEffect(
                new PotionEffect(PotionEffectType.SLOW_FALLING, slowFallingDuration, 0, false, true, true));

        Location launchLocation = caster.getLocation().add(0, 0.5, 0);
        context.fx().spawnParticles(launchLocation, Particle.CAMPFIRE_COSY_SMOKE, 30, 0.5, 0.5, 0.5, 0.2);
        context.fx().playSound(launchLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
