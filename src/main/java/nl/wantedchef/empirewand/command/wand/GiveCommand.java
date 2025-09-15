package nl.wantedchef.empirewand.command.wand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to give a wand to a player.
 * Enhanced version of the get command that allows administrators to give wands to other players.
 */
public class GiveCommand implements SubCommand, CommandHelpProvider.HelpAwareCommand {

    private final String permissionPrefix;

    public GiveCommand(@NotNull String permissionPrefix, @NotNull String wandDisplayName, @NotNull Material wandMaterial) {
        this.permissionPrefix = permissionPrefix;
    }

    @Override
    public @NotNull String getName() {
        return "give";
    }

    @Override
    public @Nullable String getPermission() {
        return permissionPrefix + ".give";
    }

    @Override
    public @NotNull String getUsage() {
        return "give <player> [wandKey]";
    }

    @Override
    public @NotNull String getDescription() {
        return "Give a wand to a player";
    }

    @Override
    public boolean requiresPlayer() {
        return false; // Can be used from console
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("grant");
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        try (var timing = context.startTiming("wand.give")) {
            String[] args = context.args();

            if (args.length < 2) {
                throw new CommandException("Usage: " + getUsage(), "INVALID_USAGE");
            }

            String targetPlayerName = args[1];
            String wandKey = args.length > 2 ? args[2] : getDefaultWandKey();

            // Find target player
            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
            if (targetPlayer == null) {
                throw new CommandException("Player not found: " + targetPlayerName, "PLAYER_NOT_FOUND");
            }

            // Validate wand key
            if (!isValidWandKey(wandKey)) {
                throw new CommandException("Invalid wand key: " + wandKey + ". Use 'empirewand' or 'mephidantes_zeist'", "INVALID_WAND_KEY");
            }

            // Give the appropriate wand
            giveWandToPlayer(context, targetPlayer, wandKey);

            // Send confirmation messages
            String displayName = formatWandDisplayName(wandKey);

            // Message to command sender
            context.sendMessage(Component.text("Gave " + displayName + " to " + targetPlayer.getName())
                    .color(NamedTextColor.GREEN));

            // Message to target player
            targetPlayer.sendMessage(Component.text("You have received a " + displayName + " from " + context.sender().getName() + "!")
                    .color(NamedTextColor.GREEN));

        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw e;
            }
            throw new CommandException("Failed to give wand: " + e.getMessage(), e, "WAND_GIVE_FAILED");
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        String[] args = context.args();

        if (args.length == 2) {
            // Complete player names
            String partial = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        } else if (args.length == 3) {
            // Complete wand keys
            String partial = args[2].toLowerCase();
            List<String> wandKeys = List.of("empirewand", "mephidantes_zeist");
            return wandKeys.stream()
                    .filter(key -> key.startsWith(partial))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public @NotNull List<CommandHelpProvider.CommandExample> getExamples() {
        return List.of(
                new CommandHelpProvider.CommandExample("give Steve", "Give default wand to Steve"),
                new CommandHelpProvider.CommandExample("give Alex empirewand", "Give Empire Wand to Alex"),
                new CommandHelpProvider.CommandExample("give Bob mephidantes_zeist", "Give Mephidantes Zeist to Bob")
        );
    }

    private String getDefaultWandKey() {
        // Return default wand based on permission prefix
        return permissionPrefix.equals("mephidanteszeist") ? "mephidantes_zeist" : "empirewand";
    }

    private boolean isValidWandKey(@NotNull String wandKey) {
        return wandKey.equals("empirewand") || wandKey.equals("mephidantes_zeist");
    }

    private void giveWandToPlayer(@NotNull CommandContext context, @NotNull Player targetPlayer, @NotNull String wandKey)
            throws CommandException {

        try {
            if (wandKey.equals("mephidantes_zeist")) {
                context.wandService().giveMephidantesZeist(targetPlayer);
            } else {
                context.wandService().giveWand(targetPlayer);
            }
        } catch (Exception e) {
            throw new CommandException("Failed to give wand to player: " + e.getMessage(), e, "WAND_SERVICE_ERROR");
        }
    }

    private String formatWandDisplayName(@NotNull String wandKey) {
        return switch (wandKey.toLowerCase()) {
            case "empirewand" -> "Empire Wand";
            case "mephidantes_zeist" -> "Mephidantes Zeist";
            default -> {
                // Convert snake_case to Title Case
                String[] parts = wandKey.split("_");
                StringBuilder displayName = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        displayName.append(" ");
                    }
                    String part = parts[i];
                    displayName.append(part.substring(0, 1).toUpperCase())
                              .append(part.substring(1).toLowerCase());
                }
                yield displayName.toString();
            }
        };
    }
}