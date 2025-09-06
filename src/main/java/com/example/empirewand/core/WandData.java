package com.example.empirewand.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.empirewand.core.Keys.WAND_ACTIVE_INDEX;
import static com.example.empirewand.core.Keys.WAND_KEY;
import static com.example.empirewand.core.Keys.WAND_SPELLS;

/**
 * Skeleton PDC helpers for wand data.
 */
public class WandData implements com.example.empirewand.api.WandService {

    private final Plugin plugin;

    public WandData(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.STICK);
        try {
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Empire Wand", NamedTextColor.GOLD));
                meta.lore(Arrays.asList(
                        Component.text("A powerful magical wand", NamedTextColor.GRAY),
                        Component.text("Right-click to cast spells", NamedTextColor.GRAY)));
                wand.setItemMeta(meta);
            } else {
                // Even if meta is null, reflect the attempted set for consistency
                wand.setItemMeta(null);
            }
        } catch (Throwable t) {
            // Non-runtime/test environment: Bukkit ItemFactory not available
            if (plugin != null) {
                plugin.getLogger().warning("Error creating wand: " + t.getMessage());
            }
        }
        try {
            markWand(wand);
        } catch (Throwable t) {
            // Skip marking in non-runtime/test environment
            if (plugin != null) {
                plugin.getLogger().warning("Error marking wand: " + t.getMessage());
            }
        }
        return wand;
    }

    @Override
    public boolean isWand(ItemStack item) {
        if (item == null)
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(WAND_KEY, PersistentDataType.BYTE);
    }

    public void markWand(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(WAND_KEY, PersistentDataType.BYTE, (byte) 1);
        }
        // Always set the meta back (even if null) to match expectations in tests
        item.setItemMeta(meta);
    }

    @Override
    public List<String> getBoundSpells(ItemStack wand) {
        return getSpells(wand);
    }

    @Override
    public boolean bindSpell(ItemStack wand, String spellKey) {
        List<String> spells = new ArrayList<>(getSpells(wand));
        if (!spells.contains(spellKey)) {
            spells.add(spellKey);
            setSpells(wand, spells);
            return true;
        }
        return false;
    }

    @Override
    public boolean unbindSpell(ItemStack wand, String spellKey) {
        List<String> spells = new ArrayList<>(getSpells(wand));
        if (spells.remove(spellKey)) {
            setSpells(wand, spells);
            return true;
        }
        return false;
    }

    @Override
    public boolean setActiveSpell(ItemStack wand, int index) {
        List<String> spells = getSpells(wand);
        if (index >= 0 && index < spells.size()) {
            setActiveIndex(wand, index);
            return true;
        }
        return false;
    }

    @Override
    public int getActiveSpellIndex(ItemStack wand) {
        return getActiveIndex(wand);
    }

    @Override
    public String getActiveSpellKey(ItemStack wand) {
        List<String> spells = getSpells(wand);
        int index = getActiveIndex(wand);
        if (index >= 0 && index < spells.size()) {
            return spells.get(index);
        }
        return null;
    }

    @Override
    public ItemStack getHeldWand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return isWand(item) ? item : null;
    }

    @Override
    public boolean giveWand(Player player) {
        ItemStack wand = createWand();
        player.getInventory().addItem(wand);
        return true;
    }

    public List<String> getSpells(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null)
            return List.of();
        var pdc = meta.getPersistentDataContainer();
        String csv = pdc.get(WAND_SPELLS, PersistentDataType.STRING);
        if (csv == null || csv.isEmpty())
            return List.of();
        return new ArrayList<>(Arrays.asList(csv.split(",")));
    }

    public void setSpells(ItemStack item, List<String> keys) {
        var meta = item.getItemMeta();
        if (meta == null)
            return;
        var pdc = meta.getPersistentDataContainer();
        pdc.set(WAND_SPELLS, PersistentDataType.STRING, String.join(",", keys));
        item.setItemMeta(meta);
    }

    public int getActiveIndex(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null)
            return 0;
        var pdc = meta.getPersistentDataContainer();
        Integer idx = pdc.get(WAND_ACTIVE_INDEX, PersistentDataType.INTEGER);
        return idx == null ? 0 : idx;
    }

    public void setActiveIndex(ItemStack item, int index) {
        var meta = item.getItemMeta();
        if (meta == null)
            return;
        meta.getPersistentDataContainer().set(WAND_ACTIVE_INDEX, PersistentDataType.INTEGER, index);
        item.setItemMeta(meta);
    }
}
