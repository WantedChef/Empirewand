package com.example.empirewand.command.framework;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interface for all subcommands in the plugin.
 * Provides a clean, testable contract for command implementations.
 */
public interface SubCommand {

    /**
     * The name/alias of this subcommand.
     */
    @NotNull
    String getName();

    /**
     * Additional aliases for this command.
     */
    @NotNull
    default List<String> getAliases() {
        return List.of();
    }

    /**
     * The permission required to execute this command.
     * 
     * @return permission node or null if no permission required
     */
    @Nullable
    String getPermission();

    /**
     * Usage string for this command.
     */
    @NotNull
    String getUsage();

    /**
     * Short description of what this command does.
     */
    @NotNull
    String getDescription();

    /**
     * Execute the command.
     * 
     * @param context Command execution context
     * @throws CommandException if execution fails with user error
     */
    void execute(@NotNull CommandContext context) throws CommandException;

    /**
     * Provide tab completion suggestions.
     * 
     * @param context Command context
     * @return List of completion suggestions
     */
    @NotNull
    default List<String> tabComplete(@NotNull CommandContext context) {
        return List.of();
    }

    /**
     * Whether this command requires a player sender.
     */
    default boolean requiresPlayer() {
        return false;
    }
}