package nl.wantedchef.empirewand.core;

import nl.wantedchef.empirewand.core.config.ReadOnlyConfig;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ConfigService Utility Methods Tests")
class ConfigServiceUtilityMethodsTest {

    @Mock
    private Plugin plugin;

    @Mock
    private FileConfiguration config;

    @Mock
    private Logger logger;

    private ConfigService configService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup plugin mock
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getConfig()).thenReturn(config);
        
        // Setup basic config
        when(config.contains("config-version")).thenReturn(true);
        when(config.get("config-version")).thenReturn("1.0");
        
        // Mock plugin.getDataFolder()
        File dataFolder = new File(System.getProperty("java.io.tmpdir"));
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        
        // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
        doNothing().when(plugin).saveDefaultConfig();
        doNothing().when(plugin).reloadConfig();
        
        // Create service
        configService = new ConfigService(plugin);
    }

    @Nested
    @DisplayName("Message Retrieval Tests")
    class MessageLookupTests {
        
        @Test
        @DisplayName("Test getMessage with valid key returns correct value")
        void testGetMessageWithValidKeyReturnsCorrectValue() {
            String expectedMessage = "Test message content";
            when(config.getString("messages.test.key", "")).thenReturn(expectedMessage);
            
            String result = configService.getMessage("test.key");
            assertEquals(expectedMessage, result);
        }
        
        @Test
        @DisplayName("Test getMessage with missing key returns empty string")
        void testGetMessageWithMissingKeyReturnsEmptyString() {
            when(config.getString("messages.missing.key", "")).thenReturn("");
            
            String result = configService.getMessage("missing.key");
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("Test getMessage with null key returns empty string")
        void testGetMessageWithNullKeyReturnsEmptyString() {
            String result = configService.getMessage(null);
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("Test getMessage with empty key returns empty string")
        void testGetMessageWithEmptyKeyReturnsEmptyString() {
            String result = configService.getMessage("");
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("Test getMessage with whitespace-only key returns empty string")
        void testGetMessageWithWhitespaceOnlyKeyReturnsEmptyString() {
            String result = configService.getMessage("   ");
            assertEquals("", result);
        }
    }

    @Nested
    @DisplayName("Feature Flag Tests")
    class FeatureFlagTests {
        
        @Test
        @DisplayName("Test getFeatureFlag with valid key returns correct value")
        void testGetFeatureFlagWithValidKeyReturnsCorrectValue() {
            when(config.getBoolean("features.test.flag", false)).thenReturn(true);
            
            boolean result = configService.getFeatureFlag("test.flag");
            assertTrue(result);
        }
        
        @Test
        @DisplayName("Test getFeatureFlag with missing key returns false")
        void testGetFeatureFlagWithMissingKeyReturnsFalse() {
            when(config.getBoolean("features.missing.flag", false)).thenReturn(false);
            
            boolean result = configService.getFeatureFlag("missing.flag");
            assertFalse(result);
        }
        
        @Test
        @DisplayName("Test getFeatureFlag with null key returns false")
        void testGetFeatureFlagWithNullKeyReturnsFalse() {
            boolean result = configService.getFeatureFlag(null);
            assertFalse(result);
        }
        
        @Test
        @DisplayName("Test getFeatureFlag with empty key returns false")
        void testGetFeatureFlagWithEmptyKeyReturnsFalse() {
            boolean result = configService.getFeatureFlag("");
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Cooldown Tests")
    class CooldownTests {
        
        @Test
        @DisplayName("Test getDefaultCooldown returns configured value")
        void testGetDefaultCooldownReturnsConfiguredValue() {
            long expectedCooldown = 1500L;
            when(config.getLong("cooldowns.default", 500)).thenReturn(expectedCooldown);
            
            long result = configService.getDefaultCooldown();
            assertEquals(expectedCooldown, result);
        }
        
        @Test
        @DisplayName("Test getDefaultCooldown returns default when missing")
        void testGetDefaultCooldownReturnsDefaultWhenMissing() {
            when(config.getLong("cooldowns.default", 500)).thenReturn(500L);
            
            long result = configService.getDefaultCooldown();
            assertEquals(500L, result);
        }
        
        @Test
        @DisplayName("Test getDefaultCooldown returns default on exception")
        void testGetDefaultCooldownReturnsDefaultOnException() {
            when(config.getLong("cooldowns.default", 500)).thenThrow(new RuntimeException("Test exception"));
            
            long result = configService.getDefaultCooldown();
            assertEquals(500L, result); // Should return default value
        }
    }

    @Nested
    @DisplayName("Category Tests")
    class CategoryTests {
        
        @Test
        @DisplayName("Test getCategorySpells returns correct list")
        void testGetCategorySpellsReturnsCorrectList() {
            List<String> expectedSpells = List.of("fireball", "lightning", "heal");
            when(config.getStringList("categories.combat.spells")).thenReturn(expectedSpells);
            
            List<String> result = configService.getCategorySpells("combat");
            assertEquals(expectedSpells, result);
        }
        
        @Test
        @DisplayName("Test getCategorySpells returns empty list for missing category")
        void testGetCategorySpellsReturnsEmptyListForMissingCategory() {
            when(config.getStringList("categories.missing.spells")).thenReturn(null);
            
            List<String> result = configService.getCategorySpells("missing");
            assertEquals(Collections.emptyList(), result);
        }
        
        @Test
        @DisplayName("Test getCategorySpells with null category returns empty list")
        void testGetCategorySpellsWithNullCategoryReturnsEmptyList() {
            List<String> result = configService.getCategorySpells(null);
            assertEquals(Collections.emptyList(), result);
        }
        
        @Test
        @DisplayName("Test getCategorySpells with empty category returns empty list")
        void testGetCategorySpellsWithEmptyCategoryReturnsEmptyList() {
            List<String> result = configService.getCategorySpells("");
            assertEquals(Collections.emptyList(), result);
        }
        
        @Test
        @DisplayName("Test getCategoryNames returns correct set")
        void testGetCategoryNamesReturnsCorrectSet() {
            Set<String> expectedCategories = Set.of("combat", "healing", "utility");
            ConfigurationSection section = mock(ConfigurationSection.class);
            when(config.getConfigurationSection("categories")).thenReturn(section);
            when(section.getKeys(false)).thenReturn(expectedCategories);
            
            Set<String> result = configService.getCategoryNames();
            assertEquals(expectedCategories, result);
        }
        
        @Test
        @DisplayName("Test getCategoryNames returns empty set when no categories")
        void testGetCategoryNamesReturnsEmptySetWhenNoCategories() {
            when(config.getConfigurationSection("categories")).thenReturn(null);
            
            Set<String> result = configService.getCategoryNames();
            assertEquals(Set.of(), result);
        }
    }

    @Nested
    @DisplayName("Configuration Access Tests")
    class ConfigurationAccessTests {
        
        @Test
        @DisplayName("Test getConfig returns ReadableConfig instance")
        void testGetConfigReturnsReadableConfigInstance() {
            ReadableConfig result = configService.getConfig();
            assertNotNull(result);
            assertTrue(result instanceof ReadOnlyConfig);
        }
        
        @Test
        @DisplayName("Test getSpellsConfig returns ReadableConfig instance")
        void testGetSpellsConfigReturnsReadableConfigInstance() {
            ReadableConfig result = configService.getSpellsConfig();
            assertNotNull(result);
            assertTrue(result instanceof ReadOnlyConfig);
        }
        
        @Test
        @DisplayName("Test isConfigLoaded returns correct status")
        void testIsConfigLoadedReturnsCorrectStatus() {
            // Since we're using mocks, this will depend on the internal state
            boolean result = configService.isConfigLoaded();
            // With our mock setup, this should be false because we didn't fully initialize
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Reload Tests")
    class ReloadTests {
        
        @Test
        @DisplayName("Test reload method executes without exception")
        void testReloadMethodExecutesWithoutException() {
            // Setup reload behavior
            doNothing().when(plugin).reloadConfig();
            
            assertDoesNotThrow(() -> configService.reload());
        }
        
        @Test
        @DisplayName("Test reload calls plugin reload methods")
        void testReloadCallsPluginReloadMethods() {
            // Setup reload behavior
            doNothing().when(plugin).reloadConfig();
            
            configService.reload();
            
            // Verify reloadConfig was called
            verify(plugin).reloadConfig();
        }
    }
}