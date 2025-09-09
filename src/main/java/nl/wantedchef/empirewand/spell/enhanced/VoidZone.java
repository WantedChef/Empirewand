package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A terrifying dark spell that creates multiple void zones,
 * pulling enemies in and dealing massive damage.
 */
public class VoidZone extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Void Zone";
            this.description = "Creates multiple void zones that pull enemies in and deal massive damage.";
            this.cooldown = java.time.Duration.ofSeconds(50);
            this.spellType = SpellType.DARK;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new VoidZone(this);
        }
    }

    private VoidZone(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "void-zone";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 25.0);
        int zoneCount = spellConfig.getInt("values.zone-count", 5);
        double damage = spellConfig.getDouble("values.damage", 8.0);
        double pullStrength = spellConfig.getDouble("values.pull-strength", 0.3);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 120);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);

        // Play initial sound
        player.getWorld().playSound(player.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, 2.0f, 0.5f);

        // Create void zones
        new VoidZoneTask(context, player.getLocation(), radius, zoneCount, damage, pullStrength, durationTicks, affectsPlayers)
                .runTaskTimer(context.plugin(), 0L, 2L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private static class VoidZoneTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int zoneCount;
        private final double damage;
        private final double pullStrength;
        private final int durationTicks;
        private final boolean affectsPlayers;
        private final World world;
        private final Random random = new Random();
        private final List<VoidZoneInstance> voidZones = new ArrayList<>();
        private int ticks = 0;

        public VoidZoneTask(SpellContext context, Location center, double radius, int zoneCount,
                           double damage, double pullStrength, int durationTicks, boolean affectsPlayers) {
            this.context = context;
            this.center = center;
            this.radius = radius;
            this.zoneCount = zoneCount;
            this.damage = damage;
            this.pullStrength = pullStrength;
            this.durationTicks = durationTicks;
            this.affectsPlayers = affectsPlayers;
            this.world = center.getWorld();
            
            // Create initial void zones
            for (int i = 0; i < zoneCount; i++) {
                createVoidZone();
            }
        }

        @Override
        public void run() {
            if (ticks >= durationTicks) {
                this.cancel();
                return;
            }

            // Update existing void zones
            Iterator<VoidZoneInstance> iterator = voidZones.iterator();
            while (iterator.hasNext()) {
                VoidZoneInstance zone = iterator.next();
                if (!zone.update()) {
                    iterator.remove();
                }
            }

            // Create new void zones periodically
            if (ticks % 30 == 0 && voidZones.size() < zoneCount * 2) {
                createVoidZone();
            }

            // Apply pull and damage effects
            if (ticks % 5 == 0) {
                applyVoidEffects();
            }

            ticks++;
        }

        private void createVoidZone() {
            // Random location within radius
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * (radius - 5);
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;
            Location zoneLocation = new Location(world, x, world.getHighestBlockYAt((int)x, (int)z), z);
            
            voidZones.add(new VoidZoneInstance(zoneLocation, 4.0, 60));
        }

        private void applyVoidEffects() {
            for (VoidZoneInstance zone : voidZones) {
                for (LivingEntity entity : world.getNearbyLivingEntities(zone.location, zone.radius, 5, zone.radius)) {
                    if (entity.equals(context.caster())) continue;
                    if (entity instanceof Player && !affectsPlayers) continue;
                    if (entity.isDead() || !entity.isValid()) continue;

                    // Pull entity toward center of void zone
                    Vector pull = zone.location.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(pullStrength);
                    entity.setVelocity(entity.getVelocity().add(pull));

                    // Damage entity if close to center
                    double distance = entity.getLocation().distance(zone.location);
                    if (distance < zone.radius * 0.3) {
                        entity.damage(damage, context.caster());
                        
                        // Apply wither effect
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 1, false, false));
                        
                        // Visual effect
                        world.spawnParticle(Particle.SQUID_INK, entity.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
                    }
                }
            }
        }

        private class VoidZoneInstance {
            final Location location;
            final double radius;
            final int maxDuration;
            int duration;
            final Set<Block> affectedBlocks = new HashSet<>();
            final Map<Block, BlockData> originalBlocks = new HashMap<>();

            VoidZoneInstance(Location location, double radius, int duration) {
                this.location = location;
                this.radius = radius;
                this.maxDuration = duration;
                this.duration = duration;
                createVisualEffect();
            }

            boolean update() {
                duration--;
                
                // Pulsing effect
                if (duration % 10 == 0) {
                    pulseEffect();
                }
                
                // Visual particles
                createParticles();
                
                return duration > 0;
            }

            private void createVisualEffect() {
                World world = location.getWorld();
                if (world == null) return;

                int blockRadius = (int) radius;
                for (int x = -blockRadius; x <= blockRadius; x++) {
                    for (int z = -blockRadius; z <= blockRadius; z++) {
                        double distance = Math.sqrt(x * x + z * z);
                        if (distance <= radius) {
                            Block block = world.getBlockAt(
                                location.getBlockX() + x,
                                location.getBlockY() - 1,
                                location.getBlockZ() + z
                            );
                            
                            if (!block.getType().isAir() && block.getType() != Material.BEDROCK) {
                                originalBlocks.put(block, block.getBlockData());
                                affectedBlocks.add(block);
                                
                                // Particle effect
                                world.spawnParticle(Particle.SQUID_INK, block.getLocation().add(0.5, 1, 0.5), 2, 0.1, 0.1, 0.1, 0);
                            }
                        }
                    }
                }
                
                // Sound effect
                world.playSound(location, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.5f);
            }

            private void pulseEffect() {
                World world = location.getWorld();
                if (world == null) return;

                // Particle pulse
                world.spawnParticle(Particle.SQUID_INK, location, 50, radius, 0.5, radius, 0.1);
                world.spawnParticle(Particle.SMOKE, location, 30, radius * 0.5, 1, radius * 0.5, 0.05);
                
                // Sound effect
                world.playSound(location, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.5f, 0.8f);
            }

            private void createParticles() {
                World world = location.getWorld();
                if (world == null) return;

                // Ring of particles
                for (int i = 0; i < 20; i++) {
                    double angle = 2 * Math.PI * i / 20;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location particleLoc = location.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.SQUID_INK, particleLoc, 1, 0, 0, 0, 0);
                }

                // Central void effect
                world.spawnParticle(Particle.SMOKE, location, 5, 0.5, 0.5, 0.5, 0.01);
            }
        }
    }
}