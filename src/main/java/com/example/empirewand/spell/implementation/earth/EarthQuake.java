package com.example.empirewand.spell.implementation.earth;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
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
            this.description = "Leap and smash the ground, creating a crater and knocking back foes.";
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
        final World world = caster.getWorld();

        // Config (met defaults, maar griefen = prima)
        var spells = context.config().getSpellsConfig();
        final int radius = spells.getInt("EarthQuake.values.radius", 6); // midden -> rand (jouw 6)
        final int minDepth = spells.getInt("EarthQuake.values.min-depth", 4); // 4
        final int maxDepth = spells.getInt("EarthQuake.values.max-depth", 6); // 6
        final double kbStrength = spells.getDouble("EarthQuake.values.knockback", 1.35);
        final double kbUp = spells.getDouble("EarthQuake.values.knockup", 0.6);
        final double leapForward = spells.getDouble("EarthQuake.values.leap-forward", 1.25);
        final double leapUp = spells.getDouble("EarthQuake.values.leap-up", 1.55); // ~12-15 blok hoog

        // Launch in kijkrichting
        Vector dir = caster.getLocation().getDirection().normalize();
        Vector launch = dir.clone().multiply(leapForward);
        launch.setY(leapUp);
        caster.setVelocity(launch);

        // Directe cast FX (optieel, kleine hint)
        context.fx().spawnParticles(caster.getLocation(), Particle.CLOUD, 40, 0.6, 0.1, 0.6, 0.02);
        context.fx().playSound(caster.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 0.7f, 0.8f);

        // Landing detecteren en smash uitvoeren
        new BukkitRunnable() {
            boolean airborneSeen = false;
            int safetyTicks = 20 * 6; // failsafe 6s
            final Random rng = new Random();

            @Override
            public void run() {
                safetyTicks--;
                // kleine val-schade-bescherming tijdens sprong (optioneel)
                caster.setFallDistance(0.0f);

                if (!airborneSeen && !isGrounded(caster)) {
                    airborneSeen = true;
                }

                if (airborneSeen && isGrounded(caster)) {
                    // Impactlocatie = waar hij landt (krater middelpunt)
                    Location impact = caster.getLocation().clone();
                    smash(impact);
                    cancel();
                    return;
                }

                if (safetyTicks <= 0 || !caster.isValid() || caster.isDead()) {
                    cancel();
                }
            }

            private void smash(Location impact) {
                // Heftige particles (aardbeving vibe)
                // Grote explosie + stof + rook + blokstof
                world.spawnParticle(Particle.EXPLOSION, impact, 3, 0, 0, 0, 0.05);
                world.spawnParticle(Particle.EXPLOSION, impact, 6, 1.5, 0.6, 1.5, 0.02);
                world.spawnParticle(Particle.CLOUD, impact, 120, 2.5, 0.8, 2.5, 0.02);
                world.spawnParticle(Particle.LARGE_SMOKE, impact, 80, 2.2, 0.8, 2.2, 0.0);

                // Probeer lokale block dust te gebruiken (steen/grond), pak het blok onder
                // impact
                Block under = impact.clone().subtract(0, 1, 0).getBlock();
                BlockData dustData = under.getType().isAir() ? Material.DIRT.createBlockData() : under.getBlockData();
                world.spawnParticle(Particle.BLOCK, impact, 200, 2.5, 1.2, 2.5, dustData);

                // Aardbevings-geluiden (laag, zwaar)
                // Mix van zware explosie + steen vallen/breken + diepe rumble
                world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.55f);
                world.playSound(impact, Sound.BLOCK_DEEPSLATE_BREAK, SoundCategory.PLAYERS, 1.5f, 0.6f);
                world.playSound(impact, Sound.BLOCK_STONE_FALL, SoundCategory.PLAYERS, 1.7f, 0.7f);
                world.playSound(impact, Sound.BLOCK_BASALT_BREAK, SoundCategory.PLAYERS, 1.2f, 0.7f);
                // subtiele, lage "rumble"
                world.playSound(impact, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.7f, 0.4f);

                // Knockback op entiteiten
                for (var e : world.getNearbyEntities(impact, radius + 2, 5, radius + 2)) {
                    if (e instanceof LivingEntity living && !living.equals(caster) && living.isValid()
                            && !living.isDead()) {
                        Vector push = living.getLocation().toVector().subtract(impact.toVector()).normalize()
                                .multiply(kbStrength);
                        push.setY(kbUp);
                        living.setVelocity(push);
                    }
                }

                // Krater uithakken: bowl-vorm met diepte 4–6, rand op radius = 6
                // Voor performance: beperk grenswaarden en sla niet-air/solid check toe als
                // nodig
                int r = Math.max(2, radius);
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        double dist = Math.sqrt(x * x + z * z);
                        if (dist > r)
                            continue;

                        // radiale diepte (dieper in het midden, 4..6)
                        double t = 1.0 - (dist / r); // 1 in centrum -> 0 aan rand
                        int depth = (int) Math.round(minDepth + t * (maxDepth - minDepth)); // 4..6

                        for (int dy = 0; dy < depth; dy++) {
                            Block b = impact.clone().add(x, -dy, z).getBlock();
                            Material type = b.getType();
                            if (type.isAir())
                                continue;

                            // Spawn FallingBlock met outward speed
                            BlockData data = b.getBlockData();

                            // Geef een velocity weg van het midden + een klein beetje omhoog
                            Vector out = new Vector(x, 0, z).normalize();
                            double speed = 0.25 + rng.nextDouble() * 0.45; // 0.25..0.7
                            Vector v = out.multiply(speed).add(new Vector(0, 0.25 + rng.nextDouble() * 0.4, 0));

                            world.spawn(
                                    b.getLocation().add(0.5, 0.5, 0.5),
                                    FallingBlock.class,
                                    spawned -> {
                                        spawned.setBlockData(data);
                                        spawned.setDropItem(false);
                                        spawned.setHurtEntities(false);
                                        spawned.setVelocity(v);
                                    });

                            // Echte blok wordt lucht (grief = OK)
                            b.setType(Material.AIR, false);
                        }
                    }
                }

                // Extra naschok particles/sounds (subtiel)
                world.spawnParticle(Particle.LARGE_SMOKE, impact, 40, 2.0, 0.6, 2.0, 0.0);
                world.playSound(impact, Sound.BLOCK_STONE_FALL, SoundCategory.PLAYERS, 1.0f, 0.65f);
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);

        return null;
    }

    @Override
    protected void handleEffect(SpellContext context, Void result) {
        // No post-execution effects; this spell handles visuals within executeSpell
    }

    private static boolean isGrounded(Player player) {
        Location loc = player.getLocation();
        Block blockBelow = loc.clone().subtract(0, 0.2, 0).getBlock();
        // Consider grounded when the block just below feet is not passable (solid
        // ground)
        return !blockBelow.isPassable();
    }
}
