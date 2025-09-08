package com.example.empirewand.spell.implementation.life;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.EffectService;
import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BloodNova extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Blood Nova";
            this.description = "Unleashes a nova of blood, damaging nearby enemies based on stored charges.";
            this.manaCost = 10; // Example
            this.cooldown = java.time.Duration.ofSeconds(1);
            this.spellType = SpellType.LIFE;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new BloodNova(this);
        }
    }

    private BloodNova(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "blood-nova";
    }

    @Override
    public PrereqInterface prereq() {
        // Prereq could check for at least 1 blood charge
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        int charges = BloodTap.getCurrentBloodCharges(player);
        if (charges == 0) {
            context.fx().fizzle(player);
            return null;
        }

        double baseDamage = spellConfig.getDouble("values.base-damage", 4.0);
        double damagePerCharge = spellConfig.getDouble("values.damage-per-charge", 2.0);
        double radius = spellConfig.getDouble("values.radius", 4.0);
        double knockbackStrength = spellConfig.getDouble("values.knockback-strength", 1.0);
        boolean hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);

        double totalDamage = baseDamage + (charges * damagePerCharge);

        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.getLocation().distance(player.getLocation()) <= radius && !entity.equals(player)) {
                if ((entity instanceof Player && !hitPlayers) || (!(entity instanceof Player) && !hitMobs)) continue;

                entity.damage(totalDamage, player);
                Vector knockback = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(knockbackStrength).setY(0.3);
                entity.setVelocity(entity.getVelocity().add(knockback));
            }
        }

        BloodTap.consumeBloodCharges(player, charges, context.plugin());

        context.fx().playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        spawnNovaParticles(player, radius);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private void spawnNovaParticles(Player player, double radius) {
        for (int i = 0; i < 100; i++) {
            double angle = 2 * Math.PI * i / 100;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, 1, z), 1, new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 0), 1.0f));
        }
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 1, 0, 0, 0, 0.1);
    }
}