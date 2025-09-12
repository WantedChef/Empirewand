package nl.wantedchef.empirewand.core;

import nl.wantedchef.empirewand.core.config.ConfigMigrationService;
import nl.wantedchef.empirewand.core.config.ConfigValidator;
import nl.wantedchef.empirewand.core.config.ReadOnlyConfig;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ConfigService Migration Tests")
class ConfigServiceMigrationTest {

    @Mock
    private Plugin plugin;

    @Mock
    private FileConfiguration config;

    @Mock
    private FileConfiguration spellsConfig;

    @Mock
    private Logger logger;

    private ConfigService configService;
    private ConfigValidator validator;
    private ConfigMigrationService migrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup plugin mock
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getConfig()).thenReturn(config);
        
        // Initialize validator
        validator = new ConfigValidator();
    }

    @Nested
    @DisplayName("Migration Service Tests")
    class MigrationServiceTests {
        
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
        @DisplayName("Test getMigrationService returns non-null instance")
        void testGetMigrationServiceReturnsNonNullInstance() {
            ConfigMigrationService migrationService = configService.getMigrationService();
            assertNotNull(migrationService);
        }
        
        @Test
        @DisplayName("Test migration service is properly initialized")
        void testMigrationServiceIsProperlyInitialized() {
            ConfigMigrationService migrationService = configService.getMigrationService();
            
            // Verify it's the correct type
            assertTrue(migrationService instanceof ConfigMigrationService);
        }
    }

    @Nested
    @DisplayName("Migration Integration Tests")
    class MigrationIntegrationTests {
        
        @Test
        @DisplayName("Test ConfigService constructor initializes migration service correctly")
        void testConfigServiceConstructorInitializesMigrationServiceCorrectly() {
            // Setup mocks
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
            
            // Verify migration service is accessible
            ConfigMigrationService migrationService = service.getMigrationService();
            assertNotNull(migrationService);
        }
    }
}