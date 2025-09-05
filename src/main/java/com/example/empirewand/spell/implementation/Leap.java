package com.example.empirewand.spell.implementation;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class Leap implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        var spells = context.config().getSpellsConfig();
        double multiplier = spells.getDouble("leap.values.velocity-multiplier", 1.5);
        double verticalBoost = spells.getDouble("leap.values.vertical-boost", 0.0);
        var dir = player.getLocation().getDirection().normalize().multiply(multiplier);
        dir.setY(dir.getY() + verticalBoost);
        player.setVelocity(dir);
        // Burst of particles + whoosh
        context.fx().spawnParticles(player.getLocation(), Particle.CLOUD, 16, 0.3, 0.1, 0.3, 0.02);
        context.fx().playSound(player, Sound.ENTITY_RABBIT_JUMP, 0.8f, 1.2f);
    }

    @Override
    public String getName() {
        return "leap";
    }
}
