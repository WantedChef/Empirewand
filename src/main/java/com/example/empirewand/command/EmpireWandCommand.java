package com.example.empirewand.command;

import org.bukkit.Material;

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
        register(new ToggleCommand("empirewand"));

        // Advanced binding commands
        register(new BindTypeCommand("empirewand"));
        register(new BindCategoryCommand("empirewand"));

        // Information commands
        register(new SpellsCommand("empirewand"));

        // System commands
        register(new ReloadCommand("empirewand"));
        register(new MigrateCommand("empirewand"));

        // Cooldown management
        register(new CooldownCommand("empirewand"));
    }
}