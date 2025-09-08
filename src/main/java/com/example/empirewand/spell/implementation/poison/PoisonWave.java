package com.example.empirewand.spell.implementation.poison;

import com.example.empirewand.api.ConfigService;
import com.example.empirewand.api.EffectService;
import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PoisonWave extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Poison Wave";
            this.description = "Releases a cone of poisonous gas that damages enemies.";
            this.manaCost = 15;
            this.cooldown = Duration.ofMillis(3000);
            this.spellType = SpellType.POISON;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new PoisonWave(this);
        }
    }

    private PoisonWave(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "poison-wave";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 6.0);
        double coneAngle = spellConfig.getDouble("values.cone-angle-degrees", 60.0);
        int poisonDuration = spellConfig.getInt("values.poison-duration-ticks", 140);
        int poisonAmplifier = spellConfig.getInt("values.poison-amplifier", 0);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig().getBoolean("features.friendly-fire", false);

        List<LivingEntity> targets = getEntitiesInCone(player, range, coneAngle);

        for (LivingEntity target : targets) {
            if (target.equals(player) && !friendlyFire)
                continue;
            if (target.isDead() || !target.isValid())
                continue;

            // Fixed: Separated particle spawning and potion effect
            context.fx().spawnParticles(target.getLocation(), Particle.SMOKE, 10, 0.2, 0.2, 0.2, 0.1);

            // Fixed: Properly formatted potion effect application
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, poisonAmplifier, false, true));
        }

        // Fixed: Using proper service accessor
        context.fx().playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.8f, 0.8f);

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private List<LivingEntity> getEntitiesInCone(Player player, double range, double coneAngle) {
        List<LivingEntity> targets = new ArrayList<>();
        Vector playerDir = player.getEyeLocation().getDirection().normalize();
        double angleRadians = Math.toRadians(coneAngle / 2.0);

        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.equals(player) || entity.getLocation().distanceSquared(player.getLocation()) > range * range)
                continue;

            Vector toEntity = entity.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector())
                    .normalize();
            if (playerDir.angle(toEntity) <= angleRadians) {
                targets.add(entity);
            }
        }
        return targets;
    }
}