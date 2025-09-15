package nl.wantedchef.empirewand.spell.heal;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RadiantBeacon extends Spell<Void> {

    private double radius;
    private double healAmount;
    private double damageAmount;
    private int totalPulses;
    private int pulseInterval;
    private int maxTargets;
    private boolean hitPlayers;
    private boolean hitMobs;
    private boolean damagePlayers;

    private static final Set<PotionEffectType> NEGATIVE_EFFECTS = Set.of(
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA);

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Radiant Beacon";
            this.description = "Creates a beacon that heals allies and damages enemies.";
            this.cooldown = java.time.Duration.ofSeconds(35);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new RadiantBeacon(this);
        }
    }

    private RadiantBeacon(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "radiant-beacon";
    }

    @Override
    public Component displayName() {
        return Component.text("Stralingsbaken");
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        this.radius = spellConfig.getDouble("values.radius", 6.0);
        this.healAmount = spellConfig.getDouble("values.heal-amount", 1.0);
        this.damageAmount = spellConfig.getDouble("values.damage-amount", 1.0);
        this.totalPulses = spellConfig.getInt("values.duration-pulses", 8);
        this.pulseInterval = spellConfig.getInt("values.pulse-interval-ticks", 20);
        this.maxTargets = spellConfig.getInt("values.max-targets", 8);
        this.hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        this.hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);
        this.damagePlayers = spellConfig.getBoolean("flags.damage-players", false);

        ArmorStand beacon = player.getWorld().spawn(player.getLocation(), ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setPersistent(false);
        });

        // Enhanced lighthouse beacon with spectacular effects
        Plugin plugin = context.plugin();
        if (plugin == null) {
            beacon.remove();
            return null;
        }

        new SpectacularBeaconTask(beacon, context).runTaskTimer(plugin, 0L, this.pulseInterval);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private class SpectacularBeaconTask extends BukkitRunnable {
        private final ArmorStand beacon;
        private final SpellContext context;
        private int pulsesCompleted = 0;

        public SpectacularBeaconTask(ArmorStand beacon, SpellContext context) {
            this.beacon = beacon;
            this.context = context;
        }

        @Override
        public synchronized void cancel() throws IllegalStateException {
            super.cancel();
            // Spectacular beacon completion
            createBeaconCompletion();
            if (beacon.isValid()) {
                beacon.remove();
            }
        }

        @Override
        public void run() {
            if (!beacon.isValid() || pulsesCompleted >= totalPulses) {
                this.cancel();
                return;
            }

            double pulseProgress = (double) pulsesCompleted / totalPulses;
            
            // Enhanced beacon effects
            createLighthouseBeam(pulseProgress);
            createRadiantPulse(pulseProgress);
            
            // Get nearby entities with optimized targeting
            List<LivingEntity> nearbyEntities = beacon.getWorld()
                    .getNearbyLivingEntities(beacon.getLocation(), radius, entity -> !entity.equals(beacon) &&
                            ((entity instanceof Player && hitPlayers) || (!(entity instanceof Player) && hitMobs)))
                    .stream()
                    .limit(maxTargets)
                    .collect(Collectors.toList());

            // Perform healing/damage with enhanced effects
            performBeaconEffects(nearbyEntities, pulseProgress);
            
            // Enhanced beacon particles and sounds
            createSpectacularBeaconEffects(pulseProgress);
            
            pulsesCompleted++;
        }
        
        private void createLighthouseBeam(double progress) {
            // Create rotating lighthouse beam
            double beamAngle = progress * Math.PI * 4; // Multiple rotations
            Location beaconLoc = beacon.getLocation();
            
            // Main lighthouse beam
            for (int h = 0; h < 20; h++) {
                double y = h * 0.8;
                double beamRadius = h * 0.2; // Expanding beam
                
                for (int i = 0; i < 6; i++) {
                    double angle = beamAngle + (i * Math.PI / 3);
                    double x = Math.cos(angle) * beamRadius;
                    double z = Math.sin(angle) * beamRadius;
                    
                    Location beamPoint = beaconLoc.clone().add(x, y, z);
                    context.fx().spawnParticles(beamPoint, Particle.END_ROD, 2, 0.1, 0.1, 0.1, 0.02);
                    context.fx().spawnParticles(beamPoint, Particle.GLOW, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            
            // Horizontal lighthouse sweeps
            for (double sweepAngle = 0; sweepAngle < Math.PI * 2; sweepAngle += Math.PI / 16) {
                double sweepX = Math.cos(beamAngle + sweepAngle) * radius * 1.5;
                double sweepZ = Math.sin(beamAngle + sweepAngle) * radius * 1.5;
                Location sweepLoc = beaconLoc.clone().add(sweepX, 3, sweepZ);
                
                context.fx().spawnParticles(sweepLoc, Particle.END_ROD, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
        
        private void createRadiantPulse(double progress) {
            // Create radiant pulse waves
            double pulseRadius = (progress * 3 + pulsesCompleted * 0.5) % radius;
            Location beaconLoc = beacon.getLocation();
            
            // Expanding radiant ring
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 20) {
                double x = Math.cos(angle) * pulseRadius;
                double z = Math.sin(angle) * pulseRadius;
                double y = Math.sin(pulseRadius * 0.5) * 0.8 + 1;
                
                Location pulseLoc = beaconLoc.clone().add(x, y, z);
                context.fx().spawnParticles(pulseLoc, Particle.GLOW, 2, 0.1, 0.1, 0.1, 0.02);
                context.fx().spawnParticles(pulseLoc, Particle.END_ROD, 1, 0.1, 0.1, 0.1, 0.01);
                
                // Golden radiant particles
                context.fx().spawnParticles(pulseLoc, Particle.DUST, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f));
            }
        }
        
        private void performBeaconEffects(List<LivingEntity> entities, double progress) {
            for (LivingEntity entity : entities) {
                if (entity instanceof Player playerTarget) {
                    if (damagePlayers && !playerTarget.equals(context.caster())) {
                        entity.damage(damageAmount, context.caster());
                        createDamageEffect(entity.getLocation());
                    } else {
                        // Enhanced healing
                        AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                        if (maxHealthAttribute != null) {
                            double maxHealth = maxHealthAttribute.getValue();
                            entity.setHealth(Math.min(maxHealth, entity.getHealth() + healAmount));
                        }

                        // Remove negative effects with visual feedback
                        entity.getActivePotionEffects().stream()
                                .filter(e -> NEGATIVE_EFFECTS.contains(e.getType()))
                                .findFirst()
                                .ifPresent(e -> {
                                    entity.removePotionEffect(e.getType());
                                    createPurificationEffect(entity.getLocation());
                                });
                        
                        createHealingEffect(entity.getLocation(), progress);
                    }
                } else {
                    entity.damage(damageAmount, context.caster());
                    createDamageEffect(entity.getLocation());
                }
            }
        }
        
        private void createHealingEffect(Location location, double progress) {
            // Enhanced healing visual
            context.fx().spawnParticles(location.clone().add(0, 1, 0), Particle.HEART, 5, 0.3, 0.5, 0.3, 0);
            context.fx().spawnParticles(location.clone().add(0, 1, 0), Particle.GLOW, 8, 0.4, 0.4, 0.4, 0.05);
            context.fx().spawnParticles(location.clone().add(0, 1, 0), Particle.END_ROD, 3, 0.2, 0.2, 0.2, 0.02);
            
            // Healing aura ring
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                Location auraLoc = location.clone().add(x, 0.5, z);
                context.fx().spawnParticles(auraLoc, Particle.HAPPY_VILLAGER, 1, 0, 0, 0, 0);
            }
        }
        
        private void createDamageEffect(Location location) {
            // Enhanced damage visual
            context.fx().spawnParticles(location.clone().add(0, 1, 0), Particle.CRIT, 8, 0.4, 0.4, 0.4, 0.1);
            context.fx().spawnParticles(location.clone().add(0, 1, 0), Particle.ENCHANTED_HIT, 5, 0.3, 0.3, 0.3, 0.05);
        }
        
        private void createPurificationEffect(Location location) {
            // Purification visual
            context.fx().spawnParticles(location.clone().add(0, 1, 0), Particle.DUST, 10, 0.4, 0.4, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f)); // White purification
            context.fx().spawnParticles(location.clone().add(0, 1, 0), Particle.END_ROD, 5, 0.3, 0.3, 0.3, 0.03);
        }
        
        private void createSpectacularBeaconEffects(double progress) {
            // Enhanced beacon particle effects
            Location beaconLoc = beacon.getLocation();
            
            // Central beacon pillar with intensity
            int intensity = (int)(5 + progress * 10);
            for (int h = 0; h < 15; h++) {
                context.fx().spawnParticles(beaconLoc.clone().add(0, h * 0.5, 0), 
                    Particle.END_ROD, intensity, 0.2, 0.2, 0.2, 0.02);
                
                if (h % 3 == 0) {
                    context.fx().spawnParticles(beaconLoc.clone().add(0, h * 0.5, 0), 
                        Particle.GLOW, 3, 0.3, 0.3, 0.3, 0.05);
                }
            }
            
            // Ring particles with increasing intensity
            for (int i = 0; i < 30; i++) {
                double angle = 2 * Math.PI * i / 30;
                double ringRadius = radius * 0.8;
                double x = ringRadius * Math.cos(angle);
                double z = ringRadius * Math.sin(angle);
                double y = Math.sin(progress * Math.PI * 4 + i) * 0.5 + 1;
                
                Location ringLoc = beaconLoc.clone().add(x, y, z);
                context.fx().spawnParticles(ringLoc, Particle.GLOW, 2, 0.1, 0.1, 0.1, 0.02);
                
                if (i % 5 == 0) {
                    context.fx().spawnParticles(ringLoc, Particle.DUST, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.3f)); // Gold
                }
            }

            // Enhanced sounds with varying pitch
            if (pulsesCompleted % 2 == 0) {
                float pitch = 1.5f + (float)progress * 0.5f;
                context.fx().playSound(beaconLoc, Sound.BLOCK_BELL_USE, 0.8f, pitch);
                context.fx().playSound(beaconLoc, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.8f);
            }
            
            if (pulsesCompleted % 4 == 0) {
                context.fx().playSound(beaconLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
            }
        }
        
        private void createBeaconCompletion() {
            Location beaconLoc = beacon.getLocation();
            
            // Spectacular beacon completion
            context.fx().spawnParticles(beaconLoc.clone().add(0, 2, 0), Particle.EXPLOSION_EMITTER, 3, 1, 1, 1, 0);
            context.fx().spawnParticles(beaconLoc.clone().add(0, 2, 0), Particle.GLOW, 100, 3, 3, 3, 0.5);
            context.fx().spawnParticles(beaconLoc.clone().add(0, 2, 0), Particle.END_ROD, 80, 2, 4, 2, 0.3);
            
            // Final lighthouse beam sweep
            for (int i = 0; i < 16; i++) {
                final int beam = i;
                context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                    double angle = (2 * Math.PI * beam / 16);
                    for (int r = 1; r <= 10; r++) {
                        double x = Math.cos(angle) * r;
                        double z = Math.sin(angle) * r;
                        Location sweepLoc = beaconLoc.clone().add(x, 5, z);
                        context.fx().spawnParticles(sweepLoc, Particle.END_ROD, 3, 0.2, 0.2, 0.2, 0.02);
                    }
                }, beam);
            }
            
            // Completion sounds
            context.fx().playSound(beaconLoc, Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 1.0f);
            context.fx().playSound(beaconLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            
            // Message to nearby players
            for (Player player : beaconLoc.getWorld().getPlayers()) {
                if (player.getLocation().distance(beaconLoc) <= radius * 2) {
                    player.sendMessage("§6§l🏮 Radiant Beacon §ecompleted its duty! Light fades...");
                }
            }
        }

    }
}
