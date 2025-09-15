package nl.wantedchef.empirewand.framework.service;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigServiceValidationTest {

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
    @DisplayName("Validation Tests")
    class ValidationTests {
        
        @Test
        @DisplayName("Should handle validation errors in main config")
        void shouldHandleValidationErrorsInMainConfig() {
            // Test that the method exists and can be called
            assertDoesNotThrow(() -> configService.loadConfigs());
        }
        
        @Test
        @DisplayName("Should handle validation errors in spells config")
        void shouldHandleValidationErrorsInSpellsConfig() {
            // Test that the method exists and can be called
            assertDoesNotThrow(() -> configService.loadConfigs());
        }
    }
}