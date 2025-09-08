package com.example.empirewand.api;

import com.example.empirewand.spell.Spell;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Event fired when a player selects a different spell on their wand.
 *
 * <p>
 * This event is fired when players cycle through their bound spells using
 * scroll wheel, sneak+click, or command-based selection.
 * </p>
 *
 * <p>
 * <b>API Stability:</b> Experimental - Subject to change in future versions
 * </p>
 *
 * @since 1.1.0
 */
@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Bukkit event pattern; direct references are conventional and required for listener interaction.")
public class WandSelectEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Spell<?> previousSpell;
    private final Spell<?> newSpell;
    private final String previousSpellKey;
    private final String newSpellKey;
    private final SelectionMethod method;

    /**
     * Creates a new WandSelectEvent.
     *
     * @param player           the player who changed spell selection
     * @param previousSpell    the previously selected spell (may be null)
     * @param newSpell         the newly selected spell
     * @param previousSpellKey the key of the previously selected spell (may be
     *                         null)
     * @param newSpellKey      the key of the newly selected spell
     * @param method           the method used for selection
     */
    public WandSelectEvent(@NotNull Player player, @Nullable Spell<?> previousSpell, @NotNull Spell<?> newSpell,
            @Nullable String previousSpellKey, @NotNull String newSpellKey, @NotNull SelectionMethod method) {
        this.player = player;
        this.previousSpell = previousSpell;
        this.newSpell = newSpell;
        this.previousSpellKey = previousSpellKey;
        this.newSpellKey = newSpellKey;
        this.method = method;
    }

    /**
     * Gets the player who changed their spell selection.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the previously selected spell.
     *
     * @return the previous spell, or null if this was the first selection
     */
    @Nullable
    public Spell<?> getPreviousSpell() {
        return previousSpell;
    }

    /**
     * Gets the newly selected spell.
     *
     * @return the new spell
     */
    @NotNull
    public Spell<?> getNewSpell() {
        return newSpell;
    }

    /**
     * Gets the registry key of the previously selected spell.
     *
     * @return the previous spell key, or null if this was the first selection
     */
    @Nullable
    public String getPreviousSpellKey() {
        return previousSpellKey;
    }

    /**
     * Gets the registry key of the newly selected spell.
     *
     * @return the new spell key (kebab-case)
     */
    @NotNull
    public String getNewSpellKey() {
        return newSpellKey;
    }

    /**
     * Gets the method used for spell selection.
     *
     * @return the selection method
     */
    @NotNull
    public SelectionMethod getMethod() {
        return method;
    }

    /**
     * Enumeration of possible spell selection methods.
     */
    public enum SelectionMethod {
        /** Selected via scroll wheel */
        SCROLL,
        /** Selected via sneak + click */
        SNEAK_CLICK,
        /** Selected via command */
        COMMAND,
        /** Selected programmatically */
        API
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}