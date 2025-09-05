package com.example.empirewand.spell.implementation;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

        var spells = context.config().getSpellsConfig();
        boolean hitPlayers = spells.getBoolean("grasping-vines.flags.hit-players", true);
        boolean hitMobs = spells.getBoolean("grasping-vines.flags.hit-mobs", true);
        boolean isPlayer = target instanceof Player;
        if ((isPlayer && !hitPlayers) || (!isPlayer && !hitMobs)) {
            context.fx().fizzle(player.getLocation());
            return;
        }

        // Root/slow the target sharply for a short time (configurable)
        int duration = spells.getInt("grasping-vines.values.duration-ticks", 60);
        int amplifier = spells.getInt("grasping-vines.values.slow-amplifier", 250);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amplifier));
        // Vines grasp FX
        context.fx().spawnParticles(target.getLocation().add(0, 0.2, 0), Particle.SPORE_BLOSSOM_AIR, 18, 0.6, 0.4, 0.6, 0.0);
        context.fx().playSound(target.getLocation(), Sound.BLOCK_VINE_STEP, 0.8f, 0.9f);
    }

    @Override
    public String getName() {
        return "grasping-vines";
    }

    @Override
    public String key() {
        return "grasping-vines";
    }

    @Override
    public Component displayName() {
        return Component.text("Grasping Vines");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
