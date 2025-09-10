package nl.wantedchef.empirewand.spell.fire.advanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;

import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A spell that launches a fiery projectile that explodes on impact.
 */
public class Comet extends ProjectileSpell<Fireball> {

    /**
     * The builder for the Comet spell.
     */
    public static class Builder extends ProjectileSpell.Builder<Fireball> {
        /**
         * Creates a new builder for the Comet spell.
         *
         * @param api The EmpireWandAPI instance.
         */
        public Builder(EmpireWandAPI api) {
            super(api, Fireball.class);
            this.name = "Comet";
            this.description = "Launches a fiery projectile that explodes on impact.";
            this.cooldown = java.time.Duration.ofSeconds(20);
            this.spellType = SpellType.FIRE;
            this.hitSound = null; // Explosion handles sound
            this.trailParticle = null; // Custom trail
        }

        @Override
        @NotNull
        public ProjectileSpell<Fireball> build() {
            return new Comet(this);
        }
    }

    private static final float DEFAULT_YIELD = 2.5f;
    private static final int DEFAULT_TRAIL_LENGTH = 5;
    private static final int DEFAULT_PARTICLE_COUNT = 3;
    private static final int DEFAULT_BLOCK_LIFETIME_TICKS = 35;
    private static final double DEFAULT_DAMAGE = 7.0;
    private static final long TASK_TIMER_DELAY = 0L;
    private static final long TASK_TIMER_PERIOD = 1L;
    private static final float LAUNCH_SOUND_VOLUME = 1.0f;
    private static final float LAUNCH_SOUND_PITCH = 1.0f;

    private Comet(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "comet";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player caster = context.caster();
        Location launchLocation = caster.getEyeLocation();
        Vector direction = launchLocation.getDirection().normalize().multiply(speed);

        float yield = (float) spellConfig.getDouble("values.yield", DEFAULT_YIELD);
        int trailLength = spellConfig.getInt("values.trail_length", DEFAULT_TRAIL_LENGTH);
        int particleCount = spellConfig.getInt("values.particle_count", DEFAULT_PARTICLE_COUNT);
        int blockLifetime = spellConfig.getInt("values.block_lifetime_ticks", DEFAULT_BLOCK_LIFETIME_TICKS);

        Fireball fireball = launchLocation.getWorld().spawn(launchLocation, Fireball.class, fb -> {
            fb.setShooter(caster);
            fb.setYield(yield);
            fb.setIsIncendiary(false);
            fb.setDirection(direction);
            fb.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            fb.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                    caster.getUniqueId().toString());
        });

        new FieryTail(fireball, trailLength, particleCount, blockLifetime).runTaskTimer(context.plugin(),
                TASK_TIMER_DELAY, TASK_TIMER_PERIOD);
        context.fx().playSound(caster, Sound.ITEM_FIRECHARGE_USE, LAUNCH_SOUND_VOLUME, LAUNCH_SOUND_PITCH);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        context.fx().impact(projectile.getLocation());

        if (event.getHitEntity() instanceof LivingEntity target) {
            String ownerUUID = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER,
                    PersistentDataType.STRING);
            double damage = spellConfig.getDouble("values.damage", DEFAULT_DAMAGE);
            boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                    .getBoolean("features.friendly-fire", false);

            if (ownerUUID != null) {
                Player caster = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                if (caster != null && !(target.equals(caster) && !friendlyFire)) {
                    target.damage(damage, caster);
                }
            }
        }
    }

    /**
     * A runnable that creates a fiery tail effect for the comet.
     */
    private static final class FieryTail extends BukkitRunnable {
        private static final double Y_OFFSET = -0.25;
        private static final int PARTICLE_MULTIPLIER = 2;
        private static final double PARTICLE_OFFSET = 0.15;
        private static final double PARTICLE_SPEED = 0.01;
        private static final int MAX_LIFETIME_TICKS = 300; // 15 seconds

        private final Fireball fireball;
        private final org.bukkit.World world;
        private final int trailLength;
        private final int particleCount;
        private final int blockLifetime;
        private int tick = 0;
        private final java.util.Deque<TempBlock> queue = new java.util.ArrayDeque<>();
        private final java.util.Set<Block> ours = new java.util.HashSet<>();

        FieryTail(Fireball fb, int trailLength, int particleCount, int blockLifetime) {
            this.fireball = fb;
            this.world = fb.getWorld();
            this.trailLength = trailLength;
            this.particleCount = particleCount;
            this.blockLifetime = blockLifetime;
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

            for (int i = 0; i < trailLength; i++) {
                Location l = base.clone().add(dir.clone().multiply(-i));
                Block b = l.getBlock();
                if (!ours.contains(b) && (b.getType().isAir() || b.getType() == Material.SNOW)) {
                    queue.addLast(new TempBlock(b, b.getBlockData(), tick + blockLifetime));
                    b.setType(Material.NETHERRACK, false);
                    ours.add(b);
                    world.spawnParticle(Particle.FLAME, l, particleCount * PARTICLE_MULTIPLIER, PARTICLE_OFFSET,
                            PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_SPEED);
                }
            }

            while (!queue.isEmpty() && queue.peekFirst().expireTick <= tick) {
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.NETHERRACK) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
            }

            tick++;
            if (tick > MAX_LIFETIME_TICKS) {
                cleanup();
                cancel();
            }
        }

        /**
         * Cleans up any temporary blocks created by the tail.
         */
        private void cleanup() {
            while (!queue.isEmpty()) {
                TempBlock tb = queue.pollFirst();
                if (tb.block.getType() == Material.NETHERRACK) {
                    tb.block.setBlockData(tb.previous, false);
                }
                ours.remove(tb.block);
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
        }
    }
}
