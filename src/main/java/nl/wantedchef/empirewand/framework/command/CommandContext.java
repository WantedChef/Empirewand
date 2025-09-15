package nl.wantedchef.empirewand.framework.command;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.service.PermissionService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.framework.service.ConfigService;
import nl.wantedchef.empirewand.framework.service.UnifiedCooldownManager;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.core.util.PerformanceMonitor;
import net.kyori.adventure.text.Component;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * Immutable context object containing all dependencies for command execution.
 * Provides type-safe access to services and common validation methods.
 * Enhanced with performance monitoring and advanced validation capabilities.
 */
@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Record holds references to plugin/services by design; args() returns defensive copy.")
public record CommandContext(
        @NotNull EmpireWandPlugin plugin,
        @NotNull CommandSender sender,
        @NotNull String[] args,
        @NotNull ConfigService config,
        @NotNull FxService fx,
        @NotNull SpellRegistry spellRegistry,
        @NotNull WandService wandService,
        @NotNull UnifiedCooldownManager cooldownManager,
        @NotNull PermissionService permissionService) {

    /**
     * Gets the sender as a Player if possible.
     * 
     * @return Player instance or null if sender is not a player
     */
    @Nullable
    public Player asPlayer() {
        return sender instanceof Player player ? player : null;
    }

    /**
     * Requires the sender to be a player.
     * 
     * @return Player instance
     * @throws CommandException if sender is not a player
     */
    @NotNull
    public Player requirePlayer() throws CommandException {
        if (!(sender instanceof Player player)) {
            throw new CommandException("This command can only be used by players");
        }
        return player;
    }

    /**
     * Checks if the sender has the specified permission.
     */
    public boolean hasPermission(@NotNull String permission) {
        return permissionService.has(sender, permission);
    }

    /**
     * Requires the sender to have the specified permission.
     * 
     * @throws CommandException if permission is missing
     */
    public void requirePermission(@NotNull String permission) throws CommandException {
        if (!hasPermission(permission)) {
            throw new CommandException("No permission");
        }
    }

    /**
     * Sends a message to the command sender.
     */
    public void sendMessage(@NotNull Component message) {
        sender.sendMessage(message);
    }

    /**
     * Gets command argument at index with validation.
     */
    @NotNull
    public String getArg(int index) throws CommandException {
        if (index >= args.length) {
            throw new CommandException("Missing required argument at position " + index);
        }
        return args[index];
    }

    /**
     * Gets the command arguments (defensive copy).
     */
    @Override
    @NotNull
    public String[] args() {
        return args.clone();
    }

    /**
     * Gets optional command argument at index.
     */
    @Nullable
    public String getArgOrNull(int index) {
        return index < args.length ? args[index] : null;
    }

    /**
     * Starts a performance timing context for the specified operation.
     * 
     * @param operation The operation being timed
     * @return A TimingContext for measuring execution time
     */
    public PerformanceMonitor.TimingContext startTiming(@NotNull String operation) {
        return plugin.getPerformanceMonitor().startTiming("command." + operation, 50L);
    }

    /**
     * Logs a command execution with performance metrics.
     * 
     * @param commandName The name of the command executed
     * @param executionTimeMs The execution time in milliseconds
     * @param success Whether the command executed successfully
     */
    public void logCommandExecution(@NotNull String commandName, long executionTimeMs, boolean success) {
        plugin.getLogger().log(Level.INFO, String.format(
                "Command executed: %s by %s in %dms (success: %s)",
                commandName,
                sender.getName(),
                executionTimeMs,
                success ? "true" : "false"));
    }

    /**
     * Validates that a string argument matches one of the allowed values.
     * 
     * @param index The argument index
     * @param allowedValues The allowed values
     * @return The validated argument value
     * @throws CommandException if the argument is invalid
     */
    public @NotNull String validateEnumArg(int index, @NotNull String... allowedValues) throws CommandException {
        String value = getArg(index).toLowerCase();
        for (String allowed : allowedValues) {
            if (allowed.toLowerCase().equals(value)) {
                return value;
            }
        }
        throw new CommandException("Invalid value '" + value + "'. Allowed values: " + String.join(", ", allowedValues));
    }

    /**
     * Validates that an integer argument is within the specified range.
     * 
     * @param index The argument index
     * @param min The minimum allowed value
     * @param max The maximum allowed value
     * @return The validated integer value
     * @throws CommandException if the argument is invalid
     */
    public int validateIntArg(int index, int min, int max) throws CommandException {
        try {
            int value = Integer.parseInt(getArg(index));
            if (value < min || value > max) {
                throw new CommandException("Value must be between " + min + " and " + max);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid number: " + getArg(index));
        }
    }

    /**
     * Validates that a boolean argument is valid.
     * 
     * @param index The argument index
     * @return The validated boolean value
     * @throws CommandException if the argument is invalid
     */
    public boolean validateBooleanArg(int index) throws CommandException {
        String value = getArg(index).toLowerCase();
        if ("true".equals(value) || "false".equals(value)) {
            return Boolean.parseBoolean(value);
        }
        throw new CommandException("Invalid boolean value: " + value + ". Use 'true' or 'false'");
    }
}
