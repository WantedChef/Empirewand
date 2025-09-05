package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class Tornado implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double radius = spells.getDouble("tornado.values.radius", 6.0);
        double lift = spells.getDouble("tornado.values.lift-velocity", 0.9);
        int levitationDur = spells.getInt("tornado.values.levitation-duration-ticks", 40);
        int levitationAmp = spells.getInt("tornado.values.levitation-amplifier", 0);
        double damage = spells.getDouble("tornado.values.damage", 4.0);
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        for (var e : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living.equals(player) && !friendlyFire) continue;
            if (living.isDead() || !living.isValid()) continue;

            Vector up = new Vector(0, Math.max(0.1, lift), 0);
            living.setVelocity(living.getVelocity().add(up));
            living.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, levitationDur, levitationAmp, false, true));
            living.damage(damage, player);

            context.fx().spawnParticles(living.getLocation(), Particle.CLOUD, 20, 0.3, 0.6, 0.3, 0.01);
            context.fx().spawnParticles(living.getLocation(), Particle.SWEEP_ATTACK, 5, 0.2, 0.2, 0.2, 0.0);
        }

        context.fx().playSound(player, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.7f);
    }

    @Override
    public String getName() { return "tornado"; }
    @Override
    public String key() { return "tornado"; }
    @Override
    public Component displayName() { return Component.text("Tornado"); }
    @Override
    public Prereq prereq() { return new Prereq(true, Component.text("")); }
}

