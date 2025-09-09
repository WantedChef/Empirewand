package com.example.empirewand.api.spell.toggle;

import com.example.empirewand.spell.SpellContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for spells that can be toggled on and off.
 * Toggleable spells maintain their state between casts and can be
 * activated/deactivated.
 * 
 * <p>
 * Examples of toggleable spells:
 * <ul>
 * <li>Aura spells that provide continuous buffs/debuffs</li>
 * <li>Cloak spells that maintain invisibility</li>
 * <li>Shield spells that provide ongoing protection</li>
 * </ul>
 * 
 * @since 2.1.0
 */
public interface ToggleableSpell {

    /**
     * Checks if this spell is currently active for the given player.
     * 
     * @param player the player to check
     * @return {@code true} if the spell is active, {@code false} otherwise
     */
    boolean isActive(@NotNull Player player);

    /**
     * Activates this spell for the given player.
     * This method should start any ongoing effects, schedulers, or tasks.
     * 
     * @param player  the player to activate the spell for
     * @param context the spell casting context
     */
    void activate(@NotNull Player player, @NotNull SpellContext context);

    /**
     * Deactivates this spell for the given player.
     * This method should clean up any ongoing effects, cancel schedulers, and
     * remove tasks.
     * 
     * @param player  the player to deactivate the spell for
     * @param context the spell casting context
     */
    void deactivate(@NotNull Player player, @NotNull SpellContext context);

    /**
     * Toggles this spell for the given player.
     * If the spell is active, it will be deactivated. If inactive, it will be
     * activated.
     * 
     * <p>
     * The default implementation simply checks {@link #isActive(Player)} and calls
     * either {@link #activate(Player, SpellContext)} or
     * {@link #deactivate(Player, SpellContext)}
     * accordingly.
     * 
     * @param player  the player to toggle the spell for
     * @param context the spell casting context
     */
    default void toggle(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player)) {
            deactivate(player, context);
        } else {
            activate(player, context);
        }
    }

    /**
     * Gets the maximum duration this spell can remain active (in ticks).
     * Return -1 for infinite duration.
     * 
     * @return the maximum duration in ticks, or -1 for infinite
     */
    default int getMaxDuration() {
        return -1; // Infinite by default
    }

    /**
     * Called when the spell is forcibly deactivated (e.g., player logout, death).
     * This should perform the same cleanup as
     * {@link #deactivate(Player, SpellContext)}
     * but without requiring a full SpellContext.
     * 
     * @param player the player whose spell is being forcibly deactivated
     */
    default void forceDeactivate(@NotNull Player player) {
        // Default implementation - subclasses should override if they need
        // to perform cleanup without a full context
    }
}