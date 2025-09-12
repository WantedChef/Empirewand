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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Random;

/**
 * Lifewalk - Flowers bloom in your footsteps from real Empirewand
 */
public class Lifewalk extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lifewalk";
            this.description = "Flowers bloom and life flourishes in your footsteps";
            this.cooldown = Duration.ofSeconds(10);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Lifewalk(this);
        }
    }

    private static final int DEFAULT_DURATION_TICKS = 200;
    private static final double DEFAULT_RADIUS = 2.0;
    private static final Material[] FLOWERS = {
        Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID,
        Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP,
        Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP,
        Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY
    };

    private Lifewalk(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "lifewalk";
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
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        
        context.fx().playSound(player, Sound.BLOCK_GRASS_PLACE, 1.0f, 1.5f);
        player.sendMessage("§a§lLifewalk §2activated! Nature blooms in your wake for " + (duration/20) + " seconds!");
        
        new BukkitRunnable() {
            private int ticks = 0;
            private final Random random = new Random();
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    player.sendMessage("§7Lifewalk has ended.");
                    cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                
                // Nature particles
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 5, 0.5, 0.1, 0.5, 0);
                loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 3, 0.3, 0.1, 0.3, 0);
                
                // Grow plants around player
                for (double x = -radius; x <= radius; x += 0.5) {
                    for (double z = -radius; z <= radius; z += 0.5) {
                        if (x * x + z * z <= radius * radius) {
                            Location blockLoc = loc.clone().add(x, -1, z);
                            Block ground = blockLoc.getBlock();
                            Block above = blockLoc.clone().add(0, 1, 0).getBlock();
                            
                            // Convert dirt to grass
                            if (ground.getType() == Material.DIRT) {
                                ground.setType(Material.GRASS_BLOCK);
                            }
                            
                            // Place flowers
                            if (above.getType() == Material.AIR && ground.getType().isSolid() && random.nextInt(4) == 0) {
                                above.setType(FLOWERS[random.nextInt(FLOWERS.length)]);
                            }
                            
                            // Grow crops
                            if (above.getBlockData() instanceof Ageable ageable) {
                                ageable.setAge(ageable.getMaximumAge());
                                above.setBlockData(ageable);
                            }
                        }
                    }
                }
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
    }
}
