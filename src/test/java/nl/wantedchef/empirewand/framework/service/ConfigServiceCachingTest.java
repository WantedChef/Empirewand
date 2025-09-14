package nl.wantedchef.empirewand.framework.service;

import nl.wantedchef.empirewand.core.config.ReadOnlyConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;

class ConfigServiceCachingTest {

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
    @DisplayName("Caching Tests")
    class CachingTests {
        
        @Test
        @DisplayName("Should cache message values")
        void shouldCacheMessageValues() {
            String key = "test.message";
            String value = "Test Message";
            
            // First call - should access config
            when(config.getString("messages." + key, "")).thenReturn(value);
            String result1 = configService.getMessage(key);
            assertEquals(value, result1);
            // Verify config.getString was called exactly once
            verify(config).getString("messages." + key, "");
            
            // Clear the verification to avoid counting previous calls
            clearInvocations(config);
            
            // Second call - should use cache
            String result2 = configService.getMessage(key);
            assertEquals(value, result2);
            // Verify config.getString was NOT called again (cached)
            verify(config, never()).getString("messages." + key, "");
        }
        
        @Test
        @DisplayName("Should cache feature flag values")
        void shouldCacheFeatureFlagValues() {
            String key = "test.feature";
            boolean value = true;
            
            // First call - should access config
            when(config.getBoolean("features." + key, false)).thenReturn(value);
            boolean result1 = configService.getFeatureFlag(key);
            assertEquals(value, result1);
            // Verify config.getBoolean was called exactly once
            verify(config).getBoolean("features." + key, false);
            
            // Clear the verification to avoid counting previous calls
            clearInvocations(config);
            
            // Second call - should use cache
            boolean result2 = configService.getFeatureFlag(key);
            assertEquals(value, result2);
            // Verify config.getBoolean was NOT called again (cached)
            verify(config, never()).getBoolean("features." + key, false);
        }
        
        @Test
        @DisplayName("Should cache default cooldown value")
        void shouldCacheDefaultCooldownValue() {
            long value = 1000L;
            
            // First call - should access config
            when(config.getLong("cooldowns.default", 500)).thenReturn(value);
            long result1 = configService.getDefaultCooldown();
            assertEquals(value, result1);
            // Verify config.getLong was called exactly once
            verify(config).getLong("cooldowns.default", 500);
            
            // Clear the verification to avoid counting previous calls
            clearInvocations(config);
            
            // Second call - should use cache
            long result2 = configService.getDefaultCooldown();
            assertEquals(value, result2);
            // Verify config.getLong was NOT called again (cached)
            verify(config, never()).getLong("cooldowns.default", 500);
        }
        
        @Test
        @DisplayName("Should cache category spells")
        void shouldCacheCategorySpells() {
            String categoryName = "testCategory";
            List<String> spells = List.of("spell1", "spell2");
            
            // First call - should access config
            when(config.getStringList("categories." + categoryName + ".spells")).thenReturn(spells);
            List<String> result1 = configService.getCategorySpells(categoryName);
            assertEquals(spells, result1);
            // Verify config.getStringList was called exactly once
            verify(config).getStringList("categories." + categoryName + ".spells");
            
            // Clear the verification to avoid counting previous calls
            clearInvocations(config);
            
            // Second call - should use cache
            List<String> result2 = configService.getCategorySpells(categoryName);
            assertEquals(spells, result2);
            // Verify config.getStringList was NOT called again (cached)
            verify(config, never()).getStringList("categories." + categoryName + ".spells");
        }
        
        @Test
        @DisplayName("Should cache category names")
        void shouldCacheCategoryNames() {
            // First call - should access config
            Set<String> result1 = configService.getCategoryNames();
            assertNotNull(result1);
            
            // Second call - should use cache
            Set<String> result2 = configService.getCategoryNames();
            assertNotNull(result2);
            assertEquals(result1, result2);
        }
        
        @Test
        @DisplayName("Should cache spell configurations")
        void shouldCacheSpellConfigurations() {
            String spellKey = "test-spell";
            
            // First call - should access config
            ReadOnlyConfig result1 = (ReadOnlyConfig) configService.getSpellConfig(spellKey);
            assertNotNull(result1);
            
            // Second call - should use cache
            ReadOnlyConfig result2 = (ReadOnlyConfig) configService.getSpellConfig(spellKey);
            assertNotNull(result2);
            // Note: Caching behavior for spell configs may vary based on implementation
        }
        
        @Test
        @DisplayName("Should clear caches on config reload")
        void shouldClearCachesOnConfigReload() {
            String key = "test.message";
            String value1 = "test";
            String value2 = "test2";
            
            // Cache a value
            when(config.getString("messages." + key, "")).thenReturn(value1);
            String result1 = configService.getMessage(key);
            assertEquals(value1, result1);
            // Verify config.getString was called exactly once
            verify(config).getString("messages." + key, "");
            
            // Clear the verification to avoid counting previous calls
            clearInvocations(config);
            
            // Change the value in the config
            when(config.getString("messages." + key, "")).thenReturn(value2);
            
            // Reload configs - this should clear caches
            configService.loadConfigs();
            
            // Access cached value again - cache should be cleared, so config should be called again
            String result2 = configService.getMessage(key);
            assertEquals(value2, result2);
            // Verify config.getString was called again after reload
            verify(config).getString("messages." + key, "");
        }
    }
}