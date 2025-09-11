package nl.wantedchef.empirewand.spell.control;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class ZeistChronoAnchor extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Zeist Chrono Anchor";
            this.description = "Creates a time bubble that slows entities and projectiles.";
            this.cooldown = java.time.Duration.ofSeconds(20);
            this.spellType = SpellType.CONTROL;
        }

        @Override
        @NotNull
        public ZeistChronoAnchor build() {
            return new ZeistChronoAnchor(this);
        }
    }

    private ZeistChronoAnchor(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "zeist-chrono-anchor";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return null;
        }

        double radius = spellConfig.getDouble("values.radius", 5.0);
        int duration = spellConfig.getInt("values.duration-ticks", 100);
        int amplifier = spellConfig.getInt("values.slowness-amplifier", 1);
        double projectileSlow = spellConfig.getDouble("values.projectile-slow", 0.2);

        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amplifier, false, true));
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, true));
            } else if (entity instanceof Projectile projectile) {
                projectile.setVelocity(projectile.getVelocity().multiply(projectileSlow));
            }
        }

        context.fx().spawnParticles(center, Particle.END_ROD, 40, radius, radius, radius, 0.1);
        context.fx().playSound(player, Sound.BLOCK_BELL_USE, 1.0f, 0.5f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // No additional effects
    }
}
