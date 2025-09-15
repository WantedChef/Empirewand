package nl.wantedchef.empirewand.listener.wand;

import nl.wantedchef.empirewand.EmpireWandPlugin;
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
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles wand interactions: left-click to cast, right-click to switch spells.
 * Implements visual spell switching effects and ensures proper spell cycling.
 */
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.WandService;

public final class WandCastListener implements Listener {
    private final EmpireWandPlugin plugin;
    private final SpellSwitchService spellSwitchService;

    public WandCastListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        this.spellSwitchService = new SpellSwitchService(plugin, EmpireWandAPI.getService(WandService.class));
    }

    // Handle even if other plugins cancel, and as early as possible
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        // Early exit conditions for maximum performance
        // Process both hands for reliability; prefer main hand interactions
        boolean isMainHand = event.getHand() == EquipmentSlot.HAND;
        boolean isOffHand = event.getHand() == EquipmentSlot.OFF_HAND;
        if (!isMainHand && !isOffHand) {
            return;
        }

        Action action = event.getAction();

        Player player = event.getPlayer();
        // Combined validation for better performance
        if (!player.isOnline() || !player.isValid()) {
            return;
        }

        ItemStack item = isMainHand ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        if (!plugin.getWandService().isWand(item)) {
            return;
        }

        List<String> spells = new ArrayList<>(plugin.getWandService().getSpells(item));
        if (spells.isEmpty()) {
            plugin.getFxService().showError(player, "wand.no-spells");
            return;
        }

        // Treat clicks generically: just left/right with the wand, regardless of AIR/BLOCK
        boolean isLeftClick = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        boolean isRightClick = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);
        if (!isLeftClick && !isRightClick) {
            return; // Ignore non-click actions (e.g., PHYSICAL)
        }

        if (isRightClick) {
            // Right-click: cycle to next spell with visual effect
            // Shift+right-click: cycle to previous spell with visual effect
            // Always cancel the event to ensure spell selection works everywhere
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            
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
            // mark last wand click tick to help the swing listener avoid double-casting
            long now = player.getWorld().getFullTime();
            player.setMetadata("empirewand.lastWandClick", new FixedMetadataValue(plugin, now));
            castCurrentSpell(player, item, spells);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onSneak(PlayerToggleSneakEvent event) {
        // Do not switch spells on sneak anymore. Intentionally left blank to comply with requested behavior.
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

        // Show improved spell selection feedback
        Map<String, String> params = Map.of("spell", displayName);
        plugin.getFxService().actionBarKey(player, "spell-switched", params);
        plugin.getFxService().playUISound(player, "select");

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

        // Show improved spell selection feedback
        Map<String, String> params = Map.of("spell", displayName);
        plugin.getFxService().actionBarKey(player, "spell-switched", params);
        plugin.getFxService().playUISound(player, "select");

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
        String configSpellKey = spells.get(index);

        SpellRegistry registry = plugin.getSpellRegistry();
        Optional<Spell<?>> spellOpt = registry.getSpell(configSpellKey);
        if (spellOpt.isEmpty()) {
            plugin.getFxService().showError(player, "wand.unknown-spell");
            plugin.getLogger()
                    .warning(String.format("Unknown spell '%s' on wand for player %s", configSpellKey, player.getName()));
            return;
        }

        Spell<?> spell = spellOpt.get();
        String actualSpellKey = spell.key(); // Use the spell's actual key for cooldowns!

        var perms = plugin.getPermissionService();

        // Re-check permissions in case they changed dynamically (use config key for permissions)
        if (!perms.has(player, perms.getSpellUsePermission(configSpellKey))) {
            plugin.getFxService().showError(player, "wand.no-permission");
            return;
        }

        long nowTicks = player.getWorld().getFullTime();
        var spellsCfg = plugin.getConfigService().getSpellsConfig();
        int cdTicks = Math.max(0, spellsCfg.getInt(configSpellKey + ".cooldown-ticks", 40));

        var cooldownManager = plugin.getCooldownManager();
        if (cooldownManager.isSpellOnCooldown(player.getUniqueId(), actualSpellKey, nowTicks, item)) {
            long remainingTicks = cooldownManager.getSpellCooldownRemaining(player.getUniqueId(), actualSpellKey, nowTicks, item);
            double remainingSeconds = Math.max(0.1, remainingTicks / 20.0);
            String formattedTime = String.format(java.util.Locale.US, "%.1f", remainingSeconds);

            Map<String, String> ph = Map.of(
                "spell", plugin.getSpellRegistry().getSpellDisplayName(configSpellKey),
                "seconds", formattedTime
            );
            plugin.getFxService().actionBarKey(player, "wand.on-cooldown", ph);
            plugin.getFxService().playUISound(player, "cooldown");
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

            // Use new cast API and handle result
            nl.wantedchef.empirewand.spell.CastResult result = spell.cast(ctx);

            // Only apply cooldown and show success on success
            if (result.isSuccess()) {
                cooldownManager.setSpellCooldown(player.getUniqueId(), actualSpellKey, nowTicks + cdTicks);

                if (player.isOnline()) {
                    var params = new HashMap<String, String>();
                    params.put("spell", registry.getSpellDisplayName(configSpellKey));
                    fx.showSuccess(player, "spell-cast", params);
                }

                plugin.getMetricsService().recordSpellCast(configSpellKey, (System.nanoTime() - start) / 1_000_000);
            } else {
                // Failure: log error and record metrics (don't show error message to player)
                plugin.getLogger().info(String.format("Spell cast failed for '%s' by player %s",
                    configSpellKey, player.getName()));
                plugin.getMetricsService().recordFailedCast();
            }
        } catch (Exception e) {
            // Base Spell.cast handles failure events; as a safety net, log the error
            plugin.getLogger().warning(String.format("Spell cast exception for '%s' by player %s: %s",
                configSpellKey, player.getName(), e.getMessage()));
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
