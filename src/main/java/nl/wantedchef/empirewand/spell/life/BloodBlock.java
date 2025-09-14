package nl.wantedchef.empirewand.spell.life;

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
            this.description = "Creates a blood block that launches in a high arc and explodes on impact.";
            this.cooldown = java.time.Duration.ofSeconds(5);
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

        // Calculate proper arc trajectory for blood block
        Vector horizontalDirection = targetLoc.toVector().subtract(blockLoc.toVector());
        double horizontalDistance = Math.sqrt(horizontalDirection.getX() * horizontalDirection.getX() +
                                             horizontalDirection.getZ() * horizontalDirection.getZ());
        double verticalDistance = targetLoc.getY() - blockLoc.getY();

        // Arc parameters - configurable via spell config
        double launchSpeed = spellConfig.getDouble("blood-block.launch-speed", 2.2);
        double arcHeight = spellConfig.getDouble("blood-block.arc-height", 1.5);

        // Calculate launch angle for proper arc (45 degrees + height adjustment)
        double launchAngle = Math.toRadians(45.0);
        if (horizontalDistance > 0) {
            // Adjust angle based on distance and desired arc height
            double gravity = 0.04; // Minecraft gravity per tick
            double optimalAngle = 0.5 * Math.asin((gravity * horizontalDistance) / (launchSpeed * launchSpeed));
            launchAngle = Math.max(Math.toRadians(25.0), Math.min(Math.toRadians(70.0), optimalAngle + arcHeight * 0.3));
        }

        // Create velocity vector with proper arc
        Vector launchVector = new Vector();
        launchVector.setX((horizontalDirection.getX() / horizontalDistance) * launchSpeed * Math.cos(launchAngle));
        launchVector.setZ((horizontalDirection.getZ() / horizontalDistance) * launchSpeed * Math.cos(launchAngle));
        launchVector.setY(launchSpeed * Math.sin(launchAngle) + Math.max(0, verticalDistance * 0.1));

        fallingBlock.setVelocity(launchVector);

        addTrailEffect(fallingBlock, context);
        context.fx().playSound(blockLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
    }

    private void addTrailEffect(FallingBlock fallingBlock, SpellContext context) {
        new BukkitRunnable() {
            private Location lastLocation = fallingBlock.getLocation().clone();
            private int tickCount = 0;

            @Override
            public void run() {
                if (fallingBlock.isDead() || !fallingBlock.isValid()) {
                    // Block has landed - create impact effect
                    createLandingEffect(lastLocation, context);
                    this.cancel();
                    return;
                }

                Location currentLocation = fallingBlock.getLocation();

                // Enhanced blood trail particles
                context.fx().spawnParticles(currentLocation, Particle.DUST, 8, 0.15, 0.15, 0.15, 0,
                        new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.2f));

                // Blood droplet effects
                context.fx().spawnParticles(currentLocation, Particle.DRIPPING_LAVA, 3, 0.2, 0.2, 0.2, 0.05);

                // Whistling sound effect occasionally
                if (tickCount % 10 == 0) {
                    context.fx().playSound(currentLocation, Sound.ENTITY_ARROW_SHOOT, 0.3f, 1.8f);
                }

                lastLocation = currentLocation.clone();
                tickCount++;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    private void createLandingEffect(Location impactLocation, SpellContext context) {
        // Get configurable damage values
        double explosionRadius = spellConfig.getDouble("blood-block.explosion-radius", 4.0);
        double damage = spellConfig.getDouble("blood-block.damage", 8.0);

        // Blood explosion effect
        context.fx().spawnParticles(impactLocation, Particle.DUST, 50, explosionRadius * 0.5, 0.5, explosionRadius * 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.5f));

        // Additional blood splatter particles
        context.fx().spawnParticles(impactLocation, Particle.DUST, 25, explosionRadius * 0.4, 0.2, explosionRadius * 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.0f));

        // Lava droplets for dramatic effect
        context.fx().spawnParticles(impactLocation, Particle.DRIPPING_LAVA, 25, explosionRadius * 0.4, 0.8, explosionRadius * 0.4, 0.15);

        // Explosion sounds
        context.fx().playSound(impactLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
        context.fx().playSound(impactLocation, Sound.BLOCK_LAVA_POP, 0.8f, 0.6f);

        // Damage nearby entities
        impactLocation.getWorld().getNearbyLivingEntities(impactLocation, explosionRadius).forEach(entity -> {
            if (entity instanceof Player && entity.equals(context.caster())) {
                return; // Don't damage the caster
            }

            double distance = entity.getLocation().distance(impactLocation);
            if (distance <= explosionRadius) {
                // Calculate damage based on distance (closer = more damage)
                double actualDamage = damage * (1.0 - (distance / explosionRadius) * 0.7);
                entity.damage(actualDamage, context.caster());

                // Knockback effect
                Vector knockback = entity.getLocation().toVector().subtract(impactLocation.toVector()).normalize();
                knockback.multiply(1.5 - (distance / explosionRadius));
                knockback.setY(Math.max(0.3, knockback.getY()));
                entity.setVelocity(entity.getVelocity().add(knockback));

                // Blood particle effect on hit entities
                context.fx().spawnParticles(entity.getLocation().add(0, 1, 0), Particle.DUST, 15, 0.3, 0.5, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
            }
        });

        // Leave blood stains on the ground
        createBloodStains(impactLocation, context);
    }

    private void createBloodStains(Location center, SpellContext context) {
        // Create temporary blood stains around the impact area
        new BukkitRunnable() {
            private int duration = spellConfig.getInt("blood-block.stain-duration", 100); // 5 seconds default

            @Override
            public void run() {
                if (duration <= 0) {
                    this.cancel();
                    return;
                }

                // Spawn blood stain particles on the ground
                for (int i = 0; i < 3; i++) {
                    double offsetX = (Math.random() - 0.5) * 6;
                    double offsetZ = (Math.random() - 0.5) * 6;
                    Location stainLoc = center.clone().add(offsetX, 0.1, offsetZ);

                    context.fx().spawnParticles(stainLoc, Particle.DUST, 2, 0.1, 0.05, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(100, 0, 0), 0.8f));
                }

                duration--;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
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
