package nl.wantedchef.empirewand.spell;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the result of a spell cast operation.
 * <p>
 * This immutable record provides detailed information about the outcome of a spell
 * casting attempt, including the result type and an optional message for the player.
 * <p>
 * <strong>Usage Examples:</strong>
 * <pre>{@code
 * // Success without message
 * CastResult result = CastResult.SUCCESS;
 * 
 * // Success with custom message
 * CastResult result = CastResult.success(Component.text("Fireball launched!"));
 * 
 * // Failure with reason
 * CastResult result = CastResult.fail(Component.text("Not enough mana!"));
 * 
 * // Check result
 * if (result.isSuccess()) {
 *     // Handle success
 * } else {
 *     // Handle failure
 * }
 * }</pre>
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is immutable and thread-safe.
 *
 * @since 1.0.0
 */
public record CastResult(ResultType type, Component message) {

    /**
     * Enumeration of possible spell cast result types.
     * <p>
     * Each type represents a specific outcome of a spell casting attempt.
     */
    public enum ResultType {
        /** The spell was successfully cast */
        SUCCESS,
        
        /** The spell failed due to general failure */
        FAILURE,
        
        /** The spell is on cooldown */
        COOLDOWN,
        
        /** The spell target is invalid */
        INVALID_TARGET,
        
        /** The spell was blocked by external factors (e.g., protection plugins) */
        BLOCKED,
        
        /** The caster lacks required resources (mana, items, etc.) */
        INSUFFICIENT_RESOURCES,
        
        /** The caster does not meet level requirements */
        INSUFFICIENT_LEVEL,
        
        /** The spell is disabled or not available */
        SPELL_DISABLED,
        
        /** The spell cannot be cast in the current world/region */
        WRONG_WORLD,
        
        /** The spell cannot be cast in the current game mode */
        WRONG_GAMEMODE
    }

    /**
     * Predefined success result without a message.
     * <p>
     * Use this for successful casts that don't require additional feedback.
     */
    public static final CastResult SUCCESS = new CastResult(ResultType.SUCCESS, Component.empty());
    
    /**
     * Predefined failure result without a message.
     * <p>
     * Use this for generic failures when no specific reason is provided.
     */
    public static final CastResult FAILURE = new CastResult(ResultType.FAILURE, Component.empty());
    
    /**
     * Predefined cooldown result without a message.
     * <p>
     * Use this when a spell is on cooldown.
     */
    public static final CastResult COOLDOWN = new CastResult(ResultType.COOLDOWN, Component.empty());

    /**
     * Canonical constructor with validation and sane defaults.
     * <p>
     * Ensures that neither type nor message is null, providing empty component
     * for null messages.
     *
     * @param type the result type
     * @param message the result message, may be null for empty message
     * @throws NullPointerException if type is null
     */
    public CastResult {
        Objects.requireNonNull(type, "Result type cannot be null");
        message = message == null ? Component.empty() : message;
    }

    /**
     * Creates a success result with a custom message.
     * <p>
     * The message will be displayed to the player upon successful cast.
     *
     * @param message the success message
     * @return a success CastResult with the provided message
     * @throws NullPointerException if message is null
     */
    @NotNull
    public static CastResult success(@NotNull Component message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new CastResult(ResultType.SUCCESS, message);
    }

    /**
     * Creates a failure result with a custom message.
     * <p>
     * The message will be displayed to the player explaining why the spell failed.
     *
     * @param message the failure message
     * @return a failure CastResult with the provided message
     * @throws NullPointerException if message is null
     */
    @NotNull
    public static CastResult fail(@NotNull Component message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new CastResult(ResultType.FAILURE, message.color(NamedTextColor.RED));
    }

    /**
     * Creates a cooldown result with a custom message.
     * <p>
     * Use this when a spell cannot be cast due to cooldown restrictions.
     *
     * @param message the cooldown message
     * @return a cooldown CastResult with the provided message
     * @throws NullPointerException if message is null
     */
    @NotNull
    public static CastResult cooldown(@NotNull Component message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new CastResult(ResultType.COOLDOWN, message.color(NamedTextColor.YELLOW));
    }

    /**
     * Creates an invalid target result with a custom message.
     * <p>
     * Use this when a spell's target is invalid or out of range.
     *
     * @param message the invalid target message
     * @return an invalid target CastResult with the provided message
     * @throws NullPointerException if message is null
     */
    @NotNull
    public static CastResult invalidTarget(@NotNull Component message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new CastResult(ResultType.INVALID_TARGET, message.color(NamedTextColor.RED));
    }

    /**
     * Creates a blocked result with a custom message.
     * <p>
     * Use this when external factors (e.g., protection plugins) prevent spell casting.
     *
     * @param message the blocked message
     * @return a blocked CastResult with the provided message
     * @throws NullPointerException if message is null
     */
    @NotNull
    public static CastResult blocked(@NotNull Component message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new CastResult(ResultType.BLOCKED, message.color(NamedTextColor.GRAY));
    }

    /**
     * Creates an insufficient resources result with a custom message.
     * <p>
     * Use this when the caster lacks required resources (mana, items, etc.).
     *
     * @param message the resource message
     * @return an insufficient resources CastResult with the provided message
     * @throws NullPointerException if message is null
     */
    @NotNull
    public static CastResult insufficientResources(@NotNull Component message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new CastResult(ResultType.INSUFFICIENT_RESOURCES, message.color(NamedTextColor.GOLD));
    }

    /**
     * Creates an insufficient level result with a custom message.
     * <p>
     * Use this when the caster doesn't meet level requirements.
     *
     * @param message the level message
     * @return an insufficient level CastResult with the provided message
     * @throws NullPointerException if message is null
     */
    @NotNull
    public static CastResult insufficientLevel(@NotNull Component message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new CastResult(ResultType.INSUFFICIENT_LEVEL, message.color(NamedTextColor.RED));
    }

    /**
     * Creates a spell disabled result with a custom message.
     * <p>
     * Use this when a spell is disabled or not available to the caster.
     *
     * @param message the disabled message
     * @return a spell disabled CastResult with the provided message
     * @throws NullPointerException if message is null
     */
    @NotNull
    public static CastResult spellDisabled(@NotNull Component message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new CastResult(ResultType.SPELL_DISABLED, message.color(NamedTextColor.DARK_GRAY));
    }

    /**
     * Creates a wrong world result with a custom message.
     * <p>
     * Use this when a spell cannot be cast in the current world.
     *
     * @param message the world message
     * @return a wrong world CastResult with the provided message
     * @throws NullPointerException if message is null
     */
    @NotNull
    public static CastResult wrongWorld(@NotNull Component message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new CastResult(ResultType.WRONG_WORLD, message.color(NamedTextColor.DARK_RED));
    }

    /**
     * Creates a wrong gamemode result with a custom message.
     * <p>
     * Use this when a spell cannot be cast in the current game mode.
     *
     * @param message the gamemode message
     * @return a wrong gamemode CastResult with the provided message
     * @throws NullPointerException if message is null
     */
    @NotNull
    public static CastResult wrongGamemode(@NotNull Component message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new CastResult(ResultType.WRONG_GAMEMODE, message.color(NamedTextColor.RED));
    }

    /**
     * Checks if this result represents a successful cast.
     *
     * @return true if the result type is SUCCESS, false otherwise
     */
    public boolean isSuccess() {
        return type == ResultType.SUCCESS;
    }

    /**
     * Checks if this result represents a failed cast.
     * <p>
     * This includes all failure types except SUCCESS.
     *
     * @return true if the result type is not SUCCESS, false otherwise
     */
    public boolean isFailure() {
        return type != ResultType.SUCCESS;
    }

    /**
     * Checks if this result represents a specific failure type.
     *
     * @param failureType the failure type to check
     * @return true if this result matches the specified failure type
     */
    public boolean isFailureType(@NotNull ResultType failureType) {
        Objects.requireNonNull(failureType, "Failure type cannot be null");
        return type == failureType;
    }

    /**
     * Gets the message component with appropriate default styling.
     * <p>
     * The message will have default color styling based on the result type.
     *
     * @return the styled message component
     */
    @NotNull
    public Component getStyledMessage() {
        return message;
    }

    /**
     * Returns a string representation of this CastResult.
     * <p>
     * Format: "CastResult[type=SUCCESS, message='text']"
     *
     * @return a string representation of this result
     */
    @Override
    public String toString() {
        return "CastResult[type=" + type + ", message='" + message + "']";
    }
}
