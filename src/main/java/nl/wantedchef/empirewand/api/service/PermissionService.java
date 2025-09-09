package nl.wantedchef.empirewand.api.service;

import nl.wantedchef.empirewand.api.EmpireWandService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Service for managing EmpireWand permissions.
 * Provides permission checking and management capabilities.
 *
 * @since 2.0.0
 */
public interface PermissionService extends EmpireWandService {

    /**
     * Checks if a command sender has a specific permission.
     *
     * @param sender     the command sender to check
     * @param permission the permission node
     * @return true if the sender has the permission, false otherwise
     */
    boolean has(@NotNull CommandSender sender, @NotNull String permission);

    /**
     * Checks if a player has permission to use a specific spell.
     *
     * @param player   the player to check
     * @param spellKey the spell key (kebab-case)
     * @return true if the player can use the spell, false otherwise
     */
    boolean canUseSpell(@NotNull Player player, @NotNull String spellKey);

    /**
     * Checks if a player has permission to bind a specific spell.
     *
     * @param player   the player to check
     * @param spellKey the spell key (kebab-case)
     * @return true if the player can bind the spell, false otherwise
     */
    boolean canBindSpell(@NotNull Player player, @NotNull String spellKey);

    /**
     * Gets the permission node for using a specific spell.
     *
     * @param spellKey the spell key (kebab-case)
     * @return the permission node (e.g., "empirewand.spell.use.magic-missile")
     */
    @NotNull
    String getSpellUsePermission(@NotNull String spellKey);

    /**
     * Gets the permission node for binding a specific spell.
     *
     * @param spellKey the spell key (kebab-case)
     * @return the permission node (e.g., "empirewand.spell.bind.magic-missile")
     */
    @NotNull
    String getSpellBindPermission(@NotNull String spellKey);

    /**
     * Gets the permission node for a command.
     *
     * @param command the command name
     * @return the permission node (e.g., "empirewand.command.give")
     */
    @NotNull
    String getCommandPermission(@NotNull String command);
}
