package nl.wantedchef.empirewand.command.wand;

import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider;
import nl.wantedchef.empirewand.framework.command.util.AsyncCommandExecutor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command to display detailed statistics about wand usage, spells cast, and performance metrics.
 * Utilizes async execution to gather data without blocking the main thread.
 */
public class StatsCommand implements SubCommand, CommandHelpProvider.HelpAwareCommand {

    private final String wandType;

    public StatsCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "stats";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.stats";
    }

    @Override
    public @NotNull String getUsage() {
        return "stats [player]";
    }

    @Override
    public @NotNull String getDescription() {
        return "Display detailed statistics about wand usage and performance";
    }

    @Override
    public boolean requiresPlayer() {
        return false; // Can be used by console too
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        // For player stats, we need a player target
        String targetName = context.getArgOrNull(1);
        Player targetPlayer = null;

        if (targetName != null) {
            targetPlayer = context.plugin().getServer().getPlayer(targetName);
            if (targetPlayer == null) {
                throw new CommandException("Player not found: " + targetName, "PLAYER_NOT_FOUND",
                        targetName);
            }
        } else if (context.sender() instanceof Player player) {
            targetPlayer = player;
        } else {
            throw new CommandException(
                    "You must specify a player when using this command from console",
                    "CONSOLE_REQUIRES_PLAYER");
        }

        // Execute asynchronously to gather stats without blocking
        AsyncCommandExecutor asyncExecutor = new AsyncCommandExecutor(context.plugin());
        final CommandContext finalContext = context;
        final Player finalTargetPlayer = targetPlayer;
        asyncExecutor.executeAsync(context, this,
                () -> gatherStatistics(finalContext, finalTargetPlayer),
                result -> sendStatistics(finalContext, finalTargetPlayer, (StatsData) result),
                exception -> context.sendMessage(
                        Component.text("Failed to gather statistics: " + exception.getMessage())
                                .color(NamedTextColor.RED)));
    }

    /**
     * Gathers statistics data asynchronously.
     */
    private StatsData gatherStatistics(@NotNull CommandContext context, @NotNull Player player) {
        StatsData stats = new StatsData();
        stats.playerName = player.getName();

        // Get spell cast statistics
        nl.wantedchef.empirewand.framework.service.metrics.DebugMetricsService debugMetrics =
                context.plugin().getDebugMetricsService();
        stats.spellsCast = (int) debugMetrics.getTotalSpellCasts();
        stats.failedCasts = (int) debugMetrics.getTotalFailedCasts();
        stats.successRate = debugMetrics.getSpellCastSuccessRate();

        // Get performance metrics
        stats.avgCastTime = debugMetrics.getSpellCastP95(); // Using P95 as average for now
        stats.maxCastTime = debugMetrics.getSpellCastP95(); // Using P95 as max for now

        // Get wand usage stats
        // TODO: Implement proper wands created tracking in MetricsService
        stats.wandsCreated = 0; // Placeholder
        // Note: Active wands count not available, using global statistics placeholder
        stats.wandsActive = 0; // TODO: Implement active wands tracking

        return stats;
    }

    /**
     * Sends the statistics to the command sender.
     */
    private void sendStatistics(@NotNull CommandContext context, @NotNull Player player,
            @NotNull StatsData stats) {
        Component header = Component.text("=== Wand Statistics for " + stats.playerName + " ===")
                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);

        Component spellStats = Component.text("\nSpell Casting Statistics:")
                .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("  Total Spells Cast: ").color(NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(stats.spellsCast))
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("  Failed Casts: ").color(NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(stats.failedCasts)).color(NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.text("  Success Rate: ").color(NamedTextColor.WHITE))
                .append(Component.text(String.format("%.1f%%", stats.successRate))
                        .color(NamedTextColor.AQUA));

        Component performanceStats =
                Component.text("\n\nPerformance Metrics:").color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD).append(Component.newline())
                        .append(Component.text("  Average Cast Time: ").color(NamedTextColor.WHITE))
                        .append(Component.text(String.format("%.2f ms", stats.avgCastTime))
                                .color(NamedTextColor.GREEN))
                        .append(Component.newline())
                        .append(Component.text("  Maximum Cast Time: ").color(NamedTextColor.WHITE))
                        .append(Component.text(String.format("%.2f ms", stats.maxCastTime))
                                .color(NamedTextColor.RED));

        Component wandStats = Component.text("\n\nWand Statistics:").color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD).append(Component.newline())
                .append(Component.text("  Wands Created: ").color(NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(stats.wandsCreated))
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("  Active Wands: ").color(NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(stats.wandsActive))
                        .color(NamedTextColor.AQUA));

        Component fullStats = header.append(spellStats).append(performanceStats).append(wandStats);
        context.sendMessage(fullStats);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandContext context) {
        if (context.args().length == 2) {
            String partial = context.args()[1].toLowerCase();
            return context.plugin().getServer().getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial)).toList();
        }
        return List.of();
    }

    @Override
    public @NotNull List<CommandHelpProvider.CommandExample> getExamples() {
        return List.of(
                new CommandHelpProvider.CommandExample("stats", "Show your own wand statistics"),
                new CommandHelpProvider.CommandExample("stats PlayerName",
                        "Show statistics for a specific player"));
    }

    /**
     * Data class for holding statistics information.
     */
    private static class StatsData {
        String playerName;
        int spellsCast;
        int failedCasts;
        double successRate;
        double avgCastTime;
        double maxCastTime;
        int wandsCreated;
        int wandsActive;
    }
}
