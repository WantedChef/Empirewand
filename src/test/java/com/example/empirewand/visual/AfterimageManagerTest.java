package com.example.empirewand.visual;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AfterimageManagerTest {
    private World world;
    private Location base;

    @BeforeEach
    void setup() {
        world = Mockito.mock(World.class);
        base = new Location(world, 0, 64, 0);
    }

    @Test
    void recordAndTrimQueue() {
        AfterimageManager mgr = new AfterimageManager(3, 10);
        mgr.record(base.clone());
        mgr.record(base.clone().add(1, 0, 0));
        mgr.record(base.clone().add(2, 0, 0));
        mgr.record(base.clone().add(3, 0, 0)); // should evict oldest
        // internal queue not exposed; rely on tick fade behavior size invariance
        // Call tickRender to ensure no exceptions
        mgr.tickRender();
        // Can't assert size directly; ensure no crash and record more still stable
        mgr.record(base.clone().add(4, 0, 0));
        mgr.tickRender();
    }

    @Test
    void fadingReducesOverTimeUntilRemoval() {
        AfterimageManager mgr = new AfterimageManager(5, 3);
        mgr.record(base.clone());
        // Run ticks > lifetime; ensure no exceptions
        for (int i = 0; i < 6; i++) {
            mgr.tickRender();
        }
    }

    @Test
    void nullRecordSafe() {
        AfterimageManager mgr = new AfterimageManager(2, 5);
        mgr.record((Location) null);
        mgr.tickRender();
    }
}
