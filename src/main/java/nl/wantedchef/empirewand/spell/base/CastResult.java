package nl.wantedchef.empirewand.spell.base;

import java.util.Objects;

import net.kyori.adventure.text.Component;

/**
 * Represents the result of a spell cast operation.
 * Immutable and minimal by design.
 */
public record CastResult(ResultType type, Component message) {

    public enum ResultType {
        SUCCESS,
        FAILURE,
        COOLDOWN,
        INVALID_TARGET,
        BLOCKED
    }

    // ----- Canonical constructor with sane defaults -----
    public CastResult {
        type = Objects.requireNonNull(type, "type");
        message = message == null ? Component.empty() : message;
    }

    // ----- Common singletons without message -----
    public static final CastResult SUCCESS = new CastResult(ResultType.SUCCESS, Component.empty());
    public static final CastResult FAILURE = new CastResult(ResultType.FAILURE, Component.empty());

    // ----- Convenience factories with message -----
    public static CastResult success(Component message) {
        return new CastResult(ResultType.SUCCESS, message);
    }
    public static CastResult fail(Component message) {
        return new CastResult(ResultType.FAILURE, message);
    }
    public static CastResult cooldown(Component message) {
        return new CastResult(ResultType.COOLDOWN, message);
    }
    public static CastResult invalidTarget(Component message) {
        return new CastResult(ResultType.INVALID_TARGET, message);
    }
    public static CastResult blocked(Component message) {
        return new CastResult(ResultType.BLOCKED, message);
    }

    // ----- Boolean helpers -----
    public boolean isSuccess() {
        return type == ResultType.SUCCESS;
    }

    public boolean isFailure() {
        return type == ResultType.FAILURE;
    }
}





