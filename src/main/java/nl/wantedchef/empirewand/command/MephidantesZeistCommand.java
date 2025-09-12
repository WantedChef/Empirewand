package nl.wantedchef.empirewand.command;

import org.bukkit.Material;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.BaseWandCommand;

/**
 * Main command handler for Mephidantes Zeist Wands (/mz).
 * Built on the new enterprise command framework.
 */
public class MephidantesZeistCommand extends BaseWandCommand {

    private static final String PERMISSION_PREFIX = "mephidanteszeist";
    private static final String WAND_DISPLAY_NAME = "Mephidantes Zeist Wand";

    public MephidantesZeistCommand(EmpireWandPlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getPermissionPrefix() {
        return PERMISSION_PREFIX;
    }

    @Override
    protected String getWandDisplayName() {
        return WAND_DISPLAY_NAME;
    }

    @Override
    protected Material getWandMaterial() {
        return Material.NETHERITE_HOE;
    }
}
