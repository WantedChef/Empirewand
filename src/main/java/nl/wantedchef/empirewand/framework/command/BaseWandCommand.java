package nl.wantedchef.empirewand.framework.command;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.command.admin.CooldownCommand;
import nl.wantedchef.empirewand.command.admin.MigrateCommand;
import nl.wantedchef.empirewand.command.admin.ReloadCommand;
import nl.wantedchef.empirewand.command.wand.BindAllCommand;
import nl.wantedchef.empirewand.command.wand.BindCategoryCommand;
import nl.wantedchef.empirewand.command.wand.BindCommand;
import nl.wantedchef.empirewand.command.wand.BindTypeCommand;
import nl.wantedchef.empirewand.command.wand.GetCommand;
import nl.wantedchef.empirewand.command.wand.GiveCommand;
import nl.wantedchef.empirewand.command.wand.GuiCommand;
import nl.wantedchef.empirewand.command.wand.ListCommand;
import nl.wantedchef.empirewand.command.wand.SetSpellCommand;
import nl.wantedchef.empirewand.command.wand.SpellsCommand;
import nl.wantedchef.empirewand.command.wand.StatsCommand;
import nl.wantedchef.empirewand.command.wand.SwitchEffectCommand;
import nl.wantedchef.empirewand.command.wand.ToggleCommand;
import nl.wantedchef.empirewand.command.wand.UnbindCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandCache;
import nl.wantedchef.empirewand.framework.command.util.CommandErrorHandler;

import nl.wantedchef.empirewand.framework.command.util.CommandPerformanceMonitor;
import nl.wantedchef.empirewand.framework.command.util.HelpCommand;
import nl.wantedchef.empirewand.framework.command.util.InteractiveHelpProvider;
import nl.wantedchef.empirewand.framework.command.util.SmartTabCompleter;

/**
 * Base class for all wand command executors. Handles common functionality like subcommand
 * registration, permission checking, and error handling. Enhanced with performance monitoring,
 * advanced help system, and better alias management.
 */
public abstract class BaseWandCommand implements CommandExecutor, TabCompleter {

    protected final EmpireWandPlugin plugin;
    private final CommandErrorHandler errorHandler;
    private final CommandCache commandCache;
    private final CommandPerformanceMonitor performanceMonitor;
    private final SmartTabCompleter smartTabCompleter;
    private final Map<String, SubCommand> subcommands = new LinkedHashMap<>();
    private final Map<String, SubCommand> aliases = new HashMap<>();

    protected BaseWandCommand(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.commandCache = new CommandCache();
        this.performanceMonitor = new CommandPerformanceMonitor(plugin);
    this.smartTabCompleter = new SmartTabCompleter(commandCache, plugin.getSpellRegistry(), plugin);
        // Initialize error handler with basic constructor first, will update after initialization
        this.errorHandler = new CommandErrorHandler(plugin);
        // Defer initialization to avoid calling overridable methods in constructor
        this.isInitialized = false;
    }

    private boolean isInitialized = false;

    /**
     * Initialize the command system. This must be called after construction but before using the
     * command.
     */
    protected final void initialize() {
        if (isInitialized) {
            return;
        }

        registerSubcommands();

        // Register the enhanced help command
        register(new HelpCommand(getPermissionPrefix(), getPermissionPrefix(), subcommands,
                aliases));

        isInitialized = true;
    }

    /**
     * Ensures the command is properly initialized before use.
     */
    private void ensureInitialized() {
        if (!isInitialized) {
            initialize();
        }
    }

    /**
     * Register all subcommands for this wand type. Called during construction.
     * Default implementation registers all standard wand commands.
     */
    protected void registerSubcommands() {
        String prefix = getPermissionPrefix();
        String displayName = getWandDisplayName();
        Material wandMaterial = getWandMaterial();

        // Core wand management commands
        register(new GetCommand(prefix, displayName, wandMaterial));
        register(new GiveCommand(prefix, displayName, wandMaterial));
        register(new BindCommand(prefix));
        register(new UnbindCommand(prefix));
        register(new BindAllCommand(prefix));
        register(new ListCommand(prefix));
        register(new SetSpellCommand(prefix));
        register(new ToggleCommand(prefix));
        register(new SwitchEffectCommand(plugin, prefix));

        // GUI commands
        register(new GuiCommand(prefix));

        // Advanced binding commands
        register(new BindTypeCommand(prefix));
        register(new BindCategoryCommand(prefix));

        // Information commands
        register(new SpellsCommand(prefix));
        register(new StatsCommand(prefix));

        // System commands
        register(new ReloadCommand(prefix));
        register(new MigrateCommand(prefix));

        // Cooldown management
        register(new CooldownCommand(prefix));
    }

    /**
     * Get the permission prefix for this wand type. E.g., "empirewand" or "mephidanteszeist"
     */
    protected abstract String getPermissionPrefix();

    /**
     * Get the display name for this wand type.
     */
    protected abstract String getWandDisplayName();

    /**
     * Get the material for this wand type.
     */
    protected abstract Material getWandMaterial();

    /**
     * Register a subcommand.
     */
    protected void register(SubCommand subCommand) {
        String commandName = subCommand.getName().toLowerCase();

        // Check for duplicate command names
        if (subcommands.containsKey(commandName)) {
            plugin.getLogger()
                    .warning(String.format("Duplicate command registration: %s", commandName));
            return;
        }

        subcommands.put(commandName, subCommand);

        // Register aliases
        for (String alias : subCommand.getAliases()) {
            String aliasLower = alias.toLowerCase();
            if (aliases.containsKey(aliasLower)) {
                plugin.getLogger()
                        .warning(String.format("Duplicate alias registration: %s", aliasLower));
                continue;
            }
            aliases.put(aliasLower, subCommand);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        ensureInitialized();

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subcommands.get(subCommandName);

        if (subCommand == null) {
            subCommand = aliases.get(subCommandName);
        }

        if (subCommand == null) {
            sendUsage(sender);
            return true;
        }

        try {
            // Create context with performance monitoring
            CommandContext context = createContext(sender, args);

            // Start performance monitoring
            CommandPerformanceMonitor.ExecutionContext perfContext = 
                performanceMonitor.startExecution(subCommandName, sender.getName());
            boolean success = false;
            String errorType = "none";

            try {
                // Check permission with caching
                String permission = subCommand.getPermission();
                if (permission != null) {
                    Boolean cachedPermission = commandCache.getCachedPermission(sender, permission);
                    if (cachedPermission != null) {
                        if (!cachedPermission) {
                            throw new CommandException("No permission", "NO_PERMISSION");
                        }
                    } else {
                        boolean hasPermission = context.hasPermission(permission);
                        commandCache.cachePermission(sender, permission, hasPermission);
                        if (!hasPermission) {
                            throw new CommandException("No permission", "NO_PERMISSION");
                        }
                    }
                }

                // Check player requirement
                if (subCommand.requiresPlayer()) {
                    context.requirePlayer();
                }

                // Execute command
                subCommand.execute(context);

                success = true;
            } catch (CommandException e) {
                errorType = e.getErrorCode() != null ? e.getErrorCode() : "command_exception";
                throw e;
            } catch (Exception e) {
                errorType = "unexpected_exception";
                throw e;
            } finally {
                // Record performance metrics
                performanceMonitor.recordExecution(perfContext, success, errorType);
            }

        } catch (CommandException e) {
            errorHandler.handleCommandException(sender, e, subCommandName, args);
        } catch (Exception e) {
            errorHandler.handleUnexpectedException(sender, e, subCommandName, args);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
            @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        ensureInitialized();

        if (args.length == 1) {
            // Complete subcommand names with smart completion
            String partial = args[0].toLowerCase();
            
            // Check cache first
            List<String> cached = commandCache.getCachedTabCompletion(partial, args);
            if (cached != null) {
                return cached;
            }
            
            List<String> completions = subcommands.keySet().stream()
                .filter(name -> {
                    SubCommand sub = subcommands.get(name);
                    String perm = sub.getPermission();

                    // If no permission is required, allow access
                    if (perm == null) {
                        return true;
                    }

                    // Use cached permission if available
                    Boolean cachedPerm = commandCache.getCachedPermission(sender, perm);
                    if (cachedPerm != null) {
                        return cachedPerm;
                    } else {
                        boolean hasPermission = plugin.getPermissionService().has(sender, perm);
                        commandCache.cachePermission(sender, perm, hasPermission);
                        return hasPermission;
                    }
                })
                .filter(name -> name.toLowerCase().startsWith(partial))
                .sorted()
                .collect(Collectors.toList());
                
            // Cache the results
            commandCache.cacheTabCompletion(partial, args, completions);
            return completions;
        }

        if (args.length > 1) {
            // Delegate to smart tab completer
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subcommands.get(subCommandName);

            if (subCommand == null) {
                subCommand = aliases.get(subCommandName);
            }

            if (subCommand != null) {
                try {
                    // Use smart tab completion
                    String currentArg = args.length > 0 ? args[args.length - 1] : "";
                    List<String> smartCompletions = smartTabCompleter.getSmartCompletions(
                        sender, subCommand, args, currentArg);
                    
                    // Fall back to subcommand's own completion if smart completer returns nothing
                    if (smartCompletions.isEmpty()) {
                        CommandContext context = createContext(sender, args);
                        return subCommand.tabComplete(context);
                    }
                    
                    return smartCompletions;
                } catch (Exception e) {
                    // Log the error but don't break tab completion
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "Tab completion error for {0}", subCommandName);
                    return List.of();
                }
            }
        }

        return List.of();
    }

    private CommandContext createContext(CommandSender sender, String[] args) {
        return new CommandContext(plugin, sender, args, plugin.getConfigService(),
                plugin.getFxService(), plugin.getSpellRegistry(), plugin.getWandService(),
                plugin.getCooldownManager(), plugin.getPermissionService());
    }

    private void sendUsage(CommandSender sender) {
        Component helpMessage = InteractiveHelpProvider.generateInteractiveHelp(
            sender, 
            subcommands, 
            aliases,
            getPermissionPrefix(), 
            getWandDisplayName(),
            1, // Default to page 1
            "" // No search term
        );
        sender.sendMessage(helpMessage);
    }

    /**
     * Gets all registered subcommands.
     * 
     * @return Map of command names to SubCommand instances
     */
    public Map<String, SubCommand> getSubcommands() {
        return new HashMap<>(subcommands);
    }

    /**
     * Gets all registered command aliases.
     * 
     * @return Map of alias names to SubCommand instances
     */
    public Map<String, SubCommand> getAliases() {
        return new HashMap<>(aliases);
    }

    /**
     * Gets the error handler for this command executor.
     * 
     * @return The command error handler
     */
    public CommandErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    /**
     * Gets the command cache for performance optimization.
     * 
     * @return The command cache
     */
    public CommandCache getCommandCache() {
        return commandCache;
    }
    
    /**
     * Gets the performance monitor for tracking command metrics.
     * 
     * @return The performance monitor
     */
    public CommandPerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    /**
     * Gets the smart tab completer for enhanced completions.
     * 
     * @return The smart tab completer
     */
    public SmartTabCompleter getSmartTabCompleter() {
        return smartTabCompleter;
    }
    
    /**
     * Invalidates all caches. Useful when permissions change or plugin reloads.
     */
    public void invalidateCaches() {
        commandCache.invalidateAll();
    }
    
    /**
     * Invalidates cached permissions for a specific sender.
     * 
     * @param sender The command sender
     */
    public void invalidatePermissions(@NotNull CommandSender sender) {
        commandCache.invalidatePermissions(sender);
    }
    
    /**
     * Gets performance statistics for command execution.
     * 
     * @return Performance report as formatted string
     */
    @NotNull
    public String getPerformanceReport() {
        return performanceMonitor.generatePerformanceReport();
    }
    
    /**
     * Gets cache statistics for monitoring cache effectiveness.
     * 
     * @return Cache statistics
     */
    @NotNull
    public CommandCache.CacheStats getCacheStats() {
        return commandCache.getStats();
    }
    
    /**
     * Records a user's tab completion selection for learning.
     * 
     * @param playerName The player name
     * @param selection The completion they selected
     */
    public void recordTabCompletionSelection(@NotNull String playerName, @NotNull String selection) {
        smartTabCompleter.recordUserSelection(playerName, selection);
    }
}


