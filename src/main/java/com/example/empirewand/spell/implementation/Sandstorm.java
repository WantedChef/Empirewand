package com.example.empirewand.spell.implementation;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class Sandstorm implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("sandstorm.values.radius", 6.0);
        int blindDur = spells.getInt("sandstorm.values.blind-duration-ticks", 80);
        int slowDur = spells.getInt("sandstorm.values.slow-duration-ticks", 100);
        int slowAmp = spells.getInt("sandstorm.values.slow-amplifier", 1);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        BlockData sand = Material.SAND.createBlockData();

        for (var e : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living.equals(player) && !friendlyFire) continue;
            if (living.isDead() || !living.isValid()) continue;

            living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindDur, 0, false, true));
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDur, slowAmp, false, true));
            context.fx().spawnParticles(living.getLocation(), Particle.BLOCK, 20, 0.5, 0.8, 0.5, 0.01, sand);
        }

        context.fx().playSound(player, Sound.BLOCK_SAND_BREAK, 0.8f, 0.8f);
    }

    @Override
    public String getName() { return "sandstorm"; }
    @Override
    public String key() { return "sandstorm"; }
    @Override
    public Component displayName() { return Component.text("Sandstorm"); }
    @Override
    public Prereq prereq() { return new Prereq(true, Component.text("")); }
}

