package nl.wantedchef.empirewand.framework.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@DisplayName("CommandException Tests")
class CommandExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateExceptionWithMessage() {
            CommandException exception = new CommandException("Test message");
            assertEquals("Test message", exception.getMessage());
            assertNull(exception.getCause());
            assertNull(exception.getErrorCode());
            assertArrayEquals(new Object[0], exception.getContext());
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            Exception cause = new Exception("Cause");
            CommandException exception = new CommandException("Test message", cause);
            assertEquals("Test message", exception.getMessage());
            assertEquals(cause, exception.getCause());
            assertNull(exception.getErrorCode());
            assertArrayEquals(new Object[0], exception.getContext());
        }

        @Test
        @DisplayName("Should create exception with message, error code, and context")
        void shouldCreateExceptionWithMessageErrorCodeAndContext() {
            CommandException exception = new CommandException("Test message", "ERROR_CODE", "context1", "context2");
            assertEquals("Test message", exception.getMessage());
            assertNull(exception.getCause());
            assertEquals("ERROR_CODE", exception.getErrorCode());
            assertArrayEquals(new Object[]{"context1", "context2"}, exception.getContext());
        }

        @Test
        @DisplayName("Should create exception with message, cause, error code, and context")
        void shouldCreateExceptionWithMessageCauseErrorCodeAndContext() {
            Exception cause = new Exception("Cause");
            CommandException exception = new CommandException("Test message", cause, "ERROR_CODE", "context1", "context2");
            assertEquals("Test message", exception.getMessage());
            assertEquals(cause, exception.getCause());
            assertEquals("ERROR_CODE", exception.getErrorCode());
            assertArrayEquals(new Object[]{"context1", "context2"}, exception.getContext());
        }

        @Test
        @DisplayName("Should handle null context")
        void shouldHandleNullContext() {
            CommandException exception = new CommandException("Test message", "ERROR_CODE", (Object[]) null);
            assertEquals("Test message", exception.getMessage());
            assertEquals("ERROR_CODE", exception.getErrorCode());
            assertArrayEquals(new Object[0], exception.getContext());
        }
    }

    @Nested
    @DisplayName("Context Tests")
    class ContextTests {
        
        @Test
        @DisplayName("Should return defensive copy of context")
        void shouldReturnDefensiveCopyOfContext() {
            Object[] originalContext = {"context1", "context2"};
            CommandException exception = new CommandException("Test message", "ERROR_CODE", originalContext);
            
            Object[] context = exception.getContext();
            assertArrayEquals(originalContext, context);
            
            // Modify returned array - should not affect original
            context[0] = "modified";
            Object[] context2 = exception.getContext();
            assertEquals("context1", context2[0]);
        }

        @Test
        @DisplayName("Should handle empty context")
        void shouldHandleEmptyContext() {
            CommandException exception = new CommandException("Test message");
            assertArrayEquals(new Object[0], exception.getContext());
        }
    }
}