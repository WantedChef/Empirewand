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
import java.util.Locale;
import java.util.Set;

/**
 * Command to bind all spells in a category.
 */
public class BindCategoryCommand implements SubCommand {

    private final String wandType;

    public BindCategoryCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "bindcat";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.bindcat";
    }

    @Override
    public @NotNull String getUsage() {
        return "bindcat <category>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Bind all spells in a category";
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

        String cat = context.getArg(1).toLowerCase(Locale.ROOT);
        Set<String> cats = context.config().getCategoryNames();
        if (!cats.contains(cat)) {
            throw new CommandException("Unknown category. Available: " + String.join(", ", cats));
        }

        List<String> toBind = context.config().getCategorySpells(cat);
        if (toBind.isEmpty()) {
            throw new CommandException("Category '" + cat + "' has no spells");
        }

        List<String> current = context.wandService().getSpells(item);
        for (String k : toBind) {
            if (context.spellRegistry().isSpellRegistered(k) && !current.contains(k)) {
                current.add(k);
            }
        }
        context.wandService().setSpells(item, current);

        context.sendMessage(Component.text("Bound category '" + cat + "' spells to your wand")
                .color(NamedTextColor.GREEN));
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            String partial = context.args()[1].toLowerCase();
            return context.config().getCategoryNames().stream()
                    .filter(cat -> cat.startsWith(partial))
                    .toList();
        }
        return List.of();
    }
}