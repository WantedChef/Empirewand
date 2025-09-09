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
 * A devastating ice spell that creates a blizzard in a large area,
 * slowing and damaging all enemies while creating icy terrain.
 */
public class Blizzard extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blizzard";
            this.description = "Creates a devastating blizzard that slows and damages enemies while covering the area in ice.";
            this.cooldown = java.time.Duration.ofSeconds(55);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new Blizzard(this);
        }
    }

    private Blizzard(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "blizzard";
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

        // Start blizzard effect
        new BlizzardTask(context, player.getLocation(), radius, damage, slowDuration, slowAmplifier, durationTicks, createIce)
                .runTaskTimer(context.plugin(), 0L, 3L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
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
        }

        @Override
        public void run() {
            if (ticks >= durationTicks) {
                this.cancel();
                cleanupIce();
                return;
            }

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

        private void createBlizzardEffects() {
            // Create snow particles in a large area
            for (int i = 0; i < 30; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius;
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                double y = center.getY() + random.nextDouble() * 10;
                
                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.3, 0.3, 0.3, 0.01);
            }

            // Create wind effect by pushing entities
            if (ticks % 20 == 0) {
                for (LivingEntity entity : world.getNearbyLivingEntities(center, radius, 10, radius)) {
                    if (entity.equals(context.caster())) continue;
                    
                    // Push entity in random direction
                    Vector push = new Vector(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize().multiply(0.5);
                    entity.setVelocity(entity.getVelocity().add(push));
                }
            }
        }

        private void applyBlizzardEffects() {
            for (LivingEntity entity : world.getNearbyLivingEntities(center, radius, 10, radius)) {
                if (entity.equals(context.caster())) continue;
                if (entity.isDead() || !entity.isValid()) continue;

                // Damage entity
                entity.damage(damage, context.caster());

                // Apply slowness
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, false));

                // Visual effects
                world.spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
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
        }
    }
}