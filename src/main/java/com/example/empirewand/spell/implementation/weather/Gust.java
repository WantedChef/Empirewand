package com.example.empirewand.spell.implementation.weather;

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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Gust extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Gust";
            this.description = "Creates a powerful gust of wind that knocks back enemies.";
            this.manaCost = 10;
            this.cooldown = Duration.ofMillis(2000);
            this.spellType = SpellType.WEATHER;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Gust(this);
        }
    }

    private Gust(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "gust";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 10.0);
        double angle = spellConfig.getDouble("values.angle", 70.0);
        double knockback = spellConfig.getDouble("values.knockback", 1.0);
        double damage = spellConfig.getDouble("values.damage", 0.0);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig().getBoolean("features.friendly-fire", false);

        List<LivingEntity> targets = getEntitiesInCone(player, range, angle);
        for (LivingEntity target : targets) {
            if (target.equals(player) && !friendlyFire)
                continue;

            if (damage > 0)
                target.damage(damage, player);
            Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize()
                    .multiply(knockback).setY(0.25);
            target.setVelocity(push);
            context.fx().spawnParticles(target.getLocation(), Particle.SWEEP_ATTACK, 5, 0.2, 0.2, 0.2, 0.0);
        }
        context.fx().playSound(player, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.2f);

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