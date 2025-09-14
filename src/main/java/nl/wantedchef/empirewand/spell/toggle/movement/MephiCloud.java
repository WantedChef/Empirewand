package nl.wantedchef.empirewand.spell.toggle.movement;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * MephiCloud - Simplified volcanic flight spell with elegant fire effects.
 * 
 * Features:
 * - Clean volcanic platform with lava particles
 * - Optimized fire effects and ember trail
 * - Performance-friendly particle system
 * - Simple temperature-based visual effects
 */
public final class MephiCloud extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, CloudData> clouds = new WeakHashMap<>();

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Mephi Cloud";
            description = "Summon a volcanic infernal platform with advanced thermodynamic fire physics and ember storms.";
            cooldown = Duration.ofSeconds(3);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new MephiCloud(this);
        }
    }

    private MephiCloud(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "mephi-cloud";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    /* ---------------------------------------- */
    /* TOGGLE API */
    /* ---------------------------------------- */
    @Override
    public boolean isActive(Player player) {
        return clouds.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player))
            return;
        clouds.put(player.getUniqueId(), new CloudData(player, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        Optional.ofNullable(clouds.remove(player.getUniqueId())).ifPresent(CloudData::stop);
    }

    @Override
    public void forceDeactivate(Player player) {
        Optional.ofNullable(clouds.remove(player.getUniqueId())).ifPresent(CloudData::stop);
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class CloudData {
        private final Player player;
        private final BukkitTask ticker;
        private int tickCounter = 0;
        
        // Simplified volcanic variables
        private double flamePhase = 0;
        private double emberPhase = 0;
        @org.jetbrains.annotations.Nullable private Location lastLocation;

        CloudData(Player player, SpellContext context) {
            this.player = player;
            final Location location = player.getLocation();
            if (location == null) {
                this.ticker = null;
                this.lastLocation = null;
                Bukkit.getScheduler().runTask(context.plugin(), () -> forceDeactivate(player));
                return;
            }
            this.lastLocation = location.clone();
            
            // Show activation message
            player.sendMessage(Component.text(
                    "\u00A74✦ \u00A7cVolcanic platform forms beneath you! \u00A74✦"));
            
            // Play volcanic sounds
            player.playSound(location, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.8f);
            player.playSound(location, Sound.BLOCK_LAVA_AMBIENT, 0.6f, 0.9f);
            
            // Spawn activation effect
            spawnVolcanicActivation();

            // Enable flight
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }

            // Start the ticker (every 2 ticks for performance)
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 2);
        }

        void stop() {
            if (ticker != null) {
                ticker.cancel();
            }
            
            // Volcanic deactivation effect
            spawnVolcanicDeactivation();
            
            player.sendMessage(Component.text("\u00A74✦ \u00A77The volcano becomes dormant... \u00A74✦"));
            final Location location = player.getLocation();
            if (location != null) {
                player.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 0.8f);
            }
            
            // Remove flight if we granted it
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
        
        private void spawnVolcanicActivation() {
            Location loc = player.getLocation();
            if (loc == null) {
                return;
            }
            World world = java.util.Objects.requireNonNull(player.getWorld(), "world");
            
            // Simple volcanic burst
            for (int i = 0; i < 20; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = Math.random() * 2.5;
                double height = Math.random() * 2.0;
                
                Location particleLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.2, 0.1, 0.05);
                world.spawnParticle(Particle.LAVA, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                
                // Orange volcanic color
                Color volcColor = Color.fromRGB(255, 100, 0);
                world.spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(volcColor, 1.5f));
            }
        }
        
        private void spawnVolcanicDeactivation() {
            Location loc = player.getLocation();
            if (loc == null) {
                return;
            }
            World world = java.util.Objects.requireNonNull(player.getWorld(), "world");
            
            // Cooling spiral effect
            for (int i = 0; i < 15; i++) {
                double angle = i * 0.4;
                double y = i * 0.15;
                double radius = 1.5 - (i * 0.08);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                
                Location particleLoc = loc.clone().add(x, y, z);
                world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.05, 0.1, 0.02);
                world.spawnParticle(Particle.ASH, particleLoc, 1, 0.08, 0.03, 0.08, 0.01);
            }
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            tickCounter++;
            flamePhase += 0.15;
            emberPhase += 0.12;
            
            Location currentLoc = player.getLocation();
            if (currentLoc == null) {
                forceDeactivate(player);
                return;
            }
            
            // Only show particles if player is flying
            if (player.isFlying() || player.getGameMode() == GameMode.CREATIVE) {
                spawnVolcanicPlatform(currentLoc);
                spawnSimpleEmbers(currentLoc);
                
                // Movement trail
                if (lastLocation != null && currentLoc.distanceSquared(lastLocation) > 0.02) {
                    spawnVolcanicTrail(currentLoc);
                }
                
                // Periodic sounds
                if (tickCounter % 80 == 0) {
                    player.playSound(currentLoc, Sound.BLOCK_LAVA_POP, 0.4f, 0.9f);
                }
            }
            
            lastLocation = currentLoc.clone();
        }
        
        private void spawnVolcanicPlatform(Location playerLoc) {
            World world = java.util.Objects.requireNonNull(player.getWorld(), "world");
            Location platformCenter = playerLoc.clone().subtract(0, 1.5, 0);
            
            double platformRadius = 2.0;
            int particles = 12;
            
            for (int i = 0; i < particles; i++) {
                double angle = 2 * Math.PI * i / particles + flamePhase * 0.2;
                double radius = platformRadius * Math.random() * 0.8;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(flamePhase + angle) * 0.15;
                
                Location lavaLoc = platformCenter.clone().add(x, y, z);
                
                // Simple lava platform
                if (Math.random() < 0.7) {
                    world.spawnParticle(Particle.FLAME, lavaLoc, 1, 0.2, 0.1, 0.2, 0.02);
                    
                    if (Math.random() < 0.3) {
                        world.spawnParticle(Particle.LAVA, lavaLoc, 1, 0.05, 0.05, 0.05, 0);
                    }
                    
                    // Volcanic orange color
                    Color lavaColor = Color.fromRGB(255, 80, 0);
                    world.spawnParticle(Particle.DUST, lavaLoc, 1, 0.1, 0.05, 0.1, 0,
                        new Particle.DustOptions(lavaColor, 1.2f));
                }
            }
        }
        
        private void spawnSimpleEmbers(Location center) {
            World world = java.util.Objects.requireNonNull(center.getWorld(), "world");
            
            for (int i = 0; i < 8; i++) {
                double angle = emberPhase + i * 0.8;
                double radius = 2.0 + Math.sin(emberPhase + i) * 0.5;
                double height = Math.sin(angle) * 1.0 + 2.0;
                
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                
                Location emberLoc = center.clone().add(x, height, z);
                
                // Simple ember particles
                world.spawnParticle(Particle.FLAME, emberLoc, 1, 0.03, 0.03, 0.03, 0.01);
                
                if (Math.random() < 0.4) {
                    Color emberColor = Color.fromRGB(255, 120, 0);
                    world.spawnParticle(Particle.DUST, emberLoc, 1, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(emberColor, 1.0f));
                }
            }
        }
        
        private void spawnVolcanicTrail(Location current) {
            if (lastLocation == null) return;
            
            World world = java.util.Objects.requireNonNull(current.getWorld(), "world");
            Vector movement = current.toVector().subtract(lastLocation.toVector());
            
            if (movement.lengthSquared() > 0.01) {
                for (int i = 0; i < 6; i++) {
                    double progress = (double) i / 6;
                    Location trailPoint = current.clone().subtract(movement.clone().multiply(progress * 2));
                    
                    double intensity = 1.0 - progress * 0.6;
                    if (intensity > 0.2) {
                        world.spawnParticle(Particle.FLAME, trailPoint, 1, 0.1, 0.1, 0.1, 0.02);
                        
                        Color trailColor = Color.fromRGB(255, (int)(60 + 60 * intensity), 0);
                        world.spawnParticle(Particle.DUST, trailPoint, 1, 0.08, 0.08, 0.08, 0,
                            new Particle.DustOptions(trailColor, (float)intensity));
                    }
                }
            }
        }
    }
}
