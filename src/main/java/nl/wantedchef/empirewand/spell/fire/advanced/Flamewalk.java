package nl.wantedchef.empirewand.spell.fire.advanced;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Flamewalk - Burn enemies around you as you walk
 * Real Empirewand spell that creates a trail of fire
 */
public class Flamewalk extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Flamewalk";
            this.description = "Burn enemies around you as you walk, leaving a trail of fire";
            this.cooldown = Duration.ofSeconds(15);
            this.spellType = SpellType.FIRE;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Flamewalk(this);
        }
    }

    private static final int DEFAULT_DURATION_TICKS = 200; // 10 seconds
    private static final double DEFAULT_RADIUS = 3.0;
    private static final double DEFAULT_DAMAGE = 2.0;
    private static final int DEFAULT_FIRE_TICKS = 60;

    private Flamewalk(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "flamewalk";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(15);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int duration = spellConfig.getInt("values.duration_ticks", DEFAULT_DURATION_TICKS);
        double radius = spellConfig.getDouble("values.radius", DEFAULT_RADIUS);
        double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
        int fireTicks = spellConfig.getInt("values.fire_ticks", DEFAULT_FIRE_TICKS);

        context.fx().playSound(player, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);
        player.sendMessage("§6§lFlamewalk activated! §eLeaving a trail of fire for " + (duration/20) + " seconds!");

        new BukkitRunnable() {
            private int ticks = 0;
            private final Set<Block> fireBlocks = new HashSet<>();
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    // Clean up fire blocks
                    fireBlocks.forEach(block -> {
                        if (block.getType() == Material.FIRE) {
                            block.setType(Material.AIR);
                        }
                    });
                    player.sendMessage("§7Flamewalk has ended.");
                    cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                
                // Create fire particles
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 10, 0.5, 0.1, 0.5, 0.02);
                loc.getWorld().spawnParticle(Particle.LAVA, loc, 2, 0.5, 0.1, 0.5, 0);
                
                // Set fire on ground where player walks
                Block ground = loc.clone().subtract(0, 1, 0).getBlock();
                if (ground.getType().isSolid() && loc.getBlock().getType() == Material.AIR) {
                    loc.getBlock().setType(Material.FIRE);
                    fireBlocks.add(loc.getBlock());
                }
                
                // Damage and ignite nearby enemies
                for (var entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                    if (entity instanceof LivingEntity living && !living.equals(player)) {
                        living.damage(damage, player);
                        living.setFireTicks(fireTicks);
                    }
                }
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
    }
}
