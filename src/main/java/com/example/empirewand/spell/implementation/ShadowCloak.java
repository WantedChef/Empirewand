package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class ShadowCloak implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        int duration = spells.getInt("shadow-cloak.values.duration-ticks", 120);
        int speedAmp = spells.getInt("shadow-cloak.values.speed-amplifier", 1);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, speedAmp, false, true));

        context.fx().spawnParticles(player.getLocation(), Particle.WITCH, 40, 0.6, 1.0, 0.6, 0.01);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, 0.5f);
    }

    @Override
    public String getName() {
        return "shadow-cloak";
    }

    @Override
    public String key() {
        return "shadow-cloak";
    }

    @Override
    public Component displayName() {
        return Component.text("Shadow Cloak");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
