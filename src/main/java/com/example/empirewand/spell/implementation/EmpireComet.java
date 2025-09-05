package com.example.empirewand.spell.implementation;

import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class EmpireComet implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double explosionYield = spells.getDouble("empire-comet.values.yield", 3.5);
        double speed = spells.getDouble("empire-comet.values.speed", 0.8);

        // Launch large fireball (comet)
        LargeFireball comet = caster.launchProjectile(LargeFireball.class);
        comet.setYield((float) explosionYield);
        comet.setIsIncendiary(false); // No fire spread

        // Set velocity
        Vector direction = caster.getEyeLocation().getDirection();
        comet.setVelocity(direction.multiply(speed));

        // Visual effects
        context.fx().spawnParticles(caster.getEyeLocation(), org.bukkit.Particle.FLAME, 20, 0.3, 0.3, 0.3, 0.1);
        context.fx().spawnParticles(caster.getEyeLocation(), org.bukkit.Particle.SMOKE, 15, 0.2, 0.2, 0.2, 0.05);
        context.fx().playSound(caster.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
    }

    @Override
    public String getName() {
        return "empire-comet";
    }

    @Override
    public String key() {
        return "empire-comet";
    }

    @Override
    public Component displayName() {
        return Component.text("Empire Comet");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}