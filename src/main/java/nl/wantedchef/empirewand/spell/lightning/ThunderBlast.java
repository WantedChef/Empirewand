package nl.wantedchef.empirewand.spell.lightning;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.common.visual.RingRenderer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class ThunderBlast extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Thunder Blast";
            this.description = "Creates a powerful blast of lightning around you.";
            this.cooldown = java.time.Duration.ofSeconds(30);
            this.spellType = SpellType.LIGHTNING;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new ThunderBlast(this);
        }
    }

    private ThunderBlast(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "thunder-blast";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        Location center = player.getLocation();

        double radius = spellConfig.getDouble("values.radius", 12.0);
        double damage = spellConfig.getDouble("values.damage", 16.0);
        int strikes = spellConfig.getInt("values.strikes", 3);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        for (int i = 0; i < strikes; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * radius;
            Location strikeLoc = center.clone().add(distance * Math.cos(angle), 0, distance * Math.sin(angle));
            center.getWorld().strikeLightning(strikeLoc);
            damageAtStrike(context, strikeLoc, damage, friendlyFire);
        }

        context.fx().playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        int arcCount = spellConfig.getInt("arc-count", 6);
        double shockRadius = spellConfig.getDouble("shock-ring-radius", radius);
        int groundDensity = spellConfig.getInt("ground-spark-density", 10);
        spawnShockVisuals(context, center, arcCount, shockRadius, groundDensity);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private void damageAtStrike(SpellContext context, Location strikeLoc, double damage, boolean friendlyFire) {
        for (var entity : strikeLoc.getWorld().getNearbyLivingEntities(strikeLoc, 3.0)) {
            boolean shouldDamageCaster = entity.equals(context.caster()) && (friendlyFire || Math.random() < 0.2);
            if (entity.equals(context.caster()) && !shouldDamageCaster)
                continue;
            entity.damage(damage, context.caster());
            context.fx().spawnParticles(entity.getLocation(), Particle.ELECTRIC_SPARK, 10, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private void spawnShockVisuals(SpellContext context, Location center, int arcCount, double shockRadius,
            int groundDensity) {
        new BukkitRunnable() {
            double r = Math.max(2.0, shockRadius * 0.5);
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 6) {
                    cancel();
                    return;
                }
                RingRenderer.renderRing(center, r, 30,
                        (loc, vec) -> loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 1, 0, 0, 0, 0));
                r += (shockRadius - r) * 0.35;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);

        for (int i = 0; i < groundDensity; i++) {
            double ang = Math.random() * 2 * Math.PI;
            double d = Math.random() * shockRadius;
            center.getWorld().spawnParticle(Particle.CRIT, center.getX() + Math.cos(ang) * d, center.getY() + 0.1,
                    center.getZ() + Math.sin(ang) * d, 2, 0.05, 0.05, 0.05, 0.01);
        }
    }
}
