package nl.wantedchef.empirewand.spell.earth;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class EarthQuake extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "EarthQuake";
            this.description = "Leap up and smash the ground: wave-like quake, crater, launch nearby foes.";
            this.cooldown = java.time.Duration.ofSeconds(8);
            this.spellType = SpellType.CONTROL;
        }

        @Override
        public Spell<Void> build() {
            return new EarthQuake(this);
        }
    }

    private EarthQuake(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "EarthQuake";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        final Player caster = context.caster();
        if (caster == null) {
            return null;
        }
        final World world = caster.getWorld();
        final Location location = caster.getLocation();

        // Config (griefen = OK zoals gevraagd)
        var spells = context.config().getSpellsConfig();
        final int radius = Math.max(3, spells.getInt("EarthQuake.values.radius", 6)); // 6
        final int minDepth = Math.max(1, spells.getInt("EarthQuake.values.min-depth", 4)); // 4
        final int maxDepth = Math.max(minDepth, spells.getInt("EarthQuake.values.max-depth", 6)); // 6
        final double kbOut = Math.max(0.1, spells.getDouble("EarthQuake.values.knockback", 1.35));
        final double kbUp = Math.max(0.0, spells.getDouble("EarthQuake.values.knockup", 0.6));
        final double leapFwd = spells.getDouble("EarthQuake.values.leap-forward", 1.25);
        final double leapUp = spells.getDouble("EarthQuake.values.leap-up", 1.55); // ~12–15 blok hoog

        // Launch in kijkrichting
        Vector dir = location.getDirection().normalize();
        Vector launch = dir.multiply(leapFwd);
        launch.setY(leapUp);
        caster.setVelocity(launch);

        // Directe cast FX
        world.spawnParticle(Particle.CLOUD, location, 40, 0.6, 0.1, 0.6, 0.02);
        world.playSound(location, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.8f, 0.85f);

        // Landing detecteren en smash uitvoeren
        new BukkitRunnable() {
            boolean leftGround = false;
            int safetyTicks = 20 * 6; // failsafe 6s
            final Random rng = new Random();

            @Override
            public void run() {
                if (--safetyTicks <= 0 || !caster.isValid() || caster.isDead()) {
                    cancel();
                    return;
                }

                // Houd val-schade uit tijdens sprong
                caster.setFallDistance(0f);

                if (!leftGround && !isGrounded(caster)) {
                    leftGround = true; // eenmaal airborne
                }

                if (leftGround && isGrounded(caster)) {
                    Location impact = caster.getLocation();

                    // Kleine correctie zodat de speler nét buiten de krater landt:
                    // we verplaatsen het krater-middelpunt iets achter de speler.
                    Location center = impact.clone().subtract(dir.clone().setY(0).normalize().multiply(2.0));

                    quakeSmash(center, caster, radius, minDepth, maxDepth, kbOut, kbUp, rng);
                    cancel();
                }
            }

            private void quakeSmash(Location center, Player caster, int radius, int minDepth, int maxDepth,
                    double kbOut, double kbUp, Random rng) {
                World w = center.getWorld();

                // Heftige impact-FX (realistisch stof/explosie + lokale block-dust)
                w.spawnParticle(Particle.EXPLOSION, center, 2, 0, 0, 0, 0.0);
                w.spawnParticle(Particle.EXPLOSION, center, 18, 1.2, 0.4, 1.2, 0.02);
                w.spawnParticle(Particle.LARGE_SMOKE, center, 90, 2.2, 0.8, 2.2, 0.0);
                w.spawnParticle(Particle.CLOUD, center, 120, 2.6, 0.9, 2.6, 0.02);

                Block under = center.clone().subtract(0, 1, 0).getBlock();
                BlockData dustData = under.getType().isAir() ? Material.DIRT.createBlockData() : under.getBlockData();
                // BLOCK particle gebruikt BlockData als data-type (1.20+)
                w.spawnParticle(Particle.BLOCK, center, 220, 2.6, 1.1, 2.6, 0, dustData);

                // Lage, zware rumble
                w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.9f, 0.55f);
                w.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, SoundCategory.PLAYERS, 1.5f, 0.65f);
                w.playSound(center, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1.2f, 0.75f);
                w.playSound(center, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.8f, 0.55f);

                // Launch entiteiten in de buurt (golf-achtig gevoel door lichte verticale
                // boost)
                for (var e : w.getNearbyEntities(center, radius + 2, 5, radius + 2)) {
                    if (e instanceof LivingEntity living && !living.equals(caster) && living.isValid()
                            && !living.isDead()) {
                        Vector out = living.getLocation().toVector().subtract(center.toVector()).normalize();
                        Vector v = out.multiply(kbOut);
                        v.setY(Math.max(v.getY(), kbUp));
                        living.setVelocity(v);
                    }
                }

                // Krater: bowl-vorm (dieper naar het midden)
                final int r = Math.max(2, radius);
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        double dist = Math.sqrt(x * x + z * z);
                        if (dist > r)
                            continue;

                        double t = 1.0 - (dist / r); // 1 in centrum -> 0 aan rand
                        int depth = (int) Math.round(minDepth + t * (maxDepth - minDepth));

                        // golfachtige uitholling: begin iets boven center zodat oppervlak meebeweegt
                        int surfaceY = center.getBlockY();

                        for (int dy = 0; dy < depth; dy++) {
                            Block b = w.getBlockAt(center.getBlockX() + x, surfaceY - dy, center.getBlockZ() + z);
                            Material type = b.getType();
                            if (type.isAir())
                                continue;

                            BlockData data = b.getBlockData();

                            // Outward velocity + lichte random lift
                            Vector out = new Vector(x, 0, z).normalize();
                            double speed = 0.25 + rng.nextDouble() * 0.45; // 0.25..0.7
                            Vector vel = out.multiply(speed).add(new Vector(0, 0.25 + rng.nextDouble() * 0.4, 0));

                            // Moderne entity-spawn zonder deprecated overloads
                            w.spawn(b.getLocation().add(0.5, 0.5, 0.5), FallingBlock.class, fb -> {
                                fb.setBlockData(data);
                                fb.setDropItem(false); // geen items
                                fb.setHurtEntities(false); // geen entity damage
                                fb.setVelocity(vel);
                            });

                            // Verwijder echte block (griefen toegestaan)
                            b.setType(Material.AIR, false);
                        }
                    }
                }

                // Natrilling (subtiel)
                w.spawnParticle(Particle.LARGE_SMOKE, center, 40, 2.0, 0.6, 2.0, 0.0);
                w.playSound(center, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 0.9f, 0.7f);
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);

        return null;
    }

    @Override
    protected void handleEffect(SpellContext context, Void result) {
        // visuals/logic zitten in executeSpell
    }

    /**
     * Grounded via block-passability onder de voeten (modern alternatief voor
     * deprecated isOnGround()).
     */
    private static boolean isGrounded(Player player) {
        Location loc = player.getLocation();
        Block below = loc.clone().subtract(0, 0.2, 0).getBlock();
        return !below.isPassable();
    }
}
