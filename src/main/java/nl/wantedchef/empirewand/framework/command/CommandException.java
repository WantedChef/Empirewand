package nl.wantedchef.empirewand.framework.command;

import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown during command execution for user-facing errors.
 * The message will be displayed to the command sender.
 * Enhanced with error codes and additional context for better error handling.
 */
public class CommandException extends Exception {
    private final String errorCode;
    private final Object[] context;

    public CommandException(String message) {
        super(message);
        this.errorCode = null;
        this.context = new Object[0];
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.context = new Object[0];
    }

    public CommandException(String message, String errorCode, Object... context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? context.clone() : new Object[0];
    }

    public CommandException(String message, Throwable cause, String errorCode, Object... context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context != null ? context.clone() : new Object[0];
    }

    /**
     * Gets the error code associated with this exception, if any.
     * 
     * @return The error code or null if not set
     */
    @Nullable
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the context objects associated with this exception.
     * 
     * @return An array of context objects (never null)
     */
    public Object[] getContext() {
        return context.clone();
    }
}





