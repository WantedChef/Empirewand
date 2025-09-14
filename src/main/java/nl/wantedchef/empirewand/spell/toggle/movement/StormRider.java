package nl.wantedchef.empirewand.spell.toggle.movement;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * StormRider 2.0 - Devastating storm aura that commands the fury of nature.
 *
 * Features:
 * - Massive storm aura with dark storm clouds surrounding the caster
 * - Living entities entering the aura trigger 3 lightning strikes on themselves
 * - Continuous storm cloud particles and electrical effects
 * - Thunder and lightning sounds for immersive storm experience
 * - Storm intensity that builds over time
 * - Configurable aura radius and lightning damage
 */
public final class StormRider extends Spell<Void> implements ToggleableSpell {

    private final Map<UUID, StormAuraData> stormAuras = new WeakHashMap<>();

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Storm Aura";
            description = "Summon a devastating storm aura that strikes intruders with lightning.";
            cooldown = Duration.ofSeconds(5);
            spellType = SpellType.AURA;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new StormRider(this);
        }
    }

    private StormRider(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public @NotNull String key() {
        return "storm-rider";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    @Override
    public boolean isActive(@NotNull Player player) {
        return stormAuras.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player))
            return;
        stormAuras.put(player.getUniqueId(), new StormAuraData(player, context));
    }

    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        Optional.ofNullable(stormAuras.remove(player.getUniqueId())).ifPresent(StormAuraData::stop);
    }

    @Override
    public void forceDeactivate(@NotNull Player player) {
        Optional.ofNullable(stormAuras.remove(player.getUniqueId())).ifPresent(StormAuraData::stop);
    }

    @Override
    public int getMaxDuration() {
        return cfgInt("max-duration-ticks", 1800); // 1.5 minutes default
    }

    private final class StormAuraData {
        private final Player player;
        private final SpellContext context;
        private final BossBar stormIntensityBar = BossBar.bossBar(
            Component.text("Storm Intensity"),
            0.1f,
            BossBar.Color.PURPLE,
            BossBar.Overlay.PROGRESS
        );
        private final BukkitTask ticker;
        private final Set<UUID> recentlyStruck = new HashSet<>();

        // Storm aura variables
        private int tickCounter = 0;
        private double stormIntensity = 0.1;
        private double auraPhase = 0;
        private int entitiesStruck = 0;

        StormAuraData(Player player, SpellContext context) {
            this.player = player;
            this.context = context;

            player.showBossBar(stormIntensityBar);

            // Storm aura activation sounds
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.WEATHER_RAIN_ABOVE, 0.8f, 1.0f);
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.2f);

            // Show storm aura activation message
            player.sendMessage(Component.text(
                    "The storm gathers around you, ready to strike your foes!"));

            // Spawn initial storm aura effect
            spawnStormActivationEffect();

            // Start the storm aura ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            player.hideBossBar(stormIntensityBar);

            // Final storm dissipation effect
            spawnStormDeactivationEffect();

            player.sendMessage(Component.text("The storm dissipates, its fury spent..."));
            player.playSound(player.getLocation(), Sound.WEATHER_RAIN_ABOVE, 0.4f, 0.6f);
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            tickCounter++;
            auraPhase += 0.2;

            // Gradually build storm intensity
            stormIntensity = Math.min(1.0, stormIntensity + cfgDouble("storm.intensity-growth", 0.002));

            Location playerLoc = player.getLocation();

            // Update boss bar
            stormIntensityBar.progress((float) stormIntensity);
            stormIntensityBar.name(Component.text("Storm Intensity - Entities Struck: " + entitiesStruck));

            // Spawn storm aura visual effects
            spawnStormAura(playerLoc);

            // Check for entities entering the aura and strike them with lightning
            checkForLightningTargets(playerLoc);

            // Clean up recently struck entities list periodically
            if (tickCounter % 100 == 0) {
                recentlyStruck.clear();
            }

            // Play ambient storm sounds
            playStormSounds(playerLoc);
        }
        private void spawnStormAura(Location center) {
            World world = center.getWorld();
            double auraRadius = cfgDouble("aura.radius", 8.0);
            int cloudParticles = cfgInt("aura.cloud-particles", 40);

            // Storm cloud particles around the aura perimeter
            for (int i = 0; i < cloudParticles; i++) {
                double angle = 2 * Math.PI * i / cloudParticles + auraPhase * 0.5;
                double radius = auraRadius * (0.8 + 0.2 * Math.sin(auraPhase + i * 0.3));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = 2.0 + Math.sin(auraPhase * 0.7 + angle * 2) * 1.2 * stormIntensity;

                Location cloudLoc = center.clone().add(x, y, z);

                // Dark storm clouds
                world.spawnParticle(Particle.SMOKE, cloudLoc, 2, 0.3, 0.2, 0.3, 0.02);
                world.spawnParticle(Particle.CLOUD, cloudLoc, 1, 0.2, 0.1, 0.2, 0.01);

                // Electric activity in clouds
                if (Math.random() < 0.3 * stormIntensity) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, cloudLoc, 1, 0.2, 0.2, 0.2, 0.02);

                    // Dark storm cloud color
                    Color stormColor = Color.fromRGB(60, 60, 90);
                    world.spawnParticle(Particle.DUST, cloudLoc, 1, 0.15, 0.15, 0.15, 0,
                        new Particle.DustOptions(stormColor, 1.2f));
                }
            }

            // Inner storm swirl
            int innerParticles = cfgInt("aura.inner-particles", 20);
            for (int i = 0; i < innerParticles; i++) {
                double angle = auraPhase * 2 + i * 0.4;
                double radius = (innerParticles - i) * 0.3;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = 1.5 + i * 0.1;

                Location innerLoc = center.clone().add(x, y, z);

                // Swirling storm energy
                world.spawnParticle(Particle.ELECTRIC_SPARK, innerLoc, 1, 0.1, 0.1, 0.1, 0.01);

                if (Math.random() < 0.5) {
                    Color energyColor = Color.fromRGB(100, 100, 200);
                    world.spawnParticle(Particle.DUST, innerLoc, 1, 0.08, 0.08, 0.08, 0,
                        new Particle.DustOptions(energyColor, 1.0f));
                }
            }
        }

        private void checkForLightningTargets(Location center) {
            double auraRadius = cfgDouble("aura.radius", 8.0);
            double lightningDamage = cfgDouble("lightning.damage", 6.0);

            // Find living entities in the storm aura
            for (Entity entity : center.getWorld().getNearbyEntities(center, auraRadius, auraRadius, auraRadius)) {
                if (!(entity instanceof LivingEntity)) continue;
                if (entity.equals(player)) continue; // Don't strike the caster
                if (recentlyStruck.contains(entity.getUniqueId())) continue;

                LivingEntity target = (LivingEntity) entity;
                double distance = center.distance(entity.getLocation());

                if (distance <= auraRadius) {
                    // Strike this entity with 3 lightning bolts
                    strikeLightning(target, lightningDamage);
                    recentlyStruck.add(entity.getUniqueId());
                    entitiesStruck++;

                    // Message to caster
                    player.sendMessage(Component.text("Lightning strikes " +
                        (target instanceof Player ? ((Player) target).getName() : target.getType().name()) + "!"));
                }
            }
        }
        private void strikeLightning(LivingEntity target, double damage) {
            Location targetLoc = target.getLocation();

            // Strike with 3 lightning bolts in sequence
            for (int i = 0; i < 3; i++) {
                int delay = i * 5; // 5 tick delay between strikes

                Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
                    if (!target.isDead() && target.isValid()) {
                        // Create lightning visual effect
                        spawnLightningBolt(targetLoc);

                        // Damage the target
                        target.damage(damage, player);

                        // Lightning strike sounds
                        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
                        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.2f);
                    }
                }, delay);
            }
        }
        private void spawnLightningBolt(Location target) {
            World world = target.getWorld();
            Location skyLoc = target.clone().add(0, 15, 0);

            // Create lightning bolt from sky to target
            Vector direction = target.toVector().subtract(skyLoc.toVector()).normalize();
            double distance = skyLoc.distance(target);

            for (int i = 0; i < 20; i++) {
                double progress = (double) i / 20;
                Location boltLoc = skyLoc.clone().add(direction.clone().multiply(distance * progress));

                // Add slight zigzag to make it look more realistic
                double zigzag = Math.sin(progress * Math.PI * 6) * 0.3;
                boltLoc.add(zigzag, 0, zigzag * 0.5);

                // Lightning bolt particles
                world.spawnParticle(Particle.ELECTRIC_SPARK, boltLoc, 3, 0.1, 0.1, 0.1, 0.05);
                world.spawnParticle(Particle.END_ROD, boltLoc, 1, 0.05, 0.05, 0.05, 0.02);

                // Bright white lightning core
                Color lightningColor = Color.fromRGB(255, 255, 255);
                world.spawnParticle(Particle.DUST, boltLoc, 2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(lightningColor, 2.0f));
            }

            // Lightning impact effect
            spawnLightningImpact(target);
        }
        private void spawnLightningImpact(Location impact) {
            World world = impact.getWorld();

            // Lightning impact explosion
            for (int i = 0; i < 30; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 2.0;
                double height = Math.random() * 1.0;

                Location impactLoc = impact.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );

                // Impact particles
                world.spawnParticle(Particle.ELECTRIC_SPARK, impactLoc, 2, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticle(Particle.END_ROD, impactLoc, 1, 0.1, 0.1, 0.1, 0.03);

                // Lightning impact flash
                Color impactColor = Color.fromRGB(255, 255, 200);
                world.spawnParticle(Particle.DUST, impactLoc, 1, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(impactColor, 1.5f));
            }
        }
        private void spawnStormActivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();

            // Storm aura formation effect
            double auraRadius = cfgDouble("aura.radius", 8.0);

            for (int i = 0; i < 50; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * auraRadius;
                double height = Math.random() * 4.0 + 1.0;

                Location stormLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );

                // Storm gathering effect
                world.spawnParticle(Particle.SMOKE, stormLoc, 3, 0.3, 0.3, 0.3, 0.02);
                world.spawnParticle(Particle.CLOUD, stormLoc, 2, 0.2, 0.2, 0.2, 0.01);
                world.spawnParticle(Particle.ELECTRIC_SPARK, stormLoc, 1, 0.2, 0.2, 0.2, 0.02);

                // Dark storm color
                Color stormColor = Color.fromRGB(70, 70, 100);
                world.spawnParticle(Particle.DUST, stormLoc, 1, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(stormColor, 1.3f));
            }
        }

        private void spawnStormDeactivationEffect() {
            Location loc = player.getLocation();
            World world = player.getWorld();

            // Storm dissipation
            for (int i = 0; i < 30; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 6.0;
                double height = Math.random() * 3.0 + 1.0;

                Location dissipationLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );

                world.spawnParticle(Particle.CLOUD, dissipationLoc, 1, 0.3, 0.2, 0.3, 0.005);
                world.spawnParticle(Particle.SMOKE, dissipationLoc, 1, 0.2, 0.1, 0.2, 0.002);

                // Fading electrical activity
                if (Math.random() < 0.2) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, dissipationLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }

        private void playStormSounds(Location loc) {
            if (tickCounter % 80 == 0) {
                // Ambient storm sounds
                player.playSound(loc, Sound.WEATHER_RAIN_ABOVE, 0.4f, 1.0f);

                if (Math.random() < 0.3) {
                    player.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.3f, 1.1f);
                }
            }
        }
    }

    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("storm-rider." + path, def);
    }

    private int cfgInt(String path, int def) {
        return spellConfig.getInt("storm-rider." + path, def);
    }
}
