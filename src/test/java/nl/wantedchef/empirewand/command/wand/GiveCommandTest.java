package nl.wantedchef.empirewand.command.wand;

import nl.wantedchef.empirewand.core.util.PerformanceMonitor;
import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.service.WandServiceImpl;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for GiveCommand.
 * Tests command execution and validation functionality.
 */
@ExtendWith(MockitoExtension.class)
class GiveCommandTest {

    @Mock
    private CommandContext mockContext;

    @Mock
    private CommandSender mockSender;

    @Mock
    private Player mockTargetPlayer;

    @Mock
    private WandServiceImpl mockWandService;

    @Mock
    private Server mockServer;

    private GiveCommand giveCommand;
    private UUID targetPlayerId;

    @BeforeEach
    void setUp() {
        giveCommand = new GiveCommand("empirewand", "Empire Wand", Material.BLAZE_ROD);
        targetPlayerId = UUID.randomUUID();

        // Setup mock target player
        when(mockTargetPlayer.getUniqueId()).thenReturn(targetPlayerId);
        when(mockTargetPlayer.getName()).thenReturn("TargetPlayer");

        // Setup mock sender
        when(mockSender.getName()).thenReturn("AdminPlayer");

        // Setup mock context
        when(mockContext.sender()).thenReturn(mockSender);
        when(mockContext.wandService()).thenReturn(mockWandService);
        when(mockContext.startTiming(anyString())).thenReturn(mock(PerformanceMonitor.TimingContext.class));
    }

    @Test
    void testCommandProperties() {
        assertEquals("give", giveCommand.getName());
        assertEquals("empirewand.give", giveCommand.getPermission());
        assertEquals("give <player> [wandKey]", giveCommand.getUsage());
        assertEquals("Give a wand to a player", giveCommand.getDescription());
        assertFalse(giveCommand.requiresPlayer());
        assertEquals(List.of("grant"), giveCommand.getAliases());
    }

    @Test
    void testExecuteWithValidArguments() throws CommandException {
        // Setup command arguments
        String[] args = {"give", "TargetPlayer", "empirewand"};
        when(mockContext.args()).thenReturn(args);

        // Mock Bukkit.getPlayer to return our target player
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("TargetPlayer")).thenReturn(mockTargetPlayer);

            // Execute command
            assertDoesNotThrow(() -> giveCommand.execute(mockContext));

            // Verify wand service was called
            verify(mockWandService).giveWand(mockTargetPlayer);

            // Verify context interaction
            verify(mockContext).sendMessage(any());
        }
    }

    @Test
    void testExecuteWithMephidantesZeistWand() throws CommandException {
        // Setup command arguments for mephidantes_zeist wand
        String[] args = {"give", "TargetPlayer", "mephidantes_zeist"};
        when(mockContext.args()).thenReturn(args);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("TargetPlayer")).thenReturn(mockTargetPlayer);

            assertDoesNotThrow(() -> giveCommand.execute(mockContext));

            // Verify correct wand service method was called
            verify(mockWandService).giveMephidantesZeist(mockTargetPlayer);
        }
    }

    @Test
    void testExecuteWithDefaultWand() throws CommandException {
        // Setup command arguments without specifying wand type (should use default)
        String[] args = {"give", "TargetPlayer"};
        when(mockContext.args()).thenReturn(args);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("TargetPlayer")).thenReturn(mockTargetPlayer);

            assertDoesNotThrow(() -> giveCommand.execute(mockContext));

            // Should use default wand (empirewand for this command instance)
            verify(mockWandService).giveWand(mockTargetPlayer);
        }
    }

    @Test
    void testExecuteWithInvalidArguments() {
        // Test insufficient arguments
        String[] args = {"give"};
        when(mockContext.args()).thenReturn(args);

        CommandException exception = assertThrows(CommandException.class,
                () -> giveCommand.execute(mockContext));
        assertEquals("INVALID_USAGE", exception.getErrorCode());
    }

    @Test
    void testExecuteWithPlayerNotFound() {
        String[] args = {"give", "NonExistentPlayer", "empirewand"};
        when(mockContext.args()).thenReturn(args);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("NonExistentPlayer")).thenReturn(null);

            CommandException exception = assertThrows(CommandException.class,
                    () -> giveCommand.execute(mockContext));
            assertEquals("PLAYER_NOT_FOUND", exception.getErrorCode());
        }
    }

    @Test
    void testExecuteWithInvalidWandKey() {
        String[] args = {"give", "TargetPlayer", "invalid_wand"};
        when(mockContext.args()).thenReturn(args);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("TargetPlayer")).thenReturn(mockTargetPlayer);

            CommandException exception = assertThrows(CommandException.class,
                    () -> giveCommand.execute(mockContext));
            assertEquals("INVALID_WAND_KEY", exception.getErrorCode());
            assertTrue(exception.getMessage().contains("invalid_wand"));
        }
    }

    @Test
    void testTabCompletePlayerNames() {
        // Mock online players
        Collection<Player> onlinePlayers = List.of(mockTargetPlayer);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(onlinePlayers);

            // Test player name completion
            String[] args = {"give", "Target"};
            when(mockContext.args()).thenReturn(args);

            List<String> completions = giveCommand.tabComplete(mockContext);

            assertNotNull(completions);
            assertEquals(List.of("TargetPlayer"), completions);
        }
    }

    @Test
    void testTabCompleteWandKeys() {
        // Test wand key completion
        String[] args = {"give", "TargetPlayer", "empire"};
        when(mockContext.args()).thenReturn(args);

        List<String> completions = giveCommand.tabComplete(mockContext);

        assertNotNull(completions);
        assertTrue(completions.contains("empirewand"));
        assertFalse(completions.contains("mephidantes_zeist")); // Doesn't start with "empire"
    }

    @Test
    void testTabCompleteEmptyWandKey() {
        // Test completion with empty wand key
        String[] args = {"give", "TargetPlayer", ""};
        when(mockContext.args()).thenReturn(args);

        List<String> completions = giveCommand.tabComplete(mockContext);

        assertNotNull(completions);
        assertEquals(2, completions.size());
        assertTrue(completions.contains("empirewand"));
        assertTrue(completions.contains("mephidantes_zeist"));
    }

    @Test
    void testTabCompleteNoMoreArgs() {
        // Test completion with too many arguments
        String[] args = {"give", "TargetPlayer", "empirewand", "extra"};
        when(mockContext.args()).thenReturn(args);

        List<String> completions = giveCommand.tabComplete(mockContext);

        assertNotNull(completions);
        assertTrue(completions.isEmpty());
    }

    @Test
    void testWandServiceError() {
        String[] args = {"give", "TargetPlayer", "empirewand"};
        when(mockContext.args()).thenReturn(args);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("TargetPlayer")).thenReturn(mockTargetPlayer);

            // Mock wand service to throw exception
            doThrow(new RuntimeException("Wand service error"))
                    .when(mockWandService).giveWand(mockTargetPlayer);

            CommandException exception = assertThrows(CommandException.class,
                    () -> giveCommand.execute(mockContext));
            assertEquals("WAND_SERVICE_ERROR", exception.getErrorCode());
        }
    }

    @Test
    void testGetExamples() {
        var examples = giveCommand.getExamples();

        assertNotNull(examples);
        assertEquals(3, examples.size());

        // Verify example content
        assertTrue(examples.stream().anyMatch(ex -> ex.getCommand().contains("Steve")));
        assertTrue(examples.stream().anyMatch(ex -> ex.getCommand().contains("empirewand")));
        assertTrue(examples.stream().anyMatch(ex -> ex.getCommand().contains("mephidantes_zeist")));
    }

    @Test
    void testMephidantesZeistCommandVariant() {
        // Test command configured for mephidantes_zeist
        GiveCommand mzGiveCommand = new GiveCommand("mephidanteszeist", "Mephidantes Zeist", Material.GOLDEN_HOE);

        String[] args = {"give", "TargetPlayer"};
        when(mockContext.args()).thenReturn(args);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("TargetPlayer")).thenReturn(mockTargetPlayer);

            assertDoesNotThrow(() -> mzGiveCommand.execute(mockContext));

            // Should use mephidantes_zeist as default
            verify(mockWandService).giveMephidantesZeist(mockTargetPlayer);
        }
    }

    @Test
    void testWandDisplayNameFormatting() {
        // Test with various wand keys to verify display name formatting
        GiveCommand testCommand = new GiveCommand("test", "Test Wand", Material.STICK);

        // The formatWandDisplayName method is private, but we can test its effect
        // through the command execution and message sending
        String[] args = {"give", "TargetPlayer", "empirewand"};
        when(mockContext.args()).thenReturn(args);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("TargetPlayer")).thenReturn(mockTargetPlayer);

            assertDoesNotThrow(() -> testCommand.execute(mockContext));

            // Verify message was sent (display name formatting tested indirectly)
            verify(mockContext).sendMessage(any());
        }
    }
}