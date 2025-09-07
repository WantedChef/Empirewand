package com.example.empirewand.core.services;

import com.example.empirewand.api.ServiceHealth;
import com.example.empirewand.api.Version;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Permission helper for readable checks. This is the primary implementation.
 */
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

    @Override
    public String getServiceName() {
        return "PermissionService";
    }

    @Override
    public Version getServiceVersion() {
        return Version.of(2, 0, 0);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public ServiceHealth getHealth() {
        return ServiceHealth.HEALTHY;
    }

    @Override
    public void reload() {
        // Permission service doesn't need reloading
    }
}
