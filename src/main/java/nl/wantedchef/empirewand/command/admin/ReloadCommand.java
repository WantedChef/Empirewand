package nl.wantedchef.empirewand.command.admin;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Command to reload plugin configuration.
 */
public class ReloadCommand implements SubCommand {

    private final String wandType;

    public ReloadCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "reload";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.reload";
    }

    @Override
    public @NotNull String getUsage() {
        return "reload";
    }

    @Override
    public @NotNull String getDescription() {
        return "Reload plugin configuration";
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        context.config().loadConfigs();
        context.sendMessage(Component.text("Configuration reloaded").color(NamedTextColor.GREEN));
    }
}
