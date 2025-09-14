package nl.wantedchef.empirewand.spell.enhanced.defensive;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * A compact defensive earth spell that summons a 5x4 stone castle with a central tower,
 * providing protection and dealing area-of-effect damage to nearby enemies.
 * <p>
 * This spell creates a small 5x4 castle base with a towering structure, featuring:
 * <ul>
 *   <li>Compact 5x4 base with 2-block-thick walls and a central tower (up to 8 blocks high)</li>
 *   <li>Optional crenelated battlements for a castle aesthetic</li>
 *   <li>Multi-phase enemy damage: initial quake, periodic spikes, and collapse burst</li>
 *   <li>Knockback with directional push for tactical control</li>
 *   <li>Immersive VFX: dust clouds, enchanted runes, and tremor effects</li>
 *   <li>Player buffs: minor regeneration and resistance inside the castle</li>
 *   <li>Optimized cleanup with crumble animation and block restoration</li>
 *   <li>Audio effects: stone grinding, battle cries, and collapse rumbles</li>
 *   <li>Performance optimizations: batched block placement, efficient spatial queries</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell stoneCastle = new StoneFortress.Builder(api)
 *     .name("Stone Castle")
 *     .description("Summons a compact stone castle with a tower to defend and dominate.")
 *     .cooldown(Duration.ofSeconds(30))
 *     .towerHeight(8)
 *     .enableBattlements(true)
 *     .build();
 * }</pre>
 *
 * @since 2.1.0
 */
public class StoneFortress extends Spell<Void> {

    /**
     * Builder for configuring the Stone Castle spell.
     */
    public static class Builder extends Spell.Builder<Void> {
        private int towerHeight = 8;
        private double baseDamage = 6.0;
        private int durationTicks = 300; // 15 seconds
        private boolean enableBattlements = true;
        private boolean enablePlayerBuffs = true;
        private Material primaryMaterial = Material.STONE_BRICKS;
        private Material accentMaterial = Material.MOSSY_STONE_BRICKS;

        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Stone Castle";
            this.description = "Summons a compact stone castle with a tower to defend and dominate.";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.EARTH;
        }

        public Builder towerHeight(int height) { this.towerHeight = Math.max(4, height); return this; }
        public Builder baseDamage(double damage) { this.baseDamage = Math.max(1.0, damage); return this; }
        public Builder durationTicks(int ticks) { this.durationTicks = Math.max(100, ticks); return this; }
        public Builder enableBattlements(boolean enable) { this.enableBattlements = enable; return this; }
        public Builder enablePlayerBuffs(boolean enable) { this.enablePlayerBuffs = enable; return this; }
        public Builder primaryMaterial(Material mat) { this.primaryMaterial = Objects.requireNonNull(mat); return this; }
        public Builder accentMaterial(Material mat) { this.accentMaterial = Objects.requireNonNull(mat); return this; }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new StoneFortress(this);
        }
    }

    private final int towerHeight;
    private final double baseDamage;
    private final int durationTicks;
    private final boolean enableBattlements;
    private final boolean enablePlayerBuffs;
    private final Material primaryMaterial;
    private final Material accentMaterial;

    private StoneFortress(@NotNull Builder builder) {
        super(builder);
        this.towerHeight = builder.towerHeight;
        this.baseDamage = builder.baseDamage;
        this.durationTicks = builder.durationTicks;
        this.enableBattlements = builder.enableBattlements;
        this.enablePlayerBuffs = builder.enablePlayerBuffs;
        this.primaryMaterial = builder.primaryMaterial;
        this.accentMaterial = builder.accentMaterial;
    }

    @Override
    @NotNull
    public String key() {
        return "stone-castle";
    }

    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        Player player = context.caster();
        context.plugin().getTaskManager().runTaskTimer(
                new CastleTask(context, player.getLocation(), towerHeight, baseDamage,
                        durationTicks, enableBattlements, enablePlayerBuffs, primaryMaterial, accentMaterial),
                0L, 1L
        );
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects managed in CastleTask
    }

    /**
     * Task to manage the construction, maintenance, and destruction of the stone castle.
     */
    private static class CastleTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final int towerHeight;
        private final double baseDamage;
        private final int durationTicks;
        private final boolean enableBattlements;
        private final boolean enablePlayerBuffs;
        private final Material primaryMaterial;
        private final Material accentMaterial;
        private final World world;
        private final Set<Location> buildPositions = new LinkedHashSet<>();
        private final Map<Location, BlockData> originalBlocks = new HashMap<>();
        private final Queue<Location> pendingPlacements = new ArrayDeque<>();
        private int ticks = 0;
        private final int buildTicks = 40; // 2 seconds for build
        private boolean isBuilt = false;
        private final Random random = ThreadLocalRandom.current();

        public CastleTask(@NotNull SpellContext context, @NotNull Location center, int towerHeight,
                          double baseDamage, int durationTicks, boolean enableBattlements,
                          boolean enablePlayerBuffs, Material primaryMaterial, Material accentMaterial) {
            this.context = Objects.requireNonNull(context);
            this.center = Objects.requireNonNull(center).clone();
            this.towerHeight = towerHeight;
            this.baseDamage = baseDamage;
            this.durationTicks = durationTicks;
            this.enableBattlements = enableBattlements;
            this.enablePlayerBuffs = enablePlayerBuffs;
            this.primaryMaterial = primaryMaterial;
            this.accentMaterial = accentMaterial;
            this.world = center.getWorld();
            precomputeBuildPositions();
            pendingPlacements.addAll(buildPositions);
        }

        @Override
        public void run() {
            if (world == null) {
                cancel();
                return;
            }

            if (ticks >= durationTicks + buildTicks) {
                cancel();
                collapseCastle();
                return;
            }

            if (!isBuilt && ticks >= buildTicks) {
                isBuilt = true;
                unleashEarthQuake();
            }

            if (!isBuilt) {
                constructCastle();
            } else {
                maintainCastle();
            }

            if (enablePlayerBuffs) {
                buffCasterInside();
            }

            ticks++;
        }

        /**
         * Precomputes positions for a 5x4 castle base with a central tower.
         * <p>
         * Base is 5x4 (outer dimensions), walls 2 blocks thick, tower 3x3 at center.
         */
        private void precomputeBuildPositions() {
            int width = 5, depth = 4; // Outer dimensions
            int innerWidth = 3, innerDepth = 2; // Inner space
            int baseHeight = 4; // Base walls height

            // Base walls (2 blocks thick)
            for (int y = 0; y < baseHeight; y++) {
                // North/South walls (z = -2, -1, 1, 2)
                for (int x = -2; x <= 2; x++) {
                    buildPositions.add(center.clone().add(x, y, -2));
                    buildPositions.add(center.clone().add(x, y, -1));
                    buildPositions.add(center.clone().add(x, y, 1));
                    buildPositions.add(center.clone().add(x, y, 2));
                }
                // East/West walls (x = -2, -1, 1, 2, avoiding corners)
                for (int z = -1; z <= 1; z++) {
                    buildPositions.add(center.clone().add(-2, y, z));
                    buildPositions.add(center.clone().add(-1, y, z));
                    buildPositions.add(center.clone().add(1, y, z));
                    buildPositions.add(center.clone().add(2, y, z));
                }
            }

            // Tower (3x3 centered, from baseHeight to towerHeight)
            for (int y = baseHeight; y < towerHeight; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (Math.abs(x) == 1 || Math.abs(z) == 1) { // Hollow center
                            buildPositions.add(center.clone().add(x, y, z));
                        }
                    }
                }
            }

            // Battlements (optional)
            if (enableBattlements) {
                int battlementHeight = baseHeight;
                for (int x = -2; x <= 2; x += 2) {
                    for (int z = -2; z <= 2; z += 2) {
                        buildPositions.add(center.clone().add(x, battlementHeight, z));
                    }
                }
                // Tower battlements
                battlementHeight = towerHeight;
                for (int x = -1; x <= 1; x += 2) {
                    for (int z = -1; z <= 1; z += 2) {
                        buildPositions.add(center.clone().add(x, battlementHeight, z));
                    }
                }
            }
        }

        /**
         * Constructs the castle progressively with varied materials and VFX.
         */
        private void constructCastle() {
            int placementsThisTick = 6; // Balanced for performance
            for (int i = 0; i < placementsThisTick && !pendingPlacements.isEmpty(); i++) {
                Location pos = pendingPlacements.poll();
                if (pos == null) continue;
                Block block = world.getBlockAt(pos);
                if (block.getType().isAir() || block.getType() == Material.WATER || block.getType() == Material.LAVA) {
                    originalBlocks.put(pos.clone(), block.getBlockData());
                    Material mat = random.nextInt(3) == 0 ? accentMaterial : primaryMaterial; // 1/3 chance for accent
                    block.setType(mat);
                    // Build VFX
                    world.spawnParticle(Particle.BLOCK, pos.clone().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.05, mat.createBlockData());
                    world.spawnParticle(Particle.ENCHANT, pos.clone().add(0.5, 0.5, 0.5), 3, 0.1, 0.1, 0.1, 0.03);
                    if (ticks % 8 == 0) {
                        world.playSound(pos, Sound.BLOCK_STONE_PLACE, 0.9f, 0.8f + random.nextFloat() * 0.4f);
                    }
                }
            }
            if (pendingPlacements.isEmpty()) {
                isBuilt = true;
            }
        }

        /**
         * Initial earthquake: damages and repels enemies.
         */
        private void unleashEarthQuake() {
            damageAndRepelEnemies(1.3 * baseDamage);
            world.playSound(center, Sound.ENTITY_BLAZE_DEATH, 1.2f, 0.7f);
            // Ground tremor particles
            for (int i = 0; i < 15; i++) {
                double angle = 2 * Math.PI * random.nextDouble();
                double dist = 3 * random.nextDouble();
                Location tremor = center.clone().add(dist * Math.cos(angle), 0.1, dist * Math.sin(angle));
                world.spawnParticle(Particle.BLOCK, tremor, 20, 0.5, 0.2, 0.5, 0.1, primaryMaterial.createBlockData());
            }
        }

        /**
         * Maintains castle with periodic damage and atmospheric effects.
         */
        private void maintainCastle() {
            if (ticks % 20 == 0) { // Every second
                damageAndRepelEnemies(0.6 * baseDamage); // Spike damage
                // Rune-like particles
                for (int i = 0; i < 10; i++) {
                    double angle = 2 * Math.PI * random.nextDouble();
                    double dist = 2.5 * random.nextDouble();
                    Location rune = center.clone().add(dist * Math.cos(angle), random.nextDouble() * towerHeight, dist * Math.sin(angle));
                    world.spawnParticle(Particle.WITCH, rune, 2, 0.2, 0.2, 0.2, 0.05);
                }
                world.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 0.9f);
            }
        }

        /**
         * Buffs caster if inside the castle (within 3 blocks of center).
         */
        private void buffCasterInside() {
            Player caster = context.caster();
            if (caster != null && caster.getLocation().distance(center) <= 3) {
                caster.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 0, false, false));
                caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20, 0, false, false));
            }
        }

        /**
         * Damages and repels enemies outward.
         */
        private void damageAndRepelEnemies(double damageMultiplier) {
            double searchRadius = 5;
            world.getNearbyEntities(center, searchRadius, towerHeight + 2, searchRadius).stream()
                    .filter(e -> e instanceof LivingEntity le && !le.equals(context.caster()))
                    .map(e -> (LivingEntity) e)
                    .filter(le -> !le.isDead() && le.isValid())
                    .forEach(le -> {
                        le.damage(baseDamage * damageMultiplier, context.caster());
                        Vector dir = le.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.7).setY(0.4);
                        le.setVelocity(dir);
                        world.spawnParticle(Particle.BLOCK, le.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.1, primaryMaterial.createBlockData());
                    });
        }

        /**
         * Collapses castle with a final damage burst and animated crumble.
         */
        private void collapseCastle() {
            damageAndRepelEnemies(1.8 * baseDamage);
            world.playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 1.3f, 0.6f);
            new BukkitRunnable() {
                final Queue<Location> crumbleQueue = new ArrayDeque<>(originalBlocks.keySet());
                int crumbleTicks = 0;
                final int crumbleDuration = 30;

                @Override
                public void run() {
                    if (crumbleTicks >= crumbleDuration || crumbleQueue.isEmpty()) {
                        restoreBlocks();
                        cancel();
                        return;
                    }
                    int crumblesThisTick = 5;
                    for (int i = 0; i < crumblesThisTick && !crumbleQueue.isEmpty(); i++) {
                        Location pos = crumbleQueue.poll();
                        Block block = world.getBlockAt(pos);
                        if (block.getType() == primaryMaterial || block.getType() == accentMaterial) {
                            world.spawnParticle(Particle.BLOCK, pos.clone().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.1, block.getBlockData());
                            world.playSound(pos, Sound.BLOCK_STONE_BREAK, 0.7f, 0.5f + random.nextFloat() * 0.3f);
                        }
                    }
                    crumbleTicks++;
                }
            }.runTaskTimer(context.plugin(), 0L, 2L);
        }

        /**
         * Restores original blocks after collapse.
         */
        private void restoreBlocks() {
            originalBlocks.forEach((pos, data) -> {
                Block block = world.getBlockAt(pos);
                block.setBlockData(data);
            });
            world.spawnParticle(Particle.EXPLOSION, center, 50, 2, 1, 2, 0.15);
        }
    }
}