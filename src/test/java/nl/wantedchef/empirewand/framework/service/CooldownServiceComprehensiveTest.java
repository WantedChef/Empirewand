package nl.wantedchef.empirewand.framework.service;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CooldownServiceComprehensiveTest {

    private CooldownService cooldownService;
    private UUID playerId;
    private String spellKey;
    private Plugin plugin;
    private ItemStack wand;
    private ItemMeta wandMeta;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        cooldownService = new CooldownService(plugin);
        playerId = UUID.randomUUID();
        spellKey = "test-spell";
        wand = mock(ItemStack.class);
        wandMeta = mock(ItemMeta.class);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        @Test
        @DisplayName("Should create CooldownService with default constructor")
        void shouldCreateCooldownServiceWithDefaultConstructor() {
            CooldownService service = new CooldownService();
            assertNotNull(service);
        }

        @Test
        @DisplayName("Should create CooldownService with plugin constructor")
        void shouldCreateCooldownServiceWithPluginConstructor() {
            CooldownService service = new CooldownService(plugin);
            assertNotNull(service);
        }
    }

    @Nested
    @DisplayName("Basic Cooldown Functionality Tests")
    class BasicCooldownFunctionalityTests {
        @Test
        @DisplayName("Should set and check cooldown correctly")
        void shouldSetAndCheckCooldownCorrectly() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Initially not on cooldown
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));

            // Set cooldown
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);

            // Should be on cooldown
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));

            // Should still be on cooldown before expiry
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + cooldownTicks - 1));

            // Should not be on cooldown at exact expiry time (nowTicks == until)
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + cooldownTicks));

            // Should not be on cooldown after expiry
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + cooldownTicks + 1));
        }

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            long nowTicks = 1000L;

            // Test with null playerId
            assertFalse(cooldownService.isOnCooldown(null, spellKey, nowTicks));
            assertEquals(0L, cooldownService.remaining(null, spellKey, nowTicks));

            // Test with null spellKey
            assertFalse(cooldownService.isOnCooldown(playerId, null, nowTicks));
            assertEquals(0L, cooldownService.remaining(playerId, null, nowTicks));

            // Test with both null
            assertFalse(cooldownService.isOnCooldown(null, null, nowTicks));
            assertEquals(0L, cooldownService.remaining(null, null, nowTicks));

            // Test set with null parameters
            assertDoesNotThrow(() -> cooldownService.set(null, spellKey, nowTicks));
            assertDoesNotThrow(() -> cooldownService.set(playerId, null, nowTicks));
            assertDoesNotThrow(() -> cooldownService.set(null, null, nowTicks));
        }

        @Test
        @DisplayName("Should calculate remaining cooldown time correctly")
        void shouldCalculateRemainingCooldownTimeCorrectly() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set cooldown
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);

            // Check remaining time
            assertEquals(cooldownTicks, cooldownService.remaining(playerId, spellKey, nowTicks));
            assertEquals(50L, cooldownService.remaining(playerId, spellKey, nowTicks + 50));
            assertEquals(0L, cooldownService.remaining(playerId, spellKey, nowTicks + cooldownTicks));
            assertEquals(0L, cooldownService.remaining(playerId, spellKey, nowTicks + cooldownTicks + 1));
        }

        @Test
        @DisplayName("Should clear all cooldowns for player")
        void shouldClearAllCooldownsForPlayer() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set cooldowns for multiple spells
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
            cooldownService.set(playerId, "spell2", nowTicks + cooldownTicks * 2);

            // Verify on cooldown
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
            assertTrue(cooldownService.isOnCooldown(playerId, "spell2", nowTicks));

            // Clear all cooldowns
            cooldownService.clearAll(playerId);

            // Should not be on cooldown anymore
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
            assertFalse(cooldownService.isOnCooldown(playerId, "spell2", nowTicks));
        }
    }

    @Nested
    @DisplayName("Wand-Specific Cooldown Tests")
    class WandSpecificCooldownTests {
        @Test
        @DisplayName("Should handle cooldown disabled functionality")
        void shouldHandleCooldownDisabledFunctionality() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set cooldown
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);

            // Should be on cooldown
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));

            // Disable cooldowns for this player-wand combination
            cooldownService.setCooldownDisabled(playerId, wand, true);

            // Should not be on cooldown when disabled
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));

            // Re-enable cooldowns
            cooldownService.setCooldownDisabled(playerId, wand, false);

            // Should be on cooldown again
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));
        }

        @Test
        @DisplayName("Should check if cooldowns are disabled")
        void shouldCheckIfCooldownsAreDisabled() {
            // Initially not disabled
            assertFalse(cooldownService.isCooldownDisabled(playerId, wand));

            // Disable cooldowns
            cooldownService.setCooldownDisabled(playerId, wand, true);
            assertTrue(cooldownService.isCooldownDisabled(playerId, wand));

            // Re-enable cooldowns
            cooldownService.setCooldownDisabled(playerId, wand, false);
            assertFalse(cooldownService.isCooldownDisabled(playerId, wand));
        }

        @Test
        @DisplayName("Should handle null parameters in wand-specific methods")
        void shouldHandleNullParametersInWandSpecificMethods() {
            long nowTicks = 1000L;

            // Test with null playerId
            assertFalse(cooldownService.isOnCooldown(null, spellKey, nowTicks, wand));
            assertFalse(cooldownService.isCooldownDisabled(null, wand));

            // Test with null wand
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, null));
            assertFalse(cooldownService.isCooldownDisabled(playerId, null));

            // Test with both null
            assertFalse(cooldownService.isOnCooldown(null, spellKey, nowTicks, null));
            assertFalse(cooldownService.isCooldownDisabled(null, null));

            // Test set methods with null parameters
            assertDoesNotThrow(() -> cooldownService.setCooldownDisabled(null, wand, true));
            assertDoesNotThrow(() -> cooldownService.setCooldownDisabled(playerId, null, true));
            assertDoesNotThrow(() -> cooldownService.setCooldownDisabled(null, null, true));
        }

        @Test
        @DisplayName("Should generate wand identifier correctly")
        void shouldGenerateWandIdentifierCorrectly() {
            // Test with valid wand and metadata
            when(wand.getItemMeta()).thenReturn(wandMeta);
            when(wandMeta.hasDisplayName()).thenReturn(true);
            when(wandMeta.displayName()).thenReturn(net.kyori.adventure.text.Component.text("Test Wand"));
            when(wand.getType()).thenReturn(org.bukkit.Material.STICK);

            // Should not throw exception
            assertDoesNotThrow(() -> cooldownService.isCooldownDisabled(playerId, wand));

            // Test with wand without metadata
            ItemStack wandWithoutMeta = mock(ItemStack.class);
            when(wandWithoutMeta.getItemMeta()).thenReturn(null);

            // Should not throw exception
            assertDoesNotThrow(() -> cooldownService.isCooldownDisabled(playerId, wandWithoutMeta));

            // Test with wand without display name
            when(wand.getItemMeta()).thenReturn(wandMeta);
            when(wandMeta.hasDisplayName()).thenReturn(false);

            // Should not throw exception
            assertDoesNotThrow(() -> cooldownService.isCooldownDisabled(playerId, wand));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {
        @Test
        @DisplayName("Should handle negative cooldown values")
        void shouldHandleNegativeCooldownValues() {
            long nowTicks = 1000L;

            // Set negative cooldown (should not be set)
            cooldownService.set(playerId, spellKey, -1L);

            // Should not be on cooldown
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        }

        @Test
        @DisplayName("Should handle very large cooldown values")
        void shouldHandleVeryLargeCooldownValues() {
            long nowTicks = 1000L;

            // Set very large cooldown
            cooldownService.set(playerId, spellKey, Long.MAX_VALUE);

            // Should be on cooldown
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        }

        @Test
        @DisplayName("Should handle multiple players and spells")
        void shouldHandleMultiplePlayersAndSpells() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            String spell1 = "spell1";
            String spell2 = "spell2";

            // Set cooldowns for different players and spells
            cooldownService.set(player1, spell1, nowTicks + cooldownTicks);
            cooldownService.set(player1, spell2, nowTicks + cooldownTicks * 2);
            cooldownService.set(player2, spell1, nowTicks + cooldownTicks * 3);

            // Player 1 should be on cooldown for both spells
            assertTrue(cooldownService.isOnCooldown(player1, spell1, nowTicks));
            assertTrue(cooldownService.isOnCooldown(player1, spell2, nowTicks));

            // Player 2 should be on cooldown only for spell1
            assertTrue(cooldownService.isOnCooldown(player2, spell1, nowTicks));
            assertFalse(cooldownService.isOnCooldown(player2, spell2, nowTicks));

            // After first spell expires for player1
            assertFalse(cooldownService.isOnCooldown(player1, spell1, nowTicks + cooldownTicks + 1));
            assertTrue(cooldownService.isOnCooldown(player1, spell2, nowTicks + cooldownTicks + 1));
        }

        @Test
        @DisplayName("Should clear all cooldowns for non-existent player")
        void shouldClearAllCooldownsForNonExistentPlayer() {
            UUID nonExistentPlayer = UUID.randomUUID();

            // Should not throw exception
            assertDoesNotThrow(() -> cooldownService.clearAll(nonExistentPlayer));

            // Should not be on cooldown
            assertFalse(cooldownService.isOnCooldown(nonExistentPlayer, spellKey, 1000L));
        }

        @Test
        @DisplayName("Should shutdown service correctly")
        void shouldShutdownServiceCorrectly() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set some cooldowns
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);

            // Verify on cooldown
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));

            // Shutdown service
            cooldownService.shutdown();

            // Should not be on cooldown after shutdown (data cleared)
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        }
    }
}