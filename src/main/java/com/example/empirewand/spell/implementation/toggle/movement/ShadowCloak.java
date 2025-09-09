package com.example.empirewand.spell.implementation.toggle.movement;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.spell.toggle.ToggleableSpell;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;

import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

/**
 * ShadowCloak 3.0 – Hypixel-grade toggleable stealth spell
 */
public final class ShadowCloak extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, CloakData> cloaks = new WeakHashMap<>();

    /* ---------------------------------------- */
    /* CONFIG RECORD */
    /* ---------------------------------------- */
    public record Config(
            int maxDurationTicks,
            int invisibilityDurationTicks,
            int slownessAmplifier,
            int jumpBoost,
            boolean nightVision,
            boolean waterBreathing,
            int maxLightLevel,
            double energyTickCost,
            int shadowStepCooldownTicks,
            double shadowStepChancePerSecond,
            double shadowStepMaxDistance,
            int darknessAuraIntervalTicks,
            int darknessAuraParticles,
            double darknessAuraRadius,
            String activateParticleType,
            String deactivateParticleType,
            String shadowStepParticleType,
            String particlesActivate,
            String particlesDeactivate,
            String particlesShadowStep,
            String shadowStepSound,
            String darknessAuraSound,
            String messagesActivate,
            String messagesDeactivate,
            String messagesNoEnergy,
            float shadowStepVolume,
            float shadowStepPitchActivate,
            float shadowStepPitchDeactivate,
            float shadowStepPitchStep,
            float darknessAuraVolume,
            float darknessAuraPitch) {
    }

    private Config config;

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Shadow Cloak";
            description = "Become one with the shadows.";
            cooldown = Duration.ofSeconds(8);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new ShadowCloak(this);
        }
    }

    private ShadowCloak(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "shadow-cloak";
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
                spellConfig.getInt("max-duration-ticks", 1800),
                spellConfig.getInt("invisibility-duration-ticks", 100),
                spellConfig.getInt("slowness-amplifier", -1),
                spellConfig.getInt("jump-boost", 1),
                spellConfig.getBoolean("night-vision", true),
                spellConfig.getBoolean("water-breathing", true),
                spellConfig.getInt("max-light-level", 4),
                spellConfig.getDouble("energy-tick-cost", 0.5),
                spellConfig.getInt("shadow-step.cooldown-ticks", 40),
                spellConfig.getDouble("shadow-step.chance-per-second", 0.15),
                spellConfig.getDouble("shadow-step.max-distance", 6.0),
                spellConfig.getInt("darkness-aura.interval-ticks", 30),
                spellConfig.getInt("darkness-aura.particles", 32),
                spellConfig.getDouble("darkness-aura.radius", 4.0),
                spellConfig.getString("particles.activate.type", "LARGE_SMOKE"),
                spellConfig.getString("particles.deactivate.type", "LARGE_SMOKE"),
                spellConfig.getString("particles.shadow-step.type", "PORTAL"),
                spellConfig.getString("particles.activate.type", "LARGE_SMOKE"),
                spellConfig.getString("particles.deactivate.type", "LARGE_SMOKE"),
                spellConfig.getString("particles.shadow-step.type", "PORTAL"),
                spellConfig.getString("shadow-step.sound", "ENTITY_ENDERMAN_TELEPORT"),
                spellConfig.getString("darkness-aura.sound", "ENTITY_WITHER_AMBIENT"),
                spellConfig.getString("messages.activate", "§8👤 §7You merged with the shadows."),
                spellConfig.getString("messages.deactivate", "§8👤 §7You emerged from the shadows."),
                spellConfig.getString("messages.no-energy", "§cNot enough energy to maintain the cloak."),
                (float) spellConfig.getDouble("shadow-step.volume", 0.6),
                (float) spellConfig.getDouble("shadow-step.pitch", 1.3),
                (float) spellConfig.getDouble("shadow-step.pitch", 1.3),
                (float) spellConfig.getDouble("shadow-step.pitch", 1.3),
                (float) spellConfig.getDouble("darkness-aura.volume", 0.25),
                (float) spellConfig.getDouble("darkness-aura.pitch", 0.7));
    }

    /* ---------------------------------------- */
    /* TOGGLE API */
    /* ---------------------------------------- */
    @Override
    public boolean isActive(Player player) {
        return cloaks.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player))
            return;
        cloaks.put(player.getUniqueId(), new CloakData(player, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        Optional.ofNullable(cloaks.remove(player.getUniqueId())).ifPresent(CloakData::stop);
    }

    @Override
    public void forceDeactivate(Player player) {
        Optional.ofNullable(cloaks.remove(player.getUniqueId())).ifPresent(CloakData::stop);
    }

    @Override
    public int getMaxDuration() {
        return config != null ? config.maxDurationTicks : 1800;
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class CloakData {
        private final Player player;
        private final SpellContext context;
        private final BossBar energyBar = BossBar.bossBar(Component.text("Shadow Energy"), 1, BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS);
        private final BukkitTask ticker;
        private double energy = 100;
        private long lastShadowStep = 0;

        CloakData(Player player, SpellContext context) {
            this.player = player;
            this.context = context;
            player.showBossBar(energyBar);

            // Use API for effects
            try {
                Particle particle = Particle.valueOf(config.particlesActivate);
                context.fx().spawnParticles(player.getLocation(), particle, 40, 0.3, 0.5, 0.3, 0);
            } catch (IllegalArgumentException e) {
                // Fallback to default
                context.fx().spawnParticles(player.getLocation(), Particle.PORTAL, 40, 0.3, 0.5, 0.3, 0);
            }

            try {
                Sound sound = Sound.valueOf(config.shadowStepSound);
                context.fx().playSound(player, sound, config.shadowStepVolume, config.shadowStepPitchActivate);
            } catch (IllegalArgumentException e) {
                // Fallback to default
                context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, config.shadowStepVolume,
                        config.shadowStepPitchActivate);
            }

            context.fx().actionBar(player, config.messagesActivate);

            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            player.hideBossBar(energyBar);
            removeEffects();

            // Use API for effects
            try {
                Particle particle = Particle.valueOf(config.particlesDeactivate);
                context.fx().spawnParticles(player.getLocation(), particle, 25, 0.3, 0.5, 0.3, 0);
            } catch (IllegalArgumentException e) {
                context.fx().spawnParticles(player.getLocation(), Particle.PORTAL, 25, 0.3, 0.5, 0.3, 0);
            }

            context.fx().actionBar(player, config.messagesDeactivate);
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

            Location playerLoc = player.getLocation();
            if (playerLoc.getWorld() == null)
                return;

            int light = playerLoc.getBlock().getLightLevel();
            energy -= light > config.maxLightLevel ? 1.5 : config.energyTickCost;
            energyBar.progress((float) (energy / 100));

            applyEffects();
            if (player.getTicksLived() % 20 == 0)
                tryShadowStep();
            if (player.getTicksLived() % config.darknessAuraIntervalTicks == 0)
                darknessAura();
        }

        private void applyEffects() {
            player.addPotionEffect(
                    new PotionEffect(PotionEffectType.INVISIBILITY, config.invisibilityDurationTicks, 0, false, false));
            player.addPotionEffect(
                    new PotionEffect(PotionEffectType.SPEED, config.invisibilityDurationTicks,
                            config.slownessAmplifier + 1, false, false));
            player.addPotionEffect(
                    new PotionEffect(PotionEffectType.JUMP_BOOST, config.invisibilityDurationTicks, config.jumpBoost,
                            false, false));
            if (config.nightVision)
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, config.invisibilityDurationTicks,
                        0, false, false));
            if (config.waterBreathing)
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,
                        config.invisibilityDurationTicks, 0, false, false));
        }

        private void tryShadowStep() {
            if (System.currentTimeMillis() - lastShadowStep < (long) config.shadowStepCooldownTicks * 50)
                return;
            if (Math.random() > config.shadowStepChancePerSecond)
                return;

            Location playerLoc = player.getLocation();
            if (playerLoc.getWorld() == null)
                return;

            Vector dir = playerLoc.getDirection();
            Location target = playerLoc.add(dir.multiply(config.shadowStepMaxDistance));
            if (!target.getBlock().isPassable() || !target.clone().add(0, 1, 0).getBlock().isPassable())
                return;

            lastShadowStep = System.currentTimeMillis();

            // Use API for effects
            try {
                Particle particle = Particle.valueOf(config.particlesShadowStep);
                context.fx().spawnParticles(playerLoc, particle, 25, 0.3, 0.5, 0.3, 0);
            } catch (IllegalArgumentException e) {
                context.fx().spawnParticles(playerLoc, Particle.PORTAL, 25, 0.3, 0.5, 0.3, 0);
            }

            player.teleport(target, TeleportFlag.EntityState.RETAIN_PASSENGERS);

            try {
                Sound sound = Sound.valueOf(config.shadowStepSound);
                context.fx().playSound(player, sound, config.shadowStepVolume, config.shadowStepPitchStep);
            } catch (IllegalArgumentException e) {
                context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, config.shadowStepVolume,
                        config.shadowStepPitchStep);
            }
        }

        private void darknessAura() {
            Location center = player.getLocation();
            if (center.getWorld() == null)
                return;

            // Create ring of particles
            World world = center.getWorld();
            for (int i = 0; i < config.darknessAuraParticles; i++) {
                double angle = 2 * Math.PI * i / config.darknessAuraParticles;
                Location particleLoc = center.clone().add(
                        config.darknessAuraRadius * Math.cos(angle),
                        1,
                        config.darknessAuraRadius * Math.sin(angle));
                world.spawnParticle(Particle.DUST, particleLoc, 1,
                        new Particle.DustOptions(Color.BLACK, 1.3f));
            }

            try {
                Sound sound = Sound.valueOf(config.darknessAuraSound);
                context.fx().playSound(center, sound, config.darknessAuraVolume, config.darknessAuraPitch);
            } catch (IllegalArgumentException e) {
                context.fx().playSound(center, Sound.AMBIENT_CAVE, config.darknessAuraVolume, config.darknessAuraPitch);
            }
        }

        private void removeEffects() {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        }
    }
}