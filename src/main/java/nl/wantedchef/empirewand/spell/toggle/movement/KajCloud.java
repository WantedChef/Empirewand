package nl.wantedchef.empirewand.spell.toggle.movement;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * KajCloud - Simple and elegant cloud flight spell that looks like a real cloud.
 * Performance-optimized with no lag on client or server.
 */
public final class KajCloud extends Spell<Void> implements ToggleableSpell {

    private final Map<UUID, CloudData> clouds = new WeakHashMap<>();

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Kaj Cloud";
            description = "Summon a fluffy cloud platform to fly on.";
            cooldown = Duration.ofSeconds(3);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new KajCloud(this);
        }
    }

    private KajCloud(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "kaj-cloud";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        // Implementation handled by toggle methods
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Nothing to handle
    }

    @Override
    public void forceDeactivate(@NotNull Player player) {
        CloudData cloud = clouds.remove(player.getUniqueId());
        if (cloud != null) {
            cloud.stop();
        }
    }

    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        clouds.put(player.getUniqueId(), new CloudData(player, context));
    }

    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        CloudData cloud = clouds.remove(player.getUniqueId());
        if (cloud != null) {
            cloud.stop();
        }
    }

    @Override
    public boolean isActive(@NotNull Player player) {
        return clouds.containsKey(player.getUniqueId());
    }

    private final class CloudData {
        private final Player player;
        private final BukkitTask ticker;
        private int tickCounter = 0;
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

            // Simple cloud activation effect
            World world = player.getWorld();
            Location base = location.clone().subtract(0, 1.2, 0);
            
            // Clean cloud formation
            world.spawnParticle(Particle.CLOUD, base, 15, 0.8, 0.2, 0.8, 0.03);
            world.spawnParticle(Particle.WHITE_ASH, base, 8, 0.6, 0.15, 0.6, 0.02);
            
            // Subtle magical effects
            world.spawnParticle(Particle.ENCHANT, location, 12, 0.8, 1.0, 0.8, 0.5);
            world.spawnParticle(Particle.END_ROD, base.clone().add(0, 0.3, 0), 4, 0.4, 0.2, 0.4, 0.03);
            
            // Clean sound
            world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.2f);

            // Enable flight
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }

            // Start cloud visual effects - runs every 4 ticks for performance (5 times per second)
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 4);
        }

        void stop() {
            if (ticker != null) {
                ticker.cancel();
            }
            
            // Simple deactivation effect
            World world = player.getWorld();
            final Location location = player.getLocation();
            if (location == null) {
                // Turn off flight if we enabled it (keep creative/spectator intact)
                if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
                return;
            }
            Location base = location.clone().subtract(0, 1.2, 0);
            
            // Cloud dissipation
            world.spawnParticle(Particle.CLOUD, base, 12, 0.6, 0.15, 0.6, 0.02);
            world.spawnParticle(Particle.ENCHANT, location, 8, 0.6, 0.8, 0.6, 0.3);
            
            // Clean sound
            world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.4f, 0.8f);

            // Turn off flight if we enabled it (keep creative/spectator intact)
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            Location loc = player.getLocation();
            if (loc == null) {
                forceDeactivate(player);
                return;
            }
            World world = java.util.Objects.requireNonNull(player.getWorld(), "world");

            // Only show the cloud while the player is flying
            if (player.isFlying() || player.getGameMode() == GameMode.CREATIVE) {
                boolean moving = lastLocation == null || loc.distanceSquared(lastLocation) > 0.02;
                Location base = loc.clone().subtract(0, 1.3, 0);
                
                // Optimized cloud particles
                if (moving) {
                    // Moving cloud - nice trail effect
                    world.spawnParticle(Particle.CLOUD, base, 6, 0.6, 0.15, 0.6, 0.02);
                    if (tickCounter % 3 == 0) { // Every 3rd tick
                        world.spawnParticle(Particle.WHITE_ASH, base, 2, 0.4, 0.08, 0.4, 0.01);
                    }
                } else {
                    // Stationary cloud - minimal particles
                    if (tickCounter % 4 == 0) { // Every 4th tick
                        world.spawnParticle(Particle.CLOUD, base, 3, 0.4, 0.1, 0.4, 0.01);
                    }
                }
                
                tickCounter++;
            }

            lastLocation = loc.clone();
        }
    }
}