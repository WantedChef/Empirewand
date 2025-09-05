package com.example.empirewand.spell.implementation;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class EtherealForm implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        player.setCollidable(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
        // TODO: Add logic to cancel fall damage in EntityListener

        new BukkitRunnable() {
            @Override
            public void run() {
                player.setCollidable(true);
            }
        }.runTaskLater(context.plugin(), 100L); // 5 seconds

        // TODO: Add particle/sound effects
    }

    @Override
    public String getName() {
        return "ethereal-form";
    }
}
