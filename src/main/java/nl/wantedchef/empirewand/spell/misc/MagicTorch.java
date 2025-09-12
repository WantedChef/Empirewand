package nl.wantedchef.empirewand.spell.misc;

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
 * MagicTorch - Creates magical light sources from real Empirewand
 */
public class MagicTorch extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Magic Torch";
            this.description = "Create magical torches that light up the area";
            this.cooldown = Duration.ofSeconds(5);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new MagicTorch(this);
        }
    }

    private static final int DEFAULT_DURATION = 600;
    private static final int DEFAULT_RADIUS = 3;
    private static final boolean DEFAULT_FOLLOW_PLAYER = false;

    private MagicTorch(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "magictorch";
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
        int duration = spellConfig.getInt("values.duration", DEFAULT_DURATION);
        int radius = spellConfig.getInt("values.radius", DEFAULT_RADIUS);
        boolean followPlayer = spellConfig.getBoolean("flags.follow_player", DEFAULT_FOLLOW_PLAYER);
        
        if (followPlayer) {
            // Create floating torch that follows player
            player.sendMessage("§e§lMagic Torch §6will follow you for " + (duration/20) + " seconds!");
            
            new BukkitRunnable() {
                int ticks = 0;
                List<Block> lightBlocks = new ArrayList<>();
                
                @Override
                public void run() {
                    if (ticks >= duration || !player.isOnline()) {
                        // Remove light blocks
                        lightBlocks.forEach(block -> {
                            if (block.getType() == Material.LIGHT) {
                                block.setType(Material.AIR);
                            }
                        });
                        player.sendMessage("§7Magic torch has faded.");
                        cancel();
                        return;
                    }
                    
                    // Clear old light blocks
                    lightBlocks.forEach(block -> {
                        if (block.getType() == Material.LIGHT) {
                            block.setType(Material.AIR);
                        }
                    });
                    lightBlocks.clear();
                    
                    // Create light around player
                    Location playerLoc = player.getLocation();
                    for (int x = -radius; x <= radius; x++) {
                        for (int y = -1; y <= 2; y++) {
                            for (int z = -radius; z <= radius; z++) {
                                if (x * x + z * z <= radius * radius) {
                                    Block block = playerLoc.clone().add(x, y, z).getBlock();
                                    if (block.getType() == Material.AIR) {
                                        block.setType(Material.LIGHT);
                                        lightBlocks.add(block);
                                    }
                                }
                            }
                        }
                    }
                    
                    // Floating torch particle effect
                    Location torchLoc = player.getLocation().add(0, 2.5, 0);
                    torchLoc.getWorld().spawnParticle(Particle.FLAME, torchLoc, 3, 0.05, 0.05, 0.05, 0.01);
                    torchLoc.getWorld().spawnParticle(Particle.LAVA, torchLoc, 1, 0.1, 0.1, 0.1, 0);
                    
                    ticks += 10;
                }
            }.runTaskTimer(context.plugin(), 0L, 10L);
        } else {
            // Create stationary magic torches
            Location targetLoc = player.getTargetBlock(null, 20).getLocation();
            List<Block> torchBlocks = new ArrayList<>();
            
            // Place magic torches in pattern
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    
                    Location torchLoc = targetLoc.clone().add(x * 2, 1, z * 2);
                    Block block = torchLoc.getBlock();
                    
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.TORCH);
                        torchBlocks.add(block);
                        
                        // Light blocks around torch
                        for (int lx = -1; lx <= 1; lx++) {
                            for (int lz = -1; lz <= 1; lz++) {
                                Block lightBlock = torchLoc.clone().add(lx, 0, lz).getBlock();
                                if (lightBlock.getType() == Material.AIR) {
                                    lightBlock.setType(Material.LIGHT);
                                    torchBlocks.add(lightBlock);
                                }
                            }
                        }
                    }
                }
            }
            
            // Effects
            targetLoc.getWorld().spawnParticle(Particle.END_ROD, targetLoc.add(0, 1, 0), 20, 1, 0.5, 1, 0.05);
            context.fx().playSound(targetLoc, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.5f);
            
            player.sendMessage("§e§lMagic Torches §6placed for " + (duration/20) + " seconds!");
            
            // Remove after duration
            new BukkitRunnable() {
                @Override
                public void run() {
                    torchBlocks.forEach(block -> {
                        if (block.getType() == Material.TORCH || block.getType() == Material.LIGHT) {
                            block.setType(Material.AIR);
                            block.getWorld().spawnParticle(Particle.SMOKE, block.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
                        }
                    });
                    context.fx().playSound(targetLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
                }
            }.runTaskLater(context.plugin(), duration);
        }
    }
}
