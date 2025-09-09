package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.framework.command.CommandException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArgumentParser Tests")
class ArgumentParserTest {

    private ArgumentParser parser;

    @Nested
    @DisplayName("String Argument Tests")
    class StringArgumentTests {
        
        @Test
        @DisplayName("Should return string argument when present")
        void shouldReturnStringArgumentWhenPresent() throws CommandException {
            parser = new ArgumentParser(new String[]{"hello", "world"}, "test");
            assertEquals("hello", parser.getString(0));
            assertEquals("world", parser.getString(1));
        }

        @Test
        @DisplayName("Should throw exception when string argument is missing")
        void shouldThrowExceptionWhenStringArgumentIsMissing() {
            parser = new ArgumentParser(new String[]{"hello"}, "test");
            CommandException exception = assertThrows(CommandException.class, () -> {
                parser.getString(1);
            });
            assertTrue(exception.getMessage().contains("Missing required argument"));
            assertEquals("ARG_MISSING", exception.getErrorCode());
        }

        @Test
        @DisplayName("Should return default value for optional string argument when missing")
        void shouldReturnDefaultValueForOptionalStringArgumentWhenMissing() {
            parser = new ArgumentParser(new String[]{"hello"}, "test");
            assertEquals("default", parser.getString(1, "default"));
            assertEquals("hello", parser.getString(0, "default"));
        }
    }

    @Nested
    @DisplayName("Integer Argument Tests")
    class IntegerArgumentTests {
        
        @Test
        @DisplayName("Should parse valid integer argument")
        void shouldParseValidIntegerArgument() throws CommandException {
            parser = new ArgumentParser(new String[]{"123", "-456"}, "test");
            assertEquals(123, parser.getInteger(0));
            assertEquals(-456, parser.getInteger(1));
        }

        @Test
        @DisplayName("Should throw exception for invalid integer argument")
        void shouldThrowExceptionForInvalidIntegerArgument() {
            parser = new ArgumentParser(new String[]{"not_a_number"}, "test");
            CommandException exception = assertThrows(CommandException.class, () -> {
                parser.getInteger(0);
            });
            assertTrue(exception.getMessage().contains("Invalid number"));
            assertEquals("ARG_INVALID_NUMBER", exception.getErrorCode());
        }

        @Test
        @DisplayName("Should validate integer range")
        void shouldValidateIntegerRange() throws CommandException {
            parser = new ArgumentParser(new String[]{"50"}, "test");
            assertEquals(50, parser.getInteger(0, 1, 100));
            
            CommandException exception = assertThrows(CommandException.class, () -> {
                parser.getInteger(0, 1, 10);
            });
            assertTrue(exception.getMessage().contains("must be between"));
            assertEquals("ARG_OUT_OF_RANGE", exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Boolean Argument Tests")
    class BooleanArgumentTests {
        
        @Test
        @DisplayName("Should parse valid boolean arguments")
        void shouldParseValidBooleanArguments() throws CommandException {
            parser = new ArgumentParser(new String[]{"true", "false", "TRUE", "FALSE"}, "test");
            assertTrue(parser.getBoolean(0));
            assertFalse(parser.getBoolean(1));
            assertTrue(parser.getBoolean(2));
            assertFalse(parser.getBoolean(3));
        }

        @Test
        @DisplayName("Should throw exception for invalid boolean argument")
        void shouldThrowExceptionForInvalidBooleanArgument() {
            parser = new ArgumentParser(new String[]{"maybe"}, "test");
            CommandException exception = assertThrows(CommandException.class, () -> {
                parser.getBoolean(0);
            });
            assertTrue(exception.getMessage().contains("Invalid boolean value"));
            assertEquals("ARG_INVALID_BOOLEAN", exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Choice Validation Tests")
    class ChoiceValidationTests {
        
        @Test
        @DisplayName("Should validate valid choice")
        void shouldValidateValidChoice() throws CommandException {
            parser = new ArgumentParser(new String[]{"option1"}, "test");
            assertEquals("option1", parser.validateChoice(0, "option1", "option2", "option3"));
        }

        @Test
        @DisplayName("Should throw exception for invalid choice")
        void shouldThrowExceptionForInvalidChoice() {
            parser = new ArgumentParser(new String[]{"invalid"}, "test");
            CommandException exception = assertThrows(CommandException.class, () -> {
                parser.validateChoice(0, "option1", "option2", "option3");
            });
            assertTrue(exception.getMessage().contains("Invalid value"));
            assertTrue(exception.getMessage().contains("Allowed values"));
            assertEquals("ARG_INVALID_CHOICE", exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Pattern Validation Tests")
    class PatternValidationTests {
        
        @Test
        @DisplayName("Should validate argument against pattern")
        void shouldValidateArgumentAgainstPattern() throws CommandException {
            parser = new ArgumentParser(new String[]{"test123"}, "test");
            Pattern pattern = Pattern.compile("[a-z]+[0-9]+");
            assertEquals("test123", parser.validatePattern(0, pattern, "Must be letters followed by numbers"));
        }

        @Test
        @DisplayName("Should throw exception for pattern mismatch")
        void shouldThrowExceptionForPatternMismatch() {
            parser = new ArgumentParser(new String[]{"123test"}, "test");
            Pattern pattern = Pattern.compile("[a-z]+[0-9]+");
            CommandException exception = assertThrows(CommandException.class, () -> {
                parser.validatePattern(0, pattern, "Must be letters followed by numbers");
            });
            assertTrue(exception.getMessage().contains("Invalid value"));
            assertTrue(exception.getMessage().contains("Must be letters followed by numbers"));
            assertEquals("ARG_INVALID_PATTERN", exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {
        
        @Test
        @DisplayName("Should return remaining arguments")
        void shouldReturnRemainingArguments() {
            parser = new ArgumentParser(new String[]{"arg1", "arg2", "arg3", "arg4"}, "test");
            List<String> remaining = parser.getRemaining(2);
            assertEquals(2, remaining.size());
            assertEquals("arg3", remaining.get(0));
            assertEquals("arg4", remaining.get(1));
        }

        @Test
        @DisplayName("Should return empty list when no remaining arguments")
        void shouldReturnEmptyListWhenNoRemainingArguments() {
            parser = new ArgumentParser(new String[]{"arg1", "arg2"}, "test");
            List<String> remaining = parser.getRemaining(2);
            assertTrue(remaining.isEmpty());
        }

        @Test
        @DisplayName("Should return argument count")
        void shouldReturnArgumentCount() {
            parser = new ArgumentParser(new String[]{"arg1", "arg2", "arg3"}, "test");
            assertEquals(3, parser.size());
        }

        @Test
        @DisplayName("Should check if argument exists")
        void shouldCheckIfArgumentExists() {
            parser = new ArgumentParser(new String[]{"arg1", "arg2"}, "test");
            assertTrue(parser.hasArgument(0));
            assertTrue(parser.hasArgument(1));
            assertFalse(parser.hasArgument(2));
        }
    }
}