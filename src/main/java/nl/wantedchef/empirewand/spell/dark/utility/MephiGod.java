package nl.wantedchef.empirewand.spell.dark.utility;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.*;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.core.EmpireWandPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * MephiGod - Mephidantes-inspired dominance stance with layered buffs for the
 * caster
 * and suppression in the surrounding area, thematically anchored in Kingdom
 * lore.
 */
public final class MephiGod extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, GodData> gods = new WeakHashMap<>();
    private EmpireWandPlugin plugin;

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Mephi God";
            description = "Mephidantes-inspired dominance stance with layered buffs for the caster and suppression in the surrounding area.";
            cooldown = Duration.ofSeconds(60);
            spellType = SpellType.DARK;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new MephiGod(this);
        }
    }

    private MephiGod(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
    @Override
    public String key() {
        return "mephi-god";
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
    /* TOGGLE API */
    /* ---------------------------------------- */
    @Override
    public boolean isActive(Player player) {
        return gods.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player))
            return;
        plugin = context.plugin();
        gods.put(player.getUniqueId(), new GodData(player, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        Optional.ofNullable(gods.remove(player.getUniqueId())).ifPresent(GodData::stop);
    }

    @Override
    public void forceDeactivate(Player player) {
        deactivate(player, null);
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class GodData {
        private final Player player;
        private final BukkitTask ticker;
        private final BukkitTask suppressionTicker;
        private final double auraRadius;
        private final int durationTicks;
        private int tickCounter = 0;

        GodData(Player player, SpellContext context) {
            this.player = player;
            this.auraRadius = spellConfig.getDouble("values.radius", 10.0);
            this.durationTicks = spellConfig.getInt("values.duration-ticks", 600); // 30 seconds default

            // Apply caster buffs
            applyCasterBuffs();

            // Send activation message
            player.sendMessage(Component.text("§5⚡ §7Mephi God activated. Underworld dominion engaged."));

            // Start the main ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1); // Run every tick

            // Start suppression ticker (every 5 ticks)
            this.suppressionTicker = Bukkit.getScheduler().runTaskTimer(plugin, this::applySuppression, 0, 5);
        }

        void stop() {
            ticker.cancel();
            suppressionTicker.cancel();

            // Remove caster buffs
            removeCasterBuffs();

            // Spawn final shadow pulse
            spawnShadowPulse();

            player.sendMessage(Component.text("§5⚡ §7Mephi God deactivated. Dominion released."));
        }

        private void tick() {
            if (!player.isOnline() || player.isDead()) {
                forceDeactivate(player);
                return;
            }

            // Increment tick counter
            tickCounter++;

            // Check if duration has expired
            if (tickCounter >= durationTicks) {
                forceDeactivate(player);
                return;
            }

            // Apply aura effects periodically
            if (tickCounter % 3 == 0) {
                spawnAuraParticles();
            }
        }

        private void applyCasterBuffs() {
            // Apply speed buff
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    durationTicks,
                    spellConfig.getInt("values.speed-amplifier", 1),
                    false,
                    false));

            // Apply strength buff
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH,
                    durationTicks,
                    spellConfig.getInt("values.strength-amplifier", 0),
                    false,
                    false));

            // Apply resistance buff
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE,
                    durationTicks,
                    spellConfig.getInt("values.resistance-amplifier", 0),
                    false,
                    false));

            // Apply fire resistance
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE,
                    durationTicks,
                    0,
                    false,
                    false));
        }

        private void removeCasterBuffs() {
            // Remove potion effects
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        }

        private void spawnAuraParticles() {
            Location loc = player.getLocation().add(0, 1, 0);
            World world = player.getWorld();

            // Create a ring of particles around the player
            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                double x = Math.cos(angle) * auraRadius;
                double z = Math.sin(angle) * auraRadius;
                Location particleLoc = loc.clone().add(x, 0, z);

                // Alternate between different particle types
                if (tickCounter % 6 < 2) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                } else if (tickCounter % 6 < 4) {
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
                } else {
                    world.spawnParticle(Particle.ASH, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Create occasional critical particles for visual effect
            if (Math.random() < 0.4) {
                world.spawnParticle(Particle.CRIT, loc, 8, 0.8, 0.8, 0.8, 0.05);
            }

            // Create end rod particles for mystical effect
            if (Math.random() < 0.3) {
                world.spawnParticle(Particle.END_ROD, loc, 3, 0.5, 0.5, 0.5, 0.02);
            }
        }

        private void applySuppression() {
            Location playerLoc = player.getLocation();
            World world = player.getWorld();

            // Get nearby entities
            for (Entity entity : world.getNearbyEntities(playerLoc, auraRadius, auraRadius, auraRadius)) {
                if (entity instanceof LivingEntity && entity != player && !entity.isDead()) {
                    LivingEntity livingEntity = (LivingEntity) entity;

                    // Apply suppression effects
                    // Slow effect
                    livingEntity.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            40, // 2 seconds
                            spellConfig.getInt("values.suppression-slowness-amplifier", 1),
                            false,
                            true));

                    // Weakness effect
                    livingEntity.addPotionEffect(new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            40, // 2 seconds
                            spellConfig.getInt("values.suppression-weakness-amplifier", 0),
                            false,
                            true));

                    // Darkness effect (blindness-like)
                    livingEntity.addPotionEffect(new PotionEffect(
                            PotionEffectType.DARKNESS,
                            40, // 2 seconds
                            0,
                            false,
                            true));

                    // Spawn suppression particles
                    Location entityLoc = livingEntity.getLocation().add(0, 1, 0);
                    world.spawnParticle(Particle.SMOKE, entityLoc, 5, 0.3, 0.3, 0.3, 0.03);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, entityLoc, 3, 0.2, 0.2, 0.2, 0.02);
                }
            }
        }

        private void spawnShadowPulse() {
            Location loc = player.getLocation();
            World world = player.getWorld();

            // Create expanding circle of particles
            for (int r = 1; r <= auraRadius; r++) {
                for (int i = 0; i < 16; i++) {
                    double angle = 2 * Math.PI * i / 16;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Location particleLoc = loc.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0, 0, 0, 0);
                }
            }

            // Play pulse sound
            world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
        }
    }
}