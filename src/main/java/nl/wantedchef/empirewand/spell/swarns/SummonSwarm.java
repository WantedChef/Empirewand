package nl.wantedchef.empirewand.spell.swarns;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.util.SpellUtils;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Tameable;
import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Color;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import nl.wantedchef.empirewand.api.service.ConfigService;
import nl.wantedchef.empirewand.core.storage.Keys;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.Objects;

/**
 * A powerful necromancy spell that summons a swarm of minions to fight for you.
 * <p>
 * This spell summons a swarm of vex minions that follow the caster and attack
 * nearby enemies. The minions have configurable health and damage values, and
 * the spell includes visual effects with smoke and enchant particles to enhance
 * the summoning experience. The minions are automatically dismissed when the
 * spell duration expires.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li>Summoning of multiple vex minions</li>
 *   <li>Minions follow the caster and attack enemies</li>
 *   <li>Configurable minion count, health, and damage</li>
 *   <li>Animated particle visual effects</li>
 *   <li>Audio feedback for summoning and dismissal</li>
 *   <li>Automatic cleanup of summoned minions</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Spell summonSwarm = new SummonSwarm.Builder(api)
 *     .name("Summon Swarm")
 *     .description("Summons a swarm of minions to fight for you.")
 *     .cooldown(Duration.ofSeconds(70))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class SummonSwarm extends Spell<Void> {
    protected enum Variant { VEX, SHADOW, STORM, FROST, FLAME, TOXIC, ARCANE, RADIANT, VOID, WIND, STONE }

    /**
     * Builder for creating SummonSwarm spell instances.
     * <p>
     * Provides a fluent API for configuring the summon swarm spell with sensible defaults.
     */
    public static class Builder extends Spell.Builder<Void> {
        /**
         * Creates a new SummonSwarm spell builder.
         *
         * @param api the EmpireWandAPI instance
         * @throws NullPointerException if api is null
         */
        public Builder(@Nullable EmpireWandAPI api) {
            super(api);
            this.name = "Summon Swarm";
            this.description = "Summons a swarm of minions to fight for you.";
            this.cooldown = Duration.ofSeconds(70);
            this.spellType = SpellType.DARK;
        }

        /**
         * Builds and returns a new SummonSwarm spell instance.
         *
         * @return the constructed SummonSwarm spell
         */
        @Override
        @NotNull
        public Spell<Void> build() {
            return new SummonSwarm(this);
        }
    }

    /**
     * Constructs a new SummonSwarm spell instance.
     *
     * @param builder the builder containing spell configuration
     * @throws NullPointerException if builder is null
     */
    protected SummonSwarm(@NotNull Spell.Builder<Void> builder) {
        super(builder);
    }

    /**
     * Returns the unique key for this spell.
     * <p>
     * This key is used for configuration, identification, and event handling.
     *
     * @return the spell key "summon-swarm"
     */
    @Override
    @NotNull
    public String key() {
        return "summon-swarm";
    }

    /**
     * Returns the prerequisites for casting this spell.
     * <p>
     * Currently, this spell has no prerequisites beyond standard casting requirements.
     *
     * @return a no-op prerequisite
     */
    @Override
    @NotNull
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    /**
     * Executes the summon swarm spell logic.
     * <p>
     * This method creates a swarm of vex minions around the caster that follow
     * the caster and attack nearby enemies.
     *
     * @param context the spell context containing caster and target information
     * @return null (this spell produces no effect object)
     */
    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        
        Player player = context.caster();

        // Configuration
        int minionCount = SpellUtils.getConfigInt(spellConfig, "values.minion-count", 8);
        double radius = SpellUtils.getConfigDouble(spellConfig, "values.radius", 8.0);
        int durationTicks = SpellUtils.getConfigInt(spellConfig, "values.duration-ticks", 300);
        double minionHealth = SpellUtils.getConfigDouble(spellConfig, "values.minion-health", 10.0);
        double minionDamage = SpellUtils.getConfigDouble(spellConfig, "values.minion-damage", 4.0);
        Variant variant = getVariant(context);

        // Play summoning sound
        var world = player.getWorld();
        if (world != null) {
            world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 2.0f, 0.8f);
        }

        // Start summoning effect
        BukkitRunnable summonTask = new SummonSwarmTask(this, context, player.getLocation(), minionCount, radius, durationTicks,
                minionHealth, minionDamage, variant);
        context.plugin().getTaskManager().runTaskTimer(summonTask, 0L, 5L);
        return null;
    }

    /**
     * Handles the spell effect after execution.
     * <p>
     * This spell's effects are handled asynchronously through BukkitRunnables.
     *
     * @param context the spell context
     * @param result the result of the spell execution (always null for this spell)
     */
    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    /**
     * A runnable that handles the summon swarm's effects over time.
     * <p>
     * This task manages the summoning of minions, their behavior updates, visual
     * effects, and cleanup when the spell duration expires.
     */
    private static class SummonSwarmTask extends BukkitRunnable {
        private final SummonSwarm owner;
        private final SpellContext context;
        private final Location center;
        private final int minionCount;
        private final double radius;
        private final int durationTicks;
        private final double minionHealth;
        private final double minionDamage;
        private final World world;
        private int ticks = 0;
        private final int maxTicks;
        private final List<Vex> summonedMinions = new ArrayList<>();
        private final Random random = new Random();
        private final Variant variant;

        /**
         * Creates a new SummonSwarmTask instance.
         *
         * @param context the spell context
         * @param center the center location for minion summoning
         * @param minionCount the number of minions to summon
         * @param radius the radius around which to summon minions
         * @param durationTicks the duration of the spell in ticks
         * @param minionHealth the health value for each summoned minion
         * @param minionDamage the damage value for each summoned minion
         */
        public SummonSwarmTask(@NotNull SummonSwarm owner, @NotNull SpellContext context, @NotNull Location center, int minionCount, double radius,
                int durationTicks, double minionHealth, double minionDamage, Variant variant) {
            this.owner = Objects.requireNonNull(owner, "Owner cannot be null");
            this.context = Objects.requireNonNull(context, "Context cannot be null");
            this.center = Objects.requireNonNull(center, "Center location cannot be null");
            this.minionCount = minionCount;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.minionHealth = minionHealth;
            this.minionDamage = minionDamage;
            this.world = center.getWorld();
            this.maxTicks = durationTicks / 5; // Convert to our tick interval
            this.variant = variant != null ? variant : Variant.VEX;
        }

        /**
         * Runs the summon swarm task, summoning minions, updating their behavior,
         * and creating visual effects.
         */
        @Override
        public void run() {
            if (world == null) {
                this.cancel();
                return;
            }
            
            if (ticks >= maxTicks) {
                this.cancel();
                dismissMinions();
                return;
            }

            // Summon minions at the beginning
            if (ticks == 0) {
                summonMinions();
            }

            // Update minion behavior
            updateMinionBehavior();

            // CRITICAL: Emergency caster protection - check every tick
            preventCasterTargeting();

            // Create visual effects
            createVisualEffects();
            
            // Use durationTicks for duration-based effects (e.g., warning when spell is about to end)
            if (ticks >= maxTicks - (durationTicks / 20)) { // Warning in last second
                createDurationWarningEffects();
            }

            ticks++;
        }

        /**
         * Summons the vex minions in a circular pattern around the caster.
         * <p>
         * This method spawns vex minions at calculated positions and applies
         * initial configuration to them.
         */
        private void summonMinions() {
            Player player = context.caster();
            
            for (int i = 0; i < minionCount; i++) {
                // Calculate position in a circle around the player with some randomness
                double angle = 2 * Math.PI * i / minionCount + (random.nextDouble() - 0.5) * 0.5; // Add random variation
                double randomRadius = radius + (random.nextDouble() - 0.5) * 2.0; // Vary radius slightly
                double x = center.getX() + Math.cos(angle) * randomRadius;
                double z = center.getZ() + Math.sin(angle) * randomRadius;
                Location spawnLoc = new Location(world, x, world.getHighestBlockYAt((int)x, (int)z) + 1, z);
                
                // Spawn vex minion (do NOT target player; we handle following via velocity)
                Vex minion = world.spawn(spawnLoc, Vex.class, vex -> {
                    AttributeInstance maxHp = vex.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHp != null) maxHp.setBaseValue(Math.max(1.0, minionHealth));
                    AttributeInstance atk = vex.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                    if (atk != null) atk.setBaseValue(Math.max(1.0, minionDamage));
                    AttributeInstance maxHealthAttr = vex.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : minionHealth;
                    vex.setHealth(Math.max(1.0, Math.min(minionHealth, maxHealth)));
                    vex.setPersistent(false);

                    // CRITICAL: Clear any target immediately after spawn
                    vex.setTarget(null);

                    // Make Vex friendly to the caster - this prevents them from attacking their owner
                    if (vex instanceof Tameable tameable) {
                        tameable.setTamed(true);
                        tameable.setOwner(player);
                    }

                    // Tag minion owner to enforce no friendly-fire on caster
                    try {
                        vex.getPersistentDataContainer().set(
                                Keys.createKey("minion.owner"),
                                Keys.STRING_TYPE.getType(),
                                player.getUniqueId().toString());
                    } catch (Exception ignored) {
                    }
                });
                
                summonedMinions.add(minion);
                
                // Visual effect for summoning (variant themed)
                switch (variant) {
                    case STORM -> world.spawnParticle(Particle.ELECTRIC_SPARK, spawnLoc, 24, 0.5, 0.5, 0.5, 0.15);
                    case FROST -> world.spawnParticle(Particle.SNOWFLAKE, spawnLoc, 24, 0.5, 0.5, 0.5, 0.10);
                    case SHADOW -> {
                        world.spawnParticle(Particle.SMOKE, spawnLoc, 20, 0.5, 0.5, 0.5, 0.10);
                        world.spawnParticle(Particle.SQUID_INK, spawnLoc, 10, 0.3, 0.3, 0.3, 0.02);
                    }
                    case FLAME -> world.spawnParticle(Particle.FLAME, spawnLoc, 24, 0.5, 0.5, 0.5, 0.02);
                    case TOXIC -> world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, spawnLoc, 24, 0.5, 0.5, 0.5, 0.02);
                    case ARCANE -> world.spawnParticle(Particle.ENCHANT, spawnLoc, 16, 0.5, 0.5, 0.5, 0.05);
                    case RADIANT -> world.spawnParticle(Particle.END_ROD, spawnLoc, 18, 0.5, 0.5, 0.5, 0.02);
                    case VOID -> world.spawnParticle(Particle.REVERSE_PORTAL, spawnLoc, 16, 0.5, 0.5, 0.5, 0.01);
                    case WIND -> world.spawnParticle(Particle.CLOUD, spawnLoc, 20, 0.5, 0.5, 0.5, 0.02);
                    case STONE -> world.spawnParticle(Particle.BLOCK, spawnLoc, 10, 0.5, 0.5, 0.5, 0, Material.STONE.createBlockData());
                    case VEX -> world.spawnParticle(Particle.ENCHANT, spawnLoc, 16, 0.5, 0.5, 0.5, 0.05);
                }
                owner.onMinionSpawn(minion, context);
            }
            
            // Sound effect
            switch (variant) {
                case STORM -> world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 2.0f);
                case FROST -> world.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.6f);
                case SHADOW -> world.playSound(center, Sound.ENTITY_VEX_CHARGE, 1.5f, 0.8f);
                case FLAME -> world.playSound(center, Sound.ITEM_FIRECHARGE_USE, 0.9f, 1.1f);
                case TOXIC -> world.playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 0.7f, 1.2f);
                case ARCANE -> world.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.6f);
                case RADIANT -> world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
                case VOID -> world.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.7f, 0.5f);
                case WIND -> world.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.6f, 1.6f);
                case STONE -> world.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);
                default -> world.playSound(center, Sound.ENTITY_VEX_CHARGE, 1.5f, 1.2f);
            }
        }

        /**
         * Updates the behavior of summoned minions.
         * <p>
         * This method makes minions follow the caster and attack nearby enemies,
         * and creates periodic visual effects.
         */
        private void updateMinionBehavior() {
            Player player = context.caster();
            Location playerLoc = player.getLocation();
            
            if (playerLoc == null) {
                return;
            }
            
            for (Vex minion : summonedMinions) {
                if (minion.isValid() && !minion.isDead()) {
                    // CRITICAL: Always check and prevent caster targeting first
                    LivingEntity currentTarget = minion.getTarget();
                    if (currentTarget != null && currentTarget.equals(context.caster())) {
                        minion.setTarget(null); // Immediately clear if targeting caster
                    }

                    // Determine nearest valid enemy (never the caster)
                    LivingEntity target = findNearestEnemy(minion);
                    if (target != null && target.isValid() && !target.isDead()) {
                        // Double-check: never target the caster
                        if (!target.equals(context.caster())) {
                            minion.setTarget(target);
                            // On-hit themed effects per variant
                            switch (variant) {
                                case FROST -> target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1, false, true));
                                case FLAME -> target.setFireTicks(Math.max(0, target.getFireTicks()) + 40);
                                case TOXIC -> target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0, false, true));
                                case ARCANE -> target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, true));
                                case RADIANT -> target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, true));
                                case VOID -> target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, false, true));
                                case WIND -> {
                                    var ml = minion.getLocation();
                                    var tl = target.getLocation();
                                    if (ml != null && tl != null) {
                                        Vector kb = tl.toVector().subtract(ml.toVector()).normalize().multiply(0.4);
                                        kb.setY(0.25);
                                        target.setVelocity(target.getVelocity().add(kb));
                                    }
                                }
                                case STONE -> {
                                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, true));
                                    target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 1, false, true));
                                }
                                default -> {}
                            }
                        } else {
                            // Target was caster, clear it
                            minion.setTarget(null);
                        }
                    } else {
                        // No target; ensure the minion isn't hostile toward the caster
                        minion.setTarget(null);
                    }
                    
                    var minionLoc = minion.getLocation();
                    if (minionLoc != null) {
                        Vector toPlayer = playerLoc.toVector().subtract(minionLoc.toVector());
                        double d2 = toPlayer.lengthSquared();
                        if (d2 > 1.0) {
                            Vector direction = toPlayer.normalize();
                            minion.setVelocity(direction.multiply(0.5));
                        }
                    }
                    
                    // Visual effect periodically by variant
                    if (ticks % 20 == 0) {
                        var effLoc = minion.getLocation();
                        if (effLoc != null && world != null) {
                            switch (variant) {
                                case STORM -> world.spawnParticle(Particle.ELECTRIC_SPARK, effLoc.add(0, 1, 0), 6, 0.2, 0.2, 0.2, 0.02);
                                case FROST -> world.spawnParticle(Particle.SNOWFLAKE, effLoc.add(0, 1, 0), 8, 0.25, 0.25, 0.25, 0.02);
                                case SHADOW -> world.spawnParticle(Particle.SMOKE, effLoc.add(0, 1, 0), 4, 0.2, 0.2, 0.2, 0.02);
                                default -> world.spawnParticle(Particle.ENCHANT, effLoc.add(0, 1, 0), 4, 0.2, 0.2, 0.2, 0.01);
                            }
                        }
                    }
                    owner.onMinionTick(minion, ticks, context);
                }
            }

            // FINAL SAFETY CHECK: Ensure no minions are targeting caster at end of behavior update
            preventCasterTargeting();
        }

        /**
         * Finds the nearest enemy to a minion within search distance.
         * <p>
         * This method searches for valid living entities near a minion and returns
         * the closest one that is not the caster or another minion.
         *
         * @param minion the vex minion to find enemies for
         * @return the nearest enemy entity, or null if none found
         */
        private @Nullable LivingEntity findNearestEnemy(@NotNull Vex minion) {
            Objects.requireNonNull(minion, "Minion cannot be null");

            LivingEntity nearest = null;
            double nearestDistance = 16.0 * 16.0; // squared
            boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class)
                    .getMainConfig().getBoolean("features.friendly-fire", false);

            Player caster = context.caster();

            for (Entity entity : minion.getNearbyEntities(16, 16, 16)) {
                if (entity instanceof LivingEntity && !(entity instanceof Vex) &&
                    entity.isValid() && !entity.isDead()) {

                    // AGGRESSIVE CASTER PROTECTION: Multiple checks to prevent caster targeting
                    if (entity.equals(caster) || entity.getUniqueId().equals(caster.getUniqueId())) {
                        continue; // Skip caster completely
                    }

                    // Respect friendly-fire: skip players if disabled
                    if (!friendlyFire && entity instanceof Player) {
                        continue;
                    }

                    var entityLocation = entity.getLocation();
                    var minionLocation = minion.getLocation();

                    if (entityLocation != null && minionLocation != null) {
                        double d2 = entityLocation.toVector().distanceSquared(minionLocation.toVector());
                        if (d2 < nearestDistance) {
                            nearest = (LivingEntity) entity;
                            nearestDistance = d2;
                        }
                    }
                }
            }

            return nearest;
        }

        /**
         * Emergency protection method to prevent minions from targeting the caster.
         * This method runs every tick to ensure no minion ever targets its owner.
         */
        private void preventCasterTargeting() {
            Player caster = context.caster();
            for (Vex minion : summonedMinions) {
                if (minion.isValid() && !minion.isDead()) {
                    LivingEntity currentTarget = minion.getTarget();
                    if (currentTarget != null && (currentTarget.equals(caster) ||
                        currentTarget.getUniqueId().equals(caster.getUniqueId()))) {
                        minion.setTarget(null); // Immediately clear caster target
                    }
                }
            }
        }

        /**
         * Creates visual effects for the summon swarm spell.
         * <p>
         * This method generates animated enchant and smoke particles around the
         * summoning circle and minions.
         */
        private void createVisualEffects() {
            if (world == null) {
                return;
            }
            
            // Create magical circle around the center
            double currentRadius = radius + Math.sin(ticks * 0.2) * 1.5;

            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i / 24) + (ticks * 0.1);
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                switch (variant) {
                    case STORM -> world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0, 0, 0);
                    case FROST -> world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
                    case SHADOW -> world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0, 0, 0, 0);
                    case FLAME -> world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0.01);
                    case TOXIC -> world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, particleLoc, 1, 0, 0, 0, 0);
                    case ARCANE -> world.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
                    case RADIANT -> world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                    case VOID -> world.spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 1, 0, 0, 0, 0);
                    case WIND -> world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0, 0, 0.01);
                    case STONE -> world.spawnParticle(Particle.BLOCK, particleLoc, 1, 0, 0, 0, 0, Material.STONE.createBlockData());
                    default -> world.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
                }
            }
            
            // Create particles around minions
            for (Vex minion : summonedMinions) {
                if (minion.isValid() && !minion.isDead()) {
                    var minionLocation = minion.getLocation();
                    if (minionLocation != null && world != null) {
                        if (ticks % 10 == 0) {
                            switch (variant) {
                                case STORM -> world.spawnParticle(Particle.ELECTRIC_SPARK, minionLocation.add(0, 1, 0), 2, 0.15, 0.15, 0.15, 0.01);
                                case FROST -> world.spawnParticle(Particle.SNOWFLAKE, minionLocation.add(0, 1, 0), 2, 0.15, 0.15, 0.15, 0.01);
                                case SHADOW -> world.spawnParticle(Particle.SMOKE, minionLocation.add(0, 1, 0), 2, 0.15, 0.15, 0.15, 0.01);
                                case FLAME -> world.spawnParticle(Particle.FLAME, minionLocation.add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0.01);
                                case TOXIC -> world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, minionLocation.add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0);
                                case ARCANE -> world.spawnParticle(Particle.ENCHANT, minionLocation.add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0);
                                case RADIANT -> world.spawnParticle(Particle.END_ROD, minionLocation.add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0);
                                case VOID -> world.spawnParticle(Particle.REVERSE_PORTAL, minionLocation.add(0, 1, 0), 1, 0.05, 0.05, 0.05, 0);
                                case WIND -> world.spawnParticle(Particle.CLOUD, minionLocation.add(0, 1, 0), 2, 0.12, 0.12, 0.12, 0.01);
                                case STONE -> world.spawnParticle(Particle.BLOCK, minionLocation.add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0, Material.STONE.createBlockData());
                                default -> world.spawnParticle(Particle.ENCHANT, minionLocation.add(0, 1, 0), 2, 0.15, 0.15, 0.15, 0.01);
                            }
                        }
                    }
                }
            }
            
            // Create central vortex
            if (ticks % 8 == 0) {
                for (int i = 0; i < 10; i++) {
                    double height = i * 0.3;
                    Location vortexLoc = center.clone().add(0, height, 0);
                    switch (variant) {
                        case STORM -> world.spawnParticle(Particle.ELECTRIC_SPARK, vortexLoc, 2, 0.1, 0.1, 0.1, 0.01);
                        case FROST -> world.spawnParticle(Particle.SNOWFLAKE, vortexLoc, 2, 0.1, 0.1, 0.1, 0.01);
                        case SHADOW -> world.spawnParticle(Particle.SMOKE, vortexLoc, 2, 0.1, 0.1, 0.1, 0.01);
                        case FLAME -> world.spawnParticle(Particle.FLAME, vortexLoc, 2, 0.1, 0.1, 0.1, 0.005);
                        case TOXIC -> world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, vortexLoc, 2, 0.1, 0.1, 0.1, 0);
                        case ARCANE -> world.spawnParticle(Particle.ENCHANT, vortexLoc, 2, 0.1, 0.1, 0.1, 0);
                        case RADIANT -> world.spawnParticle(Particle.END_ROD, vortexLoc, 2, 0.1, 0.1, 0.1, 0);
                        case VOID -> world.spawnParticle(Particle.REVERSE_PORTAL, vortexLoc, 2, 0.1, 0.1, 0.1, 0);
                        case WIND -> world.spawnParticle(Particle.CLOUD, vortexLoc, 2, 0.1, 0.1, 0.1, 0.01);
                        case STONE -> world.spawnParticle(Particle.BLOCK, vortexLoc, 2, 0.1, 0.1, 0.1, 0, Material.STONE.createBlockData());
                        default -> world.spawnParticle(Particle.WITCH, vortexLoc, 2, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
        }

        /**
         * Creates warning effects when the spell is about to end.
         */
        private void createDurationWarningEffects() {
            if (world == null) {
                return;
            }
            
            // Create pulsing red particles to indicate spell ending
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i / 16);
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location warningLoc = center.clone().add(x, 0.5, z);
                world.spawnParticle(Particle.DUST, warningLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1.0f));
            }
            
            // Play warning sound
            world.playSound(center, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f);
        }

        /**
         * Dismisses all summoned minions with visual and audio effects.
         * <p>
         * This method removes all valid minions from the world and creates particle
         * effects to visualize their dismissal.
         */
        private void dismissMinions() {
            if (world == null) {
                return;
            }
            
            // Dismiss all minions with visual effects
            for (Vex minion : summonedMinions) {
                if (minion.isValid() && !minion.isDead()) {
                    // Visual effect for dismissal
                    var minionLocation = minion.getLocation();
                    if (minionLocation != null) {
                        world.spawnParticle(Particle.SMOKE, minionLocation, 30, 0.5, 0.5, 0.5, 0.1);
                        world.spawnParticle(Particle.EXPLOSION, minionLocation, 1, 0, 0, 0, 0);
                    }
                    owner.onMinionDismiss(minion, context);
                    minion.remove();
                }
            }
            
            // Sound effect
            world.playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 1.2f);
            
            summonedMinions.clear();
        }
    }
    
    // ===== Extensibility Hooks =====
    protected Variant getVariant(@NotNull SpellContext context) {
        String variantStr = spellConfig.getString("values.variant", "vex");
        try {
            return Variant.valueOf(variantStr.toUpperCase());
        } catch (Exception e) {
            return Variant.VEX;
        }
    }

    protected void onMinionSpawn(@NotNull Vex minion, @NotNull SpellContext context) {
        // default: no-op
    }

    protected void onMinionTick(@NotNull Vex minion, int ticks, @NotNull SpellContext context) {
        // default: no-op
    }

    protected void onMinionDismiss(@NotNull Vex minion, @NotNull SpellContext context) {
        // default: no-op
    }
}
