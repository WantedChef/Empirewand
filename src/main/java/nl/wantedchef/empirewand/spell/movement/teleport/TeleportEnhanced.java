package nl.wantedchef.empirewand.spell.movement.teleport;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Teleport to a targeted location with enhanced sound and visual effects.
 */
public class TeleportEnhanced extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Enhanced Teleport";
            this.description = "Teleport to a targeted location with enhanced effects.";
            this.cooldown = java.time.Duration.ofSeconds(10);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new TeleportEnhanced(this);
        }
    }

    private TeleportEnhanced(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "teleport-enhanced";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 30.0);

        // Get target block
        Block targetBlock = player.getTargetBlock(null, (int) range);
        if (targetBlock == null) {
            context.fx().fizzle(player);
            return null;
        }

        Location targetLocation = targetBlock.getLocation().add(0.5, 1, 0.5); // Center of block, one block above
        targetLocation.setYaw(player.getYaw());
        targetLocation.setPitch(player.getPitch());

        // Check if location is safe
        if (!isLocationSafe(targetLocation)) {
            context.fx().fizzle(player);
            return null;
        }

        // Create pre-teleport effects
        createPreTeleportEffects(context, player);

        // Delay the teleport to allow for effects
        new BukkitRunnable() {
            @Override
            public void run() {
                // Teleport the player
                player.teleport(targetLocation);
                
                // Create post-teleport effects
                createPostTeleportEffects(context, player);
            }
        }.runTaskLater(context.plugin(), 10L); // 0.5 second delay

        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private boolean isLocationSafe(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().add(0, -1, 0).getBlock();
        return !feet.getType().isSolid() && !head.getType().isSolid() && ground.getType().isSolid();
    }

    private void createPreTeleportEffects(SpellContext context, Player player) {
        Location location = player.getLocation();
        
        // Sound effects
        context.fx().playSound(player, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 0.5f);
        context.fx().playSound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.5f);
        
        // Particle effects - imploding vortex
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 10;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isValid()) {
                    cancel();
                    return;
                }
                
                double radius = 2.0 * (1.0 - (ticks / (double) duration));
                
                // Create imploding ring
                for (int i = 0; i < 24; i++) {
                    double angle = 2 * Math.PI * i / 24;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location particleLoc = location.clone().add(x, 0.5, z);
                    player.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 2, 0, 0, 0, 0);
                }
                
                // Create imploding particles that move toward center
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double distance = Math.random() * 2.5;
                    double x = distance * Math.cos(angle);
                    double z = distance * Math.sin(angle);
                    Location startLoc = location.clone().add(x, 0.5, z);
                    Vector direction = location.toVector().subtract(startLoc.toVector()).normalize().multiply(0.3);
                    Location particleLoc = startLoc.add(direction);
                    player.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
                }
                
                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    private void createPostTeleportEffects(SpellContext context, Player player) {
        Location location = player.getLocation();
        
        // Sound effects
        context.fx().playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.2f);
        context.fx().playSound(player, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
        
        // Particle effects - exploding vortex
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 15;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isValid()) {
                    cancel();
                    return;
                }
                
                double radius = 0.5 + (ticks / (double) duration) * 3.0;
                
                // Create expanding ring
                for (int i = 0; i < 24; i++) {
                    double angle = 2 * Math.PI * i / 24;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location particleLoc = location.clone().add(x, 0.5, z);
                    player.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 2, 0, 0, 0, 0);
                }
                
                // Create expanding particles that move away from center
                if (ticks % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = 2 * Math.PI * i / 8;
                        Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(0.5);
                        Location particleLoc = location.clone().add(0, 0.5, 0).add(direction);
                        player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }
}