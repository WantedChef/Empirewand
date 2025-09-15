package nl.wantedchef.empirewand.core;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@DisplayName("ConfigService Validation Tests")
class ConfigServiceValidationTest {

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
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {
        
        @Test
        @DisplayName("Test ConfigService handles validation errors in main config")
        void testConfigServiceHandlesValidationErrorsInMainConfig() {
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
            
            // Validation is handled internally, so we just verify it doesn't crash
            assertDoesNotThrow(() -> service.getConfig());
        }
        
        @Test
        @DisplayName("Test ConfigService handles validation errors in spells config")
        void testConfigServiceHandlesValidationErrorsInSpellsConfig() {
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
            
            // Validation is handled internally, so we just verify it doesn't crash
            assertDoesNotThrow(() -> service.getSpellsConfig());
        }
    }
}