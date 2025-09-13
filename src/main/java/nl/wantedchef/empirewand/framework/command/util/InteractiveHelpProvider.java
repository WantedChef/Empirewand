package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.framework.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Advanced interactive help system with rich formatting, pagination, search functionality,
 * and modern Minecraft chat features including clickable commands and hover tooltips.
 */
public class InteractiveHelpProvider {
    
    private static final int COMMANDS_PER_PAGE = 8;
    private static final String SEPARATOR = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
    
    // Color scheme for modern look
    private static final TextColor HEADER_COLOR = TextColor.fromHexString("#FFD700"); // Gold
    private static final TextColor ACCENT_COLOR = TextColor.fromHexString("#00BFFF"); // Deep sky blue
    private static final TextColor SUCCESS_COLOR = TextColor.fromHexString("#32CD32"); // Lime green
    private static final TextColor WARNING_COLOR = TextColor.fromHexString("#FFA500"); // Orange
    private static final TextColor ERROR_COLOR = TextColor.fromHexString("#FF4500"); // Red orange
    private static final TextColor MUTED_COLOR = TextColor.fromHexString("#A0A0A0"); // Gray
    
    /**
     * Generates a paginated, interactive help overview with search functionality.
     *
     * @param sender The command sender
     * @param commands Map of available commands
     * @param aliases Map of command aliases
     * @param permissionPrefix The permission prefix
     * @param displayName The display name for the command group
     * @param page The page number (1-based)
     * @param searchTerm Optional search term to filter commands
     * @return Formatted help component
     */
    @NotNull
    public static Component generateInteractiveHelp(@NotNull CommandSender sender,
                                                   @NotNull Map<String, SubCommand> commands,
                                                   @NotNull Map<String, SubCommand> aliases,
                                                   @NotNull String permissionPrefix,
                                                   @NotNull String displayName,
                                                   int page,
                                                   @NotNull String searchTerm) {
        
        // Filter commands based on permissions and search term
        List<SubCommand> availableCommands = commands.values().stream()
            .filter(cmd -> hasAccess(sender, cmd))
            .filter(cmd -> matchesSearch(cmd, searchTerm))
            .distinct()
            .sorted(Comparator.comparing(SubCommand::getName))
            .collect(Collectors.toList());
        
        if (availableCommands.isEmpty()) {
            return createNoCommandsMessage(searchTerm);
        }
        
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) availableCommands.size() / COMMANDS_PER_PAGE);
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int startIndex = (currentPage - 1) * COMMANDS_PER_PAGE;
        int endIndex = Math.min(startIndex + COMMANDS_PER_PAGE, availableCommands.size());
        
        List<SubCommand> pageCommands = availableCommands.subList(startIndex, endIndex);
        
        // Build the help message
        var builder = Component.text();
        
        // Header with decorative separator
        builder.append(Component.text(SEPARATOR)
            .color(HEADER_COLOR)
            .decoration(TextDecoration.STRIKETHROUGH, true));
        
        builder.append(Component.newline());
        builder.append(createHeader(displayName, currentPage, totalPages, searchTerm));
        builder.append(Component.newline());
        
        // Commands list
        for (SubCommand command : pageCommands) {
            builder.append(createCommandEntry(command, permissionPrefix));
            builder.append(Component.newline());
        }
        
        // Navigation footer
        if (totalPages > 1) {
            builder.append(Component.newline());
            builder.append(createNavigationFooter(permissionPrefix, currentPage, totalPages, searchTerm));
        }
        
        // Quick help footer
        builder.append(Component.newline());
        builder.append(createQuickHelpFooter(permissionPrefix));
        
        // Bottom separator
        builder.append(Component.newline());
        builder.append(Component.text(SEPARATOR)
            .color(HEADER_COLOR)
            .decoration(TextDecoration.STRIKETHROUGH, true));
        
        return builder.build();
    }
    
    /**
     * Creates a detailed command help with enhanced formatting and examples.
     *
     * @param sender The command sender
     * @param command The command to show help for
     * @param permissionPrefix The permission prefix
     * @return Formatted detailed help component
     */
    @NotNull
    public static Component generateDetailedCommandHelp(@NotNull CommandSender sender,
                                                       @NotNull SubCommand command,
                                                       @NotNull String permissionPrefix) {
        var builder = Component.text();
        
        // Decorative header
        builder.append(Component.text("╭─ Command Details ")
            .color(HEADER_COLOR)
            .append(Component.text("─".repeat(30))
                .color(MUTED_COLOR)));
        builder.append(Component.newline());
        
        // Command name with icon
        builder.append(Component.text("│ ⚡ Command: ")
            .color(MUTED_COLOR)
            .append(Component.text("/" + permissionPrefix + " " + command.getName())
                .color(ACCENT_COLOR)
                .decorate(TextDecoration.BOLD)));
        builder.append(Component.newline());
        
        // Usage with copy-to-clipboard functionality
        String fullUsage = "/" + permissionPrefix + " " + command.getUsage();
        builder.append(Component.text("│ 📝 Usage: ")
            .color(MUTED_COLOR)
            .append(Component.text(fullUsage)
                .color(SUCCESS_COLOR)
                .clickEvent(ClickEvent.suggestCommand(fullUsage))
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy usage")))));
        builder.append(Component.newline());
        
        // Description
        builder.append(Component.text("│ 📖 Description: ")
            .color(MUTED_COLOR)
            .append(Component.text(command.getDescription())
                .color(NamedTextColor.WHITE)));
        builder.append(Component.newline());
        
        // Permission info
        String permission = command.getPermission();
        if (permission != null) {
            boolean hasPermission = sender.hasPermission(permission);
            builder.append(Component.text("│ 🔑 Permission: ")
                .color(MUTED_COLOR)
                .append(Component.text(permission)
                    .color(hasPermission ? SUCCESS_COLOR : ERROR_COLOR))
                .append(Component.text(" (")
                    .color(MUTED_COLOR))
                .append(Component.text(hasPermission ? "✓ GRANTED" : "✗ DENIED")
                    .color(hasPermission ? SUCCESS_COLOR : ERROR_COLOR))
                .append(Component.text(")")
                    .color(MUTED_COLOR)));
        } else {
            builder.append(Component.text("│ 🔑 Permission: ")
                .color(MUTED_COLOR)
                .append(Component.text("None required")
                    .color(SUCCESS_COLOR)));
        }
        builder.append(Component.newline());
        
        // Player requirement
        builder.append(Component.text("│ 👤 Player Required: ")
            .color(MUTED_COLOR)
            .append(Component.text(command.requiresPlayer() ? "Yes" : "No")
                .color(command.requiresPlayer() ? WARNING_COLOR : SUCCESS_COLOR)));
        builder.append(Component.newline());
        
        // Aliases if any
        if (!command.getAliases().isEmpty()) {
            builder.append(Component.text("│ 🔄 Aliases: ")
                .color(MUTED_COLOR)
                .append(Component.text(String.join(", ", command.getAliases()))
                    .color(ACCENT_COLOR)));
            builder.append(Component.newline());
        }
        
        // Examples if available
        if (command instanceof CommandHelpProvider.HelpAwareCommand helpAware) {
            List<CommandHelpProvider.CommandExample> examples = helpAware.getExamples();
            if (!examples.isEmpty()) {
                builder.append(Component.text("│ 💡 Examples:")
                    .color(MUTED_COLOR));
                builder.append(Component.newline());
                
                for (CommandHelpProvider.CommandExample example : examples) {
                    String exampleCommand = "/" + permissionPrefix + " " + example.getCommand();
                    builder.append(Component.text("│   ")
                        .color(MUTED_COLOR)
                        .append(Component.text("▶ ")
                            .color(SUCCESS_COLOR))
                        .append(Component.text(exampleCommand)
                            .color(ACCENT_COLOR)
                            .clickEvent(ClickEvent.suggestCommand(exampleCommand))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to use this command"))))
                        .append(Component.newline())
                        .append(Component.text("│     ")
                            .color(MUTED_COLOR))
                        .append(Component.text(example.getDescription())
                            .color(NamedTextColor.GRAY)));
                    builder.append(Component.newline());
                }
            }
        }
        
        // Footer
        builder.append(Component.text("╰" + "─".repeat(50))
            .color(HEADER_COLOR));
        
        return builder.build();
    }
    
    /**
     * Creates a search results component for command searching.
     *
     * @param sender The command sender
     * @param commands Available commands
     * @param permissionPrefix The permission prefix
     * @param searchTerm The search term used
     * @param maxResults Maximum number of results to show
     * @return Search results component
     */
    @NotNull
    public static Component generateSearchResults(@NotNull CommandSender sender,
                                                 @NotNull Map<String, SubCommand> commands,
                                                 @NotNull String permissionPrefix,
                                                 @NotNull String searchTerm,
                                                 int maxResults) {
        List<SubCommand> matches = commands.values().stream()
            .filter(cmd -> hasAccess(sender, cmd))
            .filter(cmd -> matchesSearch(cmd, searchTerm))
            .distinct()
            .limit(maxResults)
            .sorted(Comparator.comparing(SubCommand::getName))
            .collect(Collectors.toList());
        
        if (matches.isEmpty()) {
            return Component.text("🔍 No commands found matching '")
                .color(WARNING_COLOR)
                .append(Component.text(searchTerm)
                    .color(ACCENT_COLOR))
                .append(Component.text("'")
                    .color(WARNING_COLOR));
        }
        
        var builder = Component.text();
        builder.append(Component.text("🔍 Found ")
            .color(SUCCESS_COLOR)
            .append(Component.text(String.valueOf(matches.size()))
                .color(ACCENT_COLOR))
            .append(Component.text(" command(s) matching '")
                .color(SUCCESS_COLOR))
            .append(Component.text(searchTerm)
                .color(ACCENT_COLOR))
            .append(Component.text("':")
                .color(SUCCESS_COLOR)));
        builder.append(Component.newline());
        
        for (SubCommand command : matches) {
            builder.append(createCommandEntry(command, permissionPrefix));
            builder.append(Component.newline());
        }
        
        return builder.build();
    }
    
    private static Component createHeader(@NotNull String displayName, int currentPage, 
                                        int totalPages, @NotNull String searchTerm) {
        var builder = Component.text();
        
        // Main title with icon
        builder.append(Component.text("⚔ ")
            .color(HEADER_COLOR))
            .append(Component.text(displayName + " Commands")
                .color(HEADER_COLOR)
                .decorate(TextDecoration.BOLD));
        
        // Search indicator
        if (!searchTerm.isEmpty()) {
            builder.append(Component.text(" (searching: '")
                .color(MUTED_COLOR))
                .append(Component.text(searchTerm)
                    .color(ACCENT_COLOR))
                .append(Component.text("')")
                    .color(MUTED_COLOR));
        }
        
        // Page indicator
        if (totalPages > 1) {
            builder.append(Component.text(" - Page ")
                .color(MUTED_COLOR))
                .append(Component.text(String.valueOf(currentPage))
                    .color(ACCENT_COLOR))
                .append(Component.text("/")
                    .color(MUTED_COLOR))
                .append(Component.text(String.valueOf(totalPages))
                    .color(ACCENT_COLOR));
        }
        
        return builder.build();
    }
    
    private static Component createCommandEntry(@NotNull SubCommand command, @NotNull String permissionPrefix) {
        String commandText = "/" + permissionPrefix + " " + command.getName();
        
        return Component.text("  ▶ ")
            .color(SUCCESS_COLOR)
            .append(Component.text(commandText)
                .color(ACCENT_COLOR)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.suggestCommand(commandText))
                .hoverEvent(HoverEvent.showText(
                    Component.text("Click to use • Shift+Click for help")
                        .append(Component.newline())
                        .append(Component.text("Usage: " + command.getUsage())
                            .color(MUTED_COLOR)))))
            .append(Component.text(" - ")
                .color(MUTED_COLOR))
            .append(Component.text(command.getDescription())
                .color(NamedTextColor.WHITE));
    }
    
    private static Component createNavigationFooter(@NotNull String permissionPrefix, 
                                                  int currentPage, int totalPages, 
                                                  @NotNull String searchTerm) {
        var builder = Component.text();
        String searchParam = searchTerm.isEmpty() ? "" : " " + searchTerm;
        
        builder.append(Component.text("📄 Navigation: ")
            .color(MUTED_COLOR));
        
        // Previous page
        if (currentPage > 1) {
            String prevCommand = "/" + permissionPrefix + " help " + (currentPage - 1) + searchParam;
            builder.append(Component.text("◀ Previous")
                .color(ACCENT_COLOR)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand(prevCommand))
                .hoverEvent(HoverEvent.showText(Component.text("Go to previous page"))));
        } else {
            builder.append(Component.text("◀ Previous")
                .color(MUTED_COLOR));
        }
        
        builder.append(Component.text(" | ")
            .color(MUTED_COLOR));
        
        // Next page
        if (currentPage < totalPages) {
            String nextCommand = "/" + permissionPrefix + " help " + (currentPage + 1) + searchParam;
            builder.append(Component.text("Next ▶")
                .color(ACCENT_COLOR)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand(nextCommand))
                .hoverEvent(HoverEvent.showText(Component.text("Go to next page"))));
        } else {
            builder.append(Component.text("Next ▶")
                .color(MUTED_COLOR));
        }
        
        return builder.build();
    }
    
    private static Component createQuickHelpFooter(@NotNull String permissionPrefix) {
        return Component.text("💡 Quick Help: ")
            .color(MUTED_COLOR)
            .append(Component.text("/" + permissionPrefix + " help <command>")
                .color(SUCCESS_COLOR)
                .clickEvent(ClickEvent.suggestCommand("/" + permissionPrefix + " help "))
                .hoverEvent(HoverEvent.showText(Component.text("Get detailed help for a specific command"))))
            .append(Component.text(" • ")
                .color(MUTED_COLOR))
            .append(Component.text("/" + permissionPrefix + " help search <term>")
                .color(SUCCESS_COLOR)
                .clickEvent(ClickEvent.suggestCommand("/" + permissionPrefix + " help search "))
                .hoverEvent(HoverEvent.showText(Component.text("Search commands by name or description"))));
    }
    
    private static Component createNoCommandsMessage(@NotNull String searchTerm) {
        if (searchTerm.isEmpty()) {
            return Component.text("❌ No commands available")
                .color(ERROR_COLOR);
        } else {
            return Component.text("🔍 No commands found matching '")
                .color(WARNING_COLOR)
                .append(Component.text(searchTerm)
                    .color(ACCENT_COLOR))
                .append(Component.text("'. Try a different search term.")
                    .color(WARNING_COLOR));
        }
    }
    
    private static boolean hasAccess(@NotNull CommandSender sender, @NotNull SubCommand command) {
        String permission = command.getPermission();
        return permission == null || sender.hasPermission(permission);
    }
    
    private static boolean matchesSearch(@NotNull SubCommand command, @NotNull String searchTerm) {
        if (searchTerm.isEmpty()) {
            return true;
        }
        
        String term = searchTerm.toLowerCase();
        return command.getName().toLowerCase().contains(term) ||
               command.getDescription().toLowerCase().contains(term) ||
               command.getAliases().stream().anyMatch(alias -> alias.toLowerCase().contains(term));
    }
}