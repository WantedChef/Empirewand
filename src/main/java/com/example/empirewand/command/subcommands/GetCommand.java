package com.example.empirewand.command.subcommands;

import com.example.empirewand.command.framework.CommandContext;
import com.example.empirewand.command.framework.CommandException;
import com.example.empirewand.command.framework.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Command to give a player a new wand.
 */
public class GetCommand implements SubCommand {

    private final String wandType;
    private final String displayName;
    private final Material material;

    public GetCommand(String wandType, String displayName, Material material) {
        this.wandType = wandType;
        this.displayName = displayName;
        this.material = material;
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
        Player player = context.requirePlayer();

        ItemStack wand = new ItemStack(material);
        var meta = wand.getItemMeta();
        meta.displayName(Component.text(displayName).color(NamedTextColor.GOLD));
        wand.setItemMeta(meta);

        context.wandService().giveWand(player);
        context.sendMessage(Component.text("You have received a " + displayName + "!")
                .color(NamedTextColor.GREEN));
    }
}