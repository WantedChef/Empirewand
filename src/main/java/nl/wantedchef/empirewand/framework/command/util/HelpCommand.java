package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider.CommandExample;
import net.kyori.adventure.text.Component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
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
        return List.of("help", "?");
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
        String[] args = context.args();
        
        if (args.length <= 1) {
            // Show interactive paginated help
            Component helpMessage = InteractiveHelpProvider.generateInteractiveHelp(
                context.sender(), 
                availableCommands,
                commandAliases,
                permissionPrefix, 
                wandType.equals("empirewand") ? "Empire Wand" : "Mephidantes Zeist Wand",
                1, // Default to page 1
                "" // No search term
            );
            context.sendMessage(helpMessage);
        } else if (args.length == 2) {
            String secondArg = args[1].toLowerCase();
            
            // Check if it's a page number
            try {
                int page = Integer.parseInt(secondArg);
                Component helpMessage = InteractiveHelpProvider.generateInteractiveHelp(
                    context.sender(), 
                    availableCommands,
                    commandAliases,
                    permissionPrefix, 
                    wandType.equals("empirewand") ? "Empire Wand" : "Mephidantes Zeist Wand",
                    page,
                    ""
                );
                context.sendMessage(helpMessage);
                return;
            } catch (NumberFormatException ignored) {
                // Not a page number, treat as command name
            }
            
            // Check if it's the search subcommand
            if ("search".equals(secondArg)) {
                throw new CommandException("Please specify a search term", "MISSING_SEARCH_TERM");
            }
            
            // Show specific command help
            String commandName = secondArg;
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
            
            Component helpMessage = InteractiveHelpProvider.generateDetailedCommandHelp(
                context.sender(), command, permissionPrefix);
            context.sendMessage(helpMessage);
            
        } else if (args.length >= 3) {
            String secondArg = args[1].toLowerCase();
            
            if ("search".equals(secondArg)) {
                // Search functionality
                String searchTerm = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                Component searchResults = InteractiveHelpProvider.generateSearchResults(
                    context.sender(),
                    availableCommands,
                    permissionPrefix,
                    searchTerm,
                    10 // Max 10 results
                );
                context.sendMessage(searchResults);
            } else {
                // Try parsing as page number with search
                try {
                    int page = Integer.parseInt(secondArg);
                    String searchTerm = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    Component helpMessage = InteractiveHelpProvider.generateInteractiveHelp(
                        context.sender(), 
                        availableCommands,
                        commandAliases,
                        permissionPrefix, 
                        wandType.equals("empirewand") ? "Empire Wand" : "Mephidantes Zeist Wand",
                        page,
                        searchTerm
                    );
                    context.sendMessage(helpMessage);
                } catch (NumberFormatException e) {
                    throw new CommandException("Invalid page number or unknown command format", "INVALID_HELP_FORMAT");
                }
            }
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