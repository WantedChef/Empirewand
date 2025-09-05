package com.example.empirewand.spell.implementation;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class EmpireLaunch implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double power = spells.getDouble("empire-launch.values.power", 1.8);
        int slowFallingDuration = spells.getInt("empire-launch.values.slow-falling-duration", 80);

        // Calculate launch vector
        Vector direction = caster.getEyeLocation().getDirection();
        Vector launchVector = direction.normalize().multiply(0.4).setY(power);

        // Apply launch
        caster.setVelocity(launchVector);

        // Apply slow falling for fall damage mitigation
        caster.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, slowFallingDuration, 0, false, true));

        // Visual effects
        context.fx().spawnParticles(caster.getLocation(), org.bukkit.Particle.CLOUD, 30, 0.5, 0.5, 0.5, 0.2);
        context.fx().playSound(caster.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
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
        return Component.text("Empire Launch");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}