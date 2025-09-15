package nl.wantedchef.empirewand.command.admin;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Advanced cooldown management command supporting:
 * - /ew cd [true/false] - Toggle personal cooldown
 * - /ew admin cd [true/false] {player} - Admin toggle cooldown for player
 * - /ew cd clear - Clear personal cooldowns
 * - /ew admin cd clear {player} - Admin clear cooldowns for player
 * - /ew cd status - Check personal cooldown status
 * - /ew admin cd status {player} - Admin check cooldown status for player
 */
public class CooldownCommand implements SubCommand {

    private final String wandType;

    public CooldownCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "cd";
    }

    @Override
    public @Nullable String getPermission() {
        return null; // We'll handle permissions dynamically
    }

    @Override
    public @NotNull String getUsage() {
        return "cd [true|false|clear|status] | admin cd [true|false|clear|status] <player>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Manage cooldown settings for yourself or other players";
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        String[] args = context.args();

        if (args.length == 1) {
            return Arrays.asList("true", "false", "clear", "status", "admin")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            return List.of("cd");
        }

        if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && "cd".equalsIgnoreCase(args[1])) {
            return Arrays.asList("true", "false", "clear", "status")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && "admin".equalsIgnoreCase(args[0]) && "cd".equalsIgnoreCase(args[1])) {
            String action = args[2].toLowerCase();
            if (Arrays.asList("true", "false", "clear", "status").contains(action)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        String[] args = context.args();

        if (args.length == 0) {
            throw new CommandException("Usage: " + getUsage());
        }

        String firstArg = args[0].toLowerCase();

        // Handle admin commands
        if ("admin".equals(firstArg)) {
            handleAdminCommand(context);
            return;
        }

        // Handle personal commands
        handlePersonalCommand(context, firstArg);
    }

    private void handlePersonalCommand(@NotNull CommandContext context, @NotNull String action)
            throws CommandException {
        Player player = context.requirePlayer();

        switch (action) {
            case "true": {
                if (!context.hasPermission(wandType + ".command.cooldown.toggle")) {
                    throw new CommandException("You don't have permission to toggle cooldown.");
                }
                // Get wand from player's hand
                var wand = player.getInventory().getItemInMainHand();
                if (!context.wandService().isWand(wand)) {
                    throw new CommandException("You must be holding a wand to toggle cooldown.");
                }
                context.cooldownManager().setCooldownDisabled(player.getUniqueId(), wand, false);
                context.sendMessage(Component.text("Cooldown enabled").color(NamedTextColor.GREEN));
                break;
            }

            case "false": {
                if (!context.hasPermission(wandType + ".command.cooldown.toggle")) {
                    throw new CommandException("You don't have permission to toggle cooldown.");
                }
                // Get wand from player's hand
                var wand = player.getInventory().getItemInMainHand();
                if (!context.wandService().isWand(wand)) {
                    throw new CommandException("You must be holding a wand to toggle cooldown.");
                }
                context.cooldownManager().setCooldownDisabled(player.getUniqueId(), wand, true);
                context.sendMessage(Component.text("Cooldown disabled").color(NamedTextColor.GREEN));
                break;
            }

            case "clear":
                if (!context.hasPermission(wandType + ".command.cooldown.clear")) {
                    throw new CommandException("You don't have permission to clear cooldown.");
                }
                context.cooldownManager().clearPlayerCooldowns(player.getUniqueId());
                context.sendMessage(Component.text("Cooldown cleared").color(NamedTextColor.GREEN));
                break;

            case "status":
                if (!context.hasPermission(wandType + ".command.cooldown.status")) {
                    throw new CommandException("You don't have permission to check cooldown status.");
                }
                showCooldownStatus(context, player);
                break;

            default:
                throw new CommandException("Usage: " + getUsage());
        }
    }

    private void handleAdminCommand(@NotNull CommandContext context) throws CommandException {
        String[] args = context.args();

        if (args.length < 3) {
            throw new CommandException("Usage: " + getUsage());
        }

        if (!"cd".equalsIgnoreCase(args[1])) {
            throw new CommandException("Usage: " + getUsage());
        }

        String action = args[2].toLowerCase();

        switch (action) {
            case "true":
            case "false":
                if (args.length != 4) {
                    throw new CommandException("Usage: admin cd [true|false] <player>");
                }
                handleAdminToggle(context, Boolean.parseBoolean(action), args[3]);
                break;

            case "clear":
                if (args.length != 4) {
                    throw new CommandException("Usage: admin cd clear <player>");
                }
                handleAdminClear(context, args[3]);
                break;

            case "status":
                if (args.length != 4) {
                    throw new CommandException("Usage: admin cd status <player>");
                }
                handleAdminStatus(context, args[3]);
                break;

            default:
                throw new CommandException("Usage: " + getUsage());
        }
    }

    private void handleAdminToggle(@NotNull CommandContext context, boolean enable, @NotNull String targetName)
            throws CommandException {
        if (!context.hasPermission(wandType + ".command.cooldown.admin")) {
            throw new CommandException("You don't have permission to manage other players' cooldowns.");
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            throw new CommandException("Player not found: " + targetName);
        }

        // Get wand from target player's hand
        var wand = target.getInventory().getItemInMainHand();
        if (!context.wandService().isWand(wand)) {
            throw new CommandException("Target player must be holding a wand.");
        }

        context.cooldownManager().setCooldownDisabled(target.getUniqueId(), wand, !enable);

        String status = enable ? "enabled" : "disabled";
        context.sendMessage(Component.text("Cooldown " + status + " for " + target.getName())
                .color(NamedTextColor.GREEN));

        // Notify target player
        target.sendMessage(Component.text("Your cooldown has been " + status + " by an admin")
                .color(enable ? NamedTextColor.YELLOW : NamedTextColor.GREEN));
    }

    private void handleAdminClear(@NotNull CommandContext context, @NotNull String targetName) throws CommandException {
        if (!context.hasPermission(wandType + ".command.cooldown.admin")) {
            throw new CommandException("You don't have permission to manage other players' cooldowns.");
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            throw new CommandException("Player not found: " + targetName);
        }

        context.cooldownManager().clearPlayerCooldowns(target.getUniqueId());

        context.sendMessage(Component.text("Cooldown cleared for " + target.getName())
                .color(NamedTextColor.GREEN));

        // Notify target player
        target.sendMessage(Component.text("Your cooldown has been cleared by an admin")
                .color(NamedTextColor.GREEN));
    }

    private void handleAdminStatus(@NotNull CommandContext context, @NotNull String targetName)
            throws CommandException {
        if (!context.hasPermission(wandType + ".command.cooldown.admin")) {
            throw new CommandException("You don't have permission to check other players' cooldowns.");
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            throw new CommandException("Player not found: " + targetName);
        }

        showCooldownStatus(context, target);
    }

    private void showCooldownStatus(@NotNull CommandContext context, @NotNull Player target) {
        // Get wand from target player's hand
        var wand = target.getInventory().getItemInMainHand();
        if (!context.wandService().isWand(wand)) {
            context.sendMessage(Component.text("Player must be holding a wand to check cooldown status.")
                    .color(NamedTextColor.RED));
            return;
        }

        boolean disabled = context.cooldownManager().isCooldownDisabled(target.getUniqueId(), wand);
        boolean enabled = !disabled;

        Component statusMessage = Component.text("Cooldown status for " + target.getName() + ":")
                .color(NamedTextColor.GRAY)
                .append(Component.newline())
                .append(Component.text("Enabled: ").color(NamedTextColor.GRAY))
                .append(Component.text(enabled ? "Yes" : "No")
                        .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED));

        // Show remaining cooldown for last spell if any
        if (enabled) {
            statusMessage = statusMessage
                    .append(Component.newline())
                    .append(Component.text("Wand: ").color(NamedTextColor.GRAY))
                    .append(Component.text(wand.getType().toString()).color(NamedTextColor.YELLOW));
        }

        context.sendMessage(statusMessage);
    }
}
