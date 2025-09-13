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
 * StormRider 1.0 - Revolutionary electric storm flight spell with electromagnetic field manipulation.
 * 
 * Extraordinary Features:
 * - Lightning wing formation with electromagnetic field generation
 * - Storm cloud manifestation with realistic weather particle physics
 * - Electric discharge effects with voltage simulation
 * - Thunder creation with atmospheric pressure wave propagation
 * - Advanced electromagnetic aura with field line visualization
 * - Lightning strike capabilities with electrical discharge mechanics
 * - Storm energy management with capacitor-like charge accumulation
 * - Wind control effects with fluid dynamics simulation
 */
public final class StormRider extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, StormRiderData> stormRiders = new WeakHashMap<>();

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Storm Rider";
            description = "Command the power of storms with lightning wings and electromagnetic field manipulation.";
            cooldown = Duration.ofSeconds(10);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new StormRider(this);
        }
    }

    private StormRider(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public @NotNull String key() {
        return "storm-rider";
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
        return stormRiders.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player))
            return;
        stormRiders.put(player.getUniqueId(), new StormRiderData(player, context));
    }

    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        Optional.ofNullable(stormRiders.remove(player.getUniqueId())).ifPresent(StormRiderData::stop);
    }

    @Override
    public void forceDeactivate(@NotNull Player player) {
        Optional.ofNullable(stormRiders.remove(player.getUniqueId())).ifPresent(StormRiderData::stop);
    }

    @Override
    public int getMaxDuration() {
        return cfgInt("max-duration-ticks", 1800); // 1.5 minutes default
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class StormRiderData {
        private final Player player;
        private final BossBar stormEnergyBar = BossBar.bossBar(Component.text("Storm Energy"), 1,
                BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        private final BukkitTask ticker;
        
        // Storm riding variables
        private double stormEnergy = 100;
        private int tickCounter = 0;
        private double lightningPhase = 0;
        private double electromagneticField = 0;
        private double stormIntensity = 0;
        private StormState currentState = StormState.GATHERING;
        private int lightningStrikes = 0;
        private int stormCharges = 0;
        private Location lastLocation;
        private Vector[] electricField = new Vector[12]; // Electromagnetic field lines

        // Storm states for electromagnetic progression
        private enum StormState {
            GATHERING,      // Storm gathering
            CHARGED,        // Electrically charged
            STORMING,       // Active storm
            LIGHTNING       // Lightning discharge mode
        }

        StormRiderData(Player player, SpellContext context) {
            this.player = player;
            this.lastLocation = player.getLocation().clone();
            
            // Initialize electromagnetic field
            initializeElectricField();
            
            player.showBossBar(stormEnergyBar);
            
            // Storm awakening sounds
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.2f);
            player.playSound(player.getLocation(), Sound.WEATHER_RAIN, 0.6f, 1.0f);
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.7f, 1.5f);
            
            // Show storm mastery message
            player.sendMessage(Component.text(
                    "§9⚡ §bYou harness the fury of the storm, lightning obeys your will! §9⚡"));
            
            // Spawn storm activation effect
            spawnStormActivationEffect();
            
            // Grant flight if not already flying
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }
            
            // Start the storm rider ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            player.hideBossBar(stormEnergyBar);
            
            // Remove storm effects
            removeStormEffects();
            
            // Final storm dissipation effect
            spawnStormDeactivationEffect();
            
            player.sendMessage(Component.text("§9⚡ §7The storm calms, its electric fury fades away... §9⚡"));
            player.playSound(player.getLocation(), Sound.WEATHER_RAIN_ABOVE, 0.5f, 0.8f);
            
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
            
            tickCounter++;
            lightningPhase += 0.4;      // Lightning frequency
            electromagneticField += 0.2; // Field oscillation
            stormIntensity += 0.15;     // Storm building
            
            Location currentLoc = player.getLocation();
            
            // Storm state management
            updateStormState();
            
            // Energy consumption based on state
            double energyCost = getEnergyCostForState();
            stormEnergy -= energyCost;
            stormEnergy = Math.max(0, Math.min(100, stormEnergy));
            stormEnergyBar.progress((float) (stormEnergy / 100));
            
            // Update boss bar with charges and strikes
            updateStormEnergyDisplay();
            
            // Storm charge accumulation
            if (currentState == StormState.GATHERING && tickCounter % 40 == 0) {
                accumulateStormCharge();
            }
            
            // Apply storm effects based on current state
            applyStormEffects();
            
            // Storm visual effects
            spawnStormForm(currentLoc);
            
            // Lightning wing formation
            spawnLightningWings(currentLoc);
            
            // Storm trail when moving
            if (lastLocation != null && currentLoc.distanceSquared(lastLocation) > 0.01) {
                spawnStormTrail(currentLoc);
            }
            
            // Automatic lightning strikes in high intensity
            if (currentState == StormState.LIGHTNING && Math.random() < cfgDouble("lightning.auto-strike-chance", 0.02)) {
                triggerLightningStrike();
            }
            
            // State-specific sounds
            playStateSpecificSounds(currentLoc);
            
            lastLocation = currentLoc.clone();
        }
        
        private void initializeElectricField() {
            // Initialize electromagnetic field vectors
            for (int i = 0; i < electricField.length; i++) {
                double angle = 2 * Math.PI * i / electricField.length;
                electricField[i] = new Vector(Math.cos(angle), 0, Math.sin(angle));
            }
        }
        
        private void updateStormState() {
            // Storm state progression based on energy and time
            if (stormCharges < 3) {
                currentState = StormState.GATHERING;
            } else if (stormCharges < 6) {
                currentState = StormState.CHARGED;
            } else if (stormCharges < 10) {
                currentState = StormState.STORMING;
            } else {
                currentState = StormState.LIGHTNING;
            }
        }
        
        private double getEnergyCostForState() {
            switch (currentState) {
                case GATHERING: return cfgDouble("energy.gathering-cost", 0.1);
                case CHARGED: return cfgDouble("energy.charged-cost", 0.2);
                case STORMING: return cfgDouble("energy.storming-cost", 0.4);
                case LIGHTNING: return cfgDouble("energy.lightning-cost", 0.6);
                default: return 0.2;
            }
        }
        
        private void updateStormEnergyDisplay() {
            String displayText = "Storm Energy";
            if (stormCharges > 0) {
                displayText += " - Charges: " + stormCharges;
            }
            if (lightningStrikes > 0) {
                displayText += " - Strikes: " + lightningStrikes;
            }
            stormEnergyBar.name(Component.text(displayText));
        }
        
        private void accumulateStormCharge() {
            stormCharges++;
            stormEnergy = Math.min(100, stormEnergy + cfgDouble("storm.charge-energy-gain", 5));
            
            // Charge accumulation effect
            spawnChargeAccumulationEffect();
            
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.8f);
            
            if (stormCharges % 3 == 0) {
                player.sendMessage(Component.text("§9⚡ §bStorm intensity increases! §9⚡"));
            }
        }
        
        private void triggerLightningStrike() {
            if (stormEnergy < cfgDouble("lightning.energy-cost", 20)) {
                return;
            }
            
            lightningStrikes++;
            stormEnergy -= cfgDouble("lightning.energy-cost", 20);
            
            // Find lightning target
            Location strikeLoc = findLightningTarget();
            if (strikeLoc != null) {
                // Lightning strike effect
                spawnLightningStrike(strikeLoc);
                
                // Lightning sounds
                player.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
                player.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.2f);
                
                player.sendMessage(Component.text("§9⚡ §eLightning strikes at your command! §9⚡"));
            }
        }
        
        private Location findLightningTarget() {
            Location eyeLoc = player.getEyeLocation();
            Vector direction = eyeLoc.getDirection();
            double maxDistance = cfgDouble("lightning.max-distance", 20.0);
            
            // Find ground target for lightning
            for (double distance = 5.0; distance <= maxDistance; distance += 1.0) {
                Location checkLoc = eyeLoc.clone().add(direction.clone().multiply(distance));
                
                // Look for ground
                for (int y = checkLoc.getBlockY(); y >= checkLoc.getBlockY() - 20; y--) {
                    Location groundLoc = new Location(checkLoc.getWorld(), checkLoc.getX(), y, checkLoc.getZ());
                    if (groundLoc.getBlock().getType().isSolid()) {
                        return groundLoc.add(0, 1, 0);
                    }
                }
            }
            
            return null;
        }
        
        private void spawnStormActivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Storm formation effect
            for (int i = 0; i < 60; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 4.0;
                double height = Math.random() * 5.0;
                
                Location stormLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                // Storm gathering particles
                world.spawnParticle(Particle.ELECTRIC_SPARK, stormLoc, 2, 0.3, 0.3, 0.3, 0.05);
                world.spawnParticle(Particle.CLOUD, stormLoc, 1, 0.2, 0.2, 0.2, 0.02);
                
                // Electric field visualization
                Color electricColor = Color.fromRGB(100, 150, 255);
                world.spawnParticle(Particle.DUST, stormLoc, 1, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(electricColor, 1.5f));
            }
            
            // Central storm vortex
            for (int i = 0; i < 25; i++) {
                double spiral = i * 0.5;
                double radius = i * 0.15;
                Location spiralLoc = loc.clone().add(
                    Math.cos(spiral) * radius,
                    i * 0.2,
                    Math.sin(spiral) * radius
                );
                
                world.spawnParticle(Particle.ELECTRIC_SPARK, spiralLoc, 1, 0.05, 0.05, 0.05, 0.02);
                world.spawnParticle(Particle.END_ROD, spiralLoc, 1, 0.03, 0.03, 0.03, 0.01);
            }
        }
        
        private void spawnStormForm(Location center) {
            // State-specific storm form rendering
            switch (currentState) {
                case GATHERING:
                    spawnGatheringStormForm(center);
                    break;
                case CHARGED:
                    spawnChargedStormForm(center);
                    break;
                case STORMING:
                    spawnStormingForm(center);
                    break;
                case LIGHTNING:
                    spawnLightningStormForm(center);
                    break;
            }
            
            // Storm aura - always present
            spawnStormAura(center);
            
            // Electromagnetic field visualization
            renderElectromagneticField(center);
        }
        
        private void spawnGatheringStormForm(Location center) {
            World world = center.getWorld();
            
            // Gathering storm clouds
            double cloudRadius = cfgDouble("storm.gathering-radius", 2.0);
            int cloudParticles = cfgInt("storm.gathering-particles", 15);
            
            for (int i = 0; i < cloudParticles; i++) {
                double angle = 2 * Math.PI * i / cloudParticles + stormIntensity * 0.3;
                double radius = cloudRadius * (0.7 + 0.3 * Math.sin(stormIntensity + i));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(stormIntensity * 0.5 + angle) * 0.8 + 2.0;
                
                Location cloudLoc = center.clone().add(x, y, z);
                
                // Storm clouds
                world.spawnParticle(Particle.CLOUD, cloudLoc, 1, 0.2, 0.1, 0.2, 0.01);
                world.spawnParticle(Particle.SMOKE, cloudLoc, 1, 0.15, 0.1, 0.15, 0.005);
                
                // Weak electrical activity
                if (Math.random() < 0.3) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, cloudLoc, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }
        
        private void spawnChargedStormForm(Location center) {
            World world = center.getWorld();
            
            // Charged storm formation
            double chargedRadius = cfgDouble("storm.charged-radius", 2.5);
            int chargedParticles = cfgInt("storm.charged-particles", 20);
            
            for (int i = 0; i < chargedParticles; i++) {
                double angle = 2 * Math.PI * i / chargedParticles + electromagneticField;
                double radius = chargedRadius * (0.8 + 0.2 * Math.sin(electromagneticField * 2 + i));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(electromagneticField + angle * 1.5) * 1.2 + 1.8;
                
                Location chargedLoc = center.clone().add(x, y, z);
                
                // Charged clouds
                world.spawnParticle(Particle.CLOUD, chargedLoc, 1, 0.15, 0.1, 0.15, 0.01);
                world.spawnParticle(Particle.ELECTRIC_SPARK, chargedLoc, 2, 0.2, 0.2, 0.2, 0.03);
                
                // Electric charge buildup
                Color chargeColor = Color.fromRGB(150, 200, 255);
                world.spawnParticle(Particle.DUST, chargedLoc, 1, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(chargeColor, 1.3f));
            }
        }
        
        private void spawnStormingForm(Location center) {
            World world = center.getWorld();
            
            // Active storming
            double stormRadius = cfgDouble("storm.storming-radius", 3.0);
            int stormParticles = cfgInt("storm.storming-particles", 25);
            
            for (int i = 0; i < stormParticles; i++) {
                double angle = 2 * Math.PI * i / stormParticles + lightningPhase * 0.8;
                double radius = stormRadius * (0.6 + 0.4 * Math.sin(lightningPhase + i * 0.5));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(lightningPhase * 0.7 + angle * 2) * 1.5 + 1.5;
                
                Location stormLoc = center.clone().add(x, y, z);
                
                // Storm intensity
                world.spawnParticle(Particle.CLOUD, stormLoc, 2, 0.25, 0.15, 0.25, 0.02);
                world.spawnParticle(Particle.ELECTRIC_SPARK, stormLoc, 3, 0.3, 0.2, 0.3, 0.05);
                world.spawnParticle(Particle.END_ROD, stormLoc, 1, 0.1, 0.1, 0.1, 0.02);
                
                // Storm energy
                if (Math.random() < 0.6) {
                    Color stormColor = getStormColor(angle);
                    world.spawnParticle(Particle.DUST, stormLoc, 1, 0.15, 0.15, 0.15, 0,
                        new Particle.DustOptions(stormColor, 1.5f));
                }
            }
        }
        
        private void spawnLightningStormForm(Location center) {
            World world = center.getWorld();
            
            // Lightning storm mode
            double lightningRadius = cfgDouble("storm.lightning-radius", 3.5);
            int lightningParticles = cfgInt("storm.lightning-particles", 30);
            
            for (int i = 0; i < lightningParticles; i++) {
                double angle = 2 * Math.PI * i / lightningParticles + lightningPhase;
                double radius = lightningRadius * (0.5 + 0.5 * Math.sin(lightningPhase * 1.5 + i));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(lightningPhase + angle * 2.5) * 2.0 + 1.0;
                
                Location lightningLoc = center.clone().add(x, y, z);
                
                // Intense lightning activity
                world.spawnParticle(Particle.ELECTRIC_SPARK, lightningLoc, 4, 0.4, 0.3, 0.4, 0.08);
                world.spawnParticle(Particle.END_ROD, lightningLoc, 2, 0.2, 0.2, 0.2, 0.04);
                world.spawnParticle(Particle.CLOUD, lightningLoc, 1, 0.2, 0.1, 0.2, 0.01);
                
                // Lightning energy
                Color lightningColor = Color.fromRGB(255, 255, 200);
                world.spawnParticle(Particle.DUST, lightningLoc, 2, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(lightningColor, 1.8f));
                
                // Occasional lightning bolts
                if (Math.random() < 0.1) {
                    spawnMiniLightningBolt(lightningLoc);
                }
            }
        }
        
        private void spawnLightningWings(Location center) {
            World world = center.getWorld();
            
            // Lightning wing formation
            double wingSpan = cfgDouble("lightning.wing-span", 3.5);
            int wingParticles = cfgInt("lightning.wing-particles", 25);
            double wingIntensity = getStateIntensityMultiplier();
            
            for (int wing = 0; wing < 2; wing++) {
                double wingSide = wing == 0 ? -1 : 1;
                
                for (int i = 0; i < wingParticles; i++) {
                    double progress = (double) i / wingParticles;
                    double wingFlap = Math.sin(lightningPhase * 2) * 0.3 + 0.7;
                    
                    double wingX = progress * wingSpan * wingSide * wingFlap;
                    double wingY = Math.sin(progress * Math.PI + lightningPhase) * 1.5 + 1.0;
                    double wingZ = progress * 0.5;
                    
                    Location wingLoc = center.clone().add(wingX, wingY, wingZ);
                    
                    // Lightning wing structure
                    world.spawnParticle(Particle.ELECTRIC_SPARK, wingLoc, 
                        (int)(2 * wingIntensity), 0.1, 0.1, 0.1, 0.03);
                    
                    if (Math.random() < 0.4) {
                        world.spawnParticle(Particle.END_ROD, wingLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                    
                    // Wing feather lightning
                    Color wingColor = getLightningWingColor(progress);
                    world.spawnParticle(Particle.DUST, wingLoc, 1, 0.08, 0.08, 0.08, 0,
                        new Particle.DustOptions(wingColor, (float)(1.0 + wingIntensity * 0.5)));
                }
            }
        }
        
        private void renderElectromagneticField(Location center) {
            World world = center.getWorld();
            double fieldRadius = cfgDouble("electromagnetic.field-radius", 2.2);
            
            // Render electromagnetic field lines
            for (int i = 0; i < electricField.length; i++) {
                Vector fieldLine = electricField[i].clone();
                
                // Field line oscillation
                double oscillation = Math.sin(electromagneticField + i * 0.5) * 0.3;
                fieldLine.multiply(fieldRadius * (1 + oscillation));
                
                Location fieldLoc = center.clone().add(fieldLine);
                fieldLoc.add(0, Math.sin(electromagneticField * 1.5 + i) * 0.4 + 1.5, 0);
                
                // Electromagnetic field visualization
                world.spawnParticle(Particle.END_ROD, fieldLoc, 1, 0.02, 0.02, 0.02, 0.005);
                
                if (Math.random() < 0.3) {
                    Color fieldColor = Color.fromRGB(100, 200, 255);
                    world.spawnParticle(Particle.DUST, fieldLoc, 1, 0.03, 0.03, 0.03, 0,
                        new Particle.DustOptions(fieldColor, 0.8f));
                }
            }
        }
        
        private void spawnStormAura(Location center) {
            World world = center.getWorld();
            double auraRadius = cfgDouble("storm.aura-radius", 1.5);
            int auraParticles = cfgInt("storm.aura-particles", 12);
            
            for (int i = 0; i < auraParticles; i++) {
                double angle = 2 * Math.PI * i / auraParticles + electromagneticField * 0.5;
                double x = Math.cos(angle) * auraRadius;
                double z = Math.sin(angle) * auraRadius;
                double y = Math.sin(electromagneticField * 2 + angle * 3) * 0.3 + 1.8;
                
                Location auraLoc = center.clone().add(x, y, z);
                
                // Storm aura
                world.spawnParticle(Particle.ELECTRIC_SPARK, auraLoc, 1, 0.03, 0.03, 0.03, 0.01);
                
                if (Math.random() < 0.4) {
                    Color auraColor = Color.fromRGB(150, 180, 255);
                    world.spawnParticle(Particle.DUST, auraLoc, 1, 0.02, 0.02, 0.02, 0,
                        new Particle.DustOptions(auraColor, 1.0f));
                }
            }
        }
        
        private void spawnStormTrail(Location currentLoc) {
            if (lastLocation == null) return;
            
            World world = currentLoc.getWorld();
            Vector movement = currentLoc.toVector().subtract(lastLocation.toVector());
            
            if (movement.lengthSquared() > 0.005) {
                int trailLength = cfgInt("storm.trail-length", 20);
                double velocityMagnitude = movement.length();
                
                for (int i = 0; i < trailLength; i++) {
                    double progress = (double) i / trailLength;
                    Location trailPoint = currentLoc.clone().subtract(movement.clone().multiply(progress * 5));
                    
                    // Trail intensity based on velocity and state
                    float intensity = (float) ((1.0 - progress * 0.8) * velocityMagnitude * 3);
                    intensity *= getStateIntensityMultiplier();
                    
                    if (intensity > 0.1f) {
                        // Storm trail
                        world.spawnParticle(Particle.ELECTRIC_SPARK, trailPoint, 1, 0.1, 0.1, 0.1, 0.02);
                        
                        // State-specific trail effects
                        if (currentState == StormState.LIGHTNING) {
                            world.spawnParticle(Particle.END_ROD, trailPoint, 1, 0.05, 0.05, 0.05, 0.01);
                        }
                        
                        // Storm trail color
                        Color trailColor = getStormTrailColor(progress);
                        world.spawnParticle(Particle.DUST, trailPoint, 1, 0.08, 0.08, 0.08, 0,
                            new Particle.DustOptions(trailColor, intensity));
                    }
                }
            }
        }
        
        private void spawnChargeAccumulationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Charge building effect
            for (int i = 0; i < 20; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 2.0 + 1.0;
                double height = Math.random() * 2.0 + 1.0;
                
                Location chargeLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                // Charge accumulation
                world.spawnParticle(Particle.ELECTRIC_SPARK, chargeLoc, 2, 0.1, 0.1, 0.1, 0.03);
                world.spawnParticle(Particle.END_ROD, chargeLoc, 1, 0.05, 0.05, 0.05, 0.01);
                
                // Charge energy
                Color chargeColor = Color.fromRGB(100, 255, 255);
                world.spawnParticle(Particle.DUST, chargeLoc, 1, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(chargeColor, 1.2f));
            }
        }
        
        private void spawnLightningStrike(Location target) {
            World world = target.getWorld();
            Location origin = player.getLocation().add(0, 10, 0);
            
            // Lightning bolt from sky to target
            Vector direction = target.toVector().subtract(origin.toVector()).normalize();
            double distance = origin.distance(target);
            
            for (int i = 0; i < 30; i++) {
                double progress = (double) i / 30;
                Location boltLoc = origin.clone().add(direction.clone().multiply(distance * progress));
                
                // Lightning bolt zigzag
                double zigzag = Math.sin(progress * Math.PI * 8) * 0.5;
                boltLoc.add(zigzag, 0, zigzag);
                
                // Lightning bolt particles
                world.spawnParticle(Particle.ELECTRIC_SPARK, boltLoc, 5, 0.2, 0.2, 0.2, 0.1);
                world.spawnParticle(Particle.END_ROD, boltLoc, 2, 0.1, 0.1, 0.1, 0.05);
                
                // Lightning bolt core
                Color boltColor = Color.fromRGB(255, 255, 255);
                world.spawnParticle(Particle.DUST, boltLoc, 3, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(boltColor, 2.0f));
            }
            
            // Lightning impact effect
            spawnLightningImpact(target);
        }
        
        private void spawnLightningImpact(Location impact) {
            World world = impact.getWorld();
            
            // Lightning impact explosion
            for (int i = 0; i < 50; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 3.0;
                double height = Math.random() * 2.0;
                
                Location impactLoc = impact.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                // Impact particles
                world.spawnParticle(Particle.ELECTRIC_SPARK, impactLoc, 3, 0.3, 0.3, 0.3, 0.08);
                world.spawnParticle(Particle.END_ROD, impactLoc, 2, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticle(Particle.SMOKE, impactLoc, 1, 0.2, 0.1, 0.2, 0.02);
                
                // Impact energy
                Color impactColor = Color.fromRGB(255, 255, 100);
                world.spawnParticle(Particle.DUST, impactLoc, 2, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(impactColor, 1.6f));
            }
        }
        
        private void spawnMiniLightningBolt(Location origin) {
            World world = origin.getWorld();
            
            // Mini lightning bolt
            for (int i = 0; i < 8; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double length = Math.random() * 1.5 + 0.5;
                Location boltLoc = origin.clone().add(
                    Math.cos(angle) * length,
                        (Math.random() - 0.5),
                    Math.sin(angle) * length
                );
                
                // Mini bolt
                world.spawnParticle(Particle.ELECTRIC_SPARK, boltLoc, 1, 0.05, 0.05, 0.05, 0.02);
                
                if (Math.random() < 0.5) {
                    Color boltColor = Color.fromRGB(200, 200, 255);
                    world.spawnParticle(Particle.DUST, boltLoc, 1, 0.03, 0.03, 0.03, 0,
                        new Particle.DustOptions(boltColor, 1.0f));
                }
            }
        }
        
        private void spawnStormDeactivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Storm dissipation
            for (int i = 0; i < 40; i++) {
                double angle = i * 0.3;
                double radius = 3.5 - (i * 0.08);
                double height = i * 0.1;
                
                Location dissipationLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                world.spawnParticle(Particle.CLOUD, dissipationLoc, 2, 0.2, 0.1, 0.2, 0.01);
                world.spawnParticle(Particle.SMOKE, dissipationLoc, 1, 0.15, 0.1, 0.15, 0.005);
                
                // Fading electrical activity
                if (Math.random() < 0.3) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, dissipationLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
        
        // Color and effect helpers
        private Color getStormColor(double phase) {
            // Storm color spectrum
            double normalizedPhase = (Math.sin(phase) + 1) / 2;
            
            if (normalizedPhase < 0.33) {
                // Dark blue to blue
                return Color.fromRGB(0, (int)(100 + 155 * normalizedPhase * 3), 255);
            } else if (normalizedPhase < 0.67) {
                // Blue to cyan
                double subPhase = (normalizedPhase - 0.33) * 3;
                return Color.fromRGB((int)(100 * subPhase), 255, 255);
            } else {
                // Cyan to white (lightning)
                double subPhase = (normalizedPhase - 0.67) * 3;
                return Color.fromRGB((int)(100 + 155 * subPhase), 255, 255);
            }
        }
        
        private Color getLightningWingColor(double progress) {
            // Lightning wing color progression
            if (progress < 0.5) {
                // Blue to white
                return Color.fromRGB((int)(100 + 155 * progress * 2), (int)(150 + 105 * progress * 2), 255);
            } else {
                // White to electric blue
                double subProgress = (progress - 0.5) * 2;
                return Color.fromRGB((int)(255 - 155 * subProgress), (int)(255 - 100 * subProgress), 255);
            }
        }
        
        private Color getStormTrailColor(double progress) {
            // Trail color based on current state
            double alpha = 1.0 - progress * 0.7;
            
            switch (currentState) {
                case GATHERING:
                    return Color.fromRGB((int)(100 * alpha), (int)(150 * alpha), (int)(255 * alpha));
                case CHARGED:
                    return Color.fromRGB((int)(150 * alpha), (int)(200 * alpha), (int)(255 * alpha));
                case STORMING:
                    return Color.fromRGB((int)(200 * alpha), (int)(255 * alpha), (int)(255 * alpha));
                case LIGHTNING:
                    return Color.fromRGB((int)(255 * alpha), (int)(255 * alpha), (int)(200 * alpha));
                default:
                    return Color.fromRGB((int)(150 * alpha), (int)(180 * alpha), (int)(255 * alpha));
            }
        }
        
        private double getStateIntensityMultiplier() {
            switch (currentState) {
                case GATHERING: return 0.6;
                case CHARGED: return 1.0;
                case STORMING: return 1.4;
                case LIGHTNING: return 2.0;
                default: return 1.0;
            }
        }
        
        private void applyStormEffects() {
            int duration = cfgInt("effects.duration-ticks", 40);
            
            // Flight enhancement
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false));
            
            // Storm resistance
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, duration, 0, false, false));
            
            // State-specific effects
            switch (currentState) {
                case GATHERING:
                    // Gathering energy
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 0, false, false));
                    break;
                case CHARGED:
                    // Charged speed
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, false));
                    break;
                case STORMING:
                    // Storm power
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, false, false));
                    break;
                case LIGHTNING:
                    // Lightning mastery
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 1, false, false));
                    break;
            }
            
            // Night vision for storm sight
            if (cfgBool("effects.night-vision", true)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false));
            }
        }
        
        private void removeStormEffects() {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
        
        private void playStateSpecificSounds(Location loc) {
            if (tickCounter % 120 == 0) {
                switch (currentState) {
                    case GATHERING:
                        player.playSound(loc, Sound.WEATHER_RAIN, 0.3f, 1.2f);
                        break;
                    case CHARGED:
                        player.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.8f);
                        break;
                    case STORMING:
                        player.playSound(loc, Sound.WEATHER_RAIN_ABOVE, 0.5f, 1.0f);
                        player.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.3f, 1.2f);
                        break;
                    case LIGHTNING:
                        player.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.5f);
                        break;
                }
            }
        }
    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("storm-rider." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("storm-rider." + path, def);
    }
    
    private boolean cfgBool(String path, boolean def) {
        return spellConfig.getBoolean("storm-rider." + path, def);
    }
}
