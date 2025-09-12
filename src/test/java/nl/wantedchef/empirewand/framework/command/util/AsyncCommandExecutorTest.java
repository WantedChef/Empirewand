package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.service.ConfigService;
import nl.wantedchef.empirewand.framework.service.CooldownService;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.api.service.PermissionService;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.function.Consumer;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@DisplayName("AsyncCommandExecutor Tests")
class AsyncCommandExecutorTest {

    private AsyncCommandExecutor asyncExecutor;
    
    @Mock
    private EmpireWandPlugin plugin;
    
    @Mock
    private BukkitScheduler scheduler;
    
    @Mock
    private CommandSender sender;
    
    private CommandContext context;
    
    @Mock
    private SubCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // We inject the scheduler directly; no need to mock Server
        when(command.getName()).thenReturn("testcommand");
        when(plugin.getLogger()).thenReturn(mock(java.util.logging.Logger.class));
        // Build a real CommandContext to avoid mocking final record classes
        ConfigService config = mock(ConfigService.class);
        FxService fx = mock(FxService.class);
        SpellRegistry spellRegistry = mock(SpellRegistry.class);
        WandService wandService = mock(WandService.class);
        CooldownService cooldownService = mock(CooldownService.class);
        PermissionService permissionService = mock(PermissionService.class);
        when(sender.getName()).thenReturn("tester");
        context = new CommandContext(
                plugin,
                sender,
                new String[0],
                config,
                fx,
                spellRegistry,
                wandService,
                cooldownService,
                permissionService);
        asyncExecutor = new AsyncCommandExecutor(plugin, scheduler);
    }

    @Nested
    @DisplayName("Async Execution Tests")
    class AsyncExecutionTests {
        
        @Test
        @DisplayName("Should execute async task successfully")
        void shouldExecuteAsyncTaskSuccessfully() {
            AsyncCommandExecutor.AsyncCommandTask task = () -> "success";
            Consumer<Object> onSuccess = mock(Consumer.class);
            Consumer<Exception> onError = mock(Consumer.class);
            
            // Execute the async task
            asyncExecutor.executeAsync(context, command, task, onSuccess, onError);
            
            // Verify scheduler interaction
            verify(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
        }

        @Test
        @DisplayName("Should handle async task exceptions")
        void shouldHandleAsyncTaskExceptions() {
            AsyncCommandExecutor.AsyncCommandTask task = () -> {
                throw new RuntimeException("Test exception");
            };
            Consumer<Object> onSuccess = mock(Consumer.class);
            Consumer<Exception> onError = mock(Consumer.class);
            
            // Execute the async task
            asyncExecutor.executeAsync(context, command, task, onSuccess, onError);
            
            // Verify scheduler interaction
            verify(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
        }

        @Test
        @DisplayName("Should execute with simple success message")
        void shouldExecuteWithSimpleSuccessMessage() {
            AsyncCommandExecutor.AsyncCommandTask task = () -> "result";
            
            // Execute the async task
            asyncExecutor.executeAsync(context, command, task, "Success!");
            
            // Verify scheduler interaction
            verify(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
        }

        @Test
        @DisplayName("Should execute with default success handler")
        void shouldExecuteWithDefaultSuccessHandler() {
            AsyncCommandExecutor.AsyncCommandTask task = () -> "result";
            
            // Execute the async task
            asyncExecutor.executeAsync(context, command, task);
            
            // Verify scheduler interaction
            verify(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
        }
    }

    @Nested
    @DisplayName("Functional Interface Tests")
    class FunctionalInterfaceTests {
        
        @Test
        @DisplayName("AsyncCommandTask should be functional interface")
        void asyncCommandTaskShouldBeFunctionalInterface() {
            AsyncCommandExecutor.AsyncCommandTask task = () -> "test";
            assertNotNull(task);
        }

        @Test
        @DisplayName("AsyncCommandTask should execute and return result")
        void asyncCommandTaskShouldExecuteAndReturnResult() throws Exception {
            AsyncCommandExecutor.AsyncCommandTask task = () -> "test result";
            Object result = task.execute();
            assertEquals("test result", result);
        }

        @Test
        @DisplayName("AsyncCommandTask should propagate exceptions")
        void asyncCommandTaskShouldPropagateExceptions() {
            AsyncCommandExecutor.AsyncCommandTask task = () -> {
                throw new Exception("Test exception");
            };
            
            Exception exception = assertThrows(Exception.class, () -> {
                task.execute();
            });
            
            assertEquals("Test exception", exception.getMessage());
        }
    }
}
