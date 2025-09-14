package nl.wantedchef.empirewand.command.wand;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.core.wand.WandSettings;
import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.service.SpellSwitchService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SwitchEffectCommand implements SubCommand {

    private final EmpireWandPlugin plugin;
    private final String permissionPrefix;

    public SwitchEffectCommand(EmpireWandPlugin plugin) {
        this(plugin, "empirewand");
    }

    public SwitchEffectCommand(EmpireWandPlugin plugin, String permissionPrefix) {
        this.plugin = plugin;
        this.permissionPrefix = (permissionPrefix == null || permissionPrefix.isBlank()) ? "empirewand" : permissionPrefix;
    }

    @Override
    public @NotNull String getName() {
        return "switcheffect";
    }

    @Override
    public @Nullable String getPermission() {
        return permissionPrefix + ".command.switcheffect";
    }

    @Override
    public @NotNull String getUsage() {
        return "switcheffect <effect>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Change your wand's spell switch effect.";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        Player player = context.requirePlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!context.wandService().isWand(item)) {
            throw new CommandException("You must be holding a wand to use this command.");
        }

        if (context.args().length < 2) {
            throw new CommandException("You must specify an effect name. Use tab-complete to see options.");
        }

        String effectName = context.getArg(1).toLowerCase();
        SpellSwitchService switchService = new SpellSwitchService(this.plugin, context.wandService());
        Set<String> availableEffects = switchService.getAvailableEffects();

        // Special-case: allow "default" to clear any explicit setting
        boolean isDefault = effectName.equals("default");
        if (!isDefault && !availableEffects.contains(effectName)) {
            throw new CommandException("Invalid effect name. Valid effects are: " + String.join(", ", availableEffects));
        }

        WandSettings settings = new WandSettings(item);
        String current = settings.getSpellSwitchEffect();

        if (isDefault) {
            // Clear stored value to fall back to default
            settings.setSpellSwitchEffect(null);
            context.sendMessage(Component.text("Wand switch effect reset to default.").color(NamedTextColor.GREEN));
            return;
        }

        if (current != null && current.equalsIgnoreCase(effectName)) {
            context.sendMessage(Component.text("That switch effect is already active.").color(NamedTextColor.YELLOW));
            return;
        }

        settings.setSpellSwitchEffect(effectName);

        context.sendMessage(Component.text("Wand switch effect set to: ").color(NamedTextColor.GREEN)
                .append(Component.text(effectName).color(NamedTextColor.AQUA)));
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            Player player = context.asPlayer();
            if (player != null) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (context.wandService().isWand(item)) {
                    String partial = context.args()[1].toLowerCase();
                    SpellSwitchService switchService = new SpellSwitchService(this.plugin, context.wandService());
                    Set<String> effects = switchService.getAvailableEffects();

                    // Include "default" as a valid target
                    List<String> candidates = effects.stream().collect(Collectors.toList());
                    candidates.add("default");

                    // Prioritize current setting first when no partial filter
                    WandSettings settings = new WandSettings(item);
                    String current = settings.getSpellSwitchEffect();

                    return candidates.stream()
                            .filter(effect -> effect.toLowerCase().startsWith(partial))
                            .sorted((a, b) -> {
                                // Current first, otherwise alphabetical
                                boolean aCur = a.equalsIgnoreCase(current);
                                boolean bCur = b.equalsIgnoreCase(current);
                                if (aCur && !bCur) return -1;
                                if (bCur && !aCur) return 1;
                                return a.compareToIgnoreCase(b);
                            })
                            .toList();
                }
            }
        }
        return List.of();
    }
}
