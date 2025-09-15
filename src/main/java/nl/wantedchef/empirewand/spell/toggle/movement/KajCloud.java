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
 * KajCloud - Divine holy spirit god cloud that creates an awe-inspiring celestial platform.
 * Magnificent particle effects with heavenly sounds and godly presence.
 */
public final class KajCloud extends Spell<Void> implements ToggleableSpell {

    private final Map<UUID, CloudData> clouds = new WeakHashMap<>();

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Divine God Cloud";
            description = "Summon a magnificent holy spirit cloud with divine power and celestial glory.";
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
        // Toggle the cloud on/off for the caster when the spell is cast
        toggle(context.caster(), context);
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

            // MAGNIFICENT DIVINE CLOUD ACTIVATION - HOLY SPIRIT GOD EFFECT!
            World world = player.getWorld();
            Location base = location.clone().subtract(0, 1.2, 0);
            
            // MASSIVE divine cloud formation with multiple layers
            world.spawnParticle(Particle.CLOUD, base, 80, 1.5, 0.4, 1.5, 0.08);
            world.spawnParticle(Particle.WHITE_ASH, base, 40, 1.2, 0.3, 1.2, 0.05);
            world.spawnParticle(Particle.SMOKE, base, 25, 1.0, 0.2, 1.0, 0.03);
            
            // HOLY SPIRIT divine radiance effects
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 30, 1.0, 1.2, 1.0, 0.1);
            world.spawnParticle(Particle.END_ROD, base.clone().add(0, 0.5, 0), 20, 0.8, 0.4, 0.8, 0.08);
            world.spawnParticle(Particle.ENCHANT, location, 50, 1.2, 1.5, 1.2, 1.0);
            world.spawnParticle(Particle.SCRAPE, base.clone().add(0, 0.2, 0), 15, 0.6, 0.1, 0.6, 0.02);
            
            // GOD-LIKE celestial aura
            world.spawnParticle(Particle.GLOW, location.clone().add(0, 0.5, 0), 35, 1.0, 0.8, 1.0, 0.1);
            world.spawnParticle(Particle.ELECTRIC_SPARK, base.clone().add(0, 0.8, 0), 25, 1.0, 0.6, 1.0, 0.15);
            
            // HEAVENLY sounds - divine activation
            world.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
            world.playSound(location, Sound.BLOCK_CONDUIT_ACTIVATE, 0.8f, 1.8f);
            world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 2.0f);
            world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);

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
            
            // DIVINE CLOUD DISSIPATION - HOLY SPIRIT DEPARTING
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
            
            // MAGNIFICENT cloud dissipation with divine ascension
            world.spawnParticle(Particle.CLOUD, base, 60, 1.2, 0.3, 1.2, 0.06);
            world.spawnParticle(Particle.WHITE_ASH, base, 30, 1.0, 0.5, 1.0, 0.08);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 25, 0.8, 1.0, 0.8, 0.2);
            world.spawnParticle(Particle.ENCHANT, location, 40, 1.0, 1.2, 1.0, 0.8);
            world.spawnParticle(Particle.GLOW, location.clone().add(0, 1.0, 0), 20, 0.6, 1.5, 0.6, 0.3);
            
            // HEAVENLY departure sounds
            world.playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.3f);
            world.playSound(location, Sound.BLOCK_CONDUIT_DEACTIVATE, 0.6f, 1.5f);
            world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 0.8f);

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
                
                // DIVINE GOD CLOUD PARTICLES - HOLY SPIRIT PRESENCE!
                if (moving) {
                    // Moving divine cloud - MAGNIFICENT trail effect
                    world.spawnParticle(Particle.CLOUD, base, 25, 1.0, 0.25, 1.0, 0.05);
                    world.spawnParticle(Particle.WHITE_ASH, base, 15, 0.8, 0.2, 0.8, 0.03);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, base.clone().add(0, 0.3, 0), 8, 0.6, 0.15, 0.6, 0.02);
                    
                    if (tickCounter % 2 == 0) { // Every 2nd tick - more frequent divine effects
                        world.spawnParticle(Particle.ENCHANT, base.clone().add(0, 0.4, 0), 12, 0.8, 0.3, 0.8, 0.1);
                        world.spawnParticle(Particle.GLOW, base.clone().add(0, 0.2, 0), 6, 0.5, 0.1, 0.5, 0.02);
                    }
                    
                    if (tickCounter % 4 == 0) { // Divine sparkles
                        world.spawnParticle(Particle.ELECTRIC_SPARK, base.clone().add(0, 0.5, 0), 4, 0.6, 0.2, 0.6, 0.05);
                    }
                } else {
                    // Stationary divine cloud - GODLY presence
                    if (tickCounter % 2 == 0) { // More frequent for divine aura
                        world.spawnParticle(Particle.CLOUD, base, 15, 0.8, 0.2, 0.8, 0.03);
                        world.spawnParticle(Particle.WHITE_ASH, base, 8, 0.6, 0.15, 0.6, 0.02);
                    }
                    
                    if (tickCounter % 3 == 0) { // Holy spirit effects
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, base.clone().add(0, 0.2, 0), 5, 0.4, 0.1, 0.4, 0.01);
                        world.spawnParticle(Particle.ENCHANT, base.clone().add(0, 0.3, 0), 8, 0.6, 0.2, 0.6, 0.05);
                    }
                    
                    if (tickCounter % 5 == 0) { // Divine glow pulses
                        world.spawnParticle(Particle.GLOW, base.clone().add(0, 0.4, 0), 6, 0.5, 0.15, 0.5, 0.02);
                    }
                }
                
                tickCounter++;
            }

            lastLocation = loc.clone();
        }
    }
}