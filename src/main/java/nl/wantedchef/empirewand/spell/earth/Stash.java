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
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Stash - Creates temporary storage from real Empirewand
 */
public class Stash extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Stash";
            this.description = "Create a temporary chest for storage";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.EARTH;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Stash(this);
        }
    }

    private static final int DEFAULT_DURATION_TICKS = 600;

    private Stash(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "stash";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(5);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        
        Location chestLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        Block block = chestLoc.getBlock();
        
        if (block.getType() != Material.AIR) {
            player.sendMessage("§cCannot place stash here!");
            return;
        }
        
        // Create chest
        block.setType(Material.CHEST);
        
        // Effects
        context.fx().playSound(chestLoc, Sound.BLOCK_WOOD_PLACE, 1.0f, 1.0f);
        chestLoc.getWorld().spawnParticle(Particle.DUST, chestLoc.add(0.5, 0.5, 0.5), 30, 0.3, 0.3, 0.3, 0.05,
            new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 69, 19), 1.5f));
        
        player.sendMessage("§6§lStash §ecreated for " + (duration/20) + " seconds!");
        
        // Open chest for player
        if (block.getState() instanceof Chest chest) {
            player.openInventory(chest.getInventory());
        }
        
        // Remove chest after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.getType() == Material.CHEST) {
                    // Drop items if any
                    if (block.getState() instanceof Chest chest) {
                        chest.getInventory().forEach(item -> {
                            if (item != null) {
                                block.getWorld().dropItemNaturally(block.getLocation(), item);
                            }
                        });
                    }
                    
                    block.setType(Material.AIR);
                    block.getWorld().spawnParticle(Particle.BLOCK_DUST, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0,
                        Material.CHEST.createBlockData());
                    context.fx().playSound(block.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.8f, 1.0f);
                }
            }
        }.runTaskLater(context.plugin(), duration);
    }
}
