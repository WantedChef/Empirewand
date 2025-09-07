package com.example.empirewand.spell.implementation.misc;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * Levitates a target entity in the caster's line of sight.
 * Features configurable duration, amplifier, boss protection, and enhanced
 * visual effects.
 */
public class EmpireLevitate implements Spell {

    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Validate caster
        if (caster == null || !caster.isValid()) {
            return;
        }

        // Load configuration with defaults
        var spellsConfig = context.config().getSpellsConfig();
        int duration = spellsConfig.getInt("empire-levitate.values.duration-ticks", 60); // 3 seconds
        int amplifier = spellsConfig.getInt("empire-levitate.values.amplifier", 0); // Levitation I
        double maxRange = spellsConfig.getDouble("empire-levitate.values.max-range", 15.0);
        double bossHealthThreshold = spellsConfig.getDouble("empire-levitate.values.boss-health-threshold", 100.0);
        boolean affectPlayers = spellsConfig.getBoolean("empire-levitate.values.affect-players", false);
        double particleCount = spellsConfig.getDouble("empire-levitate.values.particle-count", 20);
        float soundPitch = (float) spellsConfig.getDouble("empire-levitate.values.sound-pitch", 1.2);

        // Validate configuration values
        duration = Math.max(20, Math.min(duration, 600)); // Clamp between 1s and 30s
        amplifier = Math.max(0, Math.min(amplifier, 4)); // Clamp amplifier between 0 and 4
        maxRange = Math.max(5, Math.min(maxRange, 50)); // Clamp range between 5 and 50 blocks

        // Find target using raytrace for better accuracy
        RayTraceResult rayTrace = caster.rayTraceEntities((int) maxRange, false);
        if (rayTrace == null || rayTrace.getHitEntity() == null) {
            return; // No valid target in range
        }

        LivingEntity target = (LivingEntity) rayTrace.getHitEntity();

        // Validate target
        if (target == null || !target.isValid() || target.isDead()) {
            return;
        }

        // Check if target is a boss (configurable threshold)
        if (isBoss(target, bossHealthThreshold)) {
            return; // Don't affect bosses
        }

        // Check if target is a player (optional)
        if (!affectPlayers && target instanceof Player) {
            return; // Don't affect other players
        }

        // Check line of sight
        if (!hasLineOfSight(caster, target)) {
            return; // No direct line of sight
        }

        // Apply levitation effect
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.LEVITATION,
                duration,
                amplifier,
                false,
                true,
                true));

        // Enhanced visual effects at target location
        Location targetLocation = target.getLocation().add(0, 1, 0);

        // Main particle cloud effect
        context.fx().spawnParticles(
                targetLocation,
                Particle.CLOUD,
                (int) particleCount,
                0.5, 0.5, 0.5,
                0.1);

        // Magical enchant particles for levitation effect
        context.fx().spawnParticles(
                targetLocation,
                Particle.ENCHANT,
                (int) (particleCount * 0.75),
                0.3, 0.3, 0.3,
                0.05);

        // Additional sparkle particles
        context.fx().spawnParticles(
                targetLocation,
                Particle.CRIT, // Replace invalid particle types
                (int) (particleCount * 0.5),
                0.2, 0.2, 0.2,
                0.02);

        // Sound effect with spatial audio
        context.fx().playSound(
                targetLocation,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                1.0f,
                soundPitch);

        // Optional: Add a subtle glow effect
        context.fx().spawnParticles(
                targetLocation,
                Particle.GLOW,
                10,
                0.1, 0.1, 0.1,
                0.01);
    }

    /**
     * Checks if an entity is considered a boss based on health threshold.
     */
    private boolean isBoss(LivingEntity entity, double healthThreshold) {
        var maxHealthAttr = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr == null) {
            return false;
        }
        return maxHealthAttr.getValue() > healthThreshold;
    }

    /**
     * Checks if there's a clear line of sight between two entities.
     */
    private boolean hasLineOfSight(Player caster, LivingEntity target) {
        Vector start = caster.getEyeLocation().toVector();
        Vector end = target.getEyeLocation().toVector();
        Vector direction = end.clone().subtract(start).normalize();

        // Simple line of sight check
        RayTraceResult result = caster.getWorld().rayTraceBlocks(
                caster.getEyeLocation(),
                direction,
                start.distance(end),
                org.bukkit.FluidCollisionMode.NEVER,
                false);

        return result == null || result.getHitEntity() == target;
    }

    @Override
    public String getName() {
        return "empire-levitate";
    }

    @Override
    public String key() {
        return "empire-levitate";
    }

    @Override
    public Component displayName() {
        return Component.text("Empire Levitate")
                .color(TextColor.color(138, 43, 226)); // BlueViolet color
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
