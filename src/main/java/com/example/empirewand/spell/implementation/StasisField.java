package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class StasisField implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("stasis-field.values.radius", 6.0);
        int duration = spells.getInt("stasis-field.values.duration-ticks", 80);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living))
                continue;
            if (living.equals(player) && !friendlyFire)
                continue;
            if (living.isDead() || !living.isValid())
                continue;

            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 250, false, true));
            living.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 5, false, true));
            context.fx().spawnParticles(living.getLocation(), Particle.ENCHANT, 20, 0.4, 0.7, 0.4, 0.0);
        }

        context.fx().playSound(player, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.8f);
    }

    @Override
    public String getName() {
        return "stasis-field";
    }

    @Override
    public String key() {
        return "stasis-field";
    }

    @Override
    public Component displayName() {
        return Component.text("Stasis Field");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
