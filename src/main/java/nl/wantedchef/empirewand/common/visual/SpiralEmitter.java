package nl.wantedchef.empirewand.common.visual;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

/**
 * Emits a vertical spiral (helix) of particles; can be used for sandstorm or
 * frost nova swirl.
 */
public final class SpiralEmitter {
    private SpiralEmitter() {
    }

    public static void emit(Location base, double height, int turns, int stepsPerTurn, double radius,
            Particle particle) {
        if (base == null || base.getWorld() == null)
            return;
        World w = base.getWorld();
        int totalSteps = turns * stepsPerTurn;
        double dy = height / totalSteps;
        for (int i = 0; i < totalSteps; i++) {
            double progress = (double) i / stepsPerTurn; // turn-based progress
            double angle = progress * 2 * Math.PI;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            w.spawnParticle(particle, base.getX() + x, base.getY() + (i * dy), base.getZ() + z, 1, 0, 0, 0, 0);
        }
    }
}





