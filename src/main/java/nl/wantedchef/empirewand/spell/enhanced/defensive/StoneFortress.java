package nl.wantedchef.empirewand.spell.enhanced.defensive;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A defensive spell that creates a stone fortress around the caster,
 * providing protection and damaging nearby enemies.
 */
public class StoneFortress extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Stone Fortress";
            this.description = "Creates a protective stone fortress around you that damages nearby enemies.";
            this.cooldown = java.time.Duration.ofSeconds(40);
            this.spellType = SpellType.EARTH;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new StoneFortress(this);
        }
    }

    private StoneFortress(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "stone-fortress";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        int wallRadius = spellConfig.getInt("values.wall-radius", 5);
        int wallHeight = spellConfig.getInt("values.wall-height", 4);
        double damage = spellConfig.getDouble("values.damage", 6.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 200);
        boolean includeRoof = spellConfig.getBoolean("flags.include-roof", true);

        // Create fortress structure
        new FortressTask(context, player.getLocation(), wallRadius, wallHeight, damage, durationTicks, includeRoof)
                .runTaskTimer(context.plugin(), 0L, 2L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private static class FortressTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final int wallRadius;
        private final int wallHeight;
        private final double damage;
        private final int durationTicks;
        private final boolean includeRoof;
        private final World world;
        private final List<BlockRecord> placedBlocks = new ArrayList<>();
        private final Map<Block, BlockData> originalBlocks = new HashMap<>();
        private int ticks = 0;
        private final int buildDuration = 40; // 2 seconds to build
        private boolean isBuilt = false;

        public FortressTask(SpellContext context, Location center, int wallRadius, int wallHeight, 
                           double damage, int durationTicks, boolean includeRoof) {
            this.context = context;
            this.center = center;
            this.wallRadius = wallRadius;
            this.wallHeight = wallHeight;
            this.damage = damage;
            this.durationTicks = durationTicks;
            this.includeRoof = includeRoof;
            this.world = center.getWorld();
        }

        @Override
        public void run() {
            if (ticks >= durationTicks + buildDuration) {
                this.cancel();
                removeFortress();
                return;
            }

            if (!isBuilt && ticks >= buildDuration) {
                isBuilt = true;
                // Damage nearby enemies when fortress is complete
                damageNearbyEnemies();
            }

            if (!isBuilt) {
                buildFortressProgressively();
            }

            // Periodic effects while fortress exists
            if (isBuilt && ticks % 20 == 0) {
                // Particle effects
                for (int i = 0; i < 36; i++) {
                    double angle = 2 * Math.PI * i / 36;
                    double x = wallRadius * Math.cos(angle);
                    double z = wallRadius * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 1, z);
                    world.spawnParticle(Particle.BLOCK, particleLoc, 3, 0.1, 0.1, 0.1, 0.01, Material.STONE.createBlockData());
                }
                
                // Occasional damage
                if (ticks % 40 == 0) {
                    damageNearbyEnemies();
                }
            }

            ticks++;
        }

        private void buildFortressProgressively() {
            int buildProgress = Math.min(ticks, buildDuration);
            int totalBlocks = (wallRadius * 2 + 1) * wallHeight * 4; // 4 walls
            if (includeRoof) {
                totalBlocks += (wallRadius * 2 + 1) * (wallRadius * 2 + 1); // roof
            }
            
            int blocksToPlace = (int) (totalBlocks * ((double) buildProgress / buildDuration));
            
            // Place wall blocks progressively
            int blocksPlaced = 0;
            
            // Build four walls
            for (int layer = 0; layer < wallHeight && blocksPlaced < blocksToPlace; layer++) {
                for (int x = -wallRadius; x <= wallRadius && blocksPlaced < blocksToPlace; x++) {
                    // Front and back walls (z = -radius and z = radius)
                    placeBlockIfAir(center.getBlockX() + x, center.getBlockY() + layer, center.getBlockZ() - wallRadius);
                    blocksPlaced++;
                    if (blocksPlaced >= blocksToPlace) break;
                    
                    placeBlockIfAir(center.getBlockX() + x, center.getBlockY() + layer, center.getBlockZ() + wallRadius);
                    blocksPlaced++;
                    if (blocksPlaced >= blocksToPlace) break;
                }
                
                for (int z = -wallRadius + 1; z < wallRadius && blocksPlaced < blocksToPlace; z++) {
                    // Left and right walls (x = -radius and x = radius)
                    placeBlockIfAir(center.getBlockX() - wallRadius, center.getBlockY() + layer, center.getBlockZ() + z);
                    blocksPlaced++;
                    if (blocksPlaced >= blocksToPlace) break;
                    
                    placeBlockIfAir(center.getBlockX() + wallRadius, center.getBlockY() + layer, center.getBlockZ() + z);
                    blocksPlaced++;
                    if (blocksPlaced >= blocksToPlace) break;
                }
            }
            
            // Build roof if enabled
            if (includeRoof && blocksPlaced < blocksToPlace) {
                int roofLevel = center.getBlockY() + wallHeight;
                for (int x = -wallRadius; x <= wallRadius && blocksPlaced < blocksToPlace; x++) {
                    for (int z = -wallRadius; z <= wallRadius && blocksPlaced < blocksToPlace; z++) {
                        placeBlockIfAir(center.getBlockX() + x, roofLevel, center.getBlockZ() + z);
                        blocksPlaced++;
                    }
                }
            }
        }

        private void placeBlockIfAir(int x, int y, int z) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isAir() || block.getType() == Material.WATER || block.getType() == Material.LAVA) {
                // Store original block data
                originalBlocks.put(block, block.getBlockData());
                
                // Place stone block
                block.setType(Material.STONE);
                placedBlocks.add(new BlockRecord(block.getLocation(), Material.STONE));
                
                // Particle effect
                world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.1, 0.1, 0.1, 0.01, Material.STONE.createBlockData());
            }
        }

        private void damageNearbyEnemies() {
            for (LivingEntity entity : world.getNearbyLivingEntities(center, wallRadius + 2, wallHeight + 2, wallRadius + 2)) {
                if (entity instanceof Player && entity.equals(context.caster())) continue;
                if (entity.isDead() || !entity.isValid()) continue;
                
                entity.damage(damage, context.caster());
                
                // Knockback away from center
                Vector knockback = entity.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8);
                knockback.setY(0.3);
                entity.setVelocity(knockback);
                
                // Particle effect
                world.spawnParticle(Particle.BLOCK, entity.getLocation(), 10, 0.3, 0.3, 0.3, 0.01, Material.STONE.createBlockData());
            }
            
            // Sound effect
            world.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
        }

        private void removeFortress() {
            // Remove placed blocks and restore original blocks
            for (BlockRecord record : placedBlocks) {
                Block block = world.getBlockAt(record.location);
                if (block.getType() == record.material) {
                    BlockData original = originalBlocks.get(block);
                    if (original != null) {
                        block.setBlockData(original);
                    } else {
                        block.setType(Material.AIR);
                    }
                    
                    // Particle effect
                    world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.1, 0.1, 0.1, 0.01, record.material.createBlockData());
                }
            }
            
            // Sound effect
            world.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
        }

        private static class BlockRecord {
            final Location location;
            final Material material;
            
            BlockRecord(Location location, Material material) {
                this.location = location;
                this.material = material;
            }
        }
    }
}