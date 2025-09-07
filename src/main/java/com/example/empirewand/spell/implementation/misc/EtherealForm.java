package com.example.empirewand.spell.implementation.misc;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.empirewand.core.storage.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * Grants the caster ethereal form, making them intangible and granting slow
 * falling.
 * Features configurable duration, visual effects, and automatic cleanup.
 */
public class EtherealForm implements Spell {

    private BukkitRunnable cleanupTask;
    private SpellContext context;

    @Override
    public void execute(SpellContext context) {
        this.context = context;
        Player player = context.caster();

        // Validate player
        if (player == null || !player.isValid()) {
            return;
        }

        // Cancel any existing ethereal form
        cancelEtherealForm(player);

        // Load configuration with defaults
        var spellsConfig = context.config().getSpellsConfig();
        int duration = spellsConfig.getInt("ethereal-form.values.duration-ticks", 100); // default 5s
        double particleCount = spellsConfig.getDouble("ethereal-form.values.particle-count", 16);
        float particleSpread = (float) spellsConfig.getDouble("ethereal-form.values.particle-spread", 0.4);
        float soundPitch = (float) spellsConfig.getDouble("ethereal-form.values.sound-pitch", 1.3);

        // Validate configuration values
        duration = Math.max(20, Math.min(duration, 600)); // Clamp between 1s and 30s
        particleCount = Math.max(5, Math.min(particleCount, 50)); // Clamp particles between 5 and 50

        // Apply ethereal form effects
        player.setCollidable(false);
        player.setInvulnerable(true); // Additional protection

        // Apply slow falling effect
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                duration,
                0,
                false,
                true,
                true));

        // Apply speed boost for better mobility
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                duration,
                1,
                false,
                true,
                true));

        // Enhanced ambient ethereal FX
        Location loc = player.getLocation();
        if (loc != null) {
            // Main ethereal particles
            context.fx().spawnParticles(
                    loc.add(0, 1.0, 0),
                    Particle.END_ROD,
                    (int) particleCount,
                    particleSpread,
                    particleSpread,
                    particleSpread,
                    0.01);

            // Additional mystical particles
            context.fx().spawnParticles(
                    loc.add(0, 1.0, 0),
                    Particle.CRIT, // Replace SPELL_INSTANT
                    (int) (particleCount * 0.5),
                    particleSpread * 0.8,
                    particleSpread * 0.8,
                    particleSpread * 0.8,
                    0.02);

            // Subtle glow particles
            context.fx().spawnParticles(
                    loc.add(0, 1.0, 0),
                    Particle.GLOW,
                    (int) (particleCount * 0.3),
                    particleSpread * 0.5,
                    particleSpread * 0.5,
                    particleSpread * 0.5,
                    0.005);
        }

        // Sound effects with spatial audio
        context.fx().playSound(
                player,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                0.7f,
                soundPitch);

        // Tag player as ethereal via PDC for listeners
        player.getPersistentDataContainer().set(Keys.ETHEREAL_ACTIVE, Keys.BYTE_TYPE.getType(), (byte) 1);
        long nowTicks = player.getWorld().getFullTime();
        player.getPersistentDataContainer().set(Keys.ETHEREAL_EXPIRES_TICK, Keys.LONG_TYPE.getType(),
                nowTicks + duration);

        // Schedule cleanup task
        scheduleCleanup(player, duration);

        // Optional: Add visual indicator for other players
        if (context.config().getConfig().getBoolean("ethereal-form.show-others-effect", true)) {
            showOthersEffect(player, context);
        }
    }

    /**
     * Schedules the cleanup task for ethereal form.
     */
    private void scheduleCleanup(Player player, int duration) {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupEtherealForm(player);
            }
        };
        cleanupTask.runTaskLater(context.plugin(), duration);
    }

    /**
     * Cleans up the ethereal form effects.
     */
    private void cleanupEtherealForm(Player player) {
        if (player == null || !player.isValid()) {
            return;
        }

        player.setCollidable(true);
        player.setInvulnerable(false);
        player.getPersistentDataContainer().remove(Keys.ETHEREAL_ACTIVE);

        // Play end sound
        context.fx().playSound(
                player,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                0.5f,
                0.8f);

        // Cleanup task
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    /**
     * Cancels any existing ethereal form on the player.
     */
    private void cancelEtherealForm(Player player) {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        cleanupEtherealForm(player);
    }

    /**
     * Shows visual effects to other players.
     */
    private void showOthersEffect(Player player, SpellContext context) {
        Location loc = player.getLocation();
        if (loc == null)
            return;

        // Aura effect visible to others
        context.fx().spawnParticles(
                loc.add(0, 0.5, 0),
                Particle.END_ROD,
                20,
                1.0, 1.0, 1.0,
                0.02);

        // Subtle glow around player
        context.fx().spawnParticles(
                loc.add(0, 0.5, 0),
                Particle.GLOW,
                15,
                0.8, 0.8, 0.8,
                0.01);
    }

    @Override
    public String getName() {
        return "ethereal-form";
    }

    @Override
    public String key() {
        return "ethereal-form";
    }

    @Override
    public Component displayName() {
        return Component.text("Ethereal Form")
                .color(TextColor.color(147, 112, 219)); // MediumPurple color
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
