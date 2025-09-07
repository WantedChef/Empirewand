package com.example.empirewand.spell.implementation.movement;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import com.example.empirewand.visual.RingRenderer;
import com.example.empirewand.visual.Afterimages;
import net.kyori.adventure.text.Component;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" }, justification = "Defensive world/location checks kept for safety; SpotBugs may consider some redundant given Bukkit guarantees.")
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

        // Departure implosion visuals
        Location from = player.getLocation().clone();
        Afterimages.get(); // ensure initialized
        if (Afterimages.get() != null)
            Afterimages.get().record(from);
        context.fx().spawnParticles(from, Particle.PORTAL, 35, 0.5, 0.8, 0.5, 0.15);
        new BukkitRunnable() {
            double r = 2.2;
            int steps = 0;

            @Override
            public void run() {
                if (steps > 6 || from.getWorld() == null) {
                    cancel();
                    return;
                }
                RingRenderer.renderRing(from, r, 30, Particle.CRIT);
                r -= 0.3;
                steps++;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Teleport
        player.teleport(targetLoc);

        // Arrival expanding ring + afterimage
        Location to = targetLoc.clone();
        if (Afterimages.get() != null)
            Afterimages.get().record(to);
        context.fx().spawnParticles(to, Particle.PORTAL, 45, 0.6, 1.0, 0.6, 0.2);
        new BukkitRunnable() {
            double r = 0.3;
            int steps = 0;

            @Override
            public void run() {
                if (steps > 6 || to.getWorld() == null) {
                    cancel();
                    return;
                }
                RingRenderer.renderRing(to, r, 32, Particle.CRIT);
                r += 0.35;
                steps++;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
        context.fx().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    private Location getTargetLocation(Player player, double range, boolean requiresLineOfSight) {
        BlockIterator iterator = new BlockIterator(player, (int) range);
        Location targetLoc = null;

        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getType().isSolid()) {
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
        org.bukkit.World world = player.getWorld();
        if (targetLoc.getY() < world.getMinHeight())
            targetLoc.setY(world.getMinHeight());
        if (targetLoc.getY() > world.getMaxHeight())
            targetLoc.setY(world.getMaxHeight());

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
