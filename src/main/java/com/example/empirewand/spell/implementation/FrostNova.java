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

public class FrostNova implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("frost-nova.values.radius", 5.0);
        double damage = spells.getDouble("frost-nova.values.damage", 6.0);
        int slowDuration = spells.getInt("frost-nova.values.slow-duration-ticks", 100);
        int slowAmplifier = spells.getInt("frost-nova.values.slow-amplifier", 2);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living))
                continue;
            if (living.equals(player) && !friendlyFire)
                continue;
            if (living.isDead() || !living.isValid())
                continue;

            living.damage(damage, player);
            living.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, true));
            context.fx().spawnParticles(living.getLocation(), Particle.SNOWFLAKE, 10, 0.3, 0.3, 0.3, 0.05);
        }

        context.fx().impact(player.getLocation(), Particle.CLOUD, 30, Sound.BLOCK_SNOW_BREAK, 0.8f, 1.2f);
    }

    @Override
    public String getName() {
        return "frost-nova";
    }

    @Override
    public String key() {
        return "frost-nova";
    }

    @Override
    public Component displayName() {
        return Component.text("Frost Nova");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
