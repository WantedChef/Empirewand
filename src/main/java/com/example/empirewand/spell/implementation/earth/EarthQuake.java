package com.example.empirewand.spell.implementation.earth;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class EarthQuake implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("earth-quake.values.radius", 7.0);
        double knockbackStrength = spells.getDouble("earth-quake.values.knockback-strength", 1.1);
        double verticalBoost = spells.getDouble("earth-quake.values.vertical-boost", 0.35);

        // Get all nearby entities
        var nearbyEntities = caster.getWorld().getNearbyEntities(caster.getLocation(), radius, radius, radius);

        for (var entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) {
                continue;
            }

            // Skip caster
            if (living.equals(caster)) {
                continue;
            }

            // Calculate knockback vector from caster to target
            Vector casterToTarget = living.getLocation().toVector().subtract(caster.getLocation().toVector());
            Vector knockback = casterToTarget.normalize().multiply(knockbackStrength);
            knockback.setY(verticalBoost);

            // Apply knockback
            living.setVelocity(knockback);

            // Visual effects
            context.fx().spawnParticles(living.getLocation(), org.bukkit.Particle.CRIT, 10, 0.3, 0.3, 0.3, 0.1);
        }

        // Ground effects at caster location
        context.fx().spawnParticles(caster.getLocation(), org.bukkit.Particle.EXPLOSION, 50, radius, 0.5, radius, 0.1);

        // Sound effects
        context.fx().playSound(caster.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        context.fx().playSound(caster.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
    }

    @Override
    public String getName() {
        return "earth-quake";
    }

    @Override
    public String key() {
        return "earth-quake";
    }

    @Override
    public Component displayName() {
        return Component.text("Earth Quake");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
