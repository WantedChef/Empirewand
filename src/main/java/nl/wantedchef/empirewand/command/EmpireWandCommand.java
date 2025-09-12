package nl.wantedchef.empirewand.command;

import org.bukkit.Material;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.command.BaseWandCommand;

/**
 * Main command handler for Empire Wands (/ew).
 * Built on the new enterprise command framework.
 */
public class EmpireWandCommand extends BaseWandCommand {

    private static final String PERMISSION_PREFIX = "empirewand";
    private static final String WAND_DISPLAY_NAME = "Empire Wand";

    public EmpireWandCommand(EmpireWandPlugin plugin) {
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
        return Material.BLAZE_ROD;
    }
}
