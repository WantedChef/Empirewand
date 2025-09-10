package nl.wantedchef.empirewand.command.handlers.handlers;

import org.bukkit.Material;

import nl.wantedchef.empirewand.core.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.BaseWandCommand;
import nl.wantedchef.empirewand.command.wand.BindAllCommand;
import nl.wantedchef.empirewand.command.wand.BindCategoryCommand;
import nl.wantedchef.empirewand.command.wand.BindCommand;
import nl.wantedchef.empirewand.command.wand.BindTypeCommand;
import nl.wantedchef.empirewand.command.admin.CooldownCommand;
import nl.wantedchef.empirewand.command.wand.GetCommand;
import nl.wantedchef.empirewand.command.wand.ListCommand;
import nl.wantedchef.empirewand.command.admin.MigrateCommand;
import nl.wantedchef.empirewand.command.admin.ReloadCommand;
import nl.wantedchef.empirewand.command.wand.SetSpellCommand;
import nl.wantedchef.empirewand.command.wand.SpellsCommand;
import nl.wantedchef.empirewand.command.wand.StatsCommand;
import nl.wantedchef.empirewand.command.wand.ToggleCommand;
import nl.wantedchef.empirewand.command.wand.SwitchEffectCommand;
import nl.wantedchef.empirewand.command.wand.UnbindCommand;

/**
 * Main command handler for Empire Wands (/ew).
 * Built on the new enterprise command framework.
 */
public class EmpireWandCommand extends BaseWandCommand {

    public EmpireWandCommand(EmpireWandPlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getPermissionPrefix() {
        return "empirewand";
    }

    @Override
    protected String getWandDisplayName() {
        return "Empire Wand";
    }

    @Override
    protected void registerSubcommands() {
        // Core wand management commands
        register(new GetCommand("empirewand", "Empire Wand", Material.BLAZE_ROD));
        register(new BindCommand("empirewand"));
        register(new UnbindCommand("empirewand"));
        register(new BindAllCommand("empirewand"));
        register(new ListCommand("empirewand"));
        register(new SetSpellCommand("empirewand"));
        register(new ToggleCommand());
        register(new SwitchEffectCommand(plugin));

        // Advanced binding commands
        register(new BindTypeCommand("empirewand"));
        register(new BindCategoryCommand("empirewand"));

        // Information commands
        register(new SpellsCommand("empirewand"));
        register(new StatsCommand("empirewand"));

        // System commands
        register(new ReloadCommand("empirewand"));
        register(new MigrateCommand("empirewand"));

        // Cooldown management
        register(new CooldownCommand("empirewand"));
    }
}
