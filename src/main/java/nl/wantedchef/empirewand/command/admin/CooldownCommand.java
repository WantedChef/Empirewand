package nl.wantedchef.empirewand.command.admin;

import nl.wantedchef.empirewand.framework.command.BaseWandCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CooldownCommand extends BaseWandCommand {
    @Override
    protected boolean execute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return true;
    }
}
