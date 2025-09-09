package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.framework.command.CommandException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and validating command arguments with rich error messages.
 * Provides type-safe argument extraction with detailed validation.
 */
public class ArgumentParser {
    private final String[] args;
    private final String commandName;

    public ArgumentParser(@NotNull String[] args, @NotNull String commandName) {
        this.args = args;
        this.commandName = commandName;
    }

    /**
     * Gets a required string argument at the specified index.
     *
     * @param index The argument index (0-based)
     * @return The argument value
     * @throws CommandException if the argument is missing
     */
    public @NotNull String getString(int index) throws CommandException {
        if (index >= args.length) {
            throw new CommandException(
                String.format("Missing required argument at position %d", index + 1),
                "ARG_MISSING",
                index
            );
        }
        return args[index];
    }

    /**
     * Gets an optional string argument at the specified index.
     *
     * @param index The argument index (0-based)
     * @param defaultValue The default value if argument is missing
     * @return The argument value or default
     */
    public @NotNull String getString(int index, @NotNull String defaultValue) {
        if (index >= args.length) {
            return defaultValue;
        }
        return args[index];
    }

    /**
     * Gets a required integer argument at the specified index.
     *
     * @param index The argument index (0-based)
     * @return The parsed integer value
     * @throws CommandException if the argument is missing or invalid
     */
    public int getInteger(int index) throws CommandException {
        String value = getString(index);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CommandException(
                String.format("Invalid number '%s' at position %d", value, index + 1),
                "ARG_INVALID_NUMBER",
                index, value
            );
        }
    }

    /**
     * Gets an optional integer argument at the specified index.
     *
     * @param index The argument index (0-based)
     * @param defaultValue The default value if argument is missing or invalid
     * @return The parsed integer value or default
     */
    public int getInteger(int index, int defaultValue) {
        if (index >= args.length) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a required integer argument with range validation.
     *
     * @param index The argument index (0-based)
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @return The validated integer value
     * @throws CommandException if the argument is missing, invalid, or out of range
     */
    public int getInteger(int index, int min, int max) throws CommandException {
        int value = getInteger(index);
        if (value < min || value > max) {
            throw new CommandException(
                String.format("Value %d must be between %d and %d", value, min, max),
                "ARG_OUT_OF_RANGE",
                index, value, min, max
            );
        }
        return value;
    }

    /**
     * Gets a required boolean argument at the specified index.
     *
     * @param index The argument index (0-based)
     * @return The parsed boolean value
     * @throws CommandException if the argument is missing or invalid
     */
    public boolean getBoolean(int index) throws CommandException {
        String value = getString(index).toLowerCase();
        if ("true".equals(value)) {
            return true;
        } else if ("false".equals(value)) {
            return false;
        } else {
            throw new CommandException(
                String.format("Invalid boolean value '%s' at position %d. Use 'true' or 'false'", value, index + 1),
                "ARG_INVALID_BOOLEAN",
                index, value
            );
        }
    }

    /**
     * Gets an optional boolean argument at the specified index.
     *
     * @param index The argument index (0-based)
     * @param defaultValue The default value if argument is missing or invalid
     * @return The parsed boolean value or default
     */
    public boolean getBoolean(int index, boolean defaultValue) {
        if (index >= args.length) {
            return defaultValue;
        }
        String value = args[index].toLowerCase();
        if ("true".equals(value)) {
            return true;
        } else if ("false".equals(value)) {
            return false;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets a required enum argument at the specified index.
     *
     * @param index The argument index (0-based)
     * @param enumClass The enum class to parse against
     * @return The parsed enum value
     * @throws CommandException if the argument is missing or invalid
     */
    public <T extends Enum<T>> T getEnum(int index, Class<T> enumClass) throws CommandException {
        String value = getString(index);
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            String[] validValues = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .toArray(String[]::new);
            
            throw new CommandException(
                String.format("Invalid value '%s' at position %d. Valid values: %s", value, index + 1, String.join(", ", validValues)),
                "ARG_INVALID_ENUM",
                index, value, enumClass.getSimpleName(), validValues
            );
        }
    }

    /**
     * Validates that a string argument matches one of the allowed values (case-insensitive).
     *
     * @param index The argument index (0-based)
     * @param allowedValues The allowed values
     * @return The validated argument value (in original case)
     * @throws CommandException if the argument is missing or invalid
     */
    public @NotNull String validateChoice(int index, @NotNull String... allowedValues) throws CommandException {
        String value = getString(index);
        for (String allowed : allowedValues) {
            if (allowed.equalsIgnoreCase(value)) {
                return value;
            }
        }
        
        throw new CommandException(
            String.format("Invalid value '%s' at position %d. Allowed values: %s", value, index + 1, String.join(", ", allowedValues)),
            "ARG_INVALID_CHOICE",
            index, value, allowedValues
        );
    }

    /**
     * Validates that a string argument matches a regex pattern.
     *
     * @param index The argument index (0-based)
     * @param pattern The regex pattern to match
     * @param description Description of what the argument should be
     * @return The validated argument value
     * @throws CommandException if the argument is missing or invalid
     */
    public @NotNull String validatePattern(int index, @NotNull Pattern pattern, @NotNull String description) throws CommandException {
        String value = getString(index);
        if (!pattern.matcher(value).matches()) {
            throw new CommandException(
                String.format("Invalid value '%s' at position %d. %s", value, index + 1, description),
                "ARG_INVALID_PATTERN",
                index, value, description
            );
        }
        return value;
    }

    /**
     * Gets the remaining arguments as a list, starting from the specified index.
     *
     * @param startIndex The starting index (0-based)
     * @return List of remaining arguments
     */
    public @NotNull List<String> getRemaining(int startIndex) {
        if (startIndex >= args.length) {
            return List.of();
        }
        return Arrays.asList(Arrays.copyOfRange(args, startIndex, args.length));
    }

    /**
     * Gets the total number of arguments.
     *
     * @return The argument count
     */
    public int size() {
        return args.length;
    }

    /**
     * Checks if an argument exists at the specified index.
     *
     * @param index The argument index (0-based)
     * @return true if the argument exists, false otherwise
     */
    public boolean hasArgument(int index) {
        return index < args.length;
    }
}