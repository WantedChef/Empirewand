package nl.wantedchef.empirewand.framework.service;

import nl.wantedchef.empirewand.core.config.ReadOnlyConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigServiceUtilityMethodsTest {

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
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {
        
        @Test
        @DisplayName("Should get message with valid key")
        void shouldGetMessageWithValidKey() {
            String key = "test.message";
            String value = "Test Message";
            
            when(config.getString("messages." + key, "")).thenReturn(value);
            String result = configService.getMessage(key);
            assertEquals(value, result);
        }
        
        @Test
        @DisplayName("Should return empty string for null message key")
        void shouldReturnEmptyStringForNullMessageKey() {
            String result = configService.getMessage(null);
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("Should get feature flag with valid key")
        void shouldGetFeatureFlagWithValidKey() {
            String key = "test.feature";
            boolean value = true;
            
            when(config.getBoolean("features." + key, false)).thenReturn(value);
            boolean result = configService.getFeatureFlag(key);
            assertEquals(value, result);
        }
        
        @Test
        @DisplayName("Should return false for null feature flag key")
        void shouldReturnFalseForNullFeatureFlagKey() {
            boolean result = configService.getFeatureFlag(null);
            assertFalse(result);
        }
        
        @Test
        @DisplayName("Should get default cooldown")
        void shouldGetDefaultCooldown() {
            long value = 1000L;
            
            when(config.getLong("cooldowns.default", 500)).thenReturn(value);
            long result = configService.getDefaultCooldown();
            assertEquals(value, result);
        }
        
        @Test
        @DisplayName("Should get category spells")
        void shouldGetCategorySpells() {
            String categoryName = "testCategory";
            List<String> spells = List.of("spell1", "spell2");
            
            when(config.getStringList("categories." + categoryName + ".spells")).thenReturn(spells);
            List<String> result = configService.getCategorySpells(categoryName);
            assertEquals(spells, result);
        }
        
        @Test
        @DisplayName("Should return empty list for null category name")
        void shouldReturnEmptyListForNullCategoryName() {
            List<String> result = configService.getCategorySpells(null);
            assertEquals(Collections.emptyList(), result);
        }
        
        @Test
        @DisplayName("Should get category names")
        void shouldGetCategoryNames() {
            Set<String> result = configService.getCategoryNames();
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should get spell configuration")
        void shouldGetSpellConfiguration() {
            String spellKey = "test-spell";
            
            ReadOnlyConfig result = (ReadOnlyConfig) configService.getSpellConfig(spellKey);
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should return empty config for null spell key")
        void shouldReturnEmptyConfigForNullSpellKey() {
            ReadOnlyConfig result = (ReadOnlyConfig) configService.getSpellConfig(null);
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should return empty config for empty spell key")
        void shouldReturnEmptyConfigForEmptySpellKey() {
            ReadOnlyConfig result = (ReadOnlyConfig) configService.getSpellConfig("");
            assertNotNull(result);
        }
    }
}