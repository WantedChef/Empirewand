package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Utility class for handling command-related errors and creating message components.
 */
public class CommandErrorHandler {
    private final Logger logger;

    public CommandErrorHandler(EmpireWandPlugin plugin) {
        Logger pluginLogger = plugin != null ? plugin.getLogger() : null;
        this.logger = pluginLogger != null ? pluginLogger : Logger.getLogger("EmpireWand");
    }

    /**
     * Handles a {@link CommandException} by sending a user-facing error message and logging details when available.
     *
     * @param sender      The command sender to notify
     * @param exception   The command exception
     * @param commandName The command name being executed
     */
    public void handleCommandException(CommandSender sender, CommandException exception, String commandName) {
        sender.sendMessage(createErrorMessage(exception.getMessage()));
        if (exception.getErrorCode() != null) {
            logger.info("[" + commandName + "] " + exception.getErrorCode() + " " + Arrays.toString(exception.getContext()));
        }
    }

    /**
     * Handles an unexpected exception by notifying the sender and logging the stack trace.
     *
     * @param sender      The command sender to notify
     * @param exception   The unexpected exception
     * @param commandName The command name being executed
     * @param args        The command arguments
     */
    public void handleUnexpectedException(CommandSender sender, Exception exception, String commandName, String[] args) {
        sender.sendMessage(createErrorMessage("An unexpected error occurred"));
        logger.severe("Unexpected exception in command '" + commandName + "' with args " + Arrays.toString(args) + ": " + exception.getMessage());
    }

    /**
     * Creates an error message component with red color.
     *
     * @param message The message content
     * @return Error component
     */
    public static Component createErrorMessage(String message) {
        return Component.text(message, NamedTextColor.RED);
    }

    /**
     * Creates a success message component with green color.
     *
     * @param message The message content
     * @return Success component
     */
    public static Component createSuccessMessage(String message) {
        return Component.text(message, NamedTextColor.GREEN);
    }

    /**
     * Creates an info message component with yellow color.
     *
     * @param message The message content
     * @return Info component
     */
    public static Component createInfoMessage(String message) {
        return Component.text(message, NamedTextColor.YELLOW);
    }
}

