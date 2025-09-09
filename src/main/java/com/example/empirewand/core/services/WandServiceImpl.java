package com.example.empirewand.core.services;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.api.ServiceHealth;
import com.example.empirewand.api.SpellRegistry;
import com.example.empirewand.api.Version;
import com.example.empirewand.api.WandCustomizer;
import com.example.empirewand.api.WandService;
import com.example.empirewand.api.WandStatistics;
import com.example.empirewand.api.WandTemplate;
import com.example.empirewand.core.storage.Keys;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of the WandService API for managing EmpireWand wands.
 */
public class WandServiceImpl implements WandService {

    private final EmpireWandPlugin plugin;
    @SuppressWarnings("unused")
    private final SpellRegistry spellRegistry;

    // Internal storage for wand templates
    private final Map<String, WandTemplate> templates = new HashMap<>();

    public WandServiceImpl(EmpireWandPlugin plugin, SpellRegistry spellRegistry) {
        this.plugin = plugin;
        this.spellRegistry = spellRegistry;
        // Removed: initializeDefaultTemplates(); to prevent 'this' escape
    }

    private void initializeDefaultTemplates() {
        // Create default templates
        WandTemplate basicWand = createTemplate("basic_wand")
                .displayName(Component.text("Basic Wand", NamedTextColor.YELLOW))
                .material(Material.BLAZE_ROD)
                .defaultSpells("magic-missile", "heal")
                .build();
        registerTemplate(basicWand);

        WandTemplate fireWand = createTemplate("fire_wand")
                .displayName(Component.text("Fire Wand", NamedTextColor.RED))
                .material(Material.BLAZE_ROD)
                .defaultSpells("fireball", "flame-wave")
                .build();
        registerTemplate(fireWand);

        WandTemplate iceWand = createTemplate("ice_wand")
                .displayName(Component.text("Ice Wand", NamedTextColor.AQUA))
                .material(Material.STICK)
                .defaultSpells("glacial-spike", "frost-nova")
                .build();
        registerTemplate(iceWand);
    }

    // ===== EMPIRE WAND SERVICE IMPLEMENTATION =====

    @Override
    @NotNull
    public String getServiceName() {
        return "WandService";
    }

    @Override
    @NotNull
    public Version getServiceVersion() {
        return Version.of(2, 0, 0);
    }

    @Override
    public boolean isEnabled() {
        return true; // Service is always enabled for now
    }

    @Override
    @NotNull
    public ServiceHealth getHealth() {
        return ServiceHealth.HEALTHY;
    }

    @Override
    public void reload() {
        // Reload templates and configuration
        templates.clear();
        initializeDefaultTemplates();
    }

    // ===== EXISTING METHODS (ENHANCED) =====

    @Override
    @NotNull
    public ItemStack createBasicWand() {
        return createWand()
                .material(Material.BLAZE_ROD)
                .name(Component.text("Empire Wand", NamedTextColor.DARK_RED))
                .spells("magic-missile", "heal")
                .build();
    }

    @Override
    public boolean isWand(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(Keys.WAND_TYPE, PersistentDataType.STRING);
    }

    @Override
    @NotNull
    public List<String> getBoundSpells(@NotNull ItemStack wand) {
        if (!isWand(wand))
            return new ArrayList<>();
        PersistentDataContainer pdc = wand.getItemMeta().getPersistentDataContainer();
        String spellsData = pdc.getOrDefault(Keys.WAND_SPELLS, PersistentDataType.STRING, "");
        if (spellsData.isEmpty())
            return new ArrayList<>();
        return Arrays.asList(spellsData.split(","));
    }

    @Override
    @NotNull
    public List<String> getSpells(@NotNull ItemStack item) {
        return getBoundSpells(item);
    }

    @Override
    public void setSpells(@NotNull ItemStack wand, @NotNull List<String> spellKeys) {
        if (!isWand(wand))
            return;
        ItemMeta meta = wand.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String spellsData = String.join(",", spellKeys);
        pdc.set(Keys.WAND_SPELLS, PersistentDataType.STRING, spellsData);
        // Clamp active index to new bounds to avoid out-of-range access
        int current = pdc.getOrDefault(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER, 0);
        int maxIndex = Math.max(0, spellKeys.size() - 1);
        int clamped = Math.min(Math.max(0, current), maxIndex);
        pdc.set(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER, clamped);
        wand.setItemMeta(meta);
    }

    @Override
    public int getActiveIndex(@NotNull ItemStack wand) {
        if (!isWand(wand))
            return 0;
        PersistentDataContainer pdc = wand.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER, 0);
    }

    @Override
    public void setActiveIndex(@NotNull ItemStack wand, int index) {
        if (!isWand(wand))
            return;
        ItemMeta meta = wand.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER, Math.max(0, index));
        wand.setItemMeta(meta);
    }

    @Override
    public boolean bindSpell(@NotNull ItemStack wand, @NotNull String spellKey) {
        if (!isWand(wand))
            return false;
        List<String> spells = new ArrayList<>(getBoundSpells(wand));
        if (!spells.contains(spellKey)) {
            spells.add(spellKey);
            setSpells(wand, spells);
            return true;
        }
        return false;
    }

    @Override
    public boolean unbindSpell(@NotNull ItemStack wand, @NotNull String spellKey) {
        if (!isWand(wand))
            return false;
        List<String> spells = new ArrayList<>(getBoundSpells(wand));
        if (spells.remove(spellKey)) {
            setSpells(wand, spells);
            return true;
        }
        return false;
    }

    @Override
    public boolean setActiveSpell(@NotNull ItemStack wand, int index) {
        if (!isWand(wand))
            return false;
        List<String> spells = getBoundSpells(wand);
        if (index >= 0 && index < spells.size()) {
            setActiveIndex(wand, index);
            return true;
        }
        return false;
    }

    @Override
    public int getActiveSpellIndex(@NotNull ItemStack wand) {
        return getActiveIndex(wand);
    }

    @Override
    @Nullable
    public String getActiveSpellKey(@NotNull ItemStack wand) {
        if (!isWand(wand))
            return null;
        List<String> spells = getBoundSpells(wand);
        int index = getActiveIndex(wand);
        if (index >= 0 && index < spells.size()) {
            return spells.get(index);
        }
        return null;
    }

    @Override
    @Nullable
    public ItemStack getHeldWand(@NotNull Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return isWand(item) ? item : null;
    }

    @Override
    @NotNull
    public ItemStack createMephidantesZeist() {
        ItemStack item = createWand()
                .material(Material.NETHERITE_HOE)
                .name(Component.text("Mephidantes' Zeist", NamedTextColor.DARK_RED))
                .spells("mephidic-reap", "blood-block", "hemorrhage")
                .build();
        // Mark this wand with its specific type so isMephidantesZeist() works
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(Keys.WAND_TYPE, PersistentDataType.STRING, "mephidantes_zeist");
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean isMephidantesZeist(@Nullable ItemStack item) {
        if (!isWand(item))
            return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String type = pdc.getOrDefault(Keys.WAND_TYPE, PersistentDataType.STRING, "");
        return "mephidantes_zeist".equals(type);
    }

    @Override
    public boolean giveWand(@NotNull Player player) {
        ItemStack wand = createBasicWand();
        return player.getInventory().addItem(wand).isEmpty();
    }

    @Override
    public boolean giveMephidantesZeist(@NotNull Player player) {
        ItemStack zeist = createMephidantesZeist();
        return player.getInventory().addItem(zeist).isEmpty();
    }

    // ===== NEW ADVANCED METHODS =====

    @Override
    @NotNull
    public WandBuilder createWand() {
        return new WandBuilderImpl();
    }

    @Override
    @NotNull
    public WandBuilder createWand(@NotNull WandTemplate template) {
        return new WandBuilderImpl(template);
    }

    @Override
    @NotNull
    public WandCustomizer getCustomizer(@NotNull ItemStack wand) {
        return new WandCustomizerImpl(wand);
    }

    // ===== TEMPLATE MANAGEMENT =====

    @Override
    @NotNull
    public WandTemplate.Builder createTemplate(@NotNull String name) {
        return new WandTemplateImpl.BuilderImpl(name);
    }

    @Override
    @NotNull
    public Optional<WandTemplate> getTemplate(@NotNull String name) {
        if (templates.isEmpty()) {
            initializeDefaultTemplates(); // Lazy initialization
        }
        return Optional.ofNullable(templates.get(name));
    }

    @Override
    @NotNull
    public Set<String> getAvailableTemplates() {
        if (templates.isEmpty()) {
            initializeDefaultTemplates(); // Lazy initialization
        }
        return new HashSet<>(templates.keySet());
    }

    @Override
    public boolean registerTemplate(@NotNull WandTemplate template) {
        templates.put(template.getName(), template);
        return true;
    }

    @Override
    public boolean unregisterTemplate(@NotNull String name) {
        return templates.remove(name) != null;
    }

    // ===== STATISTICS AND ANALYTICS =====

    @Override
    @NotNull
    public WandStatistics getStatistics(@NotNull ItemStack wand) {
        // TODO: Implement wand statistics tracking and a command for it
        return new WandStatisticsImpl();
    }

    @Override
    @NotNull
    public WandStatistics getGlobalStatistics() {
        // TODO: Implement global statistics tracking and a command for it
        return new WandStatisticsImpl();
    }

    // ===== ADVANCED OPERATIONS =====

    @Override
    public boolean mergeWands(@NotNull ItemStack source, @NotNull ItemStack target) {
        if (!isWand(source) || !isWand(target))
            return false;

        List<String> sourceSpells = getBoundSpells(source);
        List<String> targetSpells = getBoundSpells(target);

        Set<String> mergedSpells = new LinkedHashSet<>(targetSpells);
        mergedSpells.addAll(sourceSpells);

        setSpells(target, new ArrayList<>(mergedSpells));
        return true;
    }

    @Override
    @NotNull
    public Optional<ItemStack> splitWand(@NotNull ItemStack wand, @NotNull String spellKey) {
        if (!isWand(wand))
            return Optional.empty();

        List<String> spells = getBoundSpells(wand);
        if (!spells.contains(spellKey))
            return Optional.empty();

        // Remove spell from original wand
        spells.remove(spellKey);
        setSpells(wand, spells);

        // Create new wand with just this spell
        return Optional.of(createWand()
                .material(wand.getType())
                .name(Component.text("Split Wand", NamedTextColor.GRAY))
                .spells(spellKey)
                .build());
    }

    @Override
    @NotNull
    public ItemStack cloneWand(@NotNull ItemStack wand) {
        if (!isWand(wand))
            return wand.clone();

        ItemStack clone = wand.clone();
        // Ensure the clone has the same wand data
        if (clone.hasItemMeta()) {
            ItemMeta meta = clone.getItemMeta();
            if (meta != null) {
                PersistentDataContainer originalPdc = wand.getItemMeta().getPersistentDataContainer();
                PersistentDataContainer clonePdc = meta.getPersistentDataContainer();

                // Copy all wand-related data
                for (NamespacedKey key : originalPdc.getKeys()) {
                    if (key.getNamespace().equals("empirewand")) {
                        // Copy the data based on type
                        if (originalPdc.has(key, PersistentDataType.STRING)) {
                            String value = originalPdc.get(key, PersistentDataType.STRING);
                            if (value != null) {
                                clonePdc.set(key, PersistentDataType.STRING, value);
                            }
                        } else if (originalPdc.has(key, PersistentDataType.INTEGER)) {
                            Integer value = originalPdc.get(key, PersistentDataType.INTEGER);
                            if (value != null) {
                                clonePdc.set(key, PersistentDataType.INTEGER, value);
                            }
                        }
                    }
                }
                clone.setItemMeta(meta);
            }
        }
        return clone;
    }

    @Override
    public boolean repairWand(@NotNull ItemStack wand) {
        if (!isWand(wand))
            return false;
        // just ensure the item is not damaged
        return true;
    }

    // ===== INNER CLASSES =====

    private class WandBuilderImpl implements WandBuilder {
        private Material material = Material.STICK;
        private Component name = Component.text("Wand");
        private List<Component> lore = new ArrayList<>();
        private List<String> spells = new ArrayList<>();
        private String activeSpell = null;
        private final Map<String, Object> customData = new HashMap<>();
        private Map<org.bukkit.enchantments.Enchantment, Integer> enchantments = new HashMap<>();

        public WandBuilderImpl() {
        }

        public WandBuilderImpl(WandTemplate template) {
            this.material = template.getMaterial();
            this.name = template.getDisplayName();
            this.lore = Arrays.asList(template.getDefaultLore());
            this.spells = new ArrayList<>(template.getDefaultSpells());
            this.enchantments = new HashMap<>(template.getDefaultEnchantments());
        }

        @Override
        @NotNull
        public WandBuilder material(@NotNull Material material) {
            this.material = material;
            return this;
        }

        @Override
        @NotNull
        public WandBuilder name(@NotNull Component name) {
            this.name = name;
            return this;
        }

        @Override
        @NotNull
        public WandBuilder lore(@NotNull Component... lore) {
            this.lore = Arrays.asList(lore);
            return this;
        }

        @Override
        @NotNull
        public WandBuilder spells(@NotNull String... spellKeys) {
            this.spells = Arrays.asList(spellKeys);
            return this;
        }

        @Override
        @NotNull
        public WandBuilder activeSpell(@NotNull String spellKey) {
            this.activeSpell = spellKey;
            return this;
        }

        @Override
        @NotNull
        public WandBuilder customData(@NotNull String key, Object value) {
            this.customData.put(key, value);
            return this;
        }

        @Override
        @NotNull
        public WandBuilder enchantments(@NotNull Map<org.bukkit.enchantments.Enchantment, Integer> enchantments) {
            this.enchantments = new HashMap<>(enchantments);
            return this;
        }

        @Override
        @NotNull
        public ItemStack build() {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                return item;

            // Set display name and lore
            meta.displayName(name);
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }

            // Add enchantments
            for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }

            // Set wand data
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(Keys.WAND_TYPE, PersistentDataType.STRING, "empire_wand");
            if (!spells.isEmpty()) {
                pdc.set(Keys.WAND_SPELLS, PersistentDataType.STRING, String.join(",", spells));
                int idx = 0;
                if (activeSpell != null) {
                    int found = spells.indexOf(activeSpell);
                    idx = (found >= 0) ? found : 0;
                }
                pdc.set(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER, idx);
            }

            // Add custom data
            for (Map.Entry<String, Object> entry : customData.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String strValue) {
                    pdc.set(new NamespacedKey(plugin, "custom_" + entry.getKey()),
                            PersistentDataType.STRING, strValue);
                } else if (value instanceof Integer intValue) {
                    pdc.set(new NamespacedKey(plugin, "custom_" + entry.getKey()),
                            PersistentDataType.INTEGER, intValue);
                }
            }

            item.setItemMeta(meta);
            return item;
        }
    }

    private static class WandCustomizerImpl implements WandCustomizer {
        private final ItemStack wand;

        public WandCustomizerImpl(ItemStack wand) {
            this.wand = wand;
        }

        @Override
        @NotNull
        public WandCustomizer setDisplayName(@NotNull Component displayName) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                meta.displayName(displayName);
                wand.setItemMeta(meta);
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer setLore(@NotNull Component... lore) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                meta.lore(Arrays.asList(lore));
                wand.setItemMeta(meta);
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer addLore(@NotNull Component... lore) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                List<Component> currentLore = meta.lore();
                if (currentLore == null)
                    currentLore = new ArrayList<>();
                currentLore.addAll(Arrays.asList(lore));
                meta.lore(currentLore);
                wand.setItemMeta(meta);
            }
            return this;
        }

        @Override
        @NotNull
        @SuppressWarnings("deprecation")
        public WandCustomizer setMaterial(@NotNull Material material) {
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                // Change the underlying material while preserving metadata
                wand.setType(material);
                wand.setItemMeta(meta);
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer addEnchantment(@NotNull org.bukkit.enchantments.Enchantment enchantment, int level) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                meta.addEnchant(enchantment, level, true);
                wand.setItemMeta(meta);
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer removeEnchantment(@NotNull org.bukkit.enchantments.Enchantment enchantment) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                meta.removeEnchant(enchantment);
                wand.setItemMeta(meta);
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer setEnchantments(@NotNull Map<org.bukkit.enchantments.Enchantment, Integer> enchantments) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                // Clear existing enchantments
                for (org.bukkit.enchantments.Enchantment ench : new java.util.ArrayList<>(
                        meta.getEnchants().keySet())) {
                    meta.removeEnchant(ench);
                }
                // Add new enchantments
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
                wand.setItemMeta(meta);
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer setProperty(@NotNull String key, @NotNull Object value) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                NamespacedKey nsKey = new NamespacedKey("empirewand", "custom_" + key);
                if (value instanceof String strValue) {
                    pdc.set(nsKey, PersistentDataType.STRING, strValue);
                } else if (value instanceof Integer intValue) {
                    pdc.set(nsKey, PersistentDataType.INTEGER, intValue);
                }
                wand.setItemMeta(meta);
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer removeProperty(@NotNull String key) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                NamespacedKey nsKey = new NamespacedKey("empirewand", "custom_" + key);
                pdc.remove(nsKey);
                wand.setItemMeta(meta);
            }
            return this;
        }

        @Override
        @NotNull
        public ItemStack apply() {
            // Changes are applied immediately
            return wand;
        }
    }

    private static class WandStatisticsImpl implements WandStatistics {
        @Override
        public int getSpellCount() {
            return 0;
        }

        @Override
        public long getUsageCount() {
            return 0;
        }

        @Override
        @Nullable
        public String getMostUsedSpell() {
            return null;
        }

        @Override
        public long getSpellUsageCount(@NotNull String spellKey) {
            return 0;
        }

        @Override
        public long getCreationTimestamp() {
            return 0;
        }

        @Override
        public long getLastUsedTimestamp() {
            return 0;
        }
    }
}
