package nl.wantedchef.empirewand.spell.lightning;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class LightningStorm extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lightning Storm";
            this.description = "Calls down a storm of lightning strikes around you.";
            this.cooldown = java.time.Duration.ofSeconds(45);
            this.spellType = SpellType.LIGHTNING;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new LightningStorm(this);
        }
    }

    private LightningStorm(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "lightning-storm";
    }

    @Override
    public Component displayName() {
        return Component.text("Lightning Storm").color(TextColor.color(255, 255, 0));
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        Player player = context.caster();
        Location center = resolveTargetLocation(context);

        double radius = spellConfig.getDouble("values.radius", 12.0); // default to 24-wide circle
        double damage = spellConfig.getDouble("values.damage", 16.0);
        int delayBetweenStrikes = Math.max(6, spellConfig.getInt("values.delay-ticks", 10));
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        // Visual ring + synchronized strikes along the rim
        runRingWithStrikes(context, center.clone(), radius, damage, friendlyFire, delayBetweenStrikes);

        // Keep center relatively quiet; use light ambient instead of loud thunder at caster
        context.fx().playSound(player, Sound.WEATHER_RAIN_ABOVE, 0.4f, 1.0f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Effects are handled in the scheduler.
    }

    private void damageAtStrike(SpellContext context, Location strikeLoc, double damage, boolean friendlyFire) {
        for (var entity : strikeLoc.getWorld().getNearbyLivingEntities(strikeLoc, 3.0)) {
            if (entity.equals(context.caster()) && !friendlyFire)
                continue;
            entity.damage(damage, context.caster());
            context.fx().spawnParticles(entity.getLocation(), Particle.ELECTRIC_SPARK, 10, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private Location resolveTargetLocation(SpellContext context) {
        Player player = context.caster();
        if (context.hasTargetLocation()) {
            return context.targetLocation();
        }
        // Try block in sight first (clear to the player)
        var block = player.getTargetBlockExact(40);
        if (block != null) {
            return block.getLocation().add(0.5, 0, 0.5); // center of block
        }
        // Fallback: a point in front of player
        var eye = player.getEyeLocation();
        var dir = eye.getDirection();
        return eye.add(dir.multiply(20)).toLocation(player.getWorld());
    }

    private void runRingWithStrikes(SpellContext context, Location center, double r, double damage,
                                    boolean friendlyFire, int tickDelay) {
        World world = center.getWorld();

        final int points = 72; // smooth circle
        final double delta = (2 * Math.PI) / points;
        // Start direction based on player facing
        float yaw = center.getYaw();
        double startAngle = Math.toRadians(-yaw + 90.0); // convert yaw to mathematical angle

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step >= points / 2) { // both directions cover full circle together
                    cancel();
                    return;
                }

                double angA = startAngle + step * delta;
                double angB = startAngle - step * delta;

                Location locA = center.clone().add(r * Math.cos(angA), 0.0, r * Math.sin(angA));
                Location locB = center.clone().add(r * Math.cos(angB), 0.0, r * Math.sin(angB));

                spawnWhiteFirework(context.plugin(), world, locA);
                spawnWhiteFirework(context.plugin(), world, locB);

                // subtle guiding particles along the perimeter
                world.spawnParticle(Particle.END_ROD, locA.clone().add(0, 0.2, 0), 2, 0.02, 0.02, 0.02, 0.01);
                world.spawnParticle(Particle.END_ROD, locB.clone().add(0, 0.2, 0), 2, 0.02, 0.02, 0.02, 0.01);

                // Visual lightning effect and damage at ring points
                world.strikeLightningEffect(locA);
                world.strikeLightningEffect(locB);
                damageAtStrike(context, locA, damage, friendlyFire);
                damageAtStrike(context, locB, damage, friendlyFire);

                step++;
            }
        }.runTaskTimer(context.plugin(), 0L, Math.max(4L, tickDelay)); // slower sweep for visibility
    }

    private void spawnWhiteFirework(Plugin plugin, World world, Location loc) {
        Firework fw = (Firework) world.spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.setPower(0); // minimal ascent
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BURST)
                .withColor(Color.WHITE)
                .withFade(Color.WHITE)
                .flicker(true)
                .trail(true)
                .build());
        fw.setFireworkMeta(meta);

        // Detonate almost immediately to keep it crisp and at ground level
        new BukkitRunnable() {
            @Override
            public void run() {
                fw.detonate();
            }
        }.runTaskLater(plugin, 3L); // small delay so the firework is seen before detonation
    }
}
