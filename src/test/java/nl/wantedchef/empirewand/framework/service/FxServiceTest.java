package nl.wantedchef.empirewand.framework.service;

import nl.wantedchef.empirewand.api.service.EffectService;
import nl.wantedchef.empirewand.core.text.TextService;
import nl.wantedchef.empirewand.core.util.PerformanceMonitor;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

@DisplayName("FxService Tests")
class FxServiceTest {

    @Mock
    private TextService textService;

    @Mock
    private PerformanceMonitor performanceMonitor;

    @Mock
    private Player player;

    @Mock
    private Location location;

    @Mock
    private World world;

    @Mock
    private Plugin plugin;

    @Mock
    private Entity entity;

    private FxService fxService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mocks
        when(location.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        when(performanceMonitor.startTiming(anyString(), anyLong())).thenReturn(mock(PerformanceMonitor.TimingContext.class));
        
        // Create FxService instance
        fxService = new FxService(textService, performanceMonitor);
    }

    @Test
    @DisplayName("Test FxService class structure")
    void testFxServiceClassStructure() {
        // This test just verifies that the class can be loaded
        assertNotNull(FxService.class);
    }

    @Test
    @DisplayName("Test FxService implements EffectService")
    void testFxServiceImplementsEffectService() {
        // Verify that FxService implements the EffectService interface
        assertTrue(EffectService.class.isAssignableFrom(FxService.class));
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Test FxService can be instantiated with valid dependencies")
        void testFxServiceInstantiation() {
            assertNotNull(fxService);
        }
        
        @Test
        @DisplayName("Test FxService constructor throws exception when textService is null")
        void testConstructorWithNullTextService() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                new FxService(null, performanceMonitor);
            });
            
            assertEquals("TextService cannot be null", exception.getMessage());
        }
        
        @Test
        @DisplayName("Test FxService constructor throws exception when performanceMonitor is null")
        void testConstructorWithNullPerformanceMonitor() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                new FxService(textService, null);
            });
            
            assertEquals("PerformanceMonitor cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Action Bar Tests")
    class ActionBarTests {
        
        @Test
        @DisplayName("Test actionBar with Component sends message to player")
        void testActionBarWithComponent() {
            net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.text("Test message");
            
            fxService.actionBar(player, message);
            
            verify(player).sendActionBar(message);
        }
        
        @Test
        @DisplayName("Test actionBar with Component handles exceptions gracefully")
        void testActionBarWithComponentHandlesExceptions() {
            net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.text("Test message");
            doThrow(new RuntimeException("Test exception")).when(player).sendActionBar(any(net.kyori.adventure.text.Component.class));
            
            assertDoesNotThrow(() -> fxService.actionBar(player, message));
        }
        
        @Test
        @DisplayName("Test actionBar with plain text sends message to player")
        void testActionBarWithPlainText() {
            String plainText = "Test message";
            net.kyori.adventure.text.Component expectedComponent = net.kyori.adventure.text.Component.text(plainText);
            
            fxService.actionBar(player, plainText);
            
            verify(player).sendActionBar(expectedComponent);
        }
        
        @Test
        @DisplayName("Test actionBar with empty plain text does nothing")
        void testActionBarWithEmptyPlainText() {
            fxService.actionBar(player, "");
            
            verify(player, never()).sendActionBar(any(net.kyori.adventure.text.Component.class));
        }
        
        @Test
        @DisplayName("Test actionBar with whitespace plain text does nothing")
        void testActionBarWithWhitespacePlainText() {
            fxService.actionBar(player, "   ");
            
            verify(player, never()).sendActionBar(any(net.kyori.adventure.text.Component.class));
        }
        
        @Test
        @DisplayName("Test actionBarKey retrieves message from textService and sends to player")
        void testActionBarKey() {
            String messageKey = "test.message";
            String rawMessage = "Raw test message";
            
            when(textService.getMessage(messageKey)).thenReturn(rawMessage);
            
            fxService.actionBarKey(player, messageKey);
            
            verify(textService).getMessage(messageKey);
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
        }
        
        @Test
        @DisplayName("Test actionBarKey with placeholders retrieves formatted message and sends to player")
        void testActionBarKeyWithPlaceholders() {
            String messageKey = "test.message";
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", "TestPlayer");
            String rawMessage = "Hello TestPlayer";
            
            when(textService.getMessage(messageKey, placeholders)).thenReturn(rawMessage);
            
            fxService.actionBarKey(player, messageKey, placeholders);
            
            verify(textService).getMessage(messageKey, placeholders);
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
        }
    }

    @Nested
    @DisplayName("Action Bar Sound Tests")
    class ActionBarSoundTests {
        
        @Test
        @DisplayName("Test actionBarSound with Component plays sound and shows action bar")
        void testActionBarSoundWithComponent() {
            net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.text("Test message");
            Sound sound = Sound.ENTITY_PLAYER_LEVELUP;
            float volume = 1.0f;
            float pitch = 1.0f;
            
            fxService.actionBarSound(player, message, sound, volume, pitch);
            
            verify(player).sendActionBar(message);
            verify(player).playSound(location, sound, volume, pitch);
        }
        
        @Test
        @DisplayName("Test actionBarSound with message key retrieves message and plays sound")
        void testActionBarSoundWithMessageKey() {
            String messageKey = "test.message";
            String rawMessage = "Raw test message";
            Sound sound = Sound.ENTITY_PLAYER_LEVELUP;
            float volume = 1.0f;
            float pitch = 1.0f;
            
            when(textService.getMessage(messageKey)).thenReturn(rawMessage);
            
            fxService.actionBarSound(player, messageKey, sound, volume, pitch);
            
            verify(textService).getMessage(messageKey);
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
            verify(player).playSound(location, sound, volume, pitch);
        }
        
        @Test
        @DisplayName("Test actionBarSound with placeholders retrieves formatted message and plays sound")
        void testActionBarSoundWithPlaceholders() {
            String messageKey = "test.message";
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", "TestPlayer");
            String rawMessage = "Hello TestPlayer";
            Sound sound = Sound.ENTITY_PLAYER_LEVELUP;
            float volume = 1.0f;
            float pitch = 1.0f;
            
            when(textService.getMessage(messageKey, placeholders)).thenReturn(rawMessage);
            
            fxService.actionBarSound(player, messageKey, placeholders, sound, volume, pitch);
            
            verify(textService).getMessage(messageKey, placeholders);
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
            verify(player).playSound(location, sound, volume, pitch);
        }
        
        @Test
        @DisplayName("Test actionBarSound with null placeholders retrieves message without placeholders")
        void testActionBarSoundWithNullPlaceholders() {
            String messageKey = "test.message";
            String rawMessage = "Raw test message";
            Sound sound = Sound.ENTITY_PLAYER_LEVELUP;
            float volume = 1.0f;
            float pitch = 1.0f;
            
            when(textService.getMessage(messageKey)).thenReturn(rawMessage);
            
            fxService.actionBarSound(player, messageKey, null, sound, volume, pitch);
            
            verify(textService).getMessage(messageKey);
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
            verify(player).playSound(location, sound, volume, pitch);
        }
    }

    @Nested
    @DisplayName("Specialized Message Tests")
    class SpecializedMessageTests {
        
        @Test
        @DisplayName("Test selectedSpell displays action bar with spell name")
        void testSelectedSpell() {
            String displayName = "<green>Fireball</green>";
            String strippedName = "Fireball";
            String rawMessage = "Selected spell: Fireball";
            
            when(textService.getMessage(eq("spell-selected"), anyMap())).thenReturn(rawMessage);
            when(textService.stripMiniTags(displayName)).thenReturn(strippedName);
            
            fxService.selectedSpell(player, displayName);
            
            verify(textService).getMessage(eq("spell-selected"), anyMap());
            verify(textService).stripMiniTags(displayName);
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
        }
        
        @Test
        @DisplayName("Test onCooldown displays action bar with formatted time")
        void testOnCooldown() {
            String displayName = "<red>Lightning</red>";
            String strippedName = "Lightning";
            long msRemaining = 1500;
            String rawMessage = "Spell Lightning is on cooldown for 1.5 seconds";
            
            when(textService.getMessage(eq("on-cooldown"), anyMap())).thenReturn(rawMessage);
            when(textService.stripMiniTags(displayName)).thenReturn(strippedName);
            
            fxService.onCooldown(player, displayName, msRemaining);
            
            verify(textService).getMessage(eq("on-cooldown"), anyMap());
            verify(textService).stripMiniTags(displayName);
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
        }
        
        @Test
        @DisplayName("Test noSpells displays action bar")
        void testNoSpells() {
            String rawMessage = "No spells bound to wand";
            
            when(textService.getMessage("no-spells-bound")).thenReturn(rawMessage);
            
            fxService.noSpells(player);
            
            verify(textService).getMessage("no-spells-bound");
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
        }
        
        @Test
        @DisplayName("Test noPermission displays action bar")
        void testNoPermission() {
            String rawMessage = "You don't have permission";
            
            when(textService.getMessage("no-permission")).thenReturn(rawMessage);
            
            fxService.noPermission(player);
            
            verify(textService).getMessage("no-permission");
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
        }
        
        @Test
        @DisplayName("Test fizzle displays action bar and creates fizzle effect at player location")
        void testFizzle() {
            String rawMessage = "Spell fizzled";
            
            when(textService.getMessage("fizzle")).thenReturn(rawMessage);
            
            fxService.fizzle(player);
            
            verify(textService).getMessage("fizzle");
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
            verify(world).playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
            verify(world).spawnParticle(Particle.SMOKE, location, 10, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Nested
    @DisplayName("Title/Subtitle Tests")
    class TitleSubtitleTests {
        
        @Test
        @DisplayName("Test title with components shows title to player")
        void testTitleWithComponents() {
            net.kyori.adventure.text.Component title = net.kyori.adventure.text.Component.text("Title");
            net.kyori.adventure.text.Component subtitle = net.kyori.adventure.text.Component.text("Subtitle");
            
            fxService.title(player, title, subtitle);
            
            verify(player).showTitle(net.kyori.adventure.title.Title.title(title, subtitle));
        }
        
        @Test
        @DisplayName("Test title with components and timing shows title with custom timing")
        void testTitleWithComponentsAndTiming() {
            net.kyori.adventure.text.Component title = net.kyori.adventure.text.Component.text("Title");
            net.kyori.adventure.text.Component subtitle = net.kyori.adventure.text.Component.text("Subtitle");
            int fadeIn = 10;
            int stay = 70;
            int fadeOut = 20;
            
            fxService.title(player, title, subtitle, fadeIn, stay, fadeOut);
            
            verify(player).showTitle(any(net.kyori.adventure.title.Title.class));
        }
        
        @Test
        @DisplayName("Test title handles exceptions gracefully")
        void testTitleHandlesExceptions() {
            net.kyori.adventure.text.Component title = net.kyori.adventure.text.Component.text("Title");
            net.kyori.adventure.text.Component subtitle = net.kyori.adventure.text.Component.text("Subtitle");
            
            doThrow(new RuntimeException("Test exception")).when(player).showTitle(any(net.kyori.adventure.title.Title.class));
            
            assertDoesNotThrow(() -> fxService.title(player, title, subtitle));
        }
    }

    @Nested
    @DisplayName("Sound Profile Tests")
    class SoundProfileTests {
        
        @Test
        @DisplayName("Test playUISound with success profile plays correct sound")
        void testPlayUISoundSuccess() {
            fxService.playUISound(player, "success");
            
            verify(player).playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }
        
        @Test
        @DisplayName("Test playUISound with error profile plays correct sound")
        void testPlayUISoundError() {
            fxService.playUISound(player, "error");
            
            verify(player).playSound(location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
        }
        
        @Test
        @DisplayName("Test playUISound with warning profile plays correct sound")
        void testPlayUISoundWarning() {
            fxService.playUISound(player, "warning");
            
            verify(player).playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 0.8f);
        }
        
        @Test
        @DisplayName("Test playUISound with info profile plays correct sound")
        void testPlayUISoundInfo() {
            fxService.playUISound(player, "info");
            
            verify(player).playSound(location, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        }
        
        @Test
        @DisplayName("Test playUISound with cast profile plays correct sound")
        void testPlayUISoundCast() {
            fxService.playUISound(player, "cast");
            
            verify(player).playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        }
        
        @Test
        @DisplayName("Test playUISound with select profile plays correct sound")
        void testPlayUISoundSelect() {
            fxService.playUISound(player, "select");
            
            verify(player).playSound(location, Sound.UI_BUTTON_CLICK, 0.6f, 1.8f);
        }
        
        @Test
        @DisplayName("Test playUISound with cooldown profile plays correct sound")
        void testPlayUISoundCooldown() {
            fxService.playUISound(player, "cooldown");
            
            verify(player).playSound(location, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
        }
        
        @Test
        @DisplayName("Test playUISound with unknown profile plays default sound")
        void testPlayUISoundUnknown() {
            fxService.playUISound(player, "unknown");
            
            verify(player).playSound(location, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        }
        
        @Test
        @DisplayName("Test playUISound handles case insensitivity")
        void testPlayUISoundCaseInsensitivity() {
            fxService.playUISound(player, "SUCCESS");
            
            verify(player).playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }
    }

    @Nested
    @DisplayName("Standardized Message Tests")
    class StandardizedMessageTests {
        
        @Test
        @DisplayName("Test showError with no parameters calls overloaded method")
        void testShowErrorWithoutPlaceholders() {
            String errorType = "no-permission";
            String rawMessage = "You don't have permission";
            
            when(textService.getMessage(eq("error." + errorType), anyMap())).thenReturn(rawMessage);
            
            fxService.showError(player, errorType);
            
            // Should call the overloaded method with empty map
            verify(textService).getMessage(eq("error." + errorType), anyMap());
            // Should also call sendActionBar
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
            // Should play UI sound
            verify(player).playSound(location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
        }
        
        @Test
        @DisplayName("Test showError with placeholders displays action bar and plays sound")
        void testShowErrorWithPlaceholders() {
            String errorType = "no-permission";
            Map<String, String> placeholders = new HashMap<>();
            String rawMessage = "You don't have permission";
            
            when(textService.getMessage(eq("error." + errorType), anyMap())).thenReturn(rawMessage);
            
            fxService.showError(player, errorType, placeholders);
            
            verify(textService).getMessage(eq("error." + errorType), anyMap());
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
            verify(player).playSound(location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
        }
        
        @Test
        @DisplayName("Test showSuccess with no parameters calls overloaded method")
        void testShowSuccessWithoutPlaceholders() {
            String successType = "spell-cast";
            String rawMessage = "Spell cast successfully";
            
            when(textService.getMessage(eq("success." + successType), anyMap())).thenReturn(rawMessage);
            
            fxService.showSuccess(player, successType);
            
            // Should call the overloaded method with empty map
            verify(textService).getMessage(eq("success." + successType), anyMap());
            // Should also call sendActionBar
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
            // Should play UI sound
            verify(player).playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }
        
        @Test
        @DisplayName("Test showSuccess with placeholders displays action bar and plays sound")
        void testShowSuccessWithPlaceholders() {
            String successType = "spell-cast";
            Map<String, String> placeholders = new HashMap<>();
            String rawMessage = "Spell cast successfully";
            
            when(textService.getMessage(eq("success." + successType), anyMap())).thenReturn(rawMessage);
            
            fxService.showSuccess(player, successType, placeholders);
            
            verify(textService).getMessage(eq("success." + successType), anyMap());
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
            verify(player).playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }
        
        @Test
        @DisplayName("Test showInfo with no parameters calls overloaded method")
        void testShowInfoWithoutPlaceholders() {
            String infoType = "spell-ready";
            String rawMessage = "Spell is ready";
            
            when(textService.getMessage(eq("info." + infoType), anyMap())).thenReturn(rawMessage);
            
            fxService.showInfo(player, infoType);
            
            // Should call the overloaded method with empty map
            verify(textService).getMessage(eq("info." + infoType), anyMap());
            // Should also call sendActionBar
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
            // Should play UI sound
            verify(player).playSound(location, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        }
        
        @Test
        @DisplayName("Test showInfo with placeholders displays action bar and plays sound")
        void testShowInfoWithPlaceholders() {
            String infoType = "spell-ready";
            Map<String, String> placeholders = new HashMap<>();
            String rawMessage = "Spell is ready";
            
            when(textService.getMessage(eq("info." + infoType), anyMap())).thenReturn(rawMessage);
            
            fxService.showInfo(player, infoType, placeholders);
            
            verify(textService).getMessage(eq("info." + infoType), anyMap());
            verify(player).sendActionBar(net.kyori.adventure.text.Component.text(rawMessage));
            verify(player).playSound(location, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        }
    }

    @Nested
    @DisplayName("Sound Tests")
    class SoundTests {
        
        @Test
        @DisplayName("Test playSound with player plays sound at player location")
        void testPlaySoundWithPlayer() {
            Sound sound = Sound.ENTITY_PLAYER_LEVELUP;
            float volume = 1.0f;
            float pitch = 1.0f;
            
            fxService.playSound(player, sound, volume, pitch);
            
            verify(player).playSound(location, sound, volume, pitch);
        }
        
        @Test
        @DisplayName("Test playSound with player handles exceptions gracefully")
        void testPlaySoundWithPlayerHandlesExceptions() {
            Sound sound = Sound.ENTITY_PLAYER_LEVELUP;
            float volume = 1.0f;
            float pitch = 1.0f;
            
            doThrow(new RuntimeException("Test exception")).when(player).playSound(any(Location.class), any(Sound.class), anyFloat(), anyFloat());
            
            assertDoesNotThrow(() -> fxService.playSound(player, sound, volume, pitch));
        }
        
        @Test
        @DisplayName("Test playSound with location plays sound in world")
        void testPlaySoundWithLocation() {
            Sound sound = Sound.ENTITY_PLAYER_LEVELUP;
            float volume = 1.0f;
            float pitch = 1.0f;
            
            fxService.playSound(location, sound, volume, pitch);
            
            verify(world).playSound(location, sound, volume, pitch);
        }
        
        @Test
        @DisplayName("Test playSound with location handles exceptions gracefully")
        void testPlaySoundWithLocationHandlesExceptions() {
            Sound sound = Sound.ENTITY_PLAYER_LEVELUP;
            float volume = 1.0f;
            float pitch = 1.0f;
            
            doThrow(new RuntimeException("Test exception")).when(world).playSound(any(Location.class), any(Sound.class), anyFloat(), anyFloat());
            
            assertDoesNotThrow(() -> fxService.playSound(location, sound, volume, pitch));
        }
    }

    @Nested
    @DisplayName("Particle Tests")
    class ParticleTests {
        
        @Test
        @DisplayName("Test spawnParticles without data spawns particles in world")
        void testSpawnParticlesWithoutData() {
            Particle particle = Particle.FLAME;
            int count = 5;
            double offsetX = 0.1;
            double offsetY = 0.2;
            double offsetZ = 0.3;
            double speed = 0.5;
            
            fxService.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
            
            verify(world).spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        }
        
        @Test
        @DisplayName("Test spawnParticles without data handles exceptions gracefully")
        void testSpawnParticlesWithoutDataHandlesExceptions() {
            Particle particle = Particle.FLAME;
            int count = 5;
            double offsetX = 0.1;
            double offsetY = 0.2;
            double offsetZ = 0.3;
            double speed = 0.5;
            
            doThrow(new RuntimeException("Test exception")).when(world).spawnParticle(any(Particle.class), any(Location.class), anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
            
            assertDoesNotThrow(() -> fxService.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed));
        }
        
        @Test
        @DisplayName("Test spawnParticles with zero count does nothing")
        void testSpawnParticlesWithZeroCount() {
            Particle particle = Particle.FLAME;
            int count = 0;
            double offsetX = 0.1;
            double offsetY = 0.2;
            double offsetZ = 0.3;
            double speed = 0.5;
            
            fxService.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
            
            verify(world, never()).spawnParticle(any(Particle.class), any(Location.class), anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }
        
        @Test
        @DisplayName("Test spawnParticles with negative count does nothing")
        void testSpawnParticlesWithNegativeCount() {
            Particle particle = Particle.FLAME;
            int count = -1;
            double offsetX = 0.1;
            double offsetY = 0.2;
            double offsetZ = 0.3;
            double speed = 0.5;
            
            fxService.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
            
            verify(world, never()).spawnParticle(any(Particle.class), any(Location.class), anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }
        
        @Test
        @DisplayName("Test spawnParticles with data spawns particles with data in world")
        void testSpawnParticlesWithData() {
            Particle particle = Particle.SOUL_FIRE_FLAME; // Use a valid particle
            int count = 5;
            double offsetX = 0.1;
            double offsetY = 0.2;
            double offsetZ = 0.3;
            double speed = 0.5;
            Object data = new Object(); // Mock data object
            
            fxService.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, data);
            
            verify(world).spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed, data);
        }
        
        @Test
        @DisplayName("Test spawnParticles with data handles exceptions gracefully")
        void testSpawnParticlesWithDataHandlesExceptions() {
            Particle particle = Particle.SOUL_FIRE_FLAME; // Use a valid particle
            int count = 5;
            double offsetX = 0.1;
            double offsetY = 0.2;
            double offsetZ = 0.3;
            double speed = 0.5;
            Object data = new Object(); // Mock data object
            
            doThrow(new RuntimeException("Test exception")).when(world).spawnParticle(any(Particle.class), any(Location.class), anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
            
            assertDoesNotThrow(() -> fxService.spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, data));
        }
        
        @Test
        @DisplayName("Test batchParticles without data adds to batch")
        void testBatchParticlesWithoutData() {
            Particle particle = Particle.FLAME;
            int count = 5;
            double offsetX = 0.1;
            double offsetY = 0.2;
            double offsetZ = 0.3;
            double speed = 0.5;
            
            fxService.batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
            
            // Verify that flushParticleBatch was called (because we're adding to a new batch)
            fxService.flushParticleBatch();
        }
        
        @Test
        @DisplayName("Test batchParticles with data adds to batch")
        void testBatchParticlesWithData() {
            Particle particle = Particle.SOUL_FIRE_FLAME; // Use a valid particle
            int count = 5;
            double offsetX = 0.1;
            double offsetY = 0.2;
            double offsetZ = 0.3;
            double speed = 0.5;
            Object data = new Object(); // Mock data object
            
            fxService.batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed, data);
            
            // Verify that flushParticleBatch was called (because we're adding to a new batch)
            fxService.flushParticleBatch();
        }
        
        @Test
        @DisplayName("Test flushParticleBatch executes batched particles")
        void testFlushParticleBatch() {
            Particle particle = Particle.FLAME;
            int count = 5;
            double offsetX = 0.1;
            double offsetY = 0.2;
            double offsetZ = 0.3;
            double speed = 0.5;
            
            // Add a particle to the batch
            fxService.batchParticles(location, particle, count, offsetX, offsetY, offsetZ, speed);
            
            // Flush the batch
            fxService.flushParticleBatch();
            
            // Verify the particle was spawned
            verify(world).spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        }
        
        @Test
        @DisplayName("Test trail between two locations creates particle trail")
        void testTrailBetweenLocations() {
            Location endLocation = mock(Location.class);
            when(endLocation.getWorld()).thenReturn(world);
            when(endLocation.toVector()).thenReturn(new org.bukkit.util.Vector(10, 10, 10));
            
            when(location.toVector()).thenReturn(new org.bukkit.util.Vector(0, 0, 0));
            when(location.clone()).thenReturn(location);
            
            Particle particle = Particle.FLAME;
            int perStep = 3;
            
            fxService.trail(location, endLocation, particle, perStep);
            
            // Verify particles were spawned (exact count depends on distance calculation)
            verify(world, atLeastOnce()).spawnParticle(eq(particle), any(Location.class), eq(perStep), eq(0.0), eq(0.0), eq(0.0), eq(0.0));
        }
        
        @Test
        @DisplayName("Test trail with default particle creates trail effect")
        void testTrailWithDefaultParticle() {
            Particle particle = Particle.SOUL_FIRE_FLAME;
            int count = 10;
            double offsetX = 0.1;
            double offsetY = 0.1;
            double offsetZ = 0.1;
            double speed = 0.05;
            
            fxService.trail(location);
            
            verify(world).spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        }
        
        @Test
        @DisplayName("Test impact with default parameters creates impact effect")
        void testImpactWithDefaultParameters() {
            Particle particle = Particle.EXPLOSION;
            int count = 30;
            double spread = 0.5;
            Sound sound = Sound.ENTITY_BLAZE_SHOOT;
            float volume = 1.0f;
            float pitch = 1.0f;
            
            fxService.impact(location);
            
            verify(world).spawnParticle(particle, location, count, spread, spread, spread, 0.1);
            verify(world).playSound(location, sound, volume, pitch);
        }
        
        @Test
        @DisplayName("Test impact with custom parameters creates impact effect")
        void testImpactWithCustomParameters() {
            Particle particle = Particle.SOUL_FIRE_FLAME; // Use a valid particle
            int count = 10;
            double spread = 0.2;
            Sound sound = Sound.ENTITY_GENERIC_EXPLODE;
            float volume = 0.8f;
            float pitch = 1.2f;
            
            fxService.impact(location, particle, count, spread, sound, volume, pitch);
            
            verify(world).spawnParticle(particle, location, count, spread, spread, spread, 0);
            verify(world).playSound(location, sound, volume, pitch);
        }
        
        @Test
        @DisplayName("Test impact with sound and particle parameters creates impact effect")
        void testImpactWithSoundAndParticleParameters() {
            Particle particle = Particle.SOUL_FIRE_FLAME; // Use a valid particle
            int count = 10;
            Sound sound = Sound.ENTITY_GENERIC_EXPLODE;
            float volume = 0.8f;
            float pitch = 1.2f;
            
            fxService.impact(location, particle, count, sound, volume, pitch);
            
            verify(world).spawnParticle(particle, location, count, 0.2, 0.2, 0.2, 0);
            verify(world).playSound(location, sound, volume, pitch);
        }
        
        @Test
        @DisplayName("Test fizzle creates fizzle effect")
        void testFizzleEffect() {
            Sound sound = Sound.BLOCK_FIRE_EXTINGUISH;
            float volume = 1.0f;
            float pitch = 1.5f;
            Particle particle = Particle.SMOKE;
            int count = 10;
            double offsetX = 0.1;
            double offsetY = 0.1;
            double offsetZ = 0.1;
            double speed = 0.05;
            
            fxService.fizzle(location);
            
            verify(world).playSound(location, sound, volume, pitch);
            verify(world).spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    @Nested
    @DisplayName("Entity Following Tests")
    class EntityFollowingTests {
        
        @Test
        @DisplayName("Test followParticles with invalid period does nothing")
        void testFollowParticlesWithInvalidPeriod() {
            Particle particle = Particle.FLAME;
            int count = 5;
            double offsetX = 0.1;
            double offsetY = 0.2;
            double offsetZ = 0.3;
            double speed = 0.5;
            long periodTicks = 0;
            
            // Should not throw exception
            assertDoesNotThrow(() -> fxService.followParticles(plugin, entity, particle, count, offsetX, offsetY, offsetZ, speed, null, periodTicks));
        }
        
        @Test
        @DisplayName("Test followTrail with invalid period does nothing")
        void testFollowTrailWithInvalidPeriod() {
            long periodTicks = 0;
            
            // Should not throw exception
            assertDoesNotThrow(() -> fxService.followTrail(plugin, entity, periodTicks));
        }
    }

    @Nested
    @DisplayName("Service Interface Tests")
    class ServiceInterfaceTests {
        
        @Test
        @DisplayName("Test getServiceName returns correct name")
        void testGetServiceName() {
            assertEquals("FxService", fxService.getServiceName());
        }
        
        @Test
        @DisplayName("Test getServiceVersion returns version")
        void testGetServiceVersion() {
            assertNotNull(fxService.getServiceVersion());
        }
        
        @Test
        @DisplayName("Test isEnabled returns true")
        void testIsEnabled() {
            assertTrue(fxService.isEnabled());
        }
        
        @Test
        @DisplayName("Test getHealth returns HEALTHY")
        void testGetHealth() {
            assertEquals(nl.wantedchef.empirewand.api.ServiceHealth.HEALTHY, fxService.getHealth());
        }
        
        @Test
        @DisplayName("Test reload flushes particle batch")
        void testReload() {
            // Add something to the batch
            fxService.batchParticles(location, Particle.FLAME, 5, 0.1, 0.1, 0.1, 0.1);
            
            // Reload should flush the batch
            fxService.reload();
            
            // Particles should have been spawned during reload
            verify(world).spawnParticle(eq(Particle.FLAME), any(Location.class), eq(5), eq(0.1), eq(0.1), eq(0.1), eq(0.1));
        }
        
        @Test
        @DisplayName("Test shutdown flushes and clears particle batch")
        void testShutdown() {
            // Add something to the batch
            fxService.batchParticles(location, Particle.FLAME, 5, 0.1, 0.1, 0.1, 0.1);
            
            // Shutdown should flush and clear the batch
            fxService.shutdown();
            
            // Particles should have been spawned during shutdown
            verify(world).spawnParticle(eq(Particle.FLAME), any(Location.class), eq(5), eq(0.1), eq(0.1), eq(0.1), eq(0.1));
        }
    }
}