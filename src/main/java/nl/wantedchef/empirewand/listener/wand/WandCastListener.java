package nl.wantedchef.empirewand.listener.wand;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;

public class WandCastListener implements Listener {
    private final EmpireWandPlugin plugin;
    
    public WandCastListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }
}
