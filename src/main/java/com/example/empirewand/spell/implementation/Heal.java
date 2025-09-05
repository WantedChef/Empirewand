package com.example.empirewand.spell.implementation;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class Heal implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        double healAmount = context.config().getSpellsConfig().getDouble("heal.values.heal-amount", 8.0);
        var maxAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxAttr != null ? maxAttr.getValue() : 20.0;
        double currentHealth = player.getHealth();
        player.setHealth(Math.min(maxHealth, currentHealth + healAmount));
        // Gentle heal FX
        var loc = player.getLocation();
        if (loc != null) {
            context.fx().spawnParticles(loc.add(0, 1.0, 0), Particle.HEART, 8, 0.4, 0.4, 0.4, 0.01);
        }
        context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
    }

    @Override
    public String getName() {
        return "heal";
    }

    @Override
    public String key() {
        return "heal";
    }

    @Override
    public Component displayName() {
        return Component.text("Heal");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
