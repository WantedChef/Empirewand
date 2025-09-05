package com.example.empirewand.listeners;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class WandInteractionListener implements Listener {

    private final EmpireWandPlugin plugin;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    public WandInteractionListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !plugin.getWandData().isWand(item)) {
            return;
        }

        // Prevent default interactions while holding a wand
        event.setCancelled(true);

        Action action = event.getAction();
        boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;

        if (isRightClick) {
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
                plugin.getFxService().noSpells(player);
                plugin.getFxService().noSpells(player);
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
            String display = plugin.getConfigService().getSpellsConfig().getString(activeSpellKey + ".display-name", activeSpellKey);
            display = stripMiniTags(display);
            plugin.getFxService().selectedSpell(player, display);
        } else if (isLeftClick) {
            List<String> spells = plugin.getWandData().getSpells(item);
            if (spells.isEmpty()) {
                plugin.getFxService().noSpells(player);
                plugin.getFxService().noSpells(player);
                return;
            }

            int currentIndex = plugin.getWandData().getActiveIndex(item);
            if (currentIndex < 0 || currentIndex >= spells.size()) {
                currentIndex = 0;
            }
            String activeSpellKey = spells.get(currentIndex);
            String display = plugin.getConfigService().getSpellsConfig().getString(activeSpellKey + ".display-name", activeSpellKey);
            display = stripMiniTags(display);

            Spell spell = plugin.getSpellRegistry().getSpell(activeSpellKey);
            if (spell == null) {
                plugin.getFxService().actionBar(player, Component.text("Unknown spell '" + activeSpellKey + "'").color(NamedTextColor.RED));
                return;
            }

            // Permission: empirewand.spell.use.<key>
            String node = "empirewand.spell.use." + activeSpellKey;
            if (!plugin.getPermissionService().has(player, node)) {
                plugin.getFxService().noPermission(player);
                plugin.getFxService().noPermission(player);
                return;
            }

            // Cooldown: prefer per-spell (spells.yml), fallback to default (config.yml)
            long cdMs = plugin.getConfigService().getSpellsConfig().getLong(activeSpellKey + ".cooldown",
                    plugin.getConfigService().getDefaultCooldown());
            long cdTicks = Math.max(1, cdMs / 50);
            long nowTicks = player.getWorld().getFullTime();
            if (plugin.getCooldownService().isOnCooldown(player.getUniqueId(), activeSpellKey, nowTicks)) {
                long remainingMs = plugin.getCooldownService().remaining(player.getUniqueId(), activeSpellKey, nowTicks) * 50;
                plugin.getFxService().onCooldown(player, display, remainingMs);
                return;
            }

            SpellContext context = new SpellContext(plugin, player, plugin.getConfigService(), plugin.getFxService());
            spell.execute(context);

            // Set cooldown window
            plugin.getCooldownService().set(player.getUniqueId(), activeSpellKey, nowTicks + cdTicks);

            plugin.getFxService().actionBarSound(player,
                    Component.text("Casting: ").color(NamedTextColor.YELLOW)
                            .append(Component.text(display).color(NamedTextColor.AQUA)),
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        }
    }

    private String stripMiniTags(String s) {
        if (s == null) return "";
        // Very simple strip for <#RRGGBB> and named tags like <red>
        return s.replaceAll("<[^>]+>", "");
    }
}
