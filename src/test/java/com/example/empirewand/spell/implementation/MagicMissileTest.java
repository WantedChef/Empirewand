package com.example.empirewand.spell.implementation;

import com.example.empirewand.EmpireWandTestBase;
import com.example.empirewand.core.ConfigService;
import com.example.empirewand.core.FxService;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import com.example.empirewand.core.config.ReadableConfig;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MagicMissile spell implementation.
 * Focuses on targeting, damage calculation, and FX calls.
 */
class MagicMissileTest extends EmpireWandTestBase {

    @Mock
    private Player mockPlayer;

    @Mock
    private LivingEntity mockTarget;

    @Mock
    private Location mockLocation;

    @Mock
    private World mockWorld;

    @Mock
    private ConfigService mockConfig;

    @Mock
    private FxService mockFx;

    @Mock
    private com.example.empirewand.EmpireWandPlugin mockEmpirePlugin;

    @Mock
    private org.bukkit.plugin.java.JavaPlugin mockJavaPlugin;

    @Mock
    private ReadableConfig mockSpellsConfig;

    @Mock
    private ReadableConfig mockMainConfig;

    @Mock
    private BukkitScheduler mockScheduler;

    @Mock
    private BukkitTask mockTask;

    private MagicMissile magicMissile;

    @BeforeEach
    void setUp() {
        magicMissile = new MagicMissile();

        // Setup mocks
        when(mockPlayer.getLocation()).thenReturn(mockLocation);
        when(mockPlayer.getWorld()).thenReturn(mockWorld);
        when(mockPlayer.getEyeLocation()).thenReturn(mockLocation);
        when(mockPlayer.getTargetEntity(20)).thenReturn(mockTarget);
        when(mockPlayer.isValid()).thenReturn(true);

        when(mockTarget.getEyeLocation()).thenReturn(mockLocation);
        when(mockTarget.getWorld()).thenReturn(mockWorld);
        when(mockTarget.isValid()).thenReturn(true);
        when(mockTarget.isDead()).thenReturn(false);

        when(mockConfig.getSpellsConfig()).thenReturn(mockSpellsConfig);
        when(mockConfig.getConfig()).thenReturn(mockMainConfig);

        // Default config values
        when(mockSpellsConfig.getDouble("magic-missile.values.damage-per-missile", 3.0)).thenReturn(3.0);
        when(mockSpellsConfig.getInt("magic-missile.values.missile-count", 3)).thenReturn(3);
        when(mockSpellsConfig.getInt("magic-missile.values.delay-ticks", 7)).thenReturn(7);
        when(mockSpellsConfig.getBoolean("magic-missile.flags.requires-los", true)).thenReturn(true);

        when(mockMainConfig.getBoolean("features.friendly-fire", false)).thenReturn(false);

        when(mockEmpirePlugin.getServer()).thenReturn(mock(org.bukkit.Server.class));
        when(mockEmpirePlugin.getServer().getScheduler()).thenReturn(mockScheduler);
        when(mockScheduler.runTaskTimer(eq(mockEmpirePlugin), any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(mockTask);
    }

    @Test
    void testMagicMissileKey() {
        // When & Then
        assertEquals("magic-missile", magicMissile.key());
    }

    @Test
    void testMagicMissileName() {
        // When & Then
        assertEquals("magic-missile", magicMissile.getName());
    }

    @Test
    void testMagicMissileDisplayName() {
        // When & Then
        assertEquals("Magic Missile", ((TextComponent) magicMissile.displayName()).content());
    }

    @Test
    void testMagicMissilePrereq() {
        // When
        var prereq = magicMissile.prereq();

        // Then
        assertTrue(prereq.canCast());
        assertEquals("", ((TextComponent) prereq.reason()).content());
    }

    @Test
    void testExecuteWithValidTarget() {
        // Given
        when(mockPlayer.hasLineOfSight(mockTarget)).thenReturn(true);

        // When
        magicMissile.execute(new com.example.empirewand.spell.SpellContext(
                mockEmpirePlugin, mockPlayer, mockConfig, mockFx));

        // Then
        verify(mockFx).playSound(mockPlayer, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
        verify(mockScheduler).runTaskTimer(eq(mockEmpirePlugin), any(Runnable.class), eq(0L), eq(7L));
    }

    @Test
    void testExecuteWithNoTarget() {
        // Given
        when(mockPlayer.getTargetEntity(20)).thenReturn(null);

        // When
        magicMissile.execute(new com.example.empirewand.spell.SpellContext(
                mockEmpirePlugin, mockPlayer, mockConfig, mockFx));

        // Then
        verify(mockFx).fizzle(mockPlayer);
        verify(mockScheduler, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testExecuteWithDeadTarget() {
        // Given
        when(mockTarget.isDead()).thenReturn(true);

        // When
        magicMissile.execute(new com.example.empirewand.spell.SpellContext(
                mockEmpirePlugin, mockPlayer, mockConfig, mockFx));

        // Then
        verify(mockFx).fizzle(mockPlayer);
        verify(mockScheduler, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testExecuteWithInvalidTarget() {
        // Given
        when(mockTarget.isValid()).thenReturn(false);

        // When
        magicMissile.execute(new com.example.empirewand.spell.SpellContext(
                mockEmpirePlugin, mockPlayer, mockConfig, mockFx));

        // Then
        verify(mockFx).fizzle(mockPlayer);
        verify(mockScheduler, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testExecuteWithBlockedLineOfSight() {
        // Given
        when(mockPlayer.hasLineOfSight(mockTarget)).thenReturn(false);

        // When
        magicMissile.execute(new com.example.empirewand.spell.SpellContext(
                mockEmpirePlugin, mockPlayer, mockConfig, mockFx));

        // Then
        verify(mockScheduler).runTaskTimer(eq(mockEmpirePlugin), any(Runnable.class), eq(0L), eq(7L));
        // The runnable will handle the blocked LOS internally
    }

    @Test
    void testExecuteWithCustomConfigValues() {
        // Given
        when(mockSpellsConfig.getDouble("magic-missile.values.damage-per-missile", 3.0)).thenReturn(5.0);
        when(mockSpellsConfig.getInt("magic-missile.values.missile-count", 3)).thenReturn(5);
        when(mockSpellsConfig.getInt("magic-missile.values.delay-ticks", 7)).thenReturn(10);
        when(mockPlayer.hasLineOfSight(mockTarget)).thenReturn(true);

        // When
        magicMissile.execute(new com.example.empirewand.spell.SpellContext(
                mockEmpirePlugin, mockPlayer, mockConfig, mockFx));

        // Then
        verify(mockScheduler).runTaskTimer(eq(mockEmpirePlugin), any(Runnable.class), eq(0L), eq(10L));
    }

    @Test
    void testExecuteWithFriendlyFireDisabled() {
        // Given
        Player mockTargetPlayer = mock(Player.class);
        when(mockPlayer.getTargetEntity(20)).thenReturn(mockTargetPlayer);
        when(mockTargetPlayer.isValid()).thenReturn(true);
        when(mockTargetPlayer.isDead()).thenReturn(false);
        when(mockTargetPlayer.getEyeLocation()).thenReturn(mockLocation);
        when(mockTargetPlayer.getWorld()).thenReturn(mockWorld);
        when(mockTargetPlayer.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(mockPlayer.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(mockPlayer.hasLineOfSight(mockTargetPlayer)).thenReturn(true);

        // When
        magicMissile.execute(new com.example.empirewand.spell.SpellContext(
                mockEmpirePlugin, mockPlayer, mockConfig, mockFx));

        // Then
        verify(mockScheduler).runTaskTimer(eq(mockEmpirePlugin), any(Runnable.class), eq(0L), eq(7L));
        // Damage will be applied in the runnable
    }

    @Test
    void testExecuteWithSelfTargetAndFriendlyFireDisabled() {
        // Given
        when(mockPlayer.getTargetEntity(20)).thenReturn(mockPlayer);
        when(mockPlayer.hasLineOfSight(mockPlayer)).thenReturn(true);
        when(mockMainConfig.getBoolean("features.friendly-fire", false)).thenReturn(false);

        // When
        magicMissile.execute(new com.example.empirewand.spell.SpellContext(
                mockEmpirePlugin, mockPlayer, mockConfig, mockFx));

        // Then
        verify(mockScheduler).runTaskTimer(eq(mockEmpirePlugin), any(Runnable.class), eq(0L), eq(7L));
        // Self-damage should be prevented
    }
}