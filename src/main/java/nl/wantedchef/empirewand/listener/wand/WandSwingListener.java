package nl.wantedchef.empirewand.listener.wand;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Optional: Left-click casting via PlayerAnimationEvent for more reliable detection.
 * Uses a short per-player debounce and metadata flag to avoid double casting
 * when PlayerInteractEvent has already handled the click.
 */
public final class WandSwingListener implements Listener {
    private static final String META_LAST_WAND_CLICK_TICK = "empirewand.lastWandClick";
    private final EmpireWandPlugin plugin;
    private final Map<java.util.UUID, Long> lastSwingCastTick = new HashMap<>();

    public WandSwingListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getWandService().isWand(item)) return;

        long now = player.getWorld().getFullTime();

        // If Interact already processed a wand click very recently, skip to avoid double-cast
        if (player.hasMetadata(META_LAST_WAND_CLICK_TICK)) {
            long last = player.getMetadata(META_LAST_WAND_CLICK_TICK).stream()
                    .findFirst().map(MetadataValue::asLong).orElse(0L);
            if (now - last <= 2) return; // within ~2 ticks, Interact took it
        }

        // Debounce swings themselves
        Long lastSwing = lastSwingCastTick.get(player.getUniqueId());
        if (lastSwing != null && now - lastSwing <= 3) return;

        // Proceed to cast current spell as a left-click
        List<String> spells = plugin.getWandService().getSpells(item);
        if (spells.isEmpty()) {
            plugin.getFxService().showError(player, "wand.no-spells");
            return;
        }

        int index = Math.max(0, Math.min(plugin.getWandService().getActiveIndex(item), spells.size() - 1));
        String configSpellKey = spells.get(index);
        Optional<Spell<?>> spellOpt = plugin.getSpellRegistry().getSpell(configSpellKey);
        if (spellOpt.isEmpty()) return;

        Spell<?> spell = spellOpt.get();
        String actualSpellKey = spell.key(); // Use the spell's actual key for cooldowns!

        // Cooldown check using the spell's actual key
        long nowTicks = player.getWorld().getFullTime();
        var spellsCfg = plugin.getConfigService().getSpellsConfig();
        int cdTicks = Math.max(0, spellsCfg.getInt(configSpellKey + ".cooldown-ticks", 40));
        var cooldownManager = plugin.getCooldownManager();
        if (cooldownManager.isSpellOnCooldown(player.getUniqueId(), actualSpellKey, nowTicks, item)) {
            long remaining = cooldownManager.getSpellCooldownRemaining(player.getUniqueId(), actualSpellKey, nowTicks, item);
            Map<String, String> ph = Map.of("seconds", String.valueOf(remaining / 20));
            plugin.getFxService().showError(player, "wand.on-cooldown", ph);
            return;
        }
        FxService fx = plugin.getFxService();
        SpellContext ctx = new SpellContext(plugin, player, plugin.getConfigService(), fx);

        if (!spell.canCast(ctx)) {
            fx.showError(player, "wand.cannot-cast");
            return;
        }

        var result = spell.cast(ctx);
        if (result.isSuccess()) {
            cooldownManager.setSpellCooldown(player.getUniqueId(), actualSpellKey, nowTicks + cdTicks);
            // mark metadata so subsequent animation events within 2 ticks will be skipped
            player.setMetadata(META_LAST_WAND_CLICK_TICK, new FixedMetadataValue(plugin, now));
            lastSwingCastTick.put(player.getUniqueId(), now);
        }
    }
}

