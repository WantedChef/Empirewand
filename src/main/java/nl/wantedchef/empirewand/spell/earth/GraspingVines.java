package nl.wantedchef.empirewand.spell.earth;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class GraspingVines extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Grasping Vines";
            this.description = "Roots a target briefly with conjured vines.";
            this.cooldown = Duration.ofSeconds(12);
            this.spellType = SpellType.EARTH;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new GraspingVines(this);
        }
    }

    private GraspingVines(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "grasping-vines";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        Entity targetEntity = player.getTargetEntity(10);
        if (!(targetEntity instanceof LivingEntity target)) {
            EmpireWandAPI.getService(nl.wantedchef.empirewand.api.service.EffectService.class).fizzle(player.getLocation());
            return null;
        }

        boolean hitPlayers = this.spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = this.spellConfig.getBoolean("flags.hit-mobs", true);
        if ((target instanceof Player && !hitPlayers) || (!(target instanceof Player) && !hitMobs)) {
            EmpireWandAPI.getService(nl.wantedchef.empirewand.api.service.EffectService.class).fizzle(player.getLocation());
            return null;
        }

        int duration = this.spellConfig.getInt("values.duration-ticks", 60);
        int amplifier = this.spellConfig.getInt("values.slow-amplifier", 250);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amplifier));

        EmpireWandAPI.getService(nl.wantedchef.empirewand.api.service.EffectService.class).spawnParticles(
                target.getLocation().add(0, 0.2, 0), Particle.SPORE_BLOSSOM_AIR, 18, 0.6, 0.4, 0.6, 0.0);
        EmpireWandAPI.getService(nl.wantedchef.empirewand.api.service.EffectService.class).playSound(target.getLocation(),
                Sound.BLOCK_VINE_STEP, 0.8f, 0.9f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}
