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
import org.jetbrains.annotations.Nullable;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * DragonFury — Minimal, stable flight toggle.
 *
 * Design goals:
 * - Keep only essential effects: SLOW_FALLING and RESISTANCE (short duration, re‑applied).
 * - Render low‑resolution wings with yaw smoothing to reduce jitter and particles.
 * - Subtle activate/deactivate sounds; no chat, no boss bars, no heavy helpers.
 */
public final class DragonFury extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, DragonRiderData> dragonRiders = new WeakHashMap<>();
    // Removed unused config toggles; keep the spell minimal and predictable

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Dragon Fury";
            description = "Minimal draconic glide with subtle wings and protection.";
            cooldown = Duration.ofSeconds(15);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new DragonFury(this);
        }
    }

    private DragonFury(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public @NotNull String key() {
        return "dragon-fury";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
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
    public boolean isActive(@NotNull Player player) {
        return dragonRiders.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player))
            return;
        dragonRiders.put(player.getUniqueId(), new DragonRiderData(player, context));
    }

    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        Optional.ofNullable(dragonRiders.remove(player.getUniqueId())).ifPresent(DragonRiderData::stop);
    }

    @Override
    public void forceDeactivate(@NotNull Player player) {
        Optional.ofNullable(dragonRiders.remove(player.getUniqueId())).ifPresent(DragonRiderData::stop);
    }

    @Override
    public int getMaxDuration() {
        return cfgInt("max-duration-ticks", 1500); // 1.25 minutes default
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class DragonRiderData {
        private final Player player;
        private final BukkitTask ticker;
        private int tickCounter = 0;
        private Location lastLocation;
        // Smoothed yaw (radians) to stabilize wing rendering while turning
        private double smoothedYawRad = Double.NaN;

        DragonRiderData(Player player, SpellContext context) {
            this.player = player;
            this.lastLocation = player.getLocation().clone();
            
            // Subtle activation sound (kept lightweight)
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.9f);
            
            // Minimal: no activation chat message
            
            // Grant flight if not already flying
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }
            
            // Start the dragon rider ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            
            // Remove dragon effects
            removeDragonEffects();
            // Minimal: subtle deactivation cue
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1.0f);
            
            // Remove flight if granted by spell
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            // Increment tick counter for timing
            tickCounter++;

            // Minimal fixed effects
            int duration = 60;
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, false, false));

            // Stable dragon wings behind the player (less random while turning)
            Location base = player.getLocation();
            World world = base.getWorld();
            if (world != null) {
                // Update wing positioning every 2 ticks for smoothness without trails
                if (tickCounter % 2 == 0) {
                    // Smooth yaw to avoid jitter while turning (ignore pitch)
                    double yawRad = Math.toRadians(base.getYaw());
                    if (Double.isNaN(smoothedYawRad)) smoothedYawRad = yawRad;
                    double delta = normalizeAngle(yawRad - smoothedYawRad);
                    smoothedYawRad = smoothedYawRad + delta * 0.25; // low-pass filter

                    Vector forward = new Vector(-Math.sin(smoothedYawRad), 0, Math.cos(smoothedYawRad)).normalize();
                    Vector right = new Vector(forward.getZ(), 0, -forward.getX()).normalize();
                    Vector up = new Vector(0, 1, 0);
                    Vector back = forward.clone().multiply(-1);

                    // Wing parameters (reduced for performance)
                    int points = 10; // resolution of wing curve
                    double span = 1.8; // wing half-span
                    double backOffset = 0.85; // how far behind the player

                    // Colors
                    Color outline = Color.fromRGB(10, 10, 20);
                    Color inner = Color.fromRGB(80, 20, 20);

                    for (int side = -1; side <= 1; side += 2) {
                        Vector sideVec = right.clone().multiply(side);

                        Location prevPoint = null;
                        for (int i = 0; i < points; i++) {
                            double t = i / (double) (points - 1);
                            // Wing curve: smooth arch with slight taper near tip
                            double outward = span * t;
                            double arch = 0.45 + Math.sin(t * Math.PI) * 1.05;
                            double backZ = backOffset + t * 0.24;

                            Location p = base.clone()
                                    .add(sideVec.clone().multiply(outward))
                                    .add(up.clone().multiply(arch))
                                    .add(back.clone().multiply(backZ));

                            // Outline spine
                            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0,
                                    new Particle.DustOptions(outline, 0.9f));

                            // Membrane lines inward for a solid wing look
                            if (prevPoint != null) {
                                drawLine(world, prevPoint, p, inner, 4);
                            }
                            prevPoint = p;
                        }
                    }
                }
            }

            lastLocation = base.clone();
        }

        private double normalizeAngle(double a) {
            while (a > Math.PI) a -= 2 * Math.PI;
            while (a < -Math.PI) a += 2 * Math.PI;
            return a;
        }

        private void drawLine(World world, Location from, Location to, Color color, int steps) {
            double dx = (to.getX() - from.getX()) / steps;
            double dy = (to.getY() - from.getY()) / steps;
            double dz = (to.getZ() - from.getZ()) / steps;
            for (int i = 1; i < steps; i++) {
                Location p = from.clone().add(dx * i, dy * i, dz * i);
                world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, new Particle.DustOptions(color, 0.55f));
            }
        }
        
        private void removeDragonEffects() {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            // No other effects are applied by this minimal version
        }
        
        // No additional state-specific sounds in minimal version
    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("dragon-fury." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("dragon-fury." + path, def);
    }
}
