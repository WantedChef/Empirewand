package com.example.empirewand.spell;

import net.kyori.adventure.text.Component;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Result of a spell cast attempt.
 */
@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Kyori Adventure Components are immutable-safe for consumer usage; exposing the reference is intentional and side-effect free.")
public record CastResult(boolean success, Component message) {
    public static final CastResult SUCCESS = new CastResult(true, Component.empty());
    public static final CastResult FAILURE = new CastResult(false, Component.empty());

    public static CastResult fail(Component message) {
        return new CastResult(false, message);
    }
}