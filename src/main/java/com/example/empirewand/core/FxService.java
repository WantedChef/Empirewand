package com.example.empirewand.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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

    public void actionBarSound(Player player, Component message, Sound sound, float volume, float pitch) {
        actionBar(player, message);
        playSound(player, sound, volume, pitch);
    }

    public void actionBarSound(Player player, String messageKey, Sound sound, float volume, float pitch) {
        String raw = config.getMessage(messageKey);
        actionBarSound(player, Component.text(stripMiniTags(raw)), sound, volume, pitch);
    }

    public void actionBarSound(Player player, String messageKey, Map<String, String> placeholders,
                               Sound sound, float volume, float pitch) {
        String raw = config.getMessage(messageKey);
        String formatted = applyPlaceholders(raw, placeholders);
        actionBarSound(player, Component.text(stripMiniTags(formatted)), sound, volume, pitch);
    }

    public void selectedSpell(Player player, String displayName) {
        actionBarSound(player, "spell-selected", Map.of("spell", stripMiniTags(displayName)),
                Sound.UI_BUTTON_CLICK, 0.6f, 1.8f);
    }

    public void onCooldown(Player player, String displayName, long msRemaining) {
        String raw = config.getMessage("on-cooldown");
        String formatted = applyPlaceholders(raw, Map.of(
                "spell", stripMiniTags(displayName),
                "time", Long.toString(Math.max(0, msRemaining))
        ));
        actionBarSound(player, Component.text(stripMiniTags(formatted)), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
    }

    public void noSpells(Player player) {
        actionBarSound(player, "no-spells-bound", Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
    }

    public void noPermission(Player player) {
        actionBarSound(player, "no-permission", Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
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

    public void trail(Location start, Location end, Particle particle, int perStep) {
        if (start == null || end == null || particle == null || perStep <= 0) return;
        Vector dir = end.toVector().subtract(start.toVector());
        double length = dir.length();
        if (length <= 0.001) return;
        int steps = Math.max(1, (int) (length * 4));
        Vector step = dir.normalize().multiply(length / steps);
        Location point = start.clone();
        for (int i = 0; i < steps; i++) {
            spawnParticles(point, particle, perStep, 0, 0, 0, 0);
            point.add(step);
        }
    }

    public void impact(Location location, Particle particle, int count, Sound sound, float volume, float pitch) {
        impact(location, particle, count, 0.2, sound, volume, pitch);
    }

    public void impact(Location location, Particle particle, int count, double spread, Sound sound, float volume, float pitch) {
        if (location == null) return;
        spawnParticles(location, particle, count, spread, spread, spread, 0);
        playSound(location, sound, volume, pitch);
    }

    /**
     * Subtle failure effect: short extinguish + smoke.
     */
    public void fizzle(Location location) {
        playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
        spawnParticles(location, Particle.SMOKE, 10, 0.1, 0.1, 0.1, 0.05);
    }

    /**
     * Generic projectile trail effect.
     */
    public void trail(Location location) {
        spawnParticles(location, Particle.SOUL_FIRE_FLAME, 10, 0.1, 0.1, 0.1, 0.05);
    }

    /**
     * Generic projectile impact effect.
     */
    public void impact(Location location) {
        spawnParticles(location, Particle.EXPLOSION, 30, 0.5, 0.5, 0.5, 0.1);
        playSound(location, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
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
