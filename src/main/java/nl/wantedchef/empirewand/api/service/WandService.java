package nl.wantedchef.empirewand.api.service;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.UUID;

public interface WandService {
    boolean isWand(@Nullable ItemStack item);
    @NotNull ItemStack createWand();
    @NotNull List<String> getBoundSpells(@NotNull ItemStack wand);
    boolean bindSpell(@NotNull ItemStack wand, @NotNull String spellKey);
    boolean unbindSpell(@NotNull ItemStack wand, @NotNull String spellKey);
    @Nullable String getSelectedSpell(@NotNull ItemStack wand);
    boolean setSelectedSpell(@NotNull ItemStack wand, @Nullable String spellKey);
    @Nullable String cycleSpell(@NotNull ItemStack wand);
    void updateWandDisplay(@NotNull ItemStack wand);
    @Nullable UUID getWandOwner(@NotNull ItemStack wand);
    void setWandOwner(@NotNull ItemStack wand, @Nullable UUID owner);
}
