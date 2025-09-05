package com.example.empirewand.spell.implementation;

import com.example.empirewand.EmpireWandTestBase;
import com.example.empirewand.core.ConfigService;
import com.example.empirewand.core.FxService;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Heal spell implementation.
 * Focuses on deterministic logic: health calculations, clamping, and FX calls.
 */
class HealSpellTest extends EmpireWandTestBase {

    @Mock
    private Player mockPlayer;

    @Mock
    private Location mockLocation;

    @Mock
    private ConfigService mockConfig;

    @Mock
    private FxService mockFx;

    @Mock
    private AttributeInstance mockAttributeInstance;

    private Heal healSpell;
    private SpellContext context;

    @BeforeEach
    void setUp() {
        healSpell = new Heal();

        // Setup mock player
        when(mockPlayer.getLocation()).thenReturn(mockLocation);
        when(mockPlayer.getHealth()).thenReturn(10.0);
        when(mockPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH)).thenReturn(mockAttributeInstance);
        when(mockAttributeInstance.getValue()).thenReturn(20.0);

        // Setup mock config
        var mockConfigSection = mock(org.bukkit.configuration.file.FileConfiguration.class);
        when(mockConfig.getSpellsConfig()).thenReturn(mockConfigSection);
        when(mockConfigSection.getDouble("heal.values.heal-amount", 8.0)).thenReturn(8.0);

        context = new SpellContext(null, mockPlayer, mockConfig, mockFx);
    }

    @Test
    void testHealIncreasesPlayerHealth() {
        // Given
        when(mockPlayer.getHealth()).thenReturn(10.0);

        // When
        healSpell.execute(context);

        // Then
        verify(mockPlayer).setHealth(18.0); // 10 + 8 = 18
    }

    @Test
    void testHealClampsToMaxHealth() {
        // Given
        when(mockPlayer.getHealth()).thenReturn(18.0); // 18 + 8 = 26, should clamp to 20

        // When
        healSpell.execute(context);

        // Then
        verify(mockPlayer).setHealth(20.0); // Clamped to max health
    }

    @Test
    void testHealDoesNotExceedMaxHealth() {
        // Given
        when(mockPlayer.getHealth()).thenReturn(15.0); // 15 + 8 = 23, should clamp to 20

        // When
        healSpell.execute(context);

        // Then
        verify(mockPlayer).setHealth(20.0); // Clamped to max health
    }

    @Test
    void testHealUsesConfigValue() {
        // Given
        when(mockConfig.getSpellsConfig().getDouble("heal.values.heal-amount", 8.0)).thenReturn(5.0);
        when(mockPlayer.getHealth()).thenReturn(10.0);

        // When
        healSpell.execute(context);

        // Then
        verify(mockPlayer).setHealth(15.0); // 10 + 5 = 15
    }

    @Test
    void testHealSpawnsHeartParticles() {
        // When
        healSpell.execute(context);

        // Then
        verify(mockFx).spawnParticles(
            eq(mockLocation.add(0, 1.0, 0)),
            eq(Particle.HEART),
            eq(8),
            eq(0.4), eq(0.4), eq(0.4),
            eq(0.01)
        );
    }

    @Test
    void testHealPlaysSound() {
        // When
        healSpell.execute(context);

        // Then
        verify(mockFx).playSound(
            eq(mockPlayer),
            eq(Sound.ENTITY_PLAYER_LEVELUP),
            eq(0.6f),
            eq(1.4f)
        );
    }

    @Test
    void testHealHandlesNullMaxHealthAttribute() {
        // Given
        when(mockPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH)).thenReturn(null);
        when(mockPlayer.getHealth()).thenReturn(10.0);

        // When
        healSpell.execute(context);

        // Then
        verify(mockPlayer).setHealth(18.0); // 10 + 8 = 18, uses default 20.0 max
    }

    @Test
    void testHealSpellKey() {
        // When & Then
        assertEquals("heal", healSpell.key());
    }

    @Test
    void testHealSpellName() {
        // When & Then
        assertEquals("heal", healSpell.getName());
    }

    @Test
    void testHealDisplayName() {
        // When & Then
        assertEquals("Heal", healSpell.displayName().toString());
    }

    @Test
    void testHealPrereq() {
        // When
        var prereq = healSpell.prereq();

        // Then
        assertTrue(prereq.canCast());
        assertEquals("", prereq.reason().toString());
    }
}