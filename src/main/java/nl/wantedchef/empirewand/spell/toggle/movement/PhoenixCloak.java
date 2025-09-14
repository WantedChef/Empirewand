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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * PhoenixCloak - A majestic visual phoenix transformation spell.
 * 
 * Features:
 * - Stunning phoenix wings with realistic animation
 * - Flowing fire cloak effect around the player
 * - Phoenix rebirth cycles with visual transformations
 * - Performance-optimized particle system
 * - Flight capability with phoenix trail effects
 * - Fire resistance and regeneration
 */
public final class PhoenixCloak extends Spell<Void> implements ToggleableSpell {

    private final Map<UUID, PhoenixData> phoenixes = new WeakHashMap<>();

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Phoenix Cloak";
            description = "Transform into a magnificent phoenix with stunning wings and fire cloak.";
            cooldown = Duration.ofSeconds(8);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new PhoenixCloak(this);
        }
    }

    private PhoenixCloak(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "phoenix-cloak";
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

    @Override
    public boolean isActive(Player player) {
        return phoenixes.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player)) return;
        phoenixes.put(player.getUniqueId(), new PhoenixData(player, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        Optional.ofNullable(phoenixes.remove(player.getUniqueId())).ifPresent(PhoenixData::stop);
    }

    @Override
    public void forceDeactivate(Player player) {
        Optional.ofNullable(phoenixes.remove(player.getUniqueId())).ifPresent(PhoenixData::stop);
    }

    private final class PhoenixData {
        private final Player player;
        private final BukkitTask ticker;
        private int tickCounter = 0;
        private double wingPhase = 0;
        private double cloakPhase = 0;
        private Location lastLocation;

        PhoenixData(Player player, SpellContext context) {
            this.player = player;
            this.lastLocation = player.getLocation().clone();

            // Phoenix awakening effect
            spawnPhoenixAwakening();

            // Show transformation message
            player.sendMessage(Component.text("🔥 ")
                .color(NamedTextColor.GOLD)
                .append(Component.text("The Phoenix Cloak envelops you in mystical fire!")
                .color(NamedTextColor.YELLOW)));

            // Grant flight and effects
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }

            // Start visual effects
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 2);
        }

        void stop() {
            ticker.cancel();
            
            // Phoenix farewell effect
            spawnPhoenixFarewell();
            
            player.sendMessage(Component.text("🔥 ")
                .color(NamedTextColor.GOLD)
                .append(Component.text("The Phoenix Cloak fades away...")
                .color(NamedTextColor.GRAY)));

            // Remove flight if we granted it
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }

            // Remove effects
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        }

        private void spawnPhoenixAwakening() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Phoenix cry sound
            world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.8f);
            world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 2.0f);
            
            // Awakening fire burst
            for (int i = 0; i < 30; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = Math.random() * 3.0;
                double height = Math.random() * 4.0;
                
                Location particleLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.05);
                world.spawnParticle(Particle.LAVA, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                
                // Phoenix colors
                Color phoenixColor = getPhoenixColor(i * 0.2);
                world.spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(phoenixColor, 1.5f));
            }
        }

        private void spawnPhoenixFarewell() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Farewell sound
            world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.2f);
            
            // Dissipating fire spiral
            for (int i = 0; i < 20; i++) {
                double angle = i * 0.3;
                double radius = i * 0.15;
                double height = i * 0.2;
                
                Location spiralLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                world.spawnParticle(Particle.SMOKE, spiralLoc, 1, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.ASH, spiralLoc, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            tickCounter++;
            wingPhase += 0.2;
            cloakPhase += 0.15;

            Location currentLoc = player.getLocation();
            World world = player.getWorld();

            // Apply phoenix effects
            applyPhoenixEffects();

            // Only show visuals while flying/moving
            if (player.isFlying() || player.getGameMode() == GameMode.CREATIVE) {
                
                // Phoenix wings
                spawnPhoenixWings(currentLoc);
                
                // Fire cloak around player
                spawnFireCloak(currentLoc);
                
                // Phoenix trail when moving
                if (lastLocation != null && currentLoc.distanceSquared(lastLocation) > 0.01) {
                    spawnPhoenixTrail(currentLoc);
                }

                // Periodic phoenix sounds
                if (tickCounter % 100 == 0) {
                    world.playSound(currentLoc, Sound.ENTITY_BLAZE_SHOOT, 0.3f, 1.5f);
                }
            }

            lastLocation = currentLoc.clone();
        }

        private void applyPhoenixEffects() {
            int duration = 60; // 3 seconds
            
            // Core phoenix abilities
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false));
            
            // Minor regeneration for phoenix theme
            if (tickCounter % 40 == 0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, false));
            }
        }

        private void spawnPhoenixWings(Location center) {
            Vector direction = player.getLocation().getDirection().normalize();
            Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
            
            double wingSpan = cfgDouble("phoenix.wing-span", 2.5);
            double wingHeight = cfgDouble("phoenix.wing-height", 1.8);
            int wingSegments = cfgInt("phoenix.wing-segments", 8);
            double flapIntensity = Math.sin(wingPhase) * 0.5 + 0.7;

            // Both wings
            for (int side = -1; side <= 1; side += 2) {
                Vector sideVector = right.clone().multiply(side);

                for (int segment = 0; segment < wingSegments; segment++) {
                    double progress = (double) segment / (wingSegments - 1);
                    
                    // Wing shape - realistic bird wing curve
                    double wingWidth = wingSpan * progress * (1.0 - progress * 0.3);
                    double wingVertical = wingHeight * Math.sin(progress * Math.PI * 0.8) * flapIntensity;
                    double wingBack = progress * 0.3;

                    Location wingPos = center.clone()
                        .add(sideVector.clone().multiply(wingWidth))
                        .add(0, wingVertical + 1.0, 0)
                        .add(direction.clone().multiply(-wingBack));

                    // Phoenix wing particles
                    player.getWorld().spawnParticle(Particle.FLAME, wingPos, 1, 0.03, 0.03, 0.03, 0.01);
                    
                    // Wing tip colors
                    if (progress > 0.6) {
                        Color wingColor = getPhoenixColor(wingPhase + segment);
                        player.getWorld().spawnParticle(Particle.DUST, wingPos, 1, 0.02, 0.02, 0.02, 0,
                            new Particle.DustOptions(wingColor, 1.2f));
                    }

                    // Wing membrane effect
                    if (segment > 0 && progress < 0.8) {
                        Location membranePos = wingPos.clone().add(0, -0.15, 0.05);
                        player.getWorld().spawnParticle(Particle.FLAME, membranePos, 1, 0.01, 0.01, 0.01, 0.005);
                    }
                }
            }
        }

        private void spawnFireCloak(Location center) {
            int cloakParticles = cfgInt("phoenix.cloak-particles", 12);
            double cloakRadius = cfgDouble("phoenix.cloak-radius", 1.2);
            
            for (int i = 0; i < cloakParticles; i++) {
                double angle = (2 * Math.PI * i / cloakParticles) + cloakPhase;
                double radius = cloakRadius + Math.sin(cloakPhase + i * 0.5) * 0.3;
                double height = Math.sin(angle * 2 + cloakPhase) * 0.8 + 1.5;
                
                Location cloakLoc = center.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                // Flowing fire cloak
                player.getWorld().spawnParticle(Particle.FLAME, cloakLoc, 1, 0.05, 0.1, 0.05, 0.02);
                
                // Cloak colors
                if (Math.random() < 0.6) {
                    Color cloakColor = getPhoenixColor(cloakPhase + i * 0.3);
                    player.getWorld().spawnParticle(Particle.DUST, cloakLoc, 1, 0.03, 0.03, 0.03, 0,
                        new Particle.DustOptions(cloakColor, 1.0f));
                }
            }
        }

        private void spawnPhoenixTrail(Location current) {
            Vector movement = current.toVector().subtract(lastLocation.toVector());
            double speed = movement.length();
            
            if (speed > 0.02) {
                int trailLength = cfgInt("phoenix.trail-length", 6);
                
                for (int i = 0; i < trailLength; i++) {
                    double progress = (double) i / trailLength;
                    Location trailPos = current.clone().subtract(movement.clone().multiply(progress * 3));
                    
                    // Fading trail intensity
                    double intensity = 1.0 - progress * 0.7;
                    
                    if (Math.random() < intensity * 0.8) {
                        player.getWorld().spawnParticle(Particle.FLAME, trailPos, 1, 
                            0.1 * intensity, 0.1 * intensity, 0.1 * intensity, 0.02);
                        
                        // Trail colors
                        Color trailColor = getPhoenixColor(cloakPhase + progress * Math.PI);
                        player.getWorld().spawnParticle(Particle.DUST, trailPos, 1, 
                            0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(trailColor, (float)intensity));
                    }
                }
            }
        }

        private Color getPhoenixColor(double phase) {
            double normalizedPhase = (Math.sin(phase) + 1) / 2;
            
            if (normalizedPhase < 0.4) {
                // Deep red to orange
                return Color.fromRGB(255, (int)(50 + 150 * normalizedPhase * 2.5), 0);
            } else if (normalizedPhase < 0.7) {
                // Orange to golden yellow
                double subPhase = (normalizedPhase - 0.4) * 3.33;
                return Color.fromRGB(255, (int)(200 + 55 * subPhase), (int)(50 * subPhase));
            } else {
                // Golden to bright orange
                double subPhase = (normalizedPhase - 0.7) * 3.33;
                return Color.fromRGB(255, (int)(255 - 50 * subPhase), (int)(50 * (1 - subPhase)));
            }
        }
    }

    // Config helpers
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("phoenix-cloak." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("phoenix-cloak." + path, def);
    }
}