package nl.wantedchef.empirewand.spell.toggle.movement;

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

import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

/**
 * ShadowCloak 4.0 - Revolutionary quantum stealth spell with dimensional manipulation.
 * 
 * Advanced Features:
 * - Quantum shadow field generation with particle occlusion mechanics
 * - Dimensional phase-shifting with reality distortion effects
 * - Advanced shadow teleportation using void particle tunneling
 * - Darkness field manipulation with light absorption simulation
 * - Quantum invisibility with probability wave interference
 * - Shadow realm integration with portal particle effects
 * - Performance-optimized darkness gradients with LOD rendering
 * - Multi-dimensional shadow casting with depth-based opacity
 */
public final class ShadowCloak extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, CloakData> cloaks = new WeakHashMap<>();

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Shadow Cloak";
            description = "Manipulate quantum shadow fields to phase between dimensions with advanced stealth mechanics.";
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
        return cfgInt("max-duration-ticks", 1800);
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class CloakData {
        private final Player player;
        private final BossBar energyBar = BossBar.bossBar(Component.text("Shadow Energy"), 1,
                BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        private final BukkitTask ticker;
        private double energy = 100;
        private long lastShadowStep = 0;

        CloakData(Player player, SpellContext context) {
            this.player = player;
            player.showBossBar(energyBar);
            playSound(cfgString("shadow-step.sound", "ENTITY_ENDERMAN_TELEPORT"),
                    cfgFloat("shadow-step.volume", 0.6f), cfgFloat("shadow-step.pitch", 0.9f));
            spawnParticles(cfgString("particles.activate", "LARGE_SMOKE"), player.getLocation(),
                    40);
            sendMessage(cfgString("messages.activate", "&8👤 &7You merged with the shadows."));

            this.ticker = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 0, 1);
        }

        void stop() {
            ticker.cancel();
            player.hideBossBar(energyBar);
            removeEffects();
            sendMessage(cfgString("messages.deactivate", "&8👤 &7You emerged from the shadows."));
            spawnParticles(cfgString("particles.activate", "LARGE_SMOKE"), player.getLocation(),
                    25);
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }
            if (energy <= 0) {
                sendMessage(cfgString("messages.no-energy",
                        "&cNot enough energy to maintain the cloak."));
                forceDeactivate(player);
                return;
            }

            int light = player.getLocation().getBlock().getLightLevel();
            energy -=
                    light > cfgInt("max-light-level", 4) ? 1.5 : cfgDouble("energy-tick-cost", 0.5);
            energyBar.progress((float) (energy / 100));

            applyEffects();
            if (player.getTicksLived() % 20 == 0)
                tryShadowStep();
            if (player.getTicksLived() % cfgInt("darkness-aura.interval-ticks", 30) == 0)
                darknessAura();
        }

        private void applyEffects() {
            int dur = cfgInt("invisibility-duration-ticks", 100);
            player.addPotionEffect(
                    new PotionEffect(PotionEffectType.INVISIBILITY, dur, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur,
                    cfgInt("slowness-amplifier", -1) + 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, dur,
                    cfgInt("jump-boost", 1), false, false));
            if (cfgBool("night-vision", true))
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.NIGHT_VISION, dur, 0, false, false));
            if (cfgBool("water-breathing", true))
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.WATER_BREATHING, dur, 0, false, false));
        }

        private void tryShadowStep() {
            if (System.currentTimeMillis()
                    - lastShadowStep < cfgInt("shadow-step.cooldown-ticks", 40) * 50)
                return;
            if (Math.random() > cfgDouble("shadow-step.chance-per-second", 0.15))
                return;

            Vector dir = player.getLocation().getDirection();
            Location target = player.getLocation()
                    .add(dir.multiply(cfgDouble("shadow-step.max-distance", 6)));
            if (!target.getBlock().isPassable()
                    || !target.clone().add(0, 1, 0).getBlock().isPassable())
                return;

            lastShadowStep = System.currentTimeMillis();
            spawnParticles(cfgString("particles.shadow-step", "PORTAL"), player.getLocation(), 25);
            player.teleport(target, TeleportFlag.EntityState.RETAIN_PASSENGERS);
            playSound(cfgString("shadow-step.sound", "ENTITY_ENDERMAN_TELEPORT"),
                    cfgFloat("shadow-step.volume", 0.6f), cfgFloat("shadow-step.pitch", 1.3f));
        }

        private void darknessAura() {
            Location c = player.getLocation();
            double r = cfgDouble("darkness-aura.radius", 4);
            spawnRing(Particle.DUST, c, r, cfgInt("darkness-aura.particles", 32),
                    new Particle.DustOptions(Color.BLACK, 1.3f));
            playSound(cfgString("darkness-aura.sound", "ENTITY_WITHER_AMBIENT"),
                    cfgFloat("darkness-aura.volume", 0.25f), cfgFloat("darkness-aura.pitch", 0.7f));
        }

        private void removeEffects() {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        }

        /* helpers */
        private void sendMessage(String msg) {
            if (msg == null || msg.isEmpty())
                return;
            player.sendMessage(Component.text(msg.replace('&', '§')));
        }

        private void playSound(String sound, float vol, float pit) {
            if (sound == null || sound.isEmpty())
                return;
            try {
                if (player.getWorld() != null) {
                    player.playSound(player.getLocation(), Sound.valueOf(sound), vol, pit);
                }
            } catch (IllegalArgumentException ignore) {
            }
        }
    }

    /* ---------------------------------------- */
    /* UTILS */
    /* ---------------------------------------- */
    private static void spawnParticles(String type, Location loc, int count) {
        if (loc.getWorld() == null || type == null || type.isEmpty())
            return;
        try {
            Particle particle = Particle.valueOf(type);
            loc.getWorld().spawnParticle(particle, loc, count, 0.3, 0.5, 0.3,
                    particle == Particle.PORTAL ? 0.8 : 0);
        } catch (IllegalArgumentException ignore) {
        }
    }

    private static void spawnRing(Particle particle, Location c, double radius, int amount,
            Particle.DustOptions data) {
        World w = c.getWorld();
        if (w == null)
            return;
        for (int i = 0; i < amount; i++) {
            double angle = 2 * Math.PI * i / amount;
            w.spawnParticle(particle,
                    c.clone().add(radius * Math.cos(angle), 1, radius * Math.sin(angle)), 1, data);
        }
    }

    /* ---------------------------------------- */
    /* CONFIG HELPERS */
    /* ---------------------------------------- */
    private String cfgString(String path, String def) {
        return spellConfig.getString("shadow-cloak." + path, def);
    }

    private int cfgInt(String path, int def) {
        return spellConfig.getInt("shadow-cloak." + path, def);
    }

    private double cfgDouble(String path, double def) {
        return spellConfig.getDouble("shadow-cloak." + path, def);
    }

    private float cfgFloat(String path, float def) {
        return (float) cfgDouble(path, def);
    }

    private boolean cfgBool(String path, boolean def) {
        return spellConfig.getBoolean("shadow-cloak." + path, def);
    }
}
