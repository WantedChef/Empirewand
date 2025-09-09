package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CommandCooldownManager Tests")
class CommandCooldownManagerTest {

    private CommandCooldownManager cooldownManager;
    
    @Mock
    private EmpireWandPlugin plugin;
    
    @Mock
    private CommandSender sender;
    
    @Mock
    private Player player;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cooldownManager = new CommandCooldownManager(plugin);
    }

    @Nested
    @DisplayName("Global Cooldown Tests")
    class GlobalCooldownTests {
        
        @Test
        @DisplayName("Should not throw exception when no cooldown is set")
        void shouldNotThrowExceptionWhenNoCooldownIsSet() {
            assertDoesNotThrow(() -> {
                cooldownManager.checkCooldown(sender, "testcommand", 10);
            });
        }

        @Test
        @DisplayName("Should throw exception when global cooldown is active")
        void shouldThrowExceptionWhenGlobalCooldownIsActive() throws CommandException {
            // Set cooldown
            cooldownManager.checkCooldown(sender, "testcommand", 1); // 1 second cooldown
            
            // Try again immediately - should fail
            CommandException exception = assertThrows(CommandException.class, () -> {
                cooldownManager.checkCooldown(sender, "testcommand", 1);
            });
            
            assertTrue(exception.getMessage().contains("cooldown"));
            assertEquals("COMMAND_COOLDOWN", exception.getErrorCode());
        }

        @Test
        @DisplayName("Should allow command after cooldown expires")
        void shouldAllowCommandAfterCooldownExpires() throws CommandException {
            // Set cooldown
            cooldownManager.checkCooldown(sender, "testcommand", 0); // No cooldown
            
            // Should not throw exception
            assertDoesNotThrow(() -> {
                cooldownManager.checkCooldown(sender, "testcommand", 0);
            });
        }
    }

    @Nested
    @DisplayName("Player Cooldown Tests")
    class PlayerCooldownTests {
        
        private UUID playerId;
        
        @BeforeEach
        void setUpPlayer() {
            playerId = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerId);
        }

        @Test
        @DisplayName("Should handle player cooldowns separately from global cooldowns")
        void shouldHandlePlayerCooldownsSeparatelyFromGlobalCooldowns() throws CommandException {
            // Set global cooldown
            cooldownManager.checkCooldown(sender, "testcommand", 1);
            
            // Player should still be able to execute command
            assertDoesNotThrow(() -> {
                cooldownManager.checkCooldown(player, "testcommand", 1);
            });
        }

        @Test
        @DisplayName("Should enforce player-specific cooldowns")
        void shouldEnforcePlayerSpecificCooldowns() throws CommandException {
            // Set player cooldown
            cooldownManager.checkCooldown(player, "testcommand", 1);
            
            // Same player should be blocked
            CommandException exception = assertThrows(CommandException.class, () -> {
                cooldownManager.checkCooldown(player, "testcommand", 1);
            });
            
            assertTrue(exception.getMessage().contains("cooldown"));
        }

        @Test
        @DisplayName("Should allow different players to execute same command")
        void shouldAllowDifferentPlayersToExecuteSameCommand() throws CommandException {
            UUID player1Id = UUID.randomUUID();
            UUID player2Id = UUID.randomUUID();
            
            Player player1 = mock(Player.class);
            Player player2 = mock(Player.class);
            
            when(player1.getUniqueId()).thenReturn(player1Id);
            when(player2.getUniqueId()).thenReturn(player2Id);
            
            // Set cooldown for player 1
            cooldownManager.checkCooldown(player1, "testcommand", 1);
            
            // Player 2 should still be able to execute
            assertDoesNotThrow(() -> {
                cooldownManager.checkCooldown(player2, "testcommand", 1);
            });
        }
    }

    @Nested
    @DisplayName("Cooldown Clearing Tests")
    class CooldownClearingTests {
        
        private UUID playerId;
        
        @BeforeEach
        void setUpPlayer() {
            playerId = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerId);
        }

        @Test
        @DisplayName("Should clear player cooldowns")
        void shouldClearPlayerCooldowns() throws CommandException {
            // Set player cooldown
            cooldownManager.checkCooldown(player, "testcommand", 1);
            
            // Clear cooldowns
            cooldownManager.clearPlayerCooldowns(playerId);
            
            // Should be able to execute again
            assertDoesNotThrow(() -> {
                cooldownManager.checkCooldown(player, "testcommand", 1);
            });
        }

        @Test
        @DisplayName("Should clear specific player cooldown")
        void shouldClearSpecificPlayerCooldown() throws CommandException {
            // Set player cooldown
            cooldownManager.checkCooldown(player, "testcommand", 1);
            
            // Clear specific cooldown
            cooldownManager.clearPlayerCooldown(playerId, "testcommand");
            
            // Should be able to execute again
            assertDoesNotThrow(() -> {
                cooldownManager.checkCooldown(player, "testcommand", 1);
            });
        }

        @Test
        @DisplayName("Should clear global cooldowns")
        void shouldClearGlobalCooldowns() throws CommandException {
            // Set global cooldown
            cooldownManager.checkCooldown(sender, "testcommand", 1);
            
            // Clear all global cooldowns
            cooldownManager.clearGlobalCooldowns();
            
            // Should be able to execute again
            assertDoesNotThrow(() -> {
                cooldownManager.checkCooldown(sender, "testcommand", 1);
            });
        }

        @Test
        @DisplayName("Should clear specific global cooldown")
        void shouldClearSpecificGlobalCooldown() throws CommandException {
            // Set global cooldown
            cooldownManager.checkCooldown(sender, "testcommand", 1);
            
            // Clear specific cooldown
            cooldownManager.clearGlobalCooldown("testcommand");
            
            // Should be able to execute again
            assertDoesNotThrow(() -> {
                cooldownManager.checkCooldown(sender, "testcommand", 1);
            });
        }
    }

    @Nested
    @DisplayName("Custom Cooldown Key Tests")
    class CustomCooldownKeyTests {
        
        private UUID playerId;
        
        @BeforeEach
        void setUpPlayer() {
            playerId = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerId);
        }

        @Test
        @DisplayName("Should handle custom cooldown keys")
        void shouldHandleCustomCooldownKeys() throws CommandException {
            // Set cooldown with custom key
            cooldownManager.checkCooldown(player, "testcommand", "customkey", 1);
            
            // Same command with different key should be allowed
            assertDoesNotThrow(() -> {
                cooldownManager.checkCooldown(player, "testcommand", "differentkey", 1);
            });
            
            // Same command with same key should be blocked
            CommandException exception = assertThrows(CommandException.class, () -> {
                cooldownManager.checkCooldown(player, "testcommand", "customkey", 1);
            });
            
            assertTrue(exception.getMessage().contains("cooldown"));
        }
    }
}