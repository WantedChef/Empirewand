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

public class ExplosionWave implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("explosion-wave.values.range", 8.0);
        double coneAngle = spells.getDouble("explosion-wave.values.cone-angle-degrees", 70.0);
        double damage = spells.getDouble("explosion-wave.values.damage", 6.0); // 3 hearts
        double knockbackStrength = spells.getDouble("explosion-wave.values.knockback-strength", 0.9);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Find entities in cone
        List<LivingEntity> targets = getEntitiesInCone(player, range, coneAngle);

        // Apply damage and knockback to targets
        for (LivingEntity target : targets) {
            if (target.equals(player) && !friendlyFire) continue;
            if (target.isDead() || !target.isValid()) continue;

            // Apply damage
            target.damage(damage, player);

            // Apply knockback
            Vector kbDirection = target.getLocation().toVector()
                .subtract(player.getLocation().toVector()).normalize();
            kbDirection = kbDirection.multiply(knockbackStrength).setY(0.4);
            target.setVelocity(kbDirection);

            // Effects
            context.fx().spawnParticles(target.getLocation(), Particle.EXPLOSION, 5, 0.2, 0.2, 0.2, 0.1);
        }

        // Create explosion effect at center
        Location center = player.getLocation();
        player.getWorld().createExplosion(center, 1.5F, false, false);

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
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

            // Check if entity is within cone angle
            Vector toEntityNormalized = toEntity.normalize();
            double angle = Math.toDegrees(playerDir.angle(toEntityNormalized));

            if (angle <= coneAngle / 2) {
                targets.add(living);
            }
        }

        return targets;
    }

    @Override
    public String getName() {
        return "explosion-wave";
    }

    @Override
    public String key() {
        return "explosion-wave";
    }

    @Override
    public Component displayName() {
        return Component.text("Explosion Wave");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}