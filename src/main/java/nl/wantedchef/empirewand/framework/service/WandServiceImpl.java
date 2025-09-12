package nl.wantedchef.empirewand.framework.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.ServiceHealth;
import nl.wantedchef.empirewand.api.Version;
import nl.wantedchef.empirewand.api.service.WandCustomizer;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.api.service.WandStatistics;
import nl.wantedchef.empirewand.api.service.WandTemplate;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.core.util.PerformanceMonitor;

/**
 * The primary implementation of the {@link WandService} API. This class handles
 * the creation, customization, and management of all wands.
 *
 * Optimized for performance with efficient data structures and minimal object
 * creation.
 */
public class WandServiceImpl implements WandService {

    private final EmpireWandPlugin plugin;
    @SuppressWarnings("unused")
    private final SpellRegistry spellRegistry;

    // Performance monitor for tracking wand operations
    private final PerformanceMonitor performanceMonitor;

    // Use ConcurrentHashMap for thread-safe operations and better performance
    private final Map<String, WandTemplate> templates = new ConcurrentHashMap<>(8); // Initial capacity to reduce resizing

    // Cache for frequently accessed wand data to avoid repeated PDC operations
    private final Map<String, List<String>> wandSpellsCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> wandActiveIndexCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new WandServiceImpl.
     *
     * @param plugin The plugin instance.
     * @param spellRegistry The spell registry.
     */
    public WandServiceImpl(EmpireWandPlugin plugin, SpellRegistry spellRegistry) {
        this.plugin = plugin;
        this.spellRegistry = spellRegistry;
        this.performanceMonitor = new PerformanceMonitor(plugin.getLogger());
        // Removed: initializeDefaultTemplates(); to prevent 'this' escape
    }

    /**
     * Initializes the default wand templates.
     */
    private void initializeDefaultTemplates() {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.initializeDefaultTemplates", 50)) {
            timing.observe();
            assert timing != null;
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
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.reload", 100)) {
            timing.observe();
            assert timing != null;
            // Reload templates and configuration
            templates.clear();
            wandSpellsCache.clear();
            wandActiveIndexCache.clear();
            initializeDefaultTemplates();
        }
    }

    /**
     * Shuts down the wand service and cleans up resources. This method should
     * be called during plugin shutdown to prevent memory leaks.
     */
    public void shutdown() {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.shutdown", 50)) {
            timing.observe();
            assert timing != null;
            // Clear templates and caches to free memory
            templates.clear();
            wandSpellsCache.clear();
            wandActiveIndexCache.clear();
        }
    }

    /**
     * Generates a unique cache key for a wand based on its item stack.
     *
     * @param wand The wand item stack
     * @return A unique string key for caching
     */
    private String generateWandCacheKey(@NotNull ItemStack wand) {
        if (!wand.hasItemMeta()) {
            return String.valueOf(wand.hashCode());
        }
        ItemMeta meta = wand.getItemMeta();
        if (meta == null) {
            return String.valueOf(wand.hashCode());
        }
        return String.valueOf(meta.hashCode());
    }

    // ===== EXISTING METHODS (ENHANCED) =====
    @Override
    public boolean isWand(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(Keys.WAND_TYPE, PersistentDataType.STRING);
    }

    @Override
    @NotNull
    public List<String> getBoundSpells(@NotNull ItemStack wand) {
        // Check cache first
        String cacheKey = generateWandCacheKey(wand);
        List<String> cached = wandSpellsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try (var timing = performanceMonitor.startTiming("WandServiceImpl.getBoundSpells", 5)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return Collections.emptyList();
            }

            ItemMeta meta = wand.getItemMeta();
            if (meta == null) {
                return Collections.emptyList();
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String spellsData = pdc.get(Keys.WAND_SPELLS, PersistentDataType.STRING);
            if (spellsData == null || spellsData.isEmpty()) {
                return Collections.emptyList();
            }

            // Split and return as list - avoid creating new ArrayList when possible
            String[] parts = spellsData.split(",");
            if (parts.length == 0) {
                return Collections.emptyList();
            }
            if (parts.length == 1) {
                return Collections.singletonList(parts[0]);
            }

            List<String> result = java.util.Collections.unmodifiableList(java.util.Arrays.asList(parts));
            // Cache the result (store an unmodifiable view to prevent external mutation)
            wandSpellsCache.put(cacheKey, result);
            return result;
        }
    }

    @Override
    @NotNull
    public List<String> getSpells(@NotNull ItemStack item) {
        return getBoundSpells(item);
    }

    @Override
    public void setSpells(@NotNull ItemStack wand, @NotNull List<String> spellKeys) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.setSpells", 10)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return;
            }

            ItemMeta meta = wand.getItemMeta();
            if (meta == null) {
                return;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();

            // Use StringBuilder for better performance when joining strings
            if (spellKeys.isEmpty()) {
                pdc.set(Keys.WAND_SPELLS, PersistentDataType.STRING, "");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < spellKeys.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(spellKeys.get(i));
                }
                pdc.set(Keys.WAND_SPELLS, PersistentDataType.STRING, sb.toString());
            }

            // Clamp active index to new bounds to avoid out-of-range access
            Integer current = pdc.get(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER);
            if (current == null) {
                current = Integer.valueOf(0);
            }

            int maxIndex = Math.max(0, spellKeys.size() - 1);
            int clamped = Math.min(Math.max(0, current), maxIndex);
            pdc.set(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER, clamped);

            wand.setItemMeta(meta);

            // Update cache
            String cacheKey = generateWandCacheKey(wand);
            wandSpellsCache.put(cacheKey, java.util.Collections.unmodifiableList(new ArrayList<>(spellKeys)));
            wandActiveIndexCache.put(cacheKey, clamped);
        }
    }

    @Override
    public int getActiveIndex(@NotNull ItemStack wand) {
        // Check cache first
        String cacheKey = generateWandCacheKey(wand);
        Integer cached = wandActiveIndexCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try (var timing = performanceMonitor.startTiming("WandServiceImpl.getActiveIndex", 5)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return 0;
            }

            ItemMeta meta = wand.getItemMeta();
            if (meta == null) {
                return 0;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Integer index = pdc.get(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER);
            int result = index != null ? Math.max(0, index) : 0;
            // Cache the result
            wandActiveIndexCache.put(cacheKey, result);
            return result;
        }
    }

    @Override
    public void setActiveIndex(@NotNull ItemStack wand, int index) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.setActiveIndex", 5)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return;
            }

            ItemMeta meta = wand.getItemMeta();
            if (meta == null) {
                return;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER, Math.max(0, index));
            wand.setItemMeta(meta);

            // Update cache
            String cacheKey = generateWandCacheKey(wand);
            wandActiveIndexCache.put(cacheKey, Math.max(0, index));
        }
    }

    @Override
    public boolean bindSpell(@NotNull ItemStack wand, @NotNull String spellKey) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.bindSpell", 10)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return false;
            }

            ItemMeta meta = wand.getItemMeta();
            if (meta == null) {
                return false;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String spellsData = pdc.get(Keys.WAND_SPELLS, PersistentDataType.STRING);

            // Check if spell is already bound
            if (spellsData != null && spellsData.contains(spellKey)) {
                // More precise check to avoid partial matches
                String[] existingSpells = spellsData.split(",");
                for (String spell : existingSpells) {
                    if (spellKey.equals(spell)) {
                        return false;
                    }
                }
            }

            // Add spell to the list
            String newSpellsData;
            if (spellsData == null || spellsData.isEmpty()) {
                newSpellsData = spellKey;
            } else {
                newSpellsData = spellsData + "," + spellKey;
            }

            pdc.set(Keys.WAND_SPELLS, PersistentDataType.STRING, newSpellsData);
            wand.setItemMeta(meta);

            // Invalidate cache
            String cacheKey = generateWandCacheKey(wand);
            wandSpellsCache.remove(cacheKey);
            return true;
        }
    }

    @Override
    public boolean unbindSpell(@NotNull ItemStack wand, @NotNull String spellKey) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.unbindSpell", 15)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return false;
            }

            ItemMeta meta = wand.getItemMeta();
            if (meta == null) {
                return false;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String spellsData = pdc.get(Keys.WAND_SPELLS, PersistentDataType.STRING);
            if (spellsData == null || spellsData.isEmpty()) {
                return false;
            }

            // Split, remove spell, and rejoin
            String[] spellsArray = spellsData.split(",");
            List<String> spellsList = new ArrayList<>(Arrays.asList(spellsArray));

            if (spellsList.remove(spellKey)) {
                // Update active index if needed
                Integer currentIndex = pdc.get(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER);
                if (currentIndex != null && currentIndex >= spellsList.size() && !spellsList.isEmpty()) {
                    pdc.set(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER, spellsList.size() - 1);
                }

                // Rebuild spells data
                if (spellsList.isEmpty()) {
                    pdc.set(Keys.WAND_SPELLS, PersistentDataType.STRING, "");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < spellsList.size(); i++) {
                        if (i > 0) {
                            sb.append(',');
                        }
                        sb.append(spellsList.get(i));
                    }
                    pdc.set(Keys.WAND_SPELLS, PersistentDataType.STRING, sb.toString());
                }

                wand.setItemMeta(meta);

                // Update cache
                String cacheKey = generateWandCacheKey(wand);
                wandSpellsCache.put(cacheKey, new ArrayList<>(spellsList));
                return true;
            }

            return false;
        }
    }

    @Override
    public boolean setActiveSpell(@NotNull ItemStack wand, int index) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.setActiveSpell", 5)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return false;
            }

            ItemMeta meta = wand.getItemMeta();
            if (meta == null) {
                return false;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String spellsData = pdc.get(Keys.WAND_SPELLS, PersistentDataType.STRING);
            if (spellsData == null || spellsData.isEmpty()) {
                return false;
            }

            String[] spellsArray = spellsData.split(",");
            if (index >= 0 && index < spellsArray.length) {
                pdc.set(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER, index);
                wand.setItemMeta(meta);

                // Update cache
                String cacheKey = generateWandCacheKey(wand);
                wandActiveIndexCache.put(cacheKey, index);
                return true;
            }

            return false;
        }
    }

    @Override
    public int getActiveSpellIndex(@NotNull ItemStack wand) {
        return getActiveIndex(wand);
    }

    @Override
    @Nullable
    public String getActiveSpellKey(@NotNull ItemStack wand) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.getActiveSpellKey", 5)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return null;
            }

            ItemMeta meta = wand.getItemMeta();
            if (meta == null) {
                return null;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String spellsData = pdc.get(Keys.WAND_SPELLS, PersistentDataType.STRING);
            if (spellsData == null || spellsData.isEmpty()) {
                return null;
            }

            Integer index = pdc.get(Keys.WAND_ACTIVE_SPELL, PersistentDataType.INTEGER);
            if (index == null) {
                index = 0;
            }

            String[] spellsArray = spellsData.split(",");
            if (index >= 0 && index < spellsArray.length) {
                return spellsArray[index];
            }

            return null;
        }
    }

    @Override
    @Nullable
    public ItemStack getHeldWand(@NotNull Player player) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.getHeldWand", 5)) {
            timing.observe();
            assert timing != null;
            ItemStack item = player.getInventory().getItemInMainHand();
            return isWand(item) ? item : null;
        }
    }

    @Override
    @NotNull
    public ItemStack createMephidantesZeist() {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.createMephidantesZeist", 10)) {
            timing.observe();
            assert timing != null;
            ItemStack item = createWand()
                    .material(Material.NETHERITE_HOE)
                    .name(Component.text("Mephidantes' Zeist", NamedTextColor.DARK_RED))
                    .build();
            // Mark this wand with its specific type so isMephidantesZeist() works
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(Keys.WAND_TYPE, PersistentDataType.STRING, "mephidantes_zeist");
                item.setItemMeta(meta);
            }
            return item;
        }
    }

    @Override
    public boolean isMephidantesZeist(@Nullable ItemStack item) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.isMephidantesZeist", 5)) {
            timing.observe();
            assert timing != null;
            if (!isWand(item)) {
                return false;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String type = pdc.get(Keys.WAND_TYPE, PersistentDataType.STRING);
            return "mephidantes_zeist".equals(type);
        }
    }

    @Override
    public boolean giveWand(@NotNull Player player) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.giveWand", 15)) {
            timing.observe();
            assert timing != null;
            ItemStack wand = createWand()
                    .material(Material.BLAZE_ROD)
                    .name(Component.text("Empire Wand", NamedTextColor.DARK_RED))
                    .build();
            return player.getInventory().addItem(wand).isEmpty();
        }
    }

    @Override
    public boolean giveMephidantesZeist(@NotNull Player player) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.giveMephidantesZeist", 15)) {
            timing.observe();
            assert timing != null;
            ItemStack zeist = createMephidantesZeist();
            return player.getInventory().addItem(zeist).isEmpty();
        }
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
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.getTemplate", 5)) {
            timing.observe();
            assert timing != null;
            if (templates.isEmpty()) {
                initializeDefaultTemplates(); // Lazy initialization
            }
            return Optional.ofNullable(templates.get(name));
        }
    }

    @Override
    @NotNull
    public Set<String> getAvailableTemplates() {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.getAvailableTemplates", 5)) {
            timing.observe();
            assert timing != null;
            if (templates.isEmpty()) {
                initializeDefaultTemplates(); // Lazy initialization
            }
            return new HashSet<>(templates.keySet());
        }
    }

    @Override
    public boolean registerTemplate(@NotNull WandTemplate template) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.registerTemplate", 5)) {
            timing.observe();
            assert timing != null;
            return templates.put(template.getName(), template) == null;
        }
    }

    @Override
    public boolean unregisterTemplate(@NotNull String name) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.unregisterTemplate", 5)) {
            timing.observe();
            assert timing != null;
            return templates.remove(name) != null;
        }
    }

    // ===== STATISTICS AND ANALYTICS =====
    @Override
    @NotNull
    public WandStatistics getStatistics(@NotNull ItemStack wand) {
        // Statistics tracking not yet implemented; return zeroed immutable instance
        return WandStatisticsImpl.INSTANCE;
    }

    @Override
    @NotNull
    public WandStatistics getGlobalStatistics() {
        // Statistics tracking not yet implemented; return zeroed immutable instance
        return WandStatisticsImpl.INSTANCE;
    }

    // ===== ADVANCED OPERATIONS =====
    @Override
    public boolean mergeWands(@NotNull ItemStack source, @NotNull ItemStack target) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.mergeWands", 20)) {
            timing.observe();
            assert timing != null;
            if (!isWand(source) || !isWand(target)) {
                return false;
            }

            List<String> sourceSpells = getBoundSpells(source);
            List<String> targetSpells = getBoundSpells(target);

            Set<String> mergedSpells = new LinkedHashSet<>(targetSpells);
            mergedSpells.addAll(sourceSpells);

            setSpells(target, new ArrayList<>(mergedSpells));

            // Invalidate caches for both wands
            wandSpellsCache.remove(generateWandCacheKey(source));
            wandSpellsCache.remove(generateWandCacheKey(target));
            wandActiveIndexCache.remove(generateWandCacheKey(source));
            wandActiveIndexCache.remove(generateWandCacheKey(target));

            return true;
        }
    }

    @Override
    @NotNull
    public Optional<ItemStack> splitWand(@NotNull ItemStack wand, @NotNull String spellKey) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.splitWand", 20)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return Optional.empty();
            }

            List<String> spells = getBoundSpells(wand);
            if (!spells.contains(spellKey)) {
                return Optional.empty();
            }

            // Remove spell from original wand
            List<String> newSpells = new ArrayList<>(spells);
            newSpells.remove(spellKey);
            setSpells(wand, newSpells);

            // Create new wand with just this spell
            Optional<ItemStack> result = Optional.of(createWand()
                    .material(wand.getType())
                    .name(Component.text("Split Wand", NamedTextColor.GRAY))
                    .spells(spellKey)
                    .build());

            // Invalidate cache for original wand
            wandSpellsCache.remove(generateWandCacheKey(wand));
            wandActiveIndexCache.remove(generateWandCacheKey(wand));

            return result;
        }
    }

    @Override
    @NotNull
    public ItemStack cloneWand(@NotNull ItemStack wand) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.cloneWand", 15)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return wand.clone();
            }

            ItemStack clone = wand.clone();
            // Ensure the clone has the same wand data
            if (clone.hasItemMeta()) {
                ItemMeta meta = clone.getItemMeta();
                if (meta != null) {
                    ItemMeta originalMeta = wand.getItemMeta();
                    if (originalMeta != null) {
                        PersistentDataContainer originalPdc = originalMeta.getPersistentDataContainer();
                        PersistentDataContainer clonePdc = meta.getPersistentDataContainer();

                        // Copy all wand-related data more efficiently
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
            }

            // Invalidate cache for clone
            wandSpellsCache.remove(generateWandCacheKey(clone));
            wandActiveIndexCache.remove(generateWandCacheKey(clone));

            return clone;
        }
    }

    @Override
    public boolean repairWand(@NotNull ItemStack wand) {
        try (var timing = performanceMonitor.startTiming("WandServiceImpl.repairWand", 5)) {
            timing.observe();
            assert timing != null;
            if (!isWand(wand)) {
                return false;
            }
            // just ensure the item is not damaged
            return true;
        }
    }

    /**
     * Gets performance metrics for this service.
     *
     * @return A string containing performance metrics.
     */
    public String getPerformanceMetrics() {
        return "Metrics not available.";
    }

    /**
     * Clears performance metrics for this service.
     */
    public void clearPerformanceMetrics() {
        performanceMonitor.clearMetrics();
    }

    // ===== INNER CLASSES =====
    /**
     * An implementation of the {@link WandBuilder} interface.
     *
     * Optimized for performance with pre-allocated collections and efficient
     * string building.
     */
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
            try (var timing = performanceMonitor.startTiming("WandServiceImpl.WandBuilderImpl.build", 15)) {
                timing.observe();
                assert timing != null;
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta == null) {
                    return item;
                }

                // Set display name and lore
                meta.displayName(name);
                if (!lore.isEmpty()) {
                    meta.lore(lore);
                }

                // Add enchantments more efficiently
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }

                // Set wand data
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(Keys.WAND_TYPE, PersistentDataType.STRING, "empire_wand");

                if (!spells.isEmpty()) {
                    // Use StringBuilder for better performance when joining strings
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < spells.size(); i++) {
                        if (i > 0) {
                            sb.append(',');
                        }
                        sb.append(spells.get(i));
                    }
                    pdc.set(Keys.WAND_SPELLS, PersistentDataType.STRING, sb.toString());

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
                try {
                    // Record creation in metrics service if available
                    nl.wantedchef.empirewand.framework.service.metrics.MetricsService metrics = plugin.getMetricsService();
                    if (metrics != null) {
                        metrics.recordWandCreated();
                    }
                } catch (Throwable ignored) { }
                return item;
            }
        }
    }

    /**
     * An implementation of the {@link WandCustomizer} interface.
     *
     * Optimized for performance with direct field access and reduced object
     * creation.
     */
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
                if (meta != null) {
                    meta.displayName(displayName);
                    wand.setItemMeta(meta);
                }
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer setLore(@NotNull Component... lore) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    meta.lore(Arrays.asList(lore));
                    wand.setItemMeta(meta);
                }
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer addLore(@NotNull Component... lore) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    List<Component> currentLore = meta.lore();
                    if (currentLore == null) {
                        currentLore = new ArrayList<>();
                    }
                    currentLore.addAll(Arrays.asList(lore));
                    meta.lore(currentLore);
                    wand.setItemMeta(meta);
                }
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
                if (meta != null) {
                    meta.addEnchant(enchantment, level, true);
                    wand.setItemMeta(meta);
                }
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer removeEnchantment(@NotNull org.bukkit.enchantments.Enchantment enchantment) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    meta.removeEnchant(enchantment);
                    wand.setItemMeta(meta);
                }
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer setEnchantments(@NotNull Map<org.bukkit.enchantments.Enchantment, Integer> enchantments) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    // Clear existing enchantments more efficiently
                    Set<org.bukkit.enchantments.Enchantment> toRemove = new HashSet<>(meta.getEnchants().keySet());
                    for (org.bukkit.enchantments.Enchantment ench : toRemove) {
                        meta.removeEnchant(ench);
                    }
                    // Add new enchantments
                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchantments.entrySet()) {
                        meta.addEnchant(entry.getKey(), entry.getValue(), true);
                    }
                    wand.setItemMeta(meta);
                }
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer setProperty(@NotNull String key, @NotNull Object value) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    NamespacedKey nsKey = new NamespacedKey("empirewand", "custom_" + key);
                    if (value instanceof String strValue) {
                        pdc.set(nsKey, PersistentDataType.STRING, strValue);
                    } else if (value instanceof Integer intValue) {
                        pdc.set(nsKey, PersistentDataType.INTEGER, intValue);
                    }
                    wand.setItemMeta(meta);
                }
            }
            return this;
        }

        @Override
        @NotNull
        public WandCustomizer removeProperty(@NotNull String key) {
            if (wand.hasItemMeta()) {
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    NamespacedKey nsKey = new NamespacedKey("empirewand", "custom_" + key);
                    pdc.remove(nsKey);
                    wand.setItemMeta(meta);
                }
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

    /**
     * A placeholder implementation of the {@link WandStatistics} interface.
     *
     * Optimized as a singleton to reduce object creation.
     */
    private static class WandStatisticsImpl implements WandStatistics {

        // Singleton instance to reduce object creation
        private static final WandStatisticsImpl INSTANCE = new WandStatisticsImpl();

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
