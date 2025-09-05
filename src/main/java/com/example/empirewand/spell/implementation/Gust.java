package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class Gust implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("gust.values.range", 10.0);
        double angle = spells.getDouble("gust.values.angle", 70.0);
        double knockback = spells.getDouble("gust.values.knockback", 1.0);
        double damage = spells.getDouble("gust.values.damage", 0.0);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        List<LivingEntity> targets = getEntitiesInCone(player, range, angle);
        for (LivingEntity target : targets) {
            if (target.equals(player) && !friendlyFire) continue;
            if (target.isDead() || !target.isValid()) continue;

            if (damage > 0) target.damage(damage, player);

            Vector dir = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            Vector push = dir.multiply(knockback).setY(0.25);
            target.setVelocity(push);

            context.fx().spawnParticles(target.getLocation(), Particle.SWEEP_ATTACK, 5, 0.2, 0.2, 0.2, 0.0);
            context.fx().spawnParticles(target.getLocation(), Particle.CLOUD, 15, 0.3, 0.3, 0.3, 0.01);
        }

        context.fx().playSound(player, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.2f);
    }

    private List<LivingEntity> getEntitiesInCone(Player player, double range, double coneAngle) {
        List<LivingEntity> targets = new ArrayList<>();
        Location playerLoc = player.getEyeLocation();
        Vector playerDir = playerLoc.getDirection().normalize();
        for (var entity : player.getWorld().getNearbyEntities(playerLoc, range, range, range)) {
            if (!(entity instanceof LivingEntity living)) continue;
            Vector toEntity = living.getEyeLocation().toVector().subtract(playerLoc.toVector());
            double distance = toEntity.length();
            if (distance > range) continue;
            Vector toEntityNormalized = toEntity.normalize();
            double angle = Math.toDegrees(playerDir.angle(toEntityNormalized));
            if (angle <= coneAngle / 2) targets.add(living);
        }
        return targets;
    }

    @Override
    public String getName() { return "gust"; }
    @Override
    public String key() { return "gust"; }
    @Override
    public Component displayName() { return Component.text("Gust"); }
    @Override
    public Prereq prereq() { return new Prereq(true, Component.text("")); }
}

