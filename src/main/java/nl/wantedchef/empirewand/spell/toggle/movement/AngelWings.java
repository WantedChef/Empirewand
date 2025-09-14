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

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * AngelWings 4.0 - Revolutionary divine flight spell with blazing fire wings.
 *
 * Revolutionary Fire Wing Features:
 * - Magnificent blazing fire wings that form behind the player
 * - Dynamic wing-flapping animations with flame particle physics
 * - Divine fire aura with sacred flame effects
 * - Holy fire protection and celestial blessing powers
 * - Sacred flame trails with divine combustion patterns
 * - Angelic fire harmonics and divine sound orchestration
 * - Divine energy management with fire-based regeneration
 * - Fully configurable fire wing intensity and sacred flame effects
 *
 * @author WantedChef
 */
public final class AngelWings extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, WingData> wings = new WeakHashMap<>();

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    /**
     * Builder for creating {@link AngelWings} instances.
     */
    public static class Builder extends Spell.Builder<Void> {

        /**
         * Creates a new builder for the AngelWings spell.
         *
         * @param api the plugin API
         */
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Angel Wings";
            description = "Manifest blazing wings of sacred fire and soar through the heavens with divine flames.";
            cooldown = Duration.ofSeconds(5);
            spellType = SpellType.MOVEMENT;
        }

        /**
         * Builds the AngelWings spell.
         *
         * @return the constructed spell
         */
        @Override
        public @NotNull Spell<Void> build() {
            return new AngelWings(this);
        }
    }

    /**
     * Constructs the spell from its builder.
     *
     * @param builder the spell builder
     */
    private AngelWings(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */

    /**
     * Gets the configuration key for this spell.
     *
     * @return "angel-wings"
     */
    @Override
    public @NotNull String key() {
        return "angel-wings";
    }

    /**
     * Returns the casting prerequisites.
     *
     * @return a no-op prerequisite
     */
    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Toggles the spell for the caster.
     *
     * @param context the spell context
     * @return always {@code null}
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    /**
     * No additional effect handling is required.
     *
     * @param context the spell context
     * @param result  unused
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    /* ---------------------------------------- */
    /* TOGGLE API */
    /* ---------------------------------------- */

    /**
     * Checks whether the wings are active for the player.
     *
     * @param player the player to check
     * @return {@code true} if active
     */
    @Override
    public boolean isActive(@NotNull Player player) {
        return wings.containsKey(player.getUniqueId());
    }

    /**
     * Activates the wings for a player.
     *
     * @param player  the player
     * @param context the spell context
     */
    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player)) {
            return;
        }
        wings.put(player.getUniqueId(), new WingData(player, context));
    }

    /**
     * Deactivates the wings for a player.
     *
     * @param player  the player
     * @param context the spell context
     */
    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        Optional.ofNullable(wings.remove(player.getUniqueId())).ifPresent(WingData::stop);
    }

    /**
     * Forcefully deactivates the wings.
     *
     * @param player the player
     */
    @Override
    public void forceDeactivate(@NotNull Player player) {
        Optional.ofNullable(wings.remove(player.getUniqueId())).ifPresent(WingData::stop);
    }

    /**
     * Gets the maximum duration of the wings in ticks.
     *
     * @return max duration
     */
    @Override
    public int getMaxDuration() {
        return cfgInt("max-duration-ticks", 2400); // 2 minutes default
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class WingData {
        private final Player player;
        private final BossBar divinePowerBar = BossBar.bossBar(Component.text("Divine Power"), 1,
                BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        private final BukkitTask ticker;
        private double divineEnergy = 100;
        private int tickCounter = 0;
        private double wingPhase = 0;
        private double flapIntensity = 1.0;
        @Nullable private Location lastLocation;
        private final boolean wasFlying;

        WingData(Player player, SpellContext context) {
            this.player = player;
            final Location location = player.getLocation();
            if (location == null) {
                // Cannot activate if player has no location
                this.ticker = null;
                this.wasFlying = false;
                this.lastLocation = null;
                // We can't proceed, so we should probably not even be in this state.
                // Forcing deactivation from the constructor is tricky.
                // The caller should ideally check this.
                // For now, we prevent the ticker from starting.
                Bukkit.getScheduler().runTask(context.plugin(), () -> forceDeactivate(player));
                return;
            }
            this.lastLocation = location.clone();
            this.wasFlying = player.isFlying();

            player.showBossBar(divinePowerBar);

            // Divine activation sounds
            player.playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
            player.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);

            // Show activation message
            player.sendMessage(Component.text(
                    "\u00A76⚐ \u00A7cBlazing wings of sacred fire manifest behind you! Spread your fiery wings and soar! \u00A76⚐"));

            // Spawn magnificent fire activation effect
            spawnFireActivationEffect();

            // Grant flight if not already flying
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }

            // Start the ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            if (this.ticker != null) {
                this.ticker.cancel();
            }
            this.player.hideBossBar(this.divinePowerBar);

            // Remove effects
            this.removeEffects();

            // Deactivation effect
            this.spawnFireDeactivationEffect();

            this.player.sendMessage(Component.text("\u00A76⚐ \u00A77Your blazing wings fold and fade into smoldering embers... \u00A76⚐"));
            final Location location = this.player.getLocation();
            if (location != null) {
                this.player.playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.4f);
            }

            // Remove flight if it was granted by the spell
            if (!this.wasFlying && this.player.getGameMode() != GameMode.CREATIVE && this.player.getGameMode() != GameMode.SPECTATOR) {
                this.player.setAllowFlight(false);
                this.player.setFlying(false);
            }
        }

        private void tick() {
            if (!this.player.isOnline()) {
                forceDeactivate(this.player);
                return;
            }

            this.tickCounter++;
            this.wingPhase += 0.25; // Wing animation speed

            final Location currentLoc = this.player.getLocation();
            if (currentLoc == null) {
                forceDeactivate(this.player);
                return;
            }

            // Energy management
            final boolean isFlying = this.player.isFlying() || this.player.getGameMode() == GameMode.CREATIVE;
            final boolean isMoving = this.lastLocation != null && currentLoc.distanceSquared(this.lastLocation) > 0.01;

            // Energy consumption
            double energyCost = cfgDouble("energy.base-cost", 0.3);
            if (isFlying && isMoving) {
                energyCost += cfgDouble("energy.flight-cost", 0.5);
                this.flapIntensity = 1.5; // More intense flapping when flying
            } else if (isFlying) {
                energyCost += cfgDouble("energy.hover-cost", 0.2);
                this.flapIntensity = 0.8; // Gentle hovering
            } else {
                this.flapIntensity = 0.4; // Minimal wing movement on ground
            }

            // Light level affects divine energy regeneration
            final int lightLevel = currentLoc.getBlock().getLightLevel();
            if (lightLevel >= cfgInt("light.regen-threshold", 12)) {
                this.divineEnergy += cfgDouble("energy.light-regen", 0.4);
            }

            this.divineEnergy -= energyCost;
            this.divineEnergy = Math.max(0, Math.min(100, this.divineEnergy));
            this.divinePowerBar.progress((float) (this.divineEnergy / 100));

            // Check if out of energy
            if (this.divineEnergy <= 0) {
                this.player.sendMessage(Component.text("\u00A7cYour divine energy is depleted! The wings fade..."));
                forceDeactivate(this.player);
                return;
            }

            // Apply divine effects
            this.applyDivineEffects();

            // Wing visual effects
            this.spawnAngelWings(currentLoc);

            // Divine trail when moving
            if (isMoving) {
                this.spawnDivineTrail(currentLoc);
            }

            // Periodic divine sounds
            if (this.tickCounter % 100 == 0) {
                this.player.playSound(currentLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.6f);
            }

            // Wing flap sounds when flying actively
            if (isFlying && isMoving && this.tickCounter % 20 == 0) {
                this.player.playSound(currentLoc, Sound.ITEM_ELYTRA_FLYING, 0.3f, 1.5f);
            }

            this.lastLocation = currentLoc.clone();
        }
        
        private void spawnFireActivationEffect() {
            Location loc = player.getLocation();
            if (loc == null) {
                return;
            }
            World world = player.getWorld();
            
            // Expanding ring of divine light
            for (int i = 0; i < 40; i++) {
                double angle = 2 * Math.PI * i / 40;
                double radius = 3.5;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location particleLoc = loc.clone().add(x, 1.0, z);
                
                // Divine white light ring
                world.spawnParticle(Particle.END_ROD, particleLoc, 2, 0.1, 0.3, 0.1, 0.05);
                world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.2, 0.2, 0.2, 0.02);
                
                // Golden divine essence
                Color divineGold = Color.fromRGB(255, 248, 220); // Cornsilk - divine white-gold
                world.spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0.0,
                    new Particle.DustOptions(divineGold, 2.0f));
                    
                // Falling feathers
                if (Math.random() < 0.3) {
                    Location featherLoc = particleLoc.clone().add(0, 2, 0);
                    world.spawnParticle(Particle.FALLING_DUST, featherLoc, 1, 0.1, 0.1, 0.1, 0);
                }
            }
            
            // Upward burst of divine light
            for (int i = 0; i < 30; i++) {
                double angle = 2 * Math.PI * i / 30;
                double radius = Math.random() * 1.5;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location burstLoc = loc.clone().add(x, 2.0 + Math.random() * 2.5, z);
                
                // Divine light burst
                world.spawnParticle(Particle.END_ROD, burstLoc, 1, 0.1, 0.1, 0.1, 0.03);
                world.spawnParticle(Particle.FIREWORK, burstLoc, 1, 0.2, 0.2, 0.2, 0.1);
                
                // Heavenly sparkles
                if (Math.random() < 0.7) {
                    Color heavenlyWhite = Color.fromRGB(255, 255, 240); // Ivory - pure white
                    world.spawnParticle(Particle.DUST, burstLoc, 1, 0.05, 0.05, 0.05, 0.0,
                        new Particle.DustOptions(heavenlyWhite, 1.8f));
                }
            }
        }
        
        private void spawnFireDeactivationEffect() {
            Location loc = player.getLocation();
            if (loc == null) {
                return;
            }
            World world = player.getWorld();
            
            // Folding fire wing effect
            for (int wing = 0; wing < 2; wing++) {
                double wingSide = wing == 0 ? -1 : 1;
                Vector wingBase = getWingBaseVector(player, wingSide);
                
                for (int i = 0; i < 20; i++) {
                    double foldProgress = i / 20.0;
                    Vector wingPoint = wingBase.clone().multiply(3.0 * (1 - foldProgress));
                    Location foldLoc = loc.clone().add(wingPoint).add(0, 0.5, 0);
                    
                    // Dying flames
                    world.spawnParticle(Particle.FLAME, foldLoc, 1, 0.15, 0.15, 0.15, 0.02);
                    
                    // Smoldering embers
                    if (Math.random() < 0.8) {
                        world.spawnParticle(Particle.LAVA, foldLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                    
                    // Fading fire
                    if (Math.random() < 0.5) {
                        int intensity = (int)(255 * (1 - foldProgress));
                        Color fadingFire = Color.fromRGB(intensity, intensity / 3, 0);
                        world.spawnParticle(Particle.DUST, foldLoc, 1, 0.08, 0.08, 0.08, 0,
                            new Particle.DustOptions(fadingFire, (float)(1.2 * (1 - foldProgress))));
                    }
                }
            }
            
            // Final fire burst
            for (int i = 0; i < 25; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 2.0;
                double height = Math.random() * 1.5;
                
                Location emberLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height + 1,
                    Math.sin(angle) * radius
                );
                
                world.spawnParticle(Particle.LAVA, emberLoc, 1, 0.1, 0.1, 0.1, 0);
                if (Math.random() < 0.4) {
                    world.spawnParticle(Particle.SMOKE, emberLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

        private void spawnAngelWings(@NotNull Location playerLoc) {
            // Calculate wing animation
            double flapCycle = Math.sin(wingPhase * flapIntensity) * 0.3 + 0.7; // 0.4 to 1.0 range
            double wingspan = cfgDouble("wings.wingspan", 3.0) * flapCycle;
            double wingHeight = cfgDouble("wings.height", 1.5);
            int particlesPerWing = cfgInt("wings.particles-per-wing", 25);

            final World world = playerLoc.getWorld();
            if (world == null) {
                return;
            }
            
            // Create both wings
            for (int wing = 0; wing < 2; wing++) {
                double wingSide = wing == 0 ? -1 : 1; // Left and right wing
                spawnWing(playerLoc, world, wingSide, wingspan, wingHeight, particlesPerWing);
            }
            
            // Divine aura around player
            if (tickCounter % 3 == 0) {
                spawnDivineAura(playerLoc, world);
            }
            
            // Occasional holy bursts
            if (tickCounter % 40 == 0) {
                spawnHolyBurst(playerLoc, world);
            }
        }
        
        private void spawnWing(@NotNull Location center, @NotNull World world, double side, double wingspan, double wingHeight, int particles) {
            Vector wingBase = getWingBaseVector(player, side);
            
            for (int i = 0; i < particles; i++) {
                double progress = (double) i / particles;
                
                // Wing shape calculation
                double wingX = progress * wingspan;
                double wingY = Math.sin(progress * Math.PI) * wingHeight; // Arched wing shape
                double wingZ = progress * 0.5; // Slight backward sweep
                
                // Add flapping motion
                double flapOffset = Math.sin(wingPhase * flapIntensity + progress * 2) * 0.2;
                wingY += flapOffset;

                Vector wingOffset = new Vector(wingX * wingBase.getX(), wingY, wingX * wingBase.getZ() + wingZ);
                Location wingLoc = center.clone().add(wingOffset).add(0, 0.5, 0);
                
                // Main wing particles - divine ethereal light
                world.spawnParticle(Particle.END_ROD, wingLoc, 1, 0.08, 0.08, 0.08, 0.02);
                
                // Soft cloud-like wing base
                if (Math.random() < 0.6) {
                    world.spawnParticle(Particle.CLOUD, wingLoc, 1, 0.08, 0.08, 0.08, 0.01);
                }
                
                // Wing outline - bright divine golden essence
                if (progress < 0.15 || progress > 0.85 || Math.random() < 0.4) {
                    Color divineGold = Color.fromRGB(255, 248, 220); // Cornsilk - divine
                    world.spawnParticle(Particle.DUST, wingLoc, 1, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(divineGold, 1.5f));
                }
                
                // Feather tips - heavenly light bursts
                if (progress > 0.7 && Math.random() < 0.5) {
                    world.spawnParticle(Particle.FIREWORK, wingLoc, 1, 0.1, 0.1, 0.1, 0.03);
                }
                
                // Divine sparkles
                if (Math.random() < 0.3) {
                    Color heavenlyWhite = Color.fromRGB(255, 255, 240); // Ivory - pure
                    world.spawnParticle(Particle.DUST, wingLoc, 1, 0.03, 0.03, 0.03, 0,
                        new Particle.DustOptions(heavenlyWhite, 1.2f));
                }
                
                // Floating feathers
                if (Math.random() < 0.25) {
                    world.spawnParticle(Particle.FALLING_DUST, wingLoc, 1, 0.05, 0.05, 0.05, 0);
                }
            }
        }
        
        private void spawnDivineAura(@NotNull Location center, @NotNull World world) {
            double auraRadius = cfgDouble("aura.radius", 1.2);
            int auraParticles = cfgInt("aura.particles", 10);
            
            for (int i = 0; i < auraParticles; i++) {
                double angle = 2 * Math.PI * i / auraParticles + wingPhase * 0.5;
                double x = Math.cos(angle) * auraRadius;
                double z = Math.sin(angle) * auraRadius;
                double y = Math.sin(wingPhase + angle * 2) * 0.4;
                
                Location auraLoc = center.clone().add(x, y + 1.0, z);
                
                // Sacred fire aura
                world.spawnParticle(Particle.FLAME, auraLoc, 1, 0.08, 0.08, 0.08, 0.02);
                
                // Golden fire essence
                if (Math.random() < 0.5) {
                    Color goldFire = Color.fromRGB(255, 215, 0);
                    world.spawnParticle(Particle.DUST, auraLoc, 1, 0.03, 0.03, 0.03, 0,
                        new Particle.DustOptions(goldFire, 1.0f));
                }
                
                // Sacred embers
                if (Math.random() < 0.3) {
                    world.spawnParticle(Particle.LAVA, auraLoc, 1, 0.04, 0.04, 0.04, 0);
                }
            }
        }
        
        private void spawnHolyBurst(@NotNull Location center, @NotNull World world) {
            // Radial burst of sacred fire
            for (int i = 0; i < 15; i++) {
                double angle = 2 * Math.PI * i / 15;
                double radius = 0.8 + Math.random() * 0.4;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                
                Location burstLoc = center.clone().add(x, 1.5, z);
                
                // Sacred fire burst
                world.spawnParticle(Particle.FLAME, burstLoc, 2, 0.1, 0.5, 0.1, 0.1);
                world.spawnParticle(Particle.LAVA, burstLoc, 1, 0.05, 0.3, 0.05, 0.05);
                
                // Golden fire sparkles
                if (Math.random() < 0.7) {
                    Color burstGold = Color.fromRGB(255, 200, 0);
                    world.spawnParticle(Particle.DUST, burstLoc, 2, 0.2, 0.3, 0.2, 0,
                        new Particle.DustOptions(burstGold, 1.3f));
                }
            }
        }
        
        private void spawnDivineTrail(@NotNull Location currentLoc) {
            if (lastLocation == null) return;
            
            final World world = currentLoc.getWorld();
            if (world == null) {
                return;
            }
            Vector movement = currentLoc.toVector().subtract(lastLocation.toVector());
            
            if (movement.lengthSquared() > 0.01) {
                Location trailStart = lastLocation.clone();
                int trailLength = cfgInt("trail.length", 15);
                
                for (int i = 0; i < trailLength; i++) {
                    double progress = (double) i / trailLength;
                    Location trailPoint = trailStart.clone().subtract(movement.clone().multiply(progress * 3));
                    
                    float alpha = (float) (1.0 - progress * 0.7);
                    if (Math.random() < alpha) {
                        // Sacred fire trail
                        world.spawnParticle(Particle.FLAME, trailPoint, 1, 0.2, 0.2, 0.2, 0.03);
                        
                        // Fire embers
                        if (Math.random() < 0.6) {
                            world.spawnParticle(Particle.LAVA, trailPoint, 1, 0.1, 0.1, 0.1, 0.01);
                        }
                        
                        // Golden fire essence
                        if (Math.random() < 0.4) {
                            int red = 255;
                            int green = (int)(215 * alpha + 40 * (1 - alpha)); // Golden to orange
                            int blue = (int)(50 * alpha); // Golden fade
                            Color trailColor = Color.fromRGB(red, green, blue);
                            world.spawnParticle(Particle.DUST, trailPoint, 1, 0.08, 0.08, 0.08, 0,
                                new Particle.DustOptions(trailColor, alpha * 1.2f));
                        }
                    }
                }
            }
        }
        
        private Vector getWingBaseVector(Player player, double side) {
            // Calculate perpendicular vector to player facing direction
            final Location location = player.getLocation();
            if (location == null) {
                return new Vector(side, 0, 0); // Return a default vector
            }
            Vector facing = location.getDirection();
            Vector right = facing.getCrossProduct(new Vector(0, 1, 0)).normalize();
            return right.multiply(side);
        }
        
        private void applyDivineEffects() {
            final Location location = player.getLocation();
            if (location == null) {
                return;
            }
            int duration = cfgInt("effects.duration-ticks", 60);
            
            // Slow falling protection
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false));
            
            // Night vision for divine sight
            if (cfgBool("effects.night-vision", true)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false));
            }
            
            // Regeneration in bright light
            int lightLevel = location.getBlock().getLightLevel();
            if (lightLevel >= cfgInt("light.regen-threshold", 12)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 
                    cfgInt("effects.regen-duration", 40), 0, false, false));
            }
            
            // Fire resistance - angels are immune to flames
            if (cfgBool("effects.fire-resistance", true)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false));
            }
        }
        
        private void removeEffects() {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        }
    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("angel-wings." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("angel-wings." + path, def);
    }
    
    private boolean cfgBool(String path, boolean def) {
        return spellConfig.getBoolean("angel-wings." + path, def);
    }
}
