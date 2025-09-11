package nl.wantedchef.empirewand.spell.movement;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Recall - Teleport to last death or saved location from real Empirewand
 */
public class Recall extends Spell<Player> {

    private static final Map<UUID, Location> savedLocations = new HashMap<>();
    private static final Map<UUID, Location> deathLocations = new HashMap<>();

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Recall";
            this.description = "Teleport to your last death or saved location";
            this.cooldown = Duration.ofSeconds(60);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Recall(this);
        }
    }

    private static final int DEFAULT_CAST_TIME = 60;

    private Recall(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "recall";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(15);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }
    
    public static void recordDeath(Player player) {
        deathLocations.put(player.getUniqueId(), player.getLocation());
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int castTime = spellConfig.getInt("values.cast_time", DEFAULT_CAST_TIME);
        
        // Save current location
        savedLocations.put(player.getUniqueId(), player.getLocation());
        
        // Determine recall location
        Location recallLocation;
        if (player.isSneaking() && deathLocations.containsKey(player.getUniqueId())) {
            recallLocation = deathLocations.get(player.getUniqueId());
            player.sendMessage("§c§lRecalling §4to last death location...");
        } else if (player.getBedSpawnLocation() != null) {
            recallLocation = player.getBedSpawnLocation();
            player.sendMessage("§a§lRecalling §2to bed spawn...");
        } else {
            recallLocation = player.getWorld().getSpawnLocation();
            player.sendMessage("§e§lRecalling §6to world spawn...");
        }
        
        // Channel effect
        new BukkitRunnable() {
            int ticks = 0;
            Location startLoc = player.getLocation().clone();
            
            @Override
            public void run() {
                if (ticks >= castTime) {
                    // Teleport player
                    player.teleport(recallLocation);
                    
                    // Effects at destination
                    recallLocation.getWorld().spawnParticle(Particle.PORTAL, 
                        recallLocation, 50, 0.5, 1, 0.5, 0.1);
                    context.fx().playSound(recallLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    
                    player.sendMessage("§aRecall complete!");
                    cancel();
                    return;
                }
                
                // Check if player moved
                if (player.getLocation().distance(startLoc) > 1) {
                    player.sendMessage("§cRecall cancelled - you moved!");
                    cancel();
                    return;
                }
                
                // Channel particles
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle) * 1.5;
                    double z = Math.sin(angle) * 1.5;
                    Location loc = player.getLocation().clone().add(x, 0.5, z);
                    loc.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 1, 0, 0, 0, 0);
                }
                
                if (ticks % 20 == 0) {
                    context.fx().playSound(player, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.0f + (float)ticks/castTime);
                    player.sendMessage("§7Recalling... " + ((castTime - ticks) / 20) + "s");
                }
                
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
    }
}
