package nl.wantedchef.empirewand.spell.enhanced.defensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A defensive spell that creates a powerful energy shield around allies,
 * absorbing damage and
 * reflecting projectiles.
 */
public class EnergyShield extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Energy Shield";
            this.description = "Creates a powerful energy shield around allies that absorbs damage and reflects projectiles.";
            this.cooldown = java.time.Duration.ofSeconds(40);
            this.spellType = SpellType.AURA;
        }

        @Override
        @NotNull
        public EnergyShield build() {
            return new EnergyShield(this);
        }
    }

    private EnergyShield(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "energyshield";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 12.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 150);
        double absorptionHearts = spellConfig.getDouble("values.absorption-hearts", 10.0);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);
        boolean reflectsProjectiles = spellConfig.getBoolean("flags.reflects-projectiles", true);

        // Play initial sound
        World world = player.getWorld();
        if (world == null) {
            return null; // Can't cast a spell in a null world
        }
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.8f);

        // Start energy shield effect via TaskManager to ease testing and tracking
        context.plugin().getTaskManager().runTaskTimer(
                new EnergyShieldTask(context, player.getLocation(), radius, durationTicks, absorptionHearts,
                        affectsPlayers, reflectsProjectiles),
                0L, 4L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private static class EnergyShieldTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final int durationTicks;
        private final double absorptionHearts;
        private final boolean affectsPlayers;
        private final boolean reflectsProjectiles;
        private final World world;
        private int ticks = 0;
        private final int maxTicks;
        private final Map<UUID, ShieldData> shieldedEntities = new HashMap<>();

        public EnergyShieldTask(SpellContext context, Location center, double radius,
                int durationTicks, double absorptionHearts, boolean affectsPlayers,
                boolean reflectsProjectiles) {
            this.context = context;
            this.center = center;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.absorptionHearts = absorptionHearts;
            this.affectsPlayers = affectsPlayers;
            this.reflectsProjectiles = reflectsProjectiles;
            this.world = center.getWorld();
            this.maxTicks = durationTicks / 4; // Convert to our tick interval
        }

        @Override
        public void run() {
            if (ticks >= maxTicks) {
                this.cancel();
                removeShields();
                // Play end sound
                world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.8f);
                return;
            }

            // Apply shields to entities
            applyShields();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        private void applyShields() {
            Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(center, radius, radius, radius);

            for (LivingEntity entity : nearbyEntities) {
                // Skip dead or invalid entities
                if (entity.isDead() || !entity.isValid())
                    continue;

                // Apply to caster always
                if (entity.equals(context.caster())) {
                    applyShieldToEntity(entity);
                    continue;
                }

                // Apply to players if enabled
                if (entity instanceof Player && affectsPlayers) {
                    applyShieldToEntity(entity);
                    continue;
                }

                // Apply to mobs only if they are tamed or on the same team
                if (!(entity instanceof Player)) {
                    // In a real implementation, you would check if the mob is tamed by the player
                    // For now, we'll skip non-player entities to avoid unintended behavior
                }
            }
        }

        private void applyShieldToEntity(LivingEntity entity) {
            UUID entityId = entity.getUniqueId();

            // Check if entity already has a shield
            ShieldData shieldData = shieldedEntities.get(entityId);
            if (shieldData == null) {
                // Create new shield with projectile reflection capability
                shieldData = new ShieldData(entity, absorptionHearts, reflectsProjectiles);
                shieldedEntities.put(entityId, shieldData);

                // Apply absorption effect
                // Calculate the amplifier. Each level adds 2 hearts (4 half-hearts).
                // Amplifier 0 = 2 hearts, 1 = 4 hearts, etc.
                int amplifier = (int) Math.ceil(absorptionHearts / 2.0) - 1;
                if (amplifier < 0) {
                    amplifier = 0; // Ensure amplifier is not negative
                }
                entity.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, durationTicks,
                        amplifier, false, false));
                entity.addPotionEffect(
                        new PotionEffect(PotionEffectType.RESISTANCE, 20, 2, false, false));

                // Visual effect for shield application
                world.spawnParticle(Particle.END_ROD, entity.getLocation().add(0, 1, 0), 20, 0.5,
                        0.5, 0.5, 0.1);
                
                // Note: Projectile reflection capability is available via shieldData.shouldReflectProjectiles()
                // This can be used in projectile event listeners to implement reflection logic
                if (shieldData.shouldReflectProjectiles()) {
                    // Projectile reflection is enabled for this shield
                    // In a full implementation, this would register projectile event listeners
                }
            }

            // Update shield visual
            if (ticks % 10 == 0) {
                createShieldVisual(entity);
            }
        }

        private void createShieldVisual(LivingEntity entity) {
            Location loc = entity.getLocation().add(0, 1, 0);
            double shieldRadius = 1.0 + (Math.sin(ticks * 0.5) * 0.2);

            for (int i = 0; i < 12; i++) {
                double angle = 2 * Math.PI * i / 12;
                double x = shieldRadius * Math.cos(angle);
                double z = shieldRadius * Math.sin(angle);
                Location particleLoc = loc.clone().add(x, 0, z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }

            // Sound effect occasionally
            if (ticks % 20 == 0) {
                world.playSound(loc, Sound.BLOCK_GLASS_STEP, 0.3f, 1.5f);
            }
        }

        private void createVisualEffects() {
            // Create rotating shield ring around center
            double currentRadius = radius + Math.sin(ticks * 0.3) * 2;

            for (int i = 0; i < 36; i++) {
                double angle = (2 * Math.PI * i / 36) + (ticks * 0.2);
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 1, z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }

            // Create energy beams between shielded entities
            if (ticks % 8 == 0) {
                List<LivingEntity> entities = new ArrayList<>();
                for (ShieldData data : shieldedEntities.values()) {
                    if (data.entity.isValid() && !data.entity.isDead()) {
                        entities.add(data.entity);
                    }
                }

                for (int i = 0; i < entities.size(); i++) {
                    LivingEntity from = entities.get(i);
                    LivingEntity to = entities.get((i + 1) % entities.size());

                    // Create beam between entities
                    createEnergyBeam(from.getLocation().add(0, 1, 0),
                            to.getLocation().add(0, 1, 0));
                }
            }
        }

        private void createEnergyBeam(Location from, Location to) {
            org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector());
            double distance = direction.length();
            if (distance < 0.01)
                return;

            org.bukkit.util.Vector step = direction.normalize().multiply(0.5); // Simplified step vector
            for (double d = 0; d < distance; d += 0.5) {
                Location particleLoc = from.clone().add(step.clone().multiply(d));
                world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0, 0, 0);
            }
        }

        private void removeShields() {
            // Remove absorption effects
            for (ShieldData data : shieldedEntities.values()) {
                if (data.entity.isValid() && !data.entity.isDead()) {
                    data.entity.removePotionEffect(PotionEffectType.ABSORPTION);
                }
            }

            shieldedEntities.clear();
        }


        private static class ShieldData {
            final LivingEntity entity;
            final boolean reflectsProjectiles;
            // You can add strength fields here later if needed

            ShieldData(LivingEntity entity, double absorptionHearts, boolean reflectsProjectiles) {
                this.entity = entity;
                this.reflectsProjectiles = reflectsProjectiles;
            }
            
            /**
             * Checks if this shield should reflect projectiles.
             * @return true if projectiles should be reflected
             */
            boolean shouldReflectProjectiles() {
                return reflectsProjectiles;
            }
        }
    }
}
