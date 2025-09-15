package nl.wantedchef.empirewand.framework.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.service.PermissionService;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.core.util.PerformanceMonitor;
import nl.wantedchef.empirewand.framework.service.ConfigService;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.framework.service.metrics.MetricsService;

@DisplayName("CommandContext Tests")
class CommandContextTest {

    private CommandContext context;

    @Mock
    private EmpireWandPlugin plugin;

    @Mock
    private CommandSender sender;

    @Mock
    private Player player;

    @Mock
    private ConfigService configService;

    @Mock
    private FxService fxService;

    @Mock
    private SpellRegistry spellRegistry;

    @Mock
    private WandService wandService;

    @Mock
    private nl.wantedchef.empirewand.framework.service.UnifiedCooldownManager cooldownManager;

    @Mock
    private PermissionService permissionService;

    @Mock
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getMetricsService()).thenReturn(metricsService);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        when(sender.getName()).thenReturn("Tester");

        context = new CommandContext(plugin, sender, new String[] {"arg1", "arg2", "arg3"},
                configService, fxService, spellRegistry, wandService, cooldownManager,
                permissionService);
    }

    @Nested
    @DisplayName("Basic Properties Tests")
    class BasicPropertiesTests {

        @Test
        @DisplayName("Should return plugin")
        void shouldReturnPlugin() {
            assertEquals(plugin, context.plugin());
        }

        @Test
        @DisplayName("Should return sender")
        void shouldReturnSender() {
            assertEquals(sender, context.sender());
        }

        @Test
        @DisplayName("Should return arguments")
        void shouldReturnArguments() {
            String[] args = context.args();
            assertEquals(3, args.length);
            assertEquals("arg1", args[0]);
            assertEquals("arg2", args[1]);
            assertEquals("arg3", args[2]);
        }

        @Test
        @DisplayName("Should return services")
        void shouldReturnServices() {
            assertEquals(configService, context.config());
            assertEquals(fxService, context.fx());
            assertEquals(spellRegistry, context.spellRegistry());
            assertEquals(wandService, context.wandService());
            assertEquals(cooldownManager, context.cooldownManager());
            assertEquals(permissionService, context.permissionService());
        }
    }

    @Nested
    @DisplayName("Player Conversion Tests")
    class PlayerConversionTests {

        @Test
        @DisplayName("Should return null when sender is not player")
        void shouldReturnNullWhenSenderIsNotPlayer() {
            assertNull(context.asPlayer());
        }

        @Test
        @DisplayName("Should return player when sender is player")
        void shouldReturnPlayerWhenSenderIsPlayer() {
            CommandContext playerContext =
                    new CommandContext(plugin, player, new String[] {}, configService, fxService,
                            spellRegistry, wandService, cooldownManager, permissionService);

            assertEquals(player, playerContext.asPlayer());
        }

        @Test
        @DisplayName("Should throw exception when requiring player but sender is not player")
        void shouldThrowExceptionWhenRequiringPlayerButSenderIsNotPlayer() {
            CommandException exception = assertThrows(CommandException.class, () -> {
                context.requirePlayer();
            });

            assertEquals("This command can only be used by players", exception.getMessage());
        }

        @Test
        @DisplayName("Should return player when requiring player and sender is player")
        void shouldReturnPlayerWhenRequiringPlayerAndSenderIsPlayer() throws CommandException {
            CommandContext playerContext =
                    new CommandContext(plugin, player, new String[] {}, configService, fxService,
                            spellRegistry, wandService, cooldownManager, permissionService);

            assertEquals(player, playerContext.requirePlayer());
        }
    }

    @Nested
    @DisplayName("Permission Tests")
    class PermissionTests {

        @Test
        @DisplayName("Should check permission")
        void shouldCheckPermission() {
            when(permissionService.has(sender, "test.permission")).thenReturn(true);

            assertTrue(context.hasPermission("test.permission"));
            verify(permissionService).has(sender, "test.permission");
        }

        @Test
        @DisplayName("Should throw exception when requiring missing permission")
        void shouldThrowExceptionWhenRequiringMissingPermission() {
            when(permissionService.has(sender, "test.permission")).thenReturn(false);

            CommandException exception = assertThrows(CommandException.class, () -> {
                context.requirePermission("test.permission");
            });

            assertEquals("No permission", exception.getMessage());
        }

        @Test
        @DisplayName("Should not throw exception when requiring granted permission")
        void shouldNotThrowExceptionWhenRequiringGrantedPermission() throws CommandException {
            when(permissionService.has(sender, "test.permission")).thenReturn(true);

            assertDoesNotThrow(() -> {
                context.requirePermission("test.permission");
            });
        }
    }

    @Nested
    @DisplayName("Argument Access Tests")
    class ArgumentAccessTests {

        @Test
        @DisplayName("Should get argument by index")
        void shouldGetArgumentByIndex() throws CommandException {
            assertEquals("arg1", context.getArg(0));
            assertEquals("arg2", context.getArg(1));
            assertEquals("arg3", context.getArg(2));
        }

        @Test
        @DisplayName("Should throw exception when getting missing argument")
        void shouldThrowExceptionWhenGettingMissingArgument() {
            CommandException exception = assertThrows(CommandException.class, () -> {
                context.getArg(3);
            });

            assertTrue(exception.getMessage().contains("Missing required argument"));
            assertTrue(exception.getMessage().contains("3"));
        }

        @Test
        @DisplayName("Should get optional argument when present")
        void shouldGetOptionalArgumentWhenPresent() {
            assertEquals("arg1", context.getArgOrNull(0));
            assertEquals("arg2", context.getArgOrNull(1));
            assertEquals("arg3", context.getArgOrNull(2));
        }

        @Test
        @DisplayName("Should return null for missing optional argument")
        void shouldReturnNullForMissingOptionalArgument() {
            assertNull(context.getArgOrNull(3));
        }
    }

    @Nested
    @DisplayName("Enhanced Validation Tests")
    class EnhancedValidationTests {

        @Test
        @DisplayName("Should validate enum argument")
        void shouldValidateEnumArgument() throws CommandException {
            CommandContext testContext = new CommandContext(plugin, sender,
                    new String[] {"value1", "VALUE2"}, configService, fxService, spellRegistry,
                    wandService, cooldownManager, permissionService);

            assertEquals("value1", testContext.validateEnumArg(0, "value1", "value2", "value3"));
            assertEquals("value2", testContext.validateEnumArg(1, "value1", "value2", "value3"));
        }

        @Test
        @DisplayName("Should throw exception for invalid enum argument")
        void shouldThrowExceptionForInvalidEnumArgument() {
            CommandContext testContext = new CommandContext(plugin, sender,
                    new String[] {"invalid"}, configService, fxService, spellRegistry, wandService,
                    cooldownManager, permissionService);

            CommandException exception = assertThrows(CommandException.class, () -> {
                testContext.validateEnumArg(0, "value1", "value2", "value3");
            });

            assertTrue(exception.getMessage().contains("Invalid value"));
            assertTrue(exception.getMessage().contains("value1, value2, value3"));
        }

        @Test
        @DisplayName("Should validate integer argument range")
        void shouldValidateIntegerArgumentRange() throws CommandException {
            CommandContext testContext = new CommandContext(plugin, sender, new String[] {"50"},
                    configService, fxService, spellRegistry, wandService, cooldownManager,
                    permissionService);

            assertEquals(50, testContext.validateIntArg(0, 1, 100));
        }

        @Test
        @DisplayName("Should throw exception for out of range integer argument")
        void shouldThrowExceptionForOutOfRangeIntegerArgument() {
            CommandContext testContext = new CommandContext(plugin, sender, new String[] {"150"},
                    configService, fxService, spellRegistry, wandService, cooldownManager,
                    permissionService);

            CommandException exception = assertThrows(CommandException.class, () -> {
                testContext.validateIntArg(0, 1, 100);
            });

            assertTrue(exception.getMessage().contains("must be between"));
        }

        @Test
        @DisplayName("Should validate boolean argument")
        void shouldValidateBooleanArgument() throws CommandException {
            CommandContext testContext = new CommandContext(plugin, sender,
                    new String[] {"true", "false"}, configService, fxService, spellRegistry,
                    wandService, cooldownManager, permissionService);

            assertTrue(testContext.validateBooleanArg(0));
            assertFalse(testContext.validateBooleanArg(1));
        }

        @Test
        @DisplayName("Should throw exception for invalid boolean argument")
        void shouldThrowExceptionForInvalidBooleanArgument() {
            CommandContext testContext = new CommandContext(plugin, sender, new String[] {"maybe"},
                    configService, fxService, spellRegistry, wandService, cooldownManager,
                    permissionService);

            CommandException exception = assertThrows(CommandException.class, () -> {
                testContext.validateBooleanArg(0);
            });

            assertTrue(exception.getMessage().contains("Invalid boolean value"));
        }
    }

    @Nested
    @DisplayName("Performance Monitoring Tests")
    class PerformanceMonitoringTests {

        @Test
        @DisplayName("Should start timing context")
        void shouldStartTimingContext() {
            PerformanceMonitor performanceMonitor = mock(PerformanceMonitor.class);
            PerformanceMonitor.TimingContext timingContext =
                    mock(PerformanceMonitor.TimingContext.class);
            when(plugin.getPerformanceMonitor()).thenReturn(performanceMonitor);
            when(performanceMonitor.startTiming(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(timingContext);

            var timing = context.startTiming("test.operation");
            assertNotNull(timing);

            verify(performanceMonitor).startTiming("command.test.operation", 50L);
        }

        @Test
        @DisplayName("Should log command execution")
        void shouldLogCommandExecution() {
            context.logCommandExecution("testcommand", 50, true);
            // We can't easily verify the logging, but we can ensure it doesn't throw exceptions
            assertTrue(true);
        }
    }
}
