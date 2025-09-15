package nl.wantedchef.empirewand.command.wand;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command to give a player a new wand.
 * Enhanced with better error handling, examples, and performance monitoring.
 */
public class GetCommand implements SubCommand, CommandHelpProvider.HelpAwareCommand {

    private final String wandType;
    private final String displayName;

    public GetCommand(String wandType, String displayName, Material material) {
        this.wandType = wandType;
        this.displayName = displayName;
    }

    @Override
    public @NotNull String getName() {
        return "get";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.get";
    }

    @Override
    public @NotNull String getUsage() {
        return "get";
    }

    @Override
    public @NotNull String getDescription() {
        return "Get a new " + displayName;
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        // Start timing for performance monitoring
        try (var timing = context.startTiming("wand.get")) {
            Player player = context.requirePlayer();

            // Give appropriate wand based on type
            if (wandType.equals("mephidanteszeist")) {
                context.wandService().giveMephidantesZeist(player);
            } else {
                context.wandService().giveWand(player);
            }
            
            context.sendMessage(Component.text("You have received a " + displayName + "!")
                    .color(NamedTextColor.GREEN));
        } catch (Exception e) {
            throw new CommandException("Failed to give wand: " + e.getMessage(), e, "WAND_GIVE_FAILED");
        }
    }

    @Override
    public @NotNull List<CommandHelpProvider.CommandExample> getExamples() {
        return List.of(
            new CommandHelpProvider.CommandExample("get", "Receive a new " + displayName)
        );
    }
}
