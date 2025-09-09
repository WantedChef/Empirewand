package nl.wantedchef.empirewand.framework.command;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.util.CommandErrorHandler;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider;
import nl.wantedchef.empirewand.framework.command.util.HelpCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base class for all wand command executors.
 * Handles common functionality like subcommand registration,
 * permission checking, and error handling.
 * Enhanced with performance monitoring, advanced help system, and better alias management.
 */
public abstract class BaseWandCommand implements CommandExecutor, TabCompleter {

    protected final EmpireWandPlugin plugin;
    private final CommandErrorHandler errorHandler;
    private final Map<String, SubCommand> subcommands = new LinkedHashMap<>();
    private final Map<String, SubCommand> aliases = new HashMap<>();

    protected BaseWandCommand(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.errorHandler = new CommandErrorHandler(plugin);
        registerSubcommands();
        
        // Register the enhanced help command
        register(new HelpCommand(getPermissionPrefix(), getPermissionPrefix(), subcommands, aliases));
    }

    /**
     * Register all subcommands for this wand type.
     * Called during construction.
     */
    protected abstract void registerSubcommands();

    /**
     * Get the permission prefix for this wand type.
     * E.g., "empirewand" or "mephidanteszeist"
     */
    protected abstract String getPermissionPrefix();

    /**
     * Get the display name for this wand type.
     */
    protected abstract String getWandDisplayName();

    /**
     * Register a subcommand.
     */
    protected void register(SubCommand subCommand) {
        String commandName = subCommand.getName().toLowerCase();
        
        // Check for duplicate command names
        if (subcommands.containsKey(commandName)) {
            plugin.getLogger().warning("Duplicate command registration: " + commandName);
            return;
        }
        
        subcommands.put(commandName, subCommand);

        // Register aliases
        for (String alias : subCommand.getAliases()) {
            String aliasLower = alias.toLowerCase();
            if (aliases.containsKey(aliasLower)) {
                plugin.getLogger().warning("Duplicate alias registration: " + aliasLower);
                continue;
            }
            aliases.put(aliasLower, subCommand);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

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
            
            // Start timing for performance monitoring
            long startTime = System.nanoTime();
            boolean success = false;
            
            try {
                // Check permission
                String permission = subCommand.getPermission();
                if (permission != null) {
                    context.requirePermission(permission);
                }

                // Check player requirement
                if (subCommand.requiresPlayer()) {
                    context.requirePlayer();
                }

                // Execute command
                subCommand.execute(context);
                
                success = true;
            } finally {
                // Log execution metrics
                long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
                context.logCommandExecution(subCommandName, executionTimeMs, success);
            }

        } catch (CommandException e) {
            errorHandler.handleCommandException(sender, e, subCommandName);
        } catch (Exception e) {
            errorHandler.handleUnexpectedException(sender, e, subCommandName, args);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {

        if (args.length == 1) {
            // Complete subcommand names
            String partial = args[0].toLowerCase();
            return subcommands.keySet().stream()
                    .filter(name -> name.startsWith(partial))
                    .filter(name -> {
                        SubCommand sub = subcommands.get(name);
                        String perm = sub.getPermission();
                        return perm == null || plugin.getPermissionService().has(sender, perm);
                    })
                    .collect(Collectors.toList());
        }

        if (args.length > 1) {
            // Delegate to subcommand
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subcommands.get(subCommandName);

            if (subCommand == null) {
                subCommand = aliases.get(subCommandName);
            }

            if (subCommand != null) {
                try {
                    CommandContext context = createContext(sender, args);
                    return subCommand.tabComplete(context);
                } catch (Exception e) {
                    return List.of();
                }
            }
        }

        return List.of();
    }

    private CommandContext createContext(CommandSender sender, String[] args) {
        return new CommandContext(
                plugin,
                sender,
                args,
                plugin.getConfigService(),
                plugin.getFxService(),
                plugin.getSpellRegistry(),
                plugin.getWandService(),
                plugin.getCooldownService(),
                plugin.getPermissionService());
    }

    private void sendUsage(CommandSender sender) {
        Component helpMessage = CommandHelpProvider.generateHelpOverview(
            sender, 
            subcommands, 
            getPermissionPrefix(), 
            getWandDisplayName()
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
}





