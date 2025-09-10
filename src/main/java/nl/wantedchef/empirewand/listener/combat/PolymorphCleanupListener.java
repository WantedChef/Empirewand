package nl.wantedchef.empirewand.listener.combat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;

public class PolymorphCleanupListener implements Listener {
    private final EmpireWandPlugin plugin;
    
    public PolymorphCleanupListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }
}
