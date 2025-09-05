package com.example.empirewand.spell.implementation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class Teleport implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("teleport.values.range", 15.0);
        boolean requiresLineOfSight = spells.getBoolean("teleport.flags.requires-los", true);

        // Get target location
        Location targetLoc = getTargetLocation(player, range, requiresLineOfSight);
        if (targetLoc == null) {
            context.fx().fizzle(player);
            return;
        }

        // Check if location is safe
        if (!isLocationSafe(targetLoc)) {
            context.fx().fizzle(player);
            return;
        }

        // Cast particles and sound
        context.fx().spawnParticles(player.getLocation(), Particle.PORTAL, 20, 0.3, 0.3, 0.3, 0.1);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Teleport
        player.teleport(targetLoc);

        // Arrival effects
        context.fx().spawnParticles(targetLoc, Particle.PORTAL, 20, 0.3, 0.3, 0.3, 0.1);
        context.fx().playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    private Location getTargetLocation(Player player, double range, boolean requiresLineOfSight) {
        BlockIterator iterator = new BlockIterator(player, (int) range);
        Location targetLoc = null;

        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getType().isSolid()) {
                // Found solid block, teleport to the block in front of it
                targetLoc = block.getLocation();
                targetLoc.setY(targetLoc.getY() + 1); // Stand on top of the block
                break;
            }
        }

        if (targetLoc == null) {
            // No solid block found within range, teleport to max range in line of sight
            targetLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(range));
        }

        // Ensure location is within world bounds
        if (targetLoc.getY() < 0) targetLoc.setY(0);
        if (targetLoc.getY() > targetLoc.getWorld().getMaxHeight()) {
            targetLoc.setY(targetLoc.getWorld().getMaxHeight());
        }

        return targetLoc;
    }

    private boolean isLocationSafe(Location location) {
        Block feetBlock = location.getBlock();
        Block headBlock = location.clone().add(0, 1, 0).getBlock();
        Block groundBlock = location.clone().add(0, -1, 0).getBlock();

        // Check if feet and head positions are not solid
        if (feetBlock.getType().isSolid() || headBlock.getType().isSolid()) {
            return false;
        }

        // Check if there's ground to stand on
        return groundBlock.getType().isSolid();
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String key() {
        return "teleport";
    }

    @Override
    public Component displayName() {
        return Component.text("Teleport");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}