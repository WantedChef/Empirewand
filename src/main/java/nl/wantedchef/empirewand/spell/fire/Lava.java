package nl.wantedchef.empirewand.spell.fire;

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
 * Lava - Summons temporary lava pools from real Empirewand
 */
public class Lava extends Spell<Location> {

    public static class Builder extends Spell.Builder<Location> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lava";
            this.description = "Summon a pool of lava at target location";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Location> build() {
            return new Lava(this);
        }
    }

    private static final int DEFAULT_RADIUS = 3;
    private static final int DEFAULT_DURATION_TICKS = 100;
    private static final double DEFAULT_RANGE = 30.0;

    private Lava(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "lava";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(25);
    }

    @Override
    protected Location executeSpell(@NotNull SpellContext context) {
        Player player = context.caster();
        double range = spellConfig.getDouble("values.range", DEFAULT_RANGE);
        
        Block targetBlock = player.getTargetBlock(null, (int) range);
        if (targetBlock.getType() == Material.AIR) {
            player.sendMessage("§cYou must target a solid block!");
            return null;
        }
        
        return targetBlock.getLocation();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Location location) {
        if (location == null) return;
        
        Player player = context.caster();
        int radius = spellConfig.getInt("values.radius", DEFAULT_RADIUS);
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        
        List<Block> lavaBlocks = new ArrayList<>();
        Location center = location.clone().add(0, 1, 0);
        
        // Create lava pool
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    Block block = center.clone().add(x, 0, z).getBlock();
                    if (block.getType() == Material.AIR || block.getType().name().contains("GRASS") || block.getType().name().contains("FLOWER")) {
                        lavaBlocks.add(block);
                        block.setType(Material.LAVA);
                    }
                }
            }
        }
        
        // Effects
        location.getWorld().spawnParticle(Particle.LAVA, center, 50, radius, 0.5, radius, 0);
        context.fx().playSound(center, Sound.BLOCK_LAVA_AMBIENT, 2.0f, 0.8f);
        context.fx().playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.5f);
        
        player.sendMessage("§6§lLava §esummoned! It will disappear in " + (duration/20) + " seconds.");
        
        // Remove lava after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : lavaBlocks) {
                    if (block.getType() == Material.LAVA) {
                        block.setType(Material.AIR);
                        block.getWorld().spawnParticle(Particle.SMOKE, block.getLocation(), 5, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }
        }.runTaskLater(context.plugin(), duration);
    }
}
