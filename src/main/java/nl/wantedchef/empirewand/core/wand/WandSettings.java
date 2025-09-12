package nl.wantedchef.empirewand.core.wand;

import nl.wantedchef.empirewand.core.storage.Keys;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages wand-specific settings including spell switch effects.
 */
public class WandSettings {

    private final ItemStack wand;
    private final PersistentDataContainer dataContainer;

    public WandSettings(ItemStack wand) {
        if (wand == null) {
            throw new IllegalArgumentException("Wand ItemStack cannot be null");
        }
        ItemMeta meta = wand.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("Wand ItemStack has no ItemMeta");
        }
        this.wand = wand;
        this.dataContainer = meta.getPersistentDataContainer();
    }

    /**
     * Gets the spell switch effect for this wand.
     * @return The switch effect name, or "default" if not set
     */
    public String getSpellSwitchEffect() {
        String effect = dataContainer.get(Keys.WAND_SWITCH_EFFECT, PersistentDataType.STRING);
        return effect != null && !effect.isBlank() ? effect : "default";
    }

    /**
     * Sets the spell switch effect for this wand.
     * @param effect The effect name (if null/blank, the setting is cleared)
     */
    public void setSpellSwitchEffect(String effect) {
        wand.editMeta(meta -> {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (effect == null || effect.isBlank()) {
                container.remove(Keys.WAND_SWITCH_EFFECT);
            } else {
                container.set(Keys.WAND_SWITCH_EFFECT, PersistentDataType.STRING, effect);
            }
        });
    }

    /**
     * Gets the toggle commands enabled for this wand.
     * @return Map of command names to their enabled status
     */
    public Map<String, Boolean> getToggleCommands() {
        Map<String, Boolean> commands = new HashMap<>();

        String commandsData = dataContainer.get(Keys.WAND_TOGGLE_COMMANDS, PersistentDataType.STRING);

        if (commandsData != null && !commandsData.isEmpty()) {
            String[] commandPairs = commandsData.split(";");
            for (String pair : commandPairs) {
                if (pair == null || pair.isEmpty()) continue;
                String[] parts = pair.split(":", 2);
                if (parts.length == 2) {
                    String name = parts[0].trim();
                    String val = parts[1].trim();
                    if (!name.isEmpty()) {
                        commands.put(name, Boolean.parseBoolean(val));
                    }
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
        wand.editMeta(meta -> {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (commands == null || commands.isEmpty()) {
                container.remove(Keys.WAND_TOGGLE_COMMANDS);
                return;
            }
            StringBuilder data = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, Boolean> entry : commands.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isBlank()) continue;
                if (!first) {
                    data.append(";");
                }
                data.append(key.trim()).append(":").append(Boolean.TRUE.equals(entry.getValue()));
                first = false;
            }
            container.set(Keys.WAND_TOGGLE_COMMANDS, PersistentDataType.STRING, data.toString());
        });
    }

    /**
     * Checks if a toggle command is enabled for this wand.
     * @param command The command name
     * @return true if enabled, false otherwise
     */
    public boolean isToggleCommandEnabled(String command) {
        if (command == null || command.isBlank()) return false;
        return getToggleCommands().getOrDefault(command.trim(), false);
    }

    /**
     * Sets the enabled status of a toggle command for this wand.
     * @param command The command name
     * @param enabled The enabled status
     */
    public void setToggleCommandEnabled(String command, boolean enabled) {
        if (command == null || command.isBlank()) return;
        Map<String, Boolean> commands = getToggleCommands();
        commands.put(command.trim(), enabled);
        setToggleCommands(commands);
    }
}