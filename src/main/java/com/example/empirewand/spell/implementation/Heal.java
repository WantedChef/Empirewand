package com.example.empirewand.spell.implementation;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class Heal implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        double healAmount = context.config().getSpellsConfig().getDouble("heal.values.heal-amount", 8.0);
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        player.setHealth(Math.min(maxHealth, currentHealth + healAmount));
        // Gentle heal FX
        context.fx().spawnParticles(player.getLocation().add(0, 1.0, 0), Particle.HEART, 8, 0.4, 0.4, 0.4, 0.01);
        context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
    }

    @Override
    public String getName() {
        return "heal";
    }
}
