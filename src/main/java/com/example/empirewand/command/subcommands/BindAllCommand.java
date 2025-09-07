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

import java.util.ArrayList;

/**
 * Command to bind all available spells to a wand.
 */
public class BindAllCommand implements SubCommand {

    private final String wandType;

    public BindAllCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "bindall";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.bindall";
    }

    @Override
    public @NotNull String getUsage() {
        return "bindall";
    }

    @Override
    public @NotNull String getDescription() {
        return "Bind all available spells to your wand";
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

        context.wandService().setSpells(item,
                new ArrayList<>(context.spellRegistry().getAllSpells().keySet()));

        context.sendMessage(Component.text("Bound all available spells to your wand")
                .color(NamedTextColor.GREEN));
    }
}