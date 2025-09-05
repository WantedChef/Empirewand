package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class BlinkStrike implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("blink-strike.values.range", 15.0);
        double behind = spells.getDouble("blink-strike.values.behind-distance", 1.5);
        double damage = spells.getDouble("blink-strike.values.damage", 10.0);
        boolean requiresLos = spells.getBoolean("blink-strike.flags.requires-los", true);

        var looked = player.getTargetEntity((int) range);
        if (!(looked instanceof LivingEntity target) || target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return;
        }

        Location targetLoc = target.getLocation();
        Vector backDir = targetLoc.getDirection().normalize().multiply(-behind);
        Location blinkLoc = targetLoc.clone().add(backDir);
        blinkLoc.setYaw(targetLoc.getYaw());
        blinkLoc.setPitch(targetLoc.getPitch());

        if (!isLocationSafe(blinkLoc)) {
            // Try a side-step if behind isn't safe
            Vector side = new Vector(-backDir.getZ(), 0, backDir.getX()).normalize().multiply(1.2);
            Location alt = targetLoc.clone().add(side);
            if (!isLocationSafe(alt)) {
                context.fx().fizzle(player);
                return;
            }
            blinkLoc = alt;
        }

        // FX and teleport
        context.fx().spawnParticles(player.getLocation(), Particle.PORTAL, 20, 0.3, 0.3, 0.3, 0.1);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        player.teleport(blinkLoc);
        context.fx().spawnParticles(blinkLoc, Particle.PORTAL, 20, 0.3, 0.3, 0.3, 0.1);

        // Backstab
        target.damage(damage, player);
        context.fx().spawnParticles(target.getLocation(), Particle.CRIT, 15, 0.2, 0.2, 0.2, 0.01);
    }

    private boolean isLocationSafe(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().add(0, -1, 0).getBlock();
        if (feet.getType().isSolid() || head.getType().isSolid()) return false;
        return ground.getType().isSolid();
    }

    @Override
    public String getName() { return "blink-strike"; }

    @Override
    public String key() { return "blink-strike"; }

    @Override
    public Component displayName() { return Component.text("Blink Strike"); }

    @Override
    public Prereq prereq() { return new Prereq(true, Component.text("")); }
}

