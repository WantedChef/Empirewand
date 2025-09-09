package nl.wantedchef.empirewand.listener.wand;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.service.CooldownService;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.framework.service.SpellSwitchService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles wand interactions: left-click to cast, right-click to switch spells.
 * Implements visual spell switching effects and ensures proper spell cycling.
 */
public final class WandCastListener implements Listener {
    private final EmpireWandPlugin plugin;
    private final SpellSwitchService spellSwitchService;

    public WandCastListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.spellSwitchService = new SpellSwitchService(plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return; // voorkom dubbele triggers

        Action action = event.getAction();
        boolean isLeftClick = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        boolean isRightClick = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);

        // Allow casting even when not looking at a block
        if (!isLeftClick && !isRightClick) {
            // Check for air clicks (when not looking at a block)
            if (action == Action.LEFT_CLICK_AIR) {
                isLeftClick = true;
            } else if (action == Action.RIGHT_CLICK_AIR) {
                isRightClick = true;
            } else {
                return;
            }
        }

        Player player = event.getPlayer();

        // Check if player is still online and valid
        if (!player.isOnline() || !player.isValid()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getWandService().isWand(item))
            return;

        List<String> spells = new ArrayList<>(plugin.getWandService().getSpells(item));
        if (spells.isEmpty()) {
            plugin.getFxService().showError(player, "wand.no-spells");
            return;
        }

        if (isRightClick) {
            // Right-click: cycle to next spell with visual effect
            // Shift+right-click: cycle to previous spell with visual effect
            event.setCancelled(true);
            if (player.isSneaking()) {
                cycleToPreviousSpell(player, item, spells);
            } else {
                cycleToNextSpell(player, item, spells);
            }
            return;
        }

        // Left-click: cast current spell
        if (isLeftClick) {
            event.setCancelled(true);
            castCurrentSpell(player, item, spells);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Only handle when player starts sneaking (not when they stop)
        if (!event.isSneaking()) {
            return;
        }

        // Check if player is still online and valid
        if (!player.isOnline() || !player.isValid()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getWandService().isWand(item)) {
            return;
        }

        List<String> spells = new ArrayList<>(plugin.getWandService().getSpells(item));
        if (spells.isEmpty()) {
            return; // Don't show error message for sneak action
        }

        // Sneak: cycle to next spell with visual effect
        cycleToNextSpell(player, item, spells);
    }

    private void cycleToNextSpell(Player player, ItemStack item, List<String> spells) {
        int currentIndex = Math.max(0, Math.min(plugin.getWandService().getActiveIndex(item), spells.size() - 1));
        int nextIndex = (currentIndex + 1) % spells.size(); // Wrap around to 0 after last spell

        String newSpellKey = spells.get(nextIndex);

        // Validate spell exists
        if (plugin.getSpellRegistry().getSpell(newSpellKey).isEmpty()) {
            plugin.getLogger()
                    .warning(String.format("Invalid spell '%s' in wand for player %s", newSpellKey, player.getName()));
            return;
        }

        // Update the active spell
        plugin.getWandService().setActiveIndex(item, nextIndex);

        // Get display name and show feedback
        String displayName = plugin.getSpellRegistry().getSpellDisplayName(newSpellKey);

        // Show action bar and chat message
        plugin.getFxService().actionBar(player, displayName);
        player.sendMessage("§aSelected: §r" + displayName);

        // Play spell switch effect
        spellSwitchService.playSpellSwitchEffect(player, item);

        // Fire WandSelectEvent for other plugins to listen to
        var previousSpellKey = spells.get(currentIndex);
        var previousSpell = plugin.getSpellRegistry().getSpell(previousSpellKey).orElse(null);
        var newSpell = plugin.getSpellRegistry().getSpell(newSpellKey).orElse(null);

        if (newSpell != null) {
            var selectEvent = new nl.wantedchef.empirewand.api.event.WandSelectEvent(player, previousSpell, newSpell,
                    previousSpellKey, newSpellKey,
                    nl.wantedchef.empirewand.api.event.WandSelectEvent.SelectionMethod.SNEAK_CLICK);
            plugin.getServer().getPluginManager().callEvent(selectEvent);
        }
    }

    private void cycleToPreviousSpell(Player player, ItemStack item, List<String> spells) {
        int currentIndex = Math.max(0, Math.min(plugin.getWandService().getActiveIndex(item), spells.size() - 1));
        int nextIndex = (currentIndex - 1 + spells.size()) % spells.size(); // Wrap around to last after first spell

        String newSpellKey = spells.get(nextIndex);

        // Validate spell exists
        if (plugin.getSpellRegistry().getSpell(newSpellKey).isEmpty()) {
            plugin.getLogger()
                    .warning(String.format("Invalid spell '%s' in wand for player %s", newSpellKey, player.getName()));
            return;
        }

        // Update the active spell
        plugin.getWandService().setActiveIndex(item, nextIndex);

        // Get display name and show feedback
        String displayName = plugin.getSpellRegistry().getSpellDisplayName(newSpellKey);

        // Show action bar and chat message
        plugin.getFxService().actionBar(player, displayName);
        player.sendMessage("§aSelected: §r" + displayName);

        // Play spell switch effect
        spellSwitchService.playSpellSwitchEffect(player, item);

        // Fire WandSelectEvent for other plugins to listen to
        var previousSpellKey = spells.get(currentIndex);
        var previousSpell = plugin.getSpellRegistry().getSpell(previousSpellKey).orElse(null);
        var newSpell = plugin.getSpellRegistry().getSpell(newSpellKey).orElse(null);

        if (newSpell != null) {
            var selectEvent = new nl.wantedchef.empirewand.api.event.WandSelectEvent(player, previousSpell, newSpell,
                    previousSpellKey, newSpellKey,
                    nl.wantedchef.empirewand.api.event.WandSelectEvent.SelectionMethod.SNEAK_CLICK);
            plugin.getServer().getPluginManager().callEvent(selectEvent);
        }
    }

    private void castCurrentSpell(Player player, ItemStack item, List<String> spells) {
        int index = Math.max(0, Math.min(plugin.getWandService().getActiveIndex(item), spells.size() - 1));
        String spellKey = spells.get(index);

        SpellRegistry registry = plugin.getSpellRegistry();
        Optional<Spell<?>> spellOpt = registry.getSpell(spellKey);
        if (spellOpt.isEmpty()) {
            plugin.getFxService().showError(player, "wand.unknown-spell");
            plugin.getLogger()
                    .warning(String.format("Unknown spell '%s' on wand for player %s", spellKey, player.getName()));
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
            plugin.getLogger().warning(String.format("Spell cast error for '%s': %s", spellKey, t.getMessage()));
            plugin.getMetricsService().recordFailedCast();
        }
    }
    
    /**
     * Public method to allow other parts of the plugin to play spell switch effects
     * @param player The player
     * @param wand The wand item
     */
    public void playSpellSwitchEffect(Player player, ItemStack wand) {
        spellSwitchService.playSpellSwitchEffect(player, wand);
    }
}
