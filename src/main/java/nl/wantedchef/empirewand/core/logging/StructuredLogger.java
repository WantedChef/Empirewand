package nl.wantedchef.empirewand.core.logging;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Structured logging utility for consistent, searchable log messages.
 * 
 * <p>This class provides structured logging capabilities that make it easier to
 * search, filter, and analyze log messages. All log messages follow a consistent
 * format with key-value pairs for easy parsing.</p>
 * 
 * <p><strong>Log Format:</strong></p>
 * <pre>
 * [CATEGORY] MESSAGE | key1=value1 | key2=value2 | ...
 * </pre>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * StructuredLogger logger = new StructuredLogger(plugin.getLogger());
 * 
 * // Spell casting
 * logger.logSpellCast(player, "fireball", 1500L, true);
 * 
 * // Performance metrics
 * logger.logPerformance("spell_execution", 25L, Map.of("spell", "lightning"));
 * 
 * // Error with context
 * logger.logError("spell_failed", "Spell execution failed", 
 *     Map.of("player", player.getName(), "spell", "fireball"));
 * }</pre>
 * 
 * @since 1.1.1
 * @author EmpireWand Team
 */
public class StructuredLogger {
    
    private final Logger logger;
    
    /**
     * Creates a new structured logger.
     * 
     * @param logger the underlying logger to use
     */
    public StructuredLogger(@NotNull Logger logger) {
        this.logger = logger;
    }
    
    // ==================== Spell-related Logging ====================
    
    /**
     * Logs a spell cast event.
     * 
     * @param player the player who cast the spell
     * @param spellKey the spell key
     * @param executionTimeMs the execution time in milliseconds
     * @param success whether the spell was successful
     */
    public void logSpellCast(@NotNull Player player, @NotNull String spellKey, long executionTimeMs, boolean success) {
        Map<String, Object> context = new HashMap<>();
        context.put("player", player.getName());
        context.put("player_uuid", player.getUniqueId().toString());
        context.put("spell", spellKey);
        context.put("execution_time_ms", executionTimeMs);
        context.put("success", success);
        context.put("world", player.getWorld().getName());
        context.put("location", formatLocation(player.getLocation()));
        
        log(Level.INFO, "SPELL_CAST", "Spell cast: " + spellKey, context);
    }
    
    /**
     * Logs a spell failure event.
     * 
     * @param player the player who attempted to cast the spell
     * @param spellKey the spell key
     * @param reason the failure reason
     * @param errorMessage the error message
     */
    public void logSpellFailure(@NotNull Player player, @NotNull String spellKey, 
                               @NotNull String reason, @Nullable String errorMessage) {
        Map<String, Object> context = new HashMap<>();
        context.put("player", player.getName());
        context.put("player_uuid", player.getUniqueId().toString());
        context.put("spell", spellKey);
        context.put("reason", reason);
        context.put("world", player.getWorld().getName());
        context.put("location", formatLocation(player.getLocation()));
        
        if (errorMessage != null) {
            context.put("error", errorMessage);
        }
        
        log(Level.WARNING, "SPELL_FAILURE", "Spell failed: " + spellKey + " (" + reason + ")", context);
    }
    
    /**
     * Logs a cooldown event.
     * 
     * @param player the player
     * @param spellKey the spell key
     * @param remainingMs the remaining cooldown time in milliseconds
     */
    public void logCooldown(@NotNull Player player, @NotNull String spellKey, long remainingMs) {
        Map<String, Object> context = new HashMap<>();
        context.put("player", player.getName());
        context.put("player_uuid", player.getUniqueId().toString());
        context.put("spell", spellKey);
        context.put("remaining_ms", remainingMs);
        
        log(Level.INFO, "COOLDOWN", "Cooldown active for spell: " + spellKey, context);
    }
    
    // ==================== Performance Logging ====================
    
    /**
     * Logs performance metrics.
     * 
     * @param operation the operation name
     * @param durationMs the duration in milliseconds
     * @param additionalContext additional context data
     */
    public void logPerformance(@NotNull String operation, long durationMs, @Nullable Map<String, Object> additionalContext) {
        Map<String, Object> context = new HashMap<>();
        context.put("operation", operation);
        context.put("duration_ms", durationMs);
        
        if (additionalContext != null) {
            context.putAll(additionalContext);
        }
        
        Level level = durationMs > 1000 ? Level.WARNING : Level.INFO;
        log(level, "PERFORMANCE", "Performance: " + operation, context);
    }
    
    /**
     * Logs a slow operation warning.
     * 
     * @param operation the operation name
     * @param durationMs the duration in milliseconds
     * @param thresholdMs the threshold that was exceeded
     */
    public void logSlowOperation(@NotNull String operation, long durationMs, long thresholdMs) {
        Map<String, Object> context = new HashMap<>();
        context.put("operation", operation);
        context.put("duration_ms", durationMs);
        context.put("threshold_ms", thresholdMs);
        context.put("excess_ms", durationMs - thresholdMs);
        
        log(Level.WARNING, "SLOW_OPERATION", "Slow operation detected: " + operation, context);
    }
    
    // ==================== System Logging ====================
    
    /**
     * Logs a system event.
     * 
     * @param event the event name
     * @param message the message
     * @param additionalContext additional context data
     */
    public void logSystemEvent(@NotNull String event, @NotNull String message, @Nullable Map<String, Object> additionalContext) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", event);
        
        if (additionalContext != null) {
            context.putAll(additionalContext);
        }
        
        log(Level.INFO, "SYSTEM", message, context);
    }
    
    /**
     * Logs an error with structured context.
     * 
     * @param errorType the error type
     * @param message the error message
     * @param additionalContext additional context data
     */
    public void logError(@NotNull String errorType, @NotNull String message, @Nullable Map<String, Object> additionalContext) {
        Map<String, Object> context = new HashMap<>();
        context.put("error_type", errorType);
        
        if (additionalContext != null) {
            context.putAll(additionalContext);
        }
        
        log(Level.SEVERE, "ERROR", message, context);
    }
    
    /**
     * Logs a warning with structured context.
     * 
     * @param warningType the warning type
     * @param message the warning message
     * @param additionalContext additional context data
     */
    public void logWarning(@NotNull String warningType, @NotNull String message, @Nullable Map<String, Object> additionalContext) {
        Map<String, Object> context = new HashMap<>();
        context.put("warning_type", warningType);
        
        if (additionalContext != null) {
            context.putAll(additionalContext);
        }
        
        log(Level.WARNING, "WARNING", message, context);
    }
    
    // ==================== Configuration Logging ====================
    
    /**
     * Logs a configuration event.
     * 
     * @param event the configuration event
     * @param message the message
     * @param additionalContext additional context data
     */
    public void logConfigEvent(@NotNull String event, @NotNull String message, @Nullable Map<String, Object> additionalContext) {
        Map<String, Object> context = new HashMap<>();
        context.put("config_event", event);
        
        if (additionalContext != null) {
            context.putAll(additionalContext);
        }
        
        log(Level.INFO, "CONFIG", message, context);
    }
    
    /**
     * Logs a configuration reload event.
     * 
     * @param configFile the configuration file that was reloaded
     * @param success whether the reload was successful
     * @param durationMs the reload duration in milliseconds
     */
    public void logConfigReload(@NotNull String configFile, boolean success, long durationMs) {
        Map<String, Object> context = new HashMap<>();
        context.put("config_file", configFile);
        context.put("success", success);
        context.put("duration_ms", durationMs);
        
        String message = "Configuration reload: " + configFile + " (" + (success ? "success" : "failed") + ")";
        log(success ? Level.INFO : Level.WARNING, "CONFIG_RELOAD", message, context);
    }
    
    // ==================== Command Logging ====================
    
    /**
     * Logs a command execution.
     * 
     * @param player the player who executed the command
     * @param command the command that was executed
     * @param args the command arguments
     * @param success whether the command was successful
     * @param executionTimeMs the execution time in milliseconds
     */
    public void logCommand(@NotNull Player player, @NotNull String command, @NotNull String[] args, 
                          boolean success, long executionTimeMs) {
        Map<String, Object> context = new HashMap<>();
        context.put("player", player.getName());
        context.put("player_uuid", player.getUniqueId().toString());
        context.put("command", command);
        context.put("args", String.join(" ", args));
        context.put("success", success);
        context.put("execution_time_ms", executionTimeMs);
        
        log(Level.INFO, "COMMAND", "Command executed: " + command, context);
    }
    
    // ==================== Core Logging Method ====================
    
    /**
     * Core logging method that formats and outputs structured log messages.
     * 
     * @param level the log level
     * @param category the log category
     * @param message the log message
     * @param context the context data
     */
    private void log(@NotNull Level level, @NotNull String category, @NotNull String message, @NotNull Map<String, Object> context) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[").append(category).append("] ").append(message);
        
        if (!context.isEmpty()) {
            logMessage.append(" |");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                logMessage.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        
        logger.log(level, logMessage.toString());
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Formats a location for logging.
     * 
     * @param location the location to format
     * @return the formatted location string
     */
    @NotNull
    private String formatLocation(@NotNull org.bukkit.Location location) {
        return String.format("%.2f,%.2f,%.2f", location.getX(), location.getY(), location.getZ());
    }
    
    /**
     * Creates a context map with common fields.
     * 
     * @param player the player
     * @return a context map with player information
     */
    @NotNull
    public Map<String, Object> createPlayerContext(@NotNull Player player) {
        Map<String, Object> context = new HashMap<>();
        context.put("player", player.getName());
        context.put("player_uuid", player.getUniqueId().toString());
        context.put("world", player.getWorld().getName());
        context.put("location", formatLocation(player.getLocation()));
        return context;
    }
    
    /**
     * Creates a context map with spell information.
     * 
     * @param spellKey the spell key
     * @return a context map with spell information
     */
    @NotNull
    public Map<String, Object> createSpellContext(@NotNull String spellKey) {
        Map<String, Object> context = new HashMap<>();
        context.put("spell", spellKey);
        return context;
    }
    
    /**
     * Creates a context map with timing information.
     * 
     * @param durationMs the duration in milliseconds
     * @return a context map with timing information
     */
    @NotNull
    public Map<String, Object> createTimingContext(long durationMs) {
        Map<String, Object> context = new HashMap<>();
        context.put("duration_ms", durationMs);
        context.put("timestamp", System.currentTimeMillis());
        return context;
    }
}
