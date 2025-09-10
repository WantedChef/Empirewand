package nl.wantedchef.empirewand.api.service;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public interface CooldownService {
    boolean isOnCooldown(@NotNull UUID playerId, @NotNull String spellKey, long currentTicks);
    void set(@NotNull UUID playerId, @NotNull String spellKey, long cooldownEndTicks);
    void clear(@NotNull UUID playerId, @NotNull String spellKey);
    void clearAll(@NotNull UUID playerId);
    long getRemainingTicks(@NotNull UUID playerId, @NotNull String spellKey, long currentTicks);
    void setCooldownDisabled(@NotNull UUID playerId, boolean disabled);
    boolean isCooldownDisabled(@NotNull UUID playerId);
}
