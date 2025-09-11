package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Utility class for executing commands asynchronously with proper error handling.
 * Provides a clean API for running long-running operations without blocking the main thread.
 */
public class AsyncCommandExecutor {
    private final EmpireWandPlugin plugin;
    private final BukkitScheduler scheduler;

    public AsyncCommandExecutor(@NotNull EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
    }

    /**
     * Package-private constructor primarily for testing to inject a scheduler.
     */
    AsyncCommandExecutor(@NotNull EmpireWandPlugin plugin, @NotNull BukkitScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    /**
     * Executes a command asynchronously with proper error handling.
     *
     * @param context The command context
     * @param command The command being executed
     * @param asyncTask The async task to execute
     * @param successMessage The message to send on success (null for no message)
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public void executeAsync(@NotNull CommandContext context, @NotNull SubCommand command,
                            @NotNull AsyncCommandTask asyncTask, @NotNull String successMessage) {
        executeAsync(context, command, asyncTask, 
            result -> context.sendMessage(Component.text(successMessage).color(NamedTextColor.GREEN)),
            error -> context.sendMessage(Component.text("An error occurred while processing your request.").color(NamedTextColor.RED)));
    }

    /**
     * Executes a command asynchronously with custom success and error handlers.
     *
     * @param context The command context
     * @param command The command being executed
     * @param asyncTask The async task to execute
     * @param onSuccess The success handler (optional)
     * @param onError The error handler (optional)
     */
    public void executeAsync(@NotNull CommandContext context, @NotNull SubCommand command,
                            @NotNull AsyncCommandTask asyncTask,
                            @NotNull Consumer<Object> onSuccess,
                            @NotNull Consumer<Exception> onError) {
        
        CommandSender sender = context.sender();
        String commandName = command.getName();
        
        // Send initial feedback
        context.sendMessage(Component.text("Processing your request...").color(NamedTextColor.GOLD));
        
        // Track start time for performance monitoring
        long startTime = System.nanoTime();
        
        scheduler.runTaskAsynchronously(plugin, () -> {
            try {
                Object result = asyncTask.execute();
                long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
                
                // Log execution metrics
                context.logCommandExecution(commandName, executionTimeMs, true);
                
                // Handle success on main thread
                scheduler.runTask(plugin, () -> {
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                });
            } catch (Exception e) {
                long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
                
                // Log execution metrics
                context.logCommandExecution(commandName, executionTimeMs, false);
                
                plugin.getLogger().log(Level.WARNING, 
                    String.format("Async command '%s' failed for sender %s", commandName, sender.getName()), e);
                
                // Handle error on main thread
                scheduler.runTask(plugin, () -> {
                    if (onError != null) {
                        onError.accept(e);
                    } else {
                        sender.sendMessage(Component.text("An error occurred while processing your request.")
                            .color(NamedTextColor.RED));
                    }
                });
            }
        });
    }

    /**
     * Executes a command asynchronously with a simple success message.
     *
     * @param context The command context
     * @param command The command being executed
     * @param asyncTask The async task to execute
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public void executeAsync(@NotNull CommandContext context, @NotNull SubCommand command,
                            @NotNull AsyncCommandTask asyncTask) {
        executeAsync(context, command, asyncTask, 
            result -> context.sendMessage(Component.text("Command executed successfully!").color(NamedTextColor.GREEN)),
            error -> context.sendMessage(Component.text("An error occurred while processing your request.").color(NamedTextColor.RED)));
    }

    /**
     * Functional interface for async command tasks.
     */
    @FunctionalInterface
    public interface AsyncCommandTask {
        /**
         * Executes the async task.
         *
         * @return The result of the task
         * @throws Exception if an error occurs
         */
        Object execute() throws Exception;
    }
}