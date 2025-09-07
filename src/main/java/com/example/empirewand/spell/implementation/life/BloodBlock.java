package com.example.empirewand.spell.implementation.life;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.example.empirewand.spell.Prereq;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;

import net.kyori.adventure.text.Component;

public class BloodBlock implements Spell {
    private static final org.bukkit.NamespacedKey BLOOD_BLOCK_LOCATION = new org.bukkit.NamespacedKey("empirewand",
            "blood-block-location");

    @Override
    public void execute(SpellContext context) {
        Player caster = context.caster();

        // Get stored block location from persistent data
        String storedLocStr = caster.getPersistentDataContainer().get(BLOOD_BLOCK_LOCATION, PersistentDataType.STRING);

        if (storedLocStr == null) {
            // First cast: place redstone block
            placeBloodBlock(caster, context);
        } else {
            // Second cast: launch the block
            Location storedLoc = deserializeLocation(storedLocStr, caster.getWorld());
            if (storedLoc != null) {
                launchBloodBlock(caster, storedLoc, context);
            }
        }
    }

    private void placeBloodBlock(Player caster, SpellContext context) {
        // Find target block
        RayTraceResult rayTrace = caster.getWorld().rayTraceBlocks(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                20.0);

        if (rayTrace == null || rayTrace.getHitBlock() == null || rayTrace.getHitBlockFace() == null) {
            return; // No valid target
        }

        Block targetBlock = rayTrace.getHitBlock();
        Location placeLoc = targetBlock.getRelative(rayTrace.getHitBlockFace()).getLocation();

        // Check if location is valid for placement
        if (!placeLoc.getBlock().isEmpty()) {
            return;
        }

        // Place redstone block
        placeLoc.getBlock().setType(Material.REDSTONE_BLOCK);

        // Store location for next cast
        String locStr = serializeLocation(placeLoc);
        caster.getPersistentDataContainer().set(BLOOD_BLOCK_LOCATION, PersistentDataType.STRING, locStr);

        // Enhanced visual effects for better UX
        context.fx().spawnParticles(placeLoc, Particle.DUST, 30, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
        context.fx().spawnParticles(placeLoc, Particle.DUST, 20, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.5f));
        context.fx().playSound(placeLoc, org.bukkit.Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);
        context.fx().playSound(placeLoc, org.bukkit.Sound.ENTITY_PLAYER_HURT, 0.8f, 0.5f); // Blood-like sound
    }

    private void launchBloodBlock(Player caster, Location blockLoc, SpellContext context) {
        // Check if block is still there
        if (!blockLoc.getBlock().getType().equals(Material.REDSTONE_BLOCK)) {
            // Clear stored location
            caster.getPersistentDataContainer().remove(BLOOD_BLOCK_LOCATION);
            return;
        }

        // Remove the block
        blockLoc.getBlock().setType(Material.AIR);

        // Clear stored location
        caster.getPersistentDataContainer().remove(BLOOD_BLOCK_LOCATION);

        // Find launch target
        RayTraceResult rayTrace = caster.getWorld().rayTraceBlocks(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                20.0);

        Location targetLoc;
        if (rayTrace != null && rayTrace.getHitPosition() != null) {
            targetLoc = rayTrace.getHitPosition().toLocation(caster.getWorld());
        } else {
            // Fallback: launch in look direction
            targetLoc = caster.getEyeLocation().add(caster.getEyeLocation().getDirection().multiply(20));
        }

        // Create falling block
        FallingBlock fallingBlock = caster.getWorld().spawn(blockLoc, FallingBlock.class, (fb) -> {
            fb.setBlockData(Material.REDSTONE_BLOCK.createBlockData());
            fb.setDropItem(false);
        });

        // Calculate velocity towards target
        Vector direction = targetLoc.toVector().subtract(blockLoc.toVector()).normalize();
        direction = direction.multiply(1.5).setY(Math.max(0.6, direction.getY())); // Increased speed for more impact

        fallingBlock.setVelocity(direction);

        // Add trail effect for better UX
        addTrailEffect(fallingBlock, context);

        // Enhanced visual effects
        context.fx().spawnParticles(blockLoc, Particle.DUST, 40, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
        context.fx().spawnParticles(blockLoc, Particle.EXPLOSION, 10, 0.5, 0.5, 0.5, 0.1);
        context.fx().playSound(blockLoc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    private Location deserializeLocation(String str, org.bukkit.World defaultWorld) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            String[] parts = str.split(",");
            if (parts.length != 4) {
                return null;
            }
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) {
                world = defaultWorld;
            }
            return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]));
        } catch (NumberFormatException e) {
            // Log error or handle it appropriately
            return null;
        }
    }

    private void addTrailEffect(FallingBlock fallingBlock, SpellContext context) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (fallingBlock.isDead() || !fallingBlock.isValid()) {
                    this.cancel();
                    return;
                }
                // Trail particles
                context.fx().spawnParticles(fallingBlock.getLocation(), Particle.DUST, 5, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
            }
        }.runTaskTimer(context.plugin(), 0L, 2L); // Every 2 ticks

        // Impact effect when it lands
        fallingBlock.setMetadata("blood_block_trail",
                new org.bukkit.metadata.FixedMetadataValue(context.plugin(), this));
        // Listen for landing in a separate handler if needed, but for simplicity, add
        // on next tick
    }

    @Override
    public String getName() {
        return "blood-block";
    }

    @Override
    public String key() {
        return "blood-block";
    }

    @Override
    public Component displayName() {
        return Component.text("Blood Block");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
