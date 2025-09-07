package com.example.empirewand.spell.implementation.fire;

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

public class FlameWave implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("flame-wave.values.range", 6.0);
        double coneAngle = spells.getDouble("flame-wave.values.cone-angle-degrees", 60.0);
        double damage = spells.getDouble("flame-wave.values.damage", 4.0); // 2 hearts
        int fireTicks = spells.getInt("flame-wave.values.fire-ticks", 80); // 4 seconds
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Find entities in cone
        List<LivingEntity> targets = getEntitiesInCone(player, range, coneAngle);

        // Apply fire damage and burn to targets
        for (LivingEntity target : targets) {
            if (target.equals(player) && !friendlyFire) continue;
            if (target.isDead() || !target.isValid()) continue;

            // Apply damage
            target.damage(damage, player);

            // Set target on fire
            target.setFireTicks(fireTicks);

            // Effects
            context.fx().spawnParticles(target.getLocation(), Particle.FLAME, 10, 0.2, 0.2, 0.2, 0.05);
            context.fx().spawnParticles(target.getLocation(), Particle.SMOKE, 5, 0.2, 0.2, 0.2, 0.1);
        }

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.8f);
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
        return "flame-wave";
    }

    @Override
    public String key() {
        return "flame-wave";
    }

    @Override
    public Component displayName() {
        return Component.text("Flame Wave");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
