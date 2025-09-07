package com.example.empirewand.core.wand;

import com.example.empirewand.api.ServiceHealth;
import com.example.empirewand.api.Version;
import com.example.empirewand.api.WandCustomizer;
import com.example.empirewand.api.WandService;
import com.example.empirewand.api.WandStatistics;
import com.example.empirewand.api.WandTemplate;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static com.example.empirewand.core.storage.Keys.MEPHIDANTES_ZEIST_KEY;
import static com.example.empirewand.core.storage.Keys.WAND_ACTIVE_INDEX;
import static com.example.empirewand.core.storage.Keys.WAND_KEY;
import static com.example.empirewand.core.storage.Keys.WAND_SPELLS;

/**
 * Implementation of the WandService API.
 * Handles creation of and data management for wands.
 */
public class WandServiceImpl implements WandService {

    private final Logger logger;

    public WandServiceImpl(Plugin plugin) {
        this.logger = plugin.getLogger();
    }

    @Override
    public ItemStack createBasicWand() {
        ItemStack wand = new ItemStack(Material.STICK);
        try {
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Empire Wand", NamedTextColor.GOLD));
                meta.lore(Arrays.asList(
                        Component.text("A powerful magical wand", NamedTextColor.GRAY),
                        Component.text("Right-click to cast spells", NamedTextColor.GRAY)));
                wand.setItemMeta(meta);
            }
            markWand(wand);
        } catch (Throwable t) {
            // Non-runtime/test environment: Bukkit ItemFactory not available
            logger.warning("Error creating wand, likely due to non-runtime environment: " + t.getMessage());
        }
        return wand;
    }

    @Override
    public ItemStack createMephidantesZeist() {
        ItemStack zeist = new ItemStack(Material.NETHERITE_HOE);
        try {
            ItemMeta meta = zeist.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("MephidantesZeist", NamedTextColor.DARK_RED));
                meta.lore(Arrays.asList(
                        Component.text("A legendary Netherite Scythe", NamedTextColor.GRAY),
                        Component.text("Forged in the fires of the Nether", NamedTextColor.GRAY),
                        Component.text("Right-click to cast spells", NamedTextColor.GRAY)));
                zeist.setItemMeta(meta);
            }
            markMephidantesZeist(zeist);
        } catch (Throwable t) {
            // Non-runtime/test environment
            logger.warning("Error creating MephidantesZeist, likely due to non-runtime environment: " + t.getMessage());
        }
        return zeist;
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

    @Override
    public boolean isMephidantesZeist(ItemStack item) {
        if (item == null)
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(MEPHIDANTES_ZEIST_KEY, PersistentDataType.BYTE);
    }

    private void markWand(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(WAND_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
    }

    private void markMephidantesZeist(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(MEPHIDANTES_ZEIST_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
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
        return (isWand(item) || isMephidantesZeist(item)) ? item : null;
    }

    @Override
    public boolean giveWand(Player player) {
        ItemStack wand = createBasicWand();
        player.getInventory().addItem(wand);
        return true;
    }

    @Override
    public boolean giveMephidantesZeist(Player player) {
        ItemStack zeist = createMephidantesZeist();
        player.getInventory().addItem(zeist);
        return true;
    }

    @Override
    public List<String> getSpells(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return List.of();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String csv = pdc.get(WAND_SPELLS, PersistentDataType.STRING);
        if (csv == null || csv.isEmpty())
            return List.of();
        return new ArrayList<>(Arrays.asList(csv.split(",")));
    }

    @Override
    public void setSpells(ItemStack item, List<String> keys) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(WAND_SPELLS, PersistentDataType.STRING, String.join(",", keys));
        item.setItemMeta(meta);
    }

    @Override
    public int getActiveIndex(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer idx = pdc.get(WAND_ACTIVE_INDEX, PersistentDataType.INTEGER);
        return idx == null ? 0 : idx;
    }

    @Override
    public void setActiveIndex(ItemStack item, int index) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        meta.getPersistentDataContainer().set(WAND_ACTIVE_INDEX, PersistentDataType.INTEGER, index);
        item.setItemMeta(meta);
    }

    // ===== IMPLEMENTATION OF MISSING METHODS =====

    @Override
    public String getServiceName() {
        return "WandService";
    }

    @Override
    public Version getServiceVersion() {
        return Version.of(2, 0, 0);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public ServiceHealth getHealth() {
        return ServiceHealth.HEALTHY;
    }

    @Override
    public void reload() {
        // Reload wand configurations if needed
    }

    @Override
    public WandBuilder createWand() {
        return new WandBuilderImpl();
    }

    @Override
    public WandBuilder createWand(WandTemplate template) {
        return new WandBuilderImpl().applyTemplate(template);
    }

    @Override
    public WandCustomizer getCustomizer(ItemStack wand) {
        return new WandCustomizerImpl(wand);
    }

    @Override
    public WandTemplate.Builder createTemplate(String name) {
        return new WandTemplateBuilderImpl(name);
    }

    @Override
    public Optional<WandTemplate> getTemplate(String name) {
        // For now, return empty - templates not implemented
        return Optional.empty();
    }

    @Override
    public Set<String> getAvailableTemplates() {
        return Collections.emptySet();
    }

    @Override
    public boolean registerTemplate(WandTemplate template) {
        // Template registration not implemented yet
        return false;
    }

    @Override
    public boolean unregisterTemplate(String name) {
        // Template unregistration not implemented yet
        return false;
    }

    @Override
    public WandStatistics getStatistics(ItemStack wand) {
        return new WandStatisticsImpl(wand);
    }

    @Override
    public WandStatistics getGlobalStatistics() {
        return new WandStatisticsImpl(null);
    }

    @Override
    public boolean mergeWands(ItemStack source, ItemStack target) {
        if (!isWand(source) || !isWand(target)) {
            return false;
        }

        List<String> sourceSpells = getSpells(source);
        List<String> targetSpells = new ArrayList<>(getSpells(target));

        for (String spell : sourceSpells) {
            if (!targetSpells.contains(spell)) {
                targetSpells.add(spell);
            }
        }

        setSpells(target, targetSpells);
        return true;
    }

    @Override
    public Optional<ItemStack> splitWand(ItemStack wand, String spellKey) {
        if (!isWand(wand) || !getSpells(wand).contains(spellKey)) {
            return Optional.empty();
        }

        // Create new wand with the extracted spell
        ItemStack newWand = createBasicWand();
        setSpells(newWand, Collections.singletonList(spellKey));

        // Remove spell from original wand
        List<String> spells = new ArrayList<>(getSpells(wand));
        spells.remove(spellKey);
        setSpells(wand, spells);

        return Optional.of(newWand);
    }

    @Override
    public ItemStack cloneWand(ItemStack wand) {
        if (!isWand(wand)) {
            return null;
        }

        ItemStack clone = new ItemStack(wand.getType());
        clone.setItemMeta(wand.getItemMeta());
        return clone;
    }

    @Override
    public boolean repairWand(ItemStack wand) {
        if (!isWand(wand)) {
            return false;
        }

        // For now, just return true - actual repair logic would depend on durability
        // system
        return true;
    }

    // ===== INNER CLASSES =====

    private class WandBuilderImpl implements WandBuilder {
        private Material material = Material.STICK;
        private Component name = Component.text("Empire Wand", NamedTextColor.GOLD);
        private Component[] lore = new Component[0];
        private String[] spellKeys = new String[0];
        private String activeSpell = null;
        private Map<String, Object> customData = new HashMap<>();
        private Map<org.bukkit.enchantments.Enchantment, Integer> enchantments = new HashMap<>();

        @Override
        public WandBuilder material(org.bukkit.Material material) {
            this.material = material;
            return this;
        }

        @Override
        public WandBuilder name(Component name) {
            this.name = name;
            return this;
        }

        @Override
        public WandBuilder lore(Component... lore) {
            this.lore = lore;
            return this;
        }

        @Override
        public WandBuilder spells(String... spellKeys) {
            this.spellKeys = spellKeys;
            return this;
        }

        @Override
        public WandBuilder activeSpell(String spellKey) {
            this.activeSpell = spellKey;
            return this;
        }

        @Override
        public WandBuilder customData(String key, Object value) {
            this.customData.put(key, value);
            return this;
        }

        @Override
        public WandBuilder enchantments(Map<org.bukkit.enchantments.Enchantment, Integer> enchantments) {
            this.enchantments = enchantments;
            return this;
        }

        public WandBuilder applyTemplate(WandTemplate template) {
            // Template application not implemented yet
            return this;
        }

        @Override
        public ItemStack build() {
            ItemStack wand = new ItemStack(material);
            try {
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    meta.displayName(name);
                    if (lore.length > 0) {
                        meta.lore(Arrays.asList(lore));
                    }

                    // Apply enchantments
                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchantments.entrySet()) {
                        meta.addEnchant(entry.getKey(), entry.getValue(), true);
                    }

                    wand.setItemMeta(meta);
                }

                // Mark as wand and set spells
                markWand(wand);
                if (spellKeys.length > 0) {
                    setSpells(wand, Arrays.asList(spellKeys));
                }

                // Set active spell if specified
                if (activeSpell != null && Arrays.asList(spellKeys).contains(activeSpell)) {
                    setActiveIndex(wand, Arrays.asList(spellKeys).indexOf(activeSpell));
                }

            } catch (Throwable t) {
                logger.warning("Error building wand: " + t.getMessage());
            }
            return wand;
        }
    }

    private class WandCustomizerImpl implements WandCustomizer {
        private final ItemStack wand;

        public WandCustomizerImpl(ItemStack wand) {
            this.wand = wand;
        }

        @Override
        public WandCustomizer setDisplayName(Component displayName) {
            // Implementation would set the wand's display name
            return this;
        }

        @Override
        public WandCustomizer setLore(Component... lore) {
            // Implementation would set the wand's lore
            return this;
        }

        @Override
        public WandCustomizer addLore(Component... lore) {
            // Implementation would modify the wand's lore
            return this;
        }

        @Override
        public WandCustomizer setMaterial(Material material) {
            // Implementation would change the wand's material
            return this;
        }

        @Override
        public WandCustomizer addEnchantment(org.bukkit.enchantments.Enchantment enchantment, int level) {
            // Implementation would add enchantment to the wand
            return this;
        }

        @Override
        public WandCustomizer removeEnchantment(org.bukkit.enchantments.Enchantment enchantment) {
            // Implementation would remove enchantment from the wand
            return this;
        }

        @Override
        public WandCustomizer setEnchantments(Map<org.bukkit.enchantments.Enchantment, Integer> enchantments) {
            // Implementation would set enchantments on the wand
            return this;
        }

        @Override
        public WandCustomizer setProperty(String key, Object value) {
            // Implementation would set custom property on the wand
            return this;
        }

        @Override
        public WandCustomizer removeProperty(String key) {
            // Implementation would remove custom property from the wand
            return this;
        }

        @Override
        public ItemStack apply() {
            // Apply all customizations and return the modified wand
            return wand;
        }
    }

    private class WandTemplateBuilderImpl implements WandTemplate.Builder {
        @SuppressWarnings("unused")
        private final String name;
        @SuppressWarnings("unused")
        private Component displayName;
        @SuppressWarnings("unused")
        private Material material = Material.STICK;
        @SuppressWarnings("unused")
        private String[] defaultSpells = new String[0];
        @SuppressWarnings("unused")
        private Component[] defaultLore = new Component[0];
        @SuppressWarnings("unused")
        private Map<org.bukkit.enchantments.Enchantment, Integer> defaultEnchantments = new HashMap<>();
        @SuppressWarnings("unused")
        private Map<String, Object> properties = new HashMap<>();

        public WandTemplateBuilderImpl(String name) {
            this.name = name;
            this.displayName = Component.text(name);
        }

        @Override
        public WandTemplate.Builder displayName(Component displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public WandTemplate.Builder material(Material material) {
            this.material = material;
            return this;
        }

        @Override
        public WandTemplate.Builder defaultSpells(String... spellKeys) {
            this.defaultSpells = spellKeys;
            return this;
        }

        @Override
        public WandTemplate.Builder defaultLore(Component... lore) {
            this.defaultLore = lore;
            return this;
        }

        @Override
        public WandTemplate.Builder defaultEnchantments(
                Map<org.bukkit.enchantments.Enchantment, Integer> enchantments) {
            this.defaultEnchantments = enchantments;
            return this;
        }

        @Override
        public WandTemplate.Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        @Override
        public WandTemplate build() {
            // Template building not fully implemented yet
            throw new UnsupportedOperationException("Template building not yet implemented");
        }
    }

    private class WandStatisticsImpl implements WandStatistics {
        private final ItemStack wand;

        public WandStatisticsImpl(ItemStack wand) {
            this.wand = wand;
        }

        @Override
        public int getSpellCount() {
            return wand != null ? getSpells(wand).size() : 0;
        }

        @Override
        public long getUsageCount() {
            return 0; // Not tracked yet
        }

        @Override
        public String getMostUsedSpell() {
            return null; // Not tracked yet
        }

        @Override
        public long getSpellUsageCount(String spellKey) {
            return 0; // Not tracked yet
        }

        @Override
        public long getCreationTimestamp() {
            return 0; // Not tracked yet
        }

        @Override
        public long getLastUsedTimestamp() {
            return 0; // Not tracked yet
        }
    }
}
