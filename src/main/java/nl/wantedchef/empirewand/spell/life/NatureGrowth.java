package nl.wantedchef.empirewand.spell.life;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Random;

/**
 * NatureGrowth - Explosive nature growth
 */
public class NatureGrowth extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Nature Growth";
            this.description = "Cause explosive growth of nature in an area";
            this.cooldown = Duration.ofSeconds(25);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new NatureGrowth(this);
        }
    }

    private static final double DEFAULT_RADIUS = 15.0;
    private static final Material[] NATURE_BLOCKS = {
        Material.GRASS_BLOCK, Material.MOSS_BLOCK, Material.AZALEA_LEAVES,
        Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES
    };

    private NatureGrowth(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "naturegrowth";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(20);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        
        Location center = player.getLocation();
        Random random = new Random();
        
        // Transform area into lush nature
        for (int x = (int) -radius; x <= radius; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = (int) -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        Block block = center.clone().add(x, y, z).getBlock();
                        
                        // Convert stone/dirt to nature blocks
                        if ((block.getType() == Material.STONE || block.getType() == Material.DIRT || 
                             block.getType() == Material.SAND) && random.nextInt(3) == 0) {
                            block.setType(NATURE_BLOCKS[random.nextInt(NATURE_BLOCKS.length)]);
                        }
                        
                        // Add vines
                        if (block.getType().isSolid() && random.nextInt(5) == 0) {
                            Block side = block.getRelative(org.bukkit.block.BlockFace.NORTH);
                            if (side.getType() == Material.AIR) {
                                side.setType(Material.VINE);
                            }
                        }
                        
                        // Add tall grass and flowers
                        if (block.getType() == Material.AIR && 
                            block.getRelative(org.bukkit.block.BlockFace.DOWN).getType().isSolid() && 
                            random.nextInt(3) == 0) {
                            block.setType(random.nextBoolean() ? Material.TALL_GRASS : Material.FERN);
                        }
                    }
                }
            }
        }
        
        // Generate some trees
        for (int i = 0; i < 5; i++) {
            Location treeLoc = center.clone().add(
                random.nextInt((int)radius * 2) - radius,
                0,
                random.nextInt((int)radius * 2) - radius
            );
            Block ground = treeLoc.getBlock();
            if (ground.getType() == Material.GRASS_BLOCK || ground.getType() == Material.DIRT) {
                treeLoc.getWorld().generateTree(treeLoc.add(0, 1, 0), org.bukkit.TreeType.BIG_TREE);
            }
        }
        
        // Effects
        center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center, 200, radius/2, 3, radius/2, 0.1);
        center.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, center, 100, radius/2, 3, radius/2, 0);
        context.fx().playSound(center, Sound.BLOCK_CHORUS_FLOWER_GROW, 2.0f, 0.8f);
        context.fx().playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 2.0f);
        
        player.sendMessage("§a§lNature Growth §2exploded into life!");
    }
}
