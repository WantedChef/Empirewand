package com.example.empirewand.command.subcommands;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.spell.toggle.SpellManager;
import com.example.empirewand.command.framework.CommandContext;
import com.example.empirewand.command.framework.CommandException;
import com.example.empirewand.command.framework.SubCommand;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command to toggle a spell on/off.
 */
public class ToggleCommand implements SubCommand {

    private final String wandType;

    public ToggleCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "toggle";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.toggle";
    }

    @Override
    public @NotNull String getUsage() {
        return "toggle <spell>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Toggle a spell on or off";
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
            throw new CommandException("You must be holding a wand to toggle a spell");
        }

        String spellKey = context.getArg(1).toLowerCase();

        // Check if spell exists
        var spellOpt = context.spellRegistry().getSpell(spellKey);
        if (spellOpt.isEmpty()) {
            throw new CommandException("Unknown spell: " + spellKey);
        }

        Spell<?> spell = spellOpt.get();

        // Check if spell is toggleable
        if (!context.spellRegistry().isToggleableSpell(spellKey)) {
            throw new CommandException("Spell '" + spellKey + "' is not toggleable");
        }

        // Check if spell is bound to the wand
        List<String> boundSpells = context.wandService().getSpells(item);
        if (!boundSpells.contains(spellKey)) {
            throw new CommandException(
                    "Spell '" + spellKey + "' is not bound to your wand. Use /ew bind " + spellKey + " first.");
        }

        // Check permissions
        if (!player.hasPermission("empirewand.spell." + spellKey)) {
            throw new CommandException("You don't have permission to use this spell");
        }

        // Create spell context
        SpellContext spellContext = new SpellContext(context.plugin(), player,
                context.plugin().getConfigService(), context.plugin().getFxService());

        // Get spell manager and toggle the spell
        SpellManager spellManager = EmpireWandAPI.getService(SpellManager.class);
        boolean wasToggled = spellManager.toggleSpell(player, spell, spellContext);

        if (!wasToggled) {
            throw new CommandException("Failed to toggle spell. Please try again.");
        }

        // Determine if spell is now active or inactive
        boolean isActive = spellManager.isSpellActive(player, spell);
        String displayName = context.config().getSpellsConfig().getString(spellKey + ".display-name", spellKey);
        displayName = context.plugin().getTextService().stripMiniTags(displayName);

        if (isActive) {
            context.sendMessage(Component.text("Activated spell: ").color(NamedTextColor.GREEN)
                    .append(Component.text(displayName).color(NamedTextColor.AQUA)));
        } else {
            context.sendMessage(Component.text("Deactivated spell: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(displayName).color(NamedTextColor.GRAY)));
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            // Return toggleable spells that are bound to the player's wand
            Player player = context.asPlayer();
            if (player != null) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (context.wandService().isWand(item) || context.wandService().isMephidantesZeist(item)) {
                    List<String> boundSpells = context.wandService().getSpells(item);
                    String partial = context.args()[1].toLowerCase();
                    return boundSpells.stream()
                            .filter(spell -> context.spellRegistry().isToggleableSpell(spell))
                            .filter(spell -> spell.toLowerCase().startsWith(partial))
                            .toList();
                }
            }
        }
        return List.of();
    }
}