package com.example.empirewand.core;

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
public class WandData {

    public WandData(Plugin plugin) {
        // Constructor can be used for DI if needed, but keys are now static
    }

    public boolean isWand(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(WAND_KEY, PersistentDataType.BYTE);
    }

    public void markWand(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(WAND_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    public List<String> getSpells(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return List.of();
        var pdc = meta.getPersistentDataContainer();
        String csv = pdc.get(WAND_SPELLS, PersistentDataType.STRING);
        if (csv == null || csv.isEmpty()) return List.of();
        return new ArrayList<>(Arrays.asList(csv.split(",")));
    }

    public void setSpells(ItemStack item, List<String> keys) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        var pdc = meta.getPersistentDataContainer();
        pdc.set(WAND_SPELLS, PersistentDataType.STRING, String.join(",", keys));
        item.setItemMeta(meta);
    }

    public int getActiveIndex(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return 0;
        var pdc = meta.getPersistentDataContainer();
        Integer idx = pdc.get(WAND_ACTIVE_INDEX, PersistentDataType.INTEGER);
        return idx == null ? 0 : idx;
    }

    public void setActiveIndex(ItemStack item, int index) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(WAND_ACTIVE_INDEX, PersistentDataType.INTEGER, index);
        item.setItemMeta(meta);
    }
}


