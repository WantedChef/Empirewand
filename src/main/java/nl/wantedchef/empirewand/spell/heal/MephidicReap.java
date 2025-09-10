package nl.wantedchef.empirewand.spell.heal;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class MephidicReap extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Mephidic Reap";
            this.description = "Throws a boomerang scythe that damages and slows enemies.";
            this.cooldown = java.time.Duration.ofSeconds(15);
            this.spellType = SpellType.POISON;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new MephidicReap(this);
        }
    }

    private MephidicReap(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "mephidic-reap";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 8.0);

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location start = player.getEyeLocation();
        Location end = start.clone().add(direction.multiply(range));

        ArmorStand scythe = player.getWorld().spawn(start, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.setInvulnerable(true);
        });

        new BoomerangTask(scythe, start, end, player, context).runTaskTimer(context.plugin(), 0L, 1L);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private class BoomerangTask extends BukkitRunnable {
        private final ArmorStand scythe;
        private final Location start;
        private final Location end;
        private final Player caster;
        private final SpellContext context;
        private final double damage;
        private final int slownessDuration;
        private final int maxPierce;
        private final int travelTicks;
        private final boolean hitPlayers;
        private final boolean hitMobs;
        private int tick = 0;
        private boolean returning = false;
        private final Set<LivingEntity> hitEntities = new HashSet<>();

        public BoomerangTask(ArmorStand scythe, Location start, Location end, Player caster, SpellContext context) {
            this.scythe = scythe;
            this.start = start;
            this.end = end;
            this.caster = caster;
            this.context = context;
            this.damage = spellConfig.getDouble("values.damage", 2.0);
            this.slownessDuration = spellConfig.getInt("values.slowness-duration-ticks", 20);
            this.maxPierce = spellConfig.getInt("values.max-pierce", 3);
            this.travelTicks = spellConfig.getInt("values.travel-ticks", 14);
            this.hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
            this.hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);
        }

        @Override
        public void run() {
            if (!scythe.isValid()) {
                this.cancel();
                return;
            }

            tick++;
            double progress = returning ? 1.0 - ((double) tick / travelTicks) : (double) tick / travelTicks;
            scythe.teleport(start.clone().add(end.toVector().subtract(start.toVector()).multiply(progress)));

            if (hitEntities.size() < maxPierce) {
                for (LivingEntity entity : scythe.getWorld().getNearbyLivingEntities(scythe.getLocation(), 1.5)) {
                    if (!entity.equals(caster) && !hitEntities.contains(entity)) {
                        if ((entity instanceof Player && !hitPlayers) || (!(entity instanceof Player) && !hitMobs))
                            continue;
                        entity.damage(damage, caster);
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, 0));
                        hitEntities.add(entity);
                        context.fx().spawnParticles(entity.getLocation(), Particle.CRIT, 5, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }

            context.fx().spawnParticles(scythe.getLocation(), Particle.SMOKE, 2, 0, 0, 0, 0);

            if (tick >= travelTicks) {
                if (returning) {
                    scythe.remove();
                    this.cancel();
                } else {
                    returning = true;
                    tick = 0;
                    hitEntities.clear(); // Can hit again on return
                }
            }
        }
    }
}
