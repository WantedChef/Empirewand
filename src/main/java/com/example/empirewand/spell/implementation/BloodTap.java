package com.example.empirewand.spell.implementation;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;

public class BloodTap implements Spell {
    private static final String CHARGES_KEY = "blood_charges";
    private static final String DECAY_TASK_KEY = "blood_decay_task";

    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();

        // Config values
        var spells = context.config().getSpellsConfig();
        double selfDamage = spells.getDouble("blood-tap.values.self-damage", 1.0);
        int maxCharges = spells.getInt("blood-tap.values.max-charges", 5);
        int decayDuration = spells.getInt("blood-tap.values.decay-duration-ticks", 200);
        double minHealth = spells.getDouble("blood-tap.values.min-health", 2.0);

        // Check minimum health
        if (player.getHealth() <= minHealth) {
            context.fx().fizzle(player);
            return;
        }

        // Get current charges
        int currentCharges = getBloodCharges(player);

        if (currentCharges >= maxCharges) {
            context.fx().fizzle(player);
            return;
        }

        // Apply self-damage
        player.damage(selfDamage);

        // Add charge
        setBloodCharges(player, currentCharges + 1, context);

        // Start/restart decay task
        startDecayTask(player, decayDuration, context);

        // Visuals and SFX
        context.fx().playSound(player, Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
        spawnBloodParticles(player);
    }

    private int getBloodCharges(Player player) {
        if (player.hasMetadata(CHARGES_KEY)) {
            return player.getMetadata(CHARGES_KEY).get(0).asInt();
        }
        return 0;
    }

    private void setBloodCharges(Player player, int charges, SpellContext context) {
        player.setMetadata(CHARGES_KEY, new FixedMetadataValue(context.plugin(), charges));
    }

    private void startDecayTask(Player player, int duration, SpellContext context) {
        // Cancel existing decay task
        if (player.hasMetadata(DECAY_TASK_KEY)) {
            BukkitRunnable existingTask = (BukkitRunnable) player.getMetadata(DECAY_TASK_KEY).get(0).value();
            if (existingTask != null) {
                existingTask.cancel();
            }
        }

        // Start new decay task
        DecayTask decayTask = new DecayTask(player, context);
        decayTask.runTaskLater(context.plugin(), duration);
        player.setMetadata(DECAY_TASK_KEY, new FixedMetadataValue(context.plugin(), decayTask));
    }

    private void spawnBloodParticles(Player player) {
        for (int i = 0; i < 20; i++) {
            double x = (Math.random() - 0.5) * 2;
            double y = Math.random() * 2;
            double z = (Math.random() - 0.5) * 2;
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, y, z), 1,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 0), 1.0f));
        }
    }

    private static class DecayTask extends BukkitRunnable {
        private final Player player;
        private final SpellContext context;

        public DecayTask(Player player, SpellContext context) {
            this.player = player;
            this.context = context;
        }

        @Override
        public void run() {
            if (!player.isValid() || player.isDead()) return;

            int charges = getBloodCharges(player);
            if (charges > 0) {
                setBloodCharges(player, charges - 1, context);
                // Restart decay for remaining charges
                if (charges > 1) {
                    var spells = context.config().getSpellsConfig();
                    int decayDuration = spells.getInt("blood-tap.values.decay-duration-ticks", 200);
                    startDecayTask(player, decayDuration, context);
                }
            }
        }

        private int getBloodCharges(Player player) {
            if (player.hasMetadata(CHARGES_KEY)) {
                return player.getMetadata(CHARGES_KEY).get(0).asInt();
            }
            return 0;
        }

        private void setBloodCharges(Player player, int charges, SpellContext context) {
            player.setMetadata(CHARGES_KEY, new FixedMetadataValue(context.plugin(), charges));
        }

        private void startDecayTask(Player player, int duration, SpellContext context) {
            DecayTask decayTask = new DecayTask(player, context);
            decayTask.runTaskLater(context.plugin(), duration);
            player.setMetadata(DECAY_TASK_KEY, new FixedMetadataValue(context.plugin(), decayTask));
        }
    }

    // Static method to get charges for other spells
    public static int getCurrentBloodCharges(Player player) {
        if (player.hasMetadata(CHARGES_KEY)) {
            return player.getMetadata(CHARGES_KEY).get(0).asInt();
        }
        return 0;
    }

    // Static method to consume charges
    public static boolean consumeBloodCharges(Player player, int amount, SpellContext context) {
        int current = getCurrentBloodCharges(player);
        if (current >= amount) {
            setBloodChargesStatic(player, current - amount, context);
            return true;
        }
        return false;
    }

    private static void setBloodChargesStatic(Player player, int charges, SpellContext context) {
        player.setMetadata(CHARGES_KEY, new FixedMetadataValue(context.plugin(), charges));
    }

    @Override
    public String getName() {
        return "blood-tap";
    }

    @Override
    public String key() {
        return "blood-tap";
    }

    @Override
    public Component displayName() {
        return Component.text("Blood Tap");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}