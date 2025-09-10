package nl.wantedchef.empirewand.listener.player;

import nl.wantedchef.empirewand.core.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.core.wand.WandSettings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

/**
 * Listener that handles auto-activation of toggle spells when players start flying.
 */
public class FlightListener implements Listener {
    private final EmpireWandPlugin plugin;

    public FlightListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        // Only handle when player starts flying (not when they stop)
        if (!event.isFlying()) {
            return;
        }

        Player player = event.getPlayer();

        // Check if player is still online and valid
        if (!player.isOnline() || !player.isValid()) {
            return;
        }

        // Get the player's inventory to find wands
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getWandService().isWand(item)) {
                // Check if this wand has auto-activation enabled for flying spells
                WandSettings settings = new WandSettings(item);
                Map<String, Boolean> toggleCommands = settings.getToggleCommands();
                
                // Check each enabled toggle command
                for (Map.Entry<String, Boolean> entry : toggleCommands.entrySet()) {
                    if (entry.getValue()) { // If command is enabled
                        String command = entry.getKey();
                        
                        // Check if it's a flying-related toggle spell
                        if (isFlyingToggleSpell(command)) {
                            // Activate the spell
                            activateFlyingSpell(player, item, command);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if a toggle command is related to flying.
     * @param command The command name
     * @return true if it's a flying-related toggle spell, false otherwise
     */
    private boolean isFlyingToggleSpell(String command) {
        return command.equals("kajcloud") || command.equals("mephicloud");
    }

    /**
     * Activates a flying toggle spell for the player.
     * @param player The player
     * @param wand The wand item
     * @param spellKey The spell key
     */
    private void activateFlyingSpell(Player player, ItemStack wand, String spellKey) {
        SpellRegistry registry = plugin.getSpellRegistry();
        Optional<ToggleableSpell> spellOpt = registry.getToggleableSpell(spellKey);
        
        if (spellOpt.isPresent()) {
            ToggleableSpell spell = spellOpt.get();
            
            // Check if the spell is already active
            if (!spell.isActive(player)) {
                // Create a spell context for activation
                nl.wantedchef.empirewand.spell.SpellContext ctx = 
                    new nl.wantedchef.empirewand.spell.SpellContext(
                        plugin, 
                        player, 
                        plugin.getConfigService(), 
                        plugin.getFxService()
                    );
                
                // Activate the spell
                spell.activate(player, ctx);
            }
        }
    }
}