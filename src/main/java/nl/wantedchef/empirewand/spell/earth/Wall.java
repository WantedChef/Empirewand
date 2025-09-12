package nl.wantedchef.empirewand.spell.earth;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Wall - Creates temporary barrier from real Empirewand
 */
public class Wall extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Wall";
            this.description = "Create a protective stone wall";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.EARTH;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Wall(this);
        }
    }

    private static final int DEFAULT_WIDTH = 7;
    private static final int DEFAULT_HEIGHT = 5;
    private static final int DEFAULT_DURATION_TICKS = 300;

    private Wall(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "wall";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(10);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int width = spellConfig.getInt("values.width", DEFAULT_WIDTH);
        int height = spellConfig.getInt("values.height", DEFAULT_HEIGHT);
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        
        Location start = player.getLocation().add(player.getLocation().getDirection().multiply(3));
        Vector direction = player.getLocation().getDirection();
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        
        List<Block> wallBlocks = new ArrayList<>();
        
        // Create the wall
        for (int w = -width/2; w <= width/2; w++) {
            for (int h = 0; h < height; h++) {
                Location blockLoc = start.clone().add(perpendicular.clone().multiply(w)).add(0, h, 0);
                Block block = blockLoc.getBlock();
                if (block.getType() == Material.AIR) {
                    block.setType(Material.STONE_BRICKS);
                    wallBlocks.add(block);
                }
            }
        }
        
        // Effects
        context.fx().playSound(start, Sound.BLOCK_STONE_PLACE, 2.0f, 0.5f);
        start.getWorld().spawnParticle(Particle.DUST, start, 50, width/2.0, height/2.0, 0.5, 0.1,
            new Particle.DustOptions(org.bukkit.Color.GRAY, 2.0f));
        
        player.sendMessage("§7§lStone Wall §8erected for " + (duration/20) + " seconds!");
        
        // Remove wall after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : wallBlocks) {
                    if (block.getType() == Material.STONE_BRICKS) {
                        block.setType(Material.AIR);
                        block.getWorld().spawnParticle(Particle.DUST, block.getLocation(), 5, 0.2, 0.2, 0.2, 0, 
                            Material.STONE_BRICKS.createBlockData());
                    }
                }
                context.fx().playSound(start, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
            }
        }.runTaskLater(context.plugin(), duration);
    }
}
