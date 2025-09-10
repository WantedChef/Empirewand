package nl.wantedchef.empirewand.spell.toggle.movement;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.EmpireWandPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * MephiCloud - Nether-themed toggleable particle effect that creates ash and fire particles under
 * the player when they are flying, representing Mephidantes' connection to the Nether.
 */
public final class MephiCloud extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, CloudData> clouds = new WeakHashMap<>();
    private EmpireWandPlugin plugin;

    /* ---------------------------------------- */
    /* BUILDER */
    /* ---------------------------------------- */
    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Mephi Cloud";
            description = "Create nether-themed ash and fire particles under you while flying.";
            cooldown = Duration.ofSeconds(5);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new MephiCloud(this);
        }
    }

    private MephiCloud(Builder builder) {
        super(builder);
    }

    /* ---------------------------------------- */
    /* SPELL API */
    /* ---------------------------------------- */
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
        return clouds.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(Player player, SpellContext context) {
        if (isActive(player))
            return;
        plugin = context.plugin();
        clouds.put(player.getUniqueId(), new CloudData(player, context));
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        Optional.ofNullable(clouds.remove(player.getUniqueId())).ifPresent(CloudData::stop);
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public void forceDeactivate(Player player) {
        deactivate(player, null);
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class CloudData {
        private final Player player;
        private final BukkitTask ticker;
        private final boolean wasFlying;

        CloudData(Player player, SpellContext context) {
            this.player = player;
            this.wasFlying = player.isFlying();

            // Show activation message
            player.sendMessage(Component.text(
                    "§c🔥 §7Mephi Cloud activated. Nether particles will appear when you fly."));

            // Start the ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 2); // Run every
                                                                                        // 2 ticks
        }

        void stop() {
            ticker.cancel();
            player.sendMessage(Component.text("§c🔥 §7Mephi Cloud deactivated."));
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            // Only show particles if player is flying or in creative mode
            if (player.isFlying() || player.getGameMode() == GameMode.CREATIVE) {
                spawnNetherParticles();
            }
        }

        private void spawnNetherParticles() {
            Location loc = player.getLocation().subtract(0, 0.5, 0); // Slightly below player
            World world = player.getWorld();

            // Create ash particles in a circle under the player
            for (int i = 0; i < 6; i++) {
                double angle = 2 * Math.PI * i / 6;
                double x = Math.cos(angle) * 0.4;
                double z = Math.sin(angle) * 0.4;
                Location particleLoc = loc.clone().add(x, 0, z);
                world.spawnParticle(Particle.ASH, particleLoc, 1, 0.1, 0.1, 0.1, 0);
            }

            // Create fire particles
            if (Math.random() < 0.4) {
                world.spawnParticle(Particle.FLAME, loc, 2, 0.2, 0.1, 0.2, 0.01);
            }

            // Occasionally create lava particles
            if (Math.random() < 0.2) {
                world.spawnParticle(Particle.LAVA, loc, 1, 0.1, 0.1, 0.1, 0);
            }

            // Occasionally create smoke particles
            if (Math.random() < 0.3) {
                world.spawnParticle(Particle.SMOKE, loc, 3, 0.3, 0.1, 0.3, 0.02);
            }
        }
    }
}
