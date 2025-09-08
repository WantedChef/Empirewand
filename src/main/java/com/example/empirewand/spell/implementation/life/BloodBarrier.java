package com.example.empirewand.spell.implementation.life;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.EffectService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class BloodBarrier extends Spell<Void> {

    private static final String BARRIER_ACTIVE_KEY = "blood_barrier_active";

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blood Barrier";
            this.description = "Creates a barrier that reduces damage and harms attackers.";
            this.manaCost = 15; // Example
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
        Player player = context.caster();

        if (player.hasMetadata(BARRIER_ACTIVE_KEY)) {
            context.fx().fizzle(player);
            return null;
        }

        int duration = spellConfig.getInt("values.duration-ticks", 120);
        double damageReduction = spellConfig.getDouble("values.damage-reduction", 0.3);
        double thornsDamage = spellConfig.getDouble("values.thorns-damage", 1.0);

        player.setMetadata(BARRIER_ACTIVE_KEY, new FixedMetadataValue(context.plugin(), new BarrierData(damageReduction, thornsDamage)));
        new BarrierTask(player, context).runTaskLater(context.plugin(), duration);

        spawnBarrierParticles(player);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled by a listener and scheduler task.
    }

    private void spawnBarrierParticles(Player player) {
        for (int i = 0; i < 30; i++) {
            double x = (Math.random() - 0.5) * 3;
            double y = Math.random() * 2;
            double z = (Math.random() - 0.5) * 3;
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, y, z), 2, new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 0), 1.0f));
        }
    }

    // This method would be called from a central, global EntityDamageByEntityEvent listener
    public static void handleDamageEvent(EntityDamageByEntityEvent event, EmpireWandAPI api) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.hasMetadata(BARRIER_ACTIVE_KEY)) return;

        Object metadataValue = player.getMetadata(BARRIER_ACTIVE_KEY).get(0).value();
        if (!(metadataValue instanceof BarrierData data)) return;

        event.setDamage(event.getDamage() * (1.0 - data.damageReduction));

        if (event.getDamager() instanceof LivingEntity attacker) {
            attacker.damage(data.thornsDamage, player);
        }
    }

    private record BarrierData(double damageReduction, double thornsDamage) {}

    private class BarrierTask extends BukkitRunnable {
        private final Player player;
        private final SpellContext context;

        public BarrierTask(Player player, SpellContext context) {
            this.player = player;
            this.context = context;
        }

        @Override
        public void run() {
            if (player.isValid() && player.hasMetadata(BARRIER_ACTIVE_KEY)) {
                player.removeMetadata(BARRIER_ACTIVE_KEY, context.plugin());
                for (LivingEntity entity : player.getWorld().getLivingEntities()) {
                    if (entity.getLocation().distance(player.getLocation()) <= 3.0 && !entity.equals(player)) {
                        entity.damage(0.5, player);
                    }
                }
            }
        }
    }
}