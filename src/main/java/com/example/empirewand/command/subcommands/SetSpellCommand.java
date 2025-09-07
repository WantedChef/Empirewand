package com.example.empirewand.command.subcommands;

import com.example.empirewand.command.framework.CommandContext;
import com.example.empirewand.command.framework.CommandException;
import com.example.empirewand.command.framework.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command to set the active spell on a wand.
 */
public class SetSpellCommand implements SubCommand {

    private final String wandType;

    public SetSpellCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "set-spell";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.set-spell";
    }

    @Override
    public @NotNull String getUsage() {
        return "set-spell <spell>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Set the active spell on your wand";
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

        String spellKey = context.getArg(1).toLowerCase();
        List<String> spells = context.wandService().getSpells(item);

        if (!spells.contains(spellKey)) {
            throw new CommandException("That spell is not bound to your wand");
        }

        int index = spells.indexOf(spellKey);
        context.wandService().setActiveIndex(item, index);
        String display = context.config().getSpellsConfig().getString(spellKey + ".display-name", spellKey);
        display = context.plugin().getTextService().stripMiniTags(display);

        context.sendMessage(Component.text("Active spell set to: ").color(NamedTextColor.GREEN)
                .append(Component.text(display).color(NamedTextColor.AQUA)));
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