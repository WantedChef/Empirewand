package nl.wantedchef.empirewand.core.services;

import nl.wantedchef.empirewand.api.EmpireWandService;
import nl.wantedchef.empirewand.api.ServiceHealth;
import nl.wantedchef.empirewand.api.Version;
import nl.wantedchef.empirewand.api.service.PermissionService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * The primary implementation of the
 * {@link nl.wantedchef.empirewand.api.service.PermissionService}.
 * This class provides concrete implementations for permission checks related to
 * spells and commands.
 */
public class PermissionServiceImpl implements PermissionService, EmpireWandService {

    @Override
    public boolean has(CommandSender sender, String node) {
        if (sender == null || node == null || node.trim().isEmpty()) {
            return false;
        }
        try {
            return sender.hasPermission(node);
        } catch (Exception e) {
            // Log error but don't crash - permission checks should never break
            // functionality
            return false;
        }
    }

    @Override
    public boolean canUseSpell(Player player, String spellKey) {
        if (player == null || spellKey == null || spellKey.trim().isEmpty()) {
            return false;
        }
        try {
            return has(player, getSpellUsePermission(spellKey));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean canBindSpell(Player player, String spellKey) {
        if (player == null || spellKey == null || spellKey.trim().isEmpty()) {
            return false;
        }
        try {
            return has(player, getSpellBindPermission(spellKey));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getSpellUsePermission(String spellKey) {
        if (spellKey == null || spellKey.trim().isEmpty()) {
            return "empirewand.spell.use.unknown";
        }
        try {
            return "empirewand.spell.use." + spellKey.toLowerCase();
        } catch (Exception e) {
            return "empirewand.spell.use.unknown";
        }
    }

    @Override
    public String getSpellBindPermission(String spellKey) {
        if (spellKey == null || spellKey.trim().isEmpty()) {
            return "empirewand.spell.bind.unknown";
        }
        try {
            return "empirewand.spell.bind." + spellKey.toLowerCase();
        } catch (Exception e) {
            return "empirewand.spell.bind.unknown";
        }
    }

    @Override
    public String getCommandPermission(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "empirewand.command.unknown";
        }
        try {
            return "empirewand.command." + command.toLowerCase();
        } catch (Exception e) {
            return "empirewand.command.unknown";
        }
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
        try {
            // Basic health check - verify we can create permission strings
            String testPermission = getSpellUsePermission("test");
            if (testPermission == null || testPermission.isEmpty()) {
                return ServiceHealth.UNHEALTHY;
            }
            return ServiceHealth.HEALTHY;
        } catch (Exception e) {
            return ServiceHealth.UNHEALTHY;
        }
    }

    @Override
    public void reload() {
        // Permission service doesn't need reloading
    }
}
