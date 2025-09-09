package nl.wantedchef.empirewand.spell.weather;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import java.time.Duration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Tornado extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Tornado";
            this.description = "Creates a powerful whirlwind that lifts and damages enemies.";
            this.cooldown = Duration.ofMillis(8000);
            this.spellType = SpellType.WEATHER;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Tornado(this);
        }
    }

    private Tornado(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "tornado";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double radius = spellConfig.getDouble("values.radius", 6.0);
        double lift = spellConfig.getDouble("values.lift-velocity", 0.9);
        int levitationDur = spellConfig.getInt("values.levitation-duration-ticks", 40);
        int levitationAmp = spellConfig.getInt("values.levitation-amplifier", 0);
        double damage = spellConfig.getDouble("values.damage", 4.0);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        for (var e : player.getWorld().getNearbyLivingEntities(player.getLocation(), radius)) {
            if (e.equals(player) && !friendlyFire)
                continue;

            e.setVelocity(e.getVelocity().add(new Vector(0, lift, 0)));
            e.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, levitationDur, levitationAmp, false, true));
            e.damage(damage, player);

            context.fx().spawnParticles(e.getLocation(), Particle.CLOUD, 20, 0.3, 0.6, 0.3, 0.01);
            context.fx().spawnParticles(e.getLocation(), Particle.SWEEP_ATTACK, 5, 0.2, 0.2, 0.2, 0.0);
        }

        context.fx().playSound(player, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.7f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
