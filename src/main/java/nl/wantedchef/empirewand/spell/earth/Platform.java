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
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Platform - Creates a platform to stand on from real Empirewand
 */
public class Platform extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Platform";
            this.description = "Create a platform beneath your feet";
            this.cooldown = Duration.ofSeconds(5);
            this.spellType = SpellType.EARTH;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Platform(this);
        }
    }

    private static final int DEFAULT_SIZE = 5;
    private static final int DEFAULT_DURATION_TICKS = 400;

    private Platform(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "platform";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int size = spellConfig.getInt("values.size", DEFAULT_SIZE);
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        
        Location center = player.getLocation().subtract(0, 1, 0);
        List<Block> platformBlocks = new ArrayList<>();
        
        // Create platform
        for (int x = -size/2; x <= size/2; x++) {
            for (int z = -size/2; z <= size/2; z++) {
                Location blockLoc = center.clone().add(x, 0, z);
                Block block = blockLoc.getBlock();
                if (block.getType() == Material.AIR) {
                    block.setType(Material.STONE);
                    platformBlocks.add(block);
                }
            }
        }
        
        // Effects
        context.fx().playSound(center, Sound.BLOCK_STONE_PLACE, 1.5f, 1.0f);
        center.getWorld().spawnParticle(Particle.DUST, center.add(0, 1, 0), 30, size/2.0, 0.2, size/2.0, 0.05,
            new Particle.DustOptions(org.bukkit.Color.GRAY, 1.5f));
        
        player.sendMessage("§7§lPlatform §8created for " + (duration/20) + " seconds!");
        
        // Remove platform after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : platformBlocks) {
                    if (block.getType() == Material.STONE) {
                        block.setType(Material.AIR);
                        block.getWorld().spawnParticle(Particle.DUST, block.getLocation(), 3, 0.2, 0.1, 0.2, 0,
                            new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(169, 169, 169), 1.0f)); // Stone gray dust
                    }
                }
                context.fx().playSound(center, Sound.BLOCK_STONE_BREAK, 0.8f, 1.2f);
            }
        }.runTaskLater(context.plugin(), duration);
    }
}
