package nl.wantedchef.empirewand.api.event;

import nl.wantedchef.empirewand.spell.Spell;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Event fired when a spell fails to cast.
 *
 * <p>
 * This event is fired when a spell cannot be cast due to various reasons such
 * as
 * insufficient permissions, cooldown active, invalid target, or other
 * validation failures.
 * </p>
 *
 * <p>
 * <b>API Stability:</b> Experimental - Subject to change in future versions
 * </p>
 *
 * @since 1.1.0
 */
@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Bukkit event pattern; exposes Player/Spell for listener logic; immutable contract externally.")
public class SpellFailEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player caster;
    private final Spell<?> spell;
    private final String spellKey;
    private final FailReason reason;
    private final String message;

    /**
     * Creates a new SpellFailEvent.
     *
     * @param caster   the player who attempted to cast the spell
     * @param spell    the spell that failed (may be null if spell lookup failed)
     * @param spellKey the registry key of the attempted spell
     * @param reason   the reason for the failure
     * @param message  the user-facing failure message
     */
    public SpellFailEvent(@NotNull Player caster, @Nullable Spell<?> spell, @NotNull String spellKey,
            @NotNull FailReason reason, @NotNull String message) {
        this.caster = caster;
        this.spell = spell;
        this.spellKey = spellKey;
        this.reason = reason;
        this.message = message;
    }

    /**
     * Gets the player who attempted to cast the spell.
     *
     * @return the caster player
     */
    @NotNull
    public Player getCaster() {
        return caster;
    }

    /**
     * Gets the spell that failed to cast.
     *
     * @return the spell instance, or null if spell lookup failed
     */
    @Nullable
    public Spell<?> getSpell() {
        return spell;
    }

    /**
     * Gets the registry key of the spell that was attempted.
     *
     * @return the spell key (kebab-case)
     */
    @NotNull
    public String getSpellKey() {
        return spellKey;
    }

    /**
     * Gets the reason for the spell failure.
     *
     * @return the failure reason
     */
    @NotNull
    public FailReason getReason() {
        return reason;
    }

    /**
     * Gets the user-facing failure message.
     *
     * @return the localized failure message
     */
    @NotNull
    public String getMessage() {
        return message;
    }

    /**
     * Enumeration of possible spell failure reasons.
     */
    public enum FailReason {
        /** Player lacks permission to cast this spell */
        NO_PERMISSION,
        /** Spell is currently on cooldown */
        ON_COOLDOWN,
        /** Invalid or missing target for the spell */
        INVALID_TARGET,
        /** Target is out of range */
        OUT_OF_RANGE,
        /** Spell is disabled in configuration */
        SPELL_DISABLED,
        /** Unknown spell key */
        UNKNOWN_SPELL,
        /** Spell validation failed */
        VALIDATION_FAILED,
        /** Other unspecified failure */
        OTHER
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
