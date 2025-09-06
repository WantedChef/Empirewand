package com.example.empirewand.spell.implementation;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class EmpireLevitate implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        int duration = spells.getInt("empire-levitate.values.duration-ticks", 60); // 3 seconds
        int amplifier = spells.getInt("empire-levitate.values.amplifier", 0); // Levitation I

        // Find target in line of sight
        var target = caster.getTargetEntity(15);
        if (!(target instanceof LivingEntity living)) {
            return; // No valid target
        }

        // Check if target is a boss (optional - could be configurable)
        // Note: getMaxHealth() is deprecated, using a simple check for now
        var maxHealthAttr = living.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            double maxHealth = maxHealthAttr.getValue();
            if (maxHealth > 100) {
                return; // Don't affect bosses
            }
        }

        // Apply levitation effect
        living.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, amplifier, false, true));

        // Visual effects
        context.fx().spawnParticles(living.getLocation(), org.bukkit.Particle.CLOUD, 20, 0.5, 0.5, 0.5, 0.1);
        context.fx().spawnParticles(living.getLocation(), org.bukkit.Particle.ENCHANT, 15, 0.3, 0.3, 0.3, 0.05);

        // Sound effect
        context.fx().playSound(living.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
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
        return Component.text("Empire Levitate");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}