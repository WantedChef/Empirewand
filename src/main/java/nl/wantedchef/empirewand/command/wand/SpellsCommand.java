package nl.wantedchef.empirewand.command.wand;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.SpellTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to list all available spells.
 * Enhanced with better argument parsing, examples, and performance monitoring.
 */
public class SpellsCommand implements SubCommand, CommandHelpProvider.HelpAwareCommand {

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
        // Start timing for performance monitoring
        var timing = context.startTiming("spells.list");
        try {
            context.requirePlayer(); // Just to ensure it's a player

            if (context.args().length > 1) {
                String typeArg = context.getArgOrNull(1);
                if (typeArg != null) {
                    typeArg = typeArg.toLowerCase();
                    List<String> valid = SpellTypes.validTypeNames();
                    if (!valid.contains(typeArg)) {
                        throw new CommandException("Unknown type. Valid: " + String.join(", ", valid), 
                            "SPELLS_INVALID_TYPE", typeArg, valid.toArray());
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
                        throw new CommandException("No spells of type '" + typeArg + "' found", 
                            "SPELLS_NO_SPELLS_FOUND", typeArg);
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
        } finally {
            timing.complete();
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

    @Override
    public @NotNull List<CommandHelpProvider.CommandExample> getExamples() {
        return List.of(
            new CommandHelpProvider.CommandExample("spells", "List all available spells"),
            new CommandHelpProvider.CommandExample("spells fire", "List all fire spells"),
            new CommandHelpProvider.CommandExample("spells utility", "List all utility spells")
        );
    }
}
