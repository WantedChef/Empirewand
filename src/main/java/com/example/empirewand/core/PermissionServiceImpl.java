package com.example.empirewand.core;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Internal implementation of the public PermissionService API.
 */
@SuppressFBWarnings(value = {
        "EI_EXPOSE_REP" }, justification = "Implementation is stateless and exposes no mutable internals.")
public class PermissionServiceImpl implements com.example.empirewand.api.PermissionService {

    @Override
    public boolean has(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    @Override
    public boolean canUseSpell(Player player, String spellKey) {
        return has(player, getSpellUsePermission(spellKey));
    }

    @Override
    public boolean canBindSpell(Player player, String spellKey) {
        return has(player, getSpellBindPermission(spellKey));
    }

    @Override
    public String getSpellUsePermission(String spellKey) {
        return "empirewand.spell.use." + spellKey;
    }

    @Override
    public String getSpellBindPermission(String spellKey) {
        return "empirewand.spell.bind." + spellKey;
    }

    @Override
    public String getCommandPermission(String command) {
        return "empirewand.command." + command;
    }
}
