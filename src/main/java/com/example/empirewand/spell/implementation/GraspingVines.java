package com.example.empirewand.spell.implementation;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GraspingVines implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        Entity targetEntity = player.getTargetEntity(10);
        if (!(targetEntity instanceof LivingEntity target)) {
            // Fizzle feedback when no valid target in sight
            context.fx().fizzle(player.getLocation());
            return;
        }

        // Root/slow the target sharply for a short time
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 250));
        // TODO: Add particle/sound effects for vines grasping
    }

    @Override
    public String getName() {
        return "grasping-vines";
    }
}
