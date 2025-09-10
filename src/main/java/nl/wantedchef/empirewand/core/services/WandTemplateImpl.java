package nl.wantedchef.empirewand.core.services;

import nl.wantedchef.empirewand.api.service.WandTemplate;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The primary implementation of the {@link WandTemplate} interface.
 * This class defines a reusable template for creating wands with predefined
 * properties.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Adventure Components are immutable")
public class WandTemplateImpl implements WandTemplate {

    private final String name;
    private final Material material;
    private final Component displayName;
    private final Component[] defaultLore;
    private final Set<String> defaultSpells;
    private final Map<Enchantment, Integer> defaultEnchantments;
    private final Map<String, Object> properties;

    private WandTemplateImpl(BuilderImpl builder) {
        this.name = builder.name;
        this.material = builder.material;
        this.displayName = builder.displayName;
        this.defaultLore = builder.lore.toArray(new Component[0]);
        this.defaultSpells = new HashSet<>(builder.spells);
        this.defaultEnchantments = new HashMap<>(builder.enchantments);
        this.properties = new HashMap<>(builder.properties);
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public Material getMaterial() {
        return material;
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return displayName;
    }

    @Override
    @NotNull
    public Component[] getDefaultLore() {
        return Arrays.copyOf(defaultLore, defaultLore.length);
    }

    @Override
    @NotNull
    public Set<String> getDefaultSpells() {
        return new HashSet<>(defaultSpells);
    }

    @Override
    @NotNull
    public Map<Enchantment, Integer> getDefaultEnchantments() {
        return new HashMap<>(defaultEnchantments);
    }

    @Override
    @Nullable
    public Object getProperty(@NotNull String key) {
        return properties.get(key);
    }

    /**
     * The primary implementation of the {@link WandTemplate.Builder} interface.
     */
    public static class BuilderImpl implements Builder {
        private final String name;
        private Material material = Material.STICK;
        private Component displayName = Component.text("Wand");
        private final List<Component> lore = new ArrayList<>();
        private final List<String> spells = new ArrayList<>();
        private final Map<Enchantment, Integer> enchantments = new HashMap<>();
        private final Map<String, Object> properties = new HashMap<>();

        /**
         * Constructs a new BuilderImpl.
         *
         * @param name The name of the template.
         */
        public BuilderImpl(@NotNull String name) {
            this.name = name;
        }

        @Override
        @NotNull
        public Builder displayName(@NotNull Component displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        @NotNull
        public Builder material(@NotNull Material material) {
            this.material = material;
            return this;
        }

        @Override
        @NotNull
        public Builder defaultLore(@NotNull Component... lore) {
            this.lore.clear();
            this.lore.addAll(Arrays.asList(lore));
            return this;
        }

        @Override
        @NotNull
        public Builder defaultSpells(@NotNull String... spells) {
            this.spells.clear();
            this.spells.addAll(Arrays.asList(spells));
            return this;
        }

        @Override
        @NotNull
        public Builder defaultEnchantments(@NotNull Map<Enchantment, Integer> enchantments) {
            this.enchantments.clear();
            this.enchantments.putAll(enchantments);
            return this;
        }

        @Override
        @NotNull
        public Builder property(@NotNull String key, @NotNull Object value) {
            this.properties.put(key, value);
            return this;
        }

        @Override
        @NotNull
        public WandTemplate build() {
            return new WandTemplateImpl(this);
        }
    }
}
