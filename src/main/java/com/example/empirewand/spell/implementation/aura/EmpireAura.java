package com.example.empirewand.spell.implementation.aura;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class EmpireAura extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empire Aura";
            this.description = "Grants allies buffs and weakens foes around you.";
            this.manaCost = 10;
            this.cooldown = java.time.Duration.ofSeconds(30);
            this.spellType = SpellType.AURA;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EmpireAura(this);
        }
    }

    private EmpireAura(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "empire-aura";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double radius = spellConfig.getDouble("values.radius", 6.0);
        int duration = spellConfig.getInt("values.duration-ticks", 400);
        int tickInterval = spellConfig.getInt("values.tick-interval", 10);

        startAuraTask(player, radius, duration, tickInterval, context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effect is handled in the scheduler
    }

    private void startAuraTask(Player caster, double radius, int duration, int tickInterval, SpellContext context) {
        new AuraTask(caster, radius, duration, tickInterval, context).runTaskTimer(context.plugin(), 0L, tickInterval);
    }

    private static class AuraTask extends BukkitRunnable {
        private final Player caster;
        private final double radius;
        private final int duration;
        private final int tickInterval;
        private final SpellContext context;
        private int ticks = 0;

        public AuraTask(Player caster, double radius, int duration, int tickInterval, SpellContext context) {
            this.caster = caster;
            this.radius = radius;
            this.duration = duration;
            this.tickInterval = tickInterval;
            this.context = context;
        }

        @Override
        public void run() {
            if (ticks >= duration || !caster.isValid() || caster.isDead()) {
                this.cancel();
                return;
            }

            Location center = caster.getLocation();

            for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) {
                    continue;
                }

                if (living instanceof Player) { // Simplified ally check
                    living.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, true));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, true));
                    context.plugin().getFxService().spawnParticles(living.getLocation(), Particle.SMOKE, 3, 0.2, 0.2, 0.2, 0.1);
                } else {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, true));
                    context.plugin().getFxService().spawnParticles(living.getLocation(), Particle.SMOKE, 3, 0.2, 0, 0.2, 0.1);
                }
            }

            createAuraRing(center, radius);
            ticks += tickInterval;
        }

        private void createAuraRing(Location center, double radius) {
            for (int i = 0; i < 360; i += 20) {
                double angle = Math.toRadians(i);
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                Location particleLoc = new Location(center.getWorld(), x, center.getY(), z);
                context.plugin().getFxService().spawnParticles(particleLoc, Particle.SMOKE, 1, 0, 0, 0, 0);
            }
        }
    }
}