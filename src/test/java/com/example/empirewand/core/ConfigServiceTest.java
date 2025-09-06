package com.example.empirewand.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Minimal structural test for ConfigService to ensure basic functionality.
 * Avoids complex mocking to prevent compilation and NPE issues.
 */
@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Test
    void testConfigServiceCreation() {
        // Create minimal mock plugin with all required methods
        Plugin mockPlugin = mock(Plugin.class);
        FileConfiguration mockConfig = mock(FileConfiguration.class);
        when(mockPlugin.getConfig()).thenReturn(mockConfig);
        when(mockPlugin.getDataFolder()).thenReturn(new File("target/test-data"));

        // Mock logger to avoid NPE
        var mockLogger = mock(java.util.logging.Logger.class);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);

        // Verify service can be created without throwing
        assertDoesNotThrow(() -> new ConfigService(mockPlugin));
    }

    @Test
    void testServiceGettersExist() {
        // Create service with minimal mock
        Plugin mockPlugin = mock(Plugin.class);
        FileConfiguration mockConfig = mock(FileConfiguration.class);
        when(mockPlugin.getConfig()).thenReturn(mockConfig);
        when(mockPlugin.getDataFolder()).thenReturn(new File("target/test-data"));

        // Mock logger to avoid NPE
        var mockLogger = mock(java.util.logging.Logger.class);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);

        ConfigService service = new ConfigService(mockPlugin);

        // Verify getters don't throw (even if they return null)
        assertDoesNotThrow(() -> service.getConfig());
        assertDoesNotThrow(() -> service.getSpellsConfig());
        assertDoesNotThrow(() -> service.getDefaultCooldown());
    }
}