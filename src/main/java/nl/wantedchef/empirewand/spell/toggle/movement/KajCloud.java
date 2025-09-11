package nl.wantedchef.empirewand.spell.toggle.movement;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

public class KajCloud extends Spell<Void> implements ToggleableSpell {

    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, CloudData> clouds = new WeakHashMap<>();
    private EmpireWandPlugin plugin;

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Kaj Cloud";
            description = "Create beautiful cloud particles under you while flying.";
            cooldown = Duration.ofSeconds(5);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new KajCloud(this);
        }
    }

    private KajCloud(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "kaj-cloud";
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
        // Instant effect - toggle handling is done in executeSpell
    }

    @Override
    public boolean isActive(@NotNull Player player) {
        return clouds.containsKey(player.getUniqueId());
    }

    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        if (isActive(player))
            return;
        plugin = context.plugin();
        clouds.put(player.getUniqueId(), new CloudData(player, context));
    }

    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        Optional.ofNullable(clouds.remove(player.getUniqueId())).ifPresent(CloudData::stop);
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public void forceDeactivate(@NotNull Player player) {
        deactivate(player, null);
    }

    /* ---------------------------------------- */
    /* INTERNAL CLASS */
    /* ---------------------------------------- */
    private final class CloudData {
        private final Player player;
        private final BukkitTask ticker;

        CloudData(Player player, SpellContext context) {
            this.player = player;

            // Show activation message
            player.sendMessage(Component.text(
                    "§b☁ §7Kaj Cloud activated. Beautiful cloud particles will appear when you fly."));

            // Start the ticker
            this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 3); // Run every 3 ticks
        }

        void stop() {
            ticker.cancel();
            player.sendMessage(Component.text("§b☁ §7Kaj Cloud deactivated."));
        }

        private void tick() {
            if (!player.isOnline()) {
                forceDeactivate(player);
                return;
            }

            // Only show particles if player is flying or in creative mode
            if (player.isFlying() || player.getGameMode() == GameMode.CREATIVE) {
                spawnCloudParticles();
            }
        }

        private void spawnCloudParticles() {
            Location loc = player.getLocation().subtract(0, 0.5, 0); // Slightly below player
            World world = player.getWorld();

            // Create white cloud particles in a circle under the player
            for (int i = 0; i < 8; i++) {
                double angle = 2 * Math.PI * i / 8;
                double x = Math.cos(angle) * 0.6;
                double z = Math.sin(angle) * 0.6;
                Location particleLoc = loc.clone().add(x, 0, z);
                world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.2, 0.1, 0.2, 0.02);
            }

            // Create white ash particles for a soft effect
            if (Math.random() < 0.5) {
                world.spawnParticle(Particle.WHITE_ASH, loc, 3, 0.3, 0.1, 0.3, 0.02);
            }

            // Create snowflake particles occasionally
            if (Math.random() < 0.3) {
                world.spawnParticle(Particle.SNOWFLAKE, loc, 2, 0.4, 0.1, 0.4, 0.01);
            }

            // Create end rod particles for magical sparkle
            if (Math.random() < 0.2) {
                world.spawnParticle(Particle.END_ROD, loc, 1, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }
}