package nl.wantedchef.empirewand.listener.wand;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;

public class WandSwapHandListener implements Listener {
    private final EmpireWandPlugin plugin;
    
    public WandSwapHandListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }
}
