package com.example.empirewand.spell.implementation.misc;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * Launches the caster into the air with enhanced mobility and fall protection.
 * Features configurable power, duration, and visual effects.
 */
public class EmpireLaunch implements Spell {

    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Validate caster
        if (caster == null || !caster.isValid()) {
            return;
        }

        // Load configuration with defaults
        var spellsConfig = context.config().getSpellsConfig();
        double power = spellsConfig.getDouble("empire-launch.values.power", 1.8);
        int slowFallingDuration = spellsConfig.getInt("empire-launch.values.slow-falling-duration", 80);
        double particleCount = spellsConfig.getDouble("empire-launch.values.particle-count", 30);
        float particleSpread = (float) spellsConfig.getDouble("empire-launch.values.particle-spread", 0.5);
        float soundPitch = (float) spellsConfig.getDouble("empire-launch.values.sound-pitch", 0.8);

        // Validate configuration values
        power = Math.max(0.1, Math.min(power, 5.0)); // Clamp power between 0.1 and 5.0
        slowFallingDuration = Math.max(20, Math.min(slowFallingDuration, 600)); // Clamp duration between 1s and 30s
        particleCount = Math.max(10, Math.min(particleCount, 100)); // Clamp particles between 10 and 100

        // Calculate launch vector with eye direction
        Vector direction = caster.getEyeLocation().getDirection();
        Vector launchVector = direction.normalize()
                .multiply(0.4) // Horizontal boost
                .setY(power); // Vertical boost

        // Apply launch with safety check
        if (!caster.isSwimming() && !caster.isInWater()) { // Replace deprecated isOnGround()
            caster.setVelocity(launchVector);
        }

        // Apply slow falling effect for fall damage mitigation
        caster.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                slowFallingDuration,
                0,
                false,
                true,
                true));

        // Enhanced visual effects
        Location launchLocation = caster.getLocation().add(0, 0.5, 0);

        // Main particle effect
        context.fx().spawnParticles(
                launchLocation,
                Particle.CAMPFIRE_COSY_SMOKE, // Replace SMOKE_NORMAL
                (int) particleCount,
                particleSpread,
                particleSpread,
                particleSpread,
                0.2);

        // Additional trail particles
        context.fx().spawnParticles(
                launchLocation,
                Particle.CLOUD,
                (int) (particleCount * 0.5),
                particleSpread * 0.8,
                particleSpread * 0.8,
                particleSpread * 0.8,
                0.1);

        // Sound effects with spatial audio
        context.fx().playSound(
                launchLocation,
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.0f,
                soundPitch);
    }

    @Override
    public String getName() {
        return "empire-launch";
    }

    @Override
    public String key() {
        return "empire-launch";
    }

    @Override
    public Component displayName() {
        return Component.text("Empire Launch")
                .color(TextColor.color(255, 215, 0)); // Gold color
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
