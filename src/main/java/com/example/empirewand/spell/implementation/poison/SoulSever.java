package com.example.empirewand.spell.implementation.poison;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class SoulSever extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Soul Sever";
            this.description = "Dash through enemies, damaging them and causing nausea.";
            this.manaCost = 10; // Example
            this.cooldown = java.time.Duration.ofSeconds(15);
            this.spellType = SpellType.POISON;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new SoulSever(this);
        }
    }

    private SoulSever(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "soul-sever";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Zielsplinters");
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @NotNull Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double dashDistance = spellConfig.getDouble("values.dash-distance", 8.0);
        double damage = spellConfig.getDouble("values.damage", 2.0);
        int nauseaDuration = spellConfig.getInt("values.nausea-duration-ticks", 40);
        int nauseaAmplifier = spellConfig.getInt("values.nausea-amplifier", 0);
        double sampleStep = spellConfig.getDouble("values.sample-step", 0.5);
        boolean hitPlayers = spellConfig.getBoolean("flags.hit-players", true);
        boolean hitMobs = spellConfig.getBoolean("flags.hit-mobs", true);

        Vector direction = player.getLocation().getDirection().normalize();
        Location start = player.getLocation().clone();
        Location end = start.clone().add(direction.clone().multiply(dashDistance));

        if (!isSafeLocation(end)) {
            context.fx().fizzle(player);
            return null;
        }

        player.teleport(end);

        Set<LivingEntity> hitEntities = new HashSet<>();
        for (double dist = 0; dist <= dashDistance; dist += sampleStep) {
            Location current = start.clone().add(direction.clone().multiply(dist));
            for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(current, 1.0)) {
                if (entity.equals(player) || hitEntities.contains(entity)) continue;
                if ((entity instanceof Player && !hitPlayers) || (!(entity instanceof Player) && !hitMobs)) continue;

                hitEntities.add(entity);
                entity.damage(damage, player);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaDuration, nauseaAmplifier));
            }
        }

        context.fx().playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        spawnDashTrail(start, end, player);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().add(0, -1, 0).getBlock();
        return feet.getType() == Material.AIR && head.getType() == Material.AIR && ground.getType().isSolid();
    }

    private void spawnDashTrail(Location start, Location end, Player player) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d <= start.distance(end); d += 0.5) {
            player.getWorld().spawnParticle(Particle.SMOKE, start.clone().add(direction.clone().multiply(d)), 3, 0, 0, 0, 0);
        }
    }
}