package nl.wantedchef.empirewand.command.admin;

import nl.wantedchef.empirewand.framework.command.BaseWandCommand;
import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandCache;
import nl.wantedchef.empirewand.framework.command.util.CommandErrorHandler;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Administrative command for viewing and managing command system performance.
 * Provides detailed metrics about command execution, caching effectiveness,
 * and system optimization insights.
 */
public class PerformanceCommand implements SubCommand, CommandHelpProvider.HelpAwareCommand {
    
    private final String wandType;
    private static final TextColor HEADER_COLOR = TextColor.fromHexString("#FFD700");
    private static final TextColor METRIC_COLOR = TextColor.fromHexString("#00BFFF");
    private static final TextColor VALUE_COLOR = TextColor.fromHexString("#32CD32");
    private static final TextColor WARNING_COLOR = TextColor.fromHexString("#FFA500");
    
    public PerformanceCommand(String wandType) {
        this.wandType = wandType;
    }
    
    @Override
    public @NotNull String getName() {
        return "performance";
    }
    
    @Override
    public @NotNull List<String> getAliases() {
        return List.of("perf", "metrics", "stats");
    }
    
    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.performance";
    }
    
    @Override
    public @NotNull String getUsage() {
        return "performance [cache|reset|report]";
    }
    
    @Override
    public @NotNull String getDescription() {
        return "View command system performance metrics and cache statistics";
    }
    
    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        // This command requires elevated permissions - admin only
        if (!context.hasPermission(wandType + ".admin")) {
            throw new CommandException("This command requires admin permissions", "NO_ADMIN_PERMISSION");
        }
        
        String[] args = context.args();
        String subAction = args.length > 1 ? args[1].toLowerCase() : "report";
        
        // Try to get the BaseWandCommand instance for metrics
        BaseWandCommand wandCommand = getWandCommand(context);
        if (wandCommand == null) {
            throw new CommandException("Unable to access command metrics", "METRICS_UNAVAILABLE");
        }
        
        switch (subAction) {
            case "cache" -> showCacheStatistics(context, wandCommand);
            case "reset" -> resetMetrics(context, wandCommand);
            case "report" -> showPerformanceReport(context, wandCommand);
            default -> throw new CommandException("Unknown performance action: " + subAction + 
                ". Valid actions: cache, reset, report", "INVALID_PERF_ACTION", subAction);
        }
    }
    
    private void showCacheStatistics(@NotNull CommandContext context, @NotNull BaseWandCommand wandCommand) {
        CommandCache.CacheStats stats = wandCommand.getCacheStats();
        
        Component header = Component.text()
            .append(Component.text("📊 ", HEADER_COLOR))
            .append(Component.text("Command Cache Statistics", HEADER_COLOR))
            .build();
        
        Component separator = Component.text("▬".repeat(40), NamedTextColor.GRAY);
        
        context.sendMessage(header);
        context.sendMessage(separator);
        
        // Cache sizes
        context.sendMessage(createMetricLine("Permission Cache Size", 
            String.valueOf(stats.permissionCacheSize)));
        context.sendMessage(createMetricLine("Tab Completion Cache Size", 
            String.valueOf(stats.tabCompletionCacheSize)));
        context.sendMessage(createMetricLine("Spell Filter Cache Size", 
            String.valueOf(stats.spellFilterCacheSize)));
        context.sendMessage(createMetricLine("Help Cache Size", 
            String.valueOf(stats.helpCacheSize)));
        
        context.sendMessage(Component.empty());
        
        // Hit rates
        context.sendMessage(createMetricLine("Permission Hit Rate", 
            String.format("%.1f%%", stats.permissionHitRate * 100)));
        context.sendMessage(createMetricLine("Tab Completion Hit Rate", 
            String.format("%.1f%%", stats.tabCompletionHitRate * 100)));
        
        context.sendMessage(Component.empty());
        
        // Raw counts
        context.sendMessage(createMetricLine("Permission Hits", 
            String.valueOf(stats.permissionCacheHits)));
        context.sendMessage(createMetricLine("Permission Misses", 
            String.valueOf(stats.permissionCacheMisses)));
        context.sendMessage(createMetricLine("Tab Completion Hits", 
            String.valueOf(stats.tabCompletionCacheHits)));
        context.sendMessage(createMetricLine("Tab Completion Misses", 
            String.valueOf(stats.tabCompletionCacheMisses)));
        
        // Cache effectiveness analysis
        context.sendMessage(Component.empty());
        analyzeCacheEffectiveness(context, stats);
    }
    
    private void showPerformanceReport(@NotNull CommandContext context, @NotNull BaseWandCommand wandCommand) {
        String report = wandCommand.getPerformanceReport();
        
        Component header = Component.text()
            .append(Component.text("⚡ ", HEADER_COLOR))
            .append(Component.text("Command Performance Report", HEADER_COLOR))
            .build();
        
        Component separator = Component.text("▬".repeat(50), NamedTextColor.GRAY);
        
        context.sendMessage(header);
        context.sendMessage(separator);
        
        // Split report into lines and format nicely
        String[] lines = report.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                context.sendMessage(Component.empty());
            } else if (line.startsWith("===")) {
                // Section headers
                context.sendMessage(Component.text(line, HEADER_COLOR));
            } else if (line.startsWith("Top ") || line.startsWith("Commands with")) {
                // Subsection headers
                context.sendMessage(Component.text(line, METRIC_COLOR));
            } else if (line.matches("\\d+\\..*")) {
                // Numbered list items
                context.sendMessage(Component.text("  " + line, VALUE_COLOR));
            } else if (line.startsWith("- ")) {
                // Bullet points
                context.sendMessage(Component.text("  " + line, WARNING_COLOR));
            } else {
                // Regular metrics
                context.sendMessage(Component.text(line, NamedTextColor.WHITE));
            }
        }
        
        // Add optimization suggestions
        context.sendMessage(Component.empty());
        context.sendMessage(Component.text("💡 Optimization Tips:", HEADER_COLOR));
        context.sendMessage(Component.text("• Use 'performance cache' to view cache statistics", NamedTextColor.GRAY));
        context.sendMessage(Component.text("• High error rates may indicate user confusion", NamedTextColor.GRAY));
        context.sendMessage(Component.text("• Slow commands should be investigated and optimized", NamedTextColor.GRAY));
    }
    
    private void resetMetrics(@NotNull CommandContext context, @NotNull BaseWandCommand wandCommand) {
        wandCommand.getPerformanceMonitor().resetMetrics();
        wandCommand.invalidateCaches();
        
        Component successMessage = CommandErrorHandler.createSuccessMessage(
            "Performance metrics and caches have been reset");
        context.sendMessage(successMessage);
        
        Component infoMessage = Component.text()
            .append(Component.text("ℹ ", NamedTextColor.AQUA))
            .append(Component.text("New metrics will begin collecting immediately", NamedTextColor.GRAY))
            .build();
        context.sendMessage(infoMessage);
    }
    
    private Component createMetricLine(@NotNull String metric, @NotNull String value) {
        return Component.text()
            .append(Component.text("• ", METRIC_COLOR))
            .append(Component.text(metric + ": ", NamedTextColor.WHITE))
            .append(Component.text(value, VALUE_COLOR))
            .build();
    }
    
    private void analyzeCacheEffectiveness(@NotNull CommandContext context, 
                                         @NotNull CommandCache.CacheStats stats) {
        context.sendMessage(Component.text("📈 Cache Analysis:", HEADER_COLOR));
        
        // Permission cache analysis
        if (stats.permissionHitRate > 0.8) {
            context.sendMessage(Component.text("✅ Permission cache is highly effective", VALUE_COLOR));
        } else if (stats.permissionHitRate > 0.5) {
            context.sendMessage(Component.text("⚠ Permission cache has moderate effectiveness", WARNING_COLOR));
        } else {
            context.sendMessage(Component.text("❌ Permission cache may need optimization", NamedTextColor.RED));
        }
        
        // Tab completion cache analysis
        if (stats.tabCompletionHitRate > 0.6) {
            context.sendMessage(Component.text("✅ Tab completion cache is working well", VALUE_COLOR));
        } else if (stats.tabCompletionHitRate > 0.3) {
            context.sendMessage(Component.text("⚠ Tab completion cache has room for improvement", WARNING_COLOR));
        } else {
            context.sendMessage(Component.text("❌ Tab completion cache needs attention", NamedTextColor.RED));
        }
        
        // Cache size warnings
        if (stats.permissionCacheSize > 800) {
            context.sendMessage(Component.text("⚠ Permission cache is getting large - consider cleanup", WARNING_COLOR));
        }
        if (stats.tabCompletionCacheSize > 400) {
            context.sendMessage(Component.text("⚠ Tab completion cache is getting large", WARNING_COLOR));
        }
    }
    
    @Nullable
    private BaseWandCommand getWandCommand(@NotNull CommandContext context) {
        // This is a simplified approach - in a real implementation, you'd have
        // proper dependency injection or service location
        return null; // This would need to be properly implemented
    }
    
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            String partial = context.args()[1].toLowerCase();
            return List.of("cache", "reset", "report").stream()
                .filter(action -> action.startsWith(partial))
                .toList();
        }
        return List.of();
    }
    
    @Override
    public @NotNull List<CommandHelpProvider.CommandExample> getExamples() {
        return List.of(
            new CommandHelpProvider.CommandExample("performance", "Show complete performance report"),
            new CommandHelpProvider.CommandExample("performance cache", "Display cache statistics"),
            new CommandHelpProvider.CommandExample("performance reset", "Reset all metrics and caches")
        );
    }
}