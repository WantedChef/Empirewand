package com.example.empirewand.spell.implementation;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.entity.Player;

public class Leap implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        player.setVelocity(player.getLocation().getDirection().multiply(1.5));
        // TODO: Add sound/particle effects via FxService
    }

    @Override
    public String getName() {
        return "leap";
    }
}
