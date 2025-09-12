package nl.wantedchef.empirewand.command.wand;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.SpellTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Command to bind all spells of a specific type.
 */
public class BindTypeCommand implements SubCommand {

    private final String wandType;

    public BindTypeCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "bindtype";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.bindtype";
    }

    @Override
    public @NotNull String getUsage() {
        return "bindtype <type>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Bind all spells of a specific type";
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

        String typeArg = context.getArg(1).toLowerCase(Locale.ROOT);
        List<String> valid = SpellTypes.validTypeNames();
        if (!valid.contains(typeArg)) {
            throw new CommandException("Unknown type. Valid: " + String.join(", ", valid));
        }

        var all = new ArrayList<>(context.spellRegistry().getAllSpells().keySet());
        List<String> toBind = new ArrayList<>();
        for (String key : all) {
            SpellType t = null;
            try {
                var spellOpt = context.spellRegistry().getSpell(key);
                if (spellOpt.isPresent()) {
                    t = spellOpt.get().type();
                }
            } catch (Exception ignored) { }
            if (t == null) {
                t = SpellTypes.resolveTypeFromKey(key);
            }
            if (t != null && t.name().equalsIgnoreCase(typeArg)) {
                toBind.add(key);
            }
        }

        if (toBind.isEmpty()) {
            throw new CommandException("No spells of type '" + typeArg + "' found");
        }

        // Replace problematic inference with explicit generic and null-safe handling:
        var spells = context.wandService().getSpells(item);
        List<String> current = new ArrayList<>(spells);

        for (String k : toBind) {
            if (!current.contains(k)) {
                current.add(k);
            }
        }
        context.wandService().setSpells(item, current);

        context.sendMessage(Component.text("Bound type '" + typeArg + "' spells to your wand")
                .color(NamedTextColor.GREEN));
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





