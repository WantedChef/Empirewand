package nl.wantedchef.empirewand.spell;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.storage.Keys;

public abstract class ProjectileSpell<P extends Projectile> extends Spell<Void> {

    // Define the key locally to fix the compilation error.
    private static final NamespacedKey PROJECTILE_PROCESSED = new NamespacedKey("empirewand", "projectile_processed");

    protected final double speed;
    protected final double homingStrength;
    protected final boolean isHoming;
    protected final Particle trailParticle;
    protected final Sound hitSound;
    protected final float hitVolume;
    protected final float hitPitch;
    protected final Class<P> projectileClass;

    protected ProjectileSpell(Builder<P> builder) {
        super(builder);
        if (builder.projectileClass == null) {
            throw new IllegalArgumentException("Projectile class cannot be null");
        }
        this.speed = builder.speed;
        this.homingStrength = builder.homingStrength;
        this.isHoming = builder.isHoming;
        this.trailParticle = builder.trailParticle;
        this.hitSound = builder.hitSound;
        this.hitVolume = builder.hitVolume;
        this.hitPitch = builder.hitPitch;
        this.projectileClass = builder.projectileClass;
    }

    @Override
    @Nullable
    protected Void executeSpell(@NotNull SpellContext context) {
        launchProjectile(context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void effect) {
        // No-op.
    }

    protected void launchProjectile(@NotNull SpellContext context) {
        Player caster = context.caster();
        Location launchLocation = caster.getEyeLocation();
        Vector direction = launchLocation.getDirection().normalize().multiply(speed);

        P projectile = launchLocation.getWorld().spawn(launchLocation, this.projectileClass, proj -> {
            proj.setShooter(caster);
            proj.setVelocity(direction);
            proj.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, key());
            proj.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING,
                    caster.getUniqueId().toString());
        });

        if (trailParticle != null) {
            startTrail(projectile, context);
        }

        if (isHoming) {
            startHoming(projectile, context);
        }
    }

    private void startTrail(@NotNull Projectile projectile, @NotNull SpellContext context) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    cancel();
                    return;
                }
                projectile.getWorld().spawnParticle(trailParticle, projectile.getLocation(), 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    private void startHoming(@NotNull Projectile projectile, @NotNull SpellContext context) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    cancel();
                    return;
                }
                Entity target = findNearestTarget(projectile, context);
                if (target != null) {
                    Vector toTarget = target.getLocation().toVector().subtract(projectile.getLocation().toVector())
                            .normalize();
                    Vector newVelocity = projectile.getVelocity().normalize().add(toTarget.multiply(homingStrength))
                            .normalize().multiply(speed);
                    projectile.setVelocity(newVelocity);
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    @Nullable
    protected Entity findNearestTarget(@NotNull Projectile projectile, @NotNull SpellContext context) {
        return projectile.getNearbyEntities(10, 10, 10).stream()
                .filter(entity -> entity instanceof LivingEntity && !entity.equals(context.caster()))
                .min((e1, e2) -> Double.compare(
                        e1.getLocation().distanceSquared(projectile.getLocation()),
                        e2.getLocation().distanceSquared(projectile.getLocation())))
                .orElse(null);
    }

    @Override
    public void onProjectileHit(@NotNull ProjectileHitEvent event, @NotNull Player caster) {
        Projectile projectile = event.getEntity();
        if (projectile == null || !projectileClass.isInstance(projectile))
            return;

        if (projectile.getPersistentDataContainer().has(PROJECTILE_PROCESSED)) {
            return;
        }
        projectile.getPersistentDataContainer().set(PROJECTILE_PROCESSED, PersistentDataType.BYTE, (byte) 1);

        if (hitSound != null && projectile.getWorld() != null) {
            projectile.getWorld().playSound(projectile.getLocation(), hitSound, hitVolume, hitPitch);
        }

        if (trailParticle != null && projectile.getWorld() != null) {
            projectile.getWorld().spawnParticle(trailParticle, projectile.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
        }

        // Build SpellContext using plugin services; derive hit info from the event
        LivingEntity hitEntity = event.getHitEntity() instanceof LivingEntity ? (LivingEntity) event.getHitEntity()
                : null;
        org.bukkit.Location hitLocation = hitEntity != null
                ? hitEntity.getLocation()
                : (event.getHitBlock() != null ? event.getHitBlock().getLocation()
                        : (projectile.getLocation() != null ? projectile.getLocation() : caster.getLocation()));
        org.bukkit.plugin.Plugin rawPlugin = org.bukkit.plugin.java.JavaPlugin
                .getProvidingPlugin(EmpireWandPlugin.class);
        if (!(rawPlugin instanceof EmpireWandPlugin)) {
            return; // Plugin not available, cannot handle hit
        }
        EmpireWandPlugin plugin = (EmpireWandPlugin) rawPlugin;
        SpellContext context = new SpellContext(plugin, caster, plugin.getConfigService(), plugin.getFxService(),
                hitEntity, hitLocation, key());
        handleHit(context, projectile, event);

        projectile.remove();
    }

    protected abstract void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event);

    public abstract static class Builder<P extends Projectile> extends Spell.Builder<Void> {
        private double speed = 1.5;
        private double homingStrength = 0.1;
        private boolean isHoming = false;
        public Particle trailParticle = Particle.CRIT;
        protected Sound hitSound = Sound.ENTITY_GENERIC_EXPLODE;
        private float hitVolume = 1.0f;
        private float hitPitch = 1.0f;
        private Class<P> projectileClass;

        protected Builder(@Nullable EmpireWandAPI api, Class<P> projectileClass) {
            super(api);
            if (projectileClass == null) {
                throw new IllegalArgumentException("Projectile class cannot be null");
            }
            this.projectileClass = projectileClass;
        }

        @NotNull
        public Builder<P> speed(double speed) {
            this.speed = speed;
            return this;
        }

        @NotNull
        public Builder<P> homingStrength(double homingStrength) {
            this.homingStrength = homingStrength;
            return this;
        }

        @NotNull
        public Builder<P> isHoming(boolean isHoming) {
            this.isHoming = isHoming;
            return this;
        }

        @NotNull
        public Builder<P> trailParticle(@Nullable Particle trailParticle) {
            this.trailParticle = trailParticle;
            return this;
        }

        @NotNull
        public Builder<P> hitSound(@Nullable Sound hitSound) {
            this.hitSound = hitSound;
            return this;
        }

        @NotNull
        public Builder<P> hitVolume(float hitVolume) {
            this.hitVolume = hitVolume;
            return this;
        }

        @NotNull
        public Builder<P> hitPitch(float hitPitch) {
            this.hitPitch = hitPitch;
            return this;
        }
    }
}





