package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Enhanced error handler with smart suggestions, better formatting, and interactive help.
 * Provides context-aware error messages with actionable suggestions and clickable elements.
 */
public class CommandErrorHandler {
    private final Logger logger;
    private final Map<String, SubCommand> availableCommands = new HashMap<>();
    private final String permissionPrefix;
    
    // Enhanced color scheme
    private static final TextColor ERROR_COLOR = TextColor.fromHexString("#FF4444");
    private static final TextColor WARNING_COLOR = TextColor.fromHexString("#FFAA00");
    private static final TextColor SUGGESTION_COLOR = TextColor.fromHexString("#55AAFF");
    private static final TextColor HELP_COLOR = TextColor.fromHexString("#AAAAAA");

    public CommandErrorHandler(EmpireWandPlugin plugin) {
        Logger pluginLogger = plugin != null ? plugin.getLogger() : null;
        this.logger = pluginLogger != null ? pluginLogger : Logger.getLogger("EmpireWand");
        this.permissionPrefix = "ew"; // Default, can be overridden
    }
    
    /**
     * Enhanced constructor with command context for better error suggestions.
     */
    public CommandErrorHandler(@NotNull EmpireWandPlugin plugin, @NotNull String permissionPrefix,
                              @NotNull Map<String, SubCommand> availableCommands) {
        this.logger = plugin.getLogger();
        this.permissionPrefix = permissionPrefix;
        this.availableCommands.putAll(availableCommands);
    }

    /**
     * Handles a {@link CommandException} with enhanced error messages and smart suggestions.
     *
     * @param sender      The command sender to notify
     * @param exception   The command exception
     * @param commandName The command name being executed
     * @param args        The command arguments for context
     */
    public void handleCommandException(@NotNull CommandSender sender, @NotNull CommandException exception, 
                                     @NotNull String commandName, @NotNull String[] args) {
        Component errorMessage = createEnhancedErrorMessage(exception, commandName, args);
        sender.sendMessage(errorMessage);
        
        // Add suggestions based on error type
        Component suggestions = createErrorSuggestions(exception, commandName, args);
        if (suggestions != null) {
            sender.sendMessage(suggestions);
        }
        
        if (exception.getErrorCode() != null) {
            logger.info(String.format("[%s] %s %s", commandName, exception.getErrorCode(), 
                Arrays.toString(exception.getContext())));
        }
    }
    
    /**
     * Legacy method for backward compatibility.
     */
    public void handleCommandException(CommandSender sender, CommandException exception, String commandName) {
        handleCommandException(sender, exception, commandName, new String[0]);
    }

    /**
     * Handles an unexpected exception with detailed logging and user-friendly messaging.
     *
     * @param sender      The command sender to notify
     * @param exception   The unexpected exception
     * @param commandName The command name being executed
     * @param args        The command arguments
     */
    public void handleUnexpectedException(@NotNull CommandSender sender, @NotNull Exception exception, 
                                        @NotNull String commandName, @NotNull String[] args) {
        Component errorMessage = Component.text()
            .append(Component.text("❌ ", ERROR_COLOR))
            .append(Component.text("An unexpected error occurred while executing this command", ERROR_COLOR))
            .append(Component.newline())
            .append(Component.text("📝 ", HELP_COLOR))
            .append(Component.text("Please report this to an administrator", HELP_COLOR))
            .build();
        
        sender.sendMessage(errorMessage);
        
        // Suggest getting help
        Component helpSuggestion = Component.text()
            .append(Component.text("💡 ", SUGGESTION_COLOR))
            .append(Component.text("Try: ", HELP_COLOR))
            .append(Component.text("/" + permissionPrefix + " help " + commandName, SUGGESTION_COLOR)
                .clickEvent(ClickEvent.suggestCommand("/" + permissionPrefix + " help " + commandName))
                .hoverEvent(HoverEvent.showText(Component.text("Click to get help for this command"))))
            .build();
        
        sender.sendMessage(helpSuggestion);
        
        logger.severe(String.format("Unexpected exception in command '%s' with args %s: %s", 
            commandName, Arrays.toString(args), exception.getMessage()));
        exception.printStackTrace();
    }

    /**
     * Creates an enhanced error message with context and visual indicators.
     */
    @NotNull
    private Component createEnhancedErrorMessage(@NotNull CommandException exception, 
                                               @NotNull String commandName, 
                                               @NotNull String[] args) {
        var builder = Component.text();
        
        // Error icon and main message
        builder.append(Component.text("❌ ", ERROR_COLOR))
               .append(Component.text(exception.getMessage(), ERROR_COLOR));
        
        // Add context if available
        if (exception.getErrorCode() != null) {
            String contextInfo = getContextualErrorInfo(exception.getErrorCode(), args);
            if (contextInfo != null) {
                builder.append(Component.newline())
                       .append(Component.text("ℹ ", HELP_COLOR))
                       .append(Component.text(contextInfo, HELP_COLOR));
            }
        }
        
        return builder.build();
    }
    
    /**
     * Creates smart suggestions based on the error type and context.
     */
    @Nullable
    private Component createErrorSuggestions(@NotNull CommandException exception, 
                                           @NotNull String commandName, 
                                           @NotNull String[] args) {
        String errorCode = exception.getErrorCode();
        if (errorCode == null) {
            return null;
        }
        
        return switch (errorCode) {
            case "UNKNOWN_COMMAND" -> createUnknownCommandSuggestions(args.length > 0 ? args[0] : "");
            case "SPELLS_INVALID_TYPE" -> createSpellTypeSuggestions();
            case "NO_PERMISSION" -> createPermissionSuggestions(commandName);
            case "PLAYER_REQUIRED" -> createPlayerRequiredSuggestions();
            case "INVALID_ARGUMENT" -> createArgumentSuggestions(commandName, args);
            case "SPELL_NOT_FOUND" -> createSpellNotFoundSuggestions(args);
            default -> createGenericSuggestions(commandName);
        };
    }
    
    @NotNull
    private Component createUnknownCommandSuggestions(@NotNull String attempted) {
        var builder = Component.text();
        builder.append(Component.text("💡 ", SUGGESTION_COLOR))
               .append(Component.text("Did you mean: ", HELP_COLOR));
        
        // Find similar commands
        List<String> suggestions = findSimilarCommands(attempted);
        if (suggestions.isEmpty()) {
            builder.append(Component.text("/" + permissionPrefix + " help", SUGGESTION_COLOR)
                .clickEvent(ClickEvent.runCommand("/" + permissionPrefix + " help"))
                .hoverEvent(HoverEvent.showText(Component.text("Show all available commands"))));
        } else {
            for (int i = 0; i < Math.min(3, suggestions.size()); i++) {
                if (i > 0) builder.append(Component.text(", ", HELP_COLOR));
                String cmd = "/" + permissionPrefix + " " + suggestions.get(i);
                builder.append(Component.text(suggestions.get(i), SUGGESTION_COLOR)
                    .clickEvent(ClickEvent.suggestCommand(cmd))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to use this command"))));
            }
        }
        
        return builder.build();
    }
    
    @NotNull
    private Component createSpellTypeSuggestions() {
        return Component.text()
            .append(Component.text("💡 ", SUGGESTION_COLOR))
            .append(Component.text("Valid spell types: ", HELP_COLOR))
            .append(Component.text("fire, water, earth, air, lightning, utility, combat", SUGGESTION_COLOR))
            .build();
    }
    
    @NotNull
    private Component createPermissionSuggestions(@NotNull String commandName) {
        return Component.text()
            .append(Component.text("🔒 ", WARNING_COLOR))
            .append(Component.text("You don't have permission to use this command", WARNING_COLOR))
            .append(Component.newline())
            .append(Component.text("💬 ", HELP_COLOR))
            .append(Component.text("Contact an administrator if you believe this is an error", HELP_COLOR))
            .build();
    }
    
    @NotNull
    private Component createPlayerRequiredSuggestions() {
        return Component.text()
            .append(Component.text("👤 ", WARNING_COLOR))
            .append(Component.text("This command can only be used by players, not from console", WARNING_COLOR))
            .build();
    }
    
    @NotNull
    private Component createArgumentSuggestions(@NotNull String commandName, @NotNull String[] args) {
        return Component.text()
            .append(Component.text("💡 ", SUGGESTION_COLOR))
            .append(Component.text("Usage: ", HELP_COLOR))
            .append(Component.text("/" + permissionPrefix + " help " + commandName, SUGGESTION_COLOR)
                .clickEvent(ClickEvent.runCommand("/" + permissionPrefix + " help " + commandName))
                .hoverEvent(HoverEvent.showText(Component.text("Get detailed help for this command"))))
            .build();
    }
    
    @NotNull
    private Component createSpellNotFoundSuggestions(@NotNull String[] args) {
        var builder = Component.text();
        builder.append(Component.text("💡 ", SUGGESTION_COLOR))
               .append(Component.text("Try: ", HELP_COLOR))
               .append(Component.text("/" + permissionPrefix + " spells", SUGGESTION_COLOR)
                   .clickEvent(ClickEvent.runCommand("/" + permissionPrefix + " spells"))
                   .hoverEvent(HoverEvent.showText(Component.text("View all available spells"))))
               .append(Component.text(" to see all available spells", HELP_COLOR));
        
        return builder.build();
    }
    
    @NotNull
    private Component createGenericSuggestions(@NotNull String commandName) {
        return Component.text()
            .append(Component.text("💡 ", SUGGESTION_COLOR))
            .append(Component.text("Get help: ", HELP_COLOR))
            .append(Component.text("/" + permissionPrefix + " help " + commandName, SUGGESTION_COLOR)
                .clickEvent(ClickEvent.suggestCommand("/" + permissionPrefix + " help " + commandName))
                .hoverEvent(HoverEvent.showText(Component.text("Click to get help for this command"))))
            .build();
    }
    
    @Nullable
    private String getContextualErrorInfo(@NotNull String errorCode, @NotNull String[] args) {
        return switch (errorCode) {
            case "SPELLS_INVALID_TYPE" -> 
                args.length > 1 ? "'" + args[1] + "' is not a valid spell type" : null;
            case "SPELL_NOT_FOUND" -> 
                args.length > 1 ? "Spell '" + args[1] + "' does not exist" : null;
            case "INVALID_ARGUMENT" -> 
                "Check your command syntax and try again";
            default -> null;
        };
    }
    
    @NotNull
    private List<String> findSimilarCommands(@NotNull String attempted) {
        if (availableCommands.isEmpty()) {
            return List.of("help", "get", "spells", "bind"); // Fallback common commands
        }
        
        return availableCommands.keySet().stream()
            .filter(cmd -> calculateSimilarity(cmd, attempted) > 0.3)
            .sorted((a, b) -> Double.compare(calculateSimilarity(b, attempted), calculateSimilarity(a, attempted)))
            .limit(3)
            .collect(Collectors.toList());
    }
    
    private double calculateSimilarity(@NotNull String str1, @NotNull String str2) {
        if (str1.equals(str2)) return 1.0;
        if (str1.startsWith(str2) || str2.startsWith(str1)) return 0.8;
        
        // Simple character overlap calculation
        Set<Character> chars1 = str1.toLowerCase().chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        Set<Character> chars2 = str2.toLowerCase().chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        
        Set<Character> intersection = new HashSet<>(chars1);
        intersection.retainAll(chars2);
        
        Set<Character> union = new HashSet<>(chars1);
        union.addAll(chars2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    // Enhanced static factory methods for message creation
    
    /**
     * Creates an error message component with enhanced formatting.
     *
     * @param message The message content
     * @return Error component with icon
     */
    @NotNull
    public static Component createErrorMessage(@NotNull String message) {
        return Component.text()
            .append(Component.text("❌ ", ERROR_COLOR))
            .append(Component.text(message, ERROR_COLOR))
            .build();
    }

    /**
     * Creates a success message component with enhanced formatting.
     *
     * @param message The message content
     * @return Success component with icon
     */
    @NotNull
    public static Component createSuccessMessage(@NotNull String message) {
        return Component.text()
            .append(Component.text("✅ ", NamedTextColor.GREEN))
            .append(Component.text(message, NamedTextColor.GREEN))
            .build();
    }

    /**
     * Creates an info message component with enhanced formatting.
     *
     * @param message The message content
     * @return Info component with icon
     */
    @NotNull
    public static Component createInfoMessage(@NotNull String message) {
        return Component.text()
            .append(Component.text("ℹ ", NamedTextColor.AQUA))
            .append(Component.text(message, NamedTextColor.YELLOW))
            .build();
    }
    
    /**
     * Creates a warning message component with enhanced formatting.
     *
     * @param message The message content
     * @return Warning component with icon
     */
    @NotNull
    public static Component createWarningMessage(@NotNull String message) {
        return Component.text()
            .append(Component.text("⚠ ", WARNING_COLOR))
            .append(Component.text(message, WARNING_COLOR))
            .build();
    }
    
    /**
     * Creates a progress message component for long operations.
     *
     * @param message The message content
     * @param percentage Progress percentage (0-100)
     * @return Progress component with bar
     */
    @NotNull
    public static Component createProgressMessage(@NotNull String message, int percentage) {
        int filledBars = Math.max(0, Math.min(10, percentage / 10));
        int emptyBars = 10 - filledBars;
        
        return Component.text()
            .append(Component.text("⏳ ", NamedTextColor.YELLOW))
            .append(Component.text(message + " ", NamedTextColor.WHITE))
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("█".repeat(filledBars), NamedTextColor.GREEN))
            .append(Component.text("░".repeat(emptyBars), NamedTextColor.DARK_GRAY))
            .append(Component.text("] ", NamedTextColor.GRAY))
            .append(Component.text(percentage + "%", NamedTextColor.YELLOW))
            .build();
    }
}

