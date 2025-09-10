package nl.wantedchef.empirewand.framework.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@DisplayName("SubCommand Interface Tests")
class SubCommandTest {

    @Nested
    @DisplayName("Default Method Tests")
    class DefaultMethodTests {
        
        private final TestSubCommand subCommand = new TestSubCommand();
        
        @Test
        @DisplayName("Should return empty list for default aliases")
        void shouldReturnEmptyListForDefaultAliases() {
            List<String> aliases = subCommand.getAliases();
            assertNotNull(aliases);
            assertTrue(aliases.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list for default tab completion")
        void shouldReturnEmptyListForDefaultTabCompletion() {
            CommandContext context = mock(CommandContext.class);
            List<String> completions = subCommand.tabComplete(context);
            assertNotNull(completions);
            assertTrue(completions.isEmpty());
        }

        @Test
        @DisplayName("Should not require player by default")
        void shouldNotRequirePlayerByDefault() {
            assertFalse(subCommand.requiresPlayer());
        }
    }

    @Nested
    @DisplayName("Custom Implementation Tests")
    class CustomImplementationTests {
        
        private final CustomSubCommand subCommand = new CustomSubCommand();
        
        @Test
        @DisplayName("Should return custom aliases")
        void shouldReturnCustomAliases() {
            List<String> aliases = subCommand.getAliases();
            assertNotNull(aliases);
            assertEquals(2, aliases.size());
            assertTrue(aliases.contains("alias1"));
            assertTrue(aliases.contains("alias2"));
        }

        @Test
        @DisplayName("Should return custom tab completions")
        void shouldReturnCustomTabCompletions() {
            CommandContext context = mock(CommandContext.class);
            List<String> completions = subCommand.tabComplete(context);
            assertNotNull(completions);
            assertEquals(2, completions.size());
            assertTrue(completions.contains("option1"));
            assertTrue(completions.contains("option2"));
        }

        @Test
        @DisplayName("Should require player when specified")
        void shouldRequirePlayerWhenSpecified() {
            assertTrue(subCommand.requiresPlayer());
        }
    }

    // Simple test implementation
    private static class TestSubCommand implements SubCommand {
        @Override
        public String getName() {
            return "test";
        }

        @Override
        public String getPermission() {
            return "test.permission";
        }

        @Override
        public String getUsage() {
            return "test";
        }

        @Override
        public String getDescription() {
            return "Test command";
        }

        @Override
        public void execute(CommandContext context) {
            // Do nothing for test
        }
    }

    // Custom implementation with overridden defaults
    private static class CustomSubCommand implements SubCommand {
        @Override
        public String getName() {
            return "custom";
        }

        @Override
        public String getPermission() {
            return "custom.permission";
        }

        @Override
        public String getUsage() {
            return "custom [option]";
        }

        @Override
        public String getDescription() {
            return "Custom command with options";
        }

        @Override
        public List<String> getAliases() {
            return List.of("alias1", "alias2");
        }

        @Override
        public List<String> tabComplete(CommandContext context) {
            return List.of("option1", "option2");
        }

        @Override
        public boolean requiresPlayer() {
            return true;
        }

        @Override
        public void execute(CommandContext context) {
            // Do nothing for test
        }
    }
}