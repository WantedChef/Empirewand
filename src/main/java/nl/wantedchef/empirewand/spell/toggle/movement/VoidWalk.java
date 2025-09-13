package nl.wantedchef.empirewand.spell.toggle.movement;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
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
 * VoidWalk 1.0 - Revolutionary quantum teleportation movement spell with reality distortion mechanics.
 * Extraordinary Features:
 * - Quantum void tunneling with instantaneous teleportation
 * - Reality distortion field with dimensional rift generation
 * - Void particle portals with mathematical wormhole physics
 * - Phase-shifting locomotion with temporal displacement effects
 * - Advanced void energy management with entropy calculations
 * - Quantum uncertainty particle rendering with probability clouds
 * - Portal visualization with realistic event horizon simulation
 * - Multi-dimensional travel with space-time curvature effects
 */
public final class VoidWalk extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* CONSTANTS */
    /* ---------------------------------------- */
    private static final String VOID_RESISTANCE_PATH = "effects.void-resistance";
    private static final boolean VOID_RESISTANCE_DEFAULT = true;

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, VoidWalkerData> voidWalkers = new WeakHashMap<>();

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Void Walk";
            description = "Master quantum teleportation through void tunnels with reality distortion and dimensional rifts.";
            cooldown = Duration.ofSeconds(8);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new VoidWalk(this);
        }
    }

    private VoidWalk(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public @NotNull String key() {
        return "void-walk";
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
        return voidWalkers.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player))
            return;
        voidWalkers.put(player.getUniqueId(), new VoidWalkerData(player, context));
    }

    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        Optional.ofNullable(voidWalkers.remove(player.getUniqueId())).ifPresent(VoidWalkerData::stop);
    }

    @Override
    public void forceDeactivate(@NotNull Player player) {
        Optional.ofNullable(voidWalkers.remove(player.getUniqueId())).ifPresent(VoidWalkerData::stop);
    }

    @Override
    public int getMaxDuration() {
        return cfgInt("max-duration-ticks", 2400); // 2 minutes default
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class VoidWalkerData {
        private final Player player;
        private final BossBar voidEnergyBar = BossBar.bossBar(Component.text("Void Energy"), 1,
                BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        private final BukkitTask ticker;
        
        // Void walking variables
        private double voidEnergy = 100;
        private int tickCounter = 0;
        private double quantumPhase = 0;
        private double realityDistortion = 0;
        private double dimensionalRift = 0;
        private VoidState currentState = VoidState.PHASING;
        private final Location[] voidPortals = new Location[5];
        private int teleportations = 0;
        private int phaseCooldown = 0;
        private Location lastStableLocation;

        // Void states for quantum mechanics
        private enum VoidState {
            PHASING,        // Normal void phasing
            TUNNELING,      // Active void tunneling
            RIFTING,        // Creating dimensional rifts
            WARPING         // Reality warping mode
        }

        VoidWalkerData(Player player, SpellContext context) {
            this.player = player;
            this.lastStableLocation = player.getLocation().clone();
            
            player.showBossBar(voidEnergyBar);
            
            // Void awakening sounds
            player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.6f, 1.5f);
            
            // Show void mastery message
            player.sendMessage(Component.text(
                    "§5⚫ §dYou step into the void, reality bends to your will! §5⚫"));
            
            // Spawn void activation effect
            spawnVoidActivationEffect();
            
            // Start the void walker ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            player.hideBossBar(voidEnergyBar);
            
            // Remove void effects
            removeVoidEffects();
            
            // Final void dissipation effect
            spawnVoidDeactivationEffect();
            
            player.sendMessage(Component.text("§5⚫ §7The void releases its hold, reality returns to normal... §5⚫"));
            player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.0f);
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }
            
            tickCounter++;
            quantumPhase += 0.25;  // Quantum fluctuations
            realityDistortion += 0.15; // Reality distortion progression
            dimensionalRift += 0.1;    // Dimensional rift expansion
            
            Location currentLoc = player.getLocation();
            
            // Void state management
            updateVoidState();
            
            // Energy consumption based on state
            double energyCost = getEnergyCostForState();
            voidEnergy -= energyCost;
            voidEnergy = Math.max(0, Math.min(100, voidEnergy));
            voidEnergyBar.progress((float) (voidEnergy / 100));
            
            // Update boss bar with teleportations
            if (teleportations > 0) {
                voidEnergyBar.name(Component.text("Void Energy - Teleports: " + teleportations));
            }
            
            // Phase cooldown management
            if (phaseCooldown > 0) {
                phaseCooldown--;
            }
            
            // Check for void energy depletion
            if (voidEnergy <= 10) {
                triggerEmergencyStabilization();
            }
            
            // Apply void effects based on current state
            applyVoidEffects();
            
            // Void visual effects
            spawnVoidForm(currentLoc);
            
            // Generate void portals
            if (tickCounter % cfgInt("portal.generation-interval", 60) == 0) {
                generateVoidPortals(currentLoc);
            }
            
            // Detect teleportation attempts
            handleTeleportationInput();
            
            // State-specific sounds
            playStateSpecificSounds(currentLoc);
        }
        
        private void playStateSpecificSounds(Location currentLoc) {
            // State-specific ambient sounds
            switch (currentState) {
                case PHASING -> {
                    // Gentle void phasing sounds
                    if (tickCounter % 80 == 0) {
                        player.playSound(currentLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.5f);
                        player.playSound(currentLoc, Sound.AMBIENT_CAVE, 0.2f, 1.8f);
                    }
                }
                case TUNNELING -> {
                    // Intense void tunneling sounds
                    if (tickCounter % 60 == 0) {
                        player.playSound(currentLoc, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.4f, 0.8f);
                        player.playSound(currentLoc, Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 0.7f);
                    }
                }
                case RIFTING -> {
                    // Reality tearing rift sounds
                    if (tickCounter % 100 == 0) {
                        player.playSound(currentLoc, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 0.5f);
                        player.playSound(currentLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.2f, 0.3f);
                    }
                }
                case WARPING -> {
                    // Chaotic reality warping sounds
                    if (tickCounter % 40 == 0) {
                        player.playSound(currentLoc, Sound.ENTITY_WITHER_SPAWN, 0.3f, 1.2f);
                        player.playSound(currentLoc, Sound.BLOCK_END_GATEWAY_SPAWN, 0.4f, 0.6f);
                    }
                }
            }
        }
        
        private void updateVoidState() {
            // Void state cycle progression
            int cycleDuration = cfgInt("void.cycle-duration", 300); // ticks per cycle
            int stateProgress = tickCounter % cycleDuration;
            int stateQuarter = cycleDuration / 4;
            
            if (stateProgress < stateQuarter) {
                currentState = VoidState.PHASING;
            } else if (stateProgress < stateQuarter * 2) {
                currentState = VoidState.TUNNELING;
            } else if (stateProgress < stateQuarter * 3) {
                currentState = VoidState.RIFTING;
            } else {
                currentState = VoidState.WARPING;
            }
        }
        
        private double getEnergyCostForState() {
            return switch (currentState) {
                case PHASING -> cfgDouble("energy.phasing-cost", 0.15);
                case TUNNELING -> cfgDouble("energy.tunneling-cost", 0.25);
                case RIFTING -> cfgDouble("energy.rifting-cost", 0.4);
                case WARPING -> cfgDouble("energy.warping-cost", 0.6);
            };
        }
        
        private void triggerEmergencyStabilization() {
            voidEnergy = 30; // Emergency reserve
            
            // Emergency teleport to last stable location
            if (lastStableLocation != null) {
                performVoidTeleport(lastStableLocation, true);
                player.sendMessage(Component.text("§5⚫ §cVoid energy depleted! Emergency stabilization activated! §5⚫"));
            }
        }
        
        private void handleTeleportationInput() {
            if (phaseCooldown > 0 || voidEnergy < cfgDouble("teleport.energy-cost", 15)) {
                return;
            }
            
            // Check if player is sneaking for teleportation
            if (player.isSneaking()) {
                Location targetLoc = getVoidTeleportTarget();
                if (targetLoc != null) {
                    performVoidTeleport(targetLoc, false);
                }
            }
        }
        
        private Location getVoidTeleportTarget() {
            Location eyeLoc = player.getEyeLocation();
            Vector direction = eyeLoc.getDirection();
            double maxDistance = cfgDouble("teleport.max-distance", 15.0);
            
            // Raycast through void for teleport target
            for (double distance = 2.0; distance <= maxDistance; distance += 0.5) {
                Location checkLoc = eyeLoc.clone().add(direction.clone().multiply(distance));
                Block block = checkLoc.getBlock();
                
                // Find safe landing spot
                if (!block.getType().isSolid()) {
                    Location groundLoc = findSafeGround(checkLoc);
                    if (groundLoc != null) {
                        return groundLoc;
                    }
                }
            }
            
            return null;
        }
        
        private Location findSafeGround(Location loc) {
            World world = loc.getWorld();
            
            // Look down for solid ground
            for (int y = loc.getBlockY(); y >= loc.getBlockY() - 10; y--) {
                Location checkLoc = new Location(world, loc.getX(), y, loc.getZ());
                Block block = checkLoc.getBlock();
                Block above = world.getBlockAt(checkLoc.getBlockX(), y + 1, checkLoc.getBlockZ());
                Block above2 = world.getBlockAt(checkLoc.getBlockX(), y + 2, checkLoc.getBlockZ());
                
                if (block.getType().isSolid() && !above.getType().isSolid() && !above2.getType().isSolid()) {
                    return checkLoc.add(0.5, 1, 0.5);
                }
            }
            
            return null;
        }
        
        private void performVoidTeleport(Location target, boolean isEmergency) {
            Location origin = player.getLocation();
            
            // Pre-teleport void rift
            spawnVoidRift(origin, target);
            
            // Energy cost
            double energyCost = isEmergency ? 0 : cfgDouble("teleport.energy-cost", 15);
            voidEnergy -= energyCost;
            
            // Teleport with void effects
            player.teleport(target);
            teleportations++;
            phaseCooldown = cfgInt("teleport.cooldown-ticks", 30);
            
            if (!isEmergency) {
                lastStableLocation = target.clone();
            }
            
            // Post-teleport effects
            spawnVoidEmergence(target);
            
            // Teleport sounds
            player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            player.playSound(target, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.8f, 0.6f);
            
            if (!isEmergency) {
                player.sendMessage(Component.text("§5⚫ §dVoid tunneling successful! §5⚫"));
            }
        }
        
        private void generateVoidPortals(Location center) {
            double portalRadius = cfgDouble("portal.radius", 8.0);
            
            // Generate portal positions around player
            for (int i = 0; i < voidPortals.length; i++) {
                double angle = 2 * Math.PI * i / voidPortals.length + quantumPhase * 0.3;
                double x = Math.cos(angle) * portalRadius;
                double z = Math.sin(angle) * portalRadius;
                double y = Math.sin(quantumPhase + angle * 2) * 2.0 + 2.0;
                
                voidPortals[i] = center.clone().add(x, y, z);
            }
        }
        
        private void spawnVoidActivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();
            
            // Void opening effect
            for (int i = 0; i < 80; i++) {
                Location voidLoc = generateRandomVoidLocation(loc, 4.0, 3.0);
                
                // Void particles
                world.spawnParticle(Particle.END_ROD, voidLoc, 1, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.PORTAL, voidLoc, 2, 0.2, 0.2, 0.2, 0.5);
                
                // Dark void energy
                Color voidColor = Color.fromRGB(64, 0, 128);
                world.spawnParticle(Particle.DUST, voidLoc, 1, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(voidColor, 1.5f));
            }
            
            // Central void vortex
            for (int i = 0; i < 30; i++) {
                double spiral = i * 0.3;
                double radius = i * 0.1;
                Location spiralLoc = loc.clone().add(
                    Math.cos(spiral) * radius,
                    i * 0.1,
                    Math.sin(spiral) * radius
                );
                
                world.spawnParticle(Particle.PORTAL, spiralLoc, 1, 0.05, 0.05, 0.05, 0.3);
                world.spawnParticle(Particle.DRAGON_BREATH, spiralLoc, 1, 0.03, 0.03, 0.03, 0.01);
            }
        }
        
        private void spawnVoidForm(Location center) {
            // State-specific void form rendering
            switch (currentState) {
                case PHASING -> spawnPhasingVoidForm(center);
                case TUNNELING -> spawnTunnelingVoidForm(center);
                case RIFTING -> spawnRiftingVoidForm(center);
                case WARPING -> spawnWarpingVoidForm(center);
            }
            
            // Void aura - always present
            spawnVoidAura(center);
            
            // Portal visualization
            renderVoidPortals();
        }
        
        private void spawnPhasingVoidForm(Location center) {
            World world = center.getWorld();
            
            // Phase shifting particles
            double phaseRadius = cfgDouble("void.phasing-radius", 1.5);
            int phaseParticles = cfgInt("void.phasing-particles", 15);
            
            for (int i = 0; i < phaseParticles; i++) {
                double angle = 2 * Math.PI * i / phaseParticles + quantumPhase;
                double radius = phaseRadius * (0.8 + 0.2 * Math.sin(quantumPhase * 2 + i));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(quantumPhase + angle) * 0.5 + 1.5;
                
                Location phaseLoc = center.clone().add(x, y, z);
                
                // Phase particles
                world.spawnParticle(Particle.END_ROD, phaseLoc, 1, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.PORTAL, phaseLoc, 1, 0.1, 0.1, 0.1, 0.2);
                
                // Quantum uncertainty
                if (Math.random() < 0.6) {
                    Color quantumColor = getQuantumColor(angle);
                    world.spawnParticle(Particle.DUST, phaseLoc, 1, 0.08, 0.08, 0.08, 0,
                        new Particle.DustOptions(quantumColor, 1.2f));
                }
            }
        }
        
        private void spawnTunnelingVoidForm(Location center) {
            World world = center.getWorld();
            
            // Void tunnels
            double tunnelLength = cfgDouble("void.tunnel-length", 3.0);
            int tunnelParticles = cfgInt("void.tunnel-particles", 20);
            
            Vector direction = player.getLocation().getDirection();
            
            for (int i = 0; i < tunnelParticles; i++) {
                double progress = (double) i / tunnelParticles;
                Location tunnelLoc = center.clone().add(direction.clone().multiply(progress * tunnelLength));
                tunnelLoc.add(0, Math.sin(progress * Math.PI) * 0.8, 0);
                
                // Tunnel void particles
                world.spawnParticle(Particle.PORTAL, tunnelLoc, 2, 0.2, 0.2, 0.2, 0.4);
                world.spawnParticle(Particle.DRAGON_BREATH, tunnelLoc, 1, 0.1, 0.1, 0.1, 0.02);
                
                // Tunnel outline
                Color tunnelColor = Color.fromRGB(128, 0, 255);
                world.spawnParticle(Particle.DUST, tunnelLoc, 1, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(tunnelColor, 1.4f));
            }
        }
        
        private void spawnRiftingVoidForm(Location center) {
            World world = center.getWorld();
            
            // Dimensional rifts
            double riftSize = cfgDouble("void.rift-size", 2.5);
            int riftParticles = cfgInt("void.rift-particles", 25);
            
            for (int rift = 0; rift < 3; rift++) {
                double riftAngle = 2 * Math.PI * rift / 3 + dimensionalRift;
                
                for (int i = 0; i < riftParticles; i++) {
                    double progress = (double) i / riftParticles;
                    double riftX = Math.cos(riftAngle) * progress * riftSize;
                    double riftZ = Math.sin(riftAngle) * progress * riftSize;
                    double riftY = Math.sin(progress * Math.PI * 2 + dimensionalRift) + 1.5;
                    
                    Location riftLoc = center.clone().add(riftX, riftY, riftZ);
                    
                    // Rift distortion
                    world.spawnParticle(Particle.PORTAL, riftLoc, 2, 0.15, 0.15, 0.15, 0.6);
                    world.spawnParticle(Particle.END_ROD, riftLoc, 1, 0.1, 0.1, 0.1, 0.03);
                    
                    // Reality tears
                    if (Math.random() < 0.7) {
                        Color riftColor = Color.fromRGB(255, 0, 255);
                        world.spawnParticle(Particle.DUST, riftLoc, 1, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(riftColor, 1.6f));
                    }
                }
            }
        }
        
        private void spawnWarpingVoidForm(Location center) {
            World world = center.getWorld();
            
            // Reality warping field
            double warpRadius = cfgDouble("void.warp-radius", 3.5);
            int warpParticles = cfgInt("void.warp-particles", 30);
            
            for (int i = 0; i < warpParticles; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = warpRadius * Math.random();
                double height = (Math.random() - 0.5) * 4.0 + 2.0;
                
                // Reality distortion calculation
                double distortion = Math.sin(realityDistortion + angle * 3) * 0.5;
                double x = Math.cos(angle) * radius * (1 + distortion);
                double z = Math.sin(angle) * radius * (1 + distortion);
                
                Location warpLoc = center.clone().add(x, height, z);
                
                // Reality warping particles
                world.spawnParticle(Particle.PORTAL, warpLoc, 3, 0.3, 0.3, 0.3, 0.8);
                world.spawnParticle(Particle.DRAGON_BREATH, warpLoc, 2, 0.2, 0.2, 0.2, 0.04);
                world.spawnParticle(Particle.END_ROD, warpLoc, 1, 0.1, 0.1, 0.1, 0.02);
                
                // Warped reality colors
                Color warpColor = getWarpColor(radius / warpRadius);
                world.spawnParticle(Particle.DUST, warpLoc, 1, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(warpColor, 1.8f));
            }
        }
        
        private void spawnVoidAura(Location center) {
            World world = center.getWorld();
            double auraRadius = cfgDouble("void.aura-radius", 1.2);
            int auraParticles = cfgInt("void.aura-particles", 10);
            
            for (int i = 0; i < auraParticles; i++) {
                double angle = 2 * Math.PI * i / auraParticles + quantumPhase * 0.7;
                double x = Math.cos(angle) * auraRadius;
                double z = Math.sin(angle) * auraRadius;
                double y = Math.sin(quantumPhase * 1.5 + angle * 2) * 0.3 + 1.8;
                
                Location auraLoc = center.clone().add(x, y, z);
                
                // Void aura
                world.spawnParticle(Particle.PORTAL, auraLoc, 1, 0.03, 0.03, 0.03, 0.1);
                
                if (Math.random() < 0.4) {
                    Color auraColor = Color.fromRGB(100, 0, 200);
                    world.spawnParticle(Particle.DUST, auraLoc, 1, 0.02, 0.02, 0.02, 0,
                        new Particle.DustOptions(auraColor, 0.9f));
                }
            }
        }
        
        private void renderVoidPortals() {
            World world = player.getWorld();
            
            for (int i = 0; i < voidPortals.length; i++) {
                Location portal = voidPortals[i];
                if (portal == null) continue;
                
                // Portal visualization
                double portalPhase = quantumPhase + i * 1.2;
                int portalParticles = cfgInt("portal.particles", 8);
                
                for (int p = 0; p < portalParticles; p++) {
                    double angle = 2 * Math.PI * p / portalParticles + portalPhase;
                    double radius = 0.8 + 0.2 * Math.sin(portalPhase * 2);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    Location portalLoc = portal.clone().add(x, 0, z);
                    
                    // Portal ring
                    world.spawnParticle(Particle.PORTAL, portalLoc, 1, 0.05, 0.05, 0.05, 0.2);
                    world.spawnParticle(Particle.END_ROD, portalLoc, 1, 0.02, 0.02, 0.02, 0.01);
                }
            }
        }
        
        private void spawnVoidRift(Location origin, Location target) {
            World world = origin.getWorld();
            Vector direction = target.toVector().subtract(origin.toVector()).normalize();
            double distance = origin.distance(target);
            
            // Void rift between origin and target
            for (int i = 0; i < 20; i++) {
                double progress = (double) i / 20;
                Location riftLoc = origin.clone().add(direction.clone().multiply(distance * progress));
                
                // Expanding rift
                world.spawnParticle(Particle.PORTAL, riftLoc, 3, 0.3, 0.3, 0.3, 0.8);
                world.spawnParticle(Particle.DRAGON_BREATH, riftLoc, 2, 0.2, 0.2, 0.2, 0.03);
                
                // Rift energy
                Color riftColor = Color.fromRGB(255, 0, 255);
                world.spawnParticle(Particle.DUST, riftLoc, 2, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(riftColor, 1.5f));
            }
        }
        
        private void spawnVoidEmergence(Location target) {
            World world = target.getWorld();
            
            // Void emergence effect
            for (int i = 0; i < 40; i++) {
                Location emergeLoc = generateRandomVoidLocation(target, 2.5, 2.0);
                
                // Emergence particles
                world.spawnParticle(Particle.PORTAL, emergeLoc, 2, 0.2, 0.2, 0.2, 0.4);
                world.spawnParticle(Particle.END_ROD, emergeLoc, 1, 0.1, 0.1, 0.1, 0.02);
                
                // Void dust
                Color emergeColor = Color.fromRGB(128, 0, 255);
                world.spawnParticle(Particle.DUST, emergeLoc, 1, 0.15, 0.15, 0.15, 0,
                    new Particle.DustOptions(emergeColor, 1.3f));
            }
        }
        
        // Color and effect helpers
        private Color getQuantumColor(double phase) {
            // Quantum uncertainty color spectrum
            double normalizedPhase = (Math.sin(phase) + 1) / 2;
            
            if (normalizedPhase < 0.5) {
                // Purple to blue
                return Color.fromRGB((int)(128 + 127 * normalizedPhase * 2), 0, 255);
            } else {
                // Blue to cyan
                double subPhase = (normalizedPhase - 0.5) * 2;
                return Color.fromRGB(255, (int)(255 * subPhase), 255);
            }
        }
        
        private Color getWarpColor(double intensity) {
            // Reality warp color based on intensity
            if (intensity < 0.33) {
                // Deep purple
                return Color.fromRGB(64, 0, 128);
            } else if (intensity < 0.67) {
                // Magenta
                return Color.fromRGB(255, 0, 255);
            } else {
                // Bright cyan
                return Color.fromRGB(0, 255, 255);
            }
        }
        
        private void applyVoidEffects() {
            int duration = cfgInt("effects.duration-ticks", 40);
            
            // Basic void effects
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false));
            
            // State-specific effects
            switch (currentState) {
                case PHASING -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, false, false));
                case TUNNELING -> {
                    // Enhanced movement for tunneling
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 1, false, false));
                }
                case RIFTING -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, false, false));
                case WARPING -> {
                    // Reality warping power
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, false, false));
                }
            }
            
            // Void resistance
            if (isVoidResistanceEnabled()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, false));
            }
        }
        
        private void removeVoidEffects() {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        }
        
        private void spawnVoidDeactivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();

            // Void closing effect
            for (int i = 0; i < 50; i++) {
                double angle = i * 0.4;
                double radius = 3.0 - (i * 0.05);
                double height = i * 0.08;

                Location closeLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );

                world.spawnParticle(Particle.PORTAL, closeLoc, 1, 0.1, 0.1, 0.1, 0.2);
                world.spawnParticle(Particle.SMOKE, closeLoc, 1, 0.05, 0.05, 0.05, 0.01);

                // Fading void energy
                if (Math.random() < 0.5) {
                    Color fadeColor = Color.fromRGB((int)(100 * (1 - i * 0.02)), 0, (int)(150 * (1 - i * 0.02)));
                    world.spawnParticle(Particle.DUST, closeLoc, 1, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(fadeColor, 1.0f));
                }
            }
        }
    }
    
    /* ---------------------------------------- */
    /* UTILITY HELPERS */
    /* ---------------------------------------- */
    private Location generateRandomVoidLocation(Location center, double radius, double height) {
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double yOffset = (Math.random() - 0.5) * height;
        
        double x = Math.cos(angle) * distance;
        double z = Math.sin(angle) * distance;
        
        return center.clone().add(x, yOffset, z);
    }
    
    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("void-walk." + path, def);
    }
    
    private int cfgInt(String path, int def) {
        return spellConfig.getInt("void-walk." + path, def);
    }
    
    private boolean cfgBool(String path, boolean def) {
        return spellConfig.getBoolean("void-walk." + path, def);
    }
    
    private boolean isVoidResistanceEnabled() {
        return cfgBool(VOID_RESISTANCE_PATH, VOID_RESISTANCE_DEFAULT);
    }
}
