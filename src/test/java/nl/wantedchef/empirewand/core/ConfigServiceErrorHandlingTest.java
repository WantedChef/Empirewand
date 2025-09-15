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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ConfigService Error Handling Tests")
class ConfigServiceErrorHandlingTest {

    @Mock
    private Plugin plugin;

    @Mock
    private FileConfiguration config;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup plugin mock
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getConfig()).thenReturn(config);
    }

    @Nested
    @DisplayName("Constructor Error Handling Tests")
    class ConstructorErrorHandlingTests {
        
        @Test
        @DisplayName("Test ConfigService handles plugin.getConfig() returning null")
        void testConfigServiceHandlesPluginGetConfigReturningNull() {
            // Setup plugin to return null config
            when(plugin.getConfig()).thenReturn(null);
            when(plugin.getLogger()).thenReturn(logger);
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            // Should not throw exception
            assertDoesNotThrow(() -> new ConfigService(plugin));
            
            // Verify appropriate error logging
            verify(logger).severe("Failed to load main config.yml");
        }
        
        @Test
        @DisplayName("Test ConfigService handles exceptions in constructor")
        void testConfigServiceHandlesExceptionsInConstructor() {
            // Setup plugin to throw exception
            when(plugin.getConfig()).thenThrow(new RuntimeException("Test exception"));
            when(plugin.getLogger()).thenReturn(logger);
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            // Should not throw exception
            assertDoesNotThrow(() -> new ConfigService(plugin));
            
            // Verify appropriate error logging
            verify(logger).severe("Failed to load main config.yml");
        }
    }

    @Nested
    @DisplayName("Method Error Handling Tests")
    class MethodErrorHandlingTests {
        
        private ConfigService configService;
        
        @BeforeEach
        void setUpService() {
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
        
        @Test
        @DisplayName("Test getMessage handles configuration exceptions")
        void testGetMessageHandlesConfigurationExceptions() {
            when(config.getString("messages.test.key", "")).thenThrow(new RuntimeException("Test exception"));
            
            String result = configService.getMessage("test.key");
            assertEquals("", result);
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
        
        @Test
        @DisplayName("Test getFeatureFlag handles configuration exceptions")
        void testGetFeatureFlagHandlesConfigurationExceptions() {
            when(config.getBoolean("features.test.flag", false)).thenThrow(new RuntimeException("Test exception"));
            
            boolean result = configService.getFeatureFlag("test.flag");
            assertFalse(result);
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
        
        @Test
        @DisplayName("Test getDefaultCooldown handles configuration exceptions")
        void testGetDefaultCooldownHandlesConfigurationExceptions() {
            when(config.getLong("cooldowns.default", 500)).thenThrow(new RuntimeException("Test exception"));
            
            long result = configService.getDefaultCooldown();
            assertEquals(500L, result); // Should return default
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
        
        @Test
        @DisplayName("Test getCategorySpells handles configuration exceptions")
        void testGetCategorySpellsHandlesConfigurationExceptions() {
            when(config.getStringList("categories.test.spells")).thenThrow(new RuntimeException("Test exception"));
            
            List<String> result = configService.getCategorySpells("test");
            assertEquals(Collections.emptyList(), result);
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
        
        @Test
        @DisplayName("Test getCategoryNames handles configuration exceptions")
        void testGetCategoryNamesHandlesConfigurationExceptions() {
            ConfigurationSection section = mock(ConfigurationSection.class);
            when(config.getConfigurationSection("categories")).thenReturn(section);
            when(section.getKeys(false)).thenThrow(new RuntimeException("Test exception"));
            
            Set<String> result = configService.getCategoryNames();
            assertEquals(Set.of(), result);
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
        
        @Test
        @DisplayName("Test getConfig handles null internal config")
        void testGetConfigHandlesNullInternalConfig() {
            // Create a new service with a mock that returns null config
            when(plugin.getConfig()).thenReturn(null);
            when(plugin.getLogger()).thenReturn(logger);
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            ConfigService service = new ConfigService(plugin);
            
            ReadableConfig result = service.getConfig();
            assertNotNull(result);
            assertTrue(result instanceof ReadOnlyConfig);
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
    }

    @Nested
    @DisplayName("Null Safety Tests")
    class NullSafetyTests {
        
        private ConfigService configService;
        
        @BeforeEach
        void setUpService() {
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
        
        @Test
        @DisplayName("Test getMessage with null parameter")
        void testGetMessageWithNullParameter() {
            String result = configService.getMessage(null);
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("Test getFeatureFlag with null parameter")
        void testGetFeatureFlagWithNullParameter() {
            boolean result = configService.getFeatureFlag(null);
            assertFalse(result);
        }
        
        @Test
        @DisplayName("Test getCategorySpells with null parameter")
        void testGetCategorySpellsWithNullParameter() {
            List<String> result = configService.getCategorySpells(null);
            assertEquals(Collections.emptyList(), result);
        }
        
        @Test
        @DisplayName("Test all methods handle null gracefully")
        void testAllMethodsHandleNullGracefully() {
            // All methods should handle null gracefully without throwing exceptions
            assertDoesNotThrow(() -> configService.getMessage(null));
            assertDoesNotThrow(() -> configService.getFeatureFlag(null));
            assertDoesNotThrow(() -> configService.getCategorySpells(null));
        }
    }
}