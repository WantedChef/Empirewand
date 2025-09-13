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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        try (var timing = context.startTiming("spells.list")) {
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

                    // Enhanced spell list display with modern formatting
                    Component header = Component.text()
                        .append(Component.text("✨ ", NamedTextColor.GOLD))
                        .append(Component.text("Spells of type '", NamedTextColor.WHITE))
                        .append(Component.text(typeArg, NamedTextColor.AQUA))
                        .append(Component.text("' (", NamedTextColor.WHITE))
                        .append(Component.text(String.valueOf(filtered.size()), NamedTextColor.YELLOW))
                        .append(Component.text(" found):", NamedTextColor.WHITE))
                        .build();
                    
                    context.sendMessage(header);
                    
                    // Sort spells alphabetically for better UX
                    filtered.sort(String::compareToIgnoreCase);
                    
                    for (String key : filtered) {
                        String display = context.spellRegistry().getSpellDisplayName(key);
                        Component spellEntry = Component.text()
                            .append(Component.text("  ▶ ", NamedTextColor.GREEN))
                            .append(Component.text(display, NamedTextColor.YELLOW))
                            .append(Component.text(" (", NamedTextColor.GRAY))
                            .append(Component.text(key, NamedTextColor.DARK_GRAY))
                            .append(Component.text(")", NamedTextColor.GRAY))
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                Component.text("Click to bind this spell")
                                    .append(Component.newline())
                                    .append(Component.text("/" + wandType + " bind " + key, NamedTextColor.GRAY))))
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(
                                "/" + wandType + " bind " + key))
                            .build();
                        
                        context.sendMessage(spellEntry);
                    }
                }
            } else {
                // Enhanced all spells display with categorization
                var all = context.spellRegistry().getAllSpells();
                
                Component header = Component.text()
                    .append(Component.text("📖 ", NamedTextColor.GOLD))
                    .append(Component.text("All Available Spells (", NamedTextColor.WHITE))
                    .append(Component.text(String.valueOf(all.size()), NamedTextColor.YELLOW))
                    .append(Component.text(" total)", NamedTextColor.WHITE))
                    .build();
                
                context.sendMessage(header);
                
                // Group spells by type for better organization
                Map<SpellType, List<String>> spellsByType = new HashMap<>();
                for (var entry : all.entrySet()) {
                    String key = entry.getKey();
                    SpellType type = SpellTypes.resolveTypeFromKey(key);
                    spellsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(key);
                }
                
                // Sort types and display
                spellsByType.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey((a, b) -> a.name().compareToIgnoreCase(b.name())))
                    .forEach(typeEntry -> {
                        SpellType type = typeEntry.getKey();
                        List<String> spells = typeEntry.getValue();
                        spells.sort(String::compareToIgnoreCase);
                        
                        // Type header
                        Component typeHeader = Component.text()
                            .append(Component.text("\n🔮 ", getTypeColor(type)))
                            .append(Component.text(type.name().toUpperCase(), getTypeColor(type)))
                            .append(Component.text(" (", NamedTextColor.GRAY))
                            .append(Component.text(String.valueOf(spells.size()), NamedTextColor.WHITE))
                            .append(Component.text(" spells)", NamedTextColor.GRAY))
                            .build();
                        
                        context.sendMessage(typeHeader);
                        
                        // Spells in this type
                        for (String key : spells) {
                            String display = context.spellRegistry().getSpellDisplayName(key);
                            Component spellEntry = Component.text()
                                .append(Component.text("    • ", NamedTextColor.DARK_GRAY))
                                .append(Component.text(display, NamedTextColor.WHITE))
                                .append(Component.text(" (", NamedTextColor.GRAY))
                                .append(Component.text(key, NamedTextColor.DARK_GRAY))
                                .append(Component.text(")", NamedTextColor.GRAY))
                                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                    Component.text("Click to bind this spell")
                                        .append(Component.newline())
                                        .append(Component.text("/" + wandType + " bind " + key, NamedTextColor.GRAY))))
                                .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(
                                    "/" + wandType + " bind " + key))
                                .build();
                            
                            context.sendMessage(spellEntry);
                        }
                    });
                    
                // Footer with helpful tips
                Component footer = Component.text()
                    .append(Component.text("\n💡 Tip: ", NamedTextColor.YELLOW))
                    .append(Component.text("Use ", NamedTextColor.GRAY))
                    .append(Component.text("/" + wandType + " spells <type>", NamedTextColor.AQUA)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/" + wandType + " spells "))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            Component.text("Filter spells by type"))))
                    .append(Component.text(" to filter by spell type", NamedTextColor.GRAY))
                    .build();
                
                context.sendMessage(footer);
            }
        }
    }
    
    /**
     * Gets the color associated with a spell type for visual distinction.
     */
    private NamedTextColor getTypeColor(SpellType type) {
        return switch (type.name().toLowerCase()) {
            case "fire" -> NamedTextColor.RED;
            case "water", "ice" -> NamedTextColor.BLUE;
            case "earth" -> NamedTextColor.GREEN;
            case "air", "lightning" -> NamedTextColor.YELLOW;
            case "dark", "poison" -> NamedTextColor.DARK_PURPLE;
            case "light", "heal" -> NamedTextColor.WHITE;
            case "enhanced" -> NamedTextColor.GOLD;
            case "utility" -> NamedTextColor.GRAY;
            case "combat" -> NamedTextColor.DARK_RED;
            default -> NamedTextColor.AQUA;
        };
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
