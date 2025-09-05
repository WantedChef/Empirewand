package com.example.empirewand.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Skeleton cooldown tracker per player + spell key.
 */
public class CooldownService {
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID playerId, String key, long nowTicks) {
        var map = cooldowns.get(playerId);
        if (map == null) return false;
        var until = map.getOrDefault(key, 0L);
        return nowTicks < until;
    }

    public long remaining(UUID playerId, String key, long nowTicks) {
        var map = cooldowns.get(playerId);
        if (map == null) return 0L;
        var until = map.getOrDefault(key, 0L);
        return Math.max(0L, until - nowTicks);
    }

    public void set(UUID playerId, String key, long untilTicks) {
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(key, untilTicks);
    }

    public void clearAll(UUID playerId) {
        cooldowns.remove(playerId);
    }
}

