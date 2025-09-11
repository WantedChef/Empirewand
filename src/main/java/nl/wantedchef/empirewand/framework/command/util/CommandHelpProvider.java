package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.framework.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced help system that provides rich, interactive help messages for commands.
 * Supports examples, detailed descriptions, and clickable elements.
 */
public class CommandHelpProvider {
    
    /**
     * Generates a detailed help message for a specific subcommand.
     *
     * @param sender The command sender
     * @param command The subcommand to generate help for
     * @param permissionPrefix The permission prefix for the command
     * @return Component containing the formatted help message
     */
    public static @NotNull Component generateCommandHelp(@NotNull CommandSender sender, 
                                                        @NotNull SubCommand command, 
                                                        @NotNull String permissionPrefix) {
        Component header = Component.text("=== Command Help ===")
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD);
            
        Component name = Component.text("Command: ")
            .color(NamedTextColor.WHITE)
            .append(Component.text("/" + permissionPrefix + " " + command.getName())
                .color(NamedTextColor.YELLOW));
                
        Component usage = Component.text("Usage: ")
            .color(NamedTextColor.WHITE)
            .append(Component.text("/" + permissionPrefix + " " + command.getUsage())
                .color(NamedTextColor.GREEN));
                
        Component description = Component.text("Description: ")
            .color(NamedTextColor.WHITE)
            .append(Component.text(command.getDescription())
                .color(NamedTextColor.GRAY));
                
        Component permission = Component.text("Permission: ")
            .color(NamedTextColor.WHITE)
            .append(Component.text(command.getPermission() != null ? command.getPermission() : "None required")
                .color(NamedTextColor.AQUA));
                
        Component playerRequired = Component.text("Player Required: ")
            .color(NamedTextColor.WHITE)
            .append(Component.text(command.requiresPlayer() ? "Yes" : "No")
                .color(command.requiresPlayer() ? NamedTextColor.RED : NamedTextColor.GREEN));
                
        // Build the complete help message
        Component helpMessage = Component.empty()
            .append(header)
            .append(Component.newline())
            .append(name)
            .append(Component.newline())
            .append(usage)
            .append(Component.newline())
            .append(description)
            .append(Component.newline())
            .append(permission)
            .append(Component.newline())
            .append(playerRequired);
            
        // Add examples if available
        if (command instanceof HelpAwareCommand helpAware) {
            List<CommandExample> examples = helpAware.getExamples();
            if (!examples.isEmpty()) {
                Component examplesHeader = Component.text("\nExamples:")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD);
                helpMessage = helpMessage.append(examplesHeader);
                
                for (CommandExample example : examples) {
                    Component exampleLine = Component.text("\n  ")
                        .append(Component.text(example.getCommand(), NamedTextColor.GREEN))
                        .append(Component.text(" - ", NamedTextColor.WHITE))
                        .append(Component.text(example.getDescription(), NamedTextColor.GRAY));
                    
                    // Make examples clickable
                    exampleLine = exampleLine.clickEvent(ClickEvent.suggestCommand(
                        "/" + permissionPrefix + " " + example.getCommand()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to fill command")));
                        
                    helpMessage = helpMessage.append(exampleLine);
                }
            }
        }
        
        return helpMessage;
    }
    
    /**
     * Generates a comprehensive help overview for all available commands.
     *
     * @param sender The command sender
     * @param commands Map of command names to SubCommand instances
     * @param permissionPrefix The permission prefix for the commands
     * @param displayName The display name for the command group
     * @return Component containing the formatted help overview
     */
    public static @NotNull Component generateHelpOverview(@NotNull CommandSender sender,
                                                         @NotNull Map<String, SubCommand> commands,
                                                         @NotNull String permissionPrefix,
                                                         @NotNull String displayName) {
        Component header = Component.text("=== " + displayName + " Commands ===")
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD);

        Component intro = Component.text("\nUse ")
            .color(NamedTextColor.WHITE)
            .append(Component.text("/" + permissionPrefix + " help <command>", NamedTextColor.YELLOW))
            .append(Component.text(" for detailed information about a specific command.", NamedTextColor.WHITE));

        Component commandList = Component.text("\n\nAvailable Commands:")
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD);
            
        // Filter commands based on permissions
        List<SubCommand> availableCommands = commands.values().stream()
            .filter(cmd -> cmd.getPermission() == null || sender.hasPermission(cmd.getPermission()))
            .collect(Collectors.toList());
            
        for (SubCommand command : availableCommands) {
            Component commandLine = Component.text("\n  ")
                .append(Component.text("/" + permissionPrefix + " " + command.getUsage(), NamedTextColor.YELLOW))
                .append(Component.text(" - ", NamedTextColor.WHITE))
                .append(Component.text(command.getDescription(), NamedTextColor.GRAY));
                
            // Make command usages clickable for help
            commandLine = commandLine.clickEvent(ClickEvent.suggestCommand(
                "/" + permissionPrefix + " " + command.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to use this command")));
                
            commandList = commandList.append(commandLine);
        }
        
        return Component.empty().append(header).append(intro).append(commandList);
    }
    
    /**
     * Interface for commands that provide additional help information.
     */
    public interface HelpAwareCommand {
        /**
         * Gets examples for this command.
         *
         * @return List of command examples
         */
        @NotNull List<CommandExample> getExamples();
    }
    
    /**
     * Represents a command example with description.
     */
    public static class CommandExample {
        private final String command;
        private final String description;
        
        public CommandExample(@NotNull String command, @NotNull String description) {
            this.command = command;
            this.description = description;
        }
        
        /**
         * Gets the example command.
         *
         * @return The example command
         */
        public @NotNull String getCommand() {
            return command;
        }
        
        /**
         * Gets the description of the example.
         *
         * @return The description
         */
        public @NotNull String getDescription() {
            return description;
        }
    }
}