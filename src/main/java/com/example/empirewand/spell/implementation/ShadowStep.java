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
import com.example.empirewand.visual.Afterimages;

/**
 * ShadowStep: short-range blink that leaves fading shadow echoes along the
 * path.
 * Purely visual aside from standard teleport effect (range & safety checks
 * similar to Teleport).
 */
public class ShadowStep implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        var spells = context.config().getSpellsConfig();
        double range = spells.getDouble("shadow-step.values.range", 10.0);
        boolean requiresLOS = spells.getBoolean("shadow-step.flags.requires-los", true);
        int echoSamples = spells.getInt("shadow-step.visual.echo-samples", 6);

        Location target = getTargetLocation(player, range, requiresLOS);
        if (target == null || !isLocationSafe(target)) {
            context.fx().fizzle(player);
            return;
        }

        Location from = player.getLocation().clone();
        // Generate intermediate sample points and record into global afterimages
        // manager
        if (Afterimages.get() != null) {
            for (int i = 0; i < echoSamples; i++) {
                double t = (i + 1) / (double) (echoSamples + 1);
                Afterimages.get().record(lerp(from, target, t));
            }
            Afterimages.get().record(from);
        }

        // Departure shadow swirl
        context.fx().spawnParticles(from, Particle.CLOUD, 25, 0.4, 0.6, 0.4, 0.02);
        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.4f);

        player.teleport(target);

        // Arrival burst
        context.fx().spawnParticles(target, Particle.CLOUD, 30, 0.5, 0.8, 0.5, 0.05);
        context.fx().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.7f);

        // Afterimages manager handles fade passively; no local runnable needed
    }

    private Location lerp(Location a, Location b, double t) {
        return new Location(a.getWorld(),
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t,
                a.getZ() + (b.getZ() - a.getZ()) * t,
                a.getYaw() + (b.getYaw() - a.getYaw()) * (float) t,
                a.getPitch() + (b.getPitch() - a.getPitch()) * (float) t);
    }

    private Location getTargetLocation(Player player, double range, boolean requiresLineOfSight) {
        BlockIterator iterator = new BlockIterator(player, (int) range);
        Location targetLoc = null;
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getType().isSolid()) {
                targetLoc = block.getLocation();
                targetLoc.setY(targetLoc.getY() + 1);
                break;
            }
        }
        if (targetLoc == null) {
            targetLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(range));
        }
        var world = player.getWorld();
        if (world != null) {
            targetLoc.setY(Math.max(world.getMinHeight(), Math.min(world.getMaxHeight(), targetLoc.getY())));
        }
        return targetLoc;
    }

    private boolean isLocationSafe(Location location) {
        var feet = location.getBlock();
        var head = location.clone().add(0, 1, 0).getBlock();
        var ground = location.clone().add(0, -1, 0).getBlock();
        if (feet.getType().isSolid() || head.getType().isSolid())
            return false;
        return ground.getType().isSolid();
    }

    @Override
    public String getName() {
        return "shadow-step";
    }

    @Override
    public String key() {
        return "shadow-step";
    }

    @Override
    public Component displayName() {
        return Component.text("Shadow Step");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
