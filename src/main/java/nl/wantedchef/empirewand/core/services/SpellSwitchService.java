package nl.wantedchef.empirewand.core.services;

import nl.wantedchef.empirewand.core.EmpireWandPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that manages spell switching effects and related functionality.
 */
public class SpellSwitchService {
    private final EmpireWandPlugin plugin;
    private final Map<UUID, Long> lastSwitchTime = new ConcurrentHashMap<>();

    // Available switch effects
    private static final Set<String> AVAILABLE_EFFECTS = Set.of(
            "default", "spiral", "explosion", "portal", "fire", "ice", "lightning",
            "nether", "enchant", "hearts", "music", "ender", "dragon", "void",
            "cosmic", "rainbow", "sparkle", "vortex", "galaxy", "matrix");

    public SpellSwitchService(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the list of available switch effects.
     * 
     * @return Set of available effect names
     */
    public Set<String> getAvailableEffects() {
        return new HashSet<>(AVAILABLE_EFFECTS);
    }

    /**
     * Plays the spell switch effect for a player's wand.
     * 
     * @param player The player
     * @param wand   The wand item
     */
    public void playSpellSwitchEffect(Player player, ItemStack wand) {
        // Rate limit to prevent spam
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastSwitch = lastSwitchTime.get(playerId);
        if (lastSwitch != null && (now - lastSwitch) < 100) { // 100ms cooldown
            return;
        }
        lastSwitchTime.put(playerId, now);

        // TODO: Implement WandSettings class
        String effect = "spiral"; // Default effect

        switch (effect.toLowerCase()) {
            case "spiral":
                playSpiralEffect(player);
                break;
            case "explosion":
                playExplosionEffect(player);
                break;
            case "portal":
                playPortalEffect(player);
                break;
            case "fire":
                playFireEffect(player);
                break;
            case "ice":
                playIceEffect(player);
                break;
            case "lightning":
                playLightningEffect(player);
                break;
            case "nether":
                playNetherEffect(player);
                break;
            case "enchant":
                playEnchantEffect(player);
                break;
            case "hearts":
                playHeartsEffect(player);
                break;
            case "music":
                playMusicEffect(player);
                break;
            case "ender":
                playEnderEffect(player);
                break;
            case "dragon":
                playDragonEffect(player);
                break;
            case "void":
                playVoidEffect(player);
                break;
            case "cosmic":
                playCosmicEffect(player);
                break;
            case "rainbow":
                playRainbowEffect(player);
                break;
            case "sparkle":
                playSparkleEffect(player);
                break;
            case "vortex":
                playVortexEffect(player);
                break;
            case "galaxy":
                playGalaxyEffect(player);
                break;
            case "matrix":
                playMatrixEffect(player);
                break;
            case "default":
            default:
                playDefaultEffect(player);
                break;
        }
    }

    /**
     * Plays the default spell switch effect.
     * 
     * @param player The player
     */
    private void playDefaultEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Simple particle ring
        for (int i = 0; i < 16; i++) {
            double angle = 2 * Math.PI * i / 16;
            double x = Math.cos(angle) * 0.8;
            double z = Math.sin(angle) * 0.8;
            Location particleLoc = loc.clone().add(x, 0, z);
            world.spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0);
        }

        // Sound effect
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    /**
     * Plays a spiral spell switch effect.
     * 
     * @param player The player
     */
    private void playSpiralEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        new BukkitRunnable() {
            int step = 0;
            final int maxSteps = 20;

            @Override
            public void run() {
                if (step >= maxSteps) {
                    cancel();
                    return;
                }

                double height = step * 0.1;
                double radius = 0.5 + (step * 0.05);
                double angle = step * 0.5;

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location particleLoc = loc.clone().add(x, height, z);
                world.spawnParticle(Particle.ENCHANT, particleLoc, 2, 0, 0, 0, 0);

                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.2f);
    }

    /**
     * Plays an explosion spell switch effect.
     * 
     * @param player The player
     */
    private void playExplosionEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Initial explosion
        world.spawnParticle(Particle.EXPLOSION, loc, 3, 0, 0, 0, 0);

        // Expanding ring
        new BukkitRunnable() {
            int radius = 1;
            final int maxRadius = 3;

            @Override
            public void run() {
                if (radius > maxRadius) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 12; i++) {
                    double angle = 2 * Math.PI * i / 12;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = loc.clone().add(x, 0, z);
                    world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                }

                radius++;
            }
        }.runTaskTimer(plugin, 2L, 2L);

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);
    }

    /**
     * Plays a portal spell switch effect.
     * 
     * @param player The player
     */
    private void playPortalEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Portal ring
        for (int i = 0; i < 20; i++) {
            double angle = 2 * Math.PI * i / 20;
            double x = Math.cos(angle) * 1.0;
            double z = Math.sin(angle) * 1.0;
            Location particleLoc = loc.clone().add(x, Math.sin(i * 0.3) * 0.3, z);
            world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.0f);
    }

    /**
     * Plays a fire spell switch effect.
     * 
     * @param player The player
     */
    private void playFireEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Fire particles
        for (int i = 0; i < 30; i++) {
            double angle = 2 * Math.PI * i / 30;
            double x = Math.cos(angle) * 0.7;
            double z = Math.sin(angle) * 0.7;
            Location particleLoc = loc.clone().add(x, (Math.random() - 0.5) * 0.5, z);
            world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.2f);
    }

    /**
     * Plays an ice spell switch effect.
     * 
     * @param player The player
     */
    private void playIceEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Ice particles
        for (int i = 0; i < 25; i++) {
            double angle = 2 * Math.PI * i / 25;
            double x = Math.cos(angle) * 0.8;
            double z = Math.sin(angle) * 0.8;
            Location particleLoc = loc.clone().add(x, (Math.random() - 0.5) * 0.3, z);
            world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.5f);
    }

    /**
     * Plays a lightning spell switch effect.
     * 
     * @param player The player
     */
    private void playLightningEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Lightning particles
        for (int i = 0; i < 15; i++) {
            Location particleLoc = loc.clone().add(
                    (Math.random() - 0.5) * 1.5,
                    (Math.random() - 0.5) * 1.0,
                    (Math.random() - 0.5) * 1.5);
            world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 2, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.8f);
    }

    /**
     * Plays a nether spell switch effect.
     * 
     * @param player The player
     */
    private void playNetherEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Nether particles
        for (int i = 0; i < 20; i++) {
            Location particleLoc = loc.clone().add(
                    (Math.random() - 0.5) * 1.2,
                    (Math.random() - 0.5) * 0.8,
                    (Math.random() - 0.5) * 1.2);
            if (Math.random() < 0.5) {
                world.spawnParticle(Particle.ASH, particleLoc, 1, 0, 0, 0, 0);
            } else {
                world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
            }
        }

        world.playSound(loc, Sound.AMBIENT_NETHER_WASTES_LOOP, 0.6f, 0.9f);
    }

    /**
     * Plays an enchant spell switch effect.
     * 
     * @param player The player
     */
    private void playEnchantEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Enchant particles
        for (int i = 0; i < 35; i++) {
            double angle = 2 * Math.PI * i / 35;
            double radius = 0.5 + (Math.random() * 0.8);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = loc.clone().add(x, (Math.random() - 0.5) * 0.5, z);
            world.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 1.3f);
    }

    /**
     * Plays a hearts spell switch effect.
     * 
     * @param player The player
     */
    private void playHeartsEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Hearts particles
        for (int i = 0; i < 12; i++) {
            Location particleLoc = loc.clone().add(
                    (Math.random() - 0.5) * 1.0,
                    (Math.random() - 0.5) * 0.5,
                    (Math.random() - 0.5) * 1.0);
            world.spawnParticle(Particle.HEART, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
    }

    /**
     * Plays a music spell switch effect.
     * 
     * @param player The player
     */
    private void playMusicEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Music particles
        for (int i = 0; i < 18; i++) {
            double angle = 2 * Math.PI * i / 18;
            double x = Math.cos(angle) * 0.9;
            double z = Math.sin(angle) * 0.9;
            Location particleLoc = loc.clone().add(x, Math.sin(i * 0.4) * 0.2, z);
            world.spawnParticle(Particle.NOTE, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
    }

    /**
     * Plays an ender spell switch effect.
     * 
     * @param player The player
     */
    private void playEnderEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Ender particles
        for (int i = 0; i < 22; i++) {
            double angle = 2 * Math.PI * i / 22;
            double radius = 0.6 + (Math.random() * 0.5);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = loc.clone().add(x, (Math.random() - 0.5) * 0.4, z);
            world.spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.0f);
    }

    /**
     * Plays a dragon spell switch effect.
     * 
     * @param player The player
     */
    private void playDragonEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Dragon particles
        for (int i = 0; i < 16; i++) {
            Location particleLoc = loc.clone().add(
                    (Math.random() - 0.5) * 1.3,
                    (Math.random() - 0.5) * 0.7,
                    (Math.random() - 0.5) * 1.3);
            world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.9f);
    }

    /**
     * Plays a void spell switch effect.
     * 
     * @param player The player
     */
    private void playVoidEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Void particles
        for (int i = 0; i < 25; i++) {
            double angle = 2 * Math.PI * i / 25;
            double radius = 0.4 + (Math.random() * 0.7);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = loc.clone().add(x, (Math.random() - 0.5) * 0.6, z);
            world.spawnParticle(Particle.SQUID_INK, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, 0.6f, 0.7f);
    }

    /**
     * Plays a cosmic spell switch effect.
     * 
     * @param player The player
     */
    private void playCosmicEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Cosmic particles
        for (int i = 0; i < 20; i++) {
            double angle = 2 * Math.PI * i / 20;
            double radius = 0.6 + (Math.random() * 0.5);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = loc.clone().add(x, (Math.random() - 0.5) * 0.4, z);
            world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
        }

        // Add some sparkle particles
        for (int i = 0; i < 10; i++) {
            Location particleLoc = loc.clone().add(
                    (Math.random() - 0.5) * 1.2,
                    (Math.random() - 0.5) * 0.8,
                    (Math.random() - 0.5) * 1.2);
            world.spawnParticle(Particle.FIREWORK, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.AMBIENT_BASALT_DELTAS_LOOP, 0.6f, 1.2f);
    }

    /**
     * Plays a rainbow spell switch effect.
     * 
     * @param player The player
     */
    private void playRainbowEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Rainbow particles in a spiral
        for (int i = 0; i < 30; i++) {
            double height = i * 0.05;
            double radius = 0.3 + (i * 0.02);
            double angle = 2 * Math.PI * i / 8;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = loc.clone().add(x, height, z);

            // Cycle through different particle colors
            Particle[] rainbowParticles = { Particle.DUST, Particle.CRIT, Particle.ENCHANT, Particle.HAPPY_VILLAGER };
            Particle particleType = rainbowParticles[i % rainbowParticles.length];
            world.spawnParticle(particleType, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.5f);
    }

    /**
     * Plays a sparkle spell switch effect.
     * 
     * @param player The player
     */
    private void playSparkleEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Sparkle particles
        for (int i = 0; i < 25; i++) {
            Location particleLoc = loc.clone().add(
                    (Math.random() - 0.5) * 1.0,
                    (Math.random() - 0.5) * 0.5,
                    (Math.random() - 0.5) * 1.0);
            world.spawnParticle(Particle.FIREWORK, particleLoc, 1, 0, 0, 0, 0);
        }

        // Add some crit particles for extra sparkle
        for (int i = 0; i < 15; i++) {
            Location particleLoc = loc.clone().add(
                    (Math.random() - 0.5) * 0.8,
                    (Math.random() - 0.5) * 0.3,
                    (Math.random() - 0.5) * 0.8);
            world.spawnParticle(Particle.CRIT, particleLoc, 2, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.7f, 1.8f);
    }

    /**
     * Plays a vortex spell switch effect.
     * 
     * @param player The player
     */
    private void playVortexEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Vortex particles spiraling upward
        new BukkitRunnable() {
            int step = 0;
            final int maxSteps = 25;

            @Override
            public void run() {
                if (step >= maxSteps) {
                    cancel();
                    return;
                }

                double height = step * 0.15;
                double radius = 0.4 + (step * 0.03);
                double angle = step * 0.6;

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location particleLoc = loc.clone().add(x, height, z);
                world.spawnParticle(Particle.PORTAL, particleLoc, 2, 0, 0, 0, 0);

                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        world.playSound(loc, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.6f, 1.0f);
    }

    /**
     * Plays a galaxy spell switch effect.
     * 
     * @param player The player
     */
    private void playGalaxyEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Create a galaxy of particles
        for (int i = 0; i < 40; i++) {
            double angle = 2 * Math.PI * i / 40;
            double radius = 0.5 + (Math.random() * 0.7);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = loc.clone().add(x, (Math.random() - 0.5) * 0.3, z);

            // Use different particle types for a galaxy effect
            Particle[] galaxyParticles = { Particle.END_ROD, Particle.REVERSE_PORTAL, Particle.DRAGON_BREATH };
            Particle particleType = galaxyParticles[i % galaxyParticles.length];
            world.spawnParticle(particleType, particleLoc, 1, 0, 0, 0, 0);
        }

        // Add some central bright particles
        for (int i = 0; i < 10; i++) {
            Location particleLoc = loc.clone().add(
                    (Math.random() - 0.5) * 0.3,
                    (Math.random() - 0.5) * 0.2,
                    (Math.random() - 0.5) * 0.3);
            world.spawnParticle(Particle.FLASH, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.AMBIENT_CRIMSON_FOREST_LOOP, 0.7f, 0.9f);
    }

    /**
     * Plays a matrix spell switch effect.
     * 
     * @param player The player
     */
    private void playMatrixEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null)
            return;

        // Matrix-style falling particles
        new BukkitRunnable() {
            int step = 0;
            final int maxSteps = 30;

            @Override
            public void run() {
                if (step >= maxSteps) {
                    cancel();
                    return;
                }

                // Create vertical lines of particles
                for (int j = 0; j < 5; j++) {
                    double x = (Math.random() - 0.5) * 1.5;
                    double z = (Math.random() - 0.5) * 1.5;
                    double y = step * 0.1 + j * 0.2;
                    Location particleLoc = loc.clone().add(x, y, z);
                    world.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
                }

                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 1.3f);
    }
}