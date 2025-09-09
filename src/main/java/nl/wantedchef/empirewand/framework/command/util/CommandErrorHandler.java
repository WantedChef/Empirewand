package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for structured error handling and logging in commands.
 * Provides consistent error messages and detailed logging for debugging.
 */
public class CommandErrorHandler {
    private final EmpireWandPlugin plugin;
    private final Logger logger;

    public CommandErrorHandler(@NotNull EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Handles a command exception and sends an appropriate message to the sender.
     *
     * @param sender The command sender
     * @param exception The command exception
     * @param commandName The name of the command that failed
     */
    public void handleCommandException(@NotNull CommandSender sender, 
                                     @NotNull CommandException exception, 
                                     @NotNull String commandName) {
        // Send user-friendly error message
        sender.sendMessage(Component.text(exception.getMessage()).color(NamedTextColor.RED));
        
        // Log detailed error information for debugging
        String errorCode = exception.getErrorCode();
        Object[] context = exception.getContext();
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("Command '%s' failed for %s: %s", 
            commandName, sender.getName(), exception.getMessage()));
            
        if (errorCode != null) {
            logMessage.append(String.format(" (error code: %s)", errorCode));
        }
        
        if (context.length > 0) {
            logMessage.append(" (context: ");
            for (int i = 0; i < context.length; i++) {
                if (i > 0) logMessage.append(", ");
                logMessage.append(context[i]);
            }
            logMessage.append(")");
        }
        
        // Log at appropriate level based on error code
        if (errorCode != null && (errorCode.startsWith("ARG_") || errorCode.equals("NO_PERMISSION"))) {
            logger.info(logMessage.toString());
        } else {
            logger.log(Level.WARNING, logMessage.toString(), exception);
        }
    }

    /**
     * Handles an unexpected exception and sends a generic error message to the sender.
     *
     * @param sender The command sender
     * @param exception The unexpected exception
     * @param commandName The name of the command that failed
     * @param commandArgs The command arguments
     */
    public void handleUnexpectedException(@NotNull CommandSender sender, 
                                        @NotNull Exception exception, 
                                        @NotNull String commandName,
                                        @NotNull String[] commandArgs) {
        // Send generic error message to user
        sender.sendMessage(Component.text("An internal error occurred").color(NamedTextColor.RED));
        
        // Log detailed error information
        String argsString = String.join(" ", commandArgs);
        logger.log(Level.SEVERE, 
            String.format("Unexpected error in command '%s' with args '%s' for sender %s", 
                commandName, argsString, sender.getName()), 
            exception);
    }

    /**
     * Creates a standardized error message component.
     *
     * @param message The error message
     * @return Component with standardized error formatting
     */
    public static @NotNull Component createErrorMessage(@NotNull String message) {
        return Component.text(message).color(NamedTextColor.RED);
    }

    /**
     * Creates a standardized success message component.
     *
     * @param message The success message
     * @return Component with standardized success formatting
     */
    public static @NotNull Component createSuccessMessage(@NotNull String message) {
        return Component.text(message).color(NamedTextColor.GREEN);
    }

    /**
     * Creates a standardized informational message component.
     *
     * @param message The informational message
     * @return Component with standardized informational formatting
     */
    public static @NotNull Component createInfoMessage(@NotNull String message) {
        return Component.text(message).color(NamedTextColor.YELLOW);
    }
}