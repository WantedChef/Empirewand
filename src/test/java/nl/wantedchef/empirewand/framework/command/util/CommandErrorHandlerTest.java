package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;

@DisplayName("CommandErrorHandler Tests")
class CommandErrorHandlerTest {

    private CommandErrorHandler errorHandler;

    @Mock
    private EmpireWandPlugin plugin;

    @Mock
    private Logger logger;

    @Mock
    private CommandSender sender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getLogger()).thenReturn(logger);
        errorHandler = new CommandErrorHandler(plugin);
    }

    @Nested
    @DisplayName("Command Exception Handling Tests")
    class CommandExceptionHandlingTests {

        @Test
        @DisplayName("Should handle command exception with error code")
        void shouldHandleCommandExceptionWithErrorCode() {
            CommandException exception =
                    new CommandException("Test error", "TEST_ERROR", "context1", "context2");

            errorHandler.handleCommandException(sender, exception, "testcommand");

            // Verify error message sent to sender
            verify(sender).sendMessage(any(Component.class));

            // Verify logging
            verify(logger).info(anyString());
        }

        @Test
        @DisplayName("Should handle command exception without error code")
        void shouldHandleCommandExceptionWithoutErrorCode() {
            CommandException exception = new CommandException("Test error");

            errorHandler.handleCommandException(sender, exception, "testcommand");

            // Verify error message sent to sender
            verify(sender).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should handle unexpected exception")
        void shouldHandleUnexpectedException() {
            Exception exception = new RuntimeException("Unexpected error");

            errorHandler.handleUnexpectedException(sender, exception, "testcommand",
                    new String[] {"arg1", "arg2"});

            // Verify error message sent to sender
            verify(sender)
                    .sendMessage((Component) argThat(component -> component instanceof Component
                            && ((Component) component).color() != null
                            && ((Component) component).color().equals(NamedTextColor.RED)));

            // Verify error logging
            verify(logger).severe(anyString());
        }
    }

    @Nested
    @DisplayName("Message Creation Tests")
    class MessageCreationTests {

        @Test
        @DisplayName("Should create error message with red color")
        void shouldCreateErrorMessageWithRedColor() {
            Component message = CommandErrorHandler.createErrorMessage("Test error");
            assertEquals(NamedTextColor.RED,
                    message.color() instanceof NamedTextColor ? (NamedTextColor) message.color()
                            : NamedTextColor.WHITE);
            assertEquals("Test error",
                    ((net.kyori.adventure.text.TextComponent) message).content());
        }

        @Test
        @DisplayName("Should create success message with green color")
        void shouldCreateSuccessMessageWithGreenColor() {
            Component message = CommandErrorHandler.createSuccessMessage("Test success");
            assertEquals(NamedTextColor.GREEN,
                    message.color() instanceof NamedTextColor ? (NamedTextColor) message.color()
                            : NamedTextColor.WHITE);
            assertEquals("Test success",
                    ((net.kyori.adventure.text.TextComponent) message).content());
        }

        @Test
        @DisplayName("Should create info message with yellow color")
        void shouldCreateInfoMessageWithYellowColor() {
            Component message = CommandErrorHandler.createInfoMessage("Test info");
            assertEquals(NamedTextColor.YELLOW,
                    message.color() instanceof NamedTextColor ? (NamedTextColor) message.color()
                            : NamedTextColor.WHITE);
            assertEquals("Test info", ((net.kyori.adventure.text.TextComponent) message).content());
        }
    }
}
