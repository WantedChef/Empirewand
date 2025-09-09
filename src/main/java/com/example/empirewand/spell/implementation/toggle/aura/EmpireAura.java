package com.example.empirewand.spell.implementation.toggle.aura;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.spell.toggle.ToggleableSpell;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;

/**
 * Enhanced EmpireAura - A powerful toggleable aura spell that provides
 * continuous buffs to the caster and applies debuffs to nearby enemies.
 * Features improved particle effects, dynamic radius scaling, and
 * sophisticated targeting mechanics.
 */
public class EmpireAura extends Spell<Void> implements ToggleableSpell {

    // Configuration constants
    private static final double BASE_RADIUS = 8.0;
    private static final double MAX_RADIUS = 12.0;
    private static final int EFFECT_DURATION = 80; // 4 seconds in ticks
    private static final int TICK_INTERVAL = 20; // 1 second intervals
    // Particle density driven by currentRadius; removed unused constants

    // Particle effect configurations
    private static final Particle.DustOptions ALLY_PARTICLE = new Particle.DustOptions(Color.LIME, 1.5f);
    private static final Particle.DustOptions ENEMY_PARTICLE = new Particle.DustOptions(Color.RED, 1.2f);
    private static final Particle.DustOptions AURA_CORE = new Particle.DustOptions(Color.PURPLE, 2.0f);

    // Active spell tracking
    private final Map<UUID, AuraInstance> activeAuras = new ConcurrentHashMap<>();
    private EmpireWandPlugin plugin;

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empire Aura";
            this.description = "An enhanced toggleable aura that provides continuous effects.";
            this.cooldown = java.time.Duration.ofSeconds(3);
            this.spellType = SpellType.AURA;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EmpireAura(this);
        }
    }

    private EmpireAura(Builder builder) {
        super(builder);
        // Plugin will be set when spell is first executed
    }

    @Override
    public String key() {
        return "empire-aura";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Initialize plugin if not set
        if (this.plugin == null) {
            this.plugin = context.plugin();
        }

        // Toggle the aura
        toggle(player, context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effect handling is done in toggle methods
    }

    @Override
    public void activate(Player player, SpellContext context) {
        UUID playerId = player.getUniqueId();

        // Initialize plugin if not set
        if (this.plugin == null) {
            this.plugin = context.plugin();
        }

        // Check if already active
        if (isActive(player)) {
            player.sendMessage("§6EmpireAura is already active!");
            return;
        }

        // Validate location
        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) {
            player.sendMessage("§cCannot activate EmpireAura in invalid world!");
            return;
        }

        try {
            // Create aura instance with validation
            AuraInstance aura = new AuraInstance(player, location.clone());
            activeAuras.put(playerId, aura);

            // Start aura effects
            aura.start();

            // Initial effects
            player.sendMessage("§6⚡ EmpireAura activated! You radiate imperial power!");
            if (location.getWorld() != null) {
                player.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
                spawnActivationParticles(location);
            }

        } catch (Exception e) {
            activeAuras.remove(playerId);
            player.sendMessage(
                    "§cFailed to activate EmpireAura: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
            if (plugin != null) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                plugin.getLogger().warning(
                        String.format("Error activating EmpireAura for player %s: %s", player.getName(), errorMsg));
            }
        }
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        UUID playerId = player.getUniqueId();

        AuraInstance aura = activeAuras.remove(playerId);
        if (aura == null) {
            player.sendMessage("EmpireAura is not active!");
            return;
        }

        try {
            // Stop aura effects
            aura.stop();

            // Deactivation effects
            player.sendMessage("§6⚡ EmpireAura deactivated.");
            Location playerLoc = player.getLocation();
            if (playerLoc != null) {
                player.playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 0.8f);
                spawnDeactivationParticles(playerLoc);
            }

        } catch (Exception e) {
            player.sendMessage(
                    "Error deactivating EmpireAura: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @Override
    public boolean isActive(Player player) {
        return activeAuras.containsKey(player.getUniqueId());
    }

    @Override
    public void forceDeactivate(Player player) {
        UUID playerId = player.getUniqueId();
        AuraInstance aura = activeAuras.remove(playerId);
        if (aura != null) {
            aura.stop();

            // Notify player if online
            if (player.isOnline()) {
                player.sendMessage("§6⚡ EmpireAura was forcefully deactivated!");
            }
        }
    }

    @Override
    public int getMaxDuration() {
        return 1200; // 60 seconds in ticks (20 ticks per second)
    }

    // Particle effect methods
    private void spawnActivationParticles(Location center) {
        World world = center.getWorld();
        if (world == null)
            return;

        // Central explosion
        world.spawnParticle(Particle.EXPLOSION, center, 3, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.DUST, center, 50, 2.0, 1.0, 2.0, 0.0, AURA_CORE);
        world.spawnParticle(Particle.ENCHANTED_HIT, center, 30, 1.5, 1.0, 1.5, 0.2);
    }

    private void spawnDeactivationParticles(Location center) {
        World world = center.getWorld();
        if (world == null)
            return;

        // Dissipation effect
        world.spawnParticle(Particle.SMOKE, center, 20, 1.0, 1.0, 1.0, 0.05);
        world.spawnParticle(Particle.DUST, center, 30, 1.5, 1.0, 1.5, 0.0, AURA_CORE);
    }

    /**
     * Individual aura instance for each player
     */
    private class AuraInstance {
        private final Player player;
        private final Location centerLocation;
        private BukkitTask effectTask;
        private BukkitTask particleTask;
        private final long startTime;
        private double currentRadius;
        private int tickCount = 0;

        public AuraInstance(Player player, Location center) {
            this.player = player;
            this.centerLocation = center;
            this.startTime = System.currentTimeMillis();
            this.currentRadius = BASE_RADIUS;
        }

        public void start() {
            // Main effect task - applies buffs/debuffs every second
            effectTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !isActive(player)) {
                        cancel();
                        return;
                    }

                    // Check max duration
                    if (System.currentTimeMillis() - startTime > getMaxDuration() * 50) { // Convert ticks to ms
                        forceDeactivate(player);
                        cancel();
                        return;
                    }

                    updateAuraEffects();
                }
            }.runTaskTimer(plugin, 0L, TICK_INTERVAL);

            // Particle effect task - runs more frequently for smooth visuals
            particleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !isActive(player)) {
                        cancel();
                        return;
                    }

                    updateParticleEffects();
                }
            }.runTaskTimer(plugin, 0L, 4L); // Every 4 ticks for smooth particles
        }

        public void stop() {
            if (effectTask != null) {
                effectTask.cancel();
                effectTask = null;
            }
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }

            // Remove all applied effects
            cleanupEffects();
        }

        private void updateAuraEffects() {
            if (player.getWorld() != centerLocation.getWorld()) {
                forceDeactivate(player);
                return;
            }

            // Update radius dynamically (grows over time)
            tickCount++;
            double growth = Math.min(tickCount * 0.1, MAX_RADIUS - BASE_RADIUS);
            currentRadius = BASE_RADIUS + growth;

            // Update center location to follow player
            Location playerLoc = player.getLocation();
            if (playerLoc != null) {
                centerLocation.setX(playerLoc.getX());
                centerLocation.setY(playerLoc.getY());
                centerLocation.setZ(playerLoc.getZ());
            }

            // Apply self buffs to caster
            applyCasterBuffs();

            // Find and affect nearby entities
            World currentWorld = centerLocation.getWorld();
            if (currentWorld == null) {
                forceDeactivate(player);
                return;
            }
            Collection<Entity> nearbyEntities = currentWorld
                    .getNearbyEntities(centerLocation, currentRadius, currentRadius, currentRadius);

            for (Entity entity : nearbyEntities) {
                if (!(entity instanceof LivingEntity))
                    continue;
                if (entity.equals(player))
                    continue; // Skip self (already buffed)

                LivingEntity living = (LivingEntity) entity;
                double distance = living.getLocation().distance(centerLocation);

                if (distance <= currentRadius) {
                    if (isAlly(living)) {
                        applyAllyEffects(living, distance);
                    } else if (isEnemy(living)) {
                        applyEnemyEffects(living, distance);
                    }
                }
            }
        }

        private void updateParticleEffects() {
            World world = centerLocation.getWorld();
            if (world == null)
                return;

            // Core aura particles at center
            world.spawnParticle(Particle.DUST, centerLocation, 2, 0.3, 0.3, 0.3, 0.0, AURA_CORE);

            // Rotating particle rings at different heights
            double angle1 = (System.currentTimeMillis() / 50.0) % (2 * Math.PI);
            double angle2 = (System.currentTimeMillis() / 70.0) % (2 * Math.PI);

            // Inner ring
            spawnParticleRing(world, centerLocation.clone().add(0, 0.5, 0),
                    currentRadius * 0.6, angle1, ALLY_PARTICLE, 16);

            // Outer ring
            spawnParticleRing(world, centerLocation.clone().add(0, 1.5, 0),
                    currentRadius * 0.9, angle2, ENEMY_PARTICLE, 20);

            // Vertical energy streams
            if (tickCount % 10 == 0) { // Less frequent for performance
                for (int i = 0; i < 3; i++) {
                    Location streamLoc = centerLocation.clone().add(0, i * 0.8, 0);
                    world.spawnParticle(Particle.ENCHANTED_HIT, streamLoc, 3, 0.2, 0.1, 0.2, 0.05);
                }
            }
        }

        private void spawnParticleRing(World world, Location center, double radius,
                double rotationOffset, Particle.DustOptions color, int density) {
            if (world == null || center == null)
                return;

            for (int i = 0; i < density; i++) {
                double angle = (2 * Math.PI * i / density) + rotationOffset;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);

                Location particleLoc = new Location(world, x, center.getY(), z);
                world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, color);
            }
        }

        private void applyCasterBuffs() {
            // Enhanced buffs for the caster
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, EFFECT_DURATION, 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, EFFECT_DURATION, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, EFFECT_DURATION, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION, 0, false, false));
        }

        private void applyAllyEffects(LivingEntity ally, double distance) {
            // Stronger effects closer to center
            double effectiveness = 1.0 - (distance / currentRadius);
            int amplifier = effectiveness > 0.7 ? 1 : 0;

            // Beneficial effects for allies
            ally.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, EFFECT_DURATION, amplifier, false, false));
            ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, EFFECT_DURATION, 0, false, false));

            if (effectiveness > 0.5) {
                ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, EFFECT_DURATION, 0, false, false));
            }
        }

        private void applyEnemyEffects(LivingEntity enemy, double distance) {
            // Stronger debuffs closer to center
            double effectiveness = 1.0 - (distance / currentRadius);
            int amplifier = effectiveness > 0.7 ? 1 : 0;

            // Debuffs for enemies
            enemy.addPotionEffect(
                    new PotionEffect(PotionEffectType.WEAKNESS, EFFECT_DURATION, amplifier, false, false));
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, EFFECT_DURATION, 0, false, false));

            if (effectiveness > 0.6) {
                enemy.addPotionEffect(
                        new PotionEffect(PotionEffectType.MINING_FATIGUE, EFFECT_DURATION, 0, false, false));
            }

            // Chance for additional effects on close enemies
            if (effectiveness > 0.8 && Math.random() < 0.3) {
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, EFFECT_DURATION / 2, 0, false, false));
            }
        }

        private boolean isAlly(LivingEntity entity) {
            if (entity instanceof Player otherPlayer) {
                // Check if players are in the same scoreboard team
                org.bukkit.scoreboard.Scoreboard playerSb = player.getScoreboard();
                org.bukkit.scoreboard.Scoreboard otherSb = otherPlayer.getScoreboard();
                org.bukkit.scoreboard.Team playerTeam = playerSb.getEntryTeam(player.getName());
                org.bukkit.scoreboard.Team otherTeam = otherSb.getEntryTeam(otherPlayer.getName());

                // If both players are in teams and they're the same team, they're allies
                if (playerTeam != null && otherTeam != null && playerTeam.equals(otherTeam)) {
                    return true;
                }

                // For now, consider all other players as allies (non-hostile)
                // This can be extended with a proper party/guild system later
                return true;
            }

            // Friendly mobs
            var entityType = entity.getType();
            if (entityType == null)
                return false;
            return entityType.toString().contains("VILLAGER") ||
                    entityType.toString().contains("IRON_GOLEM") ||
                    entityType.toString().contains("WOLF") ||
                    entityType.toString().contains("CAT") ||
                    entityType.toString().contains("HORSE");
        }

        private boolean isEnemy(LivingEntity entity) {
            if (entity instanceof Player otherPlayer) {
                // Check if players are in the same team - if so, they're not enemies
                org.bukkit.scoreboard.Scoreboard playerSb = player.getScoreboard();
                org.bukkit.scoreboard.Scoreboard otherSb = otherPlayer.getScoreboard();
                org.bukkit.scoreboard.Team playerTeam = playerSb.getEntryTeam(player.getName());
                org.bukkit.scoreboard.Team otherTeam = otherSb.getEntryTeam(otherPlayer.getName());

                // If both players are in the same team, they're allies, not enemies
                if (playerTeam != null && otherTeam != null && playerTeam.equals(otherTeam)) {
                    return false;
                }

                // Check if PvP is enabled in the world
                org.bukkit.World world = player.getWorld();
                if (world != null && !world.getPVP()) {
                    return false; // PvP is disabled, don't treat as enemy
                }

                // For now, treat other players as potential enemies if PvP is enabled
                // This can be extended with more sophisticated PvP rules later
                return true;
            }

            // Hostile mobs
            var entityType = entity.getType();
            if (entityType == null)
                return false;
            String type = entityType.toString();
            return type.contains("ZOMBIE") || type.contains("SKELETON") ||
                    type.contains("CREEPER") || type.contains("SPIDER") ||
                    type.contains("WITCH") || type.contains("PILLAGER") ||
                    type.contains("VINDICATOR") || type.contains("EVOKER") ||
                    type.contains("WITHER") || type.contains("BLAZE") ||
                    type.contains("GHAST") || type.contains("ENDERMAN") ||
                    type.contains("GUARDIAN") || type.contains("PHANTOM");
        }

        private void cleanupEffects() {
            // Remove any persistent effects if needed
            // Current implementation uses timed effects that expire naturally
        }
    }
}