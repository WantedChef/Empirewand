package nl.wantedchef.empirewand.listener.combat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;

public class DeathSyncPolymorphListener implements Listener {
    private final EmpireWandPlugin plugin;
    
    public DeathSyncPolymorphListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }
}
