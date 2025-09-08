package com.example.empirewand.listeners.wand;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.services.CooldownService;
import com.example.empirewand.core.services.FxService;
import com.example.empirewand.api.SpellRegistry;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Casten via RIGHT_CLICK (alleen main hand). Bevat permissie- en
 * cooldown-checks.
 */
public final class WandCastListener implements Listener {
    private final EmpireWandPlugin plugin;

    public WandCastListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return; // voorkom dubbele triggers
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();

        // Check if player is still online and valid
        if (!player.isOnline() || !player.isValid()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getWandService().isWand(item))
            return;

        List<String> spells = plugin.getWandService().getSpells(item);
        if (spells == null || spells.isEmpty()) {
            plugin.getFxService().showError(player, "wand.no-spells");
            return;
        }

        int index = Math.max(0, Math.min(plugin.getWandService().getActiveIndex(item), spells.size() - 1));
        String spellKey = spells.get(index);

        SpellRegistry registry = plugin.getSpellRegistry();
        Optional<Spell<?>> spellOpt = registry.getSpell(spellKey);
        if (spellOpt.isEmpty()) {
            plugin.getFxService().showError(player, "wand.unknown-spell");
            plugin.getLogger().warning("Unknown spell '" + spellKey + "' on wand for player " + player.getName());
            return;
        }

        Spell<?> spell = spellOpt.get();

        var perms = plugin.getPermissionService();

        // Re-check permissions in case they changed dynamically
        if (!perms.has(player, perms.getSpellUsePermission(spellKey))) {
            plugin.getFxService().showError(player, "wand.no-permission");
            return;
        }

        long nowTicks = player.getWorld().getFullTime();
        var spellsCfg = plugin.getConfigService().getSpellsConfig();
        int cdTicks = Math.max(0, spellsCfg.getInt(spellKey + ".cooldown-ticks", 40));

        CooldownService cds = plugin.getCooldownService();
        if (cds.isOnCooldown(player.getUniqueId(), spellKey, nowTicks, item)) {
            long remaining = cds.remaining(player.getUniqueId(), spellKey, nowTicks, item);
            Map<String, String> ph = Map.of("seconds", String.valueOf(remaining / 20));
            plugin.getFxService().showError(player, "wand.on-cooldown", ph);
            return;
        }

        FxService fx = plugin.getFxService();
        SpellContext ctx = new SpellContext(plugin, player, plugin.getConfigService(), fx);

        long start = System.nanoTime();
        try {
            // Final online check before casting
            if (!player.isOnline()) {
                return;
            }

            if (!spell.canCast(ctx)) {
                fx.showError(player, "wand.cannot-cast");
                return;
            }

            // Use new cast API
            spell.cast(ctx);
            cds.set(player.getUniqueId(), spellKey, nowTicks + cdTicks);

            // Only show success message if player is still online
            if (player.isOnline()) {
                var params = new HashMap<String, String>();
                params.put("spell", registry.getSpellDisplayName(spellKey));
                fx.showSuccess(player, "spell-cast", params);
            }

            plugin.getMetricsService().recordSpellCast(spellKey, (System.nanoTime() - start) / 1_000_000);
        } catch (Throwable t) {
            // Only show error if player is still online
            if (player.isOnline()) {
                fx.showError(player, "wand.cast-error");
            }
            plugin.getLogger().warning("Spell cast error for '" + spellKey + "': " + t.getMessage());
            plugin.getMetricsService().recordFailedCast();
        }
    }
}