package nl.wantedchef.empirewand.framework.service;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.config.ConfigMigrationService;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@DisplayName("ConfigService Mock Tests")
class ConfigServiceMockTest {

    @Mock
    private EmpireWandPlugin plugin;

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
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
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
    @DisplayName("Method Existence Tests")
    class MethodExistenceTests {
        
        @Test
        @DisplayName("Test all public methods exist with correct signatures")
        void testAllPublicMethodsExist() {
            // Test constructor
            try {
                ConfigService.class.getConstructor(Plugin.class);
            } catch (NoSuchMethodException e) {
                fail("ConfigService constructor not found or has incorrect signature");
            }
            
            // Test all public methods
            assertDoesNotThrow(() -> ConfigService.class.getMethod("getConfig"));
            assertDoesNotThrow(() -> ConfigService.class.getMethod("getSpellsConfig"));
            assertDoesNotThrow(() -> ConfigService.class.getMethod("getMigrationService"));
            assertDoesNotThrow(() -> ConfigService.class.getMethod("getMessage", String.class));
            assertDoesNotThrow(() -> ConfigService.class.getMethod("getFeatureFlag", String.class));
            assertDoesNotThrow(() -> ConfigService.class.getMethod("getDefaultCooldown"));
            assertDoesNotThrow(() -> ConfigService.class.getMethod("getCategorySpells", String.class));
            assertDoesNotThrow(() -> ConfigService.class.getMethod("getCategoryNames"));
            assertDoesNotThrow(() -> ConfigService.class.getMethod("loadConfigs"));
        }
    }

    @Nested
    @DisplayName("Return Type Tests")
    class ReturnTypeTests {
        
        @Test
        @DisplayName("Test return types of all public methods")
        void testReturnTypes() throws NoSuchMethodException {
            assertEquals(ReadableConfig.class, ConfigService.class.getMethod("getConfig").getReturnType());
            assertEquals(ReadableConfig.class, ConfigService.class.getMethod("getSpellsConfig").getReturnType());
            assertEquals(ConfigMigrationService.class, ConfigService.class.getMethod("getMigrationService").getReturnType());
            assertEquals(String.class, ConfigService.class.getMethod("getMessage", String.class).getReturnType());
            assertEquals(boolean.class, ConfigService.class.getMethod("getFeatureFlag", String.class).getReturnType());
            assertEquals(long.class, ConfigService.class.getMethod("getDefaultCooldown").getReturnType());
            assertEquals(List.class, ConfigService.class.getMethod("getCategorySpells", String.class).getReturnType());
            assertEquals(Set.class, ConfigService.class.getMethod("getCategoryNames").getReturnType());
            assertEquals(void.class, ConfigService.class.getMethod("loadConfigs").getReturnType());
        }
    }

    @Nested
    @DisplayName("Configuration Method Tests")
    class ConfigurationMethodTests {
        
        @Test
        @DisplayName("Test loadConfigs method exists and is public")
        void testLoadConfigsMethod() {
            try {
                java.lang.reflect.Method method = ConfigService.class.getMethod("loadConfigs");
                assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
                assertEquals(void.class, method.getReturnType());
            } catch (NoSuchMethodException e) {
                fail("loadConfigs method not found or has incorrect signature");
            }
        }
    }
}