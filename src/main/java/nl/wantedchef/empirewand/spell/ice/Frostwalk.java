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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Frostwalk - Freeze water as you walk and slow enemies
 * Real Empirewand spell
 */
public class Frostwalk extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Frostwalk";
            this.description = "Freeze water beneath your feet and slow nearby enemies";
            this.cooldown = Duration.ofSeconds(10);
            this.spellType = SpellType.ICE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Frostwalk(this);
        }
    }

    private static final int DEFAULT_DURATION_TICKS = 200;
    private static final double DEFAULT_RADIUS = 3.0;
    private static final int DEFAULT_SLOW_AMPLIFIER = 2;

    private Frostwalk(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "frostwalk";
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
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        int slowAmplifier = spellConfig.getInt("values.slow_amplifier", DEFAULT_SLOW_AMPLIFIER);
        
        context.fx().playSound(player, Sound.BLOCK_GLASS_BREAK, 1.0f, 2.0f);
        player.sendMessage("§b§lFrostwalk §3activated for " + (duration/20) + " seconds!");
        
        new BukkitRunnable() {
            private int ticks = 0;
            private final Set<Block> iceBlocks = new HashSet<>();
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    // Revert ice blocks
                    iceBlocks.forEach(block -> {
                        if (block.getType() == Material.ICE || block.getType() == Material.PACKED_ICE) {
                            block.setType(Material.WATER);
                        }
                    });
                    player.sendMessage("§7Frostwalk has ended.");
                    cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                
                // Frost particles
                loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 8, 0.5, 0.1, 0.5, 0.02);
                loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 5, 0.3, 0.1, 0.3, 0.01);
                
                // Freeze water blocks around player
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        for (int y = -1; y <= 0; y++) {
                            Block block = loc.clone().add(x, y, z).getBlock();
                            if (block.getType() == Material.WATER) {
                                block.setType(Material.PACKED_ICE);
                                iceBlocks.add(block);
                            }
                        }
                    }
                }
                
                // Slow nearby enemies
                for (var entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                    if (entity instanceof LivingEntity living && !living.equals(player)) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, slowAmplifier));
                        living.getWorld().spawnParticle(Particle.FALLING_DUST, living.getLocation(), 3, 0.2, 0.2, 0.2, 0);
                    }
                }
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
    }
}
