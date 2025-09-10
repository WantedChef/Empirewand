package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider.CommandExample;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CommandHelpProvider Tests")
class CommandHelpProviderTest {

    @Mock
    private CommandSender sender;
    
    @Mock
    private SubCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("Command Help Generation Tests")
    class CommandHelpGenerationTests {
        
        @Test
        @DisplayName("Should generate command help with basic information")
        void shouldGenerateCommandHelpWithBasicInformation() {
            when(command.getName()).thenReturn("test");
            when(command.getUsage()).thenReturn("test <arg>");
            when(command.getDescription()).thenReturn("Test command description");
            when(command.getPermission()).thenReturn("test.permission");
            when(command.requiresPlayer()).thenReturn(true);
            
            Component help = CommandHelpProvider.generateCommandHelp(sender, command, "testprefix");
            
            assertNotNull(help);
            String helpText = Component.textOfChildren(help).content();
            assertTrue(helpText.contains("Command: /testprefix test"));
            assertTrue(helpText.contains("Usage: /testprefix test <arg>"));
            assertTrue(helpText.contains("Test command description"));
            assertTrue(helpText.contains("test.permission"));
            assertTrue(helpText.contains("Player Required: Yes"));
        }

        @Test
        @DisplayName("Should generate command help for commands without permission")
        void shouldGenerateCommandHelpForCommandsWithoutPermission() {
            when(command.getName()).thenReturn("test");
            when(command.getUsage()).thenReturn("test");
            when(command.getDescription()).thenReturn("Test command");
            when(command.getPermission()).thenReturn(null);
            when(command.requiresPlayer()).thenReturn(false);
            
            Component help = CommandHelpProvider.generateCommandHelp(sender, command, "testprefix");
            
            assertNotNull(help);
            String helpText = Component.textOfChildren(help).content();
            assertTrue(helpText.contains("Permission: None required"));
            assertTrue(helpText.contains("Player Required: No"));
        }
    }

    @Nested
    @DisplayName("Help Overview Generation Tests")
    class HelpOverviewGenerationTests {
        
        @Test
        @DisplayName("Should generate help overview with all commands")
        void shouldGenerateHelpOverviewWithAllCommands() {
            Map<String, SubCommand> commands = new HashMap<>();
            
            // Create test commands
            SubCommand cmd1 = mock(SubCommand.class);
            when(cmd1.getName()).thenReturn("cmd1");
            when(cmd1.getUsage()).thenReturn("cmd1");
            when(cmd1.getDescription()).thenReturn("First test command");
            when(cmd1.getPermission()).thenReturn(null);
            
            SubCommand cmd2 = mock(SubCommand.class);
            when(cmd2.getName()).thenReturn("cmd2");
            when(cmd2.getUsage()).thenReturn("cmd2 <arg>");
            when(cmd2.getDescription()).thenReturn("Second test command");
            when(cmd2.getPermission()).thenReturn(null);
            
            commands.put("cmd1", cmd1);
            commands.put("cmd2", cmd2);
            
            Component help = CommandHelpProvider.generateHelpOverview(sender, commands, "testprefix", "Test Commands");
            
            assertNotNull(help);
            String helpText = Component.textOfChildren(help).content();
            assertTrue(helpText.contains("Test Commands Commands"));
            assertTrue(helpText.contains("/testprefix cmd1"));
            assertTrue(helpText.contains("First test command"));
            assertTrue(helpText.contains("/testprefix cmd2 <arg>"));
            assertTrue(helpText.contains("Second test command"));
        }

        @Test
        @DisplayName("Should filter commands based on permissions")
        void shouldFilterCommandsBasedOnPermissions() {
            Map<String, SubCommand> commands = new HashMap<>();
            
            // Create test commands
            SubCommand publicCmd = mock(SubCommand.class);
            when(publicCmd.getName()).thenReturn("public");
            when(publicCmd.getUsage()).thenReturn("public");
            when(publicCmd.getDescription()).thenReturn("Public command");
            when(publicCmd.getPermission()).thenReturn(null);
            
            SubCommand restrictedCmd = mock(SubCommand.class);
            when(restrictedCmd.getName()).thenReturn("restricted");
            when(restrictedCmd.getUsage()).thenReturn("restricted");
            when(restrictedCmd.getDescription()).thenReturn("Restricted command");
            when(restrictedCmd.getPermission()).thenReturn("test.restricted");
            
            commands.put("public", publicCmd);
            commands.put("restricted", restrictedCmd);
            
            // Sender has permission for restricted command
            when(sender.hasPermission("test.restricted")).thenReturn(true);
            
            Component help = CommandHelpProvider.generateHelpOverview(sender, commands, "testprefix", "Test Commands");
            
            assertNotNull(help);
            String helpText = Component.textOfChildren(help).content();
            assertTrue(helpText.contains("/testprefix public"));
            assertTrue(helpText.contains("/testprefix restricted"));
        }

        @Test
        @DisplayName("Should exclude commands without permission")
        void shouldExcludeCommandsWithoutPermission() {
            Map<String, SubCommand> commands = new HashMap<>();
            
            // Create test commands
            SubCommand publicCmd = mock(SubCommand.class);
            when(publicCmd.getName()).thenReturn("public");
            when(publicCmd.getUsage()).thenReturn("public");
            when(publicCmd.getDescription()).thenReturn("Public command");
            when(publicCmd.getPermission()).thenReturn(null);
            
            SubCommand restrictedCmd = mock(SubCommand.class);
            when(restrictedCmd.getName()).thenReturn("restricted");
            when(restrictedCmd.getUsage()).thenReturn("restricted");
            when(restrictedCmd.getDescription()).thenReturn("Restricted command");
            when(restrictedCmd.getPermission()).thenReturn("test.restricted");
            
            commands.put("public", publicCmd);
            commands.put("restricted", restrictedCmd);
            
            // Sender does not have permission for restricted command
            when(sender.hasPermission("test.restricted")).thenReturn(false);
            
            Component help = CommandHelpProvider.generateHelpOverview(sender, commands, "testprefix", "Test Commands");
            
            assertNotNull(help);
            String helpText = Component.textOfChildren(help).content();
            assertTrue(helpText.contains("/testprefix public"));
            assertFalse(helpText.contains("/testprefix restricted"));
        }
    }

    @Nested
    @DisplayName("Command Example Tests")
    class CommandExampleTests {
        
        @Test
        @DisplayName("Should create command example with command and description")
        void shouldCreateCommandExampleWithCommandAndDescription() {
            CommandExample example = new CommandExample("test command", "Test description");
            
            assertEquals("test command", example.getCommand());
            assertEquals("Test description", example.getDescription());
        }

        @Test
        @DisplayName("Should implement HelpAwareCommand interface correctly")
        void shouldImplementHelpAwareCommandInterfaceCorrectly() {
            class TestHelpAwareCommand implements CommandHelpProvider.HelpAwareCommand {
                @Override
                public List<CommandExample> getExamples() {
                    return List.of(new CommandExample("test", "Test example"));
                }
            }
            
            TestHelpAwareCommand helpAware = new TestHelpAwareCommand();
            List<CommandExample> examples = helpAware.getExamples();
            
            assertEquals(1, examples.size());
            assertEquals("test", examples.get(0).getCommand());
            assertEquals("Test example", examples.get(0).getDescription());
        }
    }
}