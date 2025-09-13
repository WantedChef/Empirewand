package nl.wantedchef.empirewand.spell.life;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import org.bukkit.World;
import org.bukkit.Location;

public class BloodBarrier extends Spell<Void> {

    private static final String BARRIER_ACTIVE_KEY = "blood_barrier_active";

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blood Barrier";
            this.description = "Creates a barrier that reduces damage and harms attackers.";
            this.cooldown = java.time.Duration.ofSeconds(30);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new BloodBarrier(this);
        }
    }

    private BloodBarrier(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "blood-barrier";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        org.bukkit.entity.Player player = context.caster();

        if (player.hasMetadata(BARRIER_ACTIVE_KEY)) {
            context.fx().fizzle(player);
            return null;
        }

        int duration = spellConfig.getInt("values.duration-ticks", 120);
        double damageReduction = spellConfig.getDouble("values.damage-reduction", 0.3);
        double thornsDamage = spellConfig.getDouble("values.thorns-damage", 1.0);

        player.setMetadata(BARRIER_ACTIVE_KEY, new org.bukkit.metadata.FixedMetadataValue(context.plugin(),
                new BarrierData(damageReduction, thornsDamage)));
        new BarrierTask(player, context).runTaskLater(context.plugin(), duration);

        spawnBarrierParticles(player);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled by a listener and scheduler task.
    }

    private void spawnBarrierParticles(@NotNull org.bukkit.entity.Player player) {
        World world = player.getWorld();
        if (world == null) {
            return;
        }
        Location baseLoc = player.getLocation();
        if (baseLoc == null)
            return;
        final double bx = baseLoc.getX();
        final double by = baseLoc.getY();
        final double bz = baseLoc.getZ();
        final float yaw = baseLoc.getYaw();
        final float pitch = baseLoc.getPitch();
        for (int i = 0; i < 30; i++) {
            double x = (Math.random() - 0.5) * 3;
            double y = Math.random() * 2;
            double z = (Math.random() - 0.5) * 3;
            Location loc = new Location(world, bx + x, by + y, bz + z, yaw, pitch);
            Objects.requireNonNull(world, "world").spawnParticle(org.bukkit.Particle.DUST, loc, 2,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 0), 1.0f));
        }
    }

    // This method would be called from a central, global EntityDamageByEntityEvent
    // listener
    public static void handleDamageEvent(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player))
            return;
        if (!player.hasMetadata(BARRIER_ACTIVE_KEY))
            return;

        var metaList = player.getMetadata(BARRIER_ACTIVE_KEY);
        if (metaList == null || metaList.isEmpty())
            return;
        Object metadataValue = metaList.get(0).value();
        if (!(metadataValue instanceof BarrierData data))
            return;

        event.setDamage(event.getDamage() * (1.0 - data.damageReduction));

        if (event.getDamager() instanceof org.bukkit.entity.LivingEntity attacker) {
            attacker.damage(data.thornsDamage, Objects.requireNonNull(player, "player"));
        }
    }

    private record BarrierData(double damageReduction, double thornsDamage) {
    }

    private class BarrierTask extends org.bukkit.scheduler.BukkitRunnable {
        private final @NotNull org.bukkit.entity.Player player;
        private final SpellContext context;

        public BarrierTask(@NotNull org.bukkit.entity.Player player, SpellContext context) {
            this.player = player;
            this.context = context;
        }

        @Override
        public void run() {
            if (player == null) {
                return;
            }
            if (!player.isValid() || !player.hasMetadata(BARRIER_ACTIVE_KEY)) {
                return;
            }
            World world = player.getWorld();
            if (world == null) {
                player.removeMetadata(BARRIER_ACTIVE_KEY, context.plugin());
                return;
            }
            Location playerLoc = Objects.requireNonNull(player.getLocation(), "player location");
            player.removeMetadata(BARRIER_ACTIVE_KEY, context.plugin());
            for (org.bukkit.entity.LivingEntity entity : world.getLivingEntities()) {
                Location entityLoc = Objects.requireNonNull(entity.getLocation(), "entity location");
                if (entityLoc.distance(playerLoc) <= 3.0 && !entity.equals(player)) {
                    entity.damage(0.5, Objects.requireNonNull(player, "player"));
                }
            }
        }
    }
}
