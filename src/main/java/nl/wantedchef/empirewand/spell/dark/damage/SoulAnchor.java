package nl.wantedchef.empirewand.spell.dark.damage;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.*;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SoulAnchor - Places a temporary soul anchor; first 1-3 enemies in the ring
 * become "bound"
 * and when leaving are pulled back and damaged, as stationary zone control.
 */
public final class SoulAnchor extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, AnchorData> anchors = new ConcurrentHashMap<>();
    private EmpireWandPlugin plugin;

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Soul Anchor";
            description = "Places a temporary soul anchor; first 1-3 enemies in the ring become 'bound' and when leaving are pulled back and damaged.";
            cooldown = Duration.ofSeconds(20);
            spellType = SpellType.DARK;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new SoulAnchor(this);
        }
    }

    private SoulAnchor(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "soul-anchor";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();
        UUID casterId = caster.getUniqueId();

        // If anchor already exists for this player, remove it (toggle off)
        if (anchors.containsKey(casterId)) {
            deactivate(caster, context);
        } else {
            // Create new anchor
            plugin = context.plugin();
            activate(caster, context);
        }

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    /* ---------------------------------------- */
    /* TOGGLE API */
    /* ---------------------------------------- */
    @Override
    public boolean isActive(Player player) {
        return anchors.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player))
            return;
        anchors.put(player.getUniqueId(), new AnchorData(player, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        AnchorData anchorData = anchors.remove(player.getUniqueId());
        if (anchorData != null) {
            anchorData.stop();
        }
    }

    @Override
    public void forceDeactivate(Player player) {
        deactivate(player, null);
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class AnchorData {
        private final Player caster;
        private final BukkitTask ticker;
        private final Location anchorLocation;
        private final double anchorRadius;
        private final int maxBoundTargets;
        private final int durationTicks;
        private final Set<UUID> boundTargets = ConcurrentHashMap.newKeySet();
        private final Set<UUID> previouslyInZone = ConcurrentHashMap.newKeySet();
        private int tickCounter = 0;
        private boolean isActive = true;

        AnchorData(Player caster, SpellContext context) {
            this.caster = caster;
            this.anchorLocation = caster.getLocation();
            this.anchorRadius = spellConfig.getDouble("values.radius", 8.0);
            this.maxBoundTargets = spellConfig.getInt("values.max-bound-targets", 3);
            this.durationTicks = spellConfig.getInt("values.duration-ticks", 400); // 20 seconds default

            // Send activation message
            caster.sendMessage(Component.text("§5⚡ §7Soul Anchor placed. Enemies entering will be bound."));

            // Start the ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1); // Run every tick
        }

        void stop() {
            if (!isActive)
                return;
            isActive = false;
            ticker.cancel();

            // Release all bound targets with damage
            releaseBoundTargets();

            caster.sendMessage(Component.text("§5⚡ §7Soul Anchor released."));
        }

        private void tick() {
            if (!caster.isOnline() || caster.isDead()) {
                forceDeactivate(caster);
                return;
            }

            // Increment tick counter
            tickCounter++;

            // Check if duration has expired
            if (tickCounter >= durationTicks) {
                forceDeactivate(caster);
                return;
            }

            // Apply anchor effects periodically
            if (tickCounter % 3 == 0) {
                spawnAnchorParticles();
            }

            // Check entities in zone every few ticks
            if (tickCounter % 5 == 0) {
                checkEntitiesInZone();
            }
        }

        private void spawnAnchorParticles() {
            World world = anchorLocation.getWorld();

            // Create a ring of particles around the anchor
            for (int i = 0; i < 24; i++) {
                double angle = 2 * Math.PI * i / 24;
                double x = Math.cos(angle) * anchorRadius;
                double z = Math.sin(angle) * anchorRadius;
                Location particleLoc = anchorLocation.clone().add(x, 0.1, z);

                // Alternate between different particle types
                if (tickCounter % 9 < 3) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                } else if (tickCounter % 9 < 6) {
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
                } else {
                    world.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Create occasional portal particles for soul effect
            if (Math.random() < 0.4) {
                world.spawnParticle(Particle.PORTAL, anchorLocation, 8, 1.0, 0.1, 1.0, 0.1);
            }

            // Create end rod particles for mystical effect
            if (Math.random() < 0.3) {
                world.spawnParticle(Particle.END_ROD, anchorLocation, 3, 0.5, 0.5, 0.5, 0.02);
            }
        }

        private void checkEntitiesInZone() {
            World world = anchorLocation.getWorld();
            Set<UUID> currentlyInZone = ConcurrentHashMap.newKeySet();

            // Get nearby entities
            for (Entity entity : world.getNearbyEntities(anchorLocation, anchorRadius, anchorRadius, anchorRadius)) {
                if (entity instanceof LivingEntity && entity != caster && !entity.isDead()) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    UUID entityId = entity.getUniqueId();

                    currentlyInZone.add(entityId);

                    // If this is a new entity entering the zone and we haven't reached max bound
                    // targets
                    if (!previouslyInZone.contains(entityId) && boundTargets.size() < maxBoundTargets) {
                        // Bind this target
                        boundTargets.add(entityId);

                        // Apply visual effect
                        Location entityLoc = livingEntity.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.SOUL, entityLoc, 10, 0.3, 0.3, 0.3, 0.1);
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, entityLoc, 5, 0.2, 0.2, 0.2, 0.05);

                        // Play binding sound
                        world.playSound(entityLoc, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 1.5f);
                    }
                }
            }

            // Check for entities that have left the zone
            for (UUID entityId : previouslyInZone) {
                if (!currentlyInZone.contains(entityId) && boundTargets.contains(entityId)) {
                    // Entity has left the zone while bound - pull it back
                    Entity entity = Bukkit.getEntity(entityId);
                    if (entity instanceof LivingEntity) {
                        pullEntityBack((LivingEntity) entity);
                    }
                }
            }

            // Update previously in zone set
            previouslyInZone.clear();
            previouslyInZone.addAll(currentlyInZone);
        }

        private void pullEntityBack(LivingEntity entity) {
            if (entity.isDead() || !entity.isValid())
                return;

            Location entityLoc = entity.getLocation();
            Vector pullDirection = anchorLocation.toVector().subtract(entityLoc.toVector()).normalize();
            double pullStrength = spellConfig.getDouble("values.pull-strength", 1.5);

            // Apply pull velocity
            entity.setVelocity(pullDirection.multiply(pullStrength).setY(0.3));

            // Apply damage
            double pullDamage = spellConfig.getDouble("values.pull-damage", 3.0);
            entity.damage(pullDamage, caster);

            // Spawn pull effect particles
            World world = entity.getWorld();
            Location midPoint = anchorLocation.clone().add(entityLoc.toVector()).multiply(0.5);
            world.spawnParticle(Particle.SOUL, midPoint, 15, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.CRIT, entityLoc, 10, 0.3, 0.3, 0.3, 0.1);

            // Play pull sound
            world.playSound(entityLoc, Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.2f);
        }

        private void releaseBoundTargets() {
            World world = anchorLocation.getWorld();
            double releaseDamage = spellConfig.getDouble("values.release-damage", 5.0);

            // Release all bound targets with damage
            for (UUID entityId : boundTargets) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    if (livingEntity.isDead() || !livingEntity.isValid())
                        continue;

                    // Apply damage
                    livingEntity.damage(releaseDamage, caster);

                    // Apply knockback away from anchor
                    Vector knockback = livingEntity.getLocation().toVector().subtract(anchorLocation.toVector())
                            .normalize().multiply(1.0);
                    knockback.setY(0.5); // Add upward component
                    livingEntity.setVelocity(knockback);

                    // Spawn release particles
                    Location entityLoc = livingEntity.getLocation().add(0, 1, 0);
                    world.spawnParticle(Particle.EXPLOSION, entityLoc, 10, 0.5, 0.5, 0.5, 0.1);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, entityLoc, 8, 0.3, 0.3, 0.3, 0.05);
                }
            }

            // Clear bound targets
            boundTargets.clear();
        }
    }
}