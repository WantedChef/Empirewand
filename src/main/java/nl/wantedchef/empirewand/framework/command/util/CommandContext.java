package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;

/**
 * Context for command execution, providing access to sender, plugin services, and timing.
 * 
 * @since 2.0.0
 */
public class CommandContext {

    private final @NotNull EmpireWandPlugin plugin;
    private final @NotNull CommandSender sender;
    private final @NotNull ConfigService config;
    private long startTime;

    /**
     * Constructs a new CommandContext.
     * 
     * @param plugin the plugin instance
     * @param sender the command sender
     * @param config the config service
     */
    public CommandContext(@NotNull EmpireWandPlugin plugin, @NotNull CommandSender sender, @NotNull ConfigService config) {
        this.plugin = plugin;
        this.sender = sender;
        this.config = config;
        this.startTime = 0;
    }

    /**
     * Gets the plugin instance.
     * 
     * @return the plugin
     */
    public @NotNull EmpireWandPlugin plugin() {
        return plugin;
    }

    /**
     * Gets the command sender.
     * 
     * @return the sender
     */
    public @NotNull CommandSender getSender() {
        return sender;
    }

    /**
     * Gets the config service.
     * 
     * @return the config service
     */
    public @NotNull ConfigService config() {
        return config;
    }

    /**
     * Sends a message to the sender.
     * 
     * @param message the message component
     */
    public void sendMessage(@NotNull Component message) {
        sender.sendMessage(message);
    }

    /**
     * Sends a plain text message to the sender.
     * 
     * @param message the plain text message
     */
    public void sendMessage(@NotNull String message) {
        sender.sendMessage(message);
    }

    /**
     * Starts timing for command execution.
     */
    public void startTiming() {
        this.startTime = System.nanoTime();
    }

    /**
     * Gets the elapsed time since startTiming in milliseconds.
     * 
     * @return the elapsed time
     */
    public long getElapsedTimeMs() {
        if (startTime == 0) {
            return 0;
        }
        return (System.nanoTime() - startTime) / 1_000_000;
    }
}