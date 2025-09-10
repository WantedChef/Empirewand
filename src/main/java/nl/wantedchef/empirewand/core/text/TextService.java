package nl.wantedchef.empirewand.core.text;

/**
 * Text service for formatting and processing messages.
 */
public class TextService {
    
    /**
     * Formats a message with color codes.
     */
    public String format(String message) {
        return message; // Simplified implementation
    }
    
    /**
     * Converts a message to a component.
     */
    public Object toComponent(String message) {
        return message; // Simplified implementation
    }
    
    /**
     * Strips color codes from a message.
     */
    public String stripColors(String message) {
        return message.replaceAll("§[0-9a-fk-or]", "");
    }
}