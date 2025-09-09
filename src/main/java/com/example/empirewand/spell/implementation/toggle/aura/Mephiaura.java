package com.example.empirewand.spell.implementation.toggle.aura;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.spell.toggle.ToggleableSpell;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Mephiaura - Toggleable aura that pushes/knocks back players within the aura
 * to the outside,
 * dealing light damage and poison. The caster is not affected.
 */
public final class Mephiaura extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, AuraData> auras = new WeakHashMap<>();

    /* ---------------------------------------- */
    /* CONFIG RECORD */
    /* ---------------------------------------- */
    public record Config(
            int maxDurationTicks,
            double auraRadius,
            double pushForce,
            double damage,
            double poisonChance,
            int poisonDurationTicks,
            int poisonAmplifier,
            int pushIntervalTicks,
            int particleIntervalTicks,
            String particleType,
            String activateSound,
            String deactivateSound,
            String auraSound,
            String messagesActivate,
            String messagesDeactivate,
            String messagesNoEnergy,
            float activateVolume,
            float activatePitch,
            float deactivateVolume,
            float deactivatePitch,
            float auraVolume,
            float auraPitch) {
    }

    private Config config;

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Mephiaura";
            description = "Creates an aura that pushes enemies away and poisons them.";
            cooldown = Duration.ofSeconds(12);
            spellType = SpellType.POISON; // Using POISON type since it has poison effects
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new Mephiaura(this);
        }
    }

    private Mephiaura(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "mephiaura";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    /* ---------------------------------------- */
    /* CONFIG LOADING */
    /* ---------------------------------------- */
    @Override
    public void loadConfig(@NotNull com.example.empirewand.core.config.ReadableConfig spellConfig) {
        super.loadConfig(spellConfig);
        this.config = new Config(
                spellConfig.getInt("max-duration-ticks", 600), // 30 seconds
                spellConfig.getDouble("aura.radius", 6.0),
                spellConfig.getDouble("aura.push-force", 1.5),
                spellConfig.getDouble("aura.damage", 2.0),
                spellConfig.getDouble("aura.poison-chance", 0.4),
                spellConfig.getInt("aura.poison-duration-ticks", 100),
                spellConfig.getInt("aura.poison-amplifier", 0),
                spellConfig.getInt("aura.push-interval-ticks", 10), // Every 0.5 seconds
                spellConfig.getInt("aura.particle-interval-ticks", 5), // Every 0.25 seconds
                spellConfig.getString("particles.type", "SPELL_WITCH"),
                spellConfig.getString("sounds.activate", "ENTITY_EVOKER_PREPARE_ATTACK"),
                spellConfig.getString("sounds.deactivate", "ENTITY_EVOKER_CAST_SPELL"),
                spellConfig.getString("sounds.aura", "ENTITY_WITHER_AMBIENT"),
                spellConfig.getString("messages.activate", "§5🔮 §dMephiaura activated!"),
                spellConfig.getString("messages.deactivate", "§5🔮 §dMephiaura deactivated."),
                spellConfig.getString("messages.no-energy", "§cNot enough energy to maintain the aura."),
                (float) spellConfig.getDouble("sounds.activate-volume", 0.8),
                (float) spellConfig.getDouble("sounds.activate-pitch", 1.2),
                (float) spellConfig.getDouble("sounds.deactivate-volume", 0.8),
                (float) spellConfig.getDouble("sounds.deactivate-pitch", 0.8),
                (float) spellConfig.getDouble("sounds.aura-volume", 0.3),
                (float) spellConfig.getDouble("sounds.aura-pitch", 1.0));
    }

    /* ---------------------------------------- */
    /* TOGGLE API */
    /* ---------------------------------------- */
    @Override
    public boolean isActive(Player player) {
        return auras.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player))
            return;
        auras.put(player.getUniqueId(), new AuraData(player, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        Optional.ofNullable(auras.remove(player.getUniqueId())).ifPresent(AuraData::stop);
    }

    @Override
    public void forceDeactivate(Player player) {
        Optional.ofNullable(auras.remove(player.getUniqueId())).ifPresent(AuraData::stop);
    }

    @Override
    public int getMaxDuration() {
        return config != null ? config.maxDurationTicks : 600;
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class AuraData {
        private final Player player;
        private final SpellContext context;
        private final BukkitTask ticker;
        private int energy = 100;
        private long lastPush = 0;
        private long lastParticle = 0;

        AuraData(Player player, SpellContext context) {
            this.player = player;
            this.context = context;

            // Play activation effects
            playActivationEffects();
            context.fx().actionBar(player, config.messagesActivate);

            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            context.fx().actionBar(player, config.messagesDeactivate);
            playDeactivationEffects();
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            if (energy <= 0) {
                context.fx().actionBar(player, config.messagesNoEnergy);
                forceDeactivate(player);
                return;
            }

            // Consume energy
            energy -= 0.5; // Slowly drain energy
            long currentTime = System.currentTimeMillis();

            // Push enemies away
            if (currentTime - lastPush >= (long) config.pushIntervalTicks * 50) {
                pushEnemies();
                lastPush = currentTime;
            }

            // Display particles
            if (currentTime - lastParticle >= (long) config.particleIntervalTicks * 50) {
                displayParticles();
                lastParticle = currentTime;
            }
        }

        private void pushEnemies() {
            Location playerLoc = player.getLocation();

            // Get nearby entities within aura radius
            for (Entity entity : playerLoc.getWorld().getNearbyEntities(playerLoc, config.auraRadius, config.auraRadius,
                    config.auraRadius)) {
                // Skip the caster and non-living entities
                if (entity.equals(player) || !(entity instanceof LivingEntity)) {
                    continue;
                }

                LivingEntity living = (LivingEntity) entity;

                // Calculate vector from player to entity
                Vector direction = living.getLocation().toVector().subtract(playerLoc.toVector()).normalize();

                // Apply knockback
                Vector push = direction.multiply(config.pushForce);
                living.setVelocity(living.getVelocity().add(push));

                // Apply damage
                living.damage(config.damage, player);

                // Apply poison with chance
                if (Math.random() < config.poisonChance) {
                    living.addPotionEffect(new PotionEffect(
                            PotionEffectType.POISON,
                            config.poisonDurationTicks,
                            config.poisonAmplifier,
                            false,
                            true));
                }

                // Play effect on pushed entity
                try {
                    Particle particle = Particle.valueOf(config.particleType);
                    context.fx().spawnParticles(living.getLocation(), particle, 5, 0.2, 0.2, 0.2, 0.1);
                } catch (IllegalArgumentException e) {
                    context.fx().spawnParticles(living.getLocation(), Particle.SMOKE, 5, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }

        private void displayParticles() {
            Location playerLoc = player.getLocation();

            // Create a ring of particles around the player
            for (int i = 0; i < 16; i++) {
                double angle = 2 * Math.PI * i / 16;
                double x = playerLoc.getX() + Math.cos(angle) * config.auraRadius;
                double z = playerLoc.getZ() + Math.sin(angle) * config.auraRadius;
                Location particleLoc = new Location(playerLoc.getWorld(), x, playerLoc.getY() + 0.5, z);

                try {
                    Particle particle = Particle.valueOf(config.particleType);
                    context.fx().spawnParticles(particleLoc, particle, 3, 0.1, 0.1, 0.1, 0.05);
                } catch (IllegalArgumentException e) {
                    context.fx().spawnParticles(particleLoc, Particle.SMOKE, 3, 0.1, 0.1, 0.1, 0.05);
                }
            }

            // Play aura sound occasionally
            if (Math.random() < 0.1) { // 10% chance per particle tick
                try {
                    Sound sound = Sound.valueOf(config.auraSound);
                    context.fx().playSound(playerLoc, sound, config.auraVolume, config.auraPitch);
                } catch (IllegalArgumentException e) {
                    context.fx().playSound(playerLoc, Sound.ENTITY_WITHER_AMBIENT, config.auraVolume, config.auraPitch);
                }
            }
        }

        private void playActivationEffects() {
            Location loc = player.getLocation();

            // Spawn activation particles
            try {
                Particle particle = Particle.valueOf(config.particleType);
                context.fx().spawnParticles(loc, particle, 30, 0.5, 0.5, 0.5, 0.1);
            } catch (IllegalArgumentException e) {
                context.fx().spawnParticles(loc, Particle.SMOKE, 30, 0.5, 0.5, 0.5, 0.1);
            }

            // Play activation sound
            try {
                Sound sound = Sound.valueOf(config.activateSound);
                context.fx().playSound(loc, sound, config.activateVolume, config.activatePitch);
            } catch (IllegalArgumentException e) {
                context.fx().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, config.activateVolume,
                        config.activatePitch);
            }
        }

        private void playDeactivationEffects() {
            Location loc = player.getLocation();

            // Spawn deactivation particles
            try {
                Particle particle = Particle.valueOf(config.particleType);
                context.fx().spawnParticles(loc, particle, 20, 0.3, 0.3, 0.3, 0.1);
            } catch (IllegalArgumentException e) {
                context.fx().spawnParticles(loc, Particle.SMOKE, 20, 0.3, 0.3, 0.3, 0.1);
            }

            // Play deactivation sound
            try {
                Sound sound = Sound.valueOf(config.deactivateSound);
                context.fx().playSound(loc, sound, config.deactivateVolume, config.deactivatePitch);
            } catch (IllegalArgumentException e) {
                context.fx().playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, config.deactivateVolume,
                        config.deactivatePitch);
            }
        }
    }
}