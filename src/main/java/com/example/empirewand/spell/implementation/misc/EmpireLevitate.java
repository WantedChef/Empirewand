package com.example.empirewand.spell.implementation.misc;

import com.example.empirewand.api.EmpireWandAPI;

import com.example.empirewand.spell.PrereqInterface;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

public class EmpireLevitate extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Empire Levitate";
            this.description = "Levitates a target entity.";
            this.manaCost = 10; // Example
            this.cooldown = java.time.Duration.ofSeconds(20);
            this.spellType = SpellType.MISC;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new EmpireLevitate(this);
        }
    }

    private EmpireLevitate(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "empire-levitate";
    }

    @Override
    public Component displayName() {
        return Component.text("Empire Levitate").color(TextColor.color(138, 43, 226));
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player caster = context.caster();

        int duration = spellConfig.getInt("values.duration-ticks", 60);
        int amplifier = spellConfig.getInt("values.amplifier", 0);
        double maxRange = spellConfig.getDouble("values.max-range", 15.0);
        double bossHealthThreshold = spellConfig.getDouble("values.boss-health-threshold", 100.0);
        boolean affectPlayers = spellConfig.getBoolean("values.affect-players", false);

        RayTraceResult rayTrace = caster.rayTraceEntities((int) maxRange, false);
        if (rayTrace == null || !(rayTrace.getHitEntity() instanceof LivingEntity target) || target.isDead()) {
            context.fx().fizzle(caster);
            return null;
        }

        if (isBoss(target, bossHealthThreshold) || (!affectPlayers && target instanceof Player)) {
            context.fx().fizzle(caster);
            return null;
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, amplifier, false, true, true));

        Location targetLocation = target.getLocation().add(0, 1, 0);
        context.fx().spawnParticles(targetLocation, Particle.CLOUD, 20, 0.5, 0.5, 0.5, 0.1);
        context.fx().spawnParticles(targetLocation, Particle.ENCHANT, 15, 0.3, 0.3, 0.3, 0.05);
        context.fx().playSound(targetLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private boolean isBoss(LivingEntity entity, double healthThreshold) {
        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return maxHealthAttr != null && maxHealthAttr.getBaseValue() > healthThreshold;
    }
}