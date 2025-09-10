package nl.wantedchef.empirewand.command;

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
import org.bukkit.Material;

/**
 * Main command handler for Mephidantes Zeist Wands (/mz).
 * Built on the new enterprise command framework.
 */
public class MephidantesZeistCommand extends BaseWandCommand {

    public MephidantesZeistCommand(EmpireWandPlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getPermissionPrefix() {
        return "mephidanteszeist";
    }

    @Override
    protected String getWandDisplayName() {
        return "Mephidantes Zeist Wand";
    }

    @Override
    protected void registerSubcommands() {
        // Core wand management commands
        register(new GetCommand("mephidanteszeist", "Mephidantes Zeist Wand", Material.NETHERITE_HOE));
        register(new BindCommand("mephidanteszeist"));
        register(new UnbindCommand("mephidanteszeist"));
        register(new BindAllCommand("mephidanteszeist"));
        register(new ListCommand("mephidanteszeist"));
        register(new SetSpellCommand("mephidanteszeist"));
        register(new ToggleCommand());
        register(new SwitchEffectCommand(plugin));

        // Advanced binding commands
        register(new BindTypeCommand("mephidanteszeist"));
        register(new BindCategoryCommand("mephidanteszeist"));

        // Information commands
        register(new SpellsCommand("mephidanteszeist"));
        register(new StatsCommand("mephidanteszeist"));

        // System commands
        register(new ReloadCommand("mephidanteszeist"));
        register(new MigrateCommand("mephidanteszeist"));

        // Cooldown management
        register(new CooldownCommand("mephidanteszeist"));
    }
}
