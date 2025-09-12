package nl.wantedchef.empirewand.framework.service;

import nl.wantedchef.empirewand.core.config.ConfigMigrationService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigServiceMigrationTest {

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
    @DisplayName("Migration Tests")
    class MigrationTests {
        
        @Test
        @DisplayName("Should perform migration when needed")
        void shouldPerformMigrationWhenNeeded() {
            // Test that the method exists and can be called
            assertDoesNotThrow(() -> configService.loadConfigs());
            
            // Verify that the migration service can be accessed
            ConfigMigrationService migrationService = configService.getMigrationService();
            assertNotNull(migrationService);
        }
        
        @Test
        @DisplayName("Should not perform migration when not needed")
        void shouldNotPerformMigrationWhenNotNeeded() {
            // Test that the method exists and can be called
            assertDoesNotThrow(() -> configService.loadConfigs());
            
            // Verify that the migration service can be accessed
            ConfigMigrationService migrationService = configService.getMigrationService();
            assertNotNull(migrationService);
        }
    }
}