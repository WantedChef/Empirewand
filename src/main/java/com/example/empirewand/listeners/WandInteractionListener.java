package com.example.empirewand.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.PerformanceMonitor;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {
        "EI_EXPOSE_REP2" }, justification = "Listener holds plugin reference as required by Bukkit for accessing services and scheduling; no safer alternative without global statics.")
public class WandInteractionListener implements Listener {

    private final EmpireWandPlugin plugin;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final HashMap<UUID, Long> cycleCooldowns = new HashMap<>();

    public WandInteractionListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        // Only handle if player is holding a wand
        if (item == null || !plugin.getWandData().isWand(item)) {
            return;
        }

        // Cancel the item switch to prevent inventory changes
        event.setCancelled(true);

        // Check if player is sneaking for spell cycling
        if (player.isSneaking()) {
            handleSpellCycling(player, item, event.getPreviousSlot(), event.getNewSlot());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Guard: Check if item exists and is a wand
        if (item == null || !plugin.getWandData().isWand(item)) {
            return;
        }

        // Prevent default interactions while holding a wand
        event.setCancelled(true);

        Action action = event.getAction();
        boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;

        if (isRightClick) {
            handleSpellSelection(player, item);
        } else if (isLeftClick) {
            handleSpellCast(player, item);
        }
    }

    private void handleSpellCycling(Player player, ItemStack item, int previousSlot, int newSlot) {
        // Debounce cycling (200ms minimum interval)
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        if (cycleCooldowns.containsKey(playerId)) {
            if (now - cycleCooldowns.get(playerId) < 200) {
                return;
            }
        }
        cycleCooldowns.put(playerId, now);

        List<String> spells = plugin.getWandData().getSpells(item);
        if (spells.isEmpty()) {
            plugin.getFxService().showError(player, "no-spells-bound");
            return;
        }

        int currentIndex = plugin.getWandData().getActiveIndex(item);
        if (currentIndex < 0 || currentIndex >= spells.size()) {
            currentIndex = 0;
        }

        // Determine cycle direction based on scroll
        int direction = newSlot > previousSlot ? 1 : -1;
        int nextIndex = (currentIndex + direction + spells.size()) % spells.size();

        plugin.getWandData().setActiveIndex(item, nextIndex);
        String activeSpellKey = spells.get(nextIndex);
        String display = plugin.getConfigService().getSpellsConfig().getString(activeSpellKey + ".display-name",
                activeSpellKey);
        display = plugin.getTextService().stripMiniTags(display);
        plugin.getFxService().selectedSpell(player, display);
    }

    private void handleSpellSelection(Player player, ItemStack item) {
        // Debounce
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(player.getUniqueId())) {
            if (now - cooldowns.get(player.getUniqueId()) < 200) { // 200ms = 4 ticks
                return;
            }
        }
        cooldowns.put(player.getUniqueId(), now);

        List<String> spells = plugin.getWandData().getSpells(item);
        if (spells.isEmpty()) {
            plugin.getFxService().showError(player, "no-spells-bound");
            return;
        }

        int currentIndex = plugin.getWandData().getActiveIndex(item);
        if (currentIndex < 0 || currentIndex >= spells.size()) {
            currentIndex = 0;
        }
        int nextIndex;

        if (player.isSneaking()) {
            nextIndex = (currentIndex - 1 + spells.size()) % spells.size();
        } else {
            nextIndex = (currentIndex + 1) % spells.size();
        }

        plugin.getWandData().setActiveIndex(item, nextIndex);
        String activeSpellKey = spells.get(nextIndex);
        // Try to use configured display-name, fallback to key
        String display = plugin.getConfigService().getSpellsConfig().getString(activeSpellKey + ".display-name",
                activeSpellKey);
        display = plugin.getTextService().stripMiniTags(display);
        plugin.getFxService().selectedSpell(player, display);
    }

    private void handleSpellCast(Player player, ItemStack item) {
        PerformanceMonitor.TimingContext timing = PerformanceMonitor.startTiming("handleSpellCast");

        List<String> spells = plugin.getWandData().getSpells(item);
        if (spells.isEmpty()) {
            plugin.getFxService().showError(player, "no-spells-bound");
            return;
        }

        int currentIndex = plugin.getWandData().getActiveIndex(item);
        if (currentIndex < 0 || currentIndex >= spells.size()) {
            currentIndex = 0;
        }
        String activeSpellKey = spells.get(currentIndex);
        String display = plugin.getConfigService().getSpellsConfig().getString(activeSpellKey + ".display-name",
                activeSpellKey);
        display = plugin.getTextService().stripMiniTags(display);

        Spell spell = plugin.getSpellRegistry().getSpell(activeSpellKey);
        if (spell == null) {
            Map<String, String> params = new HashMap<>();
            params.put("spell", activeSpellKey);
            plugin.getFxService().showError(player, "unknown-spell", params);
            return;
        }

        // Permission: empirewand.spell.use.<key>
        String node = "empirewand.spell.use." + activeSpellKey;
        if (!plugin.getPermissionService().has(player, node)) {
            plugin.getFxService().showError(player, "no-permission");
            return;
        }

        // Cooldown: prefer per-spell (spells.yml), fallback to default (config.yml)
        long cdMs = plugin.getConfigService().getSpellsConfig().getLong(activeSpellKey + ".cooldown",
                plugin.getConfigService().getDefaultCooldown());
        long cdTicks = Math.max(1, cdMs / 50);
        long nowTicks = player.getWorld().getFullTime();
        if (plugin.getCooldownService().isOnCooldown(player.getUniqueId(), activeSpellKey, nowTicks)) {
            long remainingMs = plugin.getCooldownService().remaining(player.getUniqueId(), activeSpellKey, nowTicks)
                    * 50;
            plugin.getFxService().onCooldown(player, display, remainingMs);
            return;
        }

        SpellContext context = new SpellContext(plugin, player, plugin.getConfigService(), plugin.getFxService());
        spell.execute(context);

        // Set cooldown window
        plugin.getCooldownService().set(player.getUniqueId(), activeSpellKey, nowTicks + cdTicks);

        Map<String, String> params = new HashMap<>();
        params.put("spell", display);
        plugin.getFxService().showSuccess(player, "spell-cast", params);

        timing.complete(25); // Spell casting should complete within 25ms
    }
}
