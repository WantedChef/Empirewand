package nl.wantedchef.empirewand.spell.toggle.movement;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

public class KajCloud extends Spell<Void> implements ToggleableSpell {

    // Een map om de actieve taken per speler bij te houden.
    // De UUID is de unieke identifier van de speler.
    // BukkitTask is de taak die de deeltjes creëert.
    private static final Map<UUID, BukkitTask> ACTIVE_TASKS = new HashMap<>();

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
        // We controleren of de UUID van de speler in onze map met actieve taken staat.
        return ACTIVE_TASKS.containsKey(player.getUniqueId());
    }

    /**
     * Activeert de spreuk voor de speler.
     * Start een herhalende taak die deeltjes toont als de speler vliegt.
     * @param player De speler die de spreuk activeert.
     * @param context De context van de spreuk.
     */
    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
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
    }

    /**
     * Deactiveert de spreuk voor de speler.
     * Stopt en verwijdert de taak die deeltjes toont.
     * @param player De speler die de spreuk deactiveert.
     * @param context De context van de spreuk.
     */
    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        UUID playerUUID = player.getUniqueId();
        // Controleer of er een actieve taak is voor deze speler.
        if (ACTIVE_TASKS.containsKey(playerUUID)) {
            // Haal de taak op en annuleer deze.
            ACTIVE_TASKS.get(playerUUID).cancel();
            // Verwijder de speler uit de map.
            ACTIVE_TASKS.remove(playerUUID);
            player.sendMessage("§cKaj Cloud is gedeactiveerd.");
        }
    }
}