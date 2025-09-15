package nl.wantedchef.empirewand.spell.life;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

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
            private final double radiusSq = radius * radius;
            private int flowersPlacedThisTick = 0;
            private int cropsGrownThisTick = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    player.sendMessage("§7Lifewalk has ended.");
                    cancel();
                    return;
                }

                final ThreadLocalRandom tlr = ThreadLocalRandom.current();
                final Location loc = player.getLocation();
                final var world = loc.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }

                // Flower illusion particles instead of spawning entities
                if (tlr.nextInt(3) == 0) { // ~33% chance each tick
                    Material flowerType = FLOWERS[tlr.nextInt(FLOWERS.length)];
                    ItemStack flowerItem = new ItemStack(flowerType);
                    Location dropLoc = loc.clone().add((tlr.nextDouble() - 0.5) * 2, 0.5,
                            (tlr.nextDouble() - 0.5) * 2);
                    context.fx().spawnParticles(dropLoc, org.bukkit.Particle.ITEM, 3, 0.1, 0.1, 0.1, 0, flowerItem);
                }

                // Grow plants around player (lightweight, capped per tick)
                final int baseX = loc.getBlockX();
                final int baseY = loc.getBlockY();
                final int baseZ = loc.getBlockZ();
                final int intRadius = (int) Math.ceil(radius);
                flowersPlacedThisTick = 0;
                cropsGrownThisTick = 0;

                for (int dx = -intRadius; dx <= intRadius; dx++) {
                    for (int dz = -intRadius; dz <= intRadius; dz++) {
                        if ((dx * dx + dz * dz) > radiusSq) continue;

                        Block ground = world.getBlockAt(baseX + dx, baseY - 1, baseZ + dz);
                        Block above = world.getBlockAt(baseX + dx, baseY, baseZ + dz);

                        // Convert dirt to grass
                        if (ground.getType() == Material.DIRT) {
                            ground.setType(Material.GRASS_BLOCK, false);
                        }

                        // Place flowers (cap per tick to avoid heavy block updates)
                        if (flowersPlacedThisTick < 3 && above.getType() == Material.AIR && ground.getType().isSolid()
                                && tlr.nextInt(6) == 0) {
                            above.setType(FLOWERS[tlr.nextInt(FLOWERS.length)], false);
                            flowersPlacedThisTick++;
                        }

                        // Grow crops to max age
                        var data = above.getBlockData();
                        if (cropsGrownThisTick < 6 && data instanceof Ageable ageable) {
                            if (ageable.getAge() < ageable.getMaximumAge()) {
                                ageable.setAge(ageable.getMaximumAge());
                                above.setBlockData(ageable, false);
                                cropsGrownThisTick++;
                            }
                        }
                    }
                }

                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
    }
}
