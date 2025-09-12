package nl.wantedchef.empirewand.listener.combat;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.service.ConfigService;
import nl.wantedchef.empirewand.core.storage.Keys;
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        String tag = event.getEntity().getPersistentDataContainer().get(Keys.PROJECTILE_SPELL,
                Keys.STRING_TYPE.getType());
        if (tag == null || !"explosive".equals(tag)) {
            return;
        }

        // Access config without synchronization as ConfigService is thread-safe
        // and this is running on the main thread anyway
        ConfigService cfg = plugin.getConfigService();
        boolean blockDamage = cfg.getSpellsConfig().getBoolean("explosive.flags.block-damage", false);

        if (!blockDamage) {
            event.blockList().clear(); // Keep visuals, prevent block destruction
        }
    }
}





