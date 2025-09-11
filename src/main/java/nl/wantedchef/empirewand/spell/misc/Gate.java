package nl.wantedchef.empirewand.spell.misc;

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
 * Gate - Teleport to a saved location from real Empirewand
 */
public class Gate extends Spell<Player> {

    private static final Map<UUID, Map<String, Location>> gateLocations = new HashMap<>();

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Gate";
            this.description = "Create and teleport through magical gates";
            this.cooldown = Duration.ofSeconds(30);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Player> build() {
            return new Gate(this);
        }
    }

    private static final int DEFAULT_CAST_TIME = 40;

    private Gate(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "gate";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(20);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }
    
    private void createPortalEffect(Location loc) {
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2 * i / 20;
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            for (int y = 0; y < 3; y++) {
                Location particleLoc = loc.clone().add(x, y, z);
                particleLoc.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 5, 0.1, 0.1, 0.1, 0.5);
            }
        }
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        int castTime = spellConfig.getInt("values.cast_time", DEFAULT_CAST_TIME);
        
        Map<String, Location> playerGates = gateLocations.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        
        if (player.isSneaking()) {
            // Set gate location
            String gateName = "default";
            playerGates.put(gateName, player.getLocation());
            
            // Visual effect for setting gate
            player.getWorld().spawnParticle(Particle.END_ROD, 
                player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
            context.fx().playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            
            player.sendMessage("§d§lGate §5set at current location!");
            return;
        }
        
        // Teleport to gate
        if (!playerGates.containsKey("default")) {
            player.sendMessage("§cNo gate set! Sneak while casting to set a gate.");
            return;
        }
        
        Location gateLoc = playerGates.get("default");
        player.sendMessage("§d§lGate §5opening...");
        
        // Channel effect
        new BukkitRunnable() {
            int ticks = 0;
            Location startLoc = player.getLocation().clone();
            
            @Override
            public void run() {
                if (ticks >= castTime) {
                    // Create portal effect
                    createPortalEffect(startLoc);
                    createPortalEffect(gateLoc);
                    
                    // Teleport
                    player.teleport(gateLoc);
                    
                    context.fx().playSound(gateLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                    player.sendMessage("§5Gate travel complete!");
                    cancel();
                    return;
                }
                
                // Check if player moved
                if (player.getLocation().distance(startLoc) > 1) {
                    player.sendMessage("§cGate cancelled - you moved!");
                    cancel();
                    return;
                }
                
                // Channel particles
                double progress = (double) ticks / castTime;
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double radius = 2 * (1 - progress);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = progress * 2;
                    Location loc = player.getLocation().clone().add(x, y, z);
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc, 1, 0, 0, 0, 0);
                }
                
                if (ticks % 10 == 0) {
                    context.fx().playSound(player, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.0f + (float)progress);
                }
                
                ticks += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }
}
