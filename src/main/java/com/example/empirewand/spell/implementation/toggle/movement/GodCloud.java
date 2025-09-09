package com.example.empirewand.spell.implementation.toggle.movement;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.annotations.kajcloud;
import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.spell.toggle.ToggleableSpell;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;

/**
 * GodCloud - KajCloud-stijl: FIREWORK + CLOUD bursts direct onder de voeten.
 * Activeert alleen tijdens vliegen/gliden. Toggleable en lekvrij.
 */
@kajcloud
public class GodCloud extends Spell<Void> implements ToggleableSpell {

    // Config voor type-safe laden
    public record Config(
            int durationTicks,
            int particleInterval,
            int densityModifier, // bepaalt 10 * density als basisaantal
            double spreadXZ,
            double yOffset) {
    }

    // Defaults
    private static final int DEFAULT_DURATION_TICKS = 600; // 30s
    private static final int DEFAULT_PARTICLE_INTERVAL = 2; // elke 2 ticks
    private static final int DEFAULT_DENSITY_MODIFIER = 3; // 10 * 3 = 30 deeltjes
    private static final double DEFAULT_SPREAD_XZ = 0.3D;
    private static final double DEFAULT_Y_OFFSET = 0.10D;

    // Actieve wolken
    private final Map<UUID, CloudTask> active = new ConcurrentHashMap<>();
    private EmpireWandPlugin plugin;

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "God Cloud";
            this.description = "Spawns a divine cloud under your feet while flying (KajCloud style).";
            this.cooldown = java.time.Duration.ofSeconds(45);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new GodCloud(this);
        }
    }

    private GodCloud(Builder builder) {
        super(builder);
        this.plugin = null; // wordt gezet bij gebruik
    }

    @Override
    public String key() {
        return "god-cloud";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        if (this.plugin == null)
            this.plugin = context.plugin();
        toggle(context.caster(), context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // effecten zitten in activate/deactivate
    }

    private Config loadConfig() {
        int durationTicks = spellConfig.getInt("values.duration-ticks", DEFAULT_DURATION_TICKS);
        int interval = spellConfig.getInt("values.particle-interval", DEFAULT_PARTICLE_INTERVAL);
        int density = spellConfig.getInt("values.particle-density-modifier", DEFAULT_DENSITY_MODIFIER);
        double spreadXZ = spellConfig.getDouble("values.spread-xz", DEFAULT_SPREAD_XZ);
        double yOffset = spellConfig.getDouble("values.y-offset", DEFAULT_Y_OFFSET);
        return new Config(durationTicks, interval, density, spreadXZ, yOffset);
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (this.plugin == null)
            this.plugin = context.plugin();

        if (isActive(player)) {
            player.sendMessage("§bGodCloud staat al aan.");
            return;
        }

        Config cfg = loadConfig();
        CloudTask task = new CloudTask(player, cfg);
        active.put(player.getUniqueId(), task);
        task.start();

        player.sendMessage("§b☁ GodCloud geactiveerd!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

        // kleine start-burst
        Location c = player.getLocation().clone().add(0, cfg.yOffset(), 0);
        if (c.getWorld() != null) {
            int base = 10 * Math.max(1, cfg.densityModifier());
            c.getWorld().spawnParticle(Particle.FIREWORK, c, base, cfg.spreadXZ(), 0.0, cfg.spreadXZ(), 0);
            c.getWorld().spawnParticle(Particle.CLOUD, c, base, cfg.spreadXZ(), 0.0, cfg.spreadXZ(), 0);
        }
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        CloudTask task = active.remove(player.getUniqueId());
        if (task == null) {
            player.sendMessage("§7GodCloud is niet actief.");
            return;
        }
        task.stop();

        player.sendMessage("§b☁ GodCloud gedeactiveerd.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 0.8f);

        Location c = player.getLocation();
        if (c.getWorld() != null) {
            c.getWorld().spawnParticle(Particle.CLOUD, c, 20, 1.2, 0.4, 1.2, 0.02);
        }
    }

    @Override
    public boolean isActive(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    @Override
    public void forceDeactivate(Player player) {
        CloudTask task = active.remove(player.getUniqueId());
        if (task != null)
            task.stop();
        if (player.isOnline())
            player.sendMessage("§b☁ GodCloud geforceerd uitgezet.");
    }

    @Override
    public int getMaxDuration() {
        return DEFAULT_DURATION_TICKS;
    }

    /** Periodieke particle taak in KajCloud-stijl. */
    private final class CloudTask {
        private final Player player;
        private final Config cfg;
        private BukkitTask handle;
        private int elapsed; // in ticks

        private CloudTask(Player player, Config cfg) {
            this.player = player;
            this.cfg = cfg;
            this.elapsed = 0;
        }

        void start() {
            handle = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !isActive(player)) {
                        cancel();
                        return;
                    }

                    elapsed += Math.max(1, cfg.particleInterval());
                    if (elapsed >= cfg.durationTicks()) {
                        forceDeactivate(player);
                        cancel();
                        return;
                    }

                    // Alleen tijdens vliegen of gliden
                    if (!(player.isFlying() || player.isGliding()))
                        return; // Player API ok in 1.20+ :contentReference[oaicite:1]{index=1}

                    Location l = player.getLocation().clone().add(0.0, cfg.yOffset(), 0.0);
                    if (l.getWorld() == null)
                        return;

                    int base = 10 * Math.max(1, cfg.densityModifier());

                    // KajCloud-look: FIREWORK + CLOUD met kleine horizontale spreiding en geen
                    // verticale
                    l.getWorld().spawnParticle(Particle.FIREWORK, l, base, cfg.spreadXZ(), 0.0, cfg.spreadXZ(), 0);
                    l.getWorld().spawnParticle(Particle.CLOUD, l, base, cfg.spreadXZ(), 0.0, cfg.spreadXZ(), 0);

                    // subtiele ambience
                    if ((elapsed % 40) == 0) {
                        player.playSound(l, Sound.AMBIENT_CAVE, 0.25f, 1.0f); // Sound enum bestaat in 1.20+
                                                                              // :contentReference[oaicite:2]{index=2}
                    }
                }
            }.runTaskTimer(plugin, 0L, Math.max(1L, cfg.particleInterval()));
        }

        void stop() {
            if (handle != null) {
                handle.cancel();
                handle = null;
            }
        }
    }
}
