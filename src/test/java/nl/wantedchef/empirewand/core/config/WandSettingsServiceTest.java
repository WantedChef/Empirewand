package nl.wantedchef.empirewand.core.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import nl.wantedchef.empirewand.core.config.model.WandDifficulty;
import nl.wantedchef.empirewand.core.config.model.WandSettings;

/**
 * Test class for WandSettingsService.
 * Tests configuration loading, saving, and validation functionality.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WandSettingsServiceTest {

    @Mock
    private Plugin mockPlugin;

    @Mock
    private Logger mockLogger;

    @TempDir
    Path tempDir;

    private WandSettingsService service;
    private File pluginDataDir;

    @BeforeEach
    void setUp() throws IOException {
        // Setup mock plugin
        pluginDataDir = tempDir.resolve("plugin").toFile();
        assertTrue(pluginDataDir.mkdirs());

        when(mockPlugin.getDataFolder()).thenReturn(pluginDataDir);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);

        // Create default config resource
        createDefaultConfigResource();

        // Create service
        service = new WandSettingsService(mockPlugin);
    }

    @Test
    void testInitialization() throws ExecutionException, InterruptedException {
        // Mock resource loading
        when(mockPlugin.getResource("wand-settings.yml"))
                .thenReturn(getClass().getResourceAsStream("/test-wand-settings.yml"));

        CompletableFuture<Void> initFuture = service.initialize();
        assertDoesNotThrow(() -> initFuture.get());

        // Verify default settings are loaded
        WandSettings defaultSettings = service.getDefaultSettings();
        assertNotNull(defaultSettings);
        assertEquals(WandDifficulty.MEDIUM, defaultSettings.getDifficulty());
    }

    @Test
    void testGetWandSettings() throws ExecutionException, InterruptedException {
        // Initialize service
        when(mockPlugin.getResource("wand-settings.yml"))
                .thenReturn(getClass().getResourceAsStream("/test-wand-settings.yml"));
        service.initialize().get();

        // Test getting settings for configured wand
        WandSettings settings = service.getWandSettings("empirewand");
        assertNotNull(settings);
        assertEquals("empirewand", settings.getWandKey());
        assertEquals(WandDifficulty.MEDIUM, settings.getDifficulty());
        assertFalse(settings.isCooldownBlock());
        assertTrue(settings.isGriefBlockDamage());
        assertTrue(settings.isPlayerDamage());

        // Test getting settings for non-configured wand (should return defaults)
        WandSettings unconfiguredSettings = service.getWandSettings("test_wand");
        assertNotNull(unconfiguredSettings);
        assertEquals("test_wand", unconfiguredSettings.getWandKey());
        // Should match defaults
        assertEquals(service.getDefaultSettings().getDifficulty(), unconfiguredSettings.getDifficulty());
    }

    @Test
    void testUpdateWandSettings() throws ExecutionException, InterruptedException {
        // Initialize service
        when(mockPlugin.getResource("wand-settings.yml"))
                .thenReturn(getClass().getResourceAsStream("/test-wand-settings.yml"));
        service.initialize().get();

        // Create updated settings
        WandSettings updatedSettings = WandSettings.builder("test_wand")
                .difficulty(WandDifficulty.HARD)
                .cooldownBlock(true)
                .griefBlockDamage(false)
                .playerDamage(false)
                .build();

        // Update settings
        CompletableFuture<Void> updateFuture = service.updateWandSettings(updatedSettings);
        assertDoesNotThrow(() -> updateFuture.get());

        // Verify settings were updated
        WandSettings retrievedSettings = service.getWandSettings("test_wand");
        assertEquals(WandDifficulty.HARD, retrievedSettings.getDifficulty());
        assertTrue(retrievedSettings.isCooldownBlock());
        assertFalse(retrievedSettings.isGriefBlockDamage());
        assertFalse(retrievedSettings.isPlayerDamage());
    }

    @Test
    void testGetConfiguredWandKeys() throws ExecutionException, InterruptedException {
        // Initialize service
        when(mockPlugin.getResource("wand-settings.yml"))
                .thenReturn(getClass().getResourceAsStream("/test-wand-settings.yml"));
        service.initialize().get();

        Set<String> wandKeys = service.getConfiguredWandKeys();
        assertNotNull(wandKeys);
        assertTrue(wandKeys.contains("empirewand"));
        assertTrue(wandKeys.contains("mephidantes_zeist"));
    }

    @Test
    void testRemoveWandSettings() throws ExecutionException, InterruptedException {
        // Initialize service
        when(mockPlugin.getResource("wand-settings.yml"))
                .thenReturn(getClass().getResourceAsStream("/test-wand-settings.yml"));
        service.initialize().get();

        // Add a custom wand
        WandSettings customSettings = WandSettings.builder("custom_wand")
                .difficulty(WandDifficulty.EASY)
                .build();
        service.updateWandSettings(customSettings).get();

        // Verify it exists
        assertTrue(service.getConfiguredWandKeys().contains("custom_wand"));

        // Remove it
        service.removeWandSettings("custom_wand").get();

        // Verify it's removed
        assertFalse(service.getConfiguredWandKeys().contains("custom_wand"));

        // Getting settings should return defaults now
        WandSettings retrievedSettings = service.getWandSettings("custom_wand");
        assertEquals(service.getDefaultSettings().getDifficulty(), retrievedSettings.getDifficulty());
    }

    @Test
    void testReload() throws ExecutionException, InterruptedException {
        // Initialize service
        when(mockPlugin.getResource("wand-settings.yml"))
                .thenReturn(getClass().getResourceAsStream("/test-wand-settings.yml"));
        service.initialize().get();

        // Touch service to ensure ready
        assertNotNull(service.getWandSettings("empirewand"));

        // Modify file directly (simulate external change)
        // This would be more complex to test properly, so we'll just test reload doesn't fail
        CompletableFuture<Void> reloadFuture = service.reload();
        assertDoesNotThrow(() -> reloadFuture.get());

        // Service should still be functional
        WandSettings reloadedSettings = service.getWandSettings("empirewand");
        assertNotNull(reloadedSettings);
    }

    @Test
    void testValidation() throws ExecutionException, InterruptedException {
        // Initialize service
        when(mockPlugin.getResource("wand-settings.yml"))
                .thenReturn(getClass().getResourceAsStream("/test-wand-settings.yml"));
        service.initialize().get();

        // Test null wand key validation
        assertThrows(NullPointerException.class, () ->
                service.getWandSettings(null));

        // Test empty wand key
        assertThrows(ExecutionException.class, () -> {
            WandSettings invalidSettings = WandSettings.builder("")
                    .difficulty(WandDifficulty.EASY)
                    .build();
            service.updateWandSettings(invalidSettings).get();
        });
    }

    @Test
    void testUninitializedService() {
        // Test that using service before initialization throws exception
        assertThrows(IllegalStateException.class, () ->
                service.getWandSettings("test"));
    }

    private void createDefaultConfigResource() throws IOException {
        // Create a test configuration file
        String testConfig = """
                version: "1.0.0"

                wands:
                  empirewand:
                    cooldownBlock: false
                    griefBlockDamage: true
                    playerDamage: true
                    difficulty: "MEDIUM"

                  mephidantes_zeist:
                    cooldownBlock: false
                    griefBlockDamage: true
                    playerDamage: true
                    difficulty: "MEDIUM"

                defaults:
                  cooldownBlock: false
                  griefBlockDamage: true
                  playerDamage: true
                  difficulty: "MEDIUM"
                """;

        Path configPath = tempDir.resolve("test-wand-settings.yml");
        Files.writeString(configPath, testConfig);

        // Create an InputStream that returns our test config
        when(mockPlugin.getResource("wand-settings.yml"))
                .thenAnswer(invocation -> Files.newInputStream(configPath));
    }
}