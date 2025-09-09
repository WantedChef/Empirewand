package nl.wantedchef.empirewand.command.wand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.wand.WandSettings;
import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.service.SpellSwitchService;

/**
 * Command to manage spell switch effects for wands.
 * Usage: /ew switcheffect [effect]
 * Usage: /mz switcheffect [effect]
 */
public class SwitchEffectCommand implements SubCommand {
    private final EmpireWandPlugin plugin;
    private final SpellSwitchService spellSwitchService;

    public SwitchEffectCommand(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.spellSwitchService = new SpellSwitchService(plugin);
    }

    @Override
    public @NotNull String getName() {
        return "switcheffect";
    }

    @Override
    public @Nullable String getPermission() {
        return "empirewand.command.switcheffect";
    }

    @Override
    public @NotNull String getUsage() {
        return "switcheffect [effect]";
    }

    @Override
    public @NotNull String getDescription() {
        return "Manage spell switch effects for your wand";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return Arrays.asList("switch", "effect", "se");
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        Player player = context.requirePlayer();
        ItemStack wand = player.getInventory().getItemInMainHand();

        if (!context.wandService().isWand(wand)) {
            throw new CommandException("You must be holding a wand to use this command.");
        }

        // If no arguments, show current effect and available options
        if (context.args().length <= 1) {
            showCurrentEffect(context, wand);
            return;
        }

        // Get the effect argument
        String effect = context.getArg(1).toLowerCase();

        // Check if it's the "list" command
        if (effect.equals("list")) {
            listAvailableEffects(context);
            return;
        }

        // Validate the effect
        if (!spellSwitchService.getAvailableEffects().contains(effect)) {
            throw new CommandException(
                    "Invalid effect: " + effect + ". Use '/ew switcheffect list' to see available effects.");
        }

        // Set the effect
        WandSettings settings = new WandSettings(wand);
        settings.setSpellSwitchEffect(effect);

        context.sendMessage(Component.text("Spell switch effect set to: " + effect)
                .color(NamedTextColor.GREEN));

        // Play the effect to demonstrate it
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Use the spellSwitchService directly instead of WandCastListener
                spellSwitchService.playSpellSwitchEffect(player, wand);
            }
        }, 10L);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            String arg = context.getArgOrNull(1);
            if (arg != null) {
                final String searchArg = arg.toLowerCase();

                // Return matching effects
                List<String> effects = new ArrayList<>(spellSwitchService.getAvailableEffects());
                effects.add("list"); // Add list option

                return effects.stream()
                        .filter(effect -> effect.startsWith(searchArg))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private void showCurrentEffect(CommandContext context, ItemStack wand) {
        WandSettings settings = new WandSettings(wand);
        String currentEffect = settings.getSpellSwitchEffect();

        context.sendMessage(Component.text("Current spell switch effect: " + currentEffect));
        context.sendMessage(Component.text("Use '/ew switcheffect list' to see available effects."));
        context.sendMessage(Component.text("Use '/ew switcheffect <effect>' to change it."));
    }

    private void listAvailableEffects(CommandContext context) {
        Set<String> effects = spellSwitchService.getAvailableEffects();

        context.sendMessage(Component.text("Available spell switch effects:"));
        for (String effect : effects.stream().sorted().collect(Collectors.toList())) {
            context.sendMessage(Component.text("  - " + effect));
        }

        context.sendMessage(Component.text(""));
        context.sendMessage(Component.text("Use '/ew switcheffect <effect>' to set an effect."));
    }
}