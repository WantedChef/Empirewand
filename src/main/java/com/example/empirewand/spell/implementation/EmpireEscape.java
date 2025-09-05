package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class EmpireEscape implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double maxRange = spells.getDouble("empire-escape.values.max-range", 16.0);
        int speedDuration = spells.getInt("empire-escape.values.speed-duration", 40);

        // Ray trace for safe destination
        RayTraceResult rayTrace = caster.getWorld().rayTraceBlocks(
            caster.getEyeLocation(),
            caster.getEyeLocation().getDirection(),
            maxRange
        );

        Location destination;
        if (rayTrace != null && rayTrace.getHitPosition() != null) {
            // Use ray trace hit position
            destination = rayTrace.getHitPosition().toLocation(caster.getWorld());
            // Adjust to be just in front of the block
            Vector direction = caster.getEyeLocation().getDirection().normalize();
            destination = destination.subtract(direction.multiply(0.5));
        } else {
            // Fallback: teleport forward
            Vector direction = caster.getEyeLocation().getDirection().normalize();
            destination = caster.getLocation().add(direction.multiply(6.0));
        }

        // Ensure destination is safe (not inside blocks)
        destination = findSafeLocation(destination);

        // Teleport player
        caster.teleport(destination);

        // Reset fall damage
        caster.setFallDistance(0);

        // Apply speed boost
        caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDuration, 0, false, true));

        // Visual effects
        context.fx().spawnParticles(caster.getLocation(), org.bukkit.Particle.SMOKE, 20, 0.3, 0.3, 0.3, 0.1);
        context.fx().playSound(caster.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
    }

    private Location findSafeLocation(Location location) {
        // Check if location is safe
        if (location.getBlock().isEmpty() && location.clone().add(0, 1, 0).getBlock().isEmpty()) {
            return location;
        }

        // Try to find a safe spot nearby
        for (int y = 1; y <= 3; y++) {
            Location testLoc = location.clone().add(0, y, 0);
            if (testLoc.getBlock().isEmpty() && testLoc.clone().add(0, 1, 0).getBlock().isEmpty()) {
                return testLoc;
            }
        }

        // If no safe spot found, return original location (player will take damage)
        return location;
    }

    @Override
    public String getName() {
        return "empire-escape";
    }

    @Override
    public String key() {
        return "empire-escape";
    }

    @Override
    public Component displayName() {
        return Component.text("Empire Escape");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}