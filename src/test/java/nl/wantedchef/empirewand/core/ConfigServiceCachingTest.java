package nl.wantedchef.empirewand.core;

import nl.wantedchef.empirewand.core.config.ReadOnlyConfig;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@DisplayName("ConfigService Caching Tests")
class ConfigServiceCachingTest {

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
    @DisplayName("Caching Behavior Tests")
    class CachingBehaviorTests {
        
        @Test
        @DisplayName("Test ConfigService caches config instances")
        void testConfigServiceCachesConfigInstances() {
            // Setup mocks
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            try {
                new File(dataFolder, "spells.yml").createNewFile();
            } catch (java.io.IOException e) {
                fail(e);
            }
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            doNothing().when(plugin).saveResource(anyString(), anyBoolean());
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Get config twice
            ReadableConfig firstConfig = service.getConfig();
            ReadableConfig secondConfig = service.getConfig();
            
            // Should be the same instance due to caching
            assertSame(firstConfig, secondConfig);
        }
        
        @Test
        @DisplayName("Test ConfigService caches spells config instances")
        void testConfigServiceCachesSpellsConfigInstances() {
            // Setup mocks
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            try {
                new File(dataFolder, "spells.yml").createNewFile();
            } catch (java.io.IOException e) {
                fail(e);
            }
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            doNothing().when(plugin).saveResource(anyString(), anyBoolean());
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Get spells config twice
            ReadableConfig firstConfig = service.getSpellsConfig();
            ReadableConfig secondConfig = service.getSpellsConfig();
            
            // Should be the same instance due to caching
            assertSame(firstConfig, secondConfig);
        }
        
        @Test
        @DisplayName("Test reload recreates cached instances")
        void testReloadRecreatesCachedInstances() {
            // Setup mocks
            when(config.contains("config-version")).thenReturn(true);
            when(config.get("config-version")).thenReturn("1.0");
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            try {
                new File(dataFolder, "spells.yml").createNewFile();
            } catch (java.io.IOException e) {
                fail(e);
            }
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            doNothing().when(plugin).saveResource(anyString(), anyBoolean());
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Get config before reload
            ReadableConfig configBefore = service.getConfig();
            
            // Setup for reload
            doNothing().when(plugin).reloadConfig();
            
            // Reload configs
            service.reload();
            
            // Get config after reload
            ReadableConfig configAfter = service.getConfig();
            
            // Should be different instances after reload
            assertNotSame(configBefore, configAfter);
            assertNotNull(configAfter);
        }
        
        @Test
        @DisplayName("Test null config handling in caching")
        void testNullConfigHandlingInCaching() {
            // Setup mocks to return null config
            when(plugin.getConfig()).thenReturn(null);
            when(plugin.getLogger()).thenReturn(logger);
            
            // Mock plugin.getDataFolder()
            File dataFolder = new File(System.getProperty("java.io.tmpdir"));
            when(plugin.getDataFolder()).thenReturn(dataFolder);
            
            // Mock plugin.saveDefaultConfig() and plugin.reloadConfig()
            doNothing().when(plugin).saveDefaultConfig();
            doNothing().when(plugin).reloadConfig();
            doNothing().when(plugin).saveResource(anyString(), anyBoolean());
            
            // Create service
            ConfigService service = new ConfigService(plugin);
            
            // Should return a default empty config instead of null
            ReadableConfig readableConfig = service.getConfig();
            assertNotNull(readableConfig);
            assertTrue(readableConfig instanceof ReadOnlyConfig);
        }
    }
}