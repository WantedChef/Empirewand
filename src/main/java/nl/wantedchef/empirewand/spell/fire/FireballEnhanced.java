package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * A spell that launches a fireball projectile with enhanced area-of-effect damage.
 */
public class FireballEnhanced extends ProjectileSpell<org.bukkit.entity.Fireball> {

    /**
     * The builder for the FireballEnhanced spell.
     */
    public static class Builder extends ProjectileSpell.Builder<org.bukkit.entity.Fireball> {
        /**
         * Creates a new builder for the FireballEnhanced spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api, org.bukkit.entity.Fireball.class);
            this.name = "Enhanced Fireball";
            this.description = "Launches a powerful fireball with enhanced area damage.";
            this.cooldown = java.time.Duration.ofSeconds(5);
            this.spellType = SpellType.FIRE;
            this.trailParticle = null; // Custom trail
            this.hitSound = null; // Vanilla explosion sound
        }

        @Override
        @NotNull
        public ProjectileSpell<org.bukkit.entity.Fireball> build() {
            return new FireballEnhanced(this);
        }
    }

    private static final double DEFAULT_YIELD = 5.0;
    private static final boolean DEFAULT_INCENDIARY = true;
    private static final int DEFAULT_TRAIL_LENGTH = 6;
    private static final int DEFAULT_PARTICLE_COUNT = 3;
    private static final int DEFAULT_BLOCK_LIFETIME_TICKS = 50;
    private static final long TASK_TIMER_DELAY = 0L;
    private static final long TASK_TIMER_PERIOD = 1L;
    private static final float LAUNCH_SOUND_VOLUME = 1.0f;
    private static final float LAUNCH_SOUND_PITCH = 1.0f;
    private static final int HIT_PARTICLE_COUNT = 50;
    private static final double HIT_PARTICLE_OFFSET = 0.7;
    private static final double HIT_PARTICLE_SPEED = 0.2;
    private static final float HIT_SOUND_VOLUME = 1.5f;
    private static final float HIT_SOUND_PITCH = 0.8f;
    private static final double DAMAGE_RADIUS = 6.0;
    private static final double MAX_DAMAGE = 15.0;
    private static final double MIN_DAMAGE = 3.0;

    private FireballEnhanced(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "fireball-enhanced";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player player = context.caster();

        double explosionYield = spellConfig.getDouble("values.yield", DEFAULT_YIELD);
        boolean incendiary = spellConfig.getBoolean("flags.incendiary", DEFAULT_INCENDIARY);
        int trailLength = spellConfig.getInt("values.trail_length", DEFAULT_TRAIL_LENGTH);
        int particleCount = spellConfig.getInt("values.particle_count", DEFAULT_PARTICLE_COUNT);
        int lifeTicks = spellConfig.getInt("values.block_lifetime_ticks", DEFAULT_BLOCK_LIFETIME_TICKS);

        player.launchProjectile(org.bukkit.entity.Fireball.class,
                player.getEyeLocation().getDirection().multiply(speed), fireball -> {
                    fireball.setYield((float) explosionYield);
                    fireball.setIsIncendiary(incendiary);
                    fireball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
                    fireball.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                            player.getUniqueId().toString());
                    new FireTrail(fireball, trailLength, particleCount, lifeTicks).runTaskTimer(context.plugin(), TASK_TIMER_DELAY,
                            TASK_TIMER_PERIOD);
                });

        context.fx().playSound(player, Sound.ENTITY_BLAZE_SHOOT, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        boolean blockDamage = spellConfig.getBoolean("flags.block-damage", true);
        if (!blockDamage) {
            // If block damage is disabled, create a visual-only explosion
            // and manually damage entities, since the projectile's explosion is cancelled.
            Location hitLoc = projectile.getLocation();
            hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 1, 0, 0, 0, 0);
            hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, HIT_PARTICLE_COUNT * 2, HIT_PARTICLE_OFFSET, HIT_PARTICLE_OFFSET, HIT_PARTICLE_OFFSET, HIT_PARTICLE_SPEED);
            hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, HIT_SOUND_VOLUME, HIT_SOUND_PITCH);

            // Enhanced area damage
            applyAreaDamage(context, hitLoc);

            event.setCancelled(true); // Cancel the vanilla explosion
        } else {
            // Enhanced effects for normal explosion
            Location hitLoc = projectile.getLocation();
            hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 2, 0, 0, 0, 0);
            hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, HIT_PARTICLE_COUNT, HIT_PARTICLE_OFFSET, HIT_PARTICLE_OFFSET, HIT_PARTICLE_OFFSET, HIT_PARTICLE_SPEED);
            
            // Apply additional area effects
            applyAreaEffects(context, hitLoc);
        }
    }

    /**
     * Applies area damage to entities around the explosion point.
     */
    private void applyAreaDamage(SpellContext context, Location center) {
        for (var entity : center.getWorld().getNearbyEntities(center, DAMAGE_RADIUS, DAMAGE_RADIUS, DAMAGE_RADIUS)) {
            if (entity instanceof LivingEntity living && !living.equals(context.caster())) {
                double distance = living.getLocation().distance(center);
                double damage = MAX_DAMAGE * (1.0 - distance / DAMAGE_RADIUS); // Max damage at center
                if (damage > 0) {
                    living.damage(Math.max(damage, MIN_DAMAGE), context.caster());
                    living.setFireTicks(100); // 5 seconds of fire
                }
            }
        }
    }

    /**
     * Applies additional area effects like knockback and fire.
     */
    private void applyAreaEffects(SpellContext context, Location center) {
        for (var entity : center.getWorld().getNearbyEntities(center, DAMAGE_RADIUS, DAMAGE_RADIUS, DAMAGE_RADIUS)) {
            if (entity instanceof LivingEntity living && !living.equals(context.caster())) {
                // Apply knockback
                Vector knockback = living.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.2);
                knockback.setY(0.5); // Add upward force
                living.setVelocity(living.getVelocity().add(knockback));
                
                // Set on fire
                living.setFireTicks(80);
                
                // Visual effect
                context.fx().spawnParticles(living.getLocation(), Particle.FLAME, 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

    /**
     * A runnable that creates a fire trail effect for the fireball.
     */
    private static final class FireTrail extends BukkitRunnable {
        private static final double Y_OFFSET = -0.25;
        private static final Material TRAIL_BLOCK_MATERIAL = Material.MAGMA_BLOCK;
        private static final double PARTICLE_OFFSET = 0.15;
        private static final double PARTICLE_SPEED = 0.02;
        private static final int MAX_LIFETIME_TICKS = 300; // 15 seconds

        private final org.bukkit.entity.Fireball fireball;
        private final int trailLength;
        private final int particleCount;
        private final int lifeTicks;
        private int tick = 0;
        private final Deque<TempBlock> queue = new ArrayDeque<>();
        private final Set<Block> ours = new HashSet<>();

        FireTrail(org.bukkit.entity.Fireball fireball, int trailLength, int particleCount, int lifeTicks) {
            this.fireball = fireball;
            this.trailLength = trailLength;
            this.particleCount = particleCount;
            this.lifeTicks = lifeTicks;
        }

        @Override
        public void run() {
            if (!fireball.isValid() || fireball.isDead()) {
                cleanup();
                cancel();
                return;
            }

            Vector dir = fireball.getVelocity().clone().normalize();
            Location base = fireball.getLocation().clone().add(0, Y_OFFSET, 0);

            // Enhanced trail with more particles
            for (int i = 0; i < trailLength; i++) {
                Location l = base.clone().add(dir.clone().multiply(-i));
                Block b = l.getBlock();
                if (!ours.contains(b) && b.getType().isAir()) {
                    queue.addLast(new TempBlock(b, b.getBlockData(), tick + lifeTicks));
                    b.setType(TRAIL_BLOCK_MATERIAL, false);
                    ours.add(b);
                    // Enhanced particle effects
                    fireball.getWorld().spawnParticle(Particle.FLAME, l, particleCount * 2, PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_SPEED);
                    fireball.getWorld().spawnParticle(Particle.LAVA, l, particleCount, PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_SPEED * 0.5);
                }
            }

            while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
                queue.pollFirst().revert();
            }

            tick++;
            if (tick > MAX_LIFETIME_TICKS) {
                cleanup();
                cancel();
            }
        }

        /**
         * Cleans up any temporary blocks created by the trail.
         */
        private void cleanup() {
            while (!queue.isEmpty()) {
                queue.pollFirst().revert();
            }
        }

        /**
         * A record representing a temporary block.
         *
         * @param block      The block that was changed.
         * @param previous   The previous block data.
         * @param expireTick The tick at which the block should revert.
         */
        private record TempBlock(Block block, BlockData previous, int expireTick) {
            void revert() {
                if (block.getType() == TRAIL_BLOCK_MATERIAL) {
                    block.setBlockData(previous, false);
                }
            }
        }
    }
    
    /**
     * Keys used for persistent data storage.
     */
    private static class Keys {
        private static final org.bukkit.NamespacedKey PROJECTILE_SPELL = 
            new org.bukkit.NamespacedKey("empirewand", "projectile_spell");
        private static final org.bukkit.NamespacedKey PROJECTILE_OWNER = 
            new org.bukkit.NamespacedKey("empirewand", "projectile_owner");
    }
}