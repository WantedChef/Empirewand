package com.example.empirewand.core.services;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Permission helper for readable checks.
 */
public interface PermissionService {
    boolean has(CommandSender sender, String node);

    boolean canUseSpell(Player player, String spellKey);

    boolean canBindSpell(Player player, String spellKey);

    String getSpellUsePermission(String spellKey);

    String getSpellBindPermission(String spellKey);

    String getCommandPermission(String command);
}
