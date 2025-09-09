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

import java.util.ArrayList;
import java.util.List;

/**
 * Command to unbind a spell from a wand.
 */
public class UnbindCommand implements SubCommand {

    private final String wandType;

    public UnbindCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "unbind";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.unbind";
    }

    @Override
    public @NotNull String getUsage() {
        return "unbind <spell>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Unbind a spell from your wand";
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
            throw new CommandException("You must be holding a wand to unbind a spell");
        }

        String spellKey = context.getArg(1).toLowerCase();
        List<String> spells = new ArrayList<>(context.wandService().getSpells(item));

        if (!spells.contains(spellKey)) {
            throw new CommandException("That spell is not bound to your wand");
        }

        spells.remove(spellKey);
        context.wandService().setSpells(item, spells);

        context.sendMessage(Component.text("Unbound spell " + spellKey + " from your wand")
                .color(NamedTextColor.GREEN));
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            Player player = context.asPlayer();
            if (player != null) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (context.wandService().isWand(item) || context.wandService().isMephidantesZeist(item)) {
                    String partial = context.args()[1].toLowerCase();
                    return context.wandService().getSpells(item).stream()
                            .filter(spell -> spell.startsWith(partial))
                            .toList();
                }
            }
        }
        return List.of();
    }
}
