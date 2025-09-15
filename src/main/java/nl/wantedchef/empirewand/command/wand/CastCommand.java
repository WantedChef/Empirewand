package nl.wantedchef.empirewand.command.wand;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Command to cast a spell directly without having to select it first.
 */
public class CastCommand implements SubCommand {

    private final String wandType;

    public CastCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "cast";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.cast";
    }

    @Override
    public @NotNull String getUsage() {
        return "cast <spell>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Cast a spell directly without selecting it first";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        Player player = context.requirePlayer();
        String spellKey = context.getArg(1).toLowerCase();

        // Check if player is holding a wand
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean hasWand = context.wandService().isWand(item) || context.wandService().isMephidantesZeist(item);

        if (!hasWand) {
            throw new CommandException("You must be holding a wand to cast spells");
        }

        // Check if the spell is bound to the wand
        List<String> boundSpells = context.wandService().getSpells(item);
        if (!boundSpells.contains(spellKey)) {
            throw new CommandException("That spell is not bound to your wand. Use '/ew list' to see available spells.");
        }

        // Get the spell from registry
        SpellRegistry registry = context.plugin().getSpellRegistry();
        Optional<Spell<?>> spellOpt = registry.getSpell(spellKey);
        if (spellOpt.isEmpty()) {
            throw new CommandException("Unknown spell: " + spellKey);
        }

        Spell<?> spell = spellOpt.get();

        // Check permissions
        var perms = context.plugin().getPermissionService();
        if (!perms.has(player, perms.getSpellUsePermission(spellKey))) {
            throw new CommandException("You don't have permission to use this spell");
        }

        // Check cooldown using the spell's actual key
        String actualSpellKey = spell.key(); // Use the spell's actual key for cooldowns!
        long nowTicks = player.getWorld().getFullTime();
        var spellsCfg = context.plugin().getConfigService().getSpellsConfig();
        int cdTicks = Math.max(0, spellsCfg.getInt(spellKey + ".cooldown-ticks", 40));

        var cooldownManager = context.cooldownManager();
        if (cooldownManager.isSpellOnCooldown(player.getUniqueId(), actualSpellKey, nowTicks, item)) {
            long remaining = cooldownManager.getSpellCooldownRemaining(player.getUniqueId(), actualSpellKey, nowTicks, item);
            throw new CommandException("Spell is on cooldown for " + (remaining / 20) + " seconds");
        }

        // Create spell context and cast
        FxService fx = context.plugin().getFxService();
        SpellContext spellCtx = new SpellContext(context.plugin(), player, context.plugin().getConfigService(), fx);

        try {
            // Final online check
            if (!player.isOnline()) {
                return;
            }

            if (!spell.canCast(spellCtx)) {
                throw new CommandException("Cannot cast this spell right now");
            }

            // Cast the spell
            long start = System.nanoTime();
            spell.cast(spellCtx);
            cooldownManager.setSpellCooldown(player.getUniqueId(), actualSpellKey, nowTicks + cdTicks);

            // Show success message
            if (player.isOnline()) {
                var params = new HashMap<String, String>();
                params.put("spell", registry.getSpellDisplayName(spellKey));
                fx.showSuccess(player, "spell-cast", params);
            }

            context.plugin().getMetricsService().recordSpellCast(spellKey, (System.nanoTime() - start) / 1_000_000);

        } catch (Throwable t) {
            if (player.isOnline()) {
                fx.showError(player, "wand.cast-error");
            }
            context.plugin().getLogger()
                    .warning(String.format("Spell cast error for '%s': %s", spellKey, t.getMessage()));
            context.plugin().getMetricsService().recordFailedCast();
            throw new CommandException("Failed to cast spell: " + t.getMessage());
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            Player player = context.asPlayer();
            if (player != null) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (context.wandService().isWand(item) || context.wandService().isMephidantesZeist(item)) {
                    String partial = context.args()[1].toLowerCase();
                    return context.wandService().getSpells(item).stream()
                            .filter(spell -> spell.startsWith(partial))
                            .toList();
                }
            }
        }
        return List.of();
    }
}
