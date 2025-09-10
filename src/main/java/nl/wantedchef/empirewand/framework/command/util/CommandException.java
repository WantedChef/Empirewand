package nl.wantedchef.empirewand.framework.command.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when command argument parsing or validation fails. Provides detailed error
 * information including error codes and context data.
 */
public class CommandException extends Exception {
    private final String errorCode;
    private final int argumentIndex;
    private final Object[] contextData;

    /**
     * Creates a CommandException with a simple message.
     *
     * @param message The error message
     */
    public CommandException(@NotNull String message) {
        super(message);
        this.errorCode = "GENERIC_ERROR";
        this.argumentIndex = -1;
        this.contextData = new Object[0];
    }

    /**
     * Creates a CommandException with message and error code.
     *
     * @param message The error message
     * @param errorCode The error code
     */
    public CommandException(@NotNull String message, @NotNull String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.argumentIndex = -1;
        this.contextData = new Object[0];
    }

    /**
     * Creates a CommandException with message, error code, and argument index.
     *
     * @param message The error message
     * @param errorCode The error code
     * @param argumentIndex The index of the problematic argument
     */
    public CommandException(@NotNull String message, @NotNull String errorCode, int argumentIndex) {
        super(message);
        this.errorCode = errorCode;
        this.argumentIndex = argumentIndex;
        this.contextData = new Object[0];
    }

    /**
     * Creates a CommandException with full context information.
     *
     * @param message The error message
     * @param errorCode The error code
     * @param argumentIndex The index of the problematic argument
     * @param contextData Additional context data for the error
     */
    public CommandException(@NotNull String message, @NotNull String errorCode, int argumentIndex,
            @NotNull Object... contextData) {
        super(message);
        this.errorCode = errorCode;
        this.argumentIndex = argumentIndex;
        this.contextData = contextData.clone();
    }

    /**
     * Gets the error code for this exception.
     *
     * @return The error code
     */
    @NotNull
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the argument index that caused this exception.
     *
     * @return The argument index, or -1 if not applicable
     */
    public int getArgumentIndex() {
        return argumentIndex;
    }

    /**
     * Gets the context data associated with this exception.
     *
     * @return Array of context data objects
     */
    @NotNull
    public Object[] getContextData() {
        return contextData.clone();
    }

    /**
     * Gets a specific context data element by index.
     *
     * @param index The index of the context data element
     * @return The context data element, or null if index is out of bounds
     */
    @Nullable
    public Object getContextData(int index) {
        if (index >= 0 && index < contextData.length) {
            return contextData[index];
        }
        return null;
    }

    /**
     * Checks if this exception has context data.
     *
     * @return true if context data is available, false otherwise
     */
    public boolean hasContextData() {
        return contextData.length > 0;
    }
}
