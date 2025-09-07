package com.example.empirewand.command.subcommands;

import com.example.empirewand.command.framework.CommandContext;
import com.example.empirewand.command.framework.CommandException;
import com.example.empirewand.command.framework.SubCommand;
import com.example.empirewand.spell.SpellType;
import com.example.empirewand.spell.SpellTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to list all available spells.
 */
public class SpellsCommand implements SubCommand {

    private final String wandType;

    public SpellsCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "spells";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.list";
    }

    @Override
    public @NotNull String getUsage() {
        return "spells [type]";
    }

    @Override
    public @NotNull String getDescription() {
        return "List all available spells, optionally filtered by type";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        context.requirePlayer(); // Just to ensure it's a player

        if (context.args().length > 1) {
            String typeArg = context.getArgOrNull(1);
            if (typeArg != null) {
                typeArg = typeArg.toLowerCase();
                List<String> valid = SpellTypes.validTypeNames();
                if (!valid.contains(typeArg)) {
                    throw new CommandException("Unknown type. Valid: " + String.join(", ", valid));
                }

                var all = new ArrayList<>(context.spellRegistry().getAllSpells().keySet());
                List<String> filtered = new ArrayList<>();
                for (String key : all) {
                    SpellType t = SpellTypes.resolveTypeFromKey(key);
                    if (t.name().equalsIgnoreCase(typeArg)) {
                        filtered.add(key);
                    }
                }

                if (filtered.isEmpty()) {
                    throw new CommandException("No spells of type '" + typeArg + "' found");
                }

                context.sendMessage(Component.text("Spells of type '" + typeArg + "':")
                        .color(NamedTextColor.GREEN));
                for (String key : filtered) {
                    String display = context.spellRegistry().getSpellDisplayName(key);
                    context.sendMessage(Component.text(" - " + display + " (" + key + ")")
                            .color(NamedTextColor.GRAY));
                }
            }
        } else {
            // List all spells
            var all = context.spellRegistry().getAllSpells();
            context.sendMessage(Component.text("All available spells:").color(NamedTextColor.GREEN));
            for (var entry : all.entrySet()) {
                String key = entry.getKey();
                String display = context.spellRegistry().getSpellDisplayName(key);
                SpellType type = SpellTypes.resolveTypeFromKey(key);
                context.sendMessage(
                        Component.text(" - " + display + " (" + key + ") [" + type.name().toLowerCase() + "]")
                                .color(NamedTextColor.GRAY));
            }
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            String partial = context.args()[1].toLowerCase();
            return SpellTypes.validTypeNames().stream()
                    .filter(type -> type.startsWith(partial))
                    .toList();
        }
        return List.of();
    }
}