package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.logging.Logger;

public class CommandErrorHandler {

    private final Logger logger;

    public CommandErrorHandler(EmpireWandPlugin plugin) {
        this.logger = plugin.getLogger();
    }

    public void handleCommandException(CommandSender sender, CommandException exception, String commandName) {
        String message = "Error in command '" + commandName + "': " + exception.getMessage();
        if (exception.getErrorCode() != null) {
            message += " (Code: " + exception.getErrorCode() + ")";
        }
        sender.sendMessage(createErrorMessage(message));
        logger.info("Command error: " + message);
    }

    public void handleUnexpectedException(CommandSender sender, Exception exception, String commandName, String[] args) {
        String message = "Unexpected error in command '" + commandName + "' with args: " + Arrays.toString(args) + " - " + exception.getMessage();
        sender.sendMessage(createErrorMessage(message));
        logger.severe("Unexpected command error: " + message);
    }

    public static Component createErrorMessage(String message) {
        return Component.text(message).color(NamedTextColor.RED);
    }

    public static Component createSuccessMessage(String message) {
        return Component.text(message).color(NamedTextColor.GREEN);
    }

    public static Component createInfoMessage(String message) {
        return Component.text(message).color(NamedTextColor.YELLOW);
    }
}
