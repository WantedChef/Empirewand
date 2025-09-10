package nl.wantedchef.empirewand.core.storage;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {
    private static Plugin plugin;
    
    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
    }
    
    public static final NamespacedKey ETHEREAL_ACTIVE = key("ethereal_active");
    public static final NamespacedKey WAND_DATA = key("wand_data");
    public static final NamespacedKey SPELL_BINDS = key("spell_binds");
    public static final NamespacedKey SELECTED_SPELL = key("selected_spell");
    public static final NamespacedKey WAND_OWNER = key("wand_owner");
    
    private static NamespacedKey key(String name) {
        if (plugin == null) {
            throw new IllegalStateException("Keys not initialized");
        }
        return new NamespacedKey(plugin, name);
    }
}
