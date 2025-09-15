package nl.wantedchef.empirewand.framework.service;

import nl.wantedchef.empirewand.core.config.ConfigMigrationService;
import nl.wantedchef.empirewand.core.config.ReadOnlyConfig;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigServiceComprehensiveTest {

    private Plugin plugin;
    private FileConfiguration config;
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        config = mock(FileConfiguration.class);
        
        // Mock plugin methods that will be called during constructor
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(new File("test"));
        
        // Mock methods that will be called during loadConfigs
        doNothing().when(plugin).saveDefaultConfig();
        doNothing().when(plugin).reloadConfig();
        
        // Create ConfigService instance
        configService = new ConfigService(plugin);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should create ConfigService with valid plugin")
        void shouldCreateConfigServiceWithValidPlugin() {
            Plugin plugin = mock(Plugin.class);
            FileConfiguration config = mock(FileConfiguration.class);
            
            // Mock plugin methods that will be called during constructor
            when(plugin.getLogger()).thenReturn(mock(Logger.class));
            when(plugin.getConfig()).thenReturn(config);
            when(plugin.getDataFolder()).thenReturn(new File("test"));
            
            // Mock methods that will be called during loadConfigs
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            
            ConfigService configService = new ConfigService(plugin);
            assertNotNull(configService);
        }
        
        @Test
        @DisplayName("Should throw exception when plugin is null")
        void shouldThrowExceptionWhenPluginIsNull() {
            // This test needs to be isolated from the setUp method
            // We create a new instance without calling setUp
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
        @DisplayName("Should load configurations successfully")
        void shouldLoadConfigurationsSuccessfully() {
            // Call loadConfigs
            assertDoesNotThrow(() -> configService.loadConfigs());
            
            // Verify configurations are loaded
            assertNotNull(configService.getConfig());
            assertNotNull(configService.getSpellsConfig());
        }
        
        @Test
        @DisplayName("Should handle configuration loading errors gracefully")
        void shouldHandleConfigurationLoadingErrorsGracefully() {
            // Create a new plugin mock for this specific test
            Plugin testPlugin = mock(Plugin.class);
            when(testPlugin.getLogger()).thenReturn(mock(Logger.class));
            when(testPlugin.getDataFolder()).thenReturn(new File("test"));
            
            // Simulate exception during config loading
            when(testPlugin.getConfig()).thenThrow(new RuntimeException("Config loading error"));
            doNothing().when(testPlugin).saveDefaultConfig();
            doNothing().when(testPlugin).reloadConfig();
            
            // Create ConfigService with the mocked plugin - should handle exception gracefully
            assertDoesNotThrow(() -> {
                ConfigService testConfigService = new ConfigService(testPlugin);
                assertNotNull(testConfigService.getConfig());
                assertNotNull(testConfigService.getSpellsConfig());
            });
        }
    }

    @Nested
    @DisplayName("Configuration Access Tests")
    class ConfigurationAccessTests {
        
        @Test
        @DisplayName("Should return read-only config views")
        void shouldReturnReadOnlyConfigViews() {
            ReadableConfig mainConfig = configService.getConfig();
            ReadableConfig spellsConfig = configService.getSpellsConfig();
            
            assertNotNull(mainConfig);
            assertNotNull(spellsConfig);
            assertTrue(mainConfig instanceof ReadOnlyConfig);
            assertTrue(spellsConfig instanceof ReadOnlyConfig);
        }
    }

    @Nested
    @DisplayName("Migration Service Tests")
    class MigrationServiceTests {
        
        @Test
        @DisplayName("Should provide access to migration service")
        void shouldProvideAccessToMigrationService() {
            ConfigMigrationService migrationService = configService.getMigrationService();
            assertNotNull(migrationService);
        }
    }
}