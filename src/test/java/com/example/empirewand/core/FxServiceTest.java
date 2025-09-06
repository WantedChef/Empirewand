package com.example.empirewand.core;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.empirewand.EmpireWandTestBase;
import com.example.empirewand.core.text.TextService;

import net.kyori.adventure.text.Component;

/**
 * Unit tests for FxService.
 */
class FxServiceTest extends EmpireWandTestBase {

    @Mock
    private Player mockPlayer;

    @Mock
    private Location mockLocation;

    @Mock
    private World mockWorld;

    @Mock
    private ConfigService mockConfig;

    @Mock
    private TextService mockTextService;

    private FxService fxService;

    @BeforeEach
    void setUp() {
        fxService = new FxService(mockConfig, mockTextService);
        when(mockPlayer.getLocation()).thenReturn(mockLocation);
        when(mockLocation.getWorld()).thenReturn(mockWorld);
    }

    @Test
    void testActionBarWithComponent() {
        // Given
        Component message = Component.text("Test message");

        // When
        fxService.actionBar(mockPlayer, message);

        // Then
        verify(mockPlayer).sendActionBar(message);
    }

    @Test
    void testActionBarWithNullPlayer() {
        // Given
        Component message = Component.text("Test message");

        // When
        fxService.actionBar(null, message);

        // Then
        // No verification needed as method should handle null gracefully
    }

    @Test
    void testActionBarWithNullMessage() {
        // When
        fxService.actionBar(mockPlayer, (Component) null);

        // Then
        // No verification needed as method should handle null gracefully
    }

    @Test
    void testActionBarWithPlainText() {
        // Given
        String plainText = "Test message";
        when(mockTextService.stripMiniTags(plainText)).thenReturn(plainText);

        // When
        fxService.actionBar(mockPlayer, plainText);

        // Then
        verify(mockPlayer).sendActionBar(Component.text(plainText));
    }

    @Test
    void testActionBarKey() {
        // Given
        String messageKey = "test.message";
        String rawMessage = "Raw message";
        when(mockTextService.getMessage(messageKey)).thenReturn(rawMessage);
        when(mockTextService.stripMiniTags(rawMessage)).thenReturn(rawMessage);

        // When
        fxService.actionBarKey(mockPlayer, messageKey);

        // Then
        verify(mockPlayer).sendActionBar(Component.text(rawMessage));
    }

    @Test
    void testActionBarKeyWithPlaceholders() {
        // Given
        String messageKey = "test.message";
        Map<String, String> placeholders = Map.of("name", "John");
        String processedMessage = "Hello John";
        when(mockTextService.getMessage(messageKey, placeholders)).thenReturn(processedMessage);
        when(mockTextService.stripMiniTags(processedMessage)).thenReturn(processedMessage);

        // When
        fxService.actionBarKey(mockPlayer, messageKey, placeholders);

        // Then
        verify(mockPlayer).sendActionBar(Component.text(processedMessage));
    }

    @Test
    void testActionBarSound() {
        // Given
        Component message = Component.text("Test message");
        Sound sound = Sound.UI_BUTTON_CLICK;
        float volume = 0.8f;
        float pitch = 1.2f;

        // When
        fxService.actionBarSound(mockPlayer, message, sound, volume, pitch);

        // Then
        verify(mockPlayer).sendActionBar(message);
        verify(mockPlayer).playSound(mockLocation, sound, volume, pitch);
    }

    @Test
    void testSelectedSpell() {
        // Given
        String displayName = "Fireball";
        String strippedName = "Fireball";
        when(mockTextService.stripMiniTags(displayName)).thenReturn(strippedName);
        Map<String, String> expectedMap = Map.of("spell", strippedName);
        when(mockTextService.getMessage(eq("spell-selected"), eq(expectedMap))).thenReturn("Selected: Fireball");

        // When
        fxService.selectedSpell(mockPlayer, displayName);

        // Then
        verify(mockTextService).getMessage("spell-selected", expectedMap);
    }

    @Test
    void testOnCooldown() {
        // Given
        String displayName = "Fireball";
        long msRemaining = 5000;
        String strippedName = "Fireball";
        when(mockTextService.stripMiniTags(displayName)).thenReturn(strippedName);
        Map<String, String> expectedMap = Map.of(
                "spell", strippedName,
                "time", "5000");
        when(mockTextService.getMessage(eq("on-cooldown"), eq(expectedMap))).thenReturn("On cooldown: 5000ms");

        // When
        fxService.onCooldown(mockPlayer, displayName, msRemaining);

        // Then
        verify(mockTextService).getMessage("on-cooldown", expectedMap);
    }

    @Test
    void testNoSpells() {
        // Given
        when(mockTextService.getMessage("no-spells-bound")).thenReturn("No spells bound");

        // When
        fxService.noSpells(mockPlayer);

        // Then
        verify(mockTextService).getMessage("no-spells-bound");
    }

    @Test
    void testNoPermission() {
        // Given
        when(mockTextService.getMessage("no-permission")).thenReturn("No permission");

        // When
        fxService.noPermission(mockPlayer);

        // Then
        verify(mockTextService).getMessage("no-permission");
    }

    @Test
    void testFizzle() {
        // Given
        when(mockPlayer.getLocation()).thenReturn(mockLocation);

        // When
        fxService.fizzle(mockPlayer);

        // Then
        verify(mockTextService).getMessage("fizzle");
        verify(mockWorld).playSound(mockLocation, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
        verify(mockWorld).spawnParticle(Particle.SMOKE, mockLocation, 10, 0.1, 0.1, 0.1, 0.05);
    }

    @Test
    void testTitle() {
        // Given
        Component title = Component.text("Title");
        Component subtitle = Component.text("Subtitle");

        // When
        fxService.title(mockPlayer, title, subtitle);

        // Then
        verify(mockPlayer).showTitle(any(net.kyori.adventure.title.Title.class));
    }

    @Test
    void testTitleWithTimes() {
        // Given
        Component title = Component.text("Title");
        Component subtitle = Component.text("Subtitle");
        int fadeIn = 10, stay = 20, fadeOut = 10;

        // When
        fxService.title(mockPlayer, title, subtitle, fadeIn, stay, fadeOut);

        // Then
        verify(mockPlayer).showTitle(any(net.kyori.adventure.title.Title.class));
    }

    @Test
    void testPlayUISoundSuccess() {
        // When
        fxService.playUISound(mockPlayer, "success");

        // Then
        verify(mockPlayer).playSound(mockLocation, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
    }

    @Test
    void testPlayUISoundError() {
        // When
        fxService.playUISound(mockPlayer, "error");

        // Then
        verify(mockPlayer).playSound(mockLocation, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
    }

    @Test
    void testPlayUISoundUnknown() {
        // When
        fxService.playUISound(mockPlayer, "unknown");

        // Then
        verify(mockPlayer).playSound(mockLocation, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
    }

    @Test
    void testShowError() {
        // Given
        String errorType = "no-permission";
        when(mockTextService.getMessage("error.no-permission")).thenReturn("No permission");

        // When
        fxService.showError(mockPlayer, errorType);

        // Then
        verify(mockPlayer).sendActionBar(Component.text("No permission"));
        verify(mockPlayer).playSound(mockLocation, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
    }

    @Test
    void testShowSuccess() {
        // Given
        String successType = "spell-cast";
        when(mockTextService.getMessage("success.spell-cast")).thenReturn("Spell cast!");

        // When
        fxService.showSuccess(mockPlayer, successType);

        // Then
        verify(mockPlayer).sendActionBar(Component.text("Spell cast!"));
        verify(mockPlayer).playSound(mockLocation, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
    }

    @Test
    void testShowInfo() {
        // Given
        String infoType = "spell-selected";
        when(mockTextService.getMessage("info.spell-selected")).thenReturn("Spell selected");

        // When
        fxService.showInfo(mockPlayer, infoType);

        // Then
        verify(mockPlayer).sendActionBar(Component.text("Spell selected"));
        verify(mockPlayer).playSound(mockLocation, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
    }

    @Test
    void testPlaySoundPlayer() {
        // Given
        Sound sound = Sound.UI_BUTTON_CLICK;
        float volume = 0.8f;
        float pitch = 1.2f;

        // When
        fxService.playSound(mockPlayer, sound, volume, pitch);

        // Then
        verify(mockPlayer).playSound(mockLocation, sound, volume, pitch);
    }

    @Test
    void testPlaySoundLocation() {
        // Given
        Sound sound = Sound.UI_BUTTON_CLICK;
        float volume = 0.8f;
        float pitch = 1.2f;

        // When
        fxService.playSound(mockLocation, sound, volume, pitch);

        // Then
        verify(mockWorld).playSound(mockLocation, sound, volume, pitch);
    }

    @Test
    void testSpawnParticles() {
        // Given
        Particle particle = Particle.FLAME;
        int count = 10;
        double offsetX = 0.1, offsetY = 0.1, offsetZ = 0.1, speed = 0.05;

        // When
        fxService.spawnParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed);

        // Then
        verify(mockWorld).spawnParticle(particle, mockLocation, count, offsetX, offsetY, offsetZ, speed);
    }

    @Test
    void testSpawnParticlesWithData() {
        // Given
        Particle particle = Particle.DUST_COLOR_TRANSITION;
        int count = 10;
        double offsetX = 0.1, offsetY = 0.1, offsetZ = 0.1, speed = 0.05;
        Object data = mock(Object.class);

        // When
        fxService.spawnParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed, data);

        // Then
        verify(mockWorld).spawnParticle(particle, mockLocation, count, offsetX, offsetY, offsetZ, speed, data);
    }

    @Test
    void testBatchParticles() {
        // Given
        Particle particle = Particle.FLAME;
        int count = 10;
        double offsetX = 0.1, offsetY = 0.1, offsetZ = 0.1, speed = 0.05;

        // When
        fxService.batchParticles(mockLocation, particle, count, offsetX, offsetY, offsetZ, speed);
        fxService.flushParticleBatch();

        // Then
        verify(mockWorld).spawnParticle(particle, mockLocation, count, offsetX, offsetY, offsetZ, speed);
    }

    @Test
    void testTrail() {
        // Given
        Location start = mock(Location.class);
        Location end = mock(Location.class);
        Particle particle = Particle.FLAME;
        int perStep = 5;

        when(start.toVector()).thenReturn(new org.bukkit.util.Vector(0, 0, 0));
        when(end.toVector()).thenReturn(new org.bukkit.util.Vector(1, 0, 0));
        when(start.clone()).thenReturn(start);
        when(start.add(any(org.bukkit.util.Vector.class))).thenReturn(start);
        when(start.getWorld()).thenReturn(mockWorld);

        // When
        fxService.trail(start, end, particle, perStep);

        // Then
        verify(mockWorld).spawnParticle(eq(particle), any(Location.class), eq(perStep), eq(0.0), eq(0.0), eq(0.0),
                eq(0.0));
    }

    @Test
    void testImpact() {
        // Given
        Particle particle = Particle.EXPLOSION;
        int count = 20;
        Sound sound = Sound.ENTITY_GENERIC_EXPLODE;
        float volume = 1.0f;
        float pitch = 1.0f;

        // When
        fxService.impact(mockLocation, particle, count, sound, volume, pitch);

        // Then
        verify(mockWorld).spawnParticle(particle, mockLocation, count, 0.2, 0.2, 0.2, 0.0);
        verify(mockWorld).playSound(mockLocation, sound, volume, pitch);
    }

    @Test
    void testFizzleLocation() {
        // When
        fxService.fizzle(mockLocation);

        // Then
        verify(mockWorld).playSound(mockLocation, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
        verify(mockWorld).spawnParticle(Particle.SMOKE, mockLocation, 10, 0.1, 0.1, 0.1, 0.05);
    }

    @Test
    void testTrailLocation() {
        // When
        fxService.trail(mockLocation);

        // Then
        verify(mockWorld).spawnParticle(Particle.SOUL_FIRE_FLAME, mockLocation, 10, 0.1, 0.1, 0.1, 0.05);
    }

    @Test
    void testImpactLocation() {
        // When
        fxService.impact(mockLocation);

        // Then
        verify(mockWorld).spawnParticle(Particle.EXPLOSION, mockLocation, 30, 0.5, 0.5, 0.5, 0.1);
        verify(mockWorld).playSound(mockLocation, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }
}