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
import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.spell.toggle.ToggleableSpell;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;

/**
 * MephiCloud - Mephidantes-stijl: duistere zielenvlam + aswolk onder de voeten.
 * Toggleable en veilig.
 */
public class MephiCloud extends Spell<Void> implements ToggleableSpell {

    public record Config(
            int durationTicks,
            int particleInterval,
            int densityModifier,
            double spreadXZ,
            double yOffset) {
    }

    private static final int DEFAULT_DURATION_TICKS = 600;
    private static final int DEFAULT_PARTICLE_INTERVAL = 2;
    private static final int DEFAULT_DENSITY_MODIFIER = 3;
    private static final double DEFAULT_SPREAD_XZ = 0.28D;
    private static final double DEFAULT_Y_OFFSET = 0.10D;

    private final Map<UUID, Task> active = new ConcurrentHashMap<>();
    private EmpireWandPlugin plugin;

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Mephi Cloud";
            this.description = "Dark soul-flame cloud under your feet while flying.";
            this.cooldown = java.time.Duration.ofSeconds(45);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new MephiCloud(this);
        }
    }

    private MephiCloud(Builder builder) {
        super(builder);
        this.plugin = null;
    }

    @Override
    public String key() {
        return "mephi-cloud";
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
            player.sendMessage("§5MephiCloud staat al aan.");
            return;
        }

        Config cfg = loadConfig();
        Task t = new Task(player, cfg);
        active.put(player.getUniqueId(), t);
        t.start();

        player.sendMessage("§5☁ MephiCloud geactiveerd.");
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.25f, 1.0f); // duistere hint
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        Task t = active.remove(player.getUniqueId());
        if (t == null) {
            player.sendMessage("§7MephiCloud is niet actief.");
            return;
        }
        t.stop();

        player.sendMessage("§5☁ MephiCloud gedeactiveerd.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 0.8f);
    }

    @Override
    public boolean isActive(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    @Override
    public void forceDeactivate(Player player) {
        Task t = active.remove(player.getUniqueId());
        if (t != null)
            t.stop();
        if (player.isOnline())
            player.sendMessage("§5☁ MephiCloud geforceerd uitgezet.");
    }

    @Override
    public int getMaxDuration() {
        return DEFAULT_DURATION_TICKS;
    }

    private final class Task {
        private final Player player;
        private final Config cfg;
        private BukkitTask handle;
        private int elapsed;

        private Task(Player player, Config cfg) {
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

                    if (!(player.isFlying() || player.isGliding()))
                        return;

                    Location l = player.getLocation().clone().add(0.0, cfg.yOffset(), 0.0);
                    if (l.getWorld() == null)
                        return;

                    int base = 10 * Math.max(1, cfg.densityModifier());

                    // Mephidantes-look: zielenvlammen + as
                    // SOUL_FIRE_FLAME en ASH bestaan in 1.20.x API.
                    // :contentReference[oaicite:4]{index=4}
                    l.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, l, base, cfg.spreadXZ(), 0.0, cfg.spreadXZ(),
                            0);
                    l.getWorld().spawnParticle(Particle.ASH, l, base, cfg.spreadXZ(), 0.0, cfg.spreadXZ(), 0);

                    // Af en toe zwaardere 'adem' van rook
                    if ((elapsed % 20) == 0) {
                        l.getWorld().spawnParticle(Particle.LARGE_SMOKE, l, 6, 0.2, 0.0, 0.2, 0.01);
                    }

                    if ((elapsed % 60) == 0) {
                        player.playSound(l, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.20f, 1.0f);
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