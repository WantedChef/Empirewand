package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class VoidSwap implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("void-swap.values.range", 15.0);
        boolean requiresLos = spells.getBoolean("void-swap.flags.requires-los", true);

        var looked = player.getTargetEntity((int) range);
        if (!(looked instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return;
        }

        Location a = player.getLocation();
        Location b = target.getLocation();
        if (!isLocationSafe(a) || !isLocationSafe(b)) {
            context.fx().fizzle(player);
            return;
        }

        // FX
        context.fx().spawnParticles(a, Particle.PORTAL, 25, 0.4, 0.8, 0.4, 0.05);
        context.fx().spawnParticles(b, Particle.PORTAL, 25, 0.4, 0.8, 0.4, 0.05);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Swap
        player.teleport(b);
        target.teleport(a);
    }

    private boolean isLocationSafe(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().add(0, -1, 0).getBlock();
        if (feet.getType().isSolid() || head.getType().isSolid()) return false;
        return ground.getType().isSolid();
    }

    @Override
    public String getName() { return "void-swap"; }
    @Override
    public String key() { return "void-swap"; }
    @Override
    public Component displayName() { return Component.text("Void Swap"); }
    @Override
    public Prereq prereq() { return new Prereq(true, Component.text("")); }
}

