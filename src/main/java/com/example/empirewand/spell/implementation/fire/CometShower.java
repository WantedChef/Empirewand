package com.example.empirewand.spell.implementation.fire;

import org.bukkit.Location;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class CometShower implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        int cometCount = spells.getInt("comet-shower.values.comet-count", 5);
        double radius = spells.getDouble("comet-shower.values.radius", 8.0);
        double explosionYield = spells.getDouble("comet-shower.values.yield", 2.6);
        int delayTicks = spells.getInt("comet-shower.values.delay-ticks", 6);

        // Find target location
        Location targetLocation = findTargetLocation(caster);

        // Launch comets in sequence
        new BukkitRunnable() {
            private int launched = 0;

            @Override
            public void run() {
                if (launched >= cometCount) {
                    this.cancel();
                    return;
                }

                                launchComet(caster, targetLocation, radius, explosionYield, context);
                launched++;
            }
        }.runTaskTimer(context.plugin(), 0L, delayTicks);
    }

    private Location findTargetLocation(Player caster) {
        // Use ray trace to find target location
        var rayTrace = caster.getWorld().rayTraceBlocks(
            caster.getEyeLocation(),
            caster.getEyeLocation().getDirection(),
            25.0
        );

        if (rayTrace != null && rayTrace.getHitPosition() != null) {
            return rayTrace.getHitPosition().toLocation(caster.getWorld());
        } else {
            // Fallback to location in front of caster
            return caster.getEyeLocation().add(caster.getEyeLocation().getDirection().multiply(20));
        }
    }

    private void launchComet(Player caster, Location center, double radius, double explosionYield, SpellContext context) {
        // Generate random position within circle
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);

        // Spawn location above the target area
        Location spawnLoc = new Location(center.getWorld(), x, center.getY() + 12, z);

        // Create large fireball
        LargeFireball comet = caster.getWorld().spawn(spawnLoc, LargeFireball.class);
        comet.setYield((float) explosionYield);
        comet.setIsIncendiary(false);

        // Set velocity downward
        Vector velocity = new Vector(0, -0.8, 0);
        comet.setVelocity(velocity);

        // Visual effects
        context.fx().spawnParticles(spawnLoc, org.bukkit.Particle.FLAME, 10, 0.2, 0.2, 0.2, 0.1);
        context.fx().spawnParticles(spawnLoc, org.bukkit.Particle.LAVA, 5, 0.1, 0.1, 0.1, 0.05);
    }

    @Override
    public String getName() {
        return "comet-shower";
    }

    @Override
    public String key() {
        return "comet-shower";
    }

    @Override
    public Component displayName() {
        return Component.text("Comet Shower");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
