package com.example.empirewand.spell.implementation.life;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class BloodTap extends Spell<Void> {

    private static final String CHARGES_KEY = "blood_charges";
    private static final String DECAY_TASK_KEY = "blood_decay_task";

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blood Tap";
            this.description = "Sacrifice health to gain blood charges for other spells.";
            this.manaCost = 0;
            this.cooldown = java.time.Duration.ofMillis(500);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new BloodTap(this);
        }
    }

    private BloodTap(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "blood-tap";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @NotNull Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double selfDamage = spellConfig.getDouble("values.self-damage", 1.0);
        int maxCharges = spellConfig.getInt("values.max-charges", 5);
        double minHealth = spellConfig.getDouble("values.min-health", 2.0);

        if (player.getHealth() <= minHealth || getCurrentBloodCharges(player) >= maxCharges) {
            context.fx().fizzle(player);
            return null;
        }

        player.damage(selfDamage);
        setBloodCharges(player, getCurrentBloodCharges(player) + 1, context.plugin());
        startDecayTask(player, context);

        context.fx().playSound(player, Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
        spawnBloodParticles(player);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private void startDecayTask(Player player, SpellContext context) {
        if (player.hasMetadata(DECAY_TASK_KEY)) {
            BukkitRunnable existingTask = (BukkitRunnable) player.getMetadata(DECAY_TASK_KEY).get(0).value();
            if (existingTask != null) existingTask.cancel();
        }
        int decayDuration = spellConfig.getInt("values.decay-duration-ticks", 200);
        DecayTask decayTask = new DecayTask(player, context);
        decayTask.runTaskLater(context.plugin(), decayDuration);
        player.setMetadata(DECAY_TASK_KEY, new FixedMetadataValue(context.plugin(), decayTask));
    }

    private void spawnBloodParticles(Player player) {
        for (int i = 0; i < 20; i++) {
            double x = (Math.random() - 0.5) * 2;
            double y = Math.random() * 2;
            double z = (Math.random() - 0.5) * 2;
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, y, z), 1, new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 0), 1.0f));
        }
    }

    public static int getCurrentBloodCharges(Player player) {
        return player.hasMetadata(CHARGES_KEY) ? player.getMetadata(CHARGES_KEY).get(0).asInt() : 0;
    }

    public static void setBloodCharges(Player player, int charges, Plugin plugin) {
        player.setMetadata(CHARGES_KEY, new FixedMetadataValue(plugin, charges));
    }

    public static void consumeBloodCharges(Player player, int amount, Plugin plugin) {
        int current = getCurrentBloodCharges(player);
        if (current >= amount) {
            setBloodCharges(player, current - amount, plugin);
        }
    }

    private class DecayTask extends BukkitRunnable {
        private final Player player;
        private final SpellContext context;

        public DecayTask(Player player, SpellContext context) {
            this.player = player;
            this.context = context;
        }

        @Override
        public void run() {
            if (!player.isValid() || player.isDead()) return;

            int charges = getCurrentBloodCharges(player);
            if (charges > 0) {
                setBloodCharges(player, charges - 1, context.plugin());
                if (charges > 1) {
                    startDecayTask(player, context);
                }
            }
        }
    }
}