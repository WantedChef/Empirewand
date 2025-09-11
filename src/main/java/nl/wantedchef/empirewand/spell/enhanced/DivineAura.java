package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import java.util.Objects;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A divine healing spell that creates a powerful healing aura,
 * restoring health and granting beneficial effects to allies.
 */
public class DivineAura extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Divine Aura";
            this.description = "Creates a powerful healing aura that restores health and grants beneficial effects to allies.";
            this.cooldown = java.time.Duration.ofSeconds(45);
            this.spellType = SpellType.HEAL;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new DivineAura(this);
        }
    }

    private DivineAura(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "divine-aura";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        // Configuration
        double radius = spellConfig.getDouble("values.radius", 15.0);
        double healAmount = spellConfig.getDouble("values.heal-amount", 2.0);
        int durationTicks = spellConfig.getInt("values.duration-ticks", 200);
        boolean affectsPlayers = spellConfig.getBoolean("flags.affects-players", true);
        boolean grantsRegeneration = spellConfig.getBoolean("flags.grants-regeneration", true);
        boolean grantsResistance = spellConfig.getBoolean("flags.grants-resistance", true);

        // Play initial sound
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.5f);

        // Start aura effect
        new DivineAuraTask(context, player.getLocation(), radius, healAmount, durationTicks, affectsPlayers, 
                          grantsRegeneration, grantsResistance)
                .runTaskTimer(context.plugin(), 0L, 5L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects handled in scheduler
    }

    private static class DivineAuraTask extends BukkitRunnable {
        private final SpellContext context;
        private final Location center;
        private final double radius;
        private final double healAmount;
        private final int durationTicks;
        private final boolean affectsPlayers;
        private final boolean grantsRegeneration;
        private final boolean grantsResistance;
        private final World world;
        private int ticks = 0;
        private final int maxTicks;

        public DivineAuraTask(SpellContext context, Location center, double radius, double healAmount,
                             int durationTicks, boolean affectsPlayers, boolean grantsRegeneration, boolean grantsResistance) {
            this.context = context;
            this.center = center;
            this.radius = radius;
            this.healAmount = healAmount;
            this.durationTicks = durationTicks;
            this.affectsPlayers = affectsPlayers;
            this.grantsRegeneration = grantsRegeneration;
            this.grantsResistance = grantsResistance;
            this.world = center.getWorld();
            this.maxTicks = durationTicks / 5; // Convert to our tick interval
        }

        @Override
        public void run() {
            if (ticks >= maxTicks) {
                this.cancel();
                // Play end sound
                world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.5f);
                return;
            }

            // Apply healing and effects
            applyAuraEffects();

            // Create visual effects
            createVisualEffects();

            ticks++;
        }

        private void applyAuraEffects() {
            Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(center, radius, radius, radius);
            
            for (LivingEntity entity : nearbyEntities) {
                // Skip dead or invalid entities
                if (entity.isDead() || !entity.isValid()) continue;
                
                // Apply to caster always
                if (entity.equals(context.caster())) {
                    healAndApplyEffects(entity);
                    continue;
                }
                
                // Apply to players if enabled
                if (entity instanceof Player && affectsPlayers) {
                    healAndApplyEffects(entity);
                    continue;
                }
                
                // Apply to mobs only if they are tamed or on the same team
                if (!(entity instanceof Player)) {
                    // In a real implementation, you would check if the mob is tamed by the player
                    // For now, we'll skip non-player entities to avoid unintended behavior
                    continue;
                }
            }
        }

        private void healAndApplyEffects(LivingEntity entity) {
            // Heal entity
            double maxHealth = Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).getValue();
            entity.setHealth(Math.min(maxHealth, entity.getHealth() + healAmount));
            
            // Grant regeneration if enabled
            if (grantsRegeneration) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, false, false));
            }
            
            // Grant resistance if enabled
            if (grantsResistance) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 0, false, false));
            }
            
            // Visual effect for healed entity
            world.spawnParticle(Particle.HEART, entity.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.01);
        }

        private void createVisualEffects() {
            // Create expanding rings of particles
            double currentRadius = radius * (1.0 - (ticks / (double) maxTicks));
            
            // Main ring
            for (int i = 0; i < 36; i++) {
                double angle = 2 * Math.PI * i / 36;
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Inner ring
            double innerRadius = currentRadius * 0.7;
            for (int i = 0; i < 24; i++) {
                double angle = 2 * Math.PI * i / 24;
                double x = innerRadius * Math.cos(angle);
                double z = innerRadius * Math.sin(angle);
                Location particleLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Central beam
            if (ticks % 4 == 0) {
                for (int i = 0; i < 10; i++) {
                    Location beamLoc = center.clone().add(0, i * 0.5, 0);
                    world.spawnParticle(Particle.WITCH, beamLoc, 2, 0.2, 0.2, 0.2, 0.01);
                }
            }
            
            // Random sparkles
            if (ticks % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double distance = Math.random() * radius;
                    double x = center.getX() + Math.cos(angle) * distance;
                    double z = center.getZ() + Math.sin(angle) * distance;
                    Location sparkleLoc = new Location(world, x, center.getY() + 0.1, z);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, sparkleLoc, 1, 0, 0, 0, 0);
                }
            }
        }
    }
}