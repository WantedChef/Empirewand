package com.example.empirewand.command.framework;

/**
 * Exception thrown during command execution for user-facing errors.
 * The message will be displayed to the command sender.
 */
public class CommandException extends Exception {
    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}