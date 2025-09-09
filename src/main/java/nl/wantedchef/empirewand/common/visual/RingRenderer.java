package nl.wantedchef.empirewand.common.visual;

import java.util.function.BiConsumer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Utility to render expanding or static particle rings around a center.
 * Stateless; all parameters provided per invocation.
 */
public final class RingRenderer {
    private RingRenderer() {
    }

    public static void renderRing(Location center, double radius, int particles, Particle particle) {
        if (center == null || center.getWorld() == null)
            return;
        World w = center.getWorld();
        for (int i = 0; i < particles; i++) {
            double angle = (2 * Math.PI * i) / particles;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            w.spawnParticle(particle, center.getX() + x, center.getY(), center.getZ() + z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Renders a ring but also passes each point to a callback for custom logic
     * (e.g., block sampling or secondary particles).
     */
    public static void renderRing(Location center, double radius, int particles,
            BiConsumer<Location, Vector> pointConsumer) {
        if (center == null || center.getWorld() == null)
            return;
        for (int i = 0; i < particles; i++) {
            double angle = (2 * Math.PI * i) / particles;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location point = center.clone().add(x, 0, z);
            pointConsumer.accept(point, new Vector(x, 0, z));
        }
    }
}





