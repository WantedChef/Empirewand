package com.example.empirewand.spell.implementation;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class Heal implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        player.setHealth(Math.min(maxHealth, currentHealth + 8.0));
        // TODO: Add particle/sound effects
    }

    @Override
    public String getName() {
        return "heal";
    }
}
