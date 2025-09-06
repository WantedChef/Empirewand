package com.example.empirewand.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.ConfigService;
import com.example.empirewand.core.FxService;

/**
 * Lightweight tests for SpellContext record immutability & factory helpers.
 * Focuses on defensive cloning behavior and null-safe construction.
 */
class SpellContextTest {

    @Test
    void targetedFactoryClonesLocation() {
        EmpireWandPlugin plugin = mock(EmpireWandPlugin.class);
        Player caster = mock(Player.class);
        ConfigService config = mock(ConfigService.class);
        FxService fx = mock(FxService.class);
        LivingEntity target = mock(LivingEntity.class);
        World world = mock(World.class);
        Location loc = new Location(world, 10, 64, 10);
        when(target.getLocation()).thenReturn(loc);

        SpellContext ctx = SpellContext.targeted(plugin, caster, config, fx, target, "spell-x");
        assertNotNull(ctx.targetLocation());
        assertNotSame(loc, ctx.targetLocation(), "Target location must be cloned defensively");
    }

    @Test
    void locationFactoryClonesProvidedLocation() {
        EmpireWandPlugin plugin = mock(EmpireWandPlugin.class);
        Player caster = mock(Player.class);
        ConfigService config = mock(ConfigService.class);
        FxService fx = mock(FxService.class);
        World world = mock(World.class);
        Location provided = new Location(world, 1, 2, 3);

        SpellContext ctx = SpellContext.location(plugin, caster, config, fx, provided, "spell-y");
        assertNotNull(ctx.targetLocation());
        assertNotSame(provided, ctx.targetLocation());
        assertEquals(1, ctx.targetLocation().getX());
        assertEquals(2, ctx.targetLocation().getY());
        assertEquals(3, ctx.targetLocation().getZ());

        // Mutate original and ensure context copy unaffected
        provided.setX(9);
        assertEquals(1, ctx.targetLocation().getX(), "Context must not reflect external mutation");
    }

    @Test
    void accessorReturnsCloneEachCall() {
        EmpireWandPlugin plugin = mock(EmpireWandPlugin.class);
        Player caster = mock(Player.class);
        ConfigService config = mock(ConfigService.class);
        FxService fx = mock(FxService.class);
        World world = mock(World.class);
        Location provided = new Location(world, 4, 5, 6);
        SpellContext ctx = SpellContext.location(plugin, caster, config, fx, provided, "spell-z");

        Location first = ctx.targetLocation();
        Location second = ctx.targetLocation();
        assertNotSame(first, second, "Each accessor call should clone the location");
    }

    @Test
    void convenienceConstructorCreatesEmptyContext() {
        EmpireWandPlugin plugin = mock(EmpireWandPlugin.class);
        Player caster = mock(Player.class);
        ConfigService config = mock(ConfigService.class);
        FxService fx = mock(FxService.class);

        SpellContext ctx = new SpellContext(plugin, caster, config, fx);
        assertNull(ctx.target());
        assertNull(ctx.targetLocation());
        assertNull(ctx.spellKey());
    }

    @Test
    void nullLocationHandledSafely() {
        EmpireWandPlugin plugin = mock(EmpireWandPlugin.class);
        Player caster = mock(Player.class);
        ConfigService config = mock(ConfigService.class);
        FxService fx = mock(FxService.class);

        SpellContext ctx = SpellContext.location(plugin, caster, config, fx, null, "spell-null");
        assertNull(ctx.targetLocation());
    }
}
