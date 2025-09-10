package nl.wantedchef.empirewand.core.text;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

/**
 * Service for handling text formatting and messages.
 */
public class TextService {
    
    @NotNull
    public String colorize(@NotNull String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    @NotNull  
    public String format(@NotNull String template, Object... args) {
        return String.format(colorize(template), args);
    }
    
    @NotNull
    public String stripColor(@NotNull String message) {
        return ChatColor.stripColor(message);
    }
}
