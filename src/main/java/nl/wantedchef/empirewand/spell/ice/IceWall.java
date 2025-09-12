package nl.wantedchef.empirewand.spell.ice;

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
 * IceWall - Creates a protective ice wall
 */
public class IceWall extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Ice Wall";
            this.description = "Create a protective wall of ice";
            this.cooldown = Duration.ofSeconds(20);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new IceWall(this);
        }
    }

    private static final int DEFAULT_WIDTH = 7;
    private static final int DEFAULT_HEIGHT = 4;
    private static final int DEFAULT_DURATION_TICKS = 200;

    private IceWall(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "icewall";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(12);
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
                    block.setType(Material.PACKED_ICE);
                    wallBlocks.add(block);
                }
            }
        }
        
        // Effects
        context.fx().playSound(start, Sound.BLOCK_GLASS_PLACE, 2.0f, 0.5f);
        start.getWorld().spawnParticle(Particle.SNOWFLAKE, start, 50, width/2.0, height/2.0, 0.5, 0.1);
        
        player.sendMessage("§b§lIce Wall §3created for " + (duration/20) + " seconds!");
        
        // Remove wall after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : wallBlocks) {
                    if (block.getType() == Material.PACKED_ICE) {
                        block.setType(Material.AIR);
                        block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation(), 5, 0.2, 0.2, 0.2, 0, Material.ICE.createBlockData());
                    }
                }
                context.fx().playSound(start, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            }
        }.runTaskLater(context.plugin(), duration);
    }
}
