package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandContext;
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

    // Dependencies for CommandContext
    @Mock private nl.wantedchef.empirewand.framework.service.ConfigService configService;
    @Mock private nl.wantedchef.empirewand.framework.service.FxService fxService;
    @Mock private nl.wantedchef.empirewand.api.spell.SpellRegistry spellRegistry;
    @Mock private nl.wantedchef.empirewand.api.service.WandService wandService;
    @Mock private nl.wantedchef.empirewand.framework.service.CooldownService cooldownService;
    @Mock private nl.wantedchef.empirewand.api.service.PermissionService permissionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(command.getName()).thenReturn("testcommand");
        when(sender.getName()).thenReturn("tester");
        context = new CommandContext(
            plugin,
            sender,
            new String[] {"help", "arg"},
            configService,
            fxService,
            spellRegistry,
            wandService,
            cooldownService,
            permissionService
        );
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
            verify(scheduler).runTaskAsynchronously(org.mockito.Mockito.eq(plugin), org.mockito.Mockito.any(Runnable.class));
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
            verify(scheduler).runTaskAsynchronously(org.mockito.Mockito.eq(plugin), org.mockito.Mockito.any(Runnable.class));
        }

        @Test
        @DisplayName("Should execute with simple success message")
        void shouldExecuteWithSimpleSuccessMessage() {
            AsyncCommandExecutor.AsyncCommandTask task = () -> "result";
            
            // Execute the async task
            asyncExecutor.executeAsync(context, command, task, "Success!");
            
            // Verify scheduler interaction
            verify(scheduler).runTaskAsynchronously(org.mockito.Mockito.eq(plugin), org.mockito.Mockito.any(Runnable.class));
        }

        @Test
        @DisplayName("Should execute with default success handler")
        void shouldExecuteWithDefaultSuccessHandler() {
            AsyncCommandExecutor.AsyncCommandTask task = () -> "result";
            
            // Execute the async task
            asyncExecutor.executeAsync(context, command, task);
            
            // Verify scheduler interaction
            verify(scheduler).runTaskAsynchronously(org.mockito.Mockito.eq(plugin), org.mockito.Mockito.any(Runnable.class));
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