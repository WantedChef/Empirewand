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
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Pollinate - Nature growth spell from real Empirewand
 */
public class Pollinate extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Pollinate";
            this.description = "Instantly grow crops and plants around you";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Pollinate(this);
        }
    }

    private static final double DEFAULT_RADIUS = 10.0;

    private Pollinate(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "pollinate";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(8);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }
    
    private org.bukkit.TreeType getTreeType(Material sapling) {
        return switch (sapling) {
            case OAK_SAPLING -> org.bukkit.TreeType.TREE;
            case SPRUCE_SAPLING -> org.bukkit.TreeType.REDWOOD;
            case BIRCH_SAPLING -> org.bukkit.TreeType.BIRCH;
            case JUNGLE_SAPLING -> org.bukkit.TreeType.JUNGLE;
            case ACACIA_SAPLING -> org.bukkit.TreeType.ACACIA;
            case DARK_OAK_SAPLING -> org.bukkit.TreeType.DARK_OAK;
            default -> org.bukkit.TreeType.TREE;
        };
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        
        Location center = player.getLocation();
        int growthCount = 0;
        
        // Grow all crops and plants in radius
        for (int x = (int) -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = (int) -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        Block block = center.clone().add(x, y, z).getBlock();
                        
                        // Grow crops
                        if (block.getBlockData() instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) {
                            ageable.setAge(ageable.getMaximumAge());
                            block.setBlockData(ageable);
                            growthCount++;
                            
                            // Particle effect
                            block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, 
                                block.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0);
                        }
                        
                        // Convert saplings to trees (with small chance)
                        if (block.getType().name().contains("SAPLING") && Math.random() < 0.3) {
                            Material saplingType = block.getType();
                            block.setType(Material.AIR);
                            block.getWorld().generateTree(block.getLocation(), getTreeType(saplingType));
                            growthCount++;
                        }
                    }
                }
            }
        }
        
        // Effects
        center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center, 100, radius/2, 2, radius/2, 0.1);
        center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 50, radius/2, 2, radius/2, 0);
        context.fx().playSound(center, Sound.ITEM_BONE_MEAL_USE, 2.0f, 1.0f);
        context.fx().playSound(center, Sound.BLOCK_GRASS_PLACE, 1.5f, 1.2f);
        
        player.sendMessage("§a§lPollinate §2cast! " + growthCount + " plants grown!");
    }
}
