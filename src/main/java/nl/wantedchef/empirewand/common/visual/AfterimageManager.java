package nl.wantedchef.empirewand.common.visual;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Maintains a small queue of recent player locations and renders fading
 * afterimage particles.
 * Designed for lightweight teleport/blink visual echoes.
 */
public class AfterimageManager {
    private final Deque<Afterimage> queue = new ArrayDeque<>();
    private final int maxSize;
    private final int lifetimeTicks;

    public AfterimageManager(int maxSize, int lifetimeTicks) {
        this.maxSize = Math.max(1, maxSize);
        this.lifetimeTicks = Math.max(1, lifetimeTicks);
    }

    public void record(Player player) {
        if (player == null)
            return;
        Location loc = player.getLocation().clone();
        queue.addFirst(new Afterimage(loc, lifetimeTicks));
        while (queue.size() > maxSize) {
            queue.removeLast();
        }
    }

    /**
     * Records an arbitrary location (e.g., pre/post teleport) for afterimage
     * rendering.
     */
    public void record(Location location) {
        if (location == null)
            return;
        queue.addFirst(new Afterimage(location.clone(), lifetimeTicks));
        while (queue.size() > maxSize) {
            queue.removeLast();
        }
    }

    public void tickRender() {
        Iterator<Afterimage> it = queue.iterator();
        while (it.hasNext()) {
            Afterimage a = it.next();
            a.ticksRemaining--;
            if (a.ticksRemaining <= 0) {
                it.remove();
                continue;
            }
            render(a);
        }
    }

    private void render(Afterimage a) {
        Location l = a.location;
        World w = l.getWorld();
        if (w == null)
            return;
        float alphaRatio = (float) a.ticksRemaining / lifetimeTicks;
        // Use dust with fading color (alpha simulated by reducing count & size)
        // Use a widely supported particle (CRIT_MAGIC) and scale count as fade proxy
        int count = Math.max(1, (int) Math.round(4 * alphaRatio));
        w.spawnParticle(Particle.CRIT, l.getX(), l.getY() + 1.0, l.getZ(), count, 0.25, 0.4, 0.25, 0.01);
    }

    /**
     * Clears all afterimages and resets the manager
     */
    public void clear() {
        queue.clear();
    }

    private static final class Afterimage {
        final Location location;
        int ticksRemaining;

        Afterimage(Location location, int ticksRemaining) {
            this.location = location;
            this.ticksRemaining = ticksRemaining;
        }
    }
}





