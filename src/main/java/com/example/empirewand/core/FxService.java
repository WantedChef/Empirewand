package com.example.empirewand.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * Centralized FX helper for sounds, particles, and action bar messages.
 *
 * Keep this lightweight and safe to call from event handlers.
 */
public class FxService {
    private final ConfigService config;

    public FxService(ConfigService config) {
        this.config = config;
    }

    // ---- Action bar helpers ----

    public void actionBar(Player player, Component message) {
        if (player != null && message != null) {
            player.sendActionBar(message);
        }
    }

    public void actionBar(Player player, String plainText) {
        if (player != null && plainText != null && !plainText.isEmpty()) {
            player.sendActionBar(Component.text(stripMiniTags(plainText)));
        }
    }

    public void actionBarKey(Player player, String messageKey) {
        String raw = config.getMessage(messageKey);
        actionBar(player, raw);
    }

    public void actionBarKey(Player player, String messageKey, Map<String, String> placeholders) {
        String raw = config.getMessage(messageKey);
        actionBar(player, applyPlaceholders(raw, placeholders));
    }

    public void selectedSpell(Player player, String displayName) {
        // Uses messages.spell-selected with {spell}
        actionBarKey(player, "spell-selected", Map.of("spell", stripMiniTags(displayName)));
    }

    public void onCooldown(Player player, String displayName, long msRemaining) {
        // Fallback to generic message if the template doesn't contain {time}
        String raw = config.getMessage("on-cooldown");
        String formatted = applyPlaceholders(raw, Map.of(
                "spell", stripMiniTags(displayName),
                "time", Long.toString(Math.max(0, msRemaining))
        ));
        actionBar(player, formatted);
    }

    public void noSpells(Player player) {
        actionBarKey(player, "no-spells-bound");
    }

    public void noPermission(Player player) {
        actionBarKey(player, "no-permission");
    }

    public void fizzle(Player player) {
        actionBarKey(player, "fizzle");
        if (player != null) {
            fizzle(player.getLocation());
        }
    }

    // ---- Sound helpers ----

    public void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player != null && sound != null && player.getLocation() != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public void playSound(Location location, Sound sound, float volume, float pitch) {
        if (location == null || sound == null) return;
        World world = location.getWorld();
        if (world != null) {
            world.playSound(location, sound, volume, pitch);
        }
    }

    // ---- Particle helpers ----

    public void spawnParticles(Location location, Particle particle, int count,
                               double offsetX, double offsetY, double offsetZ, double speed) {
        if (location == null || particle == null || count <= 0) return;
        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    public void spawnParticles(Location location, Particle particle, int count,
                               double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        if (location == null || particle == null || count <= 0) return;
        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed, data);
        }
    }

    /**
     * Subtle failure effect: short extinguish + smoke.
     */
    public void fizzle(Location location) {
        playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
        spawnParticles(location, Particle.SMOKE, 10, 0.1, 0.1, 0.1, 0.05);
    }

    // ---- Small utilities ----

    private String applyPlaceholders(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty() || placeholders == null || placeholders.isEmpty()) {
            return template;
        }
        String out = template;
        for (var e : placeholders.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    private String stripMiniTags(String s) {
        if (s == null) return "";
        // Very simple strip for <#RRGGBB> and named tags like <red>
        return s.replaceAll("<[^>]+>", "");
    }
}
