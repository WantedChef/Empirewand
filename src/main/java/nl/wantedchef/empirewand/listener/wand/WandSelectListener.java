package nl.wantedchef.empirewand.listener.wand;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;

public class WandSelectListener implements Listener {
    private final EmpireWandPlugin plugin;
    
    public WandSelectListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }
}
