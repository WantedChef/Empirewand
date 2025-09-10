package nl.wantedchef.empirewand.listener.combat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;

public class FallDamageEtherealListener implements Listener {
    private final EmpireWandPlugin plugin;
    
    public FallDamageEtherealListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }
}
