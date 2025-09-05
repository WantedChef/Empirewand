package com.example.empirewand.spell;

import net.kyori.adventure.text.Component;

/**
 * Result of a spell cast attempt.
 */
public record CastResult(boolean success, Component message) {
    public static final CastResult SUCCESS = new CastResult(true, Component.empty());
    public static final CastResult FAILURE = new CastResult(false, Component.empty());

    public static CastResult fail(Component message) {
        return new CastResult(false, message);
    }
}