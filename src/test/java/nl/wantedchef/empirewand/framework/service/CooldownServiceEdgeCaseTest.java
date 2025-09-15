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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Cooldown Service Edge Case Tests")
class CooldownServiceEdgeCaseTest {

    private nl.wantedchef.empirewand.api.service.CooldownService cooldownService;
    private UnifiedCooldownManager unifiedManager;
    private UUID playerId;
    private String spellKey;
    private Plugin plugin;
    private ItemStack wand;
    private ItemMeta wandMeta;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        unifiedManager = new UnifiedCooldownManager(plugin);
        cooldownService = new nl.wantedchef.empirewand.api.impl.CooldownServiceAdapter(unifiedManager);
        playerId = UUID.randomUUID();
        spellKey = "test-spell";
        wand = mock(ItemStack.class);
        wandMeta = mock(ItemMeta.class);
    }

    @Nested
    @DisplayName("Cooldown Overwrite Tests")
    class CooldownOverwriteTests {

        @Test
        @DisplayName("Should overwrite shorter cooldown with longer cooldown")
        void shouldOverwriteShorterCooldownWithLongerCooldown() {
            long nowTicks = 1000L;
            long shortCooldown = 50L;
            long longCooldown = 200L;

            // Set short cooldown first
            cooldownService.set(playerId, spellKey, nowTicks + shortCooldown);
            assertEquals(shortCooldown, cooldownService.remaining(playerId, spellKey, nowTicks));

            // Overwrite with longer cooldown
            cooldownService.set(playerId, spellKey, nowTicks + longCooldown);
            assertEquals(longCooldown, cooldownService.remaining(playerId, spellKey, nowTicks));

            // Verify the longer cooldown is maintained
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + shortCooldown + 1));
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + longCooldown - 1));
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + longCooldown));
        }

        @Test
        @DisplayName("Should overwrite longer cooldown with shorter cooldown")
        void shouldOverwriteLongerCooldownWithShorterCooldown() {
            long nowTicks = 1000L;
            long shortCooldown = 50L;
            long longCooldown = 200L;

            // Set long cooldown first
            cooldownService.set(playerId, spellKey, nowTicks + longCooldown);
            assertEquals(longCooldown, cooldownService.remaining(playerId, spellKey, nowTicks));

            // Overwrite with shorter cooldown
            cooldownService.set(playerId, spellKey, nowTicks + shortCooldown);
            assertEquals(shortCooldown, cooldownService.remaining(playerId, spellKey, nowTicks));

            // Verify the shorter cooldown expires earlier
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + shortCooldown));
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + longCooldown));
        }

        @Test
        @DisplayName("Should handle immediate overwrite with zero cooldown")
        void shouldHandleImmediateOverwriteWithZeroCooldown() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set cooldown
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));

            // Overwrite with zero cooldown (immediate expiry)
            cooldownService.set(playerId, spellKey, nowTicks);
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
            assertEquals(0L, cooldownService.remaining(playerId, spellKey, nowTicks));
        }

        @Test
        @DisplayName("Should handle rapid successive overwrites")
        void shouldHandleRapidSuccessiveOverwrites() {
            long nowTicks = 1000L;

            // Rapid successive cooldown updates
            for (int i = 1; i <= 10; i++) {
                cooldownService.set(playerId, spellKey, nowTicks + (i * 10));
                assertEquals(i * 10, cooldownService.remaining(playerId, spellKey, nowTicks));
            }

            // Final cooldown should be the last one set
            assertEquals(100L, cooldownService.remaining(playerId, spellKey, nowTicks));
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + 99));
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + 100));
        }

        @Test
        @DisplayName("Should overwrite cooldown for different spells independently")
        void shouldOverwriteCooldownForDifferentSpellsIndependently() {
            long nowTicks = 1000L;
            String spell1 = "spell1";
            String spell2 = "spell2";

            // Set different cooldowns for different spells
            cooldownService.set(playerId, spell1, nowTicks + 100);
            cooldownService.set(playerId, spell2, nowTicks + 200);

            // Overwrite spell1 cooldown
            cooldownService.set(playerId, spell1, nowTicks + 50);

            // Verify spell1 has new cooldown, spell2 unchanged
            assertEquals(50L, cooldownService.remaining(playerId, spell1, nowTicks));
            assertEquals(200L, cooldownService.remaining(playerId, spell2, nowTicks));
        }
    }

    @Nested
    @DisplayName("Re-casting Before Expiry Tests")
    class ReCastingBeforeExpiryTests {

        @Test
        @DisplayName("Should handle re-casting with same cooldown duration")
        void shouldHandleReCastingWithSameCooldownDuration() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Initial cast
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));

            // Re-cast halfway through cooldown with same duration
            long halfwayPoint = nowTicks + (cooldownTicks / 2);
            cooldownService.set(playerId, spellKey, halfwayPoint + cooldownTicks);

            // Should now have full cooldown from the new cast time
            assertEquals(cooldownTicks, cooldownService.remaining(playerId, spellKey, halfwayPoint));
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, halfwayPoint + cooldownTicks - 1));
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, halfwayPoint + cooldownTicks));
        }

        @Test
        @DisplayName("Should handle re-casting with extended cooldown")
        void shouldHandleReCastingWithExtendedCooldown() {
            long nowTicks = 1000L;
            long initialCooldown = 100L;
            long extendedCooldown = 200L;

            // Initial cast
            cooldownService.set(playerId, spellKey, nowTicks + initialCooldown);

            // Re-cast before expiry with longer cooldown
            long reCastTime = nowTicks + 25; // 25% through original cooldown
            cooldownService.set(playerId, spellKey, reCastTime + extendedCooldown);

            // Should have extended cooldown from re-cast time
            assertEquals(extendedCooldown, cooldownService.remaining(playerId, spellKey, reCastTime));
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + initialCooldown + 50));
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, reCastTime + extendedCooldown));
        }

        @Test
        @DisplayName("Should handle re-casting with reduced cooldown")
        void shouldHandleReCastingWithReducedCooldown() {
            long nowTicks = 1000L;
            long initialCooldown = 200L;
            long reducedCooldown = 50L;

            // Initial cast
            cooldownService.set(playerId, spellKey, nowTicks + initialCooldown);

            // Re-cast before expiry with shorter cooldown
            long reCastTime = nowTicks + 30; // 15% through original cooldown
            cooldownService.set(playerId, spellKey, reCastTime + reducedCooldown);

            // Should have reduced cooldown from re-cast time
            assertEquals(reducedCooldown, cooldownService.remaining(playerId, spellKey, reCastTime));
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, reCastTime + reducedCooldown));

            // Original long cooldown should no longer apply
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + initialCooldown - 10));
        }

        @Test
        @DisplayName("Should handle re-casting at exactly one tick before expiry")
        void shouldHandleReCastingAtExactlyOneTickBeforeExpiry() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Initial cast
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);

            // Re-cast exactly one tick before expiry
            long almostExpired = nowTicks + cooldownTicks - 1;
            cooldownService.set(playerId, spellKey, almostExpired + cooldownTicks);

            // Should have full cooldown from the almost-expired time
            assertEquals(cooldownTicks, cooldownService.remaining(playerId, spellKey, almostExpired));
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, almostExpired + cooldownTicks - 1));
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, almostExpired + cooldownTicks));
        }

        @Test
        @DisplayName("Should handle multiple rapid re-casts")
        void shouldHandleMultipleRapidReCasts() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Initial cast
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);

            // Multiple rapid re-casts at different intervals
            long[] reCastTimes = {nowTicks + 10, nowTicks + 20, nowTicks + 35, nowTicks + 45};

            for (long reCastTime : reCastTimes) {
                cooldownService.set(playerId, spellKey, reCastTime + cooldownTicks);
                assertEquals(cooldownTicks, cooldownService.remaining(playerId, spellKey, reCastTime));
            }

            // Final cooldown should be from the last re-cast
            long finalReCastTime = reCastTimes[reCastTimes.length - 1];
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, finalReCastTime + cooldownTicks - 1));
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, finalReCastTime + cooldownTicks));
        }
    }

    @Nested
    @DisplayName("Disabled Cooldown Via Wand Tests")
    class DisabledCooldownViaWandTests {

        @BeforeEach
        void setupWand() {
            when(wand.getItemMeta()).thenReturn(wandMeta);
            when(wandMeta.hasDisplayName()).thenReturn(true);
            when(wandMeta.displayName()).thenReturn(net.kyori.adventure.text.Component.text("Test Wand"));
            when(wand.getType()).thenReturn(org.bukkit.Material.STICK);
        }

        @Test
        @DisplayName("Should bypass cooldown when disabled via wand")
        void shouldBypassCooldownWhenDisabledViaWand() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set cooldown
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));

            // Disable cooldown for this wand
            cooldownService.setCooldownDisabled(playerId, wand, true);

            // Should not be on cooldown when using the wand
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));

            // But should still be on cooldown without wand parameter
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));
        }

        @Test
        @DisplayName("Should re-enable cooldown when disabled flag is removed")
        void shouldReEnableCooldownWhenDisabledFlagIsRemoved() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set cooldown and disable it
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
            cooldownService.setCooldownDisabled(playerId, wand, true);
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));

            // Re-enable cooldown
            cooldownService.setCooldownDisabled(playerId, wand, false);

            // Should be on cooldown again
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));
        }

        @Test
        @DisplayName("Should handle disabled cooldown for multiple wands independently")
        void shouldHandleDisabledCooldownForMultipleWandsIndependently() {
            ItemStack wand2 = mock(ItemStack.class);
            ItemMeta wand2Meta = mock(ItemMeta.class);
            when(wand2.getItemMeta()).thenReturn(wand2Meta);
            when(wand2Meta.hasDisplayName()).thenReturn(true);
            when(wand2Meta.displayName()).thenReturn(net.kyori.adventure.text.Component.text("Test Wand 2"));
            when(wand2.getType()).thenReturn(org.bukkit.Material.BLAZE_ROD);

            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set cooldown
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);

            // Disable cooldown for first wand only
            cooldownService.setCooldownDisabled(playerId, wand, true);

            // First wand should not be on cooldown
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));

            // Second wand should still be on cooldown
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand2));
        }

        @Test
        @DisplayName("Should handle disabled cooldown for multiple players independently")
        void shouldHandleDisabledCooldownForMultiplePlayersIndependently() {
            UUID player2 = UUID.randomUUID();
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set cooldowns for both players
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
            cooldownService.set(player2, spellKey, nowTicks + cooldownTicks);

            // Disable cooldown for first player only
            cooldownService.setCooldownDisabled(playerId, wand, true);

            // First player should not be on cooldown with wand
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));

            // Second player should still be on cooldown with same wand
            assertTrue(cooldownService.isOnCooldown(player2, spellKey, nowTicks, wand));
        }

        @Test
        @DisplayName("Should handle disabled cooldown with null wand gracefully")
        void shouldHandleDisabledCooldownWithNullWandGracefully() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set cooldown
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);

            // Disable cooldown with null wand should not crash
            assertDoesNotThrow(() -> cooldownService.setCooldownDisabled(playerId, null, true));

            // Checking with null wand should return normal cooldown behavior
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, null));
        }

        @Test
        @DisplayName("Should persist disabled state across multiple spell casts")
        void shouldPersistDisabledStateAcrossMultipleSpellCasts() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Disable cooldown for wand
            cooldownService.setCooldownDisabled(playerId, wand, true);

            // Cast multiple spells with cooldowns
            for (int i = 0; i < 5; i++) {
                String spell = "spell-" + i;
                cooldownService.set(playerId, spell, nowTicks + cooldownTicks);

                // None should be on cooldown when using the disabled wand
                assertFalse(cooldownService.isOnCooldown(playerId, spell, nowTicks, wand));

                // But all should be on cooldown without wand
                assertTrue(cooldownService.isOnCooldown(playerId, spell, nowTicks));
            }
        }

        @Test
        @DisplayName("Should check disabled state correctly")
        void shouldCheckDisabledStateCorrectly() {
            // Initially not disabled
            assertFalse(cooldownService.isCooldownDisabled(playerId, wand));

            // Disable and check
            cooldownService.setCooldownDisabled(playerId, wand, true);
            assertTrue(cooldownService.isCooldownDisabled(playerId, wand));

            // Re-enable and check
            cooldownService.setCooldownDisabled(playerId, wand, false);
            assertFalse(cooldownService.isCooldownDisabled(playerId, wand));
        }
    }

    @Nested
    @DisplayName("Complex Edge Case Combinations")
    class ComplexEdgeCaseCombinations {

        @BeforeEach
        void setupWand() {
            when(wand.getItemMeta()).thenReturn(wandMeta);
            when(wandMeta.hasDisplayName()).thenReturn(true);
            when(wandMeta.displayName()).thenReturn(net.kyori.adventure.text.Component.text("Test Wand"));
            when(wand.getType()).thenReturn(org.bukkit.Material.STICK);
        }

        @Test
        @DisplayName("Should handle re-casting with disabled cooldown then re-enabling")
        void shouldHandleReCastingWithDisabledCooldownThenReEnabling() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Initial cast with cooldown
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks));

            // Disable cooldown and re-cast
            cooldownService.setCooldownDisabled(playerId, wand, true);
            cooldownService.set(playerId, spellKey, nowTicks + 50 + cooldownTicks);
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + 50, wand));

            // Re-enable cooldown
            cooldownService.setCooldownDisabled(playerId, wand, false);

            // Should now be on cooldown based on the re-cast time
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + 50, wand));
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks + 50 + cooldownTicks, wand));
        }

        @Test
        @DisplayName("Should handle overwriting cooldown while disabled then re-enabling")
        void shouldHandleOverwritingCooldownWhileDisabledThenReEnabling() {
            long nowTicks = 1000L;
            long shortCooldown = 50L;
            long longCooldown = 200L;

            // Set initial cooldown and disable
            cooldownService.set(playerId, spellKey, nowTicks + shortCooldown);
            cooldownService.setCooldownDisabled(playerId, wand, true);

            // Overwrite with longer cooldown while disabled
            cooldownService.set(playerId, spellKey, nowTicks + longCooldown);
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));

            // Re-enable cooldown
            cooldownService.setCooldownDisabled(playerId, wand, false);

            // Should have the longer cooldown
            assertTrue(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));
            assertEquals(longCooldown, cooldownService.remaining(playerId, spellKey, nowTicks));
        }

        @Test
        @DisplayName("Should handle clearing all cooldowns while some are disabled")
        void shouldHandleClearingAllCooldownsWhileSomeAreDisabled() {
            long nowTicks = 1000L;
            long cooldownTicks = 100L;

            // Set cooldowns and disable for wand
            cooldownService.set(playerId, spellKey, nowTicks + cooldownTicks);
            cooldownService.set(playerId, "spell2", nowTicks + cooldownTicks);
            cooldownService.setCooldownDisabled(playerId, wand, true);

            // Clear all cooldowns
            cooldownService.clearAll(playerId);

            // Should not be on cooldown even after re-enabling
            cooldownService.setCooldownDisabled(playerId, wand, false);
            assertFalse(cooldownService.isOnCooldown(playerId, spellKey, nowTicks, wand));
            assertFalse(cooldownService.isOnCooldown(playerId, "spell2", nowTicks, wand));
        }
    }
}