package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.common.visual.RingRenderer;
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
 * A devastating ice spell that creates a blizzard in a large area,
 * slowing and damaging enemies while creating icy terrain with enhanced visual effects.
 */
public class BlizzardEnhanced extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blizzard";
            this.description = "Creates a devastating blizzard that slows and damages enemies while covering the area in ice with enhanced visuals.";
            this.cooldown = java.time.Duration.ofSeconds(55);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new BlizzardEnhanced(this);
        }
    }

    private BlizzardEnhanced(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "blizzard-enhanced";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 20.0);
        double damage = spellConfig.getDouble("values.damage", 1.5);
        int slowDuration = spellConfig.getInt("values.slow-duration-ticks", 60);
        int slowAmplifier = spellConfig.getInt("values.slow-amplifier", 3);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 150);
        boolean createIce = spellConfig.getBoolean("flags.create-ice", true);

        // Play initial sound
        player.getWorld().playSound(player.getLocation(), Sound.AMBIENT_UNDERWATER_LOOP, 2.0f, 0.3f);

        // Create initial blizzard effect
        createInitialEffect(context, player.getLocation(), radius);

        // Start blizzard effect
        new BlizzardTask(context, player.getLocation(), radius, damage, slowDuration, slowAmplifier, durationTicks, createIce)
                .runTaskTimer(context.plugin(), 0L, 3L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private void createInitialEffect(SpellContext context, Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;

        // Create expanding ring to indicate blizzard area
        new BukkitRunnable() {
            int currentRadius = 1;
            
            @Override
            public void run() {
                if (currentRadius > radius) {
                    this.cancel();
                    return;
                }
                
                RingRenderer.renderRing(center, currentRadius, Math.max(12, currentRadius * 2),
                        (loc, vec) -> world.spawnParticle(Particle.SNOWFLAKE, loc, 2, 0, 0, 0, 0));
                
                currentRadius += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    private static class BlizzardTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final double damage;
        private final int slowDuration;
        private final int slowAmplifier;
        private final int durationTicks;
        private final boolean createIce;
        private final World world;
        private final Random random = new Random();
        private final Set<Location> iceBlocks = new HashSet<>();
        private final Map<Block, BlockData> originalBlocks = new HashMap<>();
        private int ticks = 0;
        private final List<Snowflake> snowflakes = new ArrayList<>();

        public BlizzardTask(SpellContext context, Location center, double radius, double damage,
                           int slowDuration, int slowAmplifier, int durationTicks, boolean createIce) {
            this.context = context;
            this.center = center;
            this.radius = radius;
            this.damage = damage;
            this.slowDuration = slowDuration;
            this.slowAmplifier = slowAmplifier;
            this.durationTicks = durationTicks;
            this.createIce = createIce;
            this.world = center.getWorld();
            
            // Initialize snowflakes
            for (int i = 0; i < 50; i++) {
                snowflakes.add(new Snowflake(center, radius, random));
            }
        }

        @Override
        public void run() {
            if (ticks >= durationTicks) {
                this.cancel();
                cleanupIce();
                return;
            }

            // Update snowflakes
            updateSnowflakes();

            // Create blizzard effects
            createBlizzardEffects();

            // Apply effects to entities
            if (ticks % 10 == 0) {
                applyBlizzardEffects();
            }

            // Create ice blocks
            if (createIce && ticks % 15 == 0) {
                createIceBlocks();
            }

            ticks++;
        }

        private void updateSnowflakes() {
            for (Snowflake snowflake : snowflakes) {
                snowflake.update();
                if (world != null) {
                    world.spawnParticle(Particle.SNOWFLAKE, snowflake.location, 1, 0, 0, 0, 0);
                }
            }
        }

        private void createBlizzardEffects() {
            // Create snow particles in a large area
            for (int i = 0; i < 20; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius;
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                double y = center.getY() + random.nextDouble() * 15;
                
                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.5, 0.5, 0.5, 0.02);
            }

            // Create wind effect by pushing entities
            if (ticks % 20 == 0) {
                for (LivingEntity entity : world.getNearbyLivingEntities(center, radius, 15, radius)) {
                    if (entity.equals(context.caster())) continue;
                    
                    // Push entity in random direction with added upward force
                    Vector push = new Vector(random.nextDouble() - 0.5, random.nextDouble() * 0.3, random.nextDouble() - 0.5).normalize().multiply(0.7);
                    entity.setVelocity(entity.getVelocity().add(push));
                }
            }

            // Create occasional gusts
            if (ticks % 40 == 0) {
                double gustAngle = random.nextDouble() * 2 * Math.PI;
                Vector gustDirection = new Vector(Math.cos(gustAngle), 0, Math.sin(gustAngle)).normalize().multiply(1.5);
                
                for (LivingEntity entity : world.getNearbyLivingEntities(center, radius, 15, radius)) {
                    if (entity.equals(context.caster())) continue;
                    entity.setVelocity(entity.getVelocity().add(gustDirection));
                }
                
                // Gust sound
                world.playSound(center, Sound.ENTITY_PLAYER_BREATH, 1.0f, 0.5f);
            }
        }

        private void applyBlizzardEffects() {
            for (LivingEntity entity : world.getNearbyLivingEntities(center, radius, 15, radius)) {
                if (entity.equals(context.caster())) continue;
                if (entity.isDead() || !entity.isValid()) continue;

                // Damage entity
                entity.damage(damage, context.caster());

                // Apply slowness
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, false));

                // Apply blindness occasionally
                if (random.nextDouble() < 0.3) {
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false));
                }

                // Visual effects
                world.spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 1, 0), 15, 0.5, 0.7, 0.5, 0.02);
                world.spawnParticle(Particle.CLOUD, entity.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.01);
            }
        }

        private void createIceBlocks() {
            for (int i = 0; i < 15; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * (radius - 3);
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                
                Location groundLoc = new Location(world, x, center.getY(), z);
                groundLoc.setY(world.getHighestBlockYAt(groundLoc));
                
                Block block = groundLoc.getBlock();
                if (block.getType().isSolid() && !block.getType().isAir() && block.getType() != Material.ICE && 
                    block.getType() != Material.PACKED_ICE && block.getType() != Material.BEDROCK) {
                    
                    // Store original block
                    originalBlocks.put(block, block.getBlockData());
                    
                    // Turn to ice
                    block.setType(Material.ICE);
                    iceBlocks.add(block.getLocation());
                    
                    // Particle effect
                    world.spawnParticle(Particle.SNOWFLAKE, block.getLocation().add(0.5, 1, 0.5), 8, 0.3, 0.3, 0.3, 0.02);
                    world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 1, 0.5), 5, 0.2, 0.2, 0.2, 0.01, block.getBlockData());
                }
            }
        }

        private void cleanupIce() {
            // Restore original blocks
            for (Location loc : iceBlocks) {
                Block block = world.getBlockAt(loc);
                if (block.getType() == Material.ICE) {
                    BlockData original = originalBlocks.get(block);
                    if (original != null) {
                        block.setBlockData(original);
                    } else {
                        block.setType(Material.AIR);
                    }
                }
            }
            
            // Play cleanup sound
            world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
            
            // Create dissipating effect
            createDissipatingEffect();
        }

        private void createDissipatingEffect() {
            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 20;
                
                @Override
                public void run() {
                    if (ticks >= maxTicks || world == null) {
                        this.cancel();
                        return;
                    }
                    
                    // Create shrinking ring
                    double currentRadius = radius * (1.0 - (ticks / (double) maxTicks));
                    RingRenderer.renderRing(center, currentRadius, Math.max(12, (int)currentRadius * 2),
                            (loc, vec) -> world.spawnParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.01));
                    
                    ticks++;
                }
            }.runTaskTimer(context.plugin(), 0L, 1L);
        }

        private static class Snowflake {
            Location location;
            Vector velocity;
            final Location center;
            final double radius;
            final Random random;

            Snowflake(Location center, double radius, Random random) {
                this.center = center;
                this.radius = radius;
                this.random = random;
                
                // Random starting position above the blizzard area
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius;
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                double y = center.getY() + 15 + random.nextDouble() * 10;
                
                this.location = new Location(center.getWorld(), x, y, z);
                this.velocity = new Vector(0, -0.3 - random.nextDouble() * 0.4, 0);
            }

            void update() {
                // Apply gravity
                location.add(velocity);
                
                // Apply some horizontal drift
                location.add((random.nextDouble() - 0.5) * 0.1, 0, (random.nextDouble() - 0.5) * 0.1);
                
                // Reset if fallen too far or outside radius
                double distanceFromCenter = Math.sqrt(
                    Math.pow(location.getX() - center.getX(), 2) + 
                    Math.pow(location.getZ() - center.getZ(), 2)
                );
                
                if (location.getY() < center.getY() - 5 || distanceFromCenter > radius * 1.5) {
                    // Reset to top
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double distance = random.nextDouble() * radius;
                    double x = center.getX() + Math.cos(angle) * distance;
                    double z = center.getZ() + Math.sin(angle) * distance;
                    location = new Location(center.getWorld(), x, center.getY() + 20, z);
                }
            }
        }
    }
}