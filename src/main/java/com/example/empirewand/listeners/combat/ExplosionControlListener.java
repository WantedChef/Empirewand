package com.example.empirewand.listeners.combat;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.services.ConfigService;
import com.example.empirewand.core.storage.Keys;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Houdt block-damage van explosieve spells in toom via config.
 */
public final class ExplosionControlListener implements Listener {
    private final EmpireWandPlugin plugin;

    public ExplosionControlListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityExplode(EntityExplodeEvent event) {
        String tag = event.getEntity().getPersistentDataContainer().get(Keys.PROJECTILE_SPELL,
                Keys.STRING_TYPE.getType());
        if (tag == null || !tag.equals("explosive"))
            return;

        ConfigService cfg = plugin.getConfigService();
        boolean blockDamage = cfg.getSpellsConfig().getBoolean("explosive.flags.block-damage", false);
        if (!blockDamage) {
            event.blockList().clear(); // visuals blijven, geen block destruction
        }
    }
}