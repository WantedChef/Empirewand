package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages command cooldowns and rate limiting to prevent abuse.
 * Provides both global and per-player command cooldowns.
 */
public class CommandCooldownManager {
    private final EmpireWandPlugin plugin;
    private final Map<String, Long> globalCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();

    public CommandCooldownManager(@NotNull EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if a command is on cooldown for a specific player.
     *
     * @param sender The command sender
     * @param commandName The command name
     * @param cooldownSeconds The cooldown duration in seconds
     * @throws CommandException if the command is on cooldown
     */
    public void checkCooldown(@NotNull CommandSender sender, @NotNull String commandName, int cooldownSeconds) throws CommandException {
        if (cooldownSeconds <= 0) {
            return; // No cooldown
        }

        long now = System.currentTimeMillis();
        long cooldownMs = cooldownSeconds * 1000L;
        String cooldownKey = commandName.toLowerCase();

        if (sender instanceof Player player) {
            UUID playerId = player.getUniqueId();
            Map<String, Long> playerCooldownsMap = playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
            
            Long cooldownEnd = playerCooldownsMap.get(cooldownKey);
            if (cooldownEnd != null && now < cooldownEnd) {
                long remainingSeconds = (cooldownEnd - now) / 1000;
                throw new CommandException(
                    String.format("This command is on cooldown. Please wait %d second%s.", 
                        remainingSeconds, remainingSeconds != 1 ? "s" : ""),
                    "COMMAND_COOLDOWN",
                    commandName, remainingSeconds
                );
            }
            
            // Set new cooldown
            playerCooldownsMap.put(cooldownKey, now + cooldownMs);
        } else {
            // Global cooldown for console commands
            Long cooldownEnd = globalCooldowns.get(cooldownKey);
            if (cooldownEnd != null && now < cooldownEnd) {
                long remainingSeconds = (cooldownEnd - now) / 1000;
                throw new CommandException(
                    String.format("This command is on cooldown. Please wait %d second%s.", 
                        remainingSeconds, remainingSeconds != 1 ? "s" : ""),
                    "COMMAND_COOLDOWN",
                    commandName, remainingSeconds
                );
            }
            
            // Set new cooldown
            globalCooldowns.put(cooldownKey, now + cooldownMs);
        }
    }

    /**
     * Checks if a command is on cooldown for a specific player, with custom cooldown key.
     *
     * @param sender The command sender
     * @param commandName The command name
     * @param cooldownKey The cooldown key (for differentiating command variants)
     * @param cooldownSeconds The cooldown duration in seconds
     * @throws CommandException if the command is on cooldown
     */
    public void checkCooldown(@NotNull CommandSender sender, @NotNull String commandName, 
                             @NotNull String cooldownKey, int cooldownSeconds) throws CommandException {
        if (cooldownSeconds <= 0) {
            return; // No cooldown
        }

        long now = System.currentTimeMillis();
        long cooldownMs = cooldownSeconds * 1000L;
        String fullCooldownKey = (commandName + "." + cooldownKey).toLowerCase();

        if (sender instanceof Player player) {
            UUID playerId = player.getUniqueId();
            Map<String, Long> playerCooldownsMap = playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
            
            Long cooldownEnd = playerCooldownsMap.get(fullCooldownKey);
            if (cooldownEnd != null && now < cooldownEnd) {
                long remainingSeconds = (cooldownEnd - now) / 1000;
                throw new CommandException(
                    String.format("This command is on cooldown. Please wait %d second%s.", 
                        remainingSeconds, remainingSeconds != 1 ? "s" : ""),
                    "COMMAND_COOLDOWN",
                    commandName, remainingSeconds
                );
            }
            
            // Set new cooldown
            playerCooldownsMap.put(fullCooldownKey, now + cooldownMs);
        } else {
            // Global cooldown for console commands
            Long cooldownEnd = globalCooldowns.get(fullCooldownKey);
            if (cooldownEnd != null && now < cooldownEnd) {
                long remainingSeconds = (cooldownEnd - now) / 1000;
                throw new CommandException(
                    String.format("This command is on cooldown. Please wait %d second%s.", 
                        remainingSeconds, remainingSeconds != 1 ? "s" : ""),
                    "COMMAND_COOLDOWN",
                    commandName, remainingSeconds
                );
            }
            
            // Set new cooldown
            globalCooldowns.put(fullCooldownKey, now + cooldownMs);
        }
    }

    /**
     * Clears all cooldowns for a specific player.
     *
     * @param playerId The player's UUID
     */
    public void clearPlayerCooldowns(@NotNull UUID playerId) {
        playerCooldowns.remove(playerId);
    }

    /**
     * Clears a specific cooldown for a player.
     *
     * @param playerId The player's UUID
     * @param commandName The command name
     */
    public void clearPlayerCooldown(@NotNull UUID playerId, @NotNull String commandName) {
        Map<String, Long> playerCooldownsMap = playerCooldowns.get(playerId);
        if (playerCooldownsMap != null) {
            playerCooldownsMap.remove(commandName.toLowerCase());
        }
    }

    /**
     * Clears all global cooldowns.
     */
    public void clearGlobalCooldowns() {
        globalCooldowns.clear();
    }

    /**
     * Clears a specific global cooldown.
     *
     * @param commandName The command name
     */
    public void clearGlobalCooldown(@NotNull String commandName) {
        globalCooldowns.remove(commandName.toLowerCase());
    }
}