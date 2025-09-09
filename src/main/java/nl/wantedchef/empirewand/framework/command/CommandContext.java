package nl.wantedchef.empirewand.framework.command;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.service.PermissionService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.framework.service.ConfigService;
import nl.wantedchef.empirewand.framework.service.CooldownService;
import nl.wantedchef.empirewand.framework.service.FxService;
import net.kyori.adventure.text.Component;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable context object containing all dependencies for command execution.
 * Provides type-safe access to services and common validation methods.
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
        @NotNull CooldownService cooldownService,
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
}
