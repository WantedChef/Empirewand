package nl.wantedchef.empirewand.spell.poison;

import nl.wantedchef.empirewand.api.EmpireWandAPI;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class CrimsonChains extends ProjectileSpell<Snowball> {

    public static class Builder extends ProjectileSpell.Builder<Snowball> {
        public Builder(EmpireWandAPI api) {
            super(api, Snowball.class);
            this.name = "Crimson Chains";
            this.description = "Launches a chain that pulls an enemy towards you and slows them.";
            this.cooldown = java.time.Duration.ofSeconds(12);
            this.spellType = SpellType.POISON;
            this.trailParticle = null; // Custom trail
            this.hitSound = Sound.BLOCK_CHAIN_BREAK;
        }

        @Override
        @NotNull
        public ProjectileSpell<Snowball> build() {
            return new CrimsonChains(this);
        }
    }

    private CrimsonChains(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "crimson-chains";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        super.launchProjectile(context);
        // The base class handles launching. We could add a custom sound here.
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof LivingEntity target))
            return;

        boolean hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);
        if ((target instanceof Player && !hitPlayers) || (!(target instanceof Player) && !hitMobs))
            return;

        double pullStrength = spellConfig.getDouble("values.pull-strength", 0.5);
        int slownessDuration = spellConfig.getInt("values.slowness-duration-ticks", 40);
        int slownessAmplifier = spellConfig.getInt("values.slowness-amplifier", 1);

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, slownessAmplifier));

        AttributeInstance maxHealthAttr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        boolean isBoss = maxHealthAttr != null && maxHealthAttr.getBaseValue() > 100;

        if (!isBoss) {
            Vector pullVector = context.caster().getLocation().toVector().subtract(target.getLocation().toVector())
                    .normalize();
            target.setVelocity(target.getVelocity().add(pullVector.multiply(pullStrength)));
        }

        new ChainVisual(context, context.caster().getEyeLocation(), target).runTaskTimer(context.plugin(), 0L, 1L);
    }

    private class ChainVisual extends BukkitRunnable {
        private final SpellContext context;
        private final Location start;
        private final LivingEntity target;
        private int ticks = 0;

        public ChainVisual(SpellContext context, Location start, LivingEntity target) {
            this.context = context;
            this.start = start;
            this.target = target;
        }

        @Override
        public void run() {
            if (ticks++ > 20 || !target.isValid()) {
                this.cancel();
                return;
            }
            Location end = target.getEyeLocation();
            Vector vector = end.toVector().subtract(start.toVector());
            for (double i = 0; i < vector.length(); i += 0.2) {
                Location point = start.clone().add(vector.clone().normalize().multiply(i));
                context.fx().spawnParticles(point, Particle.DUST, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(128, 0, 0), 1.0f));
            }
        }
    }
}
