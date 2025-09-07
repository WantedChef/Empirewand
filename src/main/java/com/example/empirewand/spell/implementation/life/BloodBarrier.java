package com.example.empirewand.spell.implementation.life;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.LivingEntity;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class BloodBarrier implements Spell {
    private static final String BARRIER_ACTIVE_KEY = "blood_barrier_active";

    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        int duration = spells.getInt("blood-barrier.values.duration-ticks", 120);
        double damageReduction = spells.getDouble("blood-barrier.values.damage-reduction", 0.3);
        double thornsDamage = spells.getDouble("blood-barrier.values.thorns-damage", 1.0);

        // Check if already active
        if (player.hasMetadata(BARRIER_ACTIVE_KEY)) {
            context.fx().fizzle(player);
            return;
        }

        // Activate barrier
        player.setMetadata(BARRIER_ACTIVE_KEY, new FixedMetadataValue(context.plugin(), new BarrierData(damageReduction, thornsDamage)));

        // Start duration task
        new BarrierTask(player, context).runTaskLater(context.plugin(), duration);

        // Register listener (assuming plugin handles this)
        // In a real implementation, you'd register a listener in the plugin

        // Visuals
        spawnBarrierParticles(player);
    }

    private void spawnBarrierParticles(Player player) {
        for (int i = 0; i < 30; i++) {
            double x = (Math.random() - 0.5) * 3;
            double y = Math.random() * 2;
            double z = (Math.random() - 0.5) * 3;
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, y, z), 2,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 0), 1.0f));
        }
    }

    private static class BarrierData {
        public final double damageReduction;
        public final double thornsDamage;

        public BarrierData(double damageReduction, double thornsDamage) {
            this.damageReduction = damageReduction;
            this.thornsDamage = thornsDamage;
        }
    }

    private static class BarrierTask extends BukkitRunnable {
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
                // Small AOE prickles on expire
                for (LivingEntity entity : player.getWorld().getLivingEntities()) {
                    if (entity.getLocation().distance(player.getLocation()) <= 3.0 && !entity.equals(player)) {
                        entity.damage(0.5, player);
                    }
                }
            }
        }
    }

    // Static method to handle damage events
    public static void handleDamageEvent(EntityDamageByEntityEvent event, SpellContext context) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.hasMetadata(BARRIER_ACTIVE_KEY)) return;

        BarrierData data = (BarrierData) player.getMetadata(BARRIER_ACTIVE_KEY).get(0).value();
        if (data == null) return;

        // Reduce incoming damage
        double originalDamage = event.getDamage();
        double reducedDamage = originalDamage * (1.0 - data.damageReduction);
        event.setDamage(reducedDamage);

        // Apply thorns to attacker
        if (event.getDamager() instanceof LivingEntity attacker) {
            attacker.damage(data.thornsDamage, player);
        }
    }

    @Override
    public String getName() {
        return "blood-barrier";
    }

    @Override
    public String key() {
        return "blood-barrier";
    }

    @Override
    public Component displayName() {
        return Component.text("Blood Barrier");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
