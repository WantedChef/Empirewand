package com.example.empirewand.spell.implementation.misc;

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
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates an explosive wave that damages and knocks back entities in a cone.
 * Features configurable range, angle, damage falloff, and enhanced visual
 * effects.
 */
public class ExplosionWave implements Spell {

    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Validate caster
        if (player == null || !player.isValid()) {
            return;
        }

        // Load configuration with defaults
        var spellsConfig = context.config().getSpellsConfig();
        double range = spellsConfig.getDouble("explosion-wave.values.range", 8.0);
        double coneAngle = spellsConfig.getDouble("explosion-wave.values.cone-angle-degrees", 70.0);
        double baseDamage = spellsConfig.getDouble("explosion-wave.values.damage", 6.0); // 3 hearts
        double knockbackStrength = spellsConfig.getDouble("explosion-wave.values.knockback-strength", 0.9);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);
        double particleCount = spellsConfig.getDouble("explosion-wave.values.particle-count", 30);
        float explosionPower = (float) spellsConfig.getDouble("explosion-wave.values.explosion-power", 1.5);
        boolean createSound = spellsConfig.getBoolean("explosion-wave.values.create-sound", true);

        // Validate configuration values
        range = Math.max(3, Math.min(range, 25)); // Clamp range between 3 and 25 blocks
        coneAngle = Math.max(15, Math.min(coneAngle, 180)); // Clamp angle between 15 and 180 degrees
        baseDamage = Math.max(1, Math.min(baseDamage, 20)); // Clamp damage between 0.5 and 10 hearts
        knockbackStrength = Math.max(0.1, Math.min(knockbackStrength, 3.0)); // Clamp knockback between 0.1 and 3.0

        // Find entities in cone using optimized algorithm
        List<LivingEntity> targets = getEntitiesInCone(player, range, coneAngle);

        // Apply effects to targets
        for (LivingEntity target : targets) {
            if (target.equals(player) && !friendlyFire)
                continue;
            if (target.isDead() || !target.isValid())
                continue;

            // Calculate distance-based damage falloff
            Location targetLoc = target.getLocation();
            if (targetLoc == null)
                continue;

            Location playerLoc = player.getLocation();
            if (playerLoc == null)
                continue;

            double distance = playerLoc.distance(targetLoc);
            double damageMultiplier = 1.0 - (distance / range);
            double actualDamage = baseDamage * damageMultiplier;

            // Apply damage
            target.damage(actualDamage, player);

            // Apply knockback with directional force
            Vector kbDirection = targetLoc.toVector()
                    .subtract(playerLoc.toVector()).normalize();

            // Add upward component for knockback
            kbDirection = kbDirection.multiply(knockbackStrength * damageMultiplier)
                    .setY(0.4 + (0.2 * damageMultiplier));

            target.setVelocity(kbDirection);

            // Enhanced effects at target location
            // Impact particles
            context.fx().spawnParticles(
                    targetLoc,
                    Particle.EXPLOSION,
                    8,
                    0.3, 0.3, 0.3,
                    0.15);

            // Debris particles
            context.fx().spawnParticles(
                    targetLoc,
                    Particle.CAMPFIRE_COSY_SMOKE, // Replace SMOKE_NORMAL
                    5,
                    0.2, 0.2, 0.2,
                    0.1);

            // Impact sound
            context.fx().playSound(
                    targetLoc,
                    Sound.ENTITY_GENERIC_EXPLODE,
                    0.8f,
                    1.2f);
        }

        // Create explosion effect at center
        Location center = player.getLocation();
        if (center != null && center.getWorld() != null) {
            center.getWorld().createExplosion(center, explosionPower, false, false);
        }

        // Main explosion visual effects
        if (center != null) {
            context.fx().spawnParticles(
                    center,
                    Particle.FLAME, // Replace EXPLOSION_HUGE
                    (int) particleCount,
                    1.0, 1.0, 1.0,
                    0.3);

            // Shockwave effect
            context.fx().spawnParticles(
                    center,
                    Particle.CAMPFIRE_COSY_SMOKE, // Replace SMOKE_NORMAL
                    (int) (particleCount * 1.5),
                    2.0, 2.0, 2.0,
                    0.2);

            // Cast sound
            if (createSound) {
                context.fx().playSound(
                        center,
                        Sound.ENTITY_GENERIC_EXPLODE,
                        1.0f,
                        1.0f);
            }
        }
    }

    /**
     * Optimized cone detection algorithm for finding entities in a cone.
     */
    private List<LivingEntity> getEntitiesInCone(Player player, double range, double coneAngle) {
        List<LivingEntity> targets = new ArrayList<>();
        Location playerLoc = player.getEyeLocation();
        if (playerLoc == null)
            return targets;

        Vector playerDir = playerLoc.getDirection().normalize();

        // Get nearby entities in a sphere first (more efficient than checking all
        // entities)
        var nearbyEntities = player.getWorld().getNearbyEntities(playerLoc, range, range, range);

        for (var entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity living))
                continue;

            Vector toEntity = living.getEyeLocation().toVector().subtract(playerLoc.toVector());
            double distance = toEntity.length();

            if (distance > range || distance < 0.5)
                continue; // Skip entities too close or too far

            // Check if entity is within cone angle using dot product (more efficient)
            Vector toEntityNormalized = toEntity.normalize();
            double dotProduct = playerDir.dot(toEntityNormalized);
            double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dotProduct))));

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
        return Component.text("Explosion Wave")
                .color(TextColor.color(255, 69, 0)); // RedOrange color
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
