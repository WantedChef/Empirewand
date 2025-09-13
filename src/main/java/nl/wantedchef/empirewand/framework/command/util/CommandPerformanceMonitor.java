package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Advanced performance monitoring system for commands including execution timing,
 * frequency tracking, error rates, and performance alerting. Provides detailed
 * metrics for optimization and debugging.
 */
public class CommandPerformanceMonitor {
    
    private final Logger logger;
    private final Map<String, CommandMetrics> commandMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> activeExecutions = new ConcurrentHashMap<>();
    
    // Performance thresholds
    private static final long SLOW_COMMAND_THRESHOLD_MS = 100L;
    private static final long VERY_SLOW_COMMAND_THRESHOLD_MS = 500L;
    private static final int ERROR_RATE_ALERT_THRESHOLD = 10; // 10% error rate
    private static final int FREQUENCY_ALERT_THRESHOLD = 100; // 100 executions per minute
    
    public CommandPerformanceMonitor(@NotNull EmpireWandPlugin plugin) {
        this.logger = plugin.getLogger();
        
        // Schedule periodic performance reporting
        if (plugin.getServer() != null && plugin.getServer().getScheduler() != null) {
            plugin.getServer().getScheduler().runTaskTimer(plugin, this::performPerformanceCheck,
                20L * 60L, 20L * 60L); // Every minute
        }
    }
    
    /**
     * Records the start of a command execution.
     * 
     * @param commandName The name of the command
     * @param playerName The name of the player executing the command
     * @return Execution context for timing
     */
    @NotNull
    public ExecutionContext startExecution(@NotNull String commandName, @NotNull String playerName) {
        commandMetrics.computeIfAbsent(commandName, k -> new CommandMetrics()).recordStart();
        activeExecutions.computeIfAbsent(commandName, k -> new AtomicLong(0)).incrementAndGet();
        
        return new ExecutionContext(commandName, playerName, System.nanoTime());
    }
    
    /**
     * Records the completion of a command execution.
     * 
     * @param context The execution context from startExecution
     * @param success Whether the command executed successfully
     * @param errorType Optional error type if command failed
     */
    public void recordExecution(@NotNull ExecutionContext context, boolean success, @NotNull String errorType) {
        long executionTimeMs = (System.nanoTime() - context.startTime) / 1_000_000;
        
        CommandMetrics metrics = commandMetrics.get(context.commandName);
        if (metrics != null) {
            metrics.recordCompletion(executionTimeMs, success, errorType);
            
            // Alert on slow executions
            if (executionTimeMs > VERY_SLOW_COMMAND_THRESHOLD_MS) {
                logger.warning(String.format("Very slow command execution: %s by %s took %dms", 
                    context.commandName, context.playerName, executionTimeMs));
            } else if (executionTimeMs > SLOW_COMMAND_THRESHOLD_MS) {
                logger.info(String.format("Slow command execution: %s by %s took %dms", 
                    context.commandName, context.playerName, executionTimeMs));
            }
        }
        
        activeExecutions.get(context.commandName).decrementAndGet();
    }
    
    /**
     * Gets performance metrics for a specific command.
     * 
     * @param commandName The command name
     * @return Command metrics or null if not found
     */
    public CommandMetrics getCommandMetrics(@NotNull String commandName) {
        return commandMetrics.get(commandName);
    }
    
    /**
     * Gets performance metrics for all commands.
     * 
     * @return Map of command names to metrics
     */
    @NotNull
    public Map<String, CommandMetrics> getAllMetrics() {
        return new HashMap<>(commandMetrics);
    }
    
    /**
     * Gets the top N slowest commands by average execution time.
     * 
     * @param count Number of commands to return
     * @return List of command names sorted by average execution time (slowest first)
     */
    @NotNull
    public List<String> getSlowestCommands(int count) {
        return commandMetrics.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue().getAverageExecutionTimeMs(), 
                                             e1.getValue().getAverageExecutionTimeMs()))
            .limit(count)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets the most frequently executed commands.
     * 
     * @param count Number of commands to return
     * @return List of command names sorted by execution count (highest first)
     */
    @NotNull
    public List<String> getMostFrequentCommands(int count) {
        return commandMetrics.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().getTotalExecutions(), 
                                           e1.getValue().getTotalExecutions()))
            .limit(count)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets commands with high error rates.
     * 
     * @param minimumExecutions Minimum number of executions to consider
     * @return List of command names with high error rates
     */
    @NotNull
    public List<String> getHighErrorRateCommands(int minimumExecutions) {
        return commandMetrics.entrySet().stream()
            .filter(e -> e.getValue().getTotalExecutions() >= minimumExecutions)
            .filter(e -> e.getValue().getErrorRate() >= ERROR_RATE_ALERT_THRESHOLD)
            .sorted((e1, e2) -> Double.compare(e2.getValue().getErrorRate(), 
                                             e1.getValue().getErrorRate()))
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Resets all performance metrics. Useful for benchmarking specific periods.
     */
    public void resetMetrics() {
        commandMetrics.clear();
        activeExecutions.clear();
        logger.info("Command performance metrics reset");
    }
    
    /**
     * Generates a performance report summary.
     * 
     * @return Performance report as a formatted string
     */
    @NotNull
    public String generatePerformanceReport() {
        if (commandMetrics.isEmpty()) {
            return "No command execution data available";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("=== Command Performance Report ===\n");
        
        // Overall statistics
        long totalExecutions = commandMetrics.values().stream()
            .mapToLong(CommandMetrics::getTotalExecutions)
            .sum();
        long totalErrors = commandMetrics.values().stream()
            .mapToLong(CommandMetrics::getTotalErrors)
            .sum();
        double overallErrorRate = totalExecutions > 0 ? (double) totalErrors / totalExecutions * 100 : 0;
        
        report.append(String.format("Total Executions: %d\n", totalExecutions));
        report.append(String.format("Total Errors: %d (%.2f%%)\n", totalErrors, overallErrorRate));
        report.append(String.format("Unique Commands: %d\n", commandMetrics.size()));
        
        // Top slowest commands
        List<String> slowest = getSlowestCommands(5);
        if (!slowest.isEmpty()) {
            report.append("\nTop 5 Slowest Commands (avg ms):\n");
            for (int i = 0; i < slowest.size(); i++) {
                String cmd = slowest.get(i);
                CommandMetrics metrics = commandMetrics.get(cmd);
                report.append(String.format("%d. %s: %.2f ms (%d executions)\n", 
                    i + 1, cmd, metrics.getAverageExecutionTimeMs(), metrics.getTotalExecutions()));
            }
        }
        
        // Most frequent commands
        List<String> frequent = getMostFrequentCommands(5);
        if (!frequent.isEmpty()) {
            report.append("\nTop 5 Most Frequent Commands:\n");
            for (int i = 0; i < frequent.size(); i++) {
                String cmd = frequent.get(i);
                CommandMetrics metrics = commandMetrics.get(cmd);
                report.append(String.format("%d. %s: %d executions (%.2f ms avg)\n", 
                    i + 1, cmd, metrics.getTotalExecutions(), metrics.getAverageExecutionTimeMs()));
            }
        }
        
        // High error rate commands
        List<String> highErrorRate = getHighErrorRateCommands(10);
        if (!highErrorRate.isEmpty()) {
            report.append("\nCommands with High Error Rates:\n");
            for (String cmd : highErrorRate) {
                CommandMetrics metrics = commandMetrics.get(cmd);
                report.append(String.format("- %s: %.2f%% error rate (%d/%d)\n", 
                    cmd, metrics.getErrorRate(), metrics.getTotalErrors(), metrics.getTotalExecutions()));
            }
        }
        
        return report.toString();
    }
    
    private void performPerformanceCheck() {
        try {
            // Check for commands with high error rates
            List<String> highErrorCommands = getHighErrorRateCommands(20);
            if (!highErrorCommands.isEmpty()) {
                logger.warning("Commands with high error rates detected: " + 
                    String.join(", ", highErrorCommands));
            }
            
            // Check for frequently executed commands that might need optimization
            commandMetrics.entrySet().stream()
                .filter(e -> e.getValue().getRecentExecutionRate() > FREQUENCY_ALERT_THRESHOLD)
                .forEach(e -> logger.info(String.format("High frequency command: %s (%d executions/min)", 
                    e.getKey(), e.getValue().getRecentExecutionRate())));
            
            // Log performance summary every 5 minutes
            if (System.currentTimeMillis() % (5 * 60 * 1000) < 60 * 1000) {
                String summary = generatePerformanceReport();
                logger.log(Level.INFO, "Performance Summary:\n" + summary);
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during performance check", e);
        }
    }
    
    /**
     * Execution context for tracking individual command executions.
     */
    public static class ExecutionContext {
        public final String commandName;
        public final String playerName;
        public final long startTime;
        
        ExecutionContext(@NotNull String commandName, @NotNull String playerName, long startTime) {
            this.commandName = commandName;
            this.playerName = playerName;
            this.startTime = startTime;
        }
    }
    
    /**
     * Metrics data for a specific command.
     */
    public static class CommandMetrics {
        private long totalExecutions = 0;
        private long totalErrors = 0;
        private long totalExecutionTimeMs = 0;
        private long minExecutionTimeMs = Long.MAX_VALUE;
        private long maxExecutionTimeMs = 0;
        private final Map<String, Long> errorTypes = new ConcurrentHashMap<>();
        private final Queue<Instant> recentExecutions = new LinkedList<>();
        
        synchronized void recordStart() {
            recentExecutions.offer(Instant.now());
            
            // Keep only last 10 minutes of executions for rate calculation
            Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);
            recentExecutions.removeIf(time -> time.isBefore(cutoff));
        }
        
        synchronized void recordCompletion(long executionTimeMs, boolean success, @NotNull String errorType) {
            totalExecutions++;
            totalExecutionTimeMs += executionTimeMs;
            
            if (executionTimeMs < minExecutionTimeMs) {
                minExecutionTimeMs = executionTimeMs;
            }
            if (executionTimeMs > maxExecutionTimeMs) {
                maxExecutionTimeMs = executionTimeMs;
            }
            
            if (!success) {
                totalErrors++;
                errorTypes.merge(errorType, 1L, Long::sum);
            }
        }
        
        public long getTotalExecutions() { return totalExecutions; }
        public long getTotalErrors() { return totalErrors; }
        public double getErrorRate() { 
            return totalExecutions > 0 ? (double) totalErrors / totalExecutions * 100 : 0; 
        }
        public double getAverageExecutionTimeMs() { 
            return totalExecutions > 0 ? (double) totalExecutionTimeMs / totalExecutions : 0; 
        }
        public long getMinExecutionTimeMs() { 
            return minExecutionTimeMs == Long.MAX_VALUE ? 0 : minExecutionTimeMs; 
        }
        public long getMaxExecutionTimeMs() { return maxExecutionTimeMs; }
        public Map<String, Long> getErrorTypes() { return new HashMap<>(errorTypes); }
        
        public synchronized int getRecentExecutionRate() {
            Instant cutoff = Instant.now().minus(1, ChronoUnit.MINUTES);
            return (int) recentExecutions.stream().filter(time -> time.isAfter(cutoff)).count();
        }
    }
}