package nl.wantedchef.empirewand.spell.movement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * StellarDash - Innovative teleportation spell with stellar trail effects.
 * 
 * Features:
 * - Instant teleportation to target location with line-of-sight requirement
 * - Spectacular star trail connecting origin and destination
 * - Constellation burst effects at both ends
 * - Cosmic particle shower during teleportation
 * - Multiple dash charges with cooldown system
 * - Configurable range and visual intensity
 */
public final class StellarDash extends Spell<StellarDash.DashEffect> {

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<DashEffect> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Stellar Dash";
            description = "Dash through space leaving a trail of stars in your wake.";
            cooldown = Duration.ofSeconds(4);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<DashEffect> build() {
            return new StellarDash(this);
        }
    }

    private StellarDash(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public @NotNull String key() {
        return "stellar-dash";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable DashEffect executeSpell(@NotNull SpellContext context) {
        Player player = (Player) context.caster();
        
        // Calculate target location
        double range = cfgDouble("range", 20.0);
        Location targetLoc = getTargetLocation(player, range);
        
        if (targetLoc == null) {
            player.sendMessage(Component.text("§c✦ No valid dash target found within range!"));
            return null;
        }
        
        // Check if path is clear
        if (!isPathClear(player.getLocation(), targetLoc)) {
            player.sendMessage(Component.text("§c✦ Your path through the stars is blocked!"));
            return null;
        }
        
        Location startLoc = player.getLocation().clone();
        
        // Play activation sound
        player.playSound(startLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.8f);
        player.playSound(startLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 2.0f);
        
        return new DashEffect(startLoc, targetLoc, player);
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull DashEffect effect) {
        Player player = (Player) context.caster();
        
        // Teleport player instantly
        player.teleport(effect.destination);
        
        // Play arrival sound
        player.playSound(effect.destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.9f);
        player.playSound(effect.destination, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 2.2f);
        
        // Show success message
        player.sendMessage(Component.text("§b✦ §7You dash through the cosmic void! §b✦"));
        
        // Create spectacular visual effects
        createStellarTrail(context, effect);
        createConstellationBursts(context, effect);
    }
    
    /* ---------------------------------------- */
    /* HELPER METHODS */
    /* ---------------------------------------- */
    private @Nullable Location getTargetLocation(Player player, double range) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        
        // Raycast to find target location
        for (double i = 1; i <= range; i += 0.5) {
            Location checkLoc = eyeLoc.clone().add(direction.clone().multiply(i));
            
            // Check if we hit a solid block
            if (!checkLoc.getBlock().isPassable()) {
                // Step back slightly and check if there's room for the player
                Location targetLoc = eyeLoc.clone().add(direction.clone().multiply(Math.max(1, i - 1)));
                if (isLocationSafe(targetLoc)) {
                    return targetLoc;
                }
                return null;
            }
        }
        
        // Max range location
        Location targetLoc = eyeLoc.clone().add(direction.clone().multiply(range));
        return isLocationSafe(targetLoc) ? targetLoc : null;
    }
    
    private boolean isPathClear(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        
        for (double i = 0; i < distance; i += 0.5) {
            Location checkLoc = start.clone().add(direction.clone().multiply(i));
            if (!checkLoc.getBlock().isPassable()) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isLocationSafe(Location loc) {
        return loc.getBlock().isPassable() && 
               loc.clone().add(0, 1, 0).getBlock().isPassable() &&
               !loc.clone().subtract(0, 1, 0).getBlock().isPassable();
    }
    
    private void createStellarTrail(SpellContext context, DashEffect effect) {
        new BukkitRunnable() {
            private int step = 0;
            private final int totalSteps = 30;
            private final List<Location> trailPoints = calculateTrailPoints(effect.origin, effect.destination, totalSteps);
            
            @Override
            public void run() {
                if (step >= totalSteps) {
                    cancel();
                    return;
                }
                
                // Create trail segment
                Location trailLoc = trailPoints.get(step);
                World world = trailLoc.getWorld();
                
                // Main star particles
                world.spawnParticle(Particle.END_ROD, trailLoc, 3, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticle(Particle.FIREWORK, trailLoc, 2, 0.15, 0.15, 0.15, 0.03);
                
                // Colored stardust
                Color starColor = getStarColor(step, totalSteps);
                world.spawnParticle(Particle.DUST, trailLoc, 4, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(starColor, 1.5f));
                
                // Cosmic sparkles
                if (step % 3 == 0) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, trailLoc, 2, 0.4, 0.4, 0.4, 0.1);
                }
                
                // Constellation patterns every few steps
                if (step % 5 == 0) {
                    createMiniConstellation(trailLoc);
                }
                
                step++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }
    
    private void createConstellationBursts(SpellContext context, DashEffect effect) {
        // Origin constellation burst
        new BukkitRunnable() {
            @Override
            public void run() {
                createConstellationBurst(effect.origin, true);
            }
        }.runTaskLater(context.plugin(), 2L);
        
        // Destination constellation burst
        new BukkitRunnable() {
            @Override
            public void run() {
                createConstellationBurst(effect.destination, false);
            }
        }.runTaskLater(context.plugin(), 8L);
    }
    
    private void createConstellationBurst(Location center, boolean isOrigin) {
        World world = center.getWorld();
        int constellationSize = cfgInt("constellation.size", 12);
        double radius = cfgDouble("constellation.radius", 2.5);
        
        // Create constellation pattern
        for (int i = 0; i < constellationSize; i++) {
            double angle = 2 * Math.PI * i / constellationSize;
            double starRadius = radius * (0.6 + 0.4 * Math.random());
            double x = Math.cos(angle) * starRadius;
            double z = Math.sin(angle) * starRadius;
            double y = Math.sin(angle * 2) * 0.5;
            
            Location starLoc = center.clone().add(x, y + 1, z);
            
            // Bright star particles
            world.spawnParticle(Particle.END_ROD, starLoc, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FIREWORK, starLoc, 2, 0.1, 0.1, 0.1, 0.02);
            
            // Star color based on origin/destination
            Color starColor = isOrigin ? Color.PURPLE : Color.AQUA;
            world.spawnParticle(Particle.DUST, starLoc, 2, 0.05, 0.05, 0.05, 0,
                new Particle.DustOptions(starColor, 2.0f));
        }
        
        // Central star burst
        world.spawnParticle(Particle.END_ROD, center.clone().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.FIREWORK, center.clone().add(0, 1.5, 0), 10, 0.8, 0.8, 0.8, 0.15);
    }
    
    private void createMiniConstellation(Location center) {
        World world = center.getWorld();
        
        // Small constellation of 3-5 stars
        int stars = 3 + (int) (Math.random() * 3);
        for (int i = 0; i < stars; i++) {
            double angle = 2 * Math.PI * i / stars;
            double radius = 0.8 + Math.random() * 0.4;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = Math.random() * 0.4 - 0.2;
            
            Location starLoc = center.clone().add(x, y, z);
            
            world.spawnParticle(Particle.END_ROD, starLoc, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.DUST, starLoc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(Color.WHITE, 1.2f));
        }
    }
    
    private List<Location> calculateTrailPoints(Location start, Location end, int steps) {
        List<Location> points = new ArrayList<>();
        Vector direction = end.toVector().subtract(start.toVector());
        
        for (int i = 0; i < steps; i++) {
            double progress = (double) i / (steps - 1);
            
            // Add some curve to the trail for visual appeal
            Vector offset = direction.clone().multiply(progress);
            double curve = Math.sin(progress * Math.PI) * 2.0;
            offset.setY(offset.getY() + curve);
            
            Location point = start.clone().add(offset);
            points.add(point);
        }
        
        return points;
    }
    
    private Color getStarColor(int step, int totalSteps) {
        double progress = (double) step / totalSteps;
        
        // Transition from purple to blue to white to aqua
        if (progress < 0.33) {
            // Purple to blue
            double subProgress = progress * 3;
            return Color.fromRGB((int)(128 * (1 - subProgress) + 0 * subProgress),
                                0,
                                (int)(255 * (1 - subProgress) + 255 * subProgress));
        } else if (progress < 0.67) {
            // Blue to white
            double subProgress = (progress - 0.33) * 3;
            return Color.fromRGB((int)(0 * (1 - subProgress) + 255 * subProgress),
                                (int)(0 * (1 - subProgress) + 255 * subProgress),
                                255);
        } else {
            // White to aqua
            double subProgress = (progress - 0.67) * 3;
            return Color.fromRGB((int)(255 * (1 - subProgress) + 0 * subProgress),
                                255,
                                255);
        }
    }
    
    /* ---------------------------------------- */
    /* EFFECT CLASS */
    /* ---------------------------------------- */
    public static class DashEffect {
        public final Location origin;
        public final Location destination;
        public final Player player;
        
        public DashEffect(Location origin, Location destination, Player player) {
            this.origin = origin;
            this.destination = destination;
            this.player = player;
        }
    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("stellar-dash." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("stellar-dash." + path, def);
    }
}