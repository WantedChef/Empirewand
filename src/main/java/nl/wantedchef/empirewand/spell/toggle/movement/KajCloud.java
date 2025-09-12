package nl.wantedchef.empirewand.spell.toggle.movement;

import java.time.Duration;
<<<<<<< HEAD
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
=======
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
>>>>>>> origin/main

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
<<<<<<< HEAD
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;
=======
>>>>>>> origin/main
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

public class KajCloud extends Spell<Void> implements ToggleableSpell {

<<<<<<< HEAD
    // Een map om de actieve taken per speler bij te houden.
    // De UUID is de unieke identifier van de speler.
    // BukkitTask is de taak die de deeltjes creëert.
    private static final Map<UUID, BukkitTask> ACTIVE_TASKS = new HashMap<>();
=======
    /* ---------------------------------------- */
    /* DATA */
    /* ---------------------------------------- */
    private final Map<UUID, CloudData> clouds = new WeakHashMap<>();
    private EmpireWandPlugin plugin;
>>>>>>> origin/main

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
        // Deze methode roept automatisch activate() of deactivate() aan,
        // afhankelijk van of de spreuk al actief is voor de speler.
        toggle(context.caster(), context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Omdat dit een toggle-spreuk is, gebeurt de logica in activate/deactivate.
        // Deze methode blijft dus leeg.
    }

    /**
     * Controleert of de spreuk momenteel actief is voor de opgegeven speler.
     * @param player De speler om te controleren.
     * @return true als de spreuk actief is, anders false.
     */
    @Override
    public boolean isActive(@NotNull Player player) {
<<<<<<< HEAD
        // We controleren of de UUID van de speler in onze map met actieve taken staat.
        return ACTIVE_TASKS.containsKey(player.getUniqueId());
=======
        return clouds.containsKey(player.getUniqueId());
>>>>>>> origin/main
    }

    /**
     * Activeert de spreuk voor de speler.
     * Start een herhalende taak die deeltjes toont als de speler vliegt.
     * @param player De speler die de spreuk activeert.
     * @param context De context van de spreuk.
     */
    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
<<<<<<< HEAD
        // Haal de plugin instance op om een taak te kunnen plannen.
        JavaPlugin plugin = context.plugin();

        // Maak een nieuwe herhalende taak (BukkitRunnable).
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Veiligheidscheck: als de speler niet meer online is, stop de taak.
                if (!player.isOnline()) {
                    deactivate(player, context); // Schakel de spreuk netjes uit.
                    return;
                }

                // Toon deeltjes alleen als de speler vliegt.
                if (player.isFlying()) {
                    // Spawn wolk-deeltjes iets onder de voeten van de speler.
                    // fx().spawnParticles(location, particle, count, offsetX, offsetY, offsetZ, speed)
                    context.fx().spawnParticles(player.getLocation().subtract(0, 0.2, 0), Particle.CLOUD, 10, 0.3, 0.1, 0.3, 0.01);
                }
            }
            // De taak start direct (0L) en herhaalt elke 4 ticks (ongeveer 5 keer per seconde).
        }.runTaskTimer(plugin, 0L, 4L);

        // Sla de taak op in de map, gekoppeld aan de speler zijn UUID.
        ACTIVE_TASKS.put(player.getUniqueId(), task);
        player.sendMessage("§aKaj Cloud is geactiveerd!");
=======
        if (isActive(player))
            return;
        plugin = context.plugin();
        clouds.put(player.getUniqueId(), new CloudData(player, context));
>>>>>>> origin/main
    }

    /**
     * Deactiveert de spreuk voor de speler.
     * Stopt en verwijdert de taak die deeltjes toont.
     * @param player De speler die de spreuk deactiveert.
     * @param context De context van de spreuk.
     */
    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
<<<<<<< HEAD
        UUID playerUUID = player.getUniqueId();
        // Controleer of er een actieve taak is voor deze speler.
        if (ACTIVE_TASKS.containsKey(playerUUID)) {
            // Haal de taak op en annuleer deze.
            ACTIVE_TASKS.get(playerUUID).cancel();
            // Verwijder de speler uit de map.
            ACTIVE_TASKS.remove(playerUUID);
            player.sendMessage("§cKaj Cloud is gedeactiveerd.");
=======
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
>>>>>>> origin/main
        }
    }
}