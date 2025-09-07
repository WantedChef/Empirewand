package com.example.empirewand.spell.implementation.control;

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

public class Confuse implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("confuse.values.range", 15.0);
        double damage = spells.getDouble("confuse.values.damage", 6.0); // 3 hearts
        int duration = spells.getInt("confuse.values.duration-ticks", 80); // 4 seconds
        int slowAmplifier = spells.getInt("confuse.values.slow-amplifier", 2); // Slowness III
        boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);

        // Get target
        var targetEntity = player.getTargetEntity((int) range);
        if (!(targetEntity instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return;
        }

        // Apply nausea effect (confusion)
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 0, false, true));

        // Apply slow effect
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, slowAmplifier, false, true));

        // Deal damage
        if (!(target instanceof Player p && !friendlyFire && p.getUniqueId().equals(player.getUniqueId()))) {
            target.damage(damage, player);
        }

        // Effects
        context.fx().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0f, 0.5f);
        context.fx().spawnParticles(target.getLocation(), Particle.SMOKE, 20, 0.3, 0.3, 0.3, 0.1);

        // Cast sound
        context.fx().playSound(player, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
    }

    @Override
    public String getName() {
        return "confuse";
    }

    @Override
    public String key() {
        return "confuse";
    }

    @Override
    public Component displayName() {
        return Component.text("Confuse");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
