package nl.wantedchef.empirewand.command.wand;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.service.metrics.DebugMetricsService;
import nl.wantedchef.empirewand.framework.service.metrics.MetricsService;
import nl.wantedchef.empirewand.api.service.WandService;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("StatsCommand Tests")
class StatsCommandTest {

    private StatsCommand statsCommand;
    
    @Mock
    private EmpireWandPlugin plugin;
    
    @Mock
    private CommandContext context;
    
    @Mock
    private CommandSender sender;
    
    @Mock
    private Player player;
    
    @Mock
    private Server server;
    
    @Mock
    private MetricsService metricsService;
    
    @Mock
    private DebugMetricsService debugMetricsService;
    
    @Mock
    private WandService wandService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        statsCommand = new StatsCommand("empirewand");
        
        when(context.plugin()).thenReturn(plugin);
        when(context.sender()).thenReturn(sender);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getMetricsService()).thenReturn(metricsService);
        when(plugin.getDebugMetricsService()).thenReturn(debugMetricsService);
        when(context.wandService()).thenReturn(wandService);
    }

    @Nested
    @DisplayName("Basic Properties Tests")
    class BasicPropertiesTests {
        
        @Test
        @DisplayName("Should return correct command name")
        void shouldReturnCorrectCommandName() {
            assertEquals("stats", statsCommand.getName());
        }

        @Test
        @DisplayName("Should return correct permission")
        void shouldReturnCorrectPermission() {
            assertEquals("empirewand.command.stats", statsCommand.getPermission());
        }

        @Test
        @DisplayName("Should return correct usage")
        void shouldReturnCorrectUsage() {
            assertEquals("stats [player]", statsCommand.getUsage());
        }

        @Test
        @DisplayName("Should return correct description")
        void shouldReturnCorrectDescription() {
            assertEquals("Display detailed statistics about wand usage and performance", 
                statsCommand.getDescription());
        }

        @Test
        @DisplayName("Should not require player")
        void shouldNotRequirePlayer() {
            assertFalse(statsCommand.requiresPlayer());
        }
    }

    @Nested
    @DisplayName("Execution Tests")
    class ExecutionTests {
        
        @Test
        @DisplayName("Should throw exception when no player specified from console")
        void shouldThrowExceptionWhenNoPlayerSpecifiedFromConsole() {
            when(context.sender()).thenReturn(sender);
            when(context.args()).thenReturn(new String[]{"stats"});
            
            CommandException exception = assertThrows(CommandException.class, () -> {
                statsCommand.execute(context);
            });
            
            assertTrue(exception.getMessage().contains("must specify a player"));
            assertEquals("CONSOLE_REQUIRES_PLAYER", exception.getErrorCode());
        }

        @Test
        @DisplayName("Should throw exception for unknown player")
        void shouldThrowExceptionForUnknownPlayer() {
            when(context.args()).thenReturn(new String[]{"stats", "unknown"});
            when(server.getPlayer("unknown")).thenReturn(null);
            
            CommandException exception = assertThrows(CommandException.class, () -> {
                statsCommand.execute(context);
            });
            
            assertTrue(exception.getMessage().contains("Player not found"));
            assertEquals("PLAYER_NOT_FOUND", exception.getErrorCode());
        }

        @Test
        @DisplayName("Should execute for valid player target")
        void shouldExecuteForValidPlayerTarget() throws CommandException {
            when(context.args()).thenReturn(new String[]{"stats", "testplayer"});
            when(server.getPlayer("testplayer")).thenReturn(player);
            when(player.getName()).thenReturn("TestPlayer");
            
            // Mock metrics service
            when(debugMetricsService.getTotalSpellCasts()).thenReturn(100L);
            when(debugMetricsService.getTotalFailedCasts()).thenReturn(5L);
            when(debugMetricsService.getSpellCastSuccessRate()).thenReturn(95.0);
            when(debugMetricsService.getSpellCastP95()).thenReturn(120L);
            
            assertDoesNotThrow(() -> {
                statsCommand.execute(context);
            });
        }
    }

    @Nested
    @DisplayName("Tab Completion Tests")
    class TabCompletionTests {
        
        @Test
        @DisplayName("Should complete player names")
        void shouldCompletePlayerNames() {
            Player player1 = mock(Player.class);
            Player player2 = mock(Player.class);
            
            when(player1.getName()).thenReturn("Alice");
            when(player2.getName()).thenReturn("Bob");
            when(server.getOnlinePlayers()).thenReturn(new java.util.ArrayList<>());
            when(context.args()).thenReturn(new String[]{"stats", "A"});
            
            List<String> completions = statsCommand.tabComplete(context);
            assertEquals(1, completions.size());
            assertEquals("Alice", completions.get(0));
        }

        @Test
        @DisplayName("Should return empty list for wrong argument position")
        void shouldReturnEmptyListForWrongArgumentPosition() {
            when(context.args()).thenReturn(new String[]{"stats"});
            
            List<String> completions = statsCommand.tabComplete(context);
            assertTrue(completions.isEmpty());
        }
    }

    @Nested
    @DisplayName("Examples Tests")
    class ExamplesTests {
        
        @Test
        @DisplayName("Should provide command examples")
        void shouldProvideCommandExamples() {
            var examples = statsCommand.getExamples();
            assertEquals(2, examples.size());
            
            var example1 = examples.get(0);
            assertEquals("stats", example1.getCommand());
            assertTrue(example1.getDescription().contains("your own"));
            
            var example2 = examples.get(1);
            assertEquals("stats PlayerName", example2.getCommand());
            assertTrue(example2.getDescription().contains("specific player"));
        }
    }
}