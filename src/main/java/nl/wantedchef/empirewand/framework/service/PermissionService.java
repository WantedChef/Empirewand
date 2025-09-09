package nl.wantedchef.empirewand.framework.service;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * A service for handling player permissions in a standardized way.
 * This interface provides methods for checking if a player has permission to use spells, bind spells, or execute commands.
 */
public interface PermissionService {
    /**
     * Checks if a command sender has a specific permission node.
     *
     * @param sender The command sender.
     * @param node   The permission node.
     * @return true if the sender has the permission, false otherwise.
     */
    boolean has(CommandSender sender, String node);

    /**
     * Checks if a player has permission to use a specific spell.
     *
     * @param player   The player.
     * @param spellKey The key of the spell.
     * @return true if the player can use the spell, false otherwise.
     */
    boolean canUseSpell(Player player, String spellKey);

    /**
     * Checks if a player has permission to bind a specific spell to a wand.
     *
     * @param player   The player.
     * @param spellKey The key of the spell.
     * @return true if the player can bind the spell, false otherwise.
     */
    boolean canBindSpell(Player player, String spellKey);

    /**
     * Gets the permission node required to use a specific spell.
     *
     * @param spellKey The key of the spell.
     * @return The permission node for using the spell.
     */
    String getSpellUsePermission(String spellKey);

    /**
     * Gets the permission node required to bind a specific spell.
     *
     * @param spellKey The key of the spell.
     * @return The permission node for binding the spell.
     */
    String getSpellBindPermission(String spellKey);

    /**
     * Gets the permission node required to use a specific command.
     *
     * @param command The name of the command.
     * @return The permission node for the command.
     */
    String getCommandPermission(String command);
}





