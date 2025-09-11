package nl.wantedchef.empirewand.framework.command;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.util.CommandErrorHandler;
import nl.wantedchef.empirewand.framework.service.ConfigService;
import nl.wantedchef.empirewand.framework.service.CooldownService;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.api.service.PermissionService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@DisplayName("BaseWandCommand Tests")
class BaseWandCommandTest {

    private TestBaseWandCommand baseCommand;

    @Mock
    private EmpireWandPlugin plugin;

    @Mock
    private ConfigService configService;

    @Mock
    private FxService fxService;

    @Mock
    private SpellRegistry spellRegistry;

    @Mock
    private WandService wandService;

    @Mock
    private CooldownService cooldownService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private CommandSender sender;

    @Mock
    private Player player;

    @Mock
    private Command command;

    // Test implementation of BaseWandCommand
    private static class TestBaseWandCommand extends BaseWandCommand {
        public TestBaseWandCommand(EmpireWandPlugin plugin) {
            super(plugin);
        }

        @Override
        protected void registerSubcommands() {
            // Register a test command
            register(new TestSubCommand());
        }

        @Override
        protected String getPermissionPrefix() {
            return "testwand";
        }

        @Override
        protected String getWandDisplayName() {
            return "Test Wand";
        }
    }

    // Test subcommand implementation
    private static class TestSubCommand implements SubCommand {
        @Override
        public String getName() {
            return "test";
        }

        @Override
        public String getPermission() {
            return "testwand.command.test";
        }

        @Override
        public String getUsage() {
            return "test";
        }

        @Override
        public String getDescription() {
            return "Test command";
        }

        @Override
        public void execute(CommandContext context) throws CommandException {
            context.sendMessage(net.kyori.adventure.text.Component.text("Test command executed"));
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock plugin services
        when(plugin.getConfigService()).thenReturn(configService);
        when(plugin.getFxService()).thenReturn(fxService);
        when(plugin.getSpellRegistry()).thenReturn(spellRegistry);
        when(plugin.getWandService()).thenReturn(wandService);
        when(plugin.getCooldownService()).thenReturn(cooldownService);
        when(plugin.getPermissionService()).thenReturn(permissionService);

        baseCommand = new TestBaseWandCommand(plugin);
        baseCommand.initialize();
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize with plugin")
        void shouldInitializeWithPlugin() {
            assertNotNull(baseCommand);
        }

        @Test
        @DisplayName("Should register subcommands during initialization")
        void shouldRegisterSubcommandsDuringInitialization() {
            Map<String, SubCommand> subcommands = baseCommand.getSubcommands();
            assertTrue(subcommands.containsKey("test"));
            assertTrue(subcommands.containsKey("help")); // Help command should be automatically
                                                         // registered
        }

        @Test
        @DisplayName("Should have error handler")
        void shouldHaveErrorHandler() {
            CommandErrorHandler errorHandler = baseCommand.getErrorHandler();
            assertNotNull(errorHandler);
        }
    }

    @Nested
    @DisplayName("Command Execution Tests")
    class CommandExecutionTests {

        @Test
        @DisplayName("Should show help when no arguments provided")
        void shouldShowHelpWhenNoArgumentsProvided() {
            boolean result = baseCommand.onCommand(sender, command, "test", new String[] {});
            assertTrue(result);
            verify(sender).sendMessage((Component) any());
        }

        @Test
        @DisplayName("Should execute valid subcommand")
        void shouldExecuteValidSubcommand() {
            when(permissionService.has(sender, "testwand.command.test")).thenReturn(true);

            boolean result = baseCommand.onCommand(sender, command, "test", new String[] {"test"});
            assertTrue(result);
        }

        @Test
        @DisplayName("Should show help for unknown command")
        void shouldShowHelpForUnknownCommand() {
            boolean result =
                    baseCommand.onCommand(sender, command, "test", new String[] {"unknown"});
            assertTrue(result);
            verify(sender).sendMessage((Component) any());
        }
    }

    @Nested
    @DisplayName("Tab Completion Tests")
    class TabCompletionTests {

        @Test
        @DisplayName("Should complete subcommand names")
        void shouldCompleteSubcommandNames() {
            when(permissionService.has(sender, "testwand.command.test")).thenReturn(true);

            var completions =
                    baseCommand.onTabComplete(sender, command, "test", new String[] {"t"});
            assertNotNull(completions);
            assertTrue(completions.contains("test"));
        }

        @Test
        @DisplayName("Should filter by permissions")
        void shouldFilterByPermissions() {
            when(permissionService.has(sender, "testwand.command.test")).thenReturn(false);

            var completions =
                    baseCommand.onTabComplete(sender, command, "test", new String[] {"t"});
            assertNotNull(completions);
            assertFalse(completions.contains("test"));
        }
    }
}
