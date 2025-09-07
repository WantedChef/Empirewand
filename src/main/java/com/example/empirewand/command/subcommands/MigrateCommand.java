package com.example.empirewand.command.subcommands;

import com.example.empirewand.command.framework.CommandContext;
import com.example.empirewand.command.framework.CommandException;
import com.example.empirewand.command.framework.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * Command to migrate configurations.
 */
public class MigrateCommand implements SubCommand {

    private final String wandType;

    public MigrateCommand(String wandType) {
        this.wandType = wandType;
    }

    @Override
    public @NotNull String getName() {
        return "migrate";
    }

    @Override
    public @Nullable String getPermission() {
        return wandType + ".command.migrate";
    }

    @Override
    public @NotNull String getUsage() {
        return "migrate";
    }

    @Override
    public @NotNull String getDescription() {
        return "Migrate configurations to latest version";
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        context.sendMessage(Component.text("Starting configuration migration...")
                .color(NamedTextColor.YELLOW));

        // Run migration in a separate thread to avoid blocking the main thread
        context.plugin().getServer().getScheduler().runTaskAsynchronously(context.plugin(), () -> {
            try {
                boolean migrated = context.config().getMigrationService().migrateAllConfigs();
                if (migrated) {
                    context.plugin().getServer().getScheduler().runTask(context.plugin(),
                            () -> context.sendMessage(Component
                                    .text("Migration completed. Please restart the server for changes to take effect.")
                                    .color(NamedTextColor.GREEN)));
                } else {
                    context.plugin().getServer().getScheduler().runTask(context.plugin(), () -> context
                            .sendMessage(Component.text("No migrations were necessary.").color(NamedTextColor.GREEN)));
                }
            } catch (RuntimeException e) {
                context.plugin().getLogger().log(Level.SEVERE, "Migration failed", e);
                context.plugin().getServer().getScheduler().runTask(context.plugin(), () -> context.sendMessage(
                        Component.text("Migration failed. Check server logs for details.").color(NamedTextColor.RED)));
            }
        });
    }
}