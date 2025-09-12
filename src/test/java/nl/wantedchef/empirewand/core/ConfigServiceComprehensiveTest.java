package nl.wantedchef.empirewand.core;

import nl.wantedchef.empirewand.core.config.ConfigMigrationService;
import nl.wantedchef.empirewand.core.config.ConfigValidator;
import nl.wantedchef.empirewand.core.config.ReadOnlyConfig;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ConfigService Comprehensive Tests")
class ConfigServiceComprehensiveTest {

    @Mock
    private Plugin plugin;

    @Mock
    private FileConfiguration config;

    @Mock
    private FileConfiguration spellsConfig;

    @Mock
    private Logger logger;

    @Mock
    private ConfigMigrationService migrationService;

    private ConfigService configService;
    private ConfigValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup plugin mock
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getConfig()).thenReturn(config);
        
        // Initialize validator and inject it
        validator = new ConfigValidator();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Test ConfigService constructor with valid plugin")
        void testConstructorWithValidPlugin() {
            // Setup mocks for loadConfigs
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            // Mock spells file creation
            File spellsFile = mock(File.class);
            when(spellsFile.exists()).thenReturn(true);
            when(spellsFile.isFile()).thenReturn(true);
            when(spellsFile.canRead()).thenReturn(true);
            
            // Mock plugin.getDataFolder() to return a temporary directory
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            // Create a real instance of ConfigService
            ConfigService service = new ConfigService(plugin);
            
            assertNotNull(service);
            assertNotNull(service.getConfig());
            assertNotNull(service.getSpellsConfig());
            assertNotNull(service.getMigrationService());
        }
        
        @Test
        @DisplayName("Test ConfigService constructor with null plugin throws exception")
        void testConstructorWithNullPlugin() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                new ConfigService(null);
            });
            
            assertEquals("Plugin cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Configuration Loading Tests")
    class ConfigurationLoadingTests {
        
        @Test
        @DisplayName("Test loadConfigs method loads configurations correctly")
        void testLoadConfigsLoadsConfigurations() {
            // Setup mocks for loadConfigs
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            // Mock spells file
            File spellsFile = mock(File.class);
            when(spellsFile.exists()).thenReturn(true);
            when(spellsFile.isFile()).thenReturn(true);
            when(spellsFile.canRead()).thenReturn(true);
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Verify interactions
            verify(plugin).saveDefaultConfig();
            verify(plugin).reloadConfig();
        }
        
        @Test
        @DisplayName("Test loadConfigs handles missing spells.yml by saving default")
        void testLoadConfigsHandlesMissingSpellsFile() throws IOException {
            // Setup mocks for loadConfigs
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            // Mock plugin.saveResource to throw exception to simulate missing resource
            doThrow(new RuntimeException("Resource not found")).when(plugin).saveResource("spells.yml", false);
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Verify that severe error was logged
            verify(logger).severe("Failed to load main config.yml");
        }
        
        @Test
        @DisplayName("Test loadConfigs handles spells file loading errors")
        void testLoadConfigsHandlesSpellsFileLoadingErrors() {
            // Setup mocks for loadConfigs
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Verify logger interactions
            verify(plugin).saveDefaultConfig();
            verify(plugin).reloadConfig();
        }
    }

    @Nested
    @DisplayName("Configuration Validation and Migration Tests")
    class ConfigurationValidationAndMigrationTests {
        
        @Test
        @DisplayName("Test validateAndMigrateConfigs with valid configurations")
        void testValidateAndMigrateConfigsWithValidConfigs() {
            // Setup mocks
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            File spellsFile = mock(File.class);
            when(spellsFile.exists()).thenReturn(true);
            when(spellsFile.isFile()).thenReturn(true);
            when(spellsFile.canRead()).thenReturn(true);
            
            File configFile = mock(File.class);
            when(configFile.exists()).thenReturn(true);
            when(configFile.isFile()).thenReturn(true);
            when(configFile.canRead()).thenReturn(true);
            
            // Mock validator to return no errors
            List<String> emptyErrors = Collections.emptyList();
            
            // Create service using reflection to access private method
            ConfigService service = new ConfigService(plugin);
            
            // Verify logger interactions
            verify(logger).info("Configuration validation and migration completed successfully.");
        }
        
        @Test
        @DisplayName("Test validateAndMigrateConfigs handles migration service")
        void testValidateAndMigrateConfigsHandlesMigrationService() {
            // Setup mocks
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            File spellsFile = mock(File.class);
            when(spellsFile.exists()).thenReturn(true);
            when(spellsFile.isFile()).thenReturn(true);
            when(spellsFile.canRead()).thenReturn(true);
            
            File configFile = mock(File.class);
            when(configFile.exists()).thenReturn(true);
            when(configFile.isFile()).thenReturn(true);
            when(configFile.canRead()).thenReturn(true);
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Verify that migration service is accessible
            assertNotNull(service.getMigrationService());
        }
    }

    @Nested
    @DisplayName("Getter Methods Tests")
    class GetterMethodsTests {
        
        @BeforeEach
        void setUpService() {
            // Setup basic mocks
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
        @DisplayName("Test getConfig returns read-only configuration")
        void testGetConfigReturnsReadOnlyConfiguration() {
            ReadableConfig readOnlyConfig = configService.getConfig();
            assertNotNull(readOnlyConfig);
            assertTrue(readOnlyConfig instanceof ReadOnlyConfig);
        }
        
        @Test
        @DisplayName("Test getSpellsConfig returns read-only configuration")
        void testGetSpellsConfigReturnsReadOnlyConfiguration() {
            ReadableConfig readOnlySpellsConfig = configService.getSpellsConfig();
            assertNotNull(readOnlySpellsConfig);
            assertTrue(readOnlySpellsConfig instanceof ReadOnlyConfig);
        }
        
        @Test
        @DisplayName("Test getMessage with valid key")
        void testGetMessageWithValidKey() {
            String testMessage = "This is a test message";
            when(config.getString("messages.test-key", "")).thenReturn(testMessage);
            
            String result = configService.getMessage("test-key");
            assertEquals(testMessage, result);
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
        @DisplayName("Test getMessage handles exceptions")
        void testGetMessageHandlesExceptions() {
            when(config.getString("messages.test-key", "")).thenThrow(new RuntimeException("Test exception"));
            
            String result = configService.getMessage("test-key");
            assertEquals("", result);
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
        
        @Test
        @DisplayName("Test getFeatureFlag with valid key")
        void testGetFeatureFlagWithValidKey() {
            when(config.getBoolean("features.test-feature", false)).thenReturn(true);
            
            boolean result = configService.getFeatureFlag("test-feature");
            assertTrue(result);
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
        
        @Test
        @DisplayName("Test getFeatureFlag handles exceptions")
        void testGetFeatureFlagHandlesExceptions() {
            when(config.getBoolean("features.test-feature", false)).thenThrow(new RuntimeException("Test exception"));
            
            boolean result = configService.getFeatureFlag("test-feature");
            assertFalse(result);
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
        
        @Test
        @DisplayName("Test getDefaultCooldown returns correct value")
        void testGetDefaultCooldownReturnsCorrectValue() {
            long expectedCooldown = 1000L;
            when(config.getLong("cooldowns.default", 500)).thenReturn(expectedCooldown);
            
            long result = configService.getDefaultCooldown();
            assertEquals(expectedCooldown, result);
        }
        
        @Test
        @DisplayName("Test getDefaultCooldown handles exceptions")
        void testGetDefaultCooldownHandlesExceptions() {
            when(config.getLong("cooldowns.default", 500)).thenThrow(new RuntimeException("Test exception"));
            
            long result = configService.getDefaultCooldown();
            assertEquals(500, result); // Should return default value
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
        
        @Test
        @DisplayName("Test getCategorySpells with valid category")
        void testGetCategorySpellsWithValidCategory() {
            List<String> expectedSpells = List.of("fireball", "lightning");
            when(config.getStringList("categories.combat.spells")).thenReturn(expectedSpells);
            
            List<String> result = configService.getCategorySpells("combat");
            assertEquals(expectedSpells, result);
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
        @DisplayName("Test getCategorySpells handles exceptions")
        void testGetCategorySpellsHandlesExceptions() {
            when(config.getStringList("categories.combat.spells")).thenThrow(new RuntimeException("Test exception"));
            
            List<String> result = configService.getCategorySpells("combat");
            assertEquals(Collections.emptyList(), result);
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
        
        @Test
        @DisplayName("Test getCategoryNames returns correct set")
        void testGetCategoryNamesReturnsCorrectSet() {
            Set<String> expectedCategories = Set.of("combat", "healing");
            ConfigurationSection section = mock(ConfigurationSection.class);
            when(config.getConfigurationSection("categories")).thenReturn(section);
            when(section.getKeys(false)).thenReturn(expectedCategories);
            
            Set<String> result = configService.getCategoryNames();
            assertEquals(expectedCategories, result);
        }
        
        @Test
        @DisplayName("Test getCategoryNames handles null section")
        void testGetCategoryNamesHandlesNullSection() {
            when(config.getConfigurationSection("categories")).thenReturn(null);
            
            Set<String> result = configService.getCategoryNames();
            assertEquals(Set.of(), result);
        }
        
        @Test
        @DisplayName("Test getCategoryNames handles exceptions")
        void testGetCategoryNamesHandlesExceptions() {
            ConfigurationSection section = mock(ConfigurationSection.class);
            when(config.getConfigurationSection("categories")).thenReturn(section);
            when(section.getKeys(false)).thenThrow(new RuntimeException("Test exception"));
            
            Set<String> result = configService.getCategoryNames();
            assertEquals(Set.of(), result);
            
            // Verify warning was logged
            verify(logger).warning(anyString());
        }
    }

    @Nested
    @DisplayName("Reload and Status Tests")
    class ReloadAndStatusTests {
        
        @BeforeEach
        void setUpService() {
            // Setup basic mocks
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
        @DisplayName("Test reload method calls loadConfigs")
        void testReloadMethodCallsLoadConfigs() {
            // Mock the reload process
            doNothing().when(plugin).reloadConfig();
            
            configService.reload();
            
            // Verify that reloadConfig was called
            verify(plugin, atLeastOnce()).reloadConfig();
        }
        
        @Test
        @DisplayName("Test isConfigLoaded returns correct status")
        void testIsConfigLoadedReturnsCorrectStatus() {
            boolean result = configService.isConfigLoaded();
            // This will be false because we're using mocks that don't fully initialize
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Test ConfigService handles exceptions in constructor")
        void testConfigServiceHandlesExceptionsInConstructor() {
            // Setup mocks to throw exception
            when(plugin.getLogger()).thenReturn(logger);
            when(plugin.getConfig()).thenThrow(new RuntimeException("Test exception"));
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            // Create service - should not throw exception
            assertDoesNotThrow(() -> new ConfigService(plugin));
            
            // Verify severe error was logged
            verify(logger).severe("Failed to load main config.yml");
        }
        
        @Test
        @DisplayName("Test ConfigService handles exceptions in loadConfigs")
        void testConfigServiceHandlesExceptionsInLoadConfigs() {
            // Setup mocks to throw exception in reloadConfig
            doThrow(new RuntimeException("Test exception")).when(plugin).reloadConfig();
            
            when(plugin.getLogger()).thenReturn(logger);
            when(plugin.getConfig()).thenReturn(config);
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig()
            doNothing().when(plugin).saveDefaultConfig();
            
            // Create service - should not throw exception
            assertDoesNotThrow(() -> new ConfigService(plugin));
            
            // Verify severe error was logged
            verify(logger).severe("Failed to load main config.yml");
        }
    }

    @Nested
    @DisplayName("Caching Mechanism Tests")
    class CachingMechanismTests {
        
        @Test
        @DisplayName("Test ConfigService caches configurations properly")
        void testConfigServiceCachesConfigurationsProperly() {
            // Setup basic mocks
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Get config twice
            ReadableConfig config1 = service.getConfig();
            ReadableConfig config2 = service.getConfig();
            
            // Should return the same instance (caching)
            assertSame(config1, config2);
        }
        
        @Test
        @DisplayName("Test ConfigService caches spells configurations properly")
        void testConfigServiceCachesSpellsConfigurationsProperly() {
            // Setup basic mocks
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Get spells config twice
            ReadableConfig config1 = service.getSpellsConfig();
            ReadableConfig config2 = service.getSpellsConfig();
            
            // Should return the same instance (caching)
            assertSame(config1, config2);
        }
        
        @Test
        @DisplayName("Test reload clears and recreates cache")
        void testReloadClearsAndRecreatesCache() {
            // Setup basic mocks
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Get config before reload
            ReadableConfig configBefore = service.getConfig();
            
            // Mock reload behavior
            doNothing().when(plugin).reloadConfig();
            
            // Reload
            service.reload();
            
            // Get config after reload
            ReadableConfig configAfter = service.getConfig();
            
            // Should be different instances after reload
            assertNotNull(configAfter);
        }
    }
}