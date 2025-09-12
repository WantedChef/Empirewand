package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider.CommandExample;
import nl.wantedchef.empirewand.api.service.PermissionService;
import nl.wantedchef.empirewand.framework.service.ConfigService;
import nl.wantedchef.empirewand.framework.service.CooldownService;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.EmpireWandPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@DisplayName("HelpCommand Tests")
class HelpCommandTest {

    private HelpCommand helpCommand;
    private Map<String, SubCommand> commands;
    private Map<String, SubCommand> aliases;

    @Mock
    private CommandSender sender;

    @Mock
    private SubCommand testCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        commands = new HashMap<>();
        aliases = new HashMap<>();
        helpCommand = new HelpCommand("empirewand", "ew", commands, aliases);
    }

    private CommandContext createContext(CommandSender sender, String... args) {
        PermissionService permission = mock(PermissionService.class);
        when(permission.has(any(), any())).thenReturn(true);
        return new CommandContext(
            mock(EmpireWandPlugin.class),
            sender,
            args,
            mock(ConfigService.class),
            mock(FxService.class),
            mock(SpellRegistry.class),
            mock(WandService.class),
            mock(CooldownService.class),
            permission
        );
    }

    @Nested
    @DisplayName("Basic Properties Tests")
    class BasicPropertiesTests {

        @Test
        @DisplayName("Should return correct command name")
        void shouldReturnCorrectCommandName() {
            assertEquals("help", helpCommand.getName());
        }

        @Test
        @DisplayName("Should return correct aliases")
        void shouldReturnCorrectAliases() {
            List<String> aliases = helpCommand.getAliases();
            assertEquals(2, aliases.size());
            assertTrue(aliases.contains("help"));
            assertTrue(aliases.contains("?"));
        }

        @Test
        @DisplayName("Should not require permission")
        void shouldNotRequirePermission() {
            assertNull(helpCommand.getPermission());
        }

        @Test
        @DisplayName("Should return correct usage")
        void shouldReturnCorrectUsage() {
            assertEquals("help [command]", helpCommand.getUsage());
        }

        @Test
        @DisplayName("Should return correct description")
        void shouldReturnCorrectDescription() {
            assertEquals("Show help for commands", helpCommand.getDescription());
        }
    }

    @Nested
    @DisplayName("Execution Tests")
    class ExecutionTests {

        @Test
        @DisplayName("Should show general help when no arguments")
        void shouldShowGeneralHelpWhenNoArguments() throws CommandException {
            CommandContext context = createContext(sender, "help");

            helpCommand.execute(context);

            verify(sender).sendMessage((Component) any());
        }

        @Test
        @DisplayName("Should show specific command help when command specified")
        void shouldShowSpecificCommandHelpWhenCommandSpecified() throws CommandException {
            // Register a test command
            when(testCommand.getName()).thenReturn("test");
            when(testCommand.getUsage()).thenReturn("test");
            when(testCommand.getDescription()).thenReturn("Test command");
            when(testCommand.getPermission()).thenReturn(null);
            commands.put("test", testCommand);

            CommandContext context = createContext(sender, "help", "test");

            helpCommand.execute(context);

            verify(sender).sendMessage((Component) any());
        }

        @Test
        @DisplayName("Should throw exception for unknown command")
        void shouldThrowExceptionForUnknownCommand() {
            CommandContext context = createContext(sender, "help", "unknown");

            CommandException exception = assertThrows(CommandException.class, () -> {
                helpCommand.execute(context);
            });

            assertTrue(exception.getMessage().contains("Unknown command"));
            assertEquals("UNKNOWN_COMMAND", exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Tab Completion Tests")
    class TabCompletionTests {

        @Test
        @DisplayName("Should complete command names")
        void shouldCompleteCommandNames() {
            // Register a test command
            when(testCommand.getName()).thenReturn("testcommand");
            when(testCommand.getPermission()).thenReturn(null);
            commands.put("testcommand", testCommand);

            CommandContext context = createContext(sender, "help", "test");

            List<String> completions = helpCommand.tabComplete(context);
            assertEquals(1, completions.size());
            assertEquals("testcommand", completions.get(0));
        }

        @Test
        @DisplayName("Should return empty list for no matches")
        void shouldReturnEmptyListForNoMatches() {
            CommandContext context = createContext(sender, "help", "xyz");

            List<String> completions = helpCommand.tabComplete(context);
            assertTrue(completions.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list for wrong argument position")
        void shouldReturnEmptyListForWrongArgumentPosition() {
            CommandContext context = createContext(sender, "help");

            List<String> completions = helpCommand.tabComplete(context);
            assertTrue(completions.isEmpty());
        }
    }

    @Nested
    @DisplayName("HelpAwareCommand Tests")
    class HelpAwareCommandTests {

        @Test
        @DisplayName("Should provide command examples")
        void shouldProvideCommandExamples() {
            List<CommandExample> examples = helpCommand.getExamples();
            assertFalse(examples.isEmpty());

            // Check that we have the expected examples
            boolean foundHelpExample = false;
            boolean foundGetExample = false;
            boolean foundSpellsExample = false;

            for (CommandExample example : examples) {
                if (example.getCommand().equals("help")
                        && example.getDescription().contains("help overview")) {
                    foundHelpExample = true;
                } else if (example.getCommand().equals("help get")
                        && example.getDescription().contains("get command")) {
                    foundGetExample = true;
                } else if (example.getCommand().equals("help spells")
                        && example.getDescription().contains("spells command")) {
                    foundSpellsExample = true;
                }
            }

            assertTrue(foundHelpExample, "Should have help example");
            assertTrue(foundGetExample, "Should have get example");
            assertTrue(foundSpellsExample, "Should have spells example");
        }
    }
}
