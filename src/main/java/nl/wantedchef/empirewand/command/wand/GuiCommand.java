package nl.wantedchef.empirewand.command.wand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.wantedchef.empirewand.framework.command.CommandContext;
import nl.wantedchef.empirewand.framework.command.CommandException;
import nl.wantedchef.empirewand.framework.command.SubCommand;
import nl.wantedchef.empirewand.framework.command.util.CommandHelpProvider;
import nl.wantedchef.empirewand.gui.WandSelectorMenu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command to open the Wand Rules GUI system.
 * Allows administrators to configure wand settings through an intuitive interface.
 */
public class GuiCommand implements SubCommand, CommandHelpProvider.HelpAwareCommand {

    private final String permissionPrefix;

    public GuiCommand(@NotNull String permissionPrefix) {
        this.permissionPrefix = permissionPrefix;
    }

    @Override
    public @NotNull String getName() {
        return "gui";
    }

    @Override
    public @Nullable String getPermission() {
        return permissionPrefix + ".gui";
    }

    @Override
    public @NotNull String getUsage() {
        return "gui";
    }

    @Override
    public @NotNull String getDescription() {
        return "Open the wand settings GUI";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("settings", "config", "configure");
    }

    @Override
    public void execute(@NotNull CommandContext context) throws CommandException {
        try (var timing = context.startTiming("wand.gui")) {
            Player player = context.requirePlayer();

            // Create services directly since service registry isn't available
            var wandSettingsService = new nl.wantedchef.empirewand.core.config.WandSettingsService(context.plugin());
            var sessionManager = new nl.wantedchef.empirewand.gui.session.WandSessionManager(context.plugin().getLogger());

            // Initialize the WandSettingsService
            wandSettingsService.initialize().join(); // Block until initialization is complete

            // Create and open the wand selector menu
            WandSelectorMenu wandSelectorMenu = new WandSelectorMenu(
                    wandSettingsService,
                    sessionManager,
                    context.plugin().getLogger()
            );

            wandSelectorMenu.openMenu(player);

            // Send confirmation message
            context.sendMessage(Component.text("Opening wand settings GUI...")
                    .color(NamedTextColor.GREEN));

        } catch (Exception e) {
            throw new CommandException("Failed to open wand settings GUI: " + e.getMessage(), e, "GUI_OPEN_FAILED");
        }
    }

    @Override
    public @NotNull List<CommandHelpProvider.CommandExample> getExamples() {
        return List.of(
                new CommandHelpProvider.CommandExample("gui", "Open the wand settings interface"),
                new CommandHelpProvider.CommandExample("settings", "Open the configuration menu (alias)")
        );
    }
}