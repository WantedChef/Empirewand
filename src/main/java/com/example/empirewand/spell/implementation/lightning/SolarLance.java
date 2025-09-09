package com.example.empirewand.spell.implementation.lightning;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class SolarLance extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Solar Lance";
            this.description = "Fires a piercing lance of solar energy.";
            this.cooldown = java.time.Duration.ofSeconds(10);
            this.spellType = SpellType.LIGHTNING;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new SolarLance(this);
        }
    }

    private SolarLance(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "solar-lance";
    }

    @Override
    public Component displayName() {
        return Component.text("Zonschicht");
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 20.0);
        double damage = spellConfig.getDouble("values.damage", 6.0);
        int glowingDuration = spellConfig.getInt("values.glowing-duration-ticks", 60);
        int maxPierce = spellConfig.getInt("values.max-pierce", 3);
        double sampleStep = spellConfig.getDouble("values.sample-step", 0.5);
        boolean hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Set<LivingEntity> hitEntities = new HashSet<>();

        for (double dist = 0; dist <= range && hitEntities.size() < maxPierce; dist += sampleStep) {
            Location current = player.getEyeLocation().add(direction.clone().multiply(dist));
            player.getWorld().spawnParticle(Particle.CRIT, current, 2, 0, 0, 0, 0);
            player.getWorld().spawnParticle(Particle.DUST, current, 1,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.0f));

            for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(current, 1.0)) {
                if (entity.equals(player) || hitEntities.contains(entity))
                    continue;
                if ((entity instanceof Player && !hitPlayers) || (!(entity instanceof Player) && !hitMobs))
                    continue;

                hitEntities.add(entity);
                entity.damage(damage, player);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowingDuration, 0));
            }
        }

        context.fx().playSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.5f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }
}