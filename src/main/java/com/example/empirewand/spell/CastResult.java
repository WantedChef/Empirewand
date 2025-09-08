package com.example.empirewand.spell;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the outcome of a spell cast attempt. This is an immutable record containing the result type and any
 * associated feedback for the player, such as messages or sounds.
 */
@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Kyori Adventure Components are immutable-safe for consumer usage; exposing the reference is intentional and side-effect free.")
public record CastResult(
        @NotNull ResultType type,
        @NotNull Component message,
        @Nullable Particle particle,
        @Nullable Sound sound,
        float volume,
        float pitch) {

    /**
     * Defines the possible outcomes of a spell cast.
     */
    public enum ResultType {
        /** The spell was cast successfully. */
        SUCCESS,
        /** The spell failed for a generic reason. */
        FAILURE,
        /** The spell is on cooldown. */
        COOLDOWN,
        /** The caster has insufficient mana. */
        INSUFFICIENT_MANA,
        /** The spell was cast on an invalid target. */
        INVALID_TARGET,
        /** The spell was blocked by an external factor (e.g., a protection spell). */
        BLOCKED
    }

    /** A generic successful cast result with no message or effects. */
    public static final CastResult SUCCESS = new CastResult(ResultType.SUCCESS, Component.empty(), null, null, 1.0f,
            1.0f);
    /** A generic failed cast result with no message or effects. */
    public static final CastResult FAILURE = new CastResult(ResultType.FAILURE, Component.empty(), null, null, 1.0f,
            1.0f);

    /** Creates a SUCCESS result with a specific message. */
    public static CastResult success(@NotNull Component message) {
        return new CastResult(ResultType.SUCCESS, message, null, null, 1.0f, 1.0f);
    }

    /** Creates a FAILURE result with a specific message. */
    public static CastResult fail(@NotNull Component message) {
        return new CastResult(ResultType.FAILURE, message, null, null, 1.0f, 1.0f);
    }

    /** Creates a COOLDOWN result with a specific message. */
    public static CastResult cooldown(@NotNull Component message) {
        return new CastResult(ResultType.COOLDOWN, message, null, null, 1.0f, 1.0f);
    }

    /** Creates an INSUFFICIENT_MANA result with a specific message. */
    public static CastResult insufficientMana(@NotNull Component message) {
        return new CastResult(ResultType.INSUFFICIENT_MANA, message, null, null, 1.0f, 1.0f);
    }

    /** Creates an INVALID_TARGET result with a specific message. */
    public static CastResult invalidTarget(@NotNull Component message) {
        return new CastResult(ResultType.INVALID_TARGET, message, null, null, 1.0f, 1.0f);
    }

    /** Creates a BLOCKED result with a specific message. */
    public static CastResult blocked(@NotNull Component message) {
        return new CastResult(ResultType.BLOCKED, message, null, null, 1.0f, 1.0f);
    }

    /**
     * Checks if the cast was successful.
     *
     * @return true if the result type is SUCCESS.
     */
    public boolean isSuccess() {
        return type == ResultType.SUCCESS;
    }
}
