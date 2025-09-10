package nl.wantedchef.empirewand.spell.life.blood.defensive;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BloodBlock extends Spell<Void> {

    private static final NamespacedKey BLOOD_BLOCK_LOCATION = new NamespacedKey("empirewand", "blood-block-location");

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blood Block";
            this.description = "Creates a block of blood that can be launched at enemies.";
            this.cooldown = java.time.Duration.ofSeconds(1);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new BloodBlock(this);
        }
    }

    private BloodBlock(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "blood-block";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();
        String storedLocStr = caster.getPersistentDataContainer().get(BLOOD_BLOCK_LOCATION, PersistentDataType.STRING);

        if (storedLocStr == null) {
            placeBloodBlock(caster, context);
        } else {
            Location storedLoc = deserializeLocation(storedLocStr, caster.getWorld());
            if (storedLoc != null) {
                launchBloodBlock(caster, storedLoc, context);
            }
        }
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private void placeBloodBlock(Player caster, SpellContext context) {
        RayTraceResult rayTrace = caster.getWorld().rayTraceBlocks(caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(), 20.0);
        if (rayTrace == null || rayTrace.getHitBlock() == null || rayTrace.getHitBlockFace() == null) {
            return;
        }

        // Explicit null checks to satisfy static analysis
        var hitBlock = rayTrace.getHitBlock();
        var hitBlockFace = rayTrace.getHitBlockFace();
        if (hitBlock == null || hitBlockFace == null) {
            return;
        }

        Location placeLoc = hitBlock.getRelative(hitBlockFace).getLocation();
        if (!placeLoc.getBlock().isEmpty()) {
            return;
        }

        placeLoc.getBlock().setType(Material.REDSTONE_BLOCK);
        caster.getPersistentDataContainer().set(BLOOD_BLOCK_LOCATION, PersistentDataType.STRING,
                serializeLocation(placeLoc));

        context.fx().spawnParticles(placeLoc, Particle.DUST, 30, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
        context.fx().playSound(placeLoc, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);
    }

    private void launchBloodBlock(Player caster, Location blockLoc, SpellContext context) {
        if (blockLoc.getBlock().getType() != Material.REDSTONE_BLOCK) {
            caster.getPersistentDataContainer().remove(BLOOD_BLOCK_LOCATION);
            return;
        }

        blockLoc.getBlock().setType(Material.AIR);
        caster.getPersistentDataContainer().remove(BLOOD_BLOCK_LOCATION);

        RayTraceResult rayTrace = caster.getWorld().rayTraceBlocks(caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(), 20.0);
        Location targetLoc = (rayTrace != null)
                ? rayTrace.getHitPosition().toLocation(caster.getWorld())
                : caster.getEyeLocation().add(caster.getEyeLocation().getDirection().multiply(20));

        FallingBlock fallingBlock = caster.getWorld().spawn(blockLoc, FallingBlock.class, fb -> {
            fb.setBlockData(Material.REDSTONE_BLOCK.createBlockData());
            fb.setDropItem(false);
        });

        Vector direction = targetLoc.toVector().subtract(blockLoc.toVector()).normalize().multiply(1.5)
                .setY(Math.max(0.6, targetLoc.toVector().subtract(blockLoc.toVector()).normalize().getY()));
        fallingBlock.setVelocity(direction);

        addTrailEffect(fallingBlock, context);
        context.fx().playSound(blockLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
    }

    private void addTrailEffect(FallingBlock fallingBlock, SpellContext context) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (fallingBlock.isDead() || !fallingBlock.isValid()) {
                    this.cancel();
                    return;
                }
                context.fx().spawnParticles(fallingBlock.getLocation(), Particle.DUST, 5, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    private Location deserializeLocation(String str, org.bukkit.World defaultWorld) {
        try {
            String[] parts = str.split(",");
            org.bukkit.World world = (parts.length == 4) ? org.bukkit.Bukkit.getWorld(parts[0]) : defaultWorld;
            return new Location(world, Double.parseDouble(parts[parts.length - 3]),
                    Double.parseDouble(parts[parts.length - 2]), Double.parseDouble(parts[parts.length - 1]));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
}
