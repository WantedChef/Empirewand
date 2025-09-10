package nl.wantedchef.empirewand.spell.toggle.movement;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.base.PrereqInterface;
import nl.wantedchef.empirewand.spell.base.Spell;
import nl.wantedchef.empirewand.spell.base.SpellContext;
import nl.wantedchef.empirewand.spell.base.SpellType;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * DarkCloud - Mechanically identical to RagfCloud, but with Mephidantes vibes:
 * shadow clouds with "soul siphon" feeling and short blind/disorientation on entry.
 */
public final class DarkCloud extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /*  DATA                                    */
    /* ---------------------------------------- */
    private final Map<UUID, CloudData> clouds = new WeakHashMap<>();
    private EmpireWandPlugin plugin;

    /* ---------------------------------------- */
    /*  BUILDER                                 */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Dark Cloud";
            description = "Shadow clouds with 'soul siphon' feeling and short blind/disorientation on entry. Mechanically identical to RagfCloud.";
            cooldown = Duration.ofSeconds(25);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new DarkCloud(this);
        }
    }

    private DarkCloud(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /*  SPELL API                               */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "dark-cloud";
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
    /*  TOGGLE API                              */
    /* ---------------------------------------- */
    @Override
    public boolean isActive(Player player) {
        return clouds.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player)) return;
        plugin = context.plugin();
        clouds.put(player.getUniqueId(), new CloudData(player, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        Optional.ofNullable(clouds.remove(player.getUniqueId())).ifPresent(CloudData::stop);
    }

    @Override
    public void forceDeactivate(Player player) {
        deactivate(player, null);
    }

    /* ---------------------------------------- */
    /*  INTERNAL CLASS                          */
    /* ---------------------------------------- */
    private final class CloudData {
        private final Player player;
        private final BukkitTask ticker;
        private final BukkitTask pulseTicker;
        private final Location cloudLocation;
        private final double cloudRadius;
        private final int durationTicks;
        private final int pulseInterval;
        private int tickCounter = 0;

        CloudData(Player player, SpellContext context) {
            this.player = player;
            this.cloudLocation = player.getLocation();
            this.cloudRadius = spellConfig.getDouble("values.radius", 6.0);
            this.durationTicks = spellConfig.getInt("values.duration-ticks", 300); // 15 seconds default
            this.pulseInterval = spellConfig.getInt("values.pulse-interval-ticks", 30); // 1.5 seconds default
            
            // Send activation message
            player.sendMessage(Component.text("§5⚡ §7Dark Cloud deployed. Soul siphon zone created."));
            
            // Start the main ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1); // Run every tick
            
            // Start pulse ticker
            this.pulseTicker = Bukkit.getScheduler().runTaskTimer(plugin, this::pulse, 0, pulseInterval);
        }

        void stop() {
            ticker.cancel();
            pulseTicker.cancel();
            player.sendMessage(Component.text("§5⚡ §7Dark Cloud dissipated."));
        }

        private void tick() {
            if (!player.isOnline() || player.isDead()) {
                forceDeactivate(player);
                return;
            }

            // Increment tick counter
            tickCounter++;
            
            // Check if duration has expired
            if (tickCounter >= durationTicks) {
                forceDeactivate(player);
                return;
            }
            
            // Apply cloud particles periodically
            if (tickCounter % 4 == 0) {
                spawnCloudParticles();
            }
        }
        
        private void spawnCloudParticles() {
            World world = cloudLocation.getWorld();
            
            // Create a ring of particles around the cloud
            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                double x = Math.cos(angle) * cloudRadius;
                double z = Math.sin(angle) * cloudRadius;
                Location particleLoc = cloudLocation.clone().add(x, 0.1, z);
                
                // Alternate between different particle types for dark effect
                if (tickCounter % 12 < 4) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                } else if (tickCounter % 12 < 8) {
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
                } else {
                    world.spawnParticle(Particle.ASH, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            
            // Create occasional soul particles for siphon effect
            if (Math.random() < 0.3) {
                world.spawnParticle(Particle.SOUL, cloudLocation, 5, 1.0, 0.1, 1.0, 0.1);
            }
            
            // Create end rod particles for mystical effect
            if (Math.random() < 0.2) {
                world.spawnParticle(Particle.END_ROD, cloudLocation, 2, 0.8, 0.1, 0.8, 0.02);
            }
        }
        
        private void pulse() {
            World world = cloudLocation.getWorld();
            double pulseDamage = spellConfig.getDouble("values.pulse-damage", 2.0);
            double pullStrength = spellConfig.getDouble("values.pull-strength", 0.25);
            double knockbackStrength = spellConfig.getDouble("values.knockback-strength", 0.4);
            int disorientationDuration = spellConfig.getInt("values.disorientation-duration-ticks", 30); // 1.5 seconds
            
            // Get nearby entities
            for (Entity entity : world.getNearbyEntities(cloudLocation, cloudRadius, cloudRadius, cloudRadius)) {
                if (entity instanceof LivingEntity && entity != player && !entity.isDead()) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    Location entityLoc = livingEntity.getLocation();
                    
                    // Apply DOT damage
                    livingEntity.damage(pulseDamage, player);
                    
                    // Calculate distance from center
                    double distance = entityLoc.distance(cloudLocation);
                    
                    // Apply pull effect if entity is at edge
                    if (distance > cloudRadius * 0.7) {
                        Vector pullDirection = cloudLocation.toVector().subtract(entityLoc.toVector()).normalize();
                        livingEntity.setVelocity(pullDirection.multiply(pullStrength).setY(0.1));
                    }
                    // Apply knockback effect if entity is near center
                    else if (distance < cloudRadius * 0.3) {
                        Vector knockbackDirection = entityLoc.toVector().subtract(cloudLocation.toVector()).normalize();
                        livingEntity.setVelocity(knockbackDirection.multiply(knockbackStrength).setY(0.15));
                    }
                    
                    // Apply wither effect
                    livingEntity.addPotionEffect(new PotionEffect(
                        PotionEffectType.WITHER,
                        40, // 2 seconds
                        0,
                        false,
                        true
                    ));
                    
                    // Apply disorientation effect for new entrants
                    if (distance < cloudRadius * 0.5 && Math.random() < 0.3) {
                        // Apply blindness
                        livingEntity.addPotionEffect(new PotionEffect(
                            PotionEffectType.BLINDNESS,
                            disorientationDuration,
                            0,
                            false,
                            true
                        ));
                        
                        // Apply slow effect
                        livingEntity.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            disorientationDuration,
                            1,
                            false,
                            true
                        ));
                    }
                    
                    // Spawn pulse effect particles
                    world.spawnParticle(Particle.DAMAGE_INDICATOR, entityLoc.add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.1);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, entityLoc, 4, 0.3, 0.3, 0.3, 0.03);
                }
            }
            
            // Create visual pulse effect
            for (int i = 0; i < 30; i++) {
                double angle = 2 * Math.PI * i / 30;
                double x = Math.cos(angle) * cloudRadius;
                double z = Math.sin(angle) * cloudRadius;
                Location particleLoc = cloudLocation.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
            }
            
            // Play pulse sound
            world.playSound(cloudLocation, Sound.BLOCK_SOUL_SAND_BREAK, 0.7f, 1.2f);
        }
    }
}