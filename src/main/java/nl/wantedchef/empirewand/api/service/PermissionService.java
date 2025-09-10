package nl.wantedchef.empirewand.api.service;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PermissionService {
    boolean hasSpellUsePermission(@NotNull Player player, @NotNull String spellKey);

    boolean hasSpellBindPermission(@NotNull Player player, @NotNull String spellKey);

    boolean hasWandUsePermission(@NotNull Player player);

    boolean hasAdminPermission(@NotNull Player player);

    boolean hasCommandPermission(@NotNull Player player, @NotNull String command);

    @NotNull
    String getSpellUsePermission(@NotNull String spellKey);

    @NotNull
    String getSpellBindPermission(@NotNull String spellKey);
}
