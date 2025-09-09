package nl.wantedchef.empirewand.framework.command;

import nl.wantedchef.empirewand.EmpireWandPlugin;
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
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Base class for all wand command executors.
 * Handles common functionality like subcommand registration,
 * permission checking, and error handling.
 */
public abstract class BaseWandCommand implements CommandExecutor, TabCompleter {

    protected final EmpireWandPlugin plugin;
    private final Map<String, SubCommand> subcommands = new LinkedHashMap<>();
    private final Map<String, SubCommand> aliases = new HashMap<>();

    protected BaseWandCommand(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        registerSubcommands();
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
        subcommands.put(subCommand.getName().toLowerCase(), subCommand);

        // Register aliases
        for (String alias : subCommand.getAliases()) {
            aliases.put(alias.toLowerCase(), subCommand);
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
            // Create context
            CommandContext context = createContext(sender, args);

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

        } catch (CommandException e) {
            sender.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error executing command: " + String.join(" ", args), e);
            sender.sendMessage(Component.text("An internal error occurred").color(NamedTextColor.RED));
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
        Component header = Component.text("=== " + getWandDisplayName() + " Commands ===")
                .color(NamedTextColor.GOLD);
        sender.sendMessage(header);

        for (SubCommand subCommand : subcommands.values()) {
            String permission = subCommand.getPermission();
            if (permission != null && !plugin.getPermissionService().has(sender, permission)) {
                continue;
            }

            Component usage = Component.text("/" + getPermissionPrefix() + " " + subCommand.getUsage())
                    .color(NamedTextColor.YELLOW);
            Component description = Component.text(" - " + subCommand.getDescription())
                    .color(NamedTextColor.GRAY);

            sender.sendMessage(usage.append(description));
        }
    }
}





