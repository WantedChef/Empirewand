package com.example.empirewand.command;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.command.framework.BaseWandCommand;
import com.example.empirewand.command.subcommands.BindAllCommand;
import com.example.empirewand.command.subcommands.BindCategoryCommand;
import com.example.empirewand.command.subcommands.BindCommand;
import com.example.empirewand.command.subcommands.BindTypeCommand;
import com.example.empirewand.command.subcommands.CooldownCommand;
import com.example.empirewand.command.subcommands.GetCommand;
import com.example.empirewand.command.subcommands.ListCommand;
import com.example.empirewand.command.subcommands.MigrateCommand;
import com.example.empirewand.command.subcommands.ReloadCommand;
import com.example.empirewand.command.subcommands.SetSpellCommand;
import com.example.empirewand.command.subcommands.SpellsCommand;
import com.example.empirewand.command.subcommands.ToggleCommand;
import com.example.empirewand.command.subcommands.UnbindCommand;
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
        register(new ToggleCommand("mephidanteszeist"));

        // Advanced binding commands
        register(new BindTypeCommand("mephidanteszeist"));
        register(new BindCategoryCommand("mephidanteszeist"));

        // Information commands
        register(new SpellsCommand("mephidanteszeist"));

        // System commands
        register(new ReloadCommand("mephidanteszeist"));
        register(new MigrateCommand("mephidanteszeist"));

        // Cooldown management
        register(new CooldownCommand("mephidanteszeist"));
    }
}
