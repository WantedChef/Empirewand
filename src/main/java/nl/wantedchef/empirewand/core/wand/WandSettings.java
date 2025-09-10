package nl.wantedchef.empirewand.core.wand;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;


/**
 * Manages wand-specific settings including spell switch effects.
 */
public class WandSettings {
    private static final String SWITCH_EFFECT_KEY = "spell_switch_effect";
    private static final String TOGGLE_COMMANDS_KEY = "toggle_commands";
    
    private final ItemStack wand;
    private final PersistentDataContainer dataContainer;
    
    public WandSettings(ItemStack wand) {
        this.wand = wand;
        this.dataContainer = wand.getItemMeta().getPersistentDataContainer();
    }
    
    /**
     * Gets the spell switch effect for this wand.
     * @return The switch effect name, or "default" if not set
     */
    public String getSpellSwitchEffect() {
        String effect = dataContainer.get(
            new org.bukkit.NamespacedKey("empirewand", SWITCH_EFFECT_KEY), 
            PersistentDataType.STRING
        );
        return effect != null ? effect : "default";
    }
    
    /**
     * Sets the spell switch effect for this wand.
     * @param effect The effect name
     */
    public void setSpellSwitchEffect(String effect) {
        wand.editMeta(meta -> {
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey("empirewand", SWITCH_EFFECT_KEY), 
                PersistentDataType.STRING, 
                effect
            );
        });
    }
    
    /**
     * Gets the toggle commands enabled for this wand.
     * @return Map of command names to their enabled status
     */
    public Map<String, Boolean> getToggleCommands() {
        Map<String, Boolean> commands = new HashMap<>();
        
        String commandsData = dataContainer.get(
            new org.bukkit.NamespacedKey("empirewand", TOGGLE_COMMANDS_KEY), 
            PersistentDataType.STRING
        );
        
        if (commandsData != null && !commandsData.isEmpty()) {
            String[] commandPairs = commandsData.split(";");
            for (String pair : commandPairs) {
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    commands.put(parts[0], Boolean.parseBoolean(parts[1]));
                }
            }
        }
        
        return commands;
    }
    
    /**
     * Sets the toggle commands for this wand.
     * @param commands Map of command names to their enabled status
     */
    public void setToggleCommands(Map<String, Boolean> commands) {
        StringBuilder data = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Boolean> entry : commands.entrySet()) {
            if (!first) {
                data.append(";");
            }
            data.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }
        
        wand.editMeta(meta -> {
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey("empirewand", TOGGLE_COMMANDS_KEY), 
                PersistentDataType.STRING, 
                data.toString()
            );
        });
    }
    
    /**
     * Checks if a toggle command is enabled for this wand.
     * @param command The command name
     * @return true if enabled, false otherwise
     */
    public boolean isToggleCommandEnabled(String command) {
        return getToggleCommands().getOrDefault(command, false);
    }
    
    /**
     * Sets the enabled status of a toggle command for this wand.
     * @param command The command name
     * @param enabled The enabled status
     */
    public void setToggleCommandEnabled(String command, boolean enabled) {
        Map<String, Boolean> commands = getToggleCommands();
        commands.put(command, enabled);
        setToggleCommands(commands);
    }
}