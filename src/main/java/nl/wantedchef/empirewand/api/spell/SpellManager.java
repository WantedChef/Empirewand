package nl.wantedchef.empirewand.api.spell;

import nl.wantedchef.empirewand.api.EmpireWandService;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Service for managing toggleable spell states and lifecycle.
 * This service tracks which spells are active for which players and provides
 * methods for toggling, activating, and deactivating spells.
 * 
 * @since 2.1.0
 */
public interface SpellManager extends EmpireWandService {

    /**
     * Toggles a toggleable spell for the given player.
     * If the spell is not toggleable, this method has no effect.
     * 
     * @param player  the player to toggle the spell for
     * @param spell   the spell to toggle
     * @param context the spell casting context
     * @return {@code true} if the spell was toggled, {@code false} if the spell is
     *         not toggleable
     */
    boolean toggleSpell(@NotNull Player player, @NotNull Spell<?> spell, @NotNull SpellContext context);

    /**
     * Activates a toggleable spell for the given player.
     * If the spell is already active or not toggleable, this method has no effect.
     * 
     * @param player  the player to activate the spell for
     * @param spell   the spell to activate
     * @param context the spell casting context
     * @return {@code true} if the spell was activated, {@code false} otherwise
     */
    boolean activateSpell(@NotNull Player player, @NotNull Spell<?> spell, @NotNull SpellContext context);

    /**
     * Deactivates a toggleable spell for the given player.
     * If the spell is not active or not toggleable, this method has no effect.
     * 
     * @param player  the player to deactivate the spell for
     * @param spell   the spell to deactivate
     * @param context the spell casting context
     * @return {@code true} if the spell was deactivated, {@code false} otherwise
     */
    boolean deactivateSpell(@NotNull Player player, @NotNull Spell<?> spell, @NotNull SpellContext context);

    /**
     * Checks if a spell is currently active for the given player.
     * 
     * @param player the player to check
     * @param spell  the spell to check
     * @return {@code true} if the spell is active, {@code false} otherwise
     */
    boolean isSpellActive(@NotNull Player player, @NotNull Spell<?> spell);

    /**
     * Gets all active toggleable spells for the given player.
     * 
     * @param player the player to get active spells for
     * @return a set of active toggleable spells
     */
    @NotNull
    Set<ToggleableSpell> getActiveSpells(@NotNull Player player);

    /**
     * Deactivates all active spells for the given player.
     * This is typically called when a player logs out or dies.
     * 
     * @param player the player to deactivate all spells for
     */
    void deactivateAllSpells(@NotNull Player player);

    /**
     * Gets the number of active spells for the given player.
     * 
     * @param player the player to count active spells for
     * @return the number of active spells
     */
    int getActiveSpellCount(@NotNull Player player);

    /**
     * Checks if the given spell is a toggleable spell.
     * 
     * @param spell the spell to check
     * @return {@code true} if the spell implements ToggleableSpell, {@code false}
     *         otherwise
     */
    boolean isToggleableSpell(@NotNull Spell<?> spell);
}
