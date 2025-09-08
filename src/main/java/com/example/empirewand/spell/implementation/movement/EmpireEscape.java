package com.example.empirewand.spell.implementation.movement;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class EmpireEscape extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empire Escape";
            this.description = "Teleports you a short distance and grants a speed boost.";
            this.manaCost = 8; // Example
            this.cooldown = java.time.Duration.ofSeconds(15);
            this.spellType = SpellType.MOVEMENT;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EmpireEscape(this);
        }
    }

    private EmpireEscape(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "empire-escape";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();

        double maxRange = spellConfig.getDouble("values.max-range", 16.0);
        int speedDuration = spellConfig.getInt("values.speed-duration", 40);

        RayTraceResult rayTrace = caster.getWorld().rayTraceBlocks(caster.getEyeLocation(), caster.getEyeLocation().getDirection(), maxRange);
        Location destination = (rayTrace != null && rayTrace.getHitPosition() != null)
                ? rayTrace.getHitPosition().toLocation(caster.getWorld()).subtract(caster.getEyeLocation().getDirection().multiply(0.5))
                : caster.getLocation().add(caster.getEyeLocation().getDirection().multiply(6.0));

        caster.teleport(findSafeLocation(destination));
        caster.setFallDistance(0);
        caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDuration, 0, false, true));

        context.fx().spawnParticles(caster.getLocation(), Particle.SMOKE, 20, 0.3, 0.3, 0.3, 0.1);
        context.fx().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private Location findSafeLocation(Location location) {
        for (int y = 0; y <= 3; y++) {
            Location testLoc = location.clone().add(0, y, 0);
            if (testLoc.getBlock().isEmpty() && testLoc.clone().add(0, 1, 0).getBlock().isEmpty()) {
                return testLoc;
            }
        }
        return location;
    }
}