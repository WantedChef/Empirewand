package com.example.empirewand.spell.implementation;

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

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class BloodBlock implements Spell {
    private static final org.bukkit.NamespacedKey BLOOD_BLOCK_LOCATION = new org.bukkit.NamespacedKey("empirewand", "blood-block-location");

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
            20.0
        );

        if (rayTrace == null || rayTrace.getHitBlock() == null) {
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

        // Visual effects
        context.fx().spawnParticles(placeLoc, Particle.DUST, 20, 0.5, 0.5, 0.5, 0, new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
        context.fx().playSound(placeLoc, org.bukkit.Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
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
            20.0
        );

        Location targetLoc;
        if (rayTrace != null && rayTrace.getHitPosition() != null) {
            targetLoc = rayTrace.getHitPosition().toLocation(caster.getWorld());
        } else {
            // Fallback: launch in look direction
            targetLoc = caster.getEyeLocation().add(caster.getEyeLocation().getDirection().multiply(20));
        }

        // Create falling block using alternative method
        FallingBlock fallingBlock = caster.getWorld().spawn(blockLoc, FallingBlock.class);
        fallingBlock.setBlockData(Material.REDSTONE_BLOCK.createBlockData());

        // Calculate velocity towards target
        Vector direction = targetLoc.toVector().subtract(blockLoc.toVector()).normalize();
        direction = direction.multiply(1.2).setY(Math.max(0.4, direction.getY()));

        fallingBlock.setVelocity(direction);
        fallingBlock.setDropItem(false); // Don't drop item on land

        // Visual effects
        context.fx().spawnParticles(blockLoc, Particle.DUST, 30, 0.3, 0.3, 0.3, 0, new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
        context.fx().playSound(blockLoc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    private Location deserializeLocation(String str, org.bukkit.World defaultWorld) {
        try {
            String[] parts = str.split(",");
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) world = defaultWorld;
            return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
        } catch (Exception e) {
            return null;
        }
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