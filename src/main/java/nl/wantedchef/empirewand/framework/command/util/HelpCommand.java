package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider.CommandExample;
import net.kyori.adventure.text.Component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Standard help command that provides detailed information about available commands.
 * Integrates with the enhanced help system for rich, interactive help messages.
 */
public class HelpCommand implements SubCommand, CommandHelpProvider.HelpAwareCommand {
    private final String wandType;
    private final String permissionPrefix;
    private final Map<String, SubCommand> availableCommands;
    private final Map<String, SubCommand> commandAliases;

    public HelpCommand(String wandType, String permissionPrefix, 
                      Map<String, SubCommand> availableCommands, 
                      Map<String, SubCommand> commandAliases) {
        this.wandType = wandType;
        this.permissionPrefix = permissionPrefix;
        this.availableCommands = availableCommands;
        this.commandAliases = commandAliases;
    }

    @Override
    public @NotNull String getName() {
        return "help";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("?");
    }

        @Override
    public @Nullable String getPermission() {
        return null; // Help is available to everyone
    }

    @Override
    public @NotNull String getUsage() {
        return "help [command]";
    }

    @Override
    public @NotNull String getDescription() {
        return "Show help for commands";
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        if (context.args().length <= 1) {
            // Show general help
            Component helpMessage = CommandHelpProvider.generateHelpOverview(
                context.sender(), 
                availableCommands, 
                permissionPrefix, 
                wandType.equals("empirewand") ? "Empire Wand" : "Mephidantes Zeist Wand"
            );
            context.sendMessage(helpMessage);
        } else {
            // Show specific command help
            String commandName = context.args()[1].toLowerCase();
            SubCommand command = availableCommands.get(commandName);
            
            if (command == null) {
                command = commandAliases.get(commandName);
            }
            
            if (command == null) {
                throw new CommandException("Unknown command: " + commandName, "UNKNOWN_COMMAND", commandName);
            }
            
            // Check permissions
            String permission = command.getPermission();
            if (permission != null && !context.hasPermission(permission)) {
                throw new CommandException("No permission", "NO_PERMISSION");
            }
            
            Component helpMessage = CommandHelpProvider.generateCommandHelp(context.sender(), command, permissionPrefix);
            context.sendMessage(helpMessage);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            String partial = context.args()[1].toLowerCase();
            return availableCommands.keySet().stream()
                .filter(name -> name.startsWith(partial))
                .filter(name -> {
                    SubCommand sub = availableCommands.get(name);
                    String perm = sub.getPermission();
                    return perm == null || context.hasPermission(perm);
                })
                .collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }

    @Override
    public @NotNull List<CommandExample> getExamples() {
        return List.of(
            new CommandExample("help", "Show this help overview"),
            new CommandExample("help get", "Show detailed help for the get command"),
            new CommandExample("help spells", "Show detailed help for the spells command")
        );
    }
}