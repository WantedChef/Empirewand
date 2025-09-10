package nl.wantedchef.empirewand.core.util.performance;

import nl.wantedchef.empirewand.core.EmpireWandPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Performance monitor for tracking command and spell execution times.
 * 
 * @since 2.0.0
 */
public class PerformanceMonitor {

    private final EmpireWandPlugin plugin;
    private long commandStartTime;

    /**
     * Constructs a new PerformanceMonitor.
     * 
     * @param plugin the plugin instance
     */
    public PerformanceMonitor(@NotNull EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.commandStartTime = 0;
    }

    /**
     * Starts timing for command execution.
     */
    public void startCommandTiming() {
        this.commandStartTime = System.nanoTime();
    }

    /**
     * Records command execution time and logs performance metrics.
     * 
     * @param commandName the name of the command
     */
    public void recordCommandExecution(@NotNull String commandName) {
        if (commandStartTime == 0) {
            plugin.getLogger().warning("recordCommandExecution called without startCommandTiming for " + commandName);
            return;
        }
        
        long executionTimeMs = (System.nanoTime() - commandStartTime) / 1_000_000;
        plugin.getLogger().info(String.format("Command '%s' executed in %d ms", commandName, executionTimeMs));
        
        // Reset for next command
        this.commandStartTime = 0;
    }

    /**
     * Records command execution time without logging.
     * 
     * @param commandName the name of the command
     * @return the execution time in milliseconds
     */
    public long recordCommandExecutionSilent(@NotNull String commandName) {
        if (commandStartTime == 0) {
            return 0;
        }
        
        long executionTimeMs = (System.nanoTime() - commandStartTime) / 1_000_000;
        this.commandStartTime = 0;
        return executionTimeMs;
    }
}