package nl.wantedchef.empirewand.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * Centralized text formatting and processing service using Adventure/MiniMessage.
 * Handles color codes, formatting, text transformations, and internationalization.
 */
public class TextService {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final Pattern MINI_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final ResourceBundle messages;

    /**
     * Creates a TextService with default locale (English).
     */
    public TextService() {
        this(Locale.ENGLISH);
    }

    /**
     * Creates a TextService with specified locale.
     *
     * @param locale the locale for message localization
     */
    public TextService(Locale locale) {
        this.messages = ResourceBundle.getBundle("messages", locale);
    }

    /**
     * Converts MiniMessage formatted string to Component.
     *
     * @param miniMessage the MiniMessage formatted string
     * @return the Component representation
     */
    public Component parseMiniMessage(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(miniMessage);
    }

    /**
     * Converts Component to MiniMessage formatted string.
     *
     * @param component the Component to serialize
     * @return the MiniMessage formatted string
     */
    public String serializeMiniMessage(Component component) {
        if (component == null) {
            return "";
        }
        return MINI_MESSAGE.serialize(component);
    }

    /**
     * Strips MiniMessage tags from a string, leaving only plain text.
     *
     * @param text the text to strip tags from
     * @return the plain text without MiniMessage tags
     */
    public String stripMiniTags(String text) {
        if (text == null) {
            return "";
        }
        return MINI_TAG_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Converts legacy color codes (&) to MiniMessage format.
     *
     * @param legacyText the text with legacy color codes
     * @return the text with MiniMessage color tags
     */
    public String legacyToMiniMessage(String legacyText) {
        if (legacyText == null) {
            return "";
        }
        // Convert & to § first, then deserialize with legacy serializer
        String translated = legacyText.replace('&', '§');
        Component component = LEGACY_SERIALIZER.deserialize(translated);
        return MINI_MESSAGE.serialize(component);
    }

    /**
     * Creates a colored Component from plain text.
     *
     * @param text the plain text
     * @param color the color to apply
     * @return the colored Component
     */
    public Component coloredText(String text, NamedTextColor color) {
        if (text == null) {
            return Component.empty();
        }
        return Component.text(text).color(color);
    }

    /**
     * Creates a colored Component from plain text with hex color.
     *
     * @param text the plain text
     * @param hexColor the hex color code (e.g., "#FF0000")
     * @return the colored Component
     */
    public Component coloredText(String text, String hexColor) {
        if (text == null) {
            return Component.empty();
        }
        return Component.text(text).color(TextColor.fromHexString(hexColor));
    }

    /**
     * Gets a localized message by key.
     *
     * @param key the message key
     * @return the localized message, or the key if not found
     */
    public String getMessage(String key) {
        try {
            return messages.getString(key);
        } catch (Exception e) {
            return key; // Fallback to key if message not found
        }
    }

    /**
     * Gets a localized message by key with placeholder replacement.
     *
     * @param key the message key
     * @param placeholders the placeholder map
     * @return the localized and formatted message
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        return applyPlaceholders(message, placeholders);
    }

    /**
     * Applies placeholders to a template string.
     *
     * @param template the template string with {placeholder} markers
     * @param placeholders the placeholder map
     * @return the string with placeholders replaced
     */
    public String applyPlaceholders(String template, Map<String, String> placeholders) {
        if (template == null) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    /**
     * Formats a message key with placeholders and converts to Component.
     * Uses localized messages from ResourceBundle.
     *
     * @param messageKey the message key
     * @param placeholders the placeholder map
     * @return the formatted Component
     */
    public Component formatMessage(String messageKey, Map<String, String> placeholders) {
        String message = getMessage(messageKey, placeholders);
        return parseMiniMessage(message);
    }

    /**
     * Gets the plain text representation of a Component (for legacy compatibility).
     *
     * @param component the Component
     * @return the plain text
     */
    public String componentToPlainText(Component component) {
        if (component == null) {
            return "";
        }
        return LEGACY_SERIALIZER.serialize(component);
    }
}





