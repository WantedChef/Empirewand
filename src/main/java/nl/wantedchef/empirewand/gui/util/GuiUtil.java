package nl.wantedchef.empirewand.gui.util;

import org.jetbrains.annotations.NotNull;

/**
 * Utility methods for GUI components.
 */
public final class GuiUtil {

    private GuiUtil() {
        // Private constructor for utility class
    }

    /**
     * Formats a snake_case wand key into a Title Case display name.
     * Example: "empire_wand" -> "Empire Wand"
     *
     * @param wandKey The wand key to format.
     * @return The formatted display name.
     */
    @NotNull
    public static String formatWandDisplayName(@NotNull String wandKey) {
        // Convert snake_case to Title Case
        String[] parts = wandKey.replace('_', ' ').split(" ");
        StringBuilder displayName = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                displayName.append(" ");
            }
            String part = parts[i];
            if (!part.isEmpty()) {
                displayName.append(part.substring(0, 1).toUpperCase())
                          .append(part.substring(1).toLowerCase());
            }
        }

        return displayName.toString();
    }
}
