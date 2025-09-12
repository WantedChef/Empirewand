package nl.wantedchef.empirewand.command.wand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.wantedchef.empirewand.core.wand.WandSettings;
import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;

/**
 * Command to manage toggle commands for wands.
 * Usage: /ew toggle [command] [on|off]
 * Usage: /mz toggle [command] [on|off]
 */
public class ToggleCommand implements SubCommand {

    private final String permissionPrefix;

    public ToggleCommand() {
        this("empirewand");
    }

    public ToggleCommand(@NotNull String permissionPrefix) {
        this.permissionPrefix = (permissionPrefix == null || permissionPrefix.isBlank()) ? "empirewand" : permissionPrefix;
    }

    @Override
    @NotNull
    public String getName() {
        return "toggle";
    }

    @Override
    @NotNull
    public List<String> getAliases() {
        return Arrays.asList("togglecmd", "tc");
    }

    @Override
    @Nullable
    public String getPermission() {
        return permissionPrefix + ".command.toggle";
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Manage toggle commands for your wand";
    }

    @Override
    @NotNull
    public String getUsage() {
        return "toggle [command] [on|off]";
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        Player player = context.requirePlayer();
        ItemStack wand = player.getInventory().getItemInMainHand();

        if (!context.wandService().isWand(wand)) {
            throw new CommandException("You must be holding a wand to use this command.");
        }

        // If no arguments, show current toggle commands
        if (context.args().length <= 1) {
            showCurrentToggleCommands(context, wand);
            return;
        }

        // Get the command argument
        String toggleCommand = context.getArg(1).toLowerCase();

        // Validate command name
        if (!isValidToggleCommand(toggleCommand)) {
            throw new CommandException("Invalid toggle command: " + toggleCommand +
                    ". Valid toggle commands: kajcloud, mephicloud, shadowcloak");
        }

        // If only command specified, show current status
        if (context.args().length == 2) {
            WandSettings settings = new WandSettings(wand);
            boolean enabled = settings.isToggleCommandEnabled(toggleCommand);
            context.sendMessage(Component.text("Toggle command '" + toggleCommand + "' is currently " +
                    (enabled ? "enabled" : "disabled")).color(NamedTextColor.YELLOW));
            context.sendMessage(Component.text("Use '/ew toggle " + toggleCommand + " on|off' to change it.")
                    .color(NamedTextColor.GRAY));
            return;
        }

        // Get the on/off argument
        String state = context.getArg(2).toLowerCase();

        // Validate state
        boolean enable = switch (state) {
            case "on", "enable", "true" -> true;
            case "off", "disable", "false" -> false;
            default -> throw new CommandException("Invalid state: " + state +
                    ". Use 'on' or 'off' to enable or disable the toggle command.");
        };

        // Count currently enabled toggle commands
        WandSettings settings = new WandSettings(wand);
        Map<String, Boolean> toggleCommands = settings.getToggleCommands();
        long enabledCount = toggleCommands.values().stream().filter(Boolean::booleanValue).count();

        // If enabling and we're at the limit, show error
        if (enable && !settings.isToggleCommandEnabled(toggleCommand) && enabledCount >= 3) {
            throw new CommandException("You can only have up to 3 toggle commands enabled at once. " +
                    "Disable another toggle command first.");
        }

        // Set the toggle command state
        settings.setToggleCommandEnabled(toggleCommand, enable);

        context.sendMessage(Component.text("Toggle command '" + toggleCommand + "' " +
                (enable ? "enabled" : "disabled")).color(NamedTextColor.GREEN));
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            String arg = context.getArgOrNull(1);
            final String searchArg = (arg == null ? "" : arg).toLowerCase();

            // Return matching toggle commands
            List<String> commands = Arrays.asList("kajcloud", "mephicloud", "shadowcloak");

            return commands.stream()
                    .filter(command -> command.startsWith(searchArg))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (context.args().length == 3) {
            String arg = context.getArgOrNull(2);
            final String searchArg = (arg == null ? "" : arg).toLowerCase();

            // Return on/off options
            List<String> options = Arrays.asList("on", "off");

            return options.stream()
                    .filter(option -> option.startsWith(searchArg))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private void showCurrentToggleCommands(CommandContext context, ItemStack wand) {
        WandSettings settings = new WandSettings(wand);
        Map<String, Boolean> toggleCommands = settings.getToggleCommands();

        context.sendMessage(Component.text("Toggle commands for your wand:").color(NamedTextColor.YELLOW));

        if (toggleCommands.isEmpty()) {
            context.sendMessage(Component.text("  No toggle commands enabled.").color(NamedTextColor.GRAY));
        } else {
            for (Map.Entry<String, Boolean> entry : toggleCommands.entrySet()) {
                context.sendMessage(Component.text("  " + entry.getKey() + ": " +
                        (entry.getValue() ? "enabled" : "disabled"))
                        .color(entry.getValue() ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
        }

        context.sendMessage(Component.text(""));
        context.sendMessage(Component.text("Use '/ew toggle [command] [on|off]' to manage toggle commands.")
                .color(NamedTextColor.GRAY));
        context.sendMessage(Component.text("Maximum of 3 toggle commands can be enabled at once.")
                .color(NamedTextColor.GRAY));
    }

    private boolean isValidToggleCommand(String command) {
        return command.equals("kajcloud") ||
                command.equals("mephicloud") ||
                command.equals("shadowcloak");
    }
}