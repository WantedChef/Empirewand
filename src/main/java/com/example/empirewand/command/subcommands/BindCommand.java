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
 * Command to bind a spell to a wand.
 */
public class BindCommand implements SubCommand {

    private final String wandType;

    public BindCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "bind";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.bind";
    }

    @Override
    public @NotNull String getUsage() {
        return "bind <spell>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Bind a spell to your wand";
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
            throw new CommandException("You must be holding a wand to bind a spell");
        }

        String spellKey = context.getArg(1).toLowerCase();

        if (context.spellRegistry().getSpell(spellKey) == null) {
            throw new CommandException("Unknown spell: " + spellKey);
        }

        // Check per-spell bind permission
        String spellPermission = wandType + ".spell.bind." + spellKey;
        if (!context.hasPermission(spellPermission)) {
            throw new CommandException("No permission to bind this spell");
        }

        List<String> spells = context.wandService().getSpells(item);
        if (spells.contains(spellKey)) {
            throw new CommandException("This spell is already bound to your wand");
        }

        spells.add(spellKey);
        context.wandService().setSpells(item, spells);

        context.sendMessage(Component.text("Bound spell " + spellKey + " to your wand")
                .color(NamedTextColor.GREEN));
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            String partial = context.args()[1].toLowerCase();
            return context.spellRegistry().getAllSpells().keySet().stream()
                    .filter(spell -> spell.startsWith(partial))
                    .filter(spell -> context.hasPermission(wandType + ".spell.bind." + spell))
                    .toList();
        }
        return List.of();
    }
}