package com.example.empirewand.api;

import com.example.empirewand.spell.Spell;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Event fired when a spell is successfully cast.
 *
 * <p>
 * This event is fired after a spell has been validated, cost applied, and
 * execution completed.
 * It provides information about the caster, spell, and context of the cast.
 * </p>
 *
 * <p>
 * <b>API Stability:</b> Experimental - Subject to change in future versions
 * </p>
 *
 * @since 1.1.0
 */
@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Bukkit events intentionally expose underlying Player/Spell references; callers must treat them as read-only.")
public class SpellCastEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player caster;
    private final Spell<?> spell;
    private final String spellKey;

    /**
     * Creates a new SpellCastEvent.
     *
     * @param caster   the player who cast the spell
     * @param spell    the spell that was cast
     * @param spellKey the registry key of the spell
     */
    public SpellCastEvent(@NotNull Player caster, @NotNull Spell<?> spell, @NotNull String spellKey) {
        this.caster = caster;
        this.spell = spell;
        this.spellKey = spellKey;
    }

    /**
     * Gets the player who cast the spell.
     *
     * @return the caster player
     */
    @NotNull
    public Player getCaster() {
        return caster;
    }

    /**
     * Gets the spell that was cast.
     *
     * @return the spell instance
     */
    @NotNull
    public Spell<?> getSpell() {
        return spell;
    }

    /**
     * Gets the registry key of the spell that was cast.
     *
     * @return the spell key (kebab-case)
     */
    @NotNull
    public String getSpellKey() {
        return spellKey;
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