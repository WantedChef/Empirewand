package nl.wantedchef.empirewand.command.wand;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command to list all spells bound to a wand.
 */
public class ListCommand implements SubCommand {

    private final String wandType;

    public ListCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "list";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.list";
    }

    @Override
    public @NotNull String getUsage() {
        return "list";
    }

    @Override
    public @NotNull String getDescription() {
        return "List all spells bound to your wand";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        Player player = context.requirePlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!context.wandService().isWand(item) && !context.wandService().isMephidantesZeist(item)) {
            throw new CommandException("You must be holding a wand");
        }

        List<String> spells = context.wandService().getSpells(item);
        if (spells.isEmpty()) {
            context.sendMessage(Component.text("No spells bound").color(NamedTextColor.YELLOW));
            return;
        }

        int idx = context.wandService().getActiveIndex(item);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spells.size(); i++) {
            if (i > 0)
                sb.append(", ");
            String key = spells.get(i);
            String display = context.config().getSpellsConfig().getString(key + ".display-name", key);
            if (i == idx) {
                sb.append("[").append(context.plugin().getTextService().stripMiniTags(display)).append("]");
            } else {
                sb.append(context.plugin().getTextService().stripMiniTags(display));
            }
        }

        context.sendMessage(Component.text("Spells: ").color(NamedTextColor.GRAY)
                .append(Component.text(sb.toString()).color(NamedTextColor.AQUA)));
    }
}
